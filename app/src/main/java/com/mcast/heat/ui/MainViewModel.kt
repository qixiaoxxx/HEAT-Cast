package com.mcast.heat.ui


import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mcast.heat.BaseViewModel
import com.mcast.heat.BuildConfig
import com.mcast.heat.data.CastSessionInfo
import com.mcast.heat.data.UpdateRequest
import com.mcast.heat.data.UpdateResponse
import com.mcast.heat.data.repository.IServiceRepository
import com.mcast.heat.util.calculateDurationToLong
import com.mcast.heat.util.formatDurationFromSeconds
import com.mcast.heat.util.getAndroidId
import com.mcast.heat.util.getCurrentFormattedTime
import com.mcast.heat.util.getManufactureModel
import com.mcast.heat.util.logFirebaseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val serviceRepository: IServiceRepository,
) : BaseViewModel(application) {
    val updateInfo = MutableLiveData<UpdateResponse>()
    private var sessionId: String = ""
    private var sessionStartTime: String = ""
    private var androidId: String = ""
    private var manufactureModel: String = ""
    private var resolution: String = "" // 用于存储分辨率

    // 用于存储投屏SDK相关事件的时间
    private var initSdkTime: String = "not_initialized"
    private var startSdkTime: String = "not_started"
    private val totalCastCount = AtomicInteger(0) // 累计投屏次数
    private var totalCastDurationSeconds = 0L  // 累计投屏总时长（秒）

    // 用于累计周期内单次投屏详细信息的列表
    private val castSessionDetails = mutableListOf<CastSessionInfo>()
    private val gson = Gson()
    private var periodicReportJob: Job? = null

    init {
        asyncGetUpdate(application)
    }

    // 调用升级接口
    private fun asyncGetUpdate(context: Context) {
        viewModelScope.launch {
            try {
                serviceRepository.getUpdateResponse(
                    UpdateRequest(
                        BuildConfig.APPLICATION_ID,
                        "",
                        "main",
                        BuildConfig.VERSION_CODE,
                        BuildConfig.VERSION_NAME,
                        getAndroidId(context),
                        Build.MANUFACTURER
                    )
                ).collect { updateResponse ->
                    updateInfo.value = updateResponse
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 记录 initSdk 的发生时间
    fun recordInitSdkTime() {
        this.initSdkTime = getCurrentFormattedTime()
    }

    // 记录 startSdk 的发生时间
    fun recordStartSdkTime() {
        this.startSdkTime = getCurrentFormattedTime()
    }

    // 在App启动时调用
    fun onSessionStart(context: Context, resolution: String) {
        if (sessionId.isNotEmpty()) return // 防止重复初始化
        this.sessionId = "session_${System.currentTimeMillis()}"
        this.sessionStartTime = getCurrentFormattedTime()
        this.androidId = getAndroidId(context)
        this.manufactureModel = getManufactureModel()
        this.resolution = resolution // 保存分辨率
        // 立即上报一次【启动事件】
        reportSessionData(context, "app_session_start")
        // 启动【周期性上报】任务
        startPeriodicReporting(context)
    }

    // 在App退出时调用
    fun onSessionStop(context: Context) {
        // 取消周期性任务
        periodicReportJob?.cancel()
        periodicReportJob = null
        // 上报最终的【退出事件】
        reportSessionData(context, "app_session_stop")
    }

    // 当一次投屏会话完成时，调用此方法
    fun recordCompletedCast(castStartTime: String, castEndTime: String) {
        try {
            val durationSeconds = calculateDurationToLong(castStartTime, castEndTime)
            if (durationSeconds >= 0) { // 允许0秒的投屏
                totalCastCount.incrementAndGet()
                totalCastDurationSeconds += durationSeconds
                val castInfo = CastSessionInfo(
                    startTime = castStartTime,
                    endTime = castEndTime,
                    duration = formatDurationFromSeconds(durationSeconds) // <<--- 使用格式化方法
                )
                // 将本次投屏详情加入到列表中，等待周期性上报
                synchronized(castSessionDetails) {
                    castSessionDetails.add(castInfo)
                }
            }
        } catch (e: Exception) {
            // 时间计算或格式化可能出错，记录下来但不要让应用崩溃
            e.printStackTrace()
        }
    }

    // 启动周期性上报任务，每小时上报一次
    private fun startPeriodicReporting(context: Context) {
        periodicReportJob?.cancel()
        periodicReportJob = viewModelScope.launch {
            while (true) {
                delay(3_600_000) // 1小时
                reportSessionData(context, "app_session_periodic")
            }
        }
    }

    // 统一的会话数据上报方法 (启动、周期、退出)
    private fun reportSessionData(context: Context, eventName: String) {
        val params = mutableListOf<Pair<String, String>>()
        // 静态会话信息
        params.add("session_id" to sessionId)
        params.add("event_time" to getCurrentFormattedTime())
        params.add("session_start_time" to sessionStartTime)
        params.add("android_id" to androidId)
        params.add("manufacture_model" to manufactureModel)
        params.add("resolution" to resolution)
        // 添加SDK初始化和启动时间
        params.add("init_sdk_time" to initSdkTime)
        params.add("start_sdk_time" to startSdkTime)
        // 动态累计的数据
        params.add("total_cast_count" to totalCastCount.get().toString())
        params.add(
            "total_cast_duration" to formatDurationFromSeconds(
                totalCastDurationSeconds
            )
        )
        // 检查并上报周期内单次投屏的详细信息列表
        synchronized(castSessionDetails) {
            if (castSessionDetails.isNotEmpty()) {
                // 将详细信息列表转换为 JSON 字符串
                val detailsJson = gson.toJson(castSessionDetails)
                params.add("cast_sessions_details" to detailsJson)
                // 上报后清空列表，为下一个周期做准备
                castSessionDetails.clear()
            }
        }
        // 如果是退出事件，额外计算总会话时长
        if (eventName == "app_session_stop") {
            try {
                val totalSessionDuration =
                    calculateDurationToLong(sessionStartTime, getCurrentFormattedTime())
                params.add(
                    "total_session_duration" to formatDurationFromSeconds(
                        totalSessionDuration
                    )
                )
            } catch (_: Exception) {
                params.add("total_session_duration_seconds" to "error")
            }
        }
        logFirebaseEvent(context, eventName, *params.toTypedArray())
    }
}
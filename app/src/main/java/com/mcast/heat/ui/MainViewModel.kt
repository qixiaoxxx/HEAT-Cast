package com.mcast.heat.ui


import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mcast.heat.BaseViewModel
import com.mcast.heat.BuildConfig
import com.mcast.heat.data.UpdateRequest
import com.mcast.heat.data.UpdateResponse
import com.mcast.heat.data.repository.IServiceRepository
import com.mcast.heat.util.getAndroidId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val serviceRepository: IServiceRepository,
) : BaseViewModel(application) {
    val updateInfo = MutableLiveData<UpdateResponse>()

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
}
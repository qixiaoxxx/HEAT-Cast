package com.mcast.heat.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.analytics.FirebaseAnalytics
import com.mcast.heat.BaseDataBindingActivity
import com.mcast.heat.BuildConfig
import com.mcast.heat.data.config.HeaderConfig
import com.mcast.heat.databinding.ActivityMainBinding
import com.mcast.heat.manager.Download
import com.mcast.heat.manager.Progress
import com.mcast.heat.ui.popwindow.PopWindowManager
import com.mcast.heat.util.UpdateUtils
import com.mcast.heat.util.WifiHelper
import com.mcast.heat.util.cleanupOldApks
import com.mcast.heat.util.getAndroidId
import com.mcast.heat.util.getCurrentFormattedTime
import com.mcast.heat.util.getDeviceName
import com.mcast.heat.util.getInt
import com.mcast.heat.util.getManufactureModel
import com.mcast.heat.util.installApk
import com.mcast.heat.util.logFirebaseEvent
import com.waxrain.airplaydmr.WaxPlayService
import com.waxrain.airplaydmr_SDK.R
import com.waxrain.droidsender.delegate.Global
import com.waxrain.ui.WaxPlayer
import com.waxrain.utils.Config
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : BaseDataBindingActivity<ActivityMainBinding>() {
    private val mainViewModel by viewModels<MainViewModel>()
    private val wifiHelper: WifiHelper by lazy {
        WifiHelper(this)
    }
    private var lastBackPressedTime: Long = 0
    private val popupWindowManager by lazy {
        PopWindowManager(this)
    }

    // 用于请求“精确位置”权限的 Launcher
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startWifiListener()
            } else {
                binding.tvWifiName.text = "No location permission"
            }
            checkAndInstallApkIfNeeded()
        }

    // 用于从“管理未知应用来源”设置页返回后的 Launcher
    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    requestRemainingPermissions()
                } else {
                    requestRemainingPermissions()
                }
            }
        }

    // 定义一个广播接收器
    private val castStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WaxPlayer.ACTION_CAST_START -> {
//                    Log.d("MainActivity", "接收到投屏开始的广播")
                    // 上报投屏开始事件
                    logFirebaseEvent(
                        this@MainActivity,
                        "cast_start", // 事件名称
                        "start_time" to getCurrentFormattedTime()
                    )
                }

                WaxPlayer.ACTION_CAST_STOP -> {
//                    Log.d("MainActivity", "接收到投屏结束的广播")
                    // 上报投屏结束事件
                    logFirebaseEvent(
                        this@MainActivity,
                        "cast_stop", // 事件名称
                        "stop_time" to getCurrentFormattedTime()
                    )
                }
            }
        }
    }


    override fun getLayoutId(): Int = com.mcast.heat.R.layout.activity_main


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 确保在新版本首次启动时，旧的安装包被删除
        cleanupOldApks(this)

        // 使用新的统一入口方法来请求权限
        requestRequiredPermissions()

        // 初始化请求头
        HeaderConfig.init(application)

        // firebase 事件上报
        logFirebaseEvent(
            this@MainActivity,
            "Android_id",
            FirebaseAnalytics.Param.ACHIEVEMENT_ID to getAndroidId(this@MainActivity),
            "resolution" to "${HeaderConfig.header_width_pixels_value}*${HeaderConfig.header_height_pixels_value}",
            "manufacture_model" to getManufactureModel(),
            "start_app" to getCurrentFormattedTime(),
        )

        initSdk()
        startSdk()

        // 注册广播接收器
        registerCastReceiver()

        //  获取并显示设备名称
        binding.tvDeviceName.text =
            getString(com.mcast.heat.R.string.Device_Name, WaxPlayService._config.nickName)
        binding.tvMirroringName.text = WaxPlayService._config.nickName
        binding.tvSelectDeviceName.text = WaxPlayService._config.nickName

        // 设置版本号
        binding.tvVersionName.text =
            getString(com.mcast.heat.R.string.Version_Name, BuildConfig.VERSION_NAME)

        collectWifiNameUpdates()

        // 初始化检查新版本下载
        initUpdate()
    }


    fun initSdk() {

        Global.RES_app_icon = com.mcast.heat.R.drawable.tiffany_1024
        Global.RES_service_notify_info = getString(R.string.service_notify_info)
        Global.RES_STRING_service_confliction = R.string.service_confliction
        Global.RES_STRING_toast_airplay_drm = R.string.waxplayer_toast_airplay_drm
        Global.RES_STRING_toast_airplay_drm2 = R.string.waxplayer_toast_airplay_drm2
        Global.RES_STRING_service_notify_pincode = R.string.service_notify_pincode
        Global.RES_STRING_set_hidden_setting_enabled = R.string.set_hidden_setting_enabled
        Global.RES_STRING_set_hidden_setting_enabled2 = R.string.set_hidden_setting_enabled2
        Global.RES_app_name = getString(com.mcast.heat.R.string.app_name)
        Global.RES_LAYOUT_toast_hws = R.layout.waxplayer_toast_hws
        Global.RES_DRAWABLE_filetype_generic = R.drawable.filetype_generic
        Global.RES_DRAWABLE_filetype_video = R.drawable.filetype_video
        Global.RES_DRAWABLE_filetype_music = R.drawable.filetype_music
        Global.RES_DRAWABLE_filetype_image = R.drawable.filetype_image
        Global.RES_DRAWABLE_filetype_dir = R.drawable.filetype_dir
        Global.RES_DRAWABLE_filetype_sysgeneric = R.drawable.filetype_sysgeneric


        //		Global.RES_DRAWABLE_filetype_sdcard = R.drawable.filetype_sdcard;
//		Global.RES_DRAWABLE_albumart_unknown = R.drawable.albumart_unknown;
        Global.RES_DRAWABLE_filetype_dms = R.drawable.filetype_dms
        Global.RES_DRAWABLE_filetype_smbet = R.drawable.filetype_smbet
        Global.RES_DRAWABLE_filetype_smb = R.drawable.filetype_smb
        Global.RES_LAYOUT_explorer_item = R.layout.explorer_item
        Global.RES_ID_explorer_resIcon = R.id.explorer_resIcon
        Global.RES_ID_explorer_resName = R.id.explorer_resName
        Global.RES_ID_explorer_resCount = R.id.explorer_resCount
        Global.RES_ID_explorer_resMeta = R.id.explorer_resMeta
        Global.RES_ID_explorer_resTime = R.id.explorer_resTime


        //		Global.RES_STRING_all_videos = R.string.all_videos;
//		Global.RES_STRING_all_audios = R.string.all_audios;
//		Global.RES_STRING_all_images = R.string.all_images;
//		Global.RES_STRING_all_docs = R.string.all_docs;
        Global.RES_layout_dialog_alert = R.layout.dialog_alert
        Global.RES_style_WaxDialog = R.style.WaxDialog
        Global.RES_style_About_dialog = R.style.About_dialog
        Global.RES_id_adg_root_view = R.id.adg_root_view
        Global.RES_id_adg_title_text = R.id.adg_title_text
        Global.RES_id_adg_message = R.id.adg_message
        Global.RES_id_adg_messageP = R.id.adg_messageP
        Global.RES_id_adg_confirm_btn = R.id.adg_confirm_btn
        Global.RES_id_adg_cancel_btn = R.id.adg_cancel_btn
        Global.RES_id_adg_left_padding = R.id.adg_left_padding
        Global.RES_id_adg_right_padding = R.id.adg_right_padding
        Global.RES_id_adg_bgview = R.id.adg_bgview
        Global.RES_string_prompt_dlg_play_yes = R.string.waxplayer_prompt_dlg_play_yes
        Global.RES_string_prompt_dlg_play_no = R.string.waxplayer_prompt_dlg_play_no
        Global.RES_dialog_doodle_view = R.layout.dialog_doodle_view
        Global.RES_doodle_view_container = R.id.doodle_view_container
        Global.RES_drawmain_menu = R.layout.drawmain_menu
        Global.RES_drawmenu_layout = R.id.drawmenu_layout
        Global.RES_drawbtn_draw_menu = R.id.drawbtn_draw_menu
        Global.RES_drawbtn_control_menu = R.id.drawbtn_control_menu
        Global.RES_drawbtn_keyMenu_menu = R.id.drawbtn_keyMenu_menu
        Global.RES_drawbtn_keyHome_menu = R.id.drawbtn_keyHome_menu
        Global.RES_drawbtn_keyBack_menu = R.id.drawbtn_keyBack_menu
        Global.RES_drawimg_draw_menu = R.id.drawimg_draw_menu
        Global.RES_drawimg_control_menu = R.id.drawimg_control_menu
        Global.RES_drawimg_keyMenu_menu = R.id.drawimg_keyMenu_menu
        Global.RES_drawimg_keyHome_menu = R.id.drawimg_keyHome_menu
        Global.RES_drawimg_keyBack_menu = R.id.drawimg_keyBack_menu
        Global.RES_drawable_drawremotemenu = R.drawable.drawremotemenu
        Global.RES_drawable_drawdrawmenu = R.drawable.drawdrawmenu
        Global.RES_drawable_drawclose = R.drawable.drawclose
        Global.RES_drawable_drawpen = R.drawable.drawpen
        Global.RES_drawable_drawrect = R.drawable.drawrect
        Global.RES_drawable_drawline = R.drawable.drawline
        Global.RES_drawable_drawback = R.drawable.drawback
        Global.RES_drawable_drawclear = R.drawable.drawclear
        Global.RES_drawable_drawshot = R.drawable.drawshot
        Global.RES_DRAWABLE_drawcursor = R.drawable.drawcursor
        Global.RES_drawimg_draw_menu_item = R.id.drawimg_draw_menu_item
        Global.RES_drawremote_layout = R.layout.drawremote_layout
        Global.RES_dimen_remotecontrol_stoke_width = R.dimen.remotecontrol_stoke_width
        Global.RES_dimen_remotecontrol_touch_indicator_radius2 =
            R.dimen.remotecontrol_touch_indicator_radius2
        Global.RES_dimen_remotecontrol_key_pannel_font2 = R.dimen.remotecontrol_key_pannel_font2
        Global.RES_remotecontrol_click_container2 = R.id.remotecontrol_click_container2
        Global.RES_drawremote_keyMenu = R.id.drawremote_keyMenu
        Global.RES_drawremote_keyHome = R.id.drawremote_keyHome
        Global.RES_drawremote_keyBack = R.id.drawremote_keyBack
        Global.RES_drawitem_draw_menu = R.layout.drawitem_draw_menu
        Global.RES_drawbtn_draw_menu_item = R.id.drawbtn_draw_menu_item
        Global.RES_drawitem_draw_bg = R.id.drawitem_draw_bg
        Global.RES_drawable_drawframe_bg = R.drawable.drawframe_bg
        Global.RES_drawitem_choice_color = R.layout.drawitem_choice_color
        Global.RES_drawitem_color = R.id.drawitem_color
        Global.RES_drawitem_color_bg = R.id.drawitem_color_bg
        Global.RES_drawitem_color_layout = R.id.drawitem_color_layout
        Global.RES_drawitem_choice_size = R.layout.drawitem_choice_size
        Global.RES_drawitem_size = R.id.drawitem_size
        Global.RES_drawitem_size_bg = R.id.drawitem_size_bg
        Global.RES_drawitem_size_layout = R.id.drawitem_size_layout
        Global.RES_drawable_drawsize_btn_bg = R.drawable.drawsize_btn_bg
        Global.RES_drawable_switch_off = R.drawable.switch_off
        Global.RES_drawable_switch_on = R.drawable.switch_on

        if (WaxPlayService._config == null) WaxPlayService._config = Config(this)

        //自定义设备连接名称
        WaxPlayService._config.nickName = getDeviceName()
        WaxPlayService._config.nickName_RMPF = 1

        if (Config.AIRMIRR_RESOLUTION != 0) WaxPlayService.amr = Config.AIRMIRR_RESOLUTION
        WaxPlayService.configScreenResolution(this)
        Config.HWS_ENABLED = 0
        Config.HWSMIRROR = 0
        WaxPlayService.settingActivityCls = MainActivity::class.java
        WaxPlayService.setting2ActivityCls = MainActivity::class.java
        WaxPlayService.playerActivityCls = WaxPlayer::class.java

        logFirebaseEvent(
            this@MainActivity,
            "init_sdk", // 事件名称
            "init_time" to getCurrentFormattedTime()
        )
    }

    fun startSdk() {
        val mIntent = Intent()
        mIntent.action = "com.waxrain.airplaydmr.WaxPlayService"
        mIntent.setPackage(packageName)
        try {
            if (Build.VERSION.SDK_INT >= 26 && applicationInfo.targetSdkVersion >= 26)
                startForegroundService(
                    mIntent
                )
            else startService(mIntent)
        } catch (ex: Exception) {
        }
        Log.i("_ADJNI_", "DEMO onCreate()")
        logFirebaseEvent(
            this@MainActivity,
            "start_sdk", // 事件名称
            "start_time" to getCurrentFormattedTime()
        )
//        Toast.makeText(this, "CAST start", Toast.LENGTH_LONG)
    }

    /**
     * 统一的权限请求入口，首先处理安装权限。
     */
    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:$packageName".toUri()
                }
                installPermissionLauncher.launch(intent)
                return
            }
        }
        requestRemainingPermissions()
    }

    /**
     * 请求剩余的权限（如位置权限），并执行依赖于这些权限的操作。
     */
    private fun requestRemainingPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startWifiListener()
            checkAndInstallApkIfNeeded()
        }
    }

    private fun checkAndInstallApkIfNeeded() {
        val pendingApkPath = UpdateUtils.getPendingApkPath(this)
        if (!pendingApkPath.isNullOrEmpty()) {
            val apkFile = File(pendingApkPath)
            if (apkFile.exists() && isApkFileValid(apkFile)) {
                installApk(apkFile)
            } else {
                Log.e("Install", "APK file is invalid or does not exist. Deleting record.")
                apkFile.delete()
                UpdateUtils.setPendingApkPath(this, "")
            }
        }
    }


    private fun isApkFileValid(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return false
        }
        try {
            val pm = packageManager
            val pInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            return pInfo != null
        } catch (e: Exception) {
            Log.e("ApkValidation", "Failed to parse APK file: ${apkFile.absolutePath}", e)
            return false
        }
    }

    /**
     *  检查服务器版本，如果需要则在后台下载 APK。
     */
    private fun initUpdate() {
        mainViewModel.updateInfo.observe(this) { updateInfo ->
            val latestVersionCode = updateInfo.release?.versionCode ?: 0
            val isForcedUpdate = (updateInfo.incompatibleVersion ?: 0) >= BuildConfig.VERSION_CODE

            lifecycleScope.launch {
                val ignoredVersionCode = getInt(this@MainActivity, "versionCode") ?: 0
                if (latestVersionCode > BuildConfig.VERSION_CODE && (isForcedUpdate || latestVersionCode > ignoredVersionCode)) {
                    if (!Download.isDownloading) {
                        val urlToDownload = updateInfo.release?.url
                        if (!urlToDownload.isNullOrEmpty()) {
                            startBackgroundDownload(urlToDownload)
                        }
                    }
                }
            }
        }
    }

    /**
     * 在后台下载新的 APK 文件。
     * 下载完成后，将路径保存到 SharedPreferences。
     * (这个方法直接使用 Download.kt)
     */
    private fun startBackgroundDownload(url: String) {
        lifecycleScope.launch {
            val filename = url.substringAfterLast("/")
            if (filename.isBlank()) {
                Log.e("Download", "Could not determine filename from URL: $url")
                return@launch
            }

            val targetApkFile = File(
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                filename
            )

            Log.d(
                "DownloadSetup",
                "Target APK location for download: ${targetApkFile.absolutePath}"
            )

            Download.downloadFile(url, this@MainActivity, targetApkFile)
                .collect { progress: Progress ->
                    if (progress.isDone) {
                        if (progress.filePath.isNotEmpty()) {
                            // 下载成功！保存路径
                            UpdateUtils.setPendingApkPath(this@MainActivity, progress.filePath)
                            Log.d("Download", "APK downloaded and path saved: ${progress.filePath}")
                        } else {
                            Log.e("Download", "Download finished but APK path is missing.")
                        }
                    } else if (progress.downloadError) {
                        Log.e(
                            "Download",
                            "Failed to download new version. Retry needed: ${progress.needRetry}"
                        )
                    } else {
                        Log.d(
                            "Download",
                            "Downloading... ${progress.progress}% (${progress.bytesRead} / ${progress.totalSize})"
                        )
                    }
                    progress.recycle()
                }
        }
    }

    private fun startWifiListener() {
        lifecycleScope.launch {
            wifiHelper.startListeningWifiChanges()
        }
    }

    @SuppressLint("InvalidAnalyticsName")
    private fun collectWifiNameUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wifiHelper.wifiName.collect { wifiName ->
                    binding.tvWifiName.text = wifiName ?: "Wi-Fi Disconnected"
                }
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressedTime < 2000) {
                popupWindowManager.showExitPopUpWindow {
                    lifecycleScope.launch {
                        finishAffinity()
                        exitProcess(0)
                    }
                }
            } else {
                lastBackPressedTime = currentTime
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // 注册广播接收器的方法
    private fun registerCastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(WaxPlayer.ACTION_CAST_START)
            addAction(WaxPlayer.ACTION_CAST_STOP)
        }
        registerReceiver(castStateReceiver, intentFilter)
    }


    override fun onDestroy() {
        if (!Global.serviceExiting) Global.serviceExiting = true
        try {
            this.onBackPressed()
            this.finish()
        } catch (_: java.lang.Exception) {
        }
        popupWindowManager.hideAllPopWindow()
        // 注销广播接收器，防止内存泄漏
        unregisterReceiver(castStateReceiver)
        logFirebaseEvent(this, "on_destroy", "on_destroy" to "on_destroy")
        super.onDestroy()
    }
}

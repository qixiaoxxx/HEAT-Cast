package com.mcast.heat.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
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
import com.mcast.heat.R
import com.mcast.heat.data.config.HeaderConfig
import com.mcast.heat.databinding.ActivityMainBinding
import com.mcast.heat.manager.Download
import com.mcast.heat.ui.popwindow.PopWindowManager
import com.mcast.heat.util.WifiHelper
import com.mcast.heat.util.getAndroidId
import com.mcast.heat.util.getDeviceName
import com.mcast.heat.util.getInt
import com.mcast.heat.util.installApk
import com.mcast.heat.util.logFirebaseEvent
import com.waxrain.airplaydmr.WaxPlayService
import com.waxrain.droidsender.delegate.Global
import com.waxrain.ui.WaxPlayer
import com.waxrain.utils.Config
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : BaseDataBindingActivity<ActivityMainBinding>() {
    private val mainViewModel by viewModels<MainViewModel>()
    private val wifiHelper: WifiHelper by lazy {
        WifiHelper(this)
    }
    private var lastBackPressedTime: Long = 0
    private var pendingDownloadUrl: String? = null
    private val popupWindowManager by lazy {
        PopWindowManager(this)
    }


    override fun getLayoutId(): Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // firebase 事件上报
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, getAndroidId(this@MainActivity))
        FirebaseAnalytics.getInstance(this@MainActivity)
            .logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)

        // 初始化请求头
        HeaderConfig.init(application)

        initSdk()
        startSdk()

        //  获取并显示设备名称
        binding.tvDeviceName.text =
            getString(R.string.Projection_TV, WaxPlayService._config.nickName)
        binding.tvMirroringName.text = WaxPlayService._config.nickName
        binding.tvSelectDeviceName.text = WaxPlayService._config.nickName

        //  请求权限以获取 Wi-Fi 名称
//        askForLocationPermission()
        initWifiWithPermissionCheck()
        collectWifiNameUpdates() // 单独设置一次监听即可

        initUpdate()

    }

    private fun initUpdate() {
        var versionCode = 0
        lifecycleScope.launch {
            versionCode = getInt(this@MainActivity, "versionCode") ?: 0
        }
        mainViewModel.updateInfo.observe(this) {
            val latestVersionCode = it.release?.versionCode ?: 0
            val isForcedUpdate = (it.incompatibleVersion ?: 0) >= BuildConfig.VERSION_CODE
            if (latestVersionCode > BuildConfig.VERSION_CODE && (isForcedUpdate || latestVersionCode > versionCode)) {
                lifecycleScope.launch {
                    if (Download.isDownloading.not()) {

                        val urlToDownload = it.release?.url
                        if (urlToDownload.isNullOrEmpty()) {
                            return@launch
                        }
                        ensureInstallPermissionAndDownload(urlToDownload)

//                        popupWindowManager.showUpDatePopUpWindow(
//                            isForcedUpdate,
//                            it.release?.changeLog ?: "",
//                            {
//                                val urlToDownload = it.release?.url
//                                if (urlToDownload.isNullOrEmpty()) {
//                                    Toast.makeText(
//                                        this@MainActivity,
//                                        "下载地址无效",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                    return@showUpDatePopUpWindow
//                                }
//                                ensureInstallPermissionAndDownload(urlToDownload)
//                            },
//                            {
//                                if ((it.incompatibleVersion ?: 0) < BuildConfig.VERSION_CODE) {
//                                    lifecycleScope.launch {
//                                        saveInt(
//                                            this@MainActivity,
//                                            "versionCode",
//                                            it.release?.versionCode ?: 0
//                                        )
//                                    }
//                                }
//                            }
//                        )
                    }
                }
            }
        }
    }

    fun initSdk() {

        Global.RES_app_icon = R.drawable.tiffany_1024
        Global.RES_service_notify_info =
            getString(com.waxrain.airplaydmr_SDK.R.string.service_notify_info)
        Global.RES_STRING_service_confliction =
            com.waxrain.airplaydmr_SDK.R.string.service_confliction
        Global.RES_STRING_toast_airplay_drm =
            com.waxrain.airplaydmr_SDK.R.string.waxplayer_toast_airplay_drm
        Global.RES_STRING_toast_airplay_drm2 =
            com.waxrain.airplaydmr_SDK.R.string.waxplayer_toast_airplay_drm2
        Global.RES_STRING_service_notify_pincode =
            com.waxrain.airplaydmr_SDK.R.string.service_notify_pincode
        Global.RES_STRING_set_hidden_setting_enabled =
            com.waxrain.airplaydmr_SDK.R.string.set_hidden_setting_enabled
        Global.RES_STRING_set_hidden_setting_enabled2 =
            com.waxrain.airplaydmr_SDK.R.string.set_hidden_setting_enabled2
        Global.RES_app_name = getString(R.string.app_name)
        Global.RES_LAYOUT_toast_hws = com.waxrain.airplaydmr_SDK.R.layout.waxplayer_toast_hws
        Global.RES_DRAWABLE_filetype_generic =
            com.waxrain.airplaydmr_SDK.R.drawable.filetype_generic
        Global.RES_DRAWABLE_filetype_video = com.waxrain.airplaydmr_SDK.R.drawable.filetype_video
        Global.RES_DRAWABLE_filetype_music = com.waxrain.airplaydmr_SDK.R.drawable.filetype_music
        Global.RES_DRAWABLE_filetype_image = com.waxrain.airplaydmr_SDK.R.drawable.filetype_image
        Global.RES_DRAWABLE_filetype_dir = com.waxrain.airplaydmr_SDK.R.drawable.filetype_dir
        Global.RES_DRAWABLE_filetype_sysgeneric =
            com.waxrain.airplaydmr_SDK.R.drawable.filetype_sysgeneric


        //		Global.RES_DRAWABLE_filetype_sdcard = R.drawable.filetype_sdcard;
//		Global.RES_DRAWABLE_albumart_unknown = R.drawable.albumart_unknown;
        Global.RES_DRAWABLE_filetype_dms = com.waxrain.airplaydmr_SDK.R.drawable.filetype_dms
        Global.RES_DRAWABLE_filetype_smbet = com.waxrain.airplaydmr_SDK.R.drawable.filetype_smbet
        Global.RES_DRAWABLE_filetype_smb = com.waxrain.airplaydmr_SDK.R.drawable.filetype_smb
        Global.RES_LAYOUT_explorer_item = com.waxrain.airplaydmr_SDK.R.layout.explorer_item
        Global.RES_ID_explorer_resIcon = com.waxrain.airplaydmr_SDK.R.id.explorer_resIcon
        Global.RES_ID_explorer_resName = com.waxrain.airplaydmr_SDK.R.id.explorer_resName
        Global.RES_ID_explorer_resCount = com.waxrain.airplaydmr_SDK.R.id.explorer_resCount
        Global.RES_ID_explorer_resMeta = com.waxrain.airplaydmr_SDK.R.id.explorer_resMeta
        Global.RES_ID_explorer_resTime = com.waxrain.airplaydmr_SDK.R.id.explorer_resTime


        //		Global.RES_STRING_all_videos = R.string.all_videos;
//		Global.RES_STRING_all_audios = R.string.all_audios;
//		Global.RES_STRING_all_images = R.string.all_images;
//		Global.RES_STRING_all_docs = R.string.all_docs;
        Global.RES_layout_dialog_alert = R.layout.dialog_exit
        Global.RES_style_WaxDialog = com.waxrain.airplaydmr_SDK.R.style.WaxDialog
        Global.RES_style_About_dialog = com.waxrain.airplaydmr_SDK.R.style.About_dialog
        Global.RES_id_adg_root_view = com.waxrain.airplaydmr_SDK.R.id.adg_root_view
        Global.RES_id_adg_title_text = com.waxrain.airplaydmr_SDK.R.id.adg_title_text
        Global.RES_id_adg_message = com.waxrain.airplaydmr_SDK.R.id.adg_message
        Global.RES_id_adg_messageP = com.waxrain.airplaydmr_SDK.R.id.adg_messageP
        Global.RES_id_adg_confirm_btn = R.id.bt_Confirm
        Global.RES_id_adg_cancel_btn = R.id.bt_Cancel
        Global.RES_id_adg_left_padding = com.waxrain.airplaydmr_SDK.R.id.adg_left_padding
        Global.RES_id_adg_right_padding = com.waxrain.airplaydmr_SDK.R.id.adg_right_padding
        Global.RES_id_adg_bgview = com.waxrain.airplaydmr_SDK.R.id.adg_bgview
        Global.RES_string_prompt_dlg_play_yes =
            com.waxrain.airplaydmr_SDK.R.string.waxplayer_prompt_dlg_play_yes
        Global.RES_string_prompt_dlg_play_no =
            com.waxrain.airplaydmr_SDK.R.string.waxplayer_prompt_dlg_play_no
        Global.RES_dialog_doodle_view = com.waxrain.airplaydmr_SDK.R.layout.dialog_doodle_view
        Global.RES_doodle_view_container = com.waxrain.airplaydmr_SDK.R.id.doodle_view_container
        Global.RES_drawmain_menu = com.waxrain.airplaydmr_SDK.R.layout.drawmain_menu
        Global.RES_drawmenu_layout = com.waxrain.airplaydmr_SDK.R.id.drawmenu_layout
        Global.RES_drawbtn_draw_menu = com.waxrain.airplaydmr_SDK.R.id.drawbtn_draw_menu
        Global.RES_drawbtn_control_menu = com.waxrain.airplaydmr_SDK.R.id.drawbtn_control_menu
        Global.RES_drawbtn_keyMenu_menu = com.waxrain.airplaydmr_SDK.R.id.drawbtn_keyMenu_menu
        Global.RES_drawbtn_keyHome_menu = com.waxrain.airplaydmr_SDK.R.id.drawbtn_keyHome_menu
        Global.RES_drawbtn_keyBack_menu = com.waxrain.airplaydmr_SDK.R.id.drawbtn_keyBack_menu
        Global.RES_drawimg_draw_menu = com.waxrain.airplaydmr_SDK.R.id.drawimg_draw_menu
        Global.RES_drawimg_control_menu = com.waxrain.airplaydmr_SDK.R.id.drawimg_control_menu
        Global.RES_drawimg_keyMenu_menu = com.waxrain.airplaydmr_SDK.R.id.drawimg_keyMenu_menu
        Global.RES_drawimg_keyHome_menu = com.waxrain.airplaydmr_SDK.R.id.drawimg_keyHome_menu
        Global.RES_drawimg_keyBack_menu = com.waxrain.airplaydmr_SDK.R.id.drawimg_keyBack_menu
        Global.RES_drawable_drawremotemenu = com.waxrain.airplaydmr_SDK.R.drawable.drawremotemenu
        Global.RES_drawable_drawdrawmenu = com.waxrain.airplaydmr_SDK.R.drawable.drawdrawmenu
        Global.RES_drawable_drawclose = com.waxrain.airplaydmr_SDK.R.drawable.drawclose
        Global.RES_drawable_drawpen = com.waxrain.airplaydmr_SDK.R.drawable.drawpen
        Global.RES_drawable_drawrect = com.waxrain.airplaydmr_SDK.R.drawable.drawrect
        Global.RES_drawable_drawline = com.waxrain.airplaydmr_SDK.R.drawable.drawline
        Global.RES_drawable_drawback = com.waxrain.airplaydmr_SDK.R.drawable.drawback
        Global.RES_drawable_drawclear = com.waxrain.airplaydmr_SDK.R.drawable.drawclear
        Global.RES_drawable_drawshot = com.waxrain.airplaydmr_SDK.R.drawable.drawshot
        Global.RES_DRAWABLE_drawcursor = com.waxrain.airplaydmr_SDK.R.drawable.drawcursor
        Global.RES_drawimg_draw_menu_item = com.waxrain.airplaydmr_SDK.R.id.drawimg_draw_menu_item
        Global.RES_drawremote_layout = com.waxrain.airplaydmr_SDK.R.layout.drawremote_layout
        Global.RES_dimen_remotecontrol_stoke_width =
            com.waxrain.airplaydmr_SDK.R.dimen.remotecontrol_stoke_width
        Global.RES_dimen_remotecontrol_touch_indicator_radius2 =
            com.waxrain.airplaydmr_SDK.R.dimen.remotecontrol_touch_indicator_radius2
        Global.RES_dimen_remotecontrol_key_pannel_font2 =
            com.waxrain.airplaydmr_SDK.R.dimen.remotecontrol_key_pannel_font2
        Global.RES_remotecontrol_click_container2 =
            com.waxrain.airplaydmr_SDK.R.id.remotecontrol_click_container2
        Global.RES_drawremote_keyMenu = com.waxrain.airplaydmr_SDK.R.id.drawremote_keyMenu
        Global.RES_drawremote_keyHome = com.waxrain.airplaydmr_SDK.R.id.drawremote_keyHome
        Global.RES_drawremote_keyBack = com.waxrain.airplaydmr_SDK.R.id.drawremote_keyBack
        Global.RES_drawitem_draw_menu = com.waxrain.airplaydmr_SDK.R.layout.drawitem_draw_menu
        Global.RES_drawbtn_draw_menu_item = com.waxrain.airplaydmr_SDK.R.id.drawbtn_draw_menu_item
        Global.RES_drawitem_draw_bg = com.waxrain.airplaydmr_SDK.R.id.drawitem_draw_bg
        Global.RES_drawable_drawframe_bg = com.waxrain.airplaydmr_SDK.R.drawable.drawframe_bg
        Global.RES_drawitem_choice_color = com.waxrain.airplaydmr_SDK.R.layout.drawitem_choice_color
        Global.RES_drawitem_color = com.waxrain.airplaydmr_SDK.R.id.drawitem_color
        Global.RES_drawitem_color_bg = com.waxrain.airplaydmr_SDK.R.id.drawitem_color_bg
        Global.RES_drawitem_color_layout = com.waxrain.airplaydmr_SDK.R.id.drawitem_color_layout
        Global.RES_drawitem_choice_size = com.waxrain.airplaydmr_SDK.R.layout.drawitem_choice_size
        Global.RES_drawitem_size = com.waxrain.airplaydmr_SDK.R.id.drawitem_size
        Global.RES_drawitem_size_bg = com.waxrain.airplaydmr_SDK.R.id.drawitem_size_bg
        Global.RES_drawitem_size_layout = com.waxrain.airplaydmr_SDK.R.id.drawitem_size_layout
        Global.RES_drawable_drawsize_btn_bg = com.waxrain.airplaydmr_SDK.R.drawable.drawsize_btn_bg
        Global.RES_drawable_switch_off = com.waxrain.airplaydmr_SDK.R.drawable.switch_off
        Global.RES_drawable_switch_on = com.waxrain.airplaydmr_SDK.R.drawable.switch_on

        if (WaxPlayService._config == null) WaxPlayService._config = Config(this)
        WaxPlayService._config.nickName = getDeviceName(this)
        WaxPlayService._config.nickName_RMPF = 1
        if (Config.AIRMIRR_RESOLUTION != 0) WaxPlayService.amr = Config.AIRMIRR_RESOLUTION
        WaxPlayService.configScreenResolution(this)
        Config.HWS_ENABLED = 0
        Config.HWSMIRROR = 0
        WaxPlayService.settingActivityCls = MainActivity::class.java
        WaxPlayService.setting2ActivityCls = MainActivity::class.java
        WaxPlayService.playerActivityCls = WaxPlayer::class.java
    }

    fun startSdk() {
        val mIntent = Intent()
        mIntent.setAction("com.waxrain.airplaydmr.WaxPlayService")
        mIntent.setPackage(packageName)
        try {
            if (Build.VERSION.SDK_INT >= 26 && applicationInfo.targetSdkVersion >= 26) startForegroundService(
                mIntent
            )
            else startService(mIntent)
        } catch (_: Exception) {
        }
        logFirebaseEvent(this, "CAST start", "startSdk")
//        Toast.makeText(this, "CAST start", Toast.LENGTH_LONG).show()
    }

    @SuppressLint("SetTextI18n")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 权限被授予后，开始监听Wi-Fi变化
                startWifiListener()
            } else {
                binding.tvWifiName.text = "无法获取Wi-Fi，需要位置权限"
                Toast.makeText(this, "未授予位置权限，无法获取Wi-Fi名称", Toast.LENGTH_SHORT).show()
            }
        }

    // 4. 创建一个清晰的入口方法
    private fun initWifiWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 已有权限，直接开始监听
            startWifiListener()
        } else {
            // 没有权限，发起请求
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // 5. 启动监听
    private fun startWifiListener() {
        lifecycleScope.launch {
            wifiHelper.startListeningWifiChanges()
        }
    }

    // 6. 设置UI更新的收集器
    private fun collectWifiNameUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wifiHelper.wifiName.collect { wifiName ->
                    binding.tvWifiName.text = wifiName ?: "Wi-Fi未连接"
                }
            }
        }
    }


//    fun askForLocationPermission() {
//        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//        wifiHelper = WifiHelper(this)
//        collectWifiNameUpdates()
//        checkAndRequestPermission()
//    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startWifiHelper()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun startWifiHelper() {
        lifecycleScope.launch {
            wifiHelper.startListeningWifiChanges()
        }
    }

//    private fun collectWifiNameUpdates() {
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                wifiHelper.wifiName.collect { wifiName ->
//                    binding.tvWifiName.text = wifiName ?: "Permission Denied"
//                }
//            }
//        }
//    }

    @SuppressLint("SetTextI18n")
    fun setWifiName() {
        val wifiHelper = WifiHelper(this)
        lifecycleScope.launch {
            wifiHelper.startListeningWifiChanges()
            wifiHelper.wifiName.collect { ssid ->
                if (ssid != null) {
                    binding.tvWifiName.text = ssid
                } else {
                    binding.tvWifiName.text = "Wi-Fi Disconnected"
                }
            }
        }
    }

    /**
     * 下载安装
     */
    private fun startDownload(url: String) {
//        popupWindowManager.showUpDateProgressPopUpWindow(
//            0, "0.0K/s", "0.0M", "0.0M"
//        )
        lifecycleScope.launch {
            Download.downloadFile(url, this@MainActivity).collect {
//                popupWindowManager.upDateProgressPopUpWindow(
//                    it.progress, it.perSecondBytes, it.bytesRead, it.totalSize
//                )
                if (it.isDone) {
//                    popupWindowManager.hideUpDateProgressPopUpWindow()
                    installApk(it.filePath)
                }
            }
            logFirebaseEvent(this@MainActivity, "Download", "startDownload")
        }
    }

    private fun ensureInstallPermissionAndDownload(url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val haveInstallPermission = packageManager.canRequestPackageInstalls()
            if (haveInstallPermission) {
                startDownload(url)
            } else {
                pendingDownloadUrl = url
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:$packageName".toUri()
                )
                installPermissionLauncher.launch(intent)
            }
        } else {
            startDownload(url)
        }
    }

    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    pendingDownloadUrl?.let { url ->
                        startDownload(url)
                        pendingDownloadUrl = null
                    }
                } else {
                    logFirebaseEvent(this, "Download Permissions", "未授予安装权限，无法完成更新")
//                    Toast.makeText(this, "未授予安装权限，无法完成更新", Toast.LENGTH_SHORT).show()
                }
            }
        }

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


    override fun onDestroy() {
        if (!Global.serviceExiting) Global.serviceExiting = true
        try {
            this.onBackPressed()
            this.finish()
        } catch (_: java.lang.Exception) {
        }
        popupWindowManager.hideAllPopWindow()
        super.onDestroy()
    }
}

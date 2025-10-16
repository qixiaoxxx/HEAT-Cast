package com.mcast.heat.ui.popwindow

import android.app.Activity

class PopWindowManager(private val context: Activity) {

    private val popWindowList = ArrayList<BasePopWindow>()

    private val exitPopUpWindow by lazy {
        val window = ExitPopUpWindow(context)
        popWindowList.add(window)
        window
    }

    private val upDatePopUpWindow by lazy {
        val window = UpDataPopUpWindow(context)
        popWindowList.add(window)
        window
    }

    private val upDateProgressPopUpWindow by lazy {
        val window = UpDateProgressPopUpWindow(context)
        popWindowList.add(window)
        window
    }

    fun showExitPopUpWindow(exit: () -> Unit) {
        exitPopUpWindow.exit(exit)
        exitPopUpWindow.stay()
        exitPopUpWindow.show()
    }


    fun showUpDatePopUpWindow(
        updateUI: Boolean,
        updateLog: String,
        install: () -> Unit,
        skip: () -> Unit
    ) {
        upDatePopUpWindow.setUI(updateUI)
        upDatePopUpWindow.setUpdateLog(updateLog)
        upDatePopUpWindow.install(install)
        upDatePopUpWindow.skip(skip)
        upDatePopUpWindow.ignore()
        upDatePopUpWindow.show()
    }


    fun showUpDateProgressPopUpWindow(
        progress: Int,
        second: String,
        downloaded: String,
        total: String
    ) {
        upDateProgressPopUpWindow.setUpdateProgress(progress, second, downloaded, total)
        upDateProgressPopUpWindow.show()
    }

    fun upDateProgressPopUpWindow(
        progress: Int,
        second: String,
        downloaded: String,
        total: String
    ) {
        upDateProgressPopUpWindow.setUpdateProgress(progress, second, downloaded, total)
    }


    fun hideUpDateProgressPopUpWindow() {
        upDateProgressPopUpWindow.dismiss()
    }

    fun hideAllPopWindow() {
        popWindowList.forEach {
            it.dismiss()
        }
    }
}

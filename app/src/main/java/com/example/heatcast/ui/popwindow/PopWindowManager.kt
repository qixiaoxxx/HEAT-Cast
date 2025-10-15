package com.example.heatcast.ui.popwindow

import android.app.Activity

class PopWindowManager(private val context: Activity) {

    private val popWindowList = ArrayList<BasePopWindow>()

    private val exitPopUpWindow by lazy {
        val window = ExitPopUpWindow(context)
        popWindowList.add(window)
        window
    }

    fun showExitPopUpWindow(exit: () -> Unit) {
        exitPopUpWindow.exit(exit)
        exitPopUpWindow.stay()
        exitPopUpWindow.show()
    }


    fun hideAllPopWindow() {
        popWindowList.forEach {
            it.dismiss()
        }
    }
}

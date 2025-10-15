package com.mcast.heat.ui.popwindow

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import com.mcast.heat.R


abstract class BasePopWindow(
    private val context: Activity,
    private val width: Int,
    private val height: Int,
) {
    lateinit var window: TipPopupWindow
    val scaleInAnimation = AnimationUtils.loadAnimation(context, R.anim.custom_scale_in)
    val scaleOutAnimation = AnimationUtils.loadAnimation(context, R.anim.custom_scale_out)

    fun createWindow(view: View) {
        window = TipPopupWindow(view, width, height, true)
        window.isTouchable = false
        window.isOutsideTouchable = false
        window.isFocusable = true
    }


    open fun show() {
        window.showAtLocation(context.window.decorView, Gravity.CENTER, 0, 0)
        window.contentView.startAnimation(scaleInAnimation)
    }

    open fun dismiss() {
        window.dismiss()
        window.contentView.startAnimation(scaleOutAnimation)
    }

    fun isShowing(): Boolean = window.isShowing
}
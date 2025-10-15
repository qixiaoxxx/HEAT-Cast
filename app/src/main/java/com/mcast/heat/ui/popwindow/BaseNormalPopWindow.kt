package com.mcast.heat.ui.popwindow

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


abstract class BaseNormalPopWindow(
    context: Activity,
    layoutId: Int,
    with: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    heigh: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
) : BasePopWindow(context, with, heigh) {
    val view: View = LayoutInflater.from(context).inflate(layoutId, null)

    init {
        createWindow(view)
    }
}
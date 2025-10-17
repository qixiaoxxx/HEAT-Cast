package com.mcast.heat.ui.popwindow

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.mcast.heat.R


class UpDateProgressPopUpWindow(val context: Activity) : BaseNormalPopWindow(
    context,
    R.layout.pop_update_progress,
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
) {

    init {
        window.isFocusable = false
        window.isOutsideTouchable = true
        window.isTouchable = true
    }

    @SuppressLint("SetTextI18n")
    fun setUpdateProgress(progress: Int, second: String, downloaded: String, total: String) {
        view.findViewById<ProgressBar>(R.id.progress_bar).progress = progress
        view.findViewById<TextView>(R.id.tv_progress_second).text = second
        view.findViewById<TextView>(R.id.tv_progress_downloaded).text = "$downloaded/$total"
    }

    override fun show() {
        window.showAtLocation(context.window.decorView, Gravity.BOTTOM or Gravity.END, 0, 0)
        window.contentView.startAnimation(scaleInAnimation)
    }
}

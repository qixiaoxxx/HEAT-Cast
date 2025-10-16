package com.mcast.heat.ui.popwindow

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mcast.heat.BuildConfig
import com.mcast.heat.R


class UpDataPopUpWindow(context: Activity) : BaseNormalPopWindow(
    context,
    R.layout.pop_update,
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT
) {
    fun setUpdateLog(updateLog: String) {
        view.findViewById<TextView>(R.id.tv_update_log).text = updateLog
    }

    fun install(install: () -> Unit) {
        view.findViewById<View>(R.id.Install_now).setOnClickListener {
            install()
            dismiss()
        }
    }

    fun ignore() {
        view.findViewById<View>(R.id.Ignore_updates).setOnClickListener {
            dismiss()
        }
    }

    fun skip(skip: () -> Unit) {
        view.findViewById<View>(R.id.skip).setOnClickListener {
            skip()
            dismiss()
        }
    }

    override fun show() {
        super.show()
        view.findViewById<View>(R.id.Install_now).requestFocus()
        view.findViewById<TextView>(R.id.tv_version_code).text = BuildConfig.VERSION_NAME
    }
}
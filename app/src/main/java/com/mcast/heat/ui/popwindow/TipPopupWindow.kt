package com.mcast.heat.ui.popwindow

import android.view.View
import android.widget.PopupWindow


class TipPopupWindow(contextView: View, width: Int, height: Int, focusable: Boolean) :
    PopupWindow(contextView, width, height, focusable) {
    private var mOnBackPressListener: View.OnClickListener? = View.OnClickListener { }

    override fun dismiss() {
        val stackTrace: Array<StackTraceElement> = Exception().stackTrace
        if (stackTrace.size >= 2 && "dispatchKeyEvent" == stackTrace[1].methodName) {
            if (mOnBackPressListener != null) {
                mOnBackPressListener?.onClick(null)
            } else {
                super.dismiss()
            }
        } else {
            super.dismiss()
        }
    }
}
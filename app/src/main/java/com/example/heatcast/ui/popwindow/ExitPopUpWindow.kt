package com.example.heatcast.ui.popwindow

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.example.heatcast.R


class ExitPopUpWindow(context: Activity) :
    BaseNormalPopWindow(
        context,
        R.layout.dialog_exit,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    ) {

    fun stay() {
        view.findViewById<View>(R.id.bt_Cancel).setOnClickListener {
            dismiss()
        }
    }

    fun exit(exit: () -> Unit) {
        view.findViewById<View>(R.id.bt_Confirm).setOnClickListener {
            exit()
            dismiss()
        }
    }

    override fun show() {
        super.show()
        view.findViewById<View>(R.id.bt_Cancel).requestFocus()
    }
}
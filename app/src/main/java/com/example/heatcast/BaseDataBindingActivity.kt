package com.example.heatcast

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import kotlin.system.exitProcess

abstract class BaseDataBindingActivity<T : ViewDataBinding> : BaseActivity() {

    protected lateinit var binding: T

    protected abstract fun getLayoutId(): Int
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, getLayoutId())
        binding.lifecycleOwner = this
        //整个应用程序中保持屏幕亮起 若要指定页面保持亮起可在xml的顶级标签设置  android:keepScreenOn="true"
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //判断用户按下的键是否是 HOME 键、RECENT_APPS 键（最近应用键）或 ALL_APPS 键（所有应用键
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_RECENT_APPS || keyCode == KeyEvent.KEYCODE_ALL_APPS) {
            // 如果用户按下了上述任意一个键，强制退出整个应用程序进程。
            exitProcess(0)
        }
        return super.onKeyDown(keyCode, event)
    }

}

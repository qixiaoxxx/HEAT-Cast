package com.mcast.heat

import com.waxrain.droidsender.SenderApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CastApplication : SenderApplication() {
    override fun onCreate() {
        super.onCreate()
    }
}
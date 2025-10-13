package com.example.heatcast

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
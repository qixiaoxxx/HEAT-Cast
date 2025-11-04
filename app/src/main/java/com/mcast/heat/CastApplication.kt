package com.mcast.heat

import android.widget.Toast
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mcast.heat.data.network.NetworkMonitor
import com.waxrain.droidsender.SenderApplication
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CastApplication : SenderApplication() {
    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            networkMonitor.isOnline.collectLatest { isOnline ->
                if (isOnline.not()) {
                    Toast.makeText(applicationContext, "No_Signal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
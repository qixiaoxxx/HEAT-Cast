package com.mcast.heat.data.network

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
    fun isCurrentlyConnected(): Boolean
    fun getCurrentNetWorkType(): Int?
}
package com.mcast.heat.data.repository

import com.mcast.heat.data.UpdateRequest
import com.mcast.heat.data.UpdateResponse

interface DataSource {
    suspend fun getUpdateData(updateRequest: UpdateRequest): UpdateResponse
}
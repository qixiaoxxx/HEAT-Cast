package com.mcast.heat.data.repository

import com.mcast.heat.data.UpdateRequest
import com.mcast.heat.data.UpdateResponse
import kotlinx.coroutines.flow.Flow

interface IServiceRepository {
    fun getUpdateResponse(updateRequest: UpdateRequest): Flow<UpdateResponse>
}

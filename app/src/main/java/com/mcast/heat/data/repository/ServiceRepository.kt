package com.mcast.heat.data.repository

import com.mcast.heat.data.UpdateRequest
import com.mcast.heat.data.UpdateResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject


class ServiceRepository @Inject constructor(
    private val dataSource: DataSource,
) : IServiceRepository {
    override fun getUpdateResponse(
        updateRequest: UpdateRequest
    ): Flow<UpdateResponse> {
        return flow {
            emit(dataSource.getUpdateData(updateRequest))
        }
    }
}

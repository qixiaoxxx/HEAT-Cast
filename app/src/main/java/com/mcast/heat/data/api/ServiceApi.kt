package com.mcast.heat.data.api

import com.mcast.heat.data.UpdateRequest
import com.mcast.heat.data.UpdateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface ServiceApi {

    /**
     * string
     * example: http://example.com/app/upgrade?t={token}
     * 检查更新 API
     */
    @POST("/api/v1/update")
    suspend fun getUpdate(
        @Body updateRequest: UpdateRequest
    ): Response<UpdateResponse>

}
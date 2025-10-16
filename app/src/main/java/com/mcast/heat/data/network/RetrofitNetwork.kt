package com.mcast.heat.data.network

import com.mcast.heat.data.UpdateRequest
import com.mcast.heat.data.UpdateResponse
import com.mcast.heat.data.api.ServiceApi
import com.mcast.heat.data.repository.DataSource
import kotlinx.serialization.json.Json
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject


class RetrofitNetwork @Inject constructor(
    retrofit: Retrofit,
    private val networkJson: Json,
) : DataSource {

    private val serviceApi by lazy { retrofit.create(ServiceApi::class.java) }

    override suspend fun getUpdateData(updateRequest: UpdateRequest): UpdateResponse {
        return serviceApi.getUpdate(updateRequest).transResponse(networkJson)
    }

}

inline fun <reified T> Response<T>.transResponse(networkJson: Json): T {
    return if (isSuccessful) {
        checkNotNull(body())
    } else {
        val errorBody = checkNotNull(errorBody()).string()
        networkJson.decodeFromString<T>(errorBody)
    }
}

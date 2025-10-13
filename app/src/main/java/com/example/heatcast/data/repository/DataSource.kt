package com.example.heatcast.data.repository

import com.example.heatcast.data.api.ServiceApi
import javax.inject.Inject

//数据仓库接口和实现

class DataSource @Inject constructor(
    val api: ServiceApi,
) {

}
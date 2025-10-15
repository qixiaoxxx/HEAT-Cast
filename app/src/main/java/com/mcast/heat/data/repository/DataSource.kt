package com.mcast.heat.data.repository

import com.mcast.heat.data.api.ServiceApi
import javax.inject.Inject

//数据仓库接口和实现

class DataSource @Inject constructor(
    val api: ServiceApi,
) {

}
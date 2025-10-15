package com.mcast.heat.di

import com.mcast.heat.data.api.ServiceApi
import com.mcast.heat.data.repository.DataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DataSourceModule {

    @Provides
    @Singleton
    fun dataSource(
        api: ServiceApi,

    ): DataSource =
        DataSource(api, )
}
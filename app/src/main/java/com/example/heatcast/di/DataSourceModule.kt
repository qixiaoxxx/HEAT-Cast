package com.example.heatcast.di

import com.example.heatcast.data.api.ServiceApi
import com.example.heatcast.data.repository.DataSource
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
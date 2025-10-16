package com.mcast.heat.di

import com.mcast.heat.data.network.RetrofitNetwork
import com.mcast.heat.data.repository.DataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
interface DataSourceModule {
    @Binds
    fun binds(impl: RetrofitNetwork): DataSource
}
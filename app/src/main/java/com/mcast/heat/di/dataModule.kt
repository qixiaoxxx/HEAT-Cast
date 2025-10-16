package com.mcast.heat.di

import com.mcast.heat.data.repository.IServiceRepository
import com.mcast.heat.data.repository.ServiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    internal abstract fun bindsServiceRepository(
        userRepository: ServiceRepository
    ): IServiceRepository

}
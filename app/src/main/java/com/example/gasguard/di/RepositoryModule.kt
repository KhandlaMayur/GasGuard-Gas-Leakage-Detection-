package com.example.gasguard.di

import com.example.gasguard.data.repository.AlertRepositoryImpl
import com.example.gasguard.data.repository.AuthRepositoryImpl
import com.example.gasguard.data.repository.DeviceRepositoryImpl
import com.example.gasguard.domain.repository.AlertRepository
import com.example.gasguard.domain.repository.AuthRepository
import com.example.gasguard.domain.repository.DeviceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        deviceRepositoryImpl: DeviceRepositoryImpl
    ): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        alertRepositoryImpl: AlertRepositoryImpl
    ): AlertRepository
}

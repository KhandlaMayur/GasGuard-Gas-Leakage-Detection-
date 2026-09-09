package com.gasguard.gasguard.di

import com.gasguard.gasguard.data.repository.AlertRepositoryImpl
import com.gasguard.gasguard.data.repository.AuthRepositoryImpl
import com.gasguard.gasguard.data.repository.DeviceRepositoryImpl
import com.gasguard.gasguard.data.repository.ReadingRepositoryImpl
import com.gasguard.gasguard.domain.repository.AlertRepository
import com.gasguard.gasguard.domain.repository.AuthRepository
import com.gasguard.gasguard.domain.repository.DeviceRepository
import com.gasguard.gasguard.domain.repository.ReadingRepository
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

    @Binds
    @Singleton
    abstract fun bindReadingRepository(
        readingRepositoryImpl: ReadingRepositoryImpl
    ): ReadingRepository
}

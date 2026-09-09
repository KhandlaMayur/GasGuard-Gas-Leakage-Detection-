package com.gasguard.gasguard.di

import android.content.Context
import com.gasguard.gasguard.presentation.notification.GasNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideGasNotificationManager(
        @ApplicationContext context: Context
    ): GasNotificationManager = GasNotificationManager(context)
}

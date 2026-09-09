package com.gasguard.gasguard.domain.repository

import com.gasguard.gasguard.domain.model.GasReading
import kotlinx.coroutines.flow.Flow

interface ReadingRepository {
    suspend fun recordReading(userId: String, reading: GasReading): Result<Unit>
    fun observeReadings(userId: String, deviceId: String): Flow<List<GasReading>>
    suspend fun getLatestReading(userId: String, deviceId: String): GasReading?
}

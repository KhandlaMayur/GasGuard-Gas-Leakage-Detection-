package com.example.gasguard.domain.repository

import com.example.gasguard.domain.model.GasDevice
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getUserDevices(userId: String): Flow<List<GasDevice>>
    suspend fun getDeviceById(deviceId: String): GasDevice?
    suspend fun addDevice(
        userId: String,
        deviceId: String,
        deviceName: String,
        location: String
    ): Result<Unit>
    suspend fun updateDeviceMetadata(
        deviceId: String,
        deviceName: String,
        location: String
    ): Result<Unit>
    suspend fun deleteDeviceFromUser(userId: String, deviceId: String): Result<Unit>
    suspend fun updateThreshold(
        userId: String,
        deviceId: String,
        threshold: Double
    ): Result<Unit>
    fun observeDevice(deviceId: String): Flow<GasDevice?>
}

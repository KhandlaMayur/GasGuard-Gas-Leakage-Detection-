package com.gasguard.gasguard.domain.repository

import com.gasguard.gasguard.domain.model.DeviceLiveData
import com.gasguard.gasguard.domain.model.GasDevice
import com.gasguard.gasguard.domain.model.SensorStatus
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

    suspend fun updateLiveDeviceData(
        userId: String,
        deviceId: String,
        gasLevel: Double,
        batteryLevel: Int?,
        sensorStatus: SensorStatus,
        lastHeartbeat: Long
    ): Result<Unit>

    suspend fun updateHeartbeat(userId: String, deviceId: String): Result<Unit>

    fun observeDevice(userId: String, deviceId: String): Flow<GasDevice?>
    fun observeDeviceLiveData(userId: String, deviceId: String): Flow<DeviceLiveData?>
}

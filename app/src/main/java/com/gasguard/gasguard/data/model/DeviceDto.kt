package com.gasguard.gasguard.data.model

import com.gasguard.gasguard.domain.model.DeviceLiveData
import com.gasguard.gasguard.domain.model.GasDevice

data class DeviceDto(
    val deviceId: String = "",
    val ownerUserId: String? = null,
    val deviceName: String = "",
    val location: String = "",
    val threshold: Double = 30.0,
    val createdAt: Long = System.currentTimeMillis(),
    // Legacy fields (backward compatibility)
    val currentGasLevel: Double? = null,
    val status: String? = null,
    val isOnline: Boolean? = null,
    val lastUpdated: Long? = null
) {
    fun toDomain(liveData: DeviceLiveData?): GasDevice {
        val gasLevelValue = liveData?.gasLevel ?: currentGasLevel ?: 0.0
        val lastUpdatedValue = liveData?.lastUpdated ?: lastUpdated ?: createdAt
        val sensorStatusValue = liveData?.sensorStatus ?: status ?: "UNKNOWN"

        return GasDevice(
            deviceId = deviceId,
            ownerUserId = ownerUserId,
            deviceName = deviceName,
            location = location,
            threshold = threshold,
            currentGasLevel = gasLevelValue,
            gasRaw = liveData?.gasRaw ?: 0,
            sensorStatus = sensorStatusValue,
            lastHeartbeat = liveData?.lastHeartbeat,
            createdAt = createdAt,
            lastUpdated = lastUpdatedValue
        )
    }
}

data class LiveDataDto(
    val deviceId: String = "",
    val gasLevel: Double = 0.0,
    val gasRaw: Int = 0,
    val lastHeartbeat: Long = 0L,
    val lastUpdated: Long = 0L,
    val sensorStatus: String = "UNKNOWN"
) {
    fun toDomain(): DeviceLiveData {
        return DeviceLiveData(
            deviceId = deviceId,
            gasLevel = gasLevel,
            gasRaw = gasRaw,
            lastHeartbeat = lastHeartbeat,
            lastUpdated = lastUpdated,
            sensorStatus = sensorStatus
        )
    }
}

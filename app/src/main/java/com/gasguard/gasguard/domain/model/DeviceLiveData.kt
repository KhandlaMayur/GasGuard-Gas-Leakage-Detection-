package com.gasguard.gasguard.domain.model

data class DeviceLiveData(
    val deviceId: String = "",
    val gasLevel: Double = 0.0,
    val gasRaw: Int = 0,
    val lastHeartbeat: Long = 0L,
    val lastUpdated: Long = 0L,
    val sensorStatus: String = "UNKNOWN"
)

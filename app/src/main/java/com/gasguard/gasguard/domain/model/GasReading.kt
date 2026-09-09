package com.gasguard.gasguard.domain.model

data class GasReading(
    val readingId: String = "",
    val deviceId: String = "",
    val gasLevel: Double = 0.0,
    val threshold: Double = 0.0,
    val status: DeviceStatus = DeviceStatus.SAFE,
    val timestamp: Long = System.currentTimeMillis()
)

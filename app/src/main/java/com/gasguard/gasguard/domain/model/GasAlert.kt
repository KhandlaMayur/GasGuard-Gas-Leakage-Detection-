package com.gasguard.gasguard.domain.model

data class GasAlert(
    val alertId: String = "",
    val deviceId: String = "",
    val ownerUserId: String = "",
    val deviceName: String = "",
    val location: String = "",
    val gasLevel: Double = 0.0,
    val threshold: Double = 0.0,
    val severity: DeviceStatus = DeviceStatus.SAFE,
    val status: AlertStatus = AlertStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val acknowledgementDeadline: Long = 0L,
    val acknowledgedAt: Long? = null,
    val expiredAt: Long? = null,
    val resolvedAt: Long? = null
)

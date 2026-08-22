package com.example.gasguard.domain.model

data class GasDevice(
    val deviceId: String = "",
    val ownerUserId: String? = null,
    val deviceName: String = "",
    val location: String = "",
    val currentGasLevel: Float = 0.0f,
    val threshold: Double = 30.0,
    val status: DeviceStatus = DeviceStatus.SAFE,
    val isOnline: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val calculatedStatus: DeviceStatus
        get() {
            if (currentGasLevel < 0 || threshold <= 0) return DeviceStatus.SENSOR_ERROR
            return when {
                currentGasLevel < threshold -> DeviceStatus.SAFE
                currentGasLevel < (threshold * 1.5) -> DeviceStatus.WARNING
                else -> DeviceStatus.DANGER
            }
        }
}

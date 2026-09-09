package com.gasguard.gasguard.domain.model

data class GasDevice(
    val deviceId: String = "",
    val ownerUserId: String? = null,
    val deviceName: String = "",
    val location: String = "",
    val threshold: Double = 30.0,
    val currentGasLevel: Double = 0.0,
    val gasRaw: Int = 0,
    val batteryLevel: Int? = null,
    val sensorStatus: String = "UNKNOWN",
    val lastHeartbeat: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEVICE_OFFLINE_TIMEOUT_MS = 60_000L
    }

    val calculatedStatus: DeviceStatus
        get() {
            if (currentGasLevel < 0 || threshold <= 0) return DeviceStatus.SENSOR_ERROR
            return when {
                currentGasLevel < threshold -> DeviceStatus.SAFE
                currentGasLevel < (threshold * 1.5) -> DeviceStatus.WARNING
                else -> DeviceStatus.DANGER
            }
        }

    val isOnline: Boolean
        get() {
            val heartbeat = lastHeartbeat ?: return false
            val currentTime = System.currentTimeMillis()
            return (currentTime - heartbeat) <= DEVICE_OFFLINE_TIMEOUT_MS
        }
    
    val connectionStatus: DeviceConnectionStatus
        get() = if (isOnline) DeviceConnectionStatus.ONLINE else DeviceConnectionStatus.OFFLINE
}

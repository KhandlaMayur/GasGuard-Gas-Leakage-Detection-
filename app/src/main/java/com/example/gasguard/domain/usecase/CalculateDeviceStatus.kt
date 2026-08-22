package com.example.gasguard.domain.usecase

import com.example.gasguard.domain.model.DeviceStatus
import com.example.gasguard.domain.model.GasDevice
import javax.inject.Inject

class CalculateDeviceStatus @Inject constructor() {
    operator fun invoke(gasLevel: Float, threshold: Double): DeviceStatus {
        return GasDevice(currentGasLevel = gasLevel, threshold = threshold).calculatedStatus
    }
}

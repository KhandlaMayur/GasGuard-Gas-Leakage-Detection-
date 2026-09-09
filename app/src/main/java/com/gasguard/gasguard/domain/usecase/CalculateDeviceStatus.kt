package com.gasguard.gasguard.domain.usecase

import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasDevice
import javax.inject.Inject

class CalculateDeviceStatus @Inject constructor() {
    operator fun invoke(gasLevel: Double, threshold: Double): DeviceStatus {
        return GasDevice(currentGasLevel = gasLevel, threshold = threshold).calculatedStatus
    }
}

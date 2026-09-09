package com.gasguard.gasguard.domain.usecase

import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasReading
import javax.inject.Inject

data class GasAnalytics(
    val minGasLevel: Double = 0.0,
    val maxGasLevel: Double = 0.0,
    val avgGasLevel: Double = 0.0,
    val totalReadings: Int = 0,
    val warningCount: Int = 0,
    val dangerCount: Int = 0
)

class CalculateGasAnalytics @Inject constructor() {
    operator fun invoke(readings: List<GasReading>): GasAnalytics {
        if (readings.isEmpty()) return GasAnalytics()

        val gasLevels = readings.map { it.gasLevel }
        return GasAnalytics(
            minGasLevel = gasLevels.minOrNull() ?: 0.0,
            maxGasLevel = gasLevels.maxOrNull() ?: 0.0,
            avgGasLevel = gasLevels.average(),
            totalReadings = readings.size,
            warningCount = readings.count { it.status == DeviceStatus.WARNING },
            dangerCount = readings.count { it.status == DeviceStatus.DANGER }
        )
    }
}

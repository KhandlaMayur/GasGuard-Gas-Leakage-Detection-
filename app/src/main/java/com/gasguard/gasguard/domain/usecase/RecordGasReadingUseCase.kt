package com.gasguard.gasguard.domain.usecase

import com.gasguard.gasguard.domain.model.GasDevice
import com.gasguard.gasguard.domain.model.GasReading
import com.gasguard.gasguard.domain.repository.ReadingRepository
import javax.inject.Inject
import kotlin.math.abs

class RecordGasReadingUseCase @Inject constructor(
    private val readingRepository: ReadingRepository
) {
    companion object {
        private const val MINIMUM_GAS_LEVEL_CHANGE = 1.0
    }

    suspend operator fun invoke(userId: String, device: GasDevice): Result<Unit> {
        if (device.currentGasLevel < 0) return Result.success(Unit) // Don't record sensor errors as normal readings

        val latestReading = readingRepository.getLatestReading(userId, device.deviceId)
        
        val shouldRecord = latestReading == null || 
            abs(device.currentGasLevel - latestReading.gasLevel) >= MINIMUM_GAS_LEVEL_CHANGE ||
            device.calculatedStatus != latestReading.status

        return if (shouldRecord) {
            val reading = GasReading(
                deviceId = device.deviceId,
                gasLevel = device.currentGasLevel.toDouble(),
                threshold = device.threshold,
                status = device.calculatedStatus,
                timestamp = System.currentTimeMillis()
            )
            readingRepository.recordReading(userId, reading)
        } else {
            Result.success(Unit)
        }
    }
}

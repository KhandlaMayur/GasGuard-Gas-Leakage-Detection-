package com.gasguard.gasguard.domain.repository

import com.gasguard.gasguard.domain.model.GasAlert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun observeActiveAlerts(userId: String): Flow<List<GasAlert>>
    fun observeAlertHistory(userId: String): Flow<List<GasAlert>>
    fun observeAlertById(userId: String, alertId: String): Flow<GasAlert?>
    
    suspend fun createAlertIfNeeded(alert: GasAlert): Result<Unit>
    suspend fun acknowledgeAlert(userId: String, alertId: String): Result<Unit>
    suspend fun expireAlert(userId: String, alertId: String): Result<Unit>
    suspend fun getActiveAlertForDevice(userId: String, deviceId: String): GasAlert?
}

package com.example.gasguard.domain.repository

import com.example.gasguard.domain.model.GasAlert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun getAlerts(): Flow<List<GasAlert>>
    fun getActiveAlerts(): Flow<List<GasAlert>>
    suspend fun createAlert(alert: GasAlert): Result<Unit>
    suspend fun acknowledgeAlert(alertId: String): Result<Unit>
    suspend fun resolveAlert(alertId: String): Result<Unit>
    fun observeAlerts(): Flow<List<GasAlert>>
}

package com.example.gasguard.data.repository

import com.example.gasguard.domain.model.GasAlert
import com.example.gasguard.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor() : AlertRepository {
    override fun getAlerts(): Flow<List<GasAlert>> {
        return flowOf(emptyList())
    }

    override fun getActiveAlerts(): Flow<List<GasAlert>> {
        return flowOf(emptyList())
    }

    override suspend fun createAlert(alert: GasAlert): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun acknowledgeAlert(alertId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun resolveAlert(alertId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeAlerts(): Flow<List<GasAlert>> {
        return flowOf(emptyList())
    }
}

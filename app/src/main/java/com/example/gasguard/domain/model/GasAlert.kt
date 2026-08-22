package com.example.gasguard.domain.model

data class GasAlert(
    val alertId: String,
    val deviceId: String,
    val userId: String,
    val gasLevel: Float,
    val threshold: Float,
    val createdAt: Long,
    val status: AlertStatus,
    val acknowledgedAt: Long?,
    val resolvedAt: Long?
)

package com.gasguard.gasguard.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable data object Splash
@Serializable data object Login
@Serializable data object Register
@Serializable data object Dashboard
@Serializable data class DeviceDetails(val deviceId: String)
@Serializable data object AddDevice
@Serializable data class ThresholdSettings(val deviceId: String)
@Serializable data class DeviceHistory(val deviceId: String, val deviceName: String)
@Serializable data object ActiveAlerts
@Serializable data class AlertDetails(val alertId: String)
@Serializable data object AlertHistory
@Serializable data object Profile

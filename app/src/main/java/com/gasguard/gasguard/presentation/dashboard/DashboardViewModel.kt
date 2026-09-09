package com.gasguard.gasguard.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasguard.gasguard.data.local.NotificationPrefs
import com.gasguard.gasguard.domain.model.AlertStatus
import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasAlert
import com.gasguard.gasguard.domain.model.GasDevice
import com.gasguard.gasguard.domain.repository.AlertRepository
import com.gasguard.gasguard.domain.repository.AuthRepository
import com.gasguard.gasguard.domain.repository.DeviceRepository
import com.gasguard.gasguard.domain.usecase.RecordGasReadingUseCase
import com.gasguard.gasguard.presentation.notification.GasNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val alertRepository: AlertRepository,
    private val notificationManager: GasNotificationManager,
    private val notificationPrefs: NotificationPrefs,
    private val recordGasReadingUseCase: RecordGasReadingUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var devicesJob: Job? = null

    companion object {
        private const val DEFAULT_ACKNOWLEDGEMENT_DURATION_SECONDS = 60L
    }

    init {
        observeUserAndDevices()
        observeActiveAlerts()
        startExpiryTicker()
    }

    private fun startExpiryTicker() {
        viewModelScope.launch {
            while (true) {
                val currentUser = authRepository.getCurrentUser().first()
                if (currentUser != null) {
                    val activeAlerts = alertRepository.observeActiveAlerts(currentUser.userId).first()
                    val currentTime = System.currentTimeMillis()
                    activeAlerts.forEach { alert ->
                        if (currentTime >= alert.acknowledgementDeadline && alert.status == AlertStatus.ACTIVE) {
                            alertRepository.expireAlert(currentUser.userId, alert.alertId)
                            notificationManager.cancelNotification(alert.alertId)
                        }
                    }
                }
                delay(5000)
            }
        }
    }

    private fun observeUserAndDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.update { it.copy(userName = user.name) }
                    observeDevices(user.userId)
                } else {
                    _uiState.update { it.copy(isLoading = false, userName = null, devices = emptyList()) }
                    devicesJob?.cancel()
                }
            }
        }
    }

    private fun observeDevices(userId: String) {
        devicesJob?.cancel()
        devicesJob = viewModelScope.launch {
            deviceRepository.getUserDevices(userId).collect { devices ->
                _uiState.update { it.copy(isLoading = false, devices = devices, error = null) }
                devices.forEach { device ->
                    checkAndCreateAlert(device)
                    recordGasReadingUseCase(userId, device)
                }
            }
        }
    }

    private fun checkAndCreateAlert(device: GasDevice) {
        val status = device.calculatedStatus
        if (status == DeviceStatus.WARNING || status == DeviceStatus.DANGER) {
            viewModelScope.launch {
                val currentTime = System.currentTimeMillis()
                val alert = GasAlert(
                    deviceId = device.deviceId,
                    ownerUserId = device.ownerUserId ?: "",
                    deviceName = device.deviceName,
                    location = device.location,
                    gasLevel = device.currentGasLevel.toDouble(),
                    threshold = device.threshold,
                    severity = status,
                    status = AlertStatus.ACTIVE,
                    createdAt = currentTime,
                    acknowledgementDeadline = currentTime + (DEFAULT_ACKNOWLEDGEMENT_DURATION_SECONDS * 1000)
                )
                alertRepository.createAlertIfNeeded(alert)
            }
        }
    }

    private fun observeActiveAlerts() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    alertRepository.observeActiveAlerts(user.userId).collect { alerts ->
                        val currentTime = System.currentTimeMillis()
                        
                        // Handle notifications for active alerts
                        alerts.filter { it.status == AlertStatus.ACTIVE && currentTime < it.acknowledgementDeadline }
                            .forEach { alert ->
                                checkAndShowNotification(alert)
                            }

                        _uiState.update { 
                            it.copy(activeAlertsCount = alerts.count { a -> 
                                a.status == AlertStatus.ACTIVE && currentTime < a.acknowledgementDeadline 
                            }) 
                        }
                    }
                }
            }
        }
    }

    private fun checkAndShowNotification(alert: GasAlert) {
        val alreadySent = notificationPrefs.isNotificationSent(alert.alertId, alert.severity)
        
        if (!alreadySent) {
            notificationManager.showNotification(alert)
            notificationPrefs.setNotificationSent(alert.alertId, alert.severity)
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val devices: List<GasDevice> = emptyList(),
    val activeAlertsCount: Int = 0,
    val error: String? = null
)

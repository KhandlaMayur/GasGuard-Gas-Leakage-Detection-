package com.gasguard.gasguard.presentation.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasguard.gasguard.domain.model.AlertStatus
import com.gasguard.gasguard.domain.model.GasAlert
import com.gasguard.gasguard.domain.repository.AlertRepository
import com.gasguard.gasguard.domain.repository.AuthRepository
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
class AlertViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val authRepository: AuthRepository,
    private val notificationManager: GasNotificationManager
) : ViewModel() {

    private val _activeAlertsState = MutableStateFlow(ActiveAlertsUiState())
    val activeAlertsState: StateFlow<ActiveAlertsUiState> = _activeAlertsState.asStateFlow()

    private val _alertHistoryState = MutableStateFlow(AlertHistoryUiState())
    val alertHistoryState: StateFlow<AlertHistoryUiState> = _alertHistoryState.asStateFlow()

    private val _alertDetailsState = MutableStateFlow(AlertDetailsUiState())
    val alertDetailsState: StateFlow<AlertDetailsUiState> = _alertDetailsState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        observeActiveAlerts()
        observeAlertHistory()
        startGlobalCountdown()
    }

    private fun observeActiveAlerts() {
        viewModelScope.launch {
            _activeAlertsState.update { it.copy(isLoading = true) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                alertRepository.observeActiveAlerts(currentUser.userId).collect { alerts ->
                    _activeAlertsState.update { it.copy(isLoading = false, alerts = alerts, error = null) }
                }
            } else {
                _activeAlertsState.update { it.copy(isLoading = false, error = "User not logged in") }
            }
        }
    }

    private fun observeAlertHistory() {
        viewModelScope.launch {
            _alertHistoryState.update { it.copy(isLoading = true) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                alertRepository.observeAlertHistory(currentUser.userId).collect { alerts ->
                    _alertHistoryState.update { it.copy(isLoading = false, alerts = alerts, error = null) }
                }
            }
        }
    }

    private fun startGlobalCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                
                // Update active alerts remaining time and check for expiry
                val currentActiveAlerts = _activeAlertsState.value.alerts
                if (currentActiveAlerts.isNotEmpty()) {
                    val currentUser = authRepository.getCurrentUser().first()
                    currentActiveAlerts.forEach { alert ->
                        if (currentTime >= alert.acknowledgementDeadline && alert.status == AlertStatus.ACTIVE) {
                            currentUser?.let {
                                alertRepository.expireAlert(it.userId, alert.alertId)
                                notificationManager.cancelNotification(alert.alertId)
                            }
                        }
                    }
                }

                // Update details screen if it's showing an active alert
                val currentDetailAlert = _alertDetailsState.value.alert
                if (currentDetailAlert != null && currentDetailAlert.status == AlertStatus.ACTIVE) {
                    if (currentTime >= currentDetailAlert.acknowledgementDeadline) {
                        val currentUser = authRepository.getCurrentUser().first()
                        currentUser?.let {
                            alertRepository.expireAlert(it.userId, currentDetailAlert.alertId)
                            notificationManager.cancelNotification(currentDetailAlert.alertId)
                        }
                    }
                }
                
                delay(1000)
            }
        }
    }

    fun observeAlertDetails(alertId: String) {
        viewModelScope.launch {
            _alertDetailsState.update { it.copy(isLoading = true) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                alertRepository.observeAlertById(currentUser.userId, alertId).collect { alert ->
                    if (alert != null) {
                        _alertDetailsState.update { it.copy(isLoading = false, alert = alert, error = null) }
                    } else {
                        _alertDetailsState.update { it.copy(isLoading = false, error = "Alert not found") }
                    }
                }
            }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            _alertDetailsState.update { it.copy(isAcknowledging = true, error = null, successMessage = null) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                val result = alertRepository.acknowledgeAlert(currentUser.userId, alertId)
                result.onSuccess {
                    notificationManager.cancelNotification(alertId)
                    _alertDetailsState.update { 
                        it.copy(
                            isAcknowledging = false, 
                            isSuccess = true, 
                            successMessage = "Alert acknowledged successfully" 
                        ) 
                    }
                }.onFailure { e ->
                    _alertDetailsState.update { it.copy(isAcknowledging = false, error = e.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _alertDetailsState.update { it.copy(error = null, successMessage = null, isSuccess = false) }
    }
}

data class ActiveAlertsUiState(
    val isLoading: Boolean = false,
    val alerts: List<GasAlert> = emptyList(),
    val error: String? = null
)

data class AlertHistoryUiState(
    val isLoading: Boolean = false,
    val alerts: List<GasAlert> = emptyList(),
    val error: String? = null
)

data class AlertDetailsUiState(
    val isLoading: Boolean = false,
    val isAcknowledging: Boolean = false,
    val alert: GasAlert? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

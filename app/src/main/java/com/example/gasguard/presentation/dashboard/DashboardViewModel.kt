package com.example.gasguard.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gasguard.domain.model.GasDevice
import com.example.gasguard.domain.repository.AuthRepository
import com.example.gasguard.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var devicesJob: Job? = null

    init {
        observeUserAndDevices()
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
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val devices: List<GasDevice> = emptyList(),
    val error: String? = null
)

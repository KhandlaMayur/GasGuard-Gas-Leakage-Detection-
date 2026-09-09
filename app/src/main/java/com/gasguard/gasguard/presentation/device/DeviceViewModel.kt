package com.gasguard.gasguard.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasguard.gasguard.domain.model.GasDevice
import com.gasguard.gasguard.domain.repository.AuthRepository
import com.gasguard.gasguard.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _deviceListState = MutableStateFlow(DeviceListUiState())
    val deviceListState: StateFlow<DeviceListUiState> = _deviceListState.asStateFlow()

    private val _addDeviceState = MutableStateFlow(AddDeviceUiState())
    val addDeviceState: StateFlow<AddDeviceUiState> = _addDeviceState.asStateFlow()

    private val _deviceDetailsState = MutableStateFlow(DeviceDetailsUiState())
    val deviceDetailsState: StateFlow<DeviceDetailsUiState> = _deviceDetailsState.asStateFlow()

    init {
        observeUserDevices()
    }

    private fun observeUserDevices() {
        viewModelScope.launch {
            _deviceListState.update { it.copy(isLoading = true) }
            authRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    deviceRepository.getUserDevices(user.userId).collect { devices ->
                        _deviceListState.update { 
                            it.copy(isLoading = false, devices = devices, error = null) 
                        }
                    }
                } else {
                    _deviceListState.update { 
                        it.copy(isLoading = false, error = "User not logged in") 
                    }
                }
            }
        }
    }

    fun addDevice(deviceId: String, deviceName: String, location: String) {
        if (!validateAddDeviceFields(deviceId, deviceName, location)) return

        viewModelScope.launch {
            _addDeviceState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                val normalizedId = deviceId.trim().uppercase()
                val result = deviceRepository.addDevice(
                    userId = currentUser.userId,
                    deviceId = normalizedId,
                    deviceName = deviceName.trim(),
                    location = location.trim()
                )
                result.onSuccess {
                    _addDeviceState.update { 
                        it.copy(isLoading = false, isSuccess = true, successMessage = "Device added successfully") 
                    }
                }.onFailure { e ->
                    _addDeviceState.update { it.copy(isLoading = false, error = e.message) }
                }
            } else {
                _addDeviceState.update { it.copy(isLoading = false, error = "User not logged in") }
            }
        }
    }

    private fun validateAddDeviceFields(id: String, name: String, loc: String): Boolean {
        var isValid = true
        var idErr: String? = null
        var nameErr: String? = null
        var locErr: String? = null

        if (id.isBlank()) {
            idErr = "Device ID is required"
            isValid = false
        }
        if (name.isBlank()) {
            nameErr = "Device name is required"
            isValid = false
        }
        if (loc.isBlank()) {
            locErr = "Location is required"
            isValid = false
        }

        _addDeviceState.update { 
            it.copy(deviceIdError = idErr, deviceNameError = nameErr, locationError = locErr) 
        }
        return isValid
    }

    fun observeDeviceDetails(deviceId: String) {
        viewModelScope.launch {
            _deviceDetailsState.update { it.copy(isLoading = true) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                deviceRepository.observeDevice(currentUser.userId, deviceId).collect { device ->
                    if (device != null) {
                        _deviceDetailsState.update { 
                            it.copy(isLoading = false, device = device, error = null) 
                        }
                    } else {
                        _deviceDetailsState.update { 
                            it.copy(isLoading = false, error = "Device not found") 
                        }
                    }
                }
            }
        }
    }

    fun updateDeviceMetadata(deviceId: String, deviceName: String, location: String) {
        viewModelScope.launch {
            _deviceDetailsState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val result = deviceRepository.updateDeviceMetadata(deviceId, deviceName, location)
            result.onSuccess {
                _deviceDetailsState.update { 
                    it.copy(isLoading = false, successMessage = "Device updated successfully") 
                }
            }.onFailure { e ->
                _deviceDetailsState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            _deviceDetailsState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                val result = deviceRepository.deleteDeviceFromUser(currentUser.userId, deviceId)
                result.onSuccess {
                    _deviceDetailsState.update { 
                        it.copy(isLoading = false, isRemoved = true, successMessage = "Device removed successfully") 
                    }
                }.onFailure { e ->
                    _deviceDetailsState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    fun updateThreshold(deviceId: String, thresholdInput: String) {
        val trimmedInput = thresholdInput.trim()
        if (trimmedInput.isEmpty()) {
            _deviceDetailsState.update { it.copy(thresholdError = "Threshold is required") }
            return
        }

        val thresholdValue = trimmedInput.toDoubleOrNull()
        if (thresholdValue == null) {
            _deviceDetailsState.update { it.copy(thresholdError = "Please enter a valid number") }
            return
        }

        if (thresholdValue <= 0) {
            _deviceDetailsState.update { it.copy(thresholdError = "Threshold must be greater than 0%") }
            return
        }

        if (thresholdValue > 100) {
            _deviceDetailsState.update { it.copy(thresholdError = "Threshold cannot be greater than 100%") }
            return
        }

        viewModelScope.launch {
            _deviceDetailsState.update { it.copy(isUpdatingThreshold = true, thresholdError = null, successMessage = null) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                val result = deviceRepository.updateThreshold(currentUser.userId, deviceId, thresholdValue)
                result.onSuccess {
                    _deviceDetailsState.update { 
                        it.copy(isUpdatingThreshold = false, successMessage = "Threshold updated successfully") 
                    }
                }.onFailure { e ->
                    _deviceDetailsState.update { it.copy(isUpdatingThreshold = false, error = e.message ?: "Failed to update threshold") }
                }
            } else {
                _deviceDetailsState.update { it.copy(isUpdatingThreshold = false, error = "User not logged in") }
            }
        }
    }

    fun clearThresholdError() {
        _deviceDetailsState.update { it.copy(thresholdError = null) }
    }

    fun clearMessages() {
        _addDeviceState.update { 
            it.copy(error = null, successMessage = null, isSuccess = false) 
        }
        _deviceDetailsState.update { 
            it.copy(error = null, isRemoved = false, successMessage = null) 
        }
    }

    fun clearErrors() {
        _addDeviceState.update {
            it.copy(deviceIdError = null, deviceNameError = null, locationError = null, error = null)
        }
    }
}

data class DeviceListUiState(
    val isLoading: Boolean = false,
    val devices: List<GasDevice> = emptyList(),
    val error: String? = null
)

data class AddDeviceUiState(
    val isLoading: Boolean = false,
    val deviceIdError: String? = null,
    val deviceNameError: String? = null,
    val locationError: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

data class DeviceDetailsUiState(
    val isLoading: Boolean = false,
    val isUpdatingThreshold: Boolean = false,
    val device: GasDevice? = null,
    val error: String? = null,
    val thresholdError: String? = null,
    val successMessage: String? = null,
    val isRemoved: Boolean = false
)

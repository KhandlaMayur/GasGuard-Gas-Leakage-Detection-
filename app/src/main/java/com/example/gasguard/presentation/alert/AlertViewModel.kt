package com.example.gasguard.presentation.alert

import androidx.lifecycle.ViewModel
import com.example.gasguard.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val alertRepository: AlertRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()
}

data class AlertUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

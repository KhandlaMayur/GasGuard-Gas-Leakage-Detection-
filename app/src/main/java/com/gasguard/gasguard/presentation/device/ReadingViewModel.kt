package com.gasguard.gasguard.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasguard.gasguard.domain.model.GasReading
import com.gasguard.gasguard.domain.repository.AuthRepository
import com.gasguard.gasguard.domain.repository.ReadingRepository
import com.gasguard.gasguard.domain.usecase.CalculateGasAnalytics
import com.gasguard.gasguard.domain.usecase.GasAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val readingRepository: ReadingRepository,
    private val authRepository: AuthRepository,
    private val calculateGasAnalytics: CalculateGasAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingHistoryUiState())
    val uiState: StateFlow<ReadingHistoryUiState> = _uiState.asStateFlow()

    fun observeReadings(deviceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                readingRepository.observeReadings(currentUser.userId, deviceId).collect { readings ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            allReadings = readings,
                            error = null
                        ) 
                    }
                    filterReadings(_uiState.value.selectedFilter)
                }
            }
        }
    }

    fun filterReadings(filter: TimeFilter) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        
        val filtered = when (filter) {
            TimeFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis
                _uiState.value.allReadings.filter { it.timestamp >= startOfDay }
            }
            TimeFilter.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startOfPeriod = calendar.timeInMillis
                _uiState.value.allReadings.filter { it.timestamp >= startOfPeriod }
            }
            TimeFilter.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val startOfPeriod = calendar.timeInMillis
                _uiState.value.allReadings.filter { it.timestamp >= startOfPeriod }
            }
            TimeFilter.ALL -> _uiState.value.allReadings
        }

        val analytics = calculateGasAnalytics(filtered)
        _uiState.update { 
            it.copy(
                selectedFilter = filter, 
                filteredReadings = filtered,
                analytics = analytics
            ) 
        }
    }
}

enum class TimeFilter {
    TODAY, LAST_7_DAYS, LAST_30_DAYS, ALL
}

data class ReadingHistoryUiState(
    val isLoading: Boolean = false,
    val allReadings: List<GasReading> = emptyList(),
    val filteredReadings: List<GasReading> = emptyList(),
    val selectedFilter: TimeFilter = TimeFilter.TODAY,
    val analytics: GasAnalytics = GasAnalytics(),
    val error: String? = null
)

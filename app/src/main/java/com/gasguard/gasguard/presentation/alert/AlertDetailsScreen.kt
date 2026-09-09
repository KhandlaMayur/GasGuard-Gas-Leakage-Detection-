package com.gasguard.gasguard.presentation.alert

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasguard.gasguard.domain.model.AlertStatus
import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasAlert
import com.gasguard.gasguard.presentation.dashboard.StatusBadge
import com.gasguard.gasguard.presentation.util.AlertUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailsScreen(
    alertId: String,
    viewModel: AlertViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.alertDetailsState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(alertId) {
        viewModel.observeAlertDetails(alertId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isLoading && uiState.alert == null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                uiState.alert?.let { alert ->
                    AlertDetailContent(
                        alert = alert,
                        onAcknowledgeClick = { showConfirmDialog = true },
                        isAcknowledging = uiState.isAcknowledging,
                        successMessage = uiState.successMessage
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Action") },
            text = { Text("Please confirm that you have checked the gas leakage situation and taken appropriate action.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.acknowledgeAlert(alertId)
                    showConfirmDialog = false
                }) {
                    Text("CONFIRM")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun AlertDetailContent(
    alert: GasAlert,
    onAcknowledgeClick: () -> Unit,
    isAcknowledging: Boolean,
    successMessage: String?
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val severityColor = if (alert.severity == DeviceStatus.DANGER) Color(0xFFD32F2F) else Color(0xFFFFA000)

    var remainingTime by remember(alert.alertId) { mutableStateOf(AlertUtils.formatCountdown(alert.acknowledgementDeadline)) }
    
    LaunchedEffect(alert.alertId, alert.status) {
        if (alert.status == AlertStatus.ACTIVE) {
            while (true) {
                remainingTime = AlertUtils.formatCountdown(alert.acknowledgementDeadline)
                delay(1000)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Incident Severity", style = MaterialTheme.typography.labelMedium)
                    StatusBadge(status = alert.severity)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alert.deviceName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Device ID: ${alert.deviceId}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Location: ${alert.location}", style = MaterialTheme.typography.bodyMedium)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                DetailRow(label = "Gas Level at Alert", value = "${alert.gasLevel}%")
                DetailRow(label = "Configured Threshold", value = "${alert.threshold}%")
                DetailRow(label = "Created Time", value = sdf.format(Date(alert.createdAt)))
                
                alert.acknowledgedAt?.let {
                    DetailRow(label = "Acknowledged At", value = sdf.format(Date(it)))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (alert.status) {
                    AlertStatus.ACTIVE -> severityColor.copy(alpha = 0.1f)
                    AlertStatus.ACKNOWLEDGED -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                    AlertStatus.EXPIRED -> Color(0xFF757575).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when (alert.status) {
                    AlertStatus.ACTIVE -> {
                        Text(
                            text = "ACTION REQUIRED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please confirm that you have checked the gas leakage situation and taken appropriate action.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "TIME REMAINING", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = remainingTime,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = severityColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAcknowledgeClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = severityColor),
                            enabled = !isAcknowledging
                        ) {
                            if (isAcknowledging) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("I HAVE TAKEN ACTION")
                            }
                        }
                    }
                    AlertStatus.ACKNOWLEDGED -> {
                        Text(text = "✓ ALERT ACKNOWLEDGED", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        if (alert.acknowledgedAt != null) {
                            Text(
                                text = "Acknowledged At: ${sdf.format(Date(alert.acknowledgedAt))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    AlertStatus.EXPIRED -> {
                        Text(text = "⚠ ACKNOWLEDGEMENT TIME EXPIRED", fontWeight = FontWeight.Bold, color = Color.Gray)
                        if (alert.expiredAt != null) {
                            Text(
                                text = "Expired At: ${sdf.format(Date(alert.expiredAt))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        if (successMessage != null) {
            Text(
                text = "✓ $successMessage",
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

package com.gasguard.gasguard.presentation.alert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasAlert
import com.gasguard.gasguard.presentation.dashboard.StatusBadge
import com.gasguard.gasguard.presentation.util.AlertUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveAlertScreen(
    viewModel: AlertViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAlertClick: (String) -> Unit
) {
    val uiState by viewModel.activeAlertsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Alerts") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "✓", fontSize = 48.sp, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No Active Gas Alerts", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "All monitored devices are currently safe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.alerts) { alert ->
                    AlertCard(alert = alert, onClick = { onAlertClick(alert.alertId) })
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: GasAlert, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val severityColor = if (alert.severity == DeviceStatus.DANGER) Color(0xFFD32F2F) else Color(0xFFFFA000)
    val severityIcon = if (alert.severity == DeviceStatus.DANGER) "🚨" else "⚠"

    var remainingTime by remember(alert.alertId) { mutableStateOf(AlertUtils.formatCountdown(alert.acknowledgementDeadline)) }
    var isUrgent by remember(alert.alertId) { mutableStateOf(AlertUtils.getRemainingSeconds(alert.acknowledgementDeadline) < 20) }

    LaunchedEffect(alert.alertId, alert.acknowledgementDeadline) {
        while (true) {
            remainingTime = AlertUtils.formatCountdown(alert.acknowledgementDeadline)
            isUrgent = AlertUtils.getRemainingSeconds(alert.acknowledgementDeadline) < 20
            delay(1000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, severityColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = severityIcon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.severity.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
                Text(
                    text = sdf.format(Date(alert.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(text = alert.deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Location: ${alert.location}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ACTION REQUIRED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Time Remaining: ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = remainingTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Gas Level: ${alert.gasLevel}%", style = MaterialTheme.typography.bodySmall)
                Text(text = "Threshold: ${alert.threshold}%", style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("VIEW DETAILS")
            }
        }
    }
}

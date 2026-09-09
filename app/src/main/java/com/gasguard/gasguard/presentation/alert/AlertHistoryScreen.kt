package com.gasguard.gasguard.presentation.alert

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistoryScreen(
    viewModel: AlertViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.alertHistoryState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert History") },
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
                Text(text = "No Alert History", style = MaterialTheme.typography.bodyLarge)
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
                    HistoryAlertCard(alert = alert)
                }
            }
        }
    }
}

@Composable
fun HistoryAlertCard(alert: GasAlert) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val severityColor = if (alert.severity == DeviceStatus.DANGER) Color(0xFFD32F2F) else Color(0xFFFFA000)
    
    val statusText = when (alert.status) {
        AlertStatus.ACKNOWLEDGED -> "✓ ACKNOWLEDGED"
        AlertStatus.EXPIRED -> "⚠ EXPIRED"
        AlertStatus.RESOLVED -> "✓ RESOLVED"
        else -> alert.status.name
    }
    
    val statusColor = when (alert.status) {
        AlertStatus.ACKNOWLEDGED -> Color(0xFF2E7D32)
        AlertStatus.EXPIRED -> Color(0xFF757575)
        AlertStatus.RESOLVED -> Color(0xFF0288D1)
        else -> Color.Black
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = alert.severity.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = alert.deviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "Location: ${alert.location}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Gas Level: ${alert.gasLevel}%", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Threshold: ${alert.threshold}%", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Created: ${sdf.format(Date(alert.createdAt))}", style = MaterialTheme.typography.labelSmall)
                    alert.acknowledgedAt?.let {
                        Text(text = "Ack: ${sdf.format(Date(it))}", style = MaterialTheme.typography.labelSmall)
                    }
                    alert.expiredAt?.let {
                        Text(text = "Expired: ${sdf.format(Date(it))}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

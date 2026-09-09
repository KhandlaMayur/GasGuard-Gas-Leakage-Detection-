package com.gasguard.gasguard.presentation.device

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasguard.gasguard.domain.model.DeviceConnectionStatus
import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasDevice
import com.gasguard.gasguard.domain.model.SensorStatus
import com.gasguard.gasguard.presentation.dashboard.ConnectionStatusBadge
import com.gasguard.gasguard.presentation.dashboard.OnlineStatusIndicator
import com.gasguard.gasguard.presentation.dashboard.StatusBadge
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    deviceId: String,
    viewModel: DeviceViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onViewHistoryClick: (String) -> Unit
) {
    val uiState by viewModel.deviceDetailsState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deviceId) {
        viewModel.observeDeviceDetails(deviceId)
    }

    LaunchedEffect(uiState.isRemoved) {
        if (uiState.isRemoved) {
            kotlinx.coroutines.delay(1.5.seconds)
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.device?.deviceName ?: "Device Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.device != null) {
                        IconButton(onClick = { onViewHistoryClick(uiState.device?.deviceName ?: "") }) {
                            Icon(Icons.Default.History, contentDescription = "View History")
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (uiState.successMessage != null) {
                Text(
                    text = "✓ ${uiState.successMessage}",
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.isLoading && uiState.device == null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.device == null && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = "Device not found")
                }
            } else {
                uiState.device?.let { device ->
                    DeviceDetailContent(
                        device = device,
                        onEditThresholdClick = { showThresholdDialog = true }
                    )
                    
                    DeviceHealthSection(device = device)
                }
            }
        }
    }

    if (showEditDialog) {
        EditDeviceDialog(
            device = uiState.device!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, loc ->
                viewModel.updateDeviceMetadata(deviceId, name, loc)
                showEditDialog = false
            }
        )
    }

    if (showThresholdDialog) {
        ChangeThresholdDialog(
            device = uiState.device!!,
            isUpdating = uiState.isUpdatingThreshold,
            error = uiState.thresholdError,
            onDismiss = { showThresholdDialog = false },
            onConfirm = { threshold ->
                viewModel.updateThreshold(deviceId, threshold)
            },
            onValueChange = { viewModel.clearThresholdError() },
            successMessage = uiState.successMessage
        )
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage == "Threshold updated successfully") {
            kotlinx.coroutines.delay(1.seconds)
            showThresholdDialog = false
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Device") },
            text = { Text("Remove this device from your account?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeDevice(deviceId)
                    showDeleteDialog = false
                }) {
                    Text("REMOVE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun DeviceDetailContent(
    device: GasDevice,
    onEditThresholdClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val lastUpdatedText = if (device.lastUpdated > 0) sdf.format(Date(device.lastUpdated)) else "Not available"
    
    val status = device.calculatedStatus
    val statusColor = when (status) {
        DeviceStatus.SAFE -> Color(0xFF2E7D32)
        DeviceStatus.WARNING -> Color(0xFFFFA000)
        DeviceStatus.DANGER -> Color(0xFFD32F2F)
        DeviceStatus.OFFLINE -> Color(0xFF757575)
        DeviceStatus.SENSOR_ERROR -> Color(0xFFD32F2F)
    }

    val statusIcon = when (status) {
        DeviceStatus.SAFE -> "✓"
        DeviceStatus.WARNING -> "⚠"
        DeviceStatus.DANGER -> "🚨"
        else -> "❓"
    }

    val statusDescription = when (status) {
        DeviceStatus.SAFE -> "Gas level is below the configured threshold."
        DeviceStatus.WARNING -> "Gas level has reached or exceeded your configured safety threshold."
        DeviceStatus.DANGER -> "Gas level is significantly above the configured threshold."
        DeviceStatus.SENSOR_ERROR -> "Sensor data is invalid or unavailable."
        DeviceStatus.OFFLINE -> "Device is currently offline."
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Safety Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = statusColor.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CURRENT SAFETY STATUS",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = statusIcon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

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
                    Column {
                        Text(text = "Device ID", style = MaterialTheme.typography.labelSmall)
                        Text(text = device.deviceId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    StatusBadge(status = status)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailItem(label = "Location", value = device.location)
                DetailItem(label = "Owner User ID", value = device.ownerUserId ?: "Unclaimed")
                DetailItem(label = "Created At", value = if (device.createdAt > 0) sdf.format(Date(device.createdAt)) else "Not available")
                DetailItem(label = "Last Updated", value = lastUpdatedText)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Current Gas Level", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f%%", device.currentGasLevel), 
                            style = MaterialTheme.typography.displaySmall, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Raw Value: ${device.gasRaw}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        ConnectionStatusBadge(status = device.connectionStatus)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onEditThresholdClick,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Threshold: ${device.threshold}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceHealthSection(device: GasDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DEVICE HEALTH",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Connection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ConnectionStatusBadge(status = device.connectionStatus)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "ESP8266 Sensor Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = device.sensorStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (device.sensorStatus == "CRITICAL" || device.sensorStatus == "ERROR") MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            DetailItem(label = "Last Heartbeat", value = device.lastHeartbeat?.let { sdf.format(Date(it)) } ?: "Not available")
            DetailItem(label = "Battery Level", value = device.batteryLevel?.let { "$it%" } ?: "Not Available")
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EditDeviceDialog(
    device: GasDevice,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(device.deviceName) }
    var location by remember { mutableStateOf(device.location) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device Metadata") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, location) }) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun ChangeThresholdDialog(
    device: GasDevice,
    isUpdating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onValueChange: () -> Unit,
    successMessage: String?
) {
    var thresholdInput by remember { mutableStateOf(device.threshold.toString()) }

    AlertDialog(
        onDismissRequest = if (isUpdating) ({}) else onDismiss,
        title = { Text("Change Gas Threshold") },
        text = {
            Column {
                Text(
                    text = "Set the gas leakage threshold percentage for this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { 
                        thresholdInput = it
                        onValueChange()
                    },
                    label = { Text("Threshold (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    enabled = !isUpdating,
                    singleLine = true
                )
                
                if (successMessage == "Threshold updated successfully") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "✓ $successMessage", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(thresholdInput) },
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("SAVE")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text("CANCEL")
            }
        }
    )
}

package com.gasguard.gasguard.presentation.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasguard.gasguard.domain.model.DeviceConnectionStatus
import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasDevice
import com.gasguard.gasguard.presentation.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onAddDeviceClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onNavigateToActiveAlerts: () -> Unit,
    onNavigateToAlertHistory: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(authState.isUserLoggedIn) {
        if (authState.isUserLoggedIn == false) {
            onNavigateToLogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GasGuard") },
                actions = {
                    BadgedBox(
                        badge = {
                            if (dashboardState.activeAlertsCount > 0) {
                                Badge { Text(dashboardState.activeAlertsCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToActiveAlerts) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                        }
                    }
                    IconButton(onClick = onNavigateToAlertHistory) {
                        Icon(Icons.Default.History, contentDescription = "Alert History")
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDeviceClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        }
    ) { paddingValues ->
        if (dashboardState.isLoading && dashboardState.devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Hello, ${dashboardState.userName ?: "User"}!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "GasGuard Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "My Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                if (dashboardState.devices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No gas detection devices connected yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onAddDeviceClick) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Device")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dashboardState.devices) { device ->
                            DeviceCard(
                                device = device,
                                onClick = { onDeviceClick(device.deviceId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: GasDevice,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.deviceId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = device.calculatedStatus)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ConnectionStatusBadge(status = device.connectionStatus)

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Location: ${device.location}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = String.format("Gas Level: %.2f%%", device.currentGasLevel), 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Threshold: ${device.threshold}%", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sdf.format(Date(device.lastUpdated)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.SAFE -> Color(0xFF2E7D32)
        DeviceStatus.WARNING -> Color(0xFFFFA000)
        DeviceStatus.DANGER -> Color(0xFFD32F2F)
        DeviceStatus.OFFLINE -> Color(0xFF757575)
        DeviceStatus.SENSOR_ERROR -> Color(0xFFD32F2F)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ConnectionStatusBadge(status: DeviceConnectionStatus) {
    val (color, text) = when (status) {
        DeviceConnectionStatus.ONLINE -> Color(0xFF2E7D32) to "ONLINE"
        DeviceConnectionStatus.STALE -> Color(0xFFFFA000) to "DATA NOT RECENT"
        DeviceConnectionStatus.OFFLINE -> Color(0xFFD32F2F) to "OFFLINE"
        DeviceConnectionStatus.UNKNOWN -> Color(0xFF757575) to "UNKNOWN"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OnlineStatusIndicator(isOnline: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isOnline) Color(0xFF2E7D32) else Color(0xFF757575),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.labelSmall,
            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFF757575)
        )
    }
}

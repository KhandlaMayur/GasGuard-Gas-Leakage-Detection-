package com.gasguard.gasguard.presentation.device

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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.addDeviceState.collectAsState()
    
    var deviceId by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.clearMessages()
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            delay(1.5.seconds)
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Gas Detection Device") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connect a gas monitoring device to your account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = deviceId,
                onValueChange = { 
                    deviceId = it.uppercase()
                    viewModel.clearErrors()
                },
                label = { Text("Device ID") },
                placeholder = { Text("e.g. DEVICE_001") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.deviceIdError != null,
                supportingText = { uiState.deviceIdError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = deviceName,
                onValueChange = { 
                    deviceName = it
                    viewModel.clearErrors()
                },
                label = { Text("Device Name") },
                placeholder = { Text("e.g. Kitchen Gas Sensor") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.deviceNameError != null,
                supportingText = { uiState.deviceNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { 
                    location = it
                    viewModel.clearErrors()
                },
                label = { Text("Location") },
                placeholder = { Text("e.g. Kitchen") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.locationError != null,
                supportingText = { uiState.locationError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.successMessage != null) {
                Text(
                    text = "✓ ${uiState.successMessage}",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.addDevice(deviceId, deviceName, location)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("ADD DEVICE")
                }
            }
        }
    }
}

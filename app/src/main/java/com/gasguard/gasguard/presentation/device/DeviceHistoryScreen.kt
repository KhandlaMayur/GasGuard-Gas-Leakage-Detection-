package com.gasguard.gasguard.presentation.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gasguard.gasguard.domain.model.GasReading
import com.gasguard.gasguard.domain.usecase.GasAnalytics
import com.gasguard.gasguard.presentation.dashboard.StatusBadge
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceHistoryScreen(
    deviceId: String,
    deviceName: String,
    viewModel: ReadingViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(deviceId) {
        viewModel.observeReadings(deviceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(text = "Gas History", style = MaterialTheme.typography.titleMedium)
                        Text(text = deviceName, style = MaterialTheme.typography.bodySmall)
                    }
                },
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
        ) {
            FilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.filterReadings(it) }
            )

            if (uiState.isLoading && uiState.allReadings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredReadings.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        GasLevelChart(readings = uiState.filteredReadings)
                    }
                    
                    item {
                        AnalyticsSection(analytics = uiState.analytics)
                    }
                    
                    item {
                        Text(
                            text = "Detailed Readings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(uiState.filteredReadings) { reading ->
                        ReadingItem(reading = reading)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabs(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TimeFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { 
                    Text(
                        text = when(filter) {
                            TimeFilter.TODAY -> "TODAY"
                            TimeFilter.LAST_7_DAYS -> "7 DAYS"
                            TimeFilter.LAST_30_DAYS -> "30 DAYS"
                            TimeFilter.ALL -> "ALL"
                        },
                        fontSize = 12.sp
                    ) 
                }
            )
        }
    }
}

@Composable
fun GasLevelChart(readings: List<GasReading>) {
    // Vico chart setup
    // For simplicity, we just use the index as x-axis and gasLevel as y-axis
    // In a real app, we'd map timestamps to x-axis properly
    val entries = readings.reversed().mapIndexed { index, reading -> 
        index.toFloat() to reading.gasLevel.toFloat() 
    }
    
    if (entries.isEmpty()) return
    
    val chartEntryModel = entryModelOf(*entries.map { it.second }.toTypedArray())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Gas Level Trend (%)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Chart(
                chart = lineChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun AnalyticsSection(analytics: GasAnalytics) {
    Column {
        Text(
            text = "GAS ANALYTICS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AnalyticsItem(label = "Minimum", value = String.format("%.1f %%", analytics.minGasLevel), modifier = Modifier.weight(1f))
                    AnalyticsItem(label = "Maximum", value = String.format("%.1f %%", analytics.maxGasLevel), modifier = Modifier.weight(1f))
                    AnalyticsItem(label = "Average", value = String.format("%.1f %%", analytics.avgGasLevel), modifier = Modifier.weight(1f))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    AnalyticsItem(label = "Total Readings", value = analytics.totalReadings.toString(), modifier = Modifier.weight(1f))
                    AnalyticsItem(label = "Warnings", value = analytics.warningCount.toString(), modifier = Modifier.weight(1f), valueColor = Color(0xFFFFA000))
                    AnalyticsItem(label = "Danger", value = analytics.dangerCount.toString(), modifier = Modifier.weight(1f), valueColor = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
fun AnalyticsItem(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun ReadingItem(reading: GasReading) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = String.format("%.1f %%", reading.gasLevel),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Threshold: ${reading.threshold}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status = reading.status)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sdf.format(Date(reading.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "No Gas History Available", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gas readings will appear here as your device reports data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

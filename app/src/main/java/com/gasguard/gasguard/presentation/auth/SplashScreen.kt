package com.gasguard.gasguard.presentation.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SplashScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isUserLoggedIn) {
        when (uiState.isUserLoggedIn) {
            true -> onNavigateToDashboard()
            false -> onNavigateToLogin()
            null -> { /* Still checking */ }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "GasGuard Splash Screen")
        }
    }
}

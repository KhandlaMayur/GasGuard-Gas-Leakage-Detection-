package com.example.gasguard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.gasguard.presentation.alert.ActiveAlertScreen
import com.example.gasguard.presentation.alert.AlertHistoryScreen
import com.example.gasguard.presentation.auth.LoginScreen
import com.example.gasguard.presentation.auth.RegisterScreen
import com.example.gasguard.presentation.auth.SplashScreen
import com.example.gasguard.presentation.dashboard.DashboardScreen
import com.example.gasguard.presentation.device.AddDeviceScreen
import com.example.gasguard.presentation.device.DeviceDetailsScreen
import com.example.gasguard.presentation.device.ThresholdSettingsScreen
import com.example.gasguard.presentation.profile.ProfileScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Splash
    ) {
        composable<Splash> {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Login) {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Dashboard) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<Login> {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Register) },
                onNavigateToDashboard = {
                    navController.navigate(Dashboard) {
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Register> {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable<Dashboard> {
            DashboardScreen(
                onAddDeviceClick = { navController.navigate(AddDevice) },
                onNavigateToLogin = {
                    navController.navigate(Login) {
                        popUpTo(Dashboard) { inclusive = true }
                    }
                },
                onDeviceClick = { deviceId ->
                    navController.navigate(DeviceDetails(deviceId))
                }
            )
        }
        composable<DeviceDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<DeviceDetails>()
            DeviceDetailsScreen(
                deviceId = route.deviceId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<AddDevice> {
            AddDeviceScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<ThresholdSettings> { backStackEntry ->
            val route = backStackEntry.toRoute<ThresholdSettings>()
            ThresholdSettingsScreen(
                deviceId = route.deviceId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<ActiveAlert> { backStackEntry ->
            val route = backStackEntry.toRoute<ActiveAlert>()
            ActiveAlertScreen(
                alertId = route.alertId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<AlertHistory> {
            AlertHistoryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<Profile> {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

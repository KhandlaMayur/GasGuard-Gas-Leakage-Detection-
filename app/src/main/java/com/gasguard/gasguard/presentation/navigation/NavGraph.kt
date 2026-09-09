package com.gasguard.gasguard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.gasguard.gasguard.presentation.alert.ActiveAlertScreen
import com.gasguard.gasguard.presentation.alert.AlertDetailsScreen
import com.gasguard.gasguard.presentation.alert.AlertHistoryScreen
import com.gasguard.gasguard.presentation.auth.LoginScreen
import com.gasguard.gasguard.presentation.auth.RegisterScreen
import com.gasguard.gasguard.presentation.auth.SplashScreen
import com.gasguard.gasguard.presentation.dashboard.DashboardScreen
import com.gasguard.gasguard.presentation.device.AddDeviceScreen
import com.gasguard.gasguard.presentation.device.DeviceDetailsScreen
import com.gasguard.gasguard.presentation.device.DeviceHistoryScreen
import com.gasguard.gasguard.presentation.device.ThresholdSettingsScreen
import com.gasguard.gasguard.presentation.profile.ProfileScreen

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
                },
                onNavigateToActiveAlerts = {
                    navController.navigate(ActiveAlerts)
                },
                onNavigateToAlertHistory = {
                    navController.navigate(AlertHistory)
                }
            )
        }
        composable<DeviceDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<DeviceDetails>()
            DeviceDetailsScreen(
                deviceId = route.deviceId,
                onBackClick = { navController.popBackStack() },
                onViewHistoryClick = { deviceName ->
                    navController.navigate(DeviceHistory(route.deviceId, deviceName))
                }
            )
        }
        composable<DeviceHistory> { backStackEntry ->
            val route = backStackEntry.toRoute<DeviceHistory>()
            DeviceHistoryScreen(
                deviceId = route.deviceId,
                deviceName = route.deviceName,
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
        composable<ActiveAlerts> {
            ActiveAlertScreen(
                onBackClick = { navController.popBackStack() },
                onAlertClick = { alertId ->
                    navController.navigate(AlertDetails(alertId))
                }
            )
        }
        composable<AlertDetails>(
            deepLinks = listOf(
                navDeepLink { uriPattern = "gasguard://alert/{alertId}" }
            )
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<AlertDetails>()
            AlertDetailsScreen(
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

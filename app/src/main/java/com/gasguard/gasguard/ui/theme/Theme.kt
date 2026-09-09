package com.gasguard.gasguard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = SecondaryTealDark,
    tertiary = TertiaryOrangeDark,
    error = ErrorRed,
    background = OnBackground,
    surface = OnBackground,
    onPrimary = OnPrimaryBlueDark,
    onSecondary = OnSecondaryTealDark,
    onTertiary = OnTertiaryOrangeDark,
    onBackground = Background,
    onSurface = Background
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    tertiary = TertiaryOrange,
    error = ErrorRed,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimaryBlue,
    onSecondary = OnSecondaryTeal,
    onTertiary = OnTertiaryOrange,
    onBackground = OnBackground,
    onSurface = OnSurface
)

@Composable
fun GasGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

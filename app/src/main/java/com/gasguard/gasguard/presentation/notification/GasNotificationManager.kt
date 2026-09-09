package com.gasguard.gasguard.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gasguard.gasguard.MainActivity
import com.gasguard.gasguard.domain.model.DeviceStatus
import com.gasguard.gasguard.domain.model.GasAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GasNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_WARNING_ID = "gas_warning_channel"
        const val CHANNEL_DANGER_ID = "gas_danger_channel"
        private const val DEEP_LINK_URI = "gasguard://alert"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val warningChannel = NotificationChannel(
                CHANNEL_WARNING_ID,
                "Gas Warning Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for gas leakage warnings"
            }

            val dangerChannel = NotificationChannel(
                CHANNEL_DANGER_ID,
                "Gas Danger Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate action required for gas leakage danger"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(warningChannel)
            notificationManager.createNotificationChannel(dangerChannel)
        }
    }

    fun showNotification(alert: GasAlert) {
        val channelId = if (alert.severity == DeviceStatus.DANGER) CHANNEL_DANGER_ID else CHANNEL_WARNING_ID
        val title = if (alert.severity == DeviceStatus.DANGER) "🚨 GAS DANGER ALERT" else "⚠ GAS WARNING"
        val message = if (alert.severity == DeviceStatus.DANGER) {
            "Gas leakage danger detected!\nDevice: ${alert.deviceName}\nLocation: ${alert.location}\nGas Level: ${alert.gasLevel}%\nImmediate action required."
        } else {
            "Gas leakage warning detected on: ${alert.deviceName}\nLocation: ${alert.location}\nGas Level: ${alert.gasLevel}%\nAction may be required."
        }

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("$DEEP_LINK_URI/${alert.alertId}"),
            context,
            MainActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.alertId.hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Replace with app icon later
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(alert.alertId.hashCode(), notification)
    }

    fun cancelNotification(alertId: String) {
        notificationManager.cancel(alertId.hashCode())
    }
}

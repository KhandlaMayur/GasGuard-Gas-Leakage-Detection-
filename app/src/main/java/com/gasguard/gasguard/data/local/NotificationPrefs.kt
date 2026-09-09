package com.gasguard.gasguard.data.local

import android.content.Context
import android.content.SharedPreferences
import com.gasguard.gasguard.domain.model.DeviceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    fun isNotificationSent(alertId: String, severity: DeviceStatus): Boolean {
        return prefs.getBoolean("${alertId}_${severity.name}", false)
    }

    fun setNotificationSent(alertId: String, severity: DeviceStatus) {
        prefs.edit().putBoolean("${alertId}_${severity.name}", true).apply()
    }
    
    fun clearNotificationState(alertId: String) {
        prefs.edit()
            .remove("${alertId}_${DeviceStatus.WARNING.name}")
            .remove("${alertId}_${DeviceStatus.DANGER.name}")
            .apply()
    }
}

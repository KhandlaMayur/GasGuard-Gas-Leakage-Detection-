package com.gasguard.gasguard.presentation.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object AlertUtils {
    fun formatCountdown(deadline: Long): String {
        val currentTime = System.currentTimeMillis()
        val remainingMillis = deadline - currentTime
        
        if (remainingMillis <= 0) return "00:00"
        
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
        
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun getRemainingSeconds(deadline: Long): Long {
        val currentTime = System.currentTimeMillis()
        val remainingMillis = deadline - currentTime
        return if (remainingMillis <= 0) 0 else remainingMillis / 1000
    }
}

package com.example.trailynapp.driver.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.trailynapp.driver.services.WearableDataListenerService

data class HealthData(
    val heartRate: Int,
    val steps: Int,
    val status: String,
    val timestamp: Long,
    val isConnected: Boolean
)

object WearOSHealthManager {
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            WearableDataListenerService.PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }
    
    fun getHealthData(context: Context): HealthData {
        val prefs = getPrefs(context)
        return HealthData(
            heartRate = prefs.getInt(WearableDataListenerService.KEY_HEART_RATE, 0),
            steps = prefs.getInt(WearableDataListenerService.KEY_STEPS, 0),
            status = prefs.getString(WearableDataListenerService.KEY_STATUS, "Sin datos") ?: "Sin datos",
            timestamp = prefs.getLong(WearableDataListenerService.KEY_TIMESTAMP, 0L),
            isConnected = isConnected(context)
        )
    }
    
    fun isConnected(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastUpdate = prefs.getLong(WearableDataListenerService.KEY_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        
        // Considerar desconectado si no hay actualización en 30 segundos
        return (now - lastUpdate) < 30000L
    }
    
    fun getHeartRateStatus(heartRate: Int): Pair<String, Int> {
        return when {
            heartRate == 0 -> Pair("Sin señal", android.R.color.darker_gray)
            heartRate < 60 -> Pair("Bradicardia", android.R.color.holo_blue_dark)
            heartRate in 60..100 -> Pair("Normal", android.R.color.holo_green_dark)
            heartRate in 101..120 -> Pair("Elevado", android.R.color.holo_orange_dark)
            else -> Pair("⚠️ Taquicardia", android.R.color.holo_red_dark)
        }
    }
    
    fun clearHealthData(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
    }
}

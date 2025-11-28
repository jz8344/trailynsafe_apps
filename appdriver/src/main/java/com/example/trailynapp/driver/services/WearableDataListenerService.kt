package com.example.trailynapp.driver.services

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WearableDataListenerService : WearableListenerService() {
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        
        dataEvents.forEach { event ->
            if (event.dataItem.uri.path == "/health_data") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val dataMap = dataMapItem.dataMap
                
                val heartRate = dataMap.getInt("heart_rate", 0)
                val steps = dataMap.getInt("steps", 0)
                val timestamp = dataMap.getLong("timestamp", 0L)
                val status = dataMap.getString("status", "Desconocido")
                
                Log.d(TAG, "📊 Datos recibidos del smartwatch:")
                Log.d(TAG, "   ❤️ Frecuencia cardíaca: $heartRate BPM")
                Log.d(TAG, "   👣 Pasos: $steps")
                Log.d(TAG, "   📍 Estado: $status")
                Log.d(TAG, "   🕐 Timestamp: $timestamp")
                
                // Guardar en SharedPreferences para acceso global
                saveHealthData(heartRate, steps, status, timestamp)
                
                // Notificar a actividades activas
                notifyHealthDataUpdate(heartRate, steps, status)
            }
        }
    }
    
    private fun saveHealthData(heartRate: Int, steps: Int, status: String, timestamp: Long) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_HEART_RATE, heartRate)
            putInt(KEY_STEPS, steps)
            putString(KEY_STATUS, status)
            putLong(KEY_TIMESTAMP, timestamp)
            putBoolean(KEY_IS_CONNECTED, true)
            apply()
        }
    }
    
    private fun notifyHealthDataUpdate(heartRate: Int, steps: Int, status: String) {
        val intent = android.content.Intent(ACTION_HEALTH_DATA_UPDATE).apply {
            putExtra(EXTRA_HEART_RATE, heartRate)
            putExtra(EXTRA_STEPS, steps)
            putExtra(EXTRA_STATUS, status)
        }
        sendBroadcast(intent)
    }
    
    companion object {
        private const val TAG = "WearableDataListener"
        const val PREFS_NAME = "WearOSHealthData"
        const val KEY_HEART_RATE = "heart_rate"
        const val KEY_STEPS = "steps"
        const val KEY_STATUS = "status"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_IS_CONNECTED = "is_connected"
        
        const val ACTION_HEALTH_DATA_UPDATE = "com.example.trailynapp.driver.HEALTH_DATA_UPDATE"
        const val EXTRA_HEART_RATE = "extra_heart_rate"
        const val EXTRA_STEPS = "extra_steps"
        const val EXTRA_STATUS = "extra_status"
    }
}

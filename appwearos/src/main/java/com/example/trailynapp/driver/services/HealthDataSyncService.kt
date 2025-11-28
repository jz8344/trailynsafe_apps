package com.example.trailynapp.driver.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class HealthDataSyncService : Service(), SensorEventListener {
    
    private lateinit var dataClient: DataClient
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null
    
    private var currentHeartRate = 0
    private var currentSteps = 0
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        
        dataClient = Wearable.getDataClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        startSensorMonitoring()
        startDataSync()
    }
    
    private fun startSensorMonitoring() {
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    private fun startDataSync() {
        serviceScope.launch {
            while (isActive) {
                delay(10000L) // Sincronizar cada 10 segundos
                
                val putDataMapReq = PutDataMapRequest.create("/health_data")
                val dataMap = putDataMapReq.dataMap
                dataMap.putInt("heart_rate", currentHeartRate)
                dataMap.putInt("steps", currentSteps)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("status", getHealthStatus(currentHeartRate))
                
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                
                try {
                    dataClient.putDataItem(putDataReq).await()
                    Log.d(TAG, "✓ Datos enviados: HR=$currentHeartRate, Steps=$currentSteps")
                } catch (e: Exception) {
                    Log.e(TAG, "Error enviando datos: ${e.message}")
                }
            }
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    currentHeartRate = it.values[0].toInt()
                    Log.d(TAG, "❤️ Frecuencia cardíaca: $currentHeartRate BPM")
                }
                Sensor.TYPE_STEP_COUNTER -> {
                    currentSteps = it.values[0].toInt()
                    Log.d(TAG, "👣 Pasos: $currentSteps")
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    private fun getHealthStatus(heartRate: Int): String {
        return when {
            heartRate == 0 -> "Sin señal"
            heartRate < 60 -> "Bradicardia"
            heartRate in 60..100 -> "Normal"
            heartRate in 101..120 -> "Elevado"
            else -> "Taquicardia"
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }
    
    companion object {
        private const val TAG = "HealthDataSyncService"
    }
}

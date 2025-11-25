package com.example.trailynapp.driver.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.trailynapp.driver.MainActivity
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.api.RetrofitClient
import com.example.trailynapp.driver.api.UbicacionRequest
import com.example.trailynapp.driver.utils.SessionManager
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_RUTA_ID = "ruta_id"
        
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        
        private const val UPDATE_INTERVAL = 10000L // 10 segundos
        private const val FASTEST_INTERVAL = 5000L // 5 segundos
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sessionManager: SessionManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var rutaId: Int? = null
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)
        
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                rutaId = intent.getIntExtra(EXTRA_RUTA_ID, -1).takeIf { it != -1 }
                startTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
        }
        
        return START_STICKY
    }

    private fun startTracking() {
        if (isTracking) return
        
        isTracking = true
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        startLocationUpdates()
    }

    private fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        stopSelf()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationToServer(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            setMaxUpdateDelayMillis(UPDATE_INTERVAL * 2)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopTracking()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToServer(location: Location) {
        val token = sessionManager.getToken() ?: return
        
        val batteryLevel = getBatteryLevel()
        
        val ubicacionRequest = UbicacionRequest(
            latitud = location.latitude,
            longitud = location.longitude,
            ruta_id = rutaId,
            velocidad = if (location.hasSpeed()) location.speed else null,
            heading = if (location.hasBearing()) location.bearing else null,
            accuracy = if (location.hasAccuracy()) location.accuracy else null,
            battery_level = batteryLevel
        )

        serviceScope.launch {
            try {
                val response = RetrofitClient.apiService.enviarUbicacion(
                    "Bearer $token",
                    ubicacionRequest
                )
                
                if (response.isSuccessful) {
                    android.util.Log.d("LocationTracking", "Ubicación enviada: ${location.latitude}, ${location.longitude}")
                } else {
                    android.util.Log.e("LocationTracking", "Error enviando ubicación: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationTracking", "Error: ${e.message}")
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal para el seguimiento de ubicación del chofer"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TrailynSafe - Rastreo Activo")
            .setContentText("Tu ubicación está siendo compartida con los padres")
            .setSmallIcon(R.drawable.ic_my_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

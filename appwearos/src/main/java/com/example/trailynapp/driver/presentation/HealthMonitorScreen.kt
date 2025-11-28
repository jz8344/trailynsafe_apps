package com.example.trailynapp.driver.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HealthMonitorScreen(
    dataClient: DataClient,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    
    var heartRate by remember { mutableStateOf(0) }
    var steps by remember { mutableStateOf(0) }
    var isMonitoring by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Conectando...") }
    
    val scope = rememberCoroutineScope()
    
    // Sensor de frecuencia cardíaca
    val heartRateSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) }
    val stepCounterSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_HEART_RATE -> {
                            heartRate = it.values[0].toInt()
                        }
                        Sensor.TYPE_STEP_COUNTER -> {
                            steps = it.values[0].toInt()
                        }
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }
    
    // Iniciar monitoreo de sensores
    LaunchedEffect(isMonitoring) {
        if (isMonitoring) {
            heartRateSensor?.let {
                sensorManager.registerListener(
                    sensorListener,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            
            stepCounterSensor?.let {
                sensorManager.registerListener(
                    sensorListener,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            
            connectionStatus = "✓ Conectado"
        } else {
            sensorManager.unregisterListener(sensorListener)
            connectionStatus = "Desconectado"
        }
    }
    
    // Enviar datos al teléfono cada 10 segundos
    LaunchedEffect(isMonitoring, heartRate, steps) {
        if (isMonitoring) {
            while (true) {
                delay(10000L) // 10 segundos
                
                val putDataMapReq = PutDataMapRequest.create("/health_data")
                val dataMap = putDataMapReq.dataMap
                dataMap.putInt("heart_rate", heartRate)
                dataMap.putInt("steps", steps)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("status", getHealthStatus(heartRate))
                
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                
                dataClient.putDataItem(putDataReq)
            }
        }
    }
    
    // Iniciar automáticamente
    LaunchedEffect(Unit) {
        delay(1000L)
        isMonitoring = true
    }
    
    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Estado de conexión
            Text(
                text = connectionStatus,
                fontSize = 12.sp,
                color = if (isMonitoring) Color.Green else Color.Red,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Frecuencia cardíaca
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "❤️",
                        fontSize = 24.sp
                    )
                    Text(
                        text = "$heartRate",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = getHeartRateColor(heartRate)
                    )
                    Text(
                        text = "BPM",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pasos
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👣",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "$steps",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pasos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Botón toggle
            Button(
                onClick = { isMonitoring = !isMonitoring },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isMonitoring) "Detener" else "Iniciar")
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }
}

@Composable
fun getHeartRateColor(heartRate: Int): Color {
    return when {
        heartRate == 0 -> Color.Gray
        heartRate < 60 -> Color.Blue
        heartRate in 60..100 -> Color.Green
        heartRate in 101..120 -> Color.Yellow
        else -> Color.Red
    }
}

fun getHealthStatus(heartRate: Int): String {
    return when {
        heartRate == 0 -> "Sin señal"
        heartRate < 60 -> "Bradicardia"
        heartRate in 60..100 -> "Normal"
        heartRate in 101..120 -> "Elevado"
        else -> "Taquicardia"
    }
}

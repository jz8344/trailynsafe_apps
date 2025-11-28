package com.example.trailynapp.driver.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.api.RetrofitClient
import com.example.trailynapp.driver.api.QRScanRequest
import com.example.trailynapp.driver.ui.trips.Viaje
import com.example.trailynapp.driver.ui.qr.QRScannerActivity
import com.example.trailynapp.driver.ui.wearos.WearOSHealthDialog
import com.example.trailynapp.driver.utils.SessionManager
import com.example.trailynapp.driver.utils.WearOSHealthManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.trailynapp.driver.services.WearableDataListenerService
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: LatLng? = null
    private var isTrackingLocation = false
    
    private lateinit var tvRutaNombre: TextView
    private lateinit var tvDistanciaTotal: TextView
    private lateinit var tvTiempoEstimado: TextView
    private lateinit var tvNextStopInfo: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvHealthStatus: TextView
    private lateinit var btnStartTrip: Button
    private lateinit var btnCompleteStop: Button
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var fabRecenter: FloatingActionButton
    private lateinit var fabHealth: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutInstructions: LinearLayout
    
    private val healthDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WearableDataListenerService.ACTION_HEALTH_DATA_UPDATE) {
                updateHealthDisplay()
            }
        }
    }
    
    private var viajeId: Int = 0
    private var currentViaje: Viaje? = null
    private var currentParadaIndex: Int = 0
    private var isNavigationActive = false
    private var healthMonitoringJob: Job? = null
    
    // Launcher para QR Scanner
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrCode = result.data?.getStringExtra(QRScannerActivity.EXTRA_QR_CODE)
            if (qrCode != null) {
                processQRCode(qrCode)
            }
        }
    }
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val PROXIMITY_THRESHOLD_METERS = 50.0 // 50 metros
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        
        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Obtener ID del viaje
        viajeId = intent.getIntExtra("VIAJE_ID", 0)
        
        // Inicializar vistas
        tvRutaNombre = findViewById(R.id.tvRutaNombre)
        tvDistanciaTotal = findViewById(R.id.tvDistanciaTotal)
        tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado)
        tvNextStopInfo = findViewById(R.id.tvNextStopInfo)
        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvHealthStatus = findViewById(R.id.tvHealthStatus)
        btnStartTrip = findViewById(R.id.btnStartTrip) // Oculto por defecto
        btnCompleteStop = findViewById(R.id.btnCompleteStop)
        fabMyLocation = findViewById(R.id.fabMyLocation)
        fabRecenter = findViewById(R.id.fabRecenter)
        fabHealth = findViewById(R.id.fabHealth)
        progressBar = findViewById(R.id.progressBar)
        layoutInstructions = findViewById(R.id.layoutInstructions)
        
        // Ocultar btnStartTrip - ya no se usa (auto-inicio)
        btnStartTrip.visibility = View.GONE
        
        // Configurar mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // Configurar botones
        fabMyLocation.setOnClickListener {
            moveToMyLocation()
        }
        
        fabRecenter.setOnClickListener {
            recenterOnRoute()
        }
        
        fabHealth.setOnClickListener {
            showWearOSHealthDialog()
        }
        
        btnCompleteStop.setOnClickListener {
            completeCurrentStop()
        }
        
        // Iniciar monitoreo de signos vitales
        updateHealthDisplay()
        startHealthMonitoring()
        
        // Cargar datos del viaje
        loadTripData()
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configurar estilo del mapa para navegación
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
            isRotateGesturesEnabled = true
            isTiltGesturesEnabled = true
        }
        
        // Solicitar permisos de ubicación
        if (checkLocationPermission()) {
            enableMyLocation()
            startLocationTracking() // Iniciar seguimiento GPS continuo
        } else {
            requestLocationPermission()
        }
    }
    
    private fun loadTripData() {
        val token = sessionManager.getToken() ?: return
        
        android.util.Log.d("NavigationActivity", "🔄 Cargando datos del viaje ID: $viajeId")
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getViajesChofer("Bearer $token")
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body() != null) {
                        android.util.Log.d("NavigationActivity", "✅ API respondió exitosamente con ${response.body()!!.size} viajes")
                        val viaje = response.body()!!.find { it.id == viajeId }
                        if (viaje != null && viaje.ruta != null) {
                            android.util.Log.d("NavigationActivity", "✅ Viaje encontrado ID: ${viaje.id}")
                            android.util.Log.d("NavigationActivity", "📍 Ruta ID: ${viaje.ruta!!.id}, Paradas: ${viaje.ruta!!.paradas?.size ?: 0}")
                            android.util.Log.d("NavigationActivity", "🗺️ Polyline: ${viaje.ruta!!.polyline?.take(50) ?: "NULL"}...")
                            
                            currentViaje = viaje
                            displayRouteInfo(viaje)
                            
                            // Si el mapa está listo, dibujar la ruta
                            if (::googleMap.isInitialized) {
                                android.util.Log.d("NavigationActivity", "🗺️ Mapa listo, dibujando ruta...")
                                drawRouteOnMap(viaje)
                            } else {
                                android.util.Log.e("NavigationActivity", "❌ MAPA NO INICIALIZADO")
                            }
                        } else {
                            android.util.Log.e("NavigationActivity", "❌ Viaje o ruta no encontrados")
                            Toast.makeText(
                                this@NavigationActivity,
                                "Ruta no encontrada",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    } else {
                        android.util.Log.e("NavigationActivity", "❌ Error en respuesta del API: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationActivity", "❌ Excepción: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@NavigationActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun displayRouteInfo(viaje: Viaje) {
        val ruta = viaje.ruta ?: return
        
        tvRutaNombre.text = ruta.nombre
        tvDistanciaTotal.text = "${ruta.distancia_total_km} km"
        tvTiempoEstimado.text = "${ruta.tiempo_estimado_minutos} min"
        
        // Mostrar información de la siguiente parada
        updateNextStopInfo()
        
        // Sistema simplificado: solo mostrar btnCompleteStop si está en_progreso
        if (ruta.estado == "en_progreso") {
            btnCompleteStop.visibility = View.VISIBLE
            isNavigationActive = true
        } else {
            // Si no está en_progreso, ocultar todo
            btnCompleteStop.visibility = View.GONE
        }
    }
    
    private fun updateNextStopInfo() {
        val viaje = currentViaje ?: return
        val ruta = viaje.ruta ?: return
        val paradas = ruta.paradas ?: return
        
        if (currentParadaIndex < paradas.size) {
            val nextStop = paradas[currentParadaIndex]
            tvNextStopInfo.text = """
                📍 Siguiente Parada (${currentParadaIndex + 1}/${paradas.size})
                ${nextStop.direccion}
                ⏰ Hora estimada: ${nextStop.hora_estimada}
                📏 ${nextStop.distancia_desde_anterior_km} km
            """.trimIndent()
        } else {
            tvNextStopInfo.text = "🏁 Última parada completada - Ir a la escuela"
        }
    }
    
    private fun drawRouteOnMap(viaje: Viaje) {
        val ruta = viaje.ruta ?: return
        val paradas = ruta.paradas ?: return
        val escuela = viaje.escuela ?: return
        
        googleMap.clear()
        
        // Dibujar marcador de la escuela (destino final)
        val escuelaLat = escuela.latitud?.toDoubleOrNull()
        val escuelaLng = escuela.longitud?.toDoubleOrNull()
        
        if (escuelaLat != null && escuelaLng != null) {
            val escuelaPos = LatLng(escuelaLat, escuelaLng)
            googleMap.addMarker(
                MarkerOptions()
                    .position(escuelaPos)
                    .title("🏫 ${escuela.nombre}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }
        
        // Dibujar paradas y líneas de ruta
        val routePoints = mutableListOf<LatLng>()
        
        paradas.forEachIndexed { index, parada ->
            val lat = parada.latitud?.toDoubleOrNull()
            val lng = parada.longitud?.toDoubleOrNull()
            
            if (lat != null && lng != null) {
                val position = LatLng(lat, lng)
                routePoints.add(position)
                
                // Color según estado
                val markerColor = when (parada.estado) {
                    "completada" -> BitmapDescriptorFactory.HUE_BLUE
                    "en_camino" -> BitmapDescriptorFactory.HUE_ORANGE
                    else -> BitmapDescriptorFactory.HUE_RED
                }
                
                googleMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title("Parada ${parada.orden}")
                        .snippet(parada.direccion)
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )
            }
        }
        
        // Agregar escuela al final de la ruta
        if (escuelaLat != null && escuelaLng != null) {
            routePoints.add(LatLng(escuelaLat, escuelaLng))
        }
        
        // Debug logs
        android.util.Log.d("NavigationActivity", "=== DIBUJANDO MAPA ===")
        android.util.Log.d("NavigationActivity", "Total paradas: ${paradas.size}")
        android.util.Log.d("NavigationActivity", "Total puntos ruta: ${routePoints.size}")
        android.util.Log.d("NavigationActivity", "Escuela: [$escuelaLat, $escuelaLng]")
        paradas.forEachIndexed { i, p ->
            android.util.Log.d("NavigationActivity", "Parada $i: [${p.latitud}, ${p.longitud}] - ${p.estado}")
        }
        
        // Dibujar polyline completa que viene de K-means (PHP)
        android.util.Log.d("NavigationActivity", "Polyline data: ${ruta.polyline}")
        
        if (!ruta.polyline.isNullOrEmpty()) {
            android.util.Log.d("NavigationActivity", "✅ Dibujando polyline de ${ruta.polyline!!.length} caracteres")
            drawPolylineFromBackend(ruta.polyline!!)
        } else {
            android.util.Log.w("NavigationActivity", "⚠️ No hay polyline, usando fallback con ${routePoints.size} puntos")
            if (routePoints.size >= 2) {
                // Fallback: líneas directas si no hay polyline
                val polylineOptions = PolylineOptions()
                    .addAll(routePoints)
                    .color(Color.parseColor("#1976D2"))
                    .width(10f)
                    .geodesic(true)
                googleMap.addPolyline(polylineOptions)
            }
        }
        
        // Posicionar cámara para mostrar TODA la ruta (paradas + escuela)
        if (routePoints.isNotEmpty()) {
            android.util.Log.d("NavigationActivity", "📍 Ajustando cámara para mostrar toda la ruta")
            
            // Crear bounds que incluya todas las paradas y la escuela
            val builder = LatLngBounds.Builder()
            routePoints.forEach { builder.include(it) }
            val bounds = builder.build()
            
            val padding = 150 // padding en pixels
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } else if (lastKnownLocation != null) {
            // Si no hay paradas, centrar en ubicación GPS
            val cameraPosition = CameraPosition.Builder()
                .target(lastKnownLocation!!)
                .zoom(19f)
                .tilt(67.5f)
                .bearing(0f)
                .build()
            
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }
    
    private fun startNavigation() {
        val token = sessionManager.getToken() ?: return
        val ruta = currentViaje?.ruta ?: return
        
        // Verificar que tenemos GPS actual
        if (lastKnownLocation == null) {
            Toast.makeText(this, "⚠️ Esperando señal GPS...", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnStartTrip.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        // Enviar GPS actual para regenerar polyline
        val gpsData = mapOf(
            "latitud" to lastKnownLocation!!.latitude,
            "longitud" to lastKnownLocation!!.longitude
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.iniciarRuta("Bearer $token", ruta.id, gpsData)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@NavigationActivity,
                            "🚀 Ruta regenerada desde tu ubicación",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        isNavigationActive = true
                        btnStartTrip.visibility = View.GONE
                        btnCompleteStop.visibility = View.VISIBLE
                        
                        // Recargar datos con nuevo polyline
                        loadTripData()
                    } else {
                        btnStartTrip.isEnabled = true
                        Toast.makeText(
                            this@NavigationActivity,
                            "Error al iniciar navegación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnStartTrip.isEnabled = true
                    Toast.makeText(
                        this@NavigationActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun completeCurrentStop() {
        val ruta = currentViaje?.ruta ?: return
        val paradas = ruta.paradas ?: return
        
        if (currentParadaIndex >= paradas.size) {
            // Todas las paradas completadas, finalizar viaje
            completeTrip()
            return
        }
        
        val parada = paradas[currentParadaIndex]
        
        // Verificar que tengamos GPS actual
        if (lastKnownLocation == null) {
            Toast.makeText(
                this,
                "⚠️ Esperando señal GPS para validar ubicación...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Validar proximidad a la parada
        val paradaLat = parada.latitud?.toDoubleOrNull()
        val paradaLng = parada.longitud?.toDoubleOrNull()
        
        if (paradaLat == null || paradaLng == null) {
            Toast.makeText(
                this,
                "⚠️ La parada no tiene coordenadas válidas",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        val paradaLocation = LatLng(paradaLat, paradaLng)
        val distanceToStop = calculateDistance(lastKnownLocation!!, paradaLocation)
        
        Log.d("NavigationActivity", "📍 Distancia a parada: ${distanceToStop.toInt()} metros")
        
        if (distanceToStop > PROXIMITY_THRESHOLD_METERS) {
            Toast.makeText(
                this,
                "⚠️ Debes estar a menos de ${PROXIMITY_THRESHOLD_METERS.toInt()}m de la parada\n" +
                "Distancia actual: ${distanceToStop.toInt()}m",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // ✅ Está cerca, abrir escáner QR
        Log.d("NavigationActivity", "✅ Dentro del rango de proximidad. Abriendo escáner QR...")
        openQRScanner()
    }
    
    private fun openQRScanner() {
        val intent = Intent(this, QRScannerActivity::class.java)
        qrScannerLauncher.launch(intent)
    }
    
    private fun processQRCode(qrCode: String) {
        val token = sessionManager.getToken() ?: return
        val ruta = currentViaje?.ruta ?: return
        val paradas = ruta.paradas ?: return
        
        if (currentParadaIndex >= paradas.size) return
        
        val parada = paradas[currentParadaIndex]
        
        Log.d("NavigationActivity", "📷 QR escaneado: $qrCode")
        
        btnCompleteStop.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        // Crear datos con GPS y código QR
        val requestData = QRScanRequest(
            codigo_qr = qrCode,
            latitud = lastKnownLocation!!.latitude,
            longitud = lastKnownLocation!!.longitude
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            var lastError: String? = null
            
            for (attempt in 1..3) {
                try {
                    val response = RetrofitClient.apiService.completarParadaConQR(
                        "Bearer $token",
                        ruta.id,
                        parada.id,
                        requestData
                    )
                    
                    if (response.isSuccessful) {
                        success = true
                        break
                    } else {
                        val errorBody = response.errorBody()?.string()
                        lastError = errorBody ?: "Error ${response.code()}"
                    }
                } catch (e: Exception) {
                    lastError = e.message
                    if (attempt < 3) {
                        delay(1000L * (1 shl (attempt - 1)))
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                btnCompleteStop.isEnabled = true
                
                if (success) {
                    Toast.makeText(
                        this@NavigationActivity,
                        "✓ Parada completada - Ruta actualizada",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    currentParadaIndex++
                    updateNextStopInfo()
                    loadTripData()
                } else {
                    Toast.makeText(
                        this@NavigationActivity,
                        "❌ $lastError",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun completeTrip() {
        val token = sessionManager.getToken() ?: return
        val ruta = currentViaje?.ruta ?: return
        
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.completarRuta("Bearer $token", ruta.id)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@NavigationActivity,
                            "🎉 Viaje completado exitosamente",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@NavigationActivity,
                            "Error al completar viaje",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@NavigationActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun recenterOnRoute() {
        val paradas = currentViaje?.ruta?.paradas ?: return
        
        if (currentParadaIndex < paradas.size) {
            val nextStop = paradas[currentParadaIndex]
            val lat = nextStop.latitud?.toDoubleOrNull()
            val lng = nextStop.longitud?.toDoubleOrNull()
            
            if (lat != null && lng != null) {
                val position = LatLng(lat, lng)
                
                val cameraPosition = CameraPosition.Builder()
                    .target(position)
                    .zoom(17f)
                    .tilt(60f)
                    .bearing(0f)
                    .build()
                
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }
    
    private fun moveToMyLocation() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }
        
        if (lastKnownLocation != null) {
            // Usar última ubicación conocida
            val cameraPosition = CameraPosition.Builder()
                .target(lastKnownLocation!!)
                .zoom(19f)
                .tilt(67.5f) // Vista isométrica tipo navegación
                .bearing(googleMap.cameraPosition.bearing) // Mantener dirección actual
                .build()
            
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            // Obtener ubicación actual
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val myLocation = LatLng(location.latitude, location.longitude)
                        lastKnownLocation = myLocation
                        
                        val cameraPosition = CameraPosition.Builder()
                            .target(myLocation)
                            .zoom(19f)
                            .tilt(67.5f)
                            .bearing(0f)
                            .build()
                        
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    private fun enableMyLocation() {
        try {
            if (checkLocationPermission()) {
                googleMap.isMyLocationEnabled = true
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error al habilitar ubicación", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startLocationTracking() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }
        
        if (isTrackingLocation) return
        isTrackingLocation = true
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Actualizar cada 5 segundos (reducir frecuencia)
        ).apply {
            setMinUpdateIntervalMillis(3000L) // Mínimo 3 segundos
            setMaxUpdateDelayMillis(10000L)
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                
                val newLocation = LatLng(location.latitude, location.longitude)
                
                // Calcular distancia desde última ubicación
                val distanceMoved = if (lastKnownLocation != null) {
                    calculateDistance(lastKnownLocation!!, newLocation)
                } else {
                    100.0 // Primera ubicación, actualizar siempre
                }
                
                // Solo actualizar si se movió más de 5 metros (evitar rotación errática)
                if (distanceMoved > 5.0) {
                    // Calcular bearing solo con movimiento significativo
                    var bearing = 0f
                    if (lastKnownLocation != null) {
                        bearing = calculateBearing(lastKnownLocation!!, newLocation)
                    }
                    
                    lastKnownLocation = newLocation
                    
                    // Actualizar cámara siguiendo ubicación con vista isométrica GPS
                    if (isNavigationActive) {
                        val cameraPosition = CameraPosition.Builder()
                            .target(newLocation)
                            .zoom(19f) // Zoom cercano tipo navegación
                            .tilt(67.5f) // Vista 3D inclinada
                            .bearing(bearing) // Dirección de movimiento
                            .build()
                        
                        googleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(cameraPosition),
                            2000, // Animación más suave de 2 segundos
                            null
                        )
                    }
                } else if (lastKnownLocation == null) {
                    // Primera vez, solo guardar ubicación sin actualizar cámara
                    lastKnownLocation = newLocation
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                mainLooper
            )
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error al iniciar seguimiento GPS", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopLocationTracking() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            isTrackingLocation = false
        }
    }
    
    private fun calculateBearing(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) -
                sin(lat1) * cos(lat2) * cos(dLon)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
    
    private fun calculateDistance(from: LatLng, to: LatLng): Double {
        val earthRadius = 6371000.0 // Radio de la Tierra en metros
        
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c // Retorna distancia en metros
    }
    
    private fun drawPolylineFromBackend(encodedPolyline: String) {
        try {
            val decodedPath = decodePolyline(encodedPolyline)
            
            if (decodedPath.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(decodedPath)
                    .color(Color.parseColor("#1976D2")) // Azul
                    .width(12f)
                    .geodesic(true)
                    .jointType(JointType.ROUND)
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                
                googleMap.addPolyline(polylineOptions)
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al dibujar ruta: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            
            val latLng = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(latLng)
        }
        
        return poly
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                }
            }
        }
    }
    
    /**
     * Actualiza la visualización de signos vitales en tiempo real
     */
    private fun updateHealthDisplay() {
        val healthData = WearOSHealthManager.getHealthData(this)
        
        if (healthData.isConnected) {
            tvHeartRate.text = "${healthData.heartRate} BPM"
            tvHealthStatus.text = healthData.status
            
            // Cambiar color según estado
            val (_, colorRes) = WearOSHealthManager.getHeartRateStatus(healthData.heartRate)
            tvHealthStatus.setTextColor(resources.getColor(colorRes, null))
            
            // Alertar si hay taquicardia
            if (healthData.heartRate > 120) {
                Toast.makeText(
                    this,
                    "⚠️ Frecuencia cardíaca elevada: ${healthData.heartRate} BPM",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            tvHeartRate.text = "-- BPM"
            tvHealthStatus.text = "Desconectado"
            tvHealthStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }
    }
    
    /**
     * Inicia el monitoreo automático de signos vitales
     */
    private fun startHealthMonitoring() {
        healthMonitoringJob?.cancel()
        healthMonitoringJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(3000L) // Actualizar cada 3 segundos
                updateHealthDisplay()
            }
        }
    }
    
    /**
     * Muestra el diálogo completo de signos vitales
     */
    private fun showWearOSHealthDialog() {
        val dialog = WearOSHealthDialog.newInstance()
        dialog.show(supportFragmentManager, WearOSHealthDialog.TAG)
    }
    
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(WearableDataListenerService.ACTION_HEALTH_DATA_UPDATE)
        registerReceiver(healthDataReceiver, filter, RECEIVER_NOT_EXPORTED)
    }
    
    override fun onPause() {
        super.onPause()
        unregisterReceiver(healthDataReceiver)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        healthMonitoringJob?.cancel()
    }
}

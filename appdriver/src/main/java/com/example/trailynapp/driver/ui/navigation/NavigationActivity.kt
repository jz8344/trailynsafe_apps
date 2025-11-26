package com.example.trailynapp.driver.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
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
import com.example.trailynapp.driver.ui.trips.Viaje
import com.example.trailynapp.driver.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NavigationActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private lateinit var tvRutaNombre: TextView
    private lateinit var tvDistanciaTotal: TextView
    private lateinit var tvTiempoEstimado: TextView
    private lateinit var tvNextStopInfo: TextView
    private lateinit var btnStartTrip: Button
    private lateinit var btnCompleteStop: Button
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var fabRecenter: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutInstructions: LinearLayout
    
    private var viajeId: Int = 0
    private var currentViaje: Viaje? = null
    private var currentParadaIndex: Int = 0
    private var isNavigationActive = false
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
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
        btnStartTrip = findViewById(R.id.btnStartTrip)
        btnCompleteStop = findViewById(R.id.btnCompleteStop)
        fabMyLocation = findViewById(R.id.fabMyLocation)
        fabRecenter = findViewById(R.id.fabRecenter)
        progressBar = findViewById(R.id.progressBar)
        layoutInstructions = findViewById(R.id.layoutInstructions)
        
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
        
        btnStartTrip.setOnClickListener {
            startNavigation()
        }
        
        btnCompleteStop.setOnClickListener {
            completeCurrentStop()
        }
        
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
        } else {
            requestLocationPermission()
        }
    }
    
    private fun loadTripData() {
        val token = sessionManager.getToken() ?: return
        
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getViajesChofer("Bearer $token")
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body() != null) {
                        val viaje = response.body()!!.find { it.id == viajeId }
                        if (viaje != null && viaje.ruta != null) {
                            currentViaje = viaje
                            displayRouteInfo(viaje)
                            
                            // Si el mapa está listo, dibujar la ruta
                            if (::googleMap.isInitialized) {
                                drawRouteOnMap(viaje)
                            }
                        } else {
                            Toast.makeText(
                                this@NavigationActivity,
                                "Ruta no encontrada",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
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
    
    private fun displayRouteInfo(viaje: Viaje) {
        val ruta = viaje.ruta ?: return
        
        tvRutaNombre.text = ruta.nombre
        tvDistanciaTotal.text = "${ruta.distancia_total_km} km"
        tvTiempoEstimado.text = "${ruta.tiempo_estimado_minutos} min"
        
        // Mostrar información de la siguiente parada
        updateNextStopInfo()
        
        // Mostrar botón según el estado
        when (ruta.estado) {
            "pendiente" -> {
                btnStartTrip.visibility = View.VISIBLE
                btnStartTrip.text = "Comenzar Viaje"
                btnCompleteStop.visibility = View.GONE
            }
            "en_progreso" -> {
                btnStartTrip.visibility = View.GONE
                btnCompleteStop.visibility = View.VISIBLE
                isNavigationActive = true
            }
            else -> {
                btnStartTrip.visibility = View.GONE
                btnCompleteStop.visibility = View.GONE
            }
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
        
        // Dibujar línea de ruta
        if (routePoints.size >= 2) {
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .color(Color.parseColor("#1976D2"))
                .width(10f)
                .geodesic(true)
            
            googleMap.addPolyline(polylineOptions)
        }
        
        // Posicionar cámara en la primera parada con vista isométrica
        if (routePoints.isNotEmpty()) {
            val firstStop = routePoints[0]
            
            val cameraPosition = CameraPosition.Builder()
                .target(firstStop)
                .zoom(17f)
                .tilt(60f) // Vista inclinada estilo navegación
                .bearing(0f)
                .build()
            
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }
    
    private fun startNavigation() {
        val token = sessionManager.getToken() ?: return
        val ruta = currentViaje?.ruta ?: return
        
        btnStartTrip.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.iniciarRuta("Bearer $token", ruta.id)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@NavigationActivity,
                            "🚀 Navegación iniciada",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        isNavigationActive = true
                        btnStartTrip.visibility = View.GONE
                        btnCompleteStop.visibility = View.VISIBLE
                        
                        // Recargar datos
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
        val token = sessionManager.getToken() ?: return
        val ruta = currentViaje?.ruta ?: return
        val paradas = ruta.paradas ?: return
        
        if (currentParadaIndex >= paradas.size) {
            // Todas las paradas completadas, finalizar viaje
            completeTrip()
            return
        }
        
        val parada = paradas[currentParadaIndex]
        
        btnCompleteStop.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.completarParada(
                    "Bearer $token",
                    ruta.id,
                    parada.id
                )
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCompleteStop.isEnabled = true
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@NavigationActivity,
                            "✓ Parada completada",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        currentParadaIndex++
                        updateNextStopInfo()
                        loadTripData()
                    } else {
                        Toast.makeText(
                            this@NavigationActivity,
                            "Error al completar parada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCompleteStop.isEnabled = true
                    Toast.makeText(
                        this@NavigationActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
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
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val myLocation = LatLng(location.latitude, location.longitude)
                    
                    val cameraPosition = CameraPosition.Builder()
                        .target(myLocation)
                        .zoom(18f)
                        .tilt(60f)
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
}

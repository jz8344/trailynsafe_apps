package com.example.trailynapp.driver.ui.trips

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.api.RetrofitClient
import com.example.trailynapp.driver.ui.navigation.NavigationActivity
import com.example.trailynapp.driver.ui.wearos.WearOSHealthDialog
import com.example.trailynapp.driver.utils.SessionManager
import com.example.trailynapp.driver.utils.WearOSHealthManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.tasks.await
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var sessionManager: SessionManager
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tvEscuelaNombre: TextView
    private lateinit var tvEstadoViaje: TextView
    private lateinit var tvTripType: TextView
    private lateinit var btnAction: Button
    private lateinit var btnViewHealth: Button
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private var viajeId: Int = 0
    private var currentViaje: Viaje? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_detail)

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener ID del viaje
        viajeId = intent.getIntExtra("VIAJE_ID", 0)

        // Inicializar vistas
        tvEscuelaNombre = findViewById(R.id.tvEscuelaNombre)
        tvEstadoViaje = findViewById(R.id.tvEstadoViaje)
        tvTripType = findViewById(R.id.tvTipoViaje)
        btnAction = findViewById(R.id.btnAccionPrincipal)
        btnViewHealth = findViewById(R.id.btnViewHealth)
        fabMyLocation = findViewById(R.id.fabMyLocation)
        progressBar = findViewById(R.id.progressBar)

        // Configurar mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar botones
        fabMyLocation.setOnClickListener {
            moveToMyLocation()
        }

        btnAction.setOnClickListener {
            handleTripAction()
        }
        
        btnViewHealth.setOnClickListener {
            showWearOSHealthDialog()
        }

        // Cargar datos del viaje
        loadTripData()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configurar estilo del mapa
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }

        // Solicitar permisos de ubicación
        if (checkLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }

        // Si ya tenemos datos del viaje, mostrarlos en el mapa
        currentViaje?.let { viaje ->
            setupMapForTrip(viaje)
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
                        val viajesResponse = response.body()!!
                        val todosLosViajes = viajesResponse.viajes_hoy + viajesResponse.viajes_otros
                        val viaje = todosLosViajes.find { it.id == viajeId }
                        if (viaje != null) {
                            currentViaje = viaje
                            displayTripInfo(viaje)

                            // Si el mapa ya está listo, configurarlo
                            if (::googleMap.isInitialized) {
                                setupMapForTrip(viaje)
                            }
                        } else {
                            Toast.makeText(
                                this@TripDetailActivity,
                                "Viaje no encontrado",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(
                            this@TripDetailActivity,
                            "Error al cargar el viaje",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@TripDetailActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun displayTripInfo(viaje: Viaje) {
        val escuela = viaje.escuela ?: return

        tvEscuelaNombre.text = escuela.nombre
        tvTripType.text = when (viaje.tipo_viaje) {
            "ida" -> "Viaje de Ida"
            "vuelta" -> "Viaje de Vuelta"
            else -> viaje.tipo_viaje
        }

        // Mostrar estado actual
        val estadoTexto = when (viaje.estado) {
            "pendiente" -> "Pendiente"
            "programado" -> "Programado"
            "en_confirmaciones" -> "En Confirmaciones (${viaje.confirmaciones_actuales ?: 0}/${viaje.cupo_maximo ?: 50})"
            "confirmado" -> "Confirmado (${viaje.confirmaciones_actuales ?: 0} confirmaciones)"
            "generando_ruta" -> "Generando Ruta..."
            "ruta_generada" -> "Listo para iniciar"
            "en_curso" -> "En Progreso"
            "completado" -> "Completado"
            "finalizado" -> "Finalizado"
            "cancelado" -> "Cancelado"
            else -> viaje.estado
        }
        tvEstadoViaje.text = estadoTexto

        // Obtener estado efectivo y estado en BD
        val estadoEfectivo = viaje.estado
        val estadoBd = viaje.estado_bd ?: viaje.estado
        
        // Configurar botón de acción según estado efectivo y estado en BD
        when {
            // Si el estado BD es 'programado' pero el estado efectivo indica que es hora de confirmar
            estadoBd == "programado" && (estadoEfectivo == "en_confirmaciones" || estadoEfectivo == "interactuable") -> {
                btnAction.text = "� ABRIR CONFIRMACIONES"
                btnAction.isEnabled = true
            }
            estadoBd == "pendiente" -> {
                btnAction.text = "📋 Programar Viaje"
                btnAction.isEnabled = true
            }
            estadoBd == "programado" -> {
                btnAction.text = "🔔 Abrir Confirmaciones"
                btnAction.isEnabled = true
            }
            estadoBd == "en_confirmaciones" -> {
                val confirmacionesHoy = viaje.confirmaciones_hoy ?: 0
                val cupoMinimo = viaje.cupo_minimo ?: 1
                if (confirmacionesHoy >= cupoMinimo) {
                    btnAction.text = "🔒 Cerrar Confirmaciones ($confirmacionesHoy confirmados)"
                    btnAction.isEnabled = true
                } else {
                    val faltantes = cupoMinimo - confirmacionesHoy
                    btnAction.text = "⏳ Faltan $faltantes confirmaciones ($confirmacionesHoy/$cupoMinimo)"
                    btnAction.isEnabled = false
                }
            }
            estadoBd == "confirmado" -> {
                btnAction.text = "📍 Generar Ruta"
                btnAction.isEnabled = true
            }
            estadoBd == "generando_ruta" -> {
                btnAction.text = "⚙️ Generando Ruta..."
                btnAction.isEnabled = false
            }
            estadoBd == "ruta_generada" -> {
                btnAction.text = "🚀 Comenzar Viaje"
                btnAction.isEnabled = true
            }
            estadoBd == "en_curso" -> {
                btnAction.text = "🗺️ Abrir Navegación"
                btnAction.isEnabled = true
                // Auto-abrir navegación si tiene ruta
                if (viaje.ruta != null) {
                    openNavigation()
                }
            }
            estadoBd == "completado" || estadoBd == "finalizado" -> {
                btnAction.text = "Viaje Completado"
                btnAction.isEnabled = false
            }
            estadoBd == "cancelado" -> {
                btnAction.text = "Viaje Cancelado"
                btnAction.isEnabled = false
            }
            else -> {
                btnAction.text = "No disponible"
                btnAction.isEnabled = false
            }
        }
    }

    private fun setupMapForTrip(viaje: Viaje) {
        val escuela = viaje.escuela ?: return

        // Obtener coordenadas de la escuela
        val latitud = escuela.latitud?.toDoubleOrNull()
        val longitud = escuela.longitud?.toDoubleOrNull()

        if (latitud != null && longitud != null) {
            val schoolLocation = LatLng(latitud, longitud)

            // Agregar marcador de la escuela
            googleMap.addMarker(
                MarkerOptions()
                    .position(schoolLocation)
                    .title(escuela.nombre)
            )

            // Configurar cámara con vista isométrica (inclinada)
            val cameraPosition = CameraPosition.Builder()
                .target(schoolLocation)
                .zoom(16f)
                .tilt(45f) // Vista inclinada tipo indicaciones
                .bearing(0f)
                .build()

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun handleTripAction() {
        val viaje = currentViaje ?: return
        val token = sessionManager.getToken() ?: return
        
        val estadoBd = viaje.estado_bd ?: viaje.estado
        
        // ⌚ VALIDACIÓN WEAR OS: Solo para estados críticos (ruta_generada, en_curso)
        if (estadoBd == "ruta_generada" || estadoBd == "en_curso") {
            if (!WearOSHealthManager.isConnected(this)) {
                showWearOSRequiredDialog()
                return
            }
        }

        // Caso especial: en_confirmaciones con cupo mínimo → mostrar diálogo de confirmación
        if (estadoBd == "en_confirmaciones") {
            val confirmacionesHoy = viaje.confirmaciones_hoy ?: 0
            val cupoMinimo = viaje.cupo_minimo ?: 1
            
            if (confirmacionesHoy >= cupoMinimo) {
                showCerrarConfirmacionesDialog(viaje, token, confirmacionesHoy)
                return
            } else {
                Toast.makeText(
                    this,
                    "⚠️ Aún no hay suficientes confirmaciones ($confirmacionesHoy/$cupoMinimo)",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        // Para otros estados, ejecutar acción directamente
        executeAction(viaje, token, estadoBd)
    }
    
    private fun showCerrarConfirmacionesDialog(viaje: Viaje, token: String, confirmaciones: Int) {
        AlertDialog.Builder(this)
            .setTitle("🔒 Cerrar Confirmaciones")
            .setMessage(
                "¿Deseas cerrar las confirmaciones y confirmar el viaje?\n\n" +
                "📊 Confirmaciones actuales: $confirmaciones\n\n" +
                "Una vez cerradas, los padres ya no podrán confirmar asistencia para este viaje."
            )
            .setPositiveButton("Sí, cerrar y confirmar") { _, _ ->
                cerrarConfirmacionesYConfirmar(viaje, token)
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }
    
    private fun cerrarConfirmacionesYConfirmar(viaje: Viaje, token: String) {
        btnAction.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener ubicación GPS del chofer
                val location = obtenerUbicacionActual()
                if (location == null) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnAction.isEnabled = true
                        Toast.makeText(
                            this@TripDetailActivity,
                            "⚠️ Necesitamos tu ubicación GPS. Habilita el GPS.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                
                val gpsData = mapOf(
                    "latitud_chofer" to location.latitude,
                    "longitud_chofer" to location.longitude
                )
                
                val response = RetrofitClient.apiService.cerrarConfirmaciones(
                    "Bearer $token", 
                    viaje.id, 
                    gpsData
                )
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@TripDetailActivity,
                            "✅ Confirmaciones cerradas. Viaje confirmado.\nAhora puedes generar la ruta.",
                            Toast.LENGTH_LONG
                        ).show()
                        loadTripData()
                    } else {
                        btnAction.isEnabled = true
                        val errorMsg = try {
                            response.errorBody()?.string() ?: "Error al cerrar confirmaciones"
                        } catch (e: Exception) {
                            "Error al cerrar confirmaciones"
                        }
                        Toast.makeText(this@TripDetailActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    Toast.makeText(this@TripDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun executeAction(viaje: Viaje, token: String, estadoBd: String) {
        btnAction.isEnabled = false
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = when (estadoBd) {
                    "pendiente" -> {
                        RetrofitClient.apiService.programarViaje("Bearer $token", viaje.id)
                    }
                    "programado" -> {
                        RetrofitClient.apiService.abrirConfirmaciones("Bearer $token", viaje.id)
                    }
                    "confirmado" -> {
                        RetrofitClient.apiService.confirmarViaje("Bearer $token", viaje.id)
                    }
                    "ruta_generada" -> {
                        openNavigation()
                        null
                    }
                    "en_curso" -> {
                        openNavigation()
                        null
                    }
                    else -> null
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response != null && response.isSuccessful) {
                        val message = when (estadoBd) {
                            "pendiente" -> "✅ Viaje programado exitosamente"
                            "programado" -> "✅ Confirmaciones abiertas. Los padres ya pueden confirmar."
                            "confirmado" -> "⚙️ Generando ruta óptima..."
                            "ruta_generada" -> "✅ Iniciando viaje..."
                            "en_curso" -> "🗺️ Abriendo navegación..."
                            else -> "Acción realizada"
                        }
                        Toast.makeText(this@TripDetailActivity, message, Toast.LENGTH_LONG).show()
                        loadTripData()
                    } else {
                        btnAction.isEnabled = true
                        val errorMsg = try {
                            response?.errorBody()?.string() ?: "Error al actualizar el viaje"
                        } catch (e: Exception) {
                            "Error al actualizar el viaje"
                        }
                        Toast.makeText(this@TripDetailActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    Toast.makeText(this@TripDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openNavigation() {
        val intent = Intent(this, NavigationActivity::class.java)
        intent.putExtra("VIAJE_ID", viajeId)
        startActivity(intent)
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
                    
                    // Mover cámara con vista isométrica
                    val cameraPosition = CameraPosition.Builder()
                        .target(myLocation)
                        .zoom(17f)
                        .tilt(45f)
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
    
    /**
     * Obtiene la ubicación GPS actual del chofer de forma síncrona
     * Retorna null si no hay ubicación disponible
     */
    private suspend fun obtenerUbicacionActual(): Location? {
        return try {
            if (!checkLocationPermission()) {
                return null
            }
            
            // Intentar obtener última ubicación conocida
            withContext(Dispatchers.IO) {
                fusedLocationClient.lastLocation.await()
            }
        } catch (e: Exception) {
            android.util.Log.e("TripDetailActivity", "Error obteniendo ubicación: ${e.message}")
            null
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
                } else {
                    Toast.makeText(
                        this,
                        "Permiso de ubicación necesario para esta función",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    /**
     * Muestra diálogo de monitoreo de signos vitales Wear OS
     */
    private fun showWearOSHealthDialog() {
        val dialog = WearOSHealthDialog.newInstance()
        dialog.show(supportFragmentManager, WearOSHealthDialog.TAG)
    }
    
    /**
     * Muestra diálogo cuando se requiere Wear OS para iniciar viaje
     */
    private fun showWearOSRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("⌚ Smartwatch Requerido")
            .setMessage(
                "Para tu seguridad, necesitamos monitorear tus signos vitales durante el viaje.\n\n" +
                "Por favor:\n" +
                "1. Conecta tu smartwatch Wear OS\n" +
                "2. Abre la app TrailynSafe en el reloj\n" +
                "3. Verifica que los datos se estén sincronizando"
            )
            .setPositiveButton("Ver Estado") { _, _ ->
                showWearOSHealthDialog()
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }
}

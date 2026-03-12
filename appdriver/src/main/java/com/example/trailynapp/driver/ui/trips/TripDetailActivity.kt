package com.example.trailynapp.driver.ui.trips

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TripDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var sessionManager: SessionManager
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tvEscuelaNombre: TextView
    private lateinit var tvEstadoViaje: TextView
    private lateinit var tvTripType: TextView
    private lateinit var tvConfirmacionesCount: TextView
    private lateinit var tvConfirmacionesLabel: TextView
    private lateinit var progressConfirmaciones: ProgressBar
    private lateinit var btnAction: Button
    private lateinit var btnViewHealth: Button
    private lateinit var switchWearOsMonitoring: SwitchMaterial
    private lateinit var tvWearOsMonitoringDesc: TextView
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private var viajeId: Int = 0
    private var currentViaje: Viaje? = null
    private var lastConfirmacionesCount: Int = -1

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable =
            object : Runnable {
                override fun run() {
                    val viaje = currentViaje
                    if (viaje != null) {
                        val estadoBd = viaje.estado_bd ?: viaje.estado
                        if (estadoBd == "en_confirmaciones" || estadoBd == "programado") {
                            loadTripData(silent = true)
                        }
                    }
                    refreshHandler.postDelayed(this, 15_000)
                }
            }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_detail)

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        viajeId = intent.getIntExtra("VIAJE_ID", 0)

        tvEscuelaNombre = findViewById(R.id.tvEscuelaNombre)
        tvEstadoViaje = findViewById(R.id.tvEstadoViaje)
        tvTripType = findViewById(R.id.tvTipoViaje)
        tvConfirmacionesCount = findViewById(R.id.tvConfirmacionesCount)
        tvConfirmacionesLabel = findViewById(R.id.tvConfirmacionesLabel)
        progressConfirmaciones = findViewById(R.id.progressConfirmaciones)
        btnAction = findViewById(R.id.btnAccionPrincipal)
        btnViewHealth = findViewById(R.id.btnViewHealth)
        switchWearOsMonitoring = findViewById(R.id.switchWearOsMonitoring)
        tvWearOsMonitoringDesc = findViewById(R.id.tvWearOsMonitoringDesc)
        fabMyLocation = findViewById(R.id.fabMyLocation)
        progressBar = findViewById(R.id.progressBar)

        val mapFragment =
                supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fabMyLocation.setOnClickListener { moveToMyLocation() }
        btnAction.setOnClickListener { handleTripAction() }
        btnViewHealth.setOnClickListener { showWearOSHealthDialog() }

        // Inicializar toggle Wear OS Monitoring desde preferencias
        val wearEnabled = sessionManager.isWearOsMonitoringEnabled()
        switchWearOsMonitoring.isChecked = wearEnabled
        updateWearOsMonitoringDesc(wearEnabled)
        switchWearOsMonitoring.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setWearOsMonitoring(isChecked)
            updateWearOsMonitoringDesc(isChecked)
        }

        loadTripData()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }

        if (checkLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }

        currentViaje?.let { viaje -> setupMapForTrip(viaje) }
    }

    private fun loadTripData(silent: Boolean = false) {
        val token = sessionManager.getToken() ?: return

        if (!silent) {
            progressBar.visibility = View.VISIBLE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getViajesChofer("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (!silent) progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val viajesResponse = response.body()!!
                        val todosLosViajes = viajesResponse.viajes_hoy + viajesResponse.viajes_otros
                        val viaje = todosLosViajes.find { it.id == viajeId }
                        if (viaje != null) {
                            currentViaje = viaje
                            displayTripInfo(viaje, animated = silent)

                            if (::googleMap.isInitialized && !silent) {
                                setupMapForTrip(viaje)
                            }
                        } else if (!silent) {
                            Toast.makeText(
                                            this@TripDetailActivity,
                                            "Viaje no encontrado",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                            finish()
                        }
                    } else if (!silent) {
                        Toast.makeText(
                                        this@TripDetailActivity,
                                        "Error al cargar el viaje",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!silent) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                                        this@TripDetailActivity,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        finish()
                    }
                }
            }
        }
    }

    private fun displayTripInfo(viaje: Viaje, animated: Boolean = false) {
        val escuela = viaje.escuela ?: return

        tvEscuelaNombre.text = escuela.nombre
        tvTripType.text =
                when (viaje.tipo_viaje) {
                    "ida" -> "Viaje de Ida"
                    "vuelta" -> "Viaje de Vuelta"
                    else -> viaje.tipo_viaje
                }

        val confirmacionesHoy = viaje.confirmaciones_hoy ?: 0
        val cupoMax = viaje.cupo_maximo ?: 50

        if (animated && lastConfirmacionesCount >= 0 && lastConfirmacionesCount != confirmacionesHoy
        ) {
            animateCounterChange(lastConfirmacionesCount, confirmacionesHoy)
        } else {
            tvConfirmacionesCount.text = "$confirmacionesHoy/$cupoMax"
        }
        lastConfirmacionesCount = confirmacionesHoy

        tvConfirmacionesLabel.text = "confirmaciones"
        progressConfirmaciones.max = cupoMax
        progressConfirmaciones.progress = confirmacionesHoy

        val estadoTexto =
                when (viaje.estado) {
                    "pendiente" -> "Pendiente"
                    "programado" -> "Programado"
                    "en_confirmaciones" -> "En Confirmaciones"
                    "confirmado" -> "Confirmado"
                    "generando_ruta" -> "Generando Ruta..."
                    "ruta_generada" -> "Listo para iniciar"
                    "en_curso" -> "En Progreso"
                    "completado" -> "Completado"
                    "finalizado" -> "Finalizado"
                    "cancelado" -> "Cancelado"
                    else -> viaje.estado
                }
        tvEstadoViaje.text = estadoTexto

        val estadoEfectivo = viaje.estado
        val estadoBd = viaje.estado_bd ?: viaje.estado

        when {
            estadoBd == "programado" &&
                    (estadoEfectivo == "en_confirmaciones" ||
                            estadoEfectivo == "interactuable") -> {
                btnAction.text = "🔔 ABRIR CONFIRMACIONES"
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
                val cupoMinimo = viaje.cupo_minimo ?: 1
                if (confirmacionesHoy >= cupoMinimo) {
                    btnAction.text = "🔒 Cerrar Confirmaciones ($confirmacionesHoy confirmados)"
                    btnAction.isEnabled = true
                } else {
                    val faltantes = cupoMinimo - confirmacionesHoy
                    btnAction.text =
                            "⏳ Faltan $faltantes confirmaciones ($confirmacionesHoy/$cupoMinimo)"
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
                if (viaje.ruta != null && !animated) {
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

    private fun animateCounterChange(from: Int, to: Int) {
        val viaje = currentViaje ?: return
        val cupoMax = viaje.cupo_maximo ?: 50

        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 800
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            tvConfirmacionesCount.text = "$value/$cupoMax"
            progressConfirmaciones.progress = value
        }
        animator.start()

        tvConfirmacionesCount
                .animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(200)
                .withEndAction {
                    tvConfirmacionesCount.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
                }
                .start()
    }

    private fun setupMapForTrip(viaje: Viaje) {
        val escuela = viaje.escuela ?: return

        val latitud = escuela.latitud?.toDoubleOrNull()
        val longitud = escuela.longitud?.toDoubleOrNull()

        if (latitud != null && longitud != null) {
            val schoolLocation = LatLng(latitud, longitud)

            googleMap.addMarker(MarkerOptions().position(schoolLocation).title(escuela.nombre))

            val cameraPosition =
                    CameraPosition.Builder()
                            .target(schoolLocation)
                            .zoom(16f)
                            .tilt(45f)
                            .bearing(0f)
                            .build()

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun updateWearOsMonitoringDesc(enabled: Boolean) {
        tvWearOsMonitoringDesc.text = if (enabled)
            "Monitoreo de salud activado — se requerirá reloj"
        else
            "Monitoreo de salud desactivado"
    }

    private fun handleTripAction() {
        val viaje = currentViaje ?: return
        val token = sessionManager.getToken() ?: return

        val estadoBd = viaje.estado_bd ?: viaje.estado

        // Solo verificar Wear OS si el toggle está activo
        if ((estadoBd == "ruta_generada" || estadoBd == "en_curso") &&
            sessionManager.isWearOsMonitoringEnabled()) {
            if (!WearOSHealthManager.isConnected(this)) {
                showWearOSRequiredDialog()
                return
            }
        }

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
                        )
                        .show()
                return
            }
        }

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
                val location = obtenerUbicacionActual()
                if (location == null) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnAction.isEnabled = true
                        Toast.makeText(
                                        this@TripDetailActivity,
                                        "⚠️ Necesitamos tu ubicación GPS. Habilita el GPS.",
                                        Toast.LENGTH_LONG
                                )
                                .show()
                    }
                    return@launch
                }

                val gpsData =
                        mapOf(
                                "latitud_chofer" to location.latitude,
                                "longitud_chofer" to location.longitude
                        )

                val response =
                        RetrofitClient.apiService.cerrarConfirmaciones(
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
                                )
                                .show()
                        loadTripData()
                    } else {
                        btnAction.isEnabled = true
                        val errorMsg =
                                try {
                                    response.errorBody()?.string()
                                            ?: "Error al cerrar confirmaciones"
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
                    Toast.makeText(
                                    this@TripDetailActivity,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }
        }
    }

    private fun executeAction(viaje: Viaje, token: String, estadoBd: String) {
        btnAction.isEnabled = false
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response =
                        when (estadoBd) {
                            "pendiente" ->
                                    RetrofitClient.apiService.programarViaje(
                                            "Bearer $token",
                                            viaje.id
                                    )
                            "programado" ->
                                    RetrofitClient.apiService.abrirConfirmaciones(
                                            "Bearer $token",
                                            viaje.id
                                    )
                            "confirmado" ->
                                    RetrofitClient.apiService.confirmarViaje(
                                            "Bearer $token",
                                            viaje.id
                                    )
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
                        val message =
                                when (estadoBd) {
                                    "pendiente" -> "✅ Viaje programado exitosamente"
                                    "programado" ->
                                            "✅ Confirmaciones abiertas. Los padres ya pueden confirmar."
                                    "confirmado" -> "⚙️ Generando ruta óptima..."
                                    "ruta_generada" -> "✅ Iniciando viaje..."
                                    "en_curso" -> "🗺️ Abriendo navegación..."
                                    else -> "Acción realizada"
                                }
                        Toast.makeText(this@TripDetailActivity, message, Toast.LENGTH_LONG).show()
                        loadTripData()
                    } else {
                        btnAction.isEnabled = true
                        val errorMsg =
                                try {
                                    response?.errorBody()?.string()
                                            ?: "Error al actualizar el viaje"
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
                    Toast.makeText(
                                    this@TripDetailActivity,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
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
                    val cameraPosition =
                            CameraPosition.Builder()
                                    .target(myLocation)
                                    .zoom(17f)
                                    .tilt(45f)
                                    .bearing(0f)
                                    .build()
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
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

    private suspend fun obtenerUbicacionActual(): Location? {
        return try {
            if (!checkLocationPermission()) return null
            withContext(Dispatchers.IO) { fusedLocationClient.lastLocation.await() }
        } catch (e: Exception) {
            android.util.Log.e("TripDetailActivity", "Error obteniendo ubicación: ${e.message}")
            null
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.postDelayed(refreshRunnable, 15_000)
        loadTripData(silent = true)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                                grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    enableMyLocation()
                } else {
                    Toast.makeText(
                                    this,
                                    "Permiso de ubicación necesario para esta función",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            }
        }
    }

    private fun showWearOSHealthDialog() {
        val dialog = WearOSHealthDialog.newInstance()
        dialog.show(supportFragmentManager, WearOSHealthDialog.TAG)
    }

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
                .setPositiveButton("Ver Estado") { _, _ -> showWearOSHealthDialog() }
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .show()
    }
}

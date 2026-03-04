package com.example.trailynapp.fragments

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.R
import com.example.trailynapp.api.ConfirmacionDetalle
import com.example.trailynapp.api.Hijo
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.api.ViajeActivo
import com.example.trailynapp.api.ViajeDisponible
import com.example.trailynapp.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class MonitorFragment : Fragment(), OnMapReadyCallback, SensorEventListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var tvInstruccion: TextView
    private lateinit var progressBarAddress: ProgressBar
    private lateinit var btnConfirmar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var bottomSheet: LinearLayout
    private lateinit var centerPinContainer: View
    private lateinit var layoutViajesList: LinearLayout
    private lateinit var navigationArrow: ImageView
    private lateinit var fabMiUbicacion: FloatingActionButton
    private lateinit var layoutYaConfirmado: LinearLayout
    private lateinit var btnCambiarUbicacion: ImageButton
    private lateinit var tvHijoConfirmado: TextView

    private var googleMap: GoogleMap? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedLocation: LatLng? = null
    private var selectedAddress: String? = null
    private var selectedViaje: ViajeDisponible? = null
    private var existingConfirmacion: ConfirmacionDetalle? = null
    private var hijosCache: List<Hijo> = emptyList()
    private var confirmacionesCache: List<ConfirmacionDetalle> = emptyList()
    private var viajesActivosCache: List<ViajeActivo> = emptyList()
    private var isEditingLocation = false

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var currentAzimuth = 0f
    private var currentRotation = 0f

    private val locationPermissionRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    enableMyLocation()
                    centerOnMyLocation()
                } else {
                    Toast.makeText(
                                    requireContext(),
                                    "Se necesita permiso de ubicación para mejor experiencia",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        geocoder = Geocoder(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        recyclerView = view.findViewById(R.id.recyclerViewViajes)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyView = view.findViewById(R.id.tvEmptyView)
        tvDireccion = view.findViewById(R.id.tvDireccion)
        tvInstruccion = view.findViewById(R.id.tvInstruccion)
        progressBarAddress = view.findViewById(R.id.progressBarAddress)
        btnConfirmar = view.findViewById(R.id.btnConfirmar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        bottomSheet = view.findViewById(R.id.bottomSheet)
        centerPinContainer = view.findViewById(R.id.centerPinContainer)
        navigationArrow = view.findViewById(R.id.centerMarker)
        layoutViajesList = view.findViewById(R.id.layoutViajesList)
        fabMiUbicacion = view.findViewById(R.id.fabMiUbicacion)
        layoutYaConfirmado = view.findViewById(R.id.layoutYaConfirmado)
        btnCambiarUbicacion = view.findViewById(R.id.btnCambiarUbicacion)
        tvHijoConfirmado = view.findViewById(R.id.tvHijoConfirmado)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        btnCancelar.setOnClickListener { ocultarMapa() }
        btnConfirmar.setOnClickListener { confirmarViaje() }
        fabMiUbicacion.setOnClickListener { centerOnMyLocation() }
        btnCambiarUbicacion.setOnClickListener { habilitarEdicionUbicacion() }

        precargarHijos()
        cargarConfirmacionesYViajes()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = false

            setOnCameraIdleListener {
                if (isEditingLocation || existingConfirmacion == null) {
                    val center = cameraPosition.target
                    selectedLocation = center
                    obtenerDireccion(center)
                }
            }
        }

        checkAndRequestLocationPermission()
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun enableMyLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            android.util.Log.e("MonitorFragment", "Permiso de ubicación no otorgado")
        }
    }

    private fun centerOnMyLocation() {
        if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val myLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 17f))
            } else {
                Toast.makeText(
                                requireContext(),
                                "No se pudo obtener tu ubicación actual",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }
    }

    private fun precargarHijos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = RetrofitClient.apiService.obtenerHijos("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    hijosCache = response.body()!!
                }
            } catch (e: Exception) {
                android.util.Log.e("MonitorFragment", "Error precargando hijos: ${e.message}")
            }
        }
    }

    private fun cargarConfirmacionesYViajes() {
        progressBar.visibility = View.VISIBLE
        tvEmptyView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch

                try {
                    val confResponse =
                            RetrofitClient.apiService.getMisConfirmacionesList("Bearer $token")
                    if (confResponse.isSuccessful && confResponse.body() != null) {
                        confirmacionesCache = confResponse.body()!!
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                            "MonitorFragment",
                            "Error cargando confirmaciones: ${e.message}"
                    )
                }

                try {
                    val activosResponse =
                            RetrofitClient.apiService.getMisViajesActivos("Bearer $token")
                    if (activosResponse.isSuccessful && activosResponse.body() != null) {
                        viajesActivosCache = activosResponse.body()!!
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                            "MonitorFragment",
                            "Error cargando viajes activos: ${e.message}"
                    )
                }

                val response = RetrofitClient.apiService.getViajesDisponiblesRaw("Bearer $token", 1)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    val gson = com.google.gson.Gson()
                    val viajesJson =
                            if (body.isJsonObject && body.asJsonObject.has("data")) {
                                body.asJsonObject.get("data")
                            } else {
                                body
                            }

                    try {
                        val viajesArray =
                                gson.fromJson(viajesJson, Array<ViajeDisponible>::class.java)
                        var viajes = viajesArray.toMutableList()

                        val idsExistentes = viajes.map { it.id }.toSet()
                        val viajesActivosConvertidos =
                                viajesActivosCache.filter { it.id !in idsExistentes }.map { activo
                                    ->
                                    ViajeDisponible(
                                            id = activo.id,
                                            nombre = activo.nombre,
                                            tipo_viaje = activo.tipo_viaje,
                                            turno = activo.turno,
                                            estado = activo.estado,
                                            hora_salida_programada = activo.hora_salida_programada,
                                            confirmaciones_actuales =
                                                    activo.confirmaciones_actuales,
                                            cupo_maximo = activo.cupo_maximo,
                                            escuela = activo.escuela,
                                            chofer = null,
                                            unidad = null,
                                            fecha_viaje = null
                                    )
                                }
                        viajes.addAll(0, viajesActivosConvertidos)

                        val viajesEnCursoIds =
                                viajesActivosCache
                                        .filter {
                                            it.estado in
                                                    listOf(
                                                            "en_curso",
                                                            "confirmado",
                                                            "ruta_generada"
                                                    )
                                        }
                                        .map { it.id }
                                        .toSet()

                        if (viajes.isEmpty()) {
                            tvEmptyView.visibility = View.VISIBLE
                            tvEmptyView.text = getString(R.string.no_trips_available)
                        } else {
                            val viajesConfirmados = confirmacionesCache.map { it.viaje_id }.toSet()

                            val adapter =
                                    ViajesDisponiblesAdapter(
                                            viajes,
                                            viajesConfirmados,
                                            viajesEnCursoIds
                                    ) { viaje ->
                                        if (viaje.id in viajesEnCursoIds) {
                                            val activo =
                                                    viajesActivosCache.find { it.id == viaje.id }
                                            if (activo?.ruta_id != null) {
                                                abrirTracking(activo.ruta_id, viaje.id)
                                            } else {
                                                mostrarMapaParaViaje(viaje)
                                            }
                                        } else {
                                            mostrarMapaParaViaje(viaje)
                                        }
                                    }
                            recyclerView.adapter = adapter
                        }
                    } catch (ex: Exception) {
                        android.util.Log.e(
                                "MonitorFragment",
                                "Error parseando viajes: ${ex.message}"
                        )
                        tvEmptyView.visibility = View.VISIBLE
                        tvEmptyView.text = getString(R.string.error_loading_trips)
                    }
                } else {
                    tvEmptyView.visibility = View.VISIBLE
                    tvEmptyView.text = getString(R.string.error_loading_trips)
                }
            } catch (e: Exception) {
                if (!isAdded) return@launch
                tvEmptyView.visibility = View.VISIBLE
                tvEmptyView.text = "Error: ${e.message}"
                val ctx = context ?: return@launch
                Toast.makeText(ctx, getString(R.string.error_loading_trips), Toast.LENGTH_SHORT)
                        .show()
            } finally {
                if (isAdded) progressBar.visibility = View.GONE
            }
        }
    }

    private fun mostrarMapaParaViaje(viaje: ViajeDisponible) {
        selectedViaje = viaje
        isEditingLocation = false
        existingConfirmacion = null

        layoutViajesList.visibility = View.GONE
        bottomSheet.visibility = View.VISIBLE
        fabMiUbicacion.visibility = View.VISIBLE

        val confirmacionesDelViaje =
                confirmacionesCache.filter { conf -> conf.viaje_id == viaje.id }

        if (confirmacionesDelViaje.isNotEmpty()) {
            existingConfirmacion = confirmacionesDelViaje.first()
            mostrarConfirmacionesExistentes(confirmacionesDelViaje)
        } else {
            mostrarModoPinNuevo(viaje)
        }
    }

    private fun mostrarConfirmacionesExistentes(confirmaciones: List<ConfirmacionDetalle>) {
        centerPinContainer.visibility = View.GONE
        layoutYaConfirmado.visibility = View.VISIBLE
        tvInstruccion.text = "Punto de recogida confirmado"
        btnConfirmar.visibility = View.GONE

        tvHijoConfirmado.visibility = View.VISIBLE
        if (confirmaciones.size == 1) {
            val conf = confirmaciones.first()
            tvHijoConfirmado.text =
                    "👦 ${conf.hijo?.nombre ?: "Hijo"} • ${conf.direccion_formateada ?: "Sin dirección"}"
            tvDireccion.text = "📍 ${conf.direccion_formateada ?: "Ubicación guardada"}"
        } else {
            val resumen =
                    confirmaciones.joinToString("\n") { conf ->
                        "👦 ${conf.hijo?.nombre ?: "Hijo"} → ${conf.direccion_formateada ?: "Sin dirección"}"
                    }
            tvHijoConfirmado.text = resumen
            tvDireccion.text = "📍 ${confirmaciones.size} hijos confirmados en este viaje"
        }

        val firstConf = confirmaciones.first()
        googleMap?.let { map ->
            map.clear()
            for (conf in confirmaciones) {
                val lat = conf.latitud ?: continue
                val lng = conf.longitud ?: continue
                val location = LatLng(lat, lng)
                map.addMarker(
                        MarkerOptions()
                                .position(location)
                                .title(conf.hijo?.nombre ?: "Hijo")
                                .snippet(conf.direccion_formateada ?: "")
                )
            }
            val lat = firstConf.latitud
            val lng = firstConf.longitud
            if (lat != null && lng != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
            }
        }
    }

    private fun habilitarEdicionUbicacion() {
        val viaje = selectedViaje ?: return
        val confirmacionesDelViaje = confirmacionesCache.filter { it.viaje_id == viaje.id }

        if (confirmacionesDelViaje.size <= 1) {
            existingConfirmacion = confirmacionesDelViaje.firstOrNull()
            iniciarEdicionUbicacion()
        } else {
            val nombres =
                    confirmacionesDelViaje
                            .map { conf ->
                                "${conf.hijo?.nombre ?: "Hijo"} — ${conf.direccion_formateada ?: "Sin dirección"}"
                            }
                            .toTypedArray()

            AlertDialog.Builder(requireContext())
                    .setTitle("¿A cuál hijo le cambias la ubicación?")
                    .setItems(nombres) { dialog: DialogInterface, which: Int ->
                        existingConfirmacion = confirmacionesDelViaje[which]
                        iniciarEdicionUbicacion()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
        }
    }

    private fun iniciarEdicionUbicacion() {
        isEditingLocation = true
        centerPinContainer.visibility = View.VISIBLE
        layoutYaConfirmado.visibility = View.GONE
        val hijoNombre = existingConfirmacion?.hijo?.nombre ?: "Hijo"
        tvInstruccion.text = "Mueve el mapa para $hijoNombre"
        tvHijoConfirmado.visibility = View.GONE

        btnConfirmar.visibility = View.VISIBLE
        btnConfirmar.text = "Actualizar Ubicación de $hijoNombre"

        googleMap?.clear()
    }

    private fun abrirTracking(rutaId: Int, viajeId: Int) {
        val intent =
                Intent(
                                requireContext(),
                                com.example.trailynapp.ui.tracking.TrackingActivity::class.java
                        )
                        .apply {
                            putExtra(
                                    com.example.trailynapp.ui.tracking.TrackingActivity
                                            .EXTRA_RUTA_ID,
                                    rutaId
                            )
                            putExtra(
                                    com.example.trailynapp.ui.tracking.TrackingActivity
                                            .EXTRA_VIAJE_ID,
                                    viajeId
                            )
                        }
        startActivity(intent)
    }

    private fun mostrarModoPinNuevo(viaje: ViajeDisponible) {
        centerPinContainer.visibility = View.VISIBLE
        layoutYaConfirmado.visibility = View.GONE
        tvInstruccion.text = getString(R.string.move_map_instruction)
        tvHijoConfirmado.visibility = View.GONE
        btnConfirmar.visibility = View.VISIBLE
        btnConfirmar.text = getString(R.string.btn_confirm_trip)

        if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val myLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 17f))
                } else {
                    centrarEnEscuela(viaje)
                }
            }
        } else {
            centrarEnEscuela(viaje)
        }
    }

    private fun centrarEnEscuela(viaje: ViajeDisponible) {
        googleMap?.let { map ->
            viaje.escuela?.let { escuela ->
                if (escuela.latitud != null && escuela.longitud != null) {
                    val escuelaLocation = LatLng(escuela.latitud, escuela.longitud)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(escuelaLocation, 15f))
                }
            }
        }
    }

    private fun ocultarMapa() {
        layoutViajesList.visibility = View.VISIBLE
        bottomSheet.visibility = View.GONE
        centerPinContainer.visibility = View.GONE
        fabMiUbicacion.visibility = View.GONE
        layoutYaConfirmado.visibility = View.GONE
        tvHijoConfirmado.visibility = View.GONE
        selectedViaje = null
        selectedLocation = null
        selectedAddress = null
        existingConfirmacion = null
        isEditingLocation = false
        googleMap?.clear()
    }

    private fun obtenerDireccion(location: LatLng) {
        if (!isAdded) return
        progressBarAddress.visibility = View.VISIBLE
        tvDireccion.text = getString(R.string.getting_address)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val addresses =
                        withContext(Dispatchers.IO) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                suspendCancellableCoroutine { cont ->
                                    geocoder.getFromLocation(
                                            location.latitude,
                                            location.longitude,
                                            1
                                    ) { result -> cont.resume(result) }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                        ?: emptyList()
                            }
                        }
                if (!isAdded) return@launch
                progressBarAddress.visibility = View.GONE
                if (addresses.isNotEmpty()) {
                    val direccion =
                            addresses[0].getAddressLine(0)
                                    ?: getString(R.string.address_not_available)
                    selectedAddress = direccion
                    tvDireccion.text = getString(R.string.address_prefix, direccion)
                } else {
                    tvDireccion.text = getString(R.string.address_error)
                }
            } catch (_: Exception) {
                if (!isAdded) return@launch
                progressBarAddress.visibility = View.GONE
                tvDireccion.text = getString(R.string.address_fetch_error)
            }
        }
    }

    private fun confirmarViaje() {
        if (isEditingLocation && existingConfirmacion != null) {
            actualizarUbicacion()
            return
        }

        if (selectedLocation == null || selectedAddress == null || selectedViaje == null) {
            Toast.makeText(
                            requireContext(),
                            getString(R.string.select_valid_location),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        if (hijosCache.isEmpty()) {
            Toast.makeText(requireContext(), "No tienes hijos registrados", Toast.LENGTH_SHORT)
                    .show()
            return
        }

        if (hijosCache.size == 1) {
            enviarConfirmacion(listOf(hijosCache[0]))
        } else {
            mostrarSelectorMultiHijos()
        }
    }

    private fun mostrarSelectorMultiHijos() {
        val viaje = selectedViaje ?: return

        val hijosDisponibles =
                hijosCache.filter { hijo ->
                    confirmacionesCache.none { conf ->
                        conf.viaje_id == viaje.id && conf.hijo_id == hijo.id
                    }
                }

        if (hijosDisponibles.isEmpty()) {
            Toast.makeText(
                            requireContext(),
                            "Todos tus hijos ya están confirmados en este viaje",
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        val nombres =
                hijosDisponibles
                        .map { "${it.nombre} - ${it.grado ?: ""}° ${it.grupo ?: ""}" }
                        .toTypedArray()
        val seleccionados = BooleanArray(nombres.size) { true }

        android.app.AlertDialog.Builder(requireContext())
                .setTitle("¿A cuáles hijos confirmar?")
                .setMultiChoiceItems(nombres, seleccionados) { _, which, isChecked ->
                    seleccionados[which] = isChecked
                }
                .setPositiveButton("Confirmar") { _, _ ->
                    val hijosSeleccionados =
                            hijosDisponibles.filterIndexed { index, _ -> seleccionados[index] }
                    if (hijosSeleccionados.isEmpty()) {
                        Toast.makeText(
                                        requireContext(),
                                        "Selecciona al menos un hijo",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    } else {
                        enviarConfirmacion(hijosSeleccionados)
                    }
                }
                .setCancelable(true)
                .setNegativeButton("Cancelar", null)
                .show()
    }

    private fun enviarConfirmacion(hijos: List<Hijo>) {
        val location = selectedLocation ?: return
        val address = selectedAddress ?: return
        val viaje = selectedViaje ?: return

        btnConfirmar.isEnabled = false
        btnConfirmar.text = "Confirmando..."
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: throw Exception("No autenticado")
                var exitosos = 0
                var errores = mutableListOf<String>()

                for (hijo in hijos) {
                    try {
                        val request =
                                com.example.trailynapp.api.ConfirmacionRequest(
                                        hijo_id = hijo.id,
                                        direccion_recogida = address,
                                        latitud = location.latitude,
                                        longitud = location.longitude,
                                        referencia = null
                                )

                        val response =
                                RetrofitClient.apiService.confirmarViaje(
                                        viajeId = viaje.id,
                                        token = "Bearer $token",
                                        confirmacion = request
                                )

                        if (response.isSuccessful) {
                            exitosos++
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Error"
                            errores.add("${hijo.nombre}: $errorBody")
                        }
                    } catch (e: Exception) {
                        errores.add("${hijo.nombre}: ${e.message}")
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = getString(R.string.btn_confirm_trip)

                    if (exitosos > 0) {
                        val msg =
                                if (hijos.size == 1) "✓ Viaje confirmado"
                                else "✓ $exitosos de ${hijos.size} confirmaciones exitosas"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        ocultarMapa()
                        cargarConfirmacionesYViajes()
                    }
                    if (errores.isNotEmpty()) {
                        Toast.makeText(
                                        requireContext(),
                                        errores.joinToString("\n"),
                                        Toast.LENGTH_LONG
                                )
                                .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = getString(R.string.btn_confirm_trip)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }

    private fun actualizarUbicacion() {
        val conf = existingConfirmacion ?: return
        val location = selectedLocation ?: return
        val address = selectedAddress ?: return

        btnConfirmar.isEnabled = false
        btnConfirmar.text = "Actualizando..."
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: throw Exception("No autenticado")

                val request =
                        com.example.trailynapp.api.ConfirmacionRequest(
                                hijo_id = conf.hijo_id,
                                direccion_recogida = address,
                                latitud = location.latitude,
                                longitud = location.longitude,
                                referencia = null
                        )

                val response =
                        RetrofitClient.apiService.actualizarDireccionConfirmacion(
                                confirmacionId = conf.id,
                                token = "Bearer $token",
                                body = request
                        )

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true

                    if (response.isSuccessful) {
                        Toast.makeText(
                                        requireContext(),
                                        "✓ Ubicación actualizada",
                                        Toast.LENGTH_LONG
                                )
                                .show()
                        ocultarMapa()
                        cargarConfirmacionesYViajes()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                                        requireContext(),
                                        "Error: ${errorBody ?: "No se pudo actualizar"}",
                                        Toast.LENGTH_LONG
                                )
                                .show()
                        btnConfirmar.text = "Actualizar Ubicación"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = "Actualizar Ubicación"
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(
                        event.values,
                        0,
                        accelerometerReading,
                        0,
                        accelerometerReading.size
                )
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        updateOrientationAndRotateArrow()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateOrientationAndRotateArrow() {
        SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        currentAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (currentAzimuth < 0) currentAzimuth += 360f

        selectedLocation?.let { destination ->
            val map = googleMap ?: return
            if (!map.isMyLocationEnabled) return

            try {
                val myLoc = map.myLocation ?: return
                val currentLocation = LatLng(myLoc.latitude, myLoc.longitude)
                val bearing = calculateBearing(currentLocation, destination)

                var rotation = bearing - currentAzimuth
                if (rotation < 0) rotation += 360f
                if (rotation > 360) rotation -= 360f

                rotateArrow(rotation)
            } catch (_: Exception) {}
        }
    }

    private fun calculateBearing(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        return bearing.toFloat()
    }

    private fun rotateArrow(targetRotation: Float) {
        val rotateAnimation =
                RotateAnimation(
                        currentRotation,
                        targetRotation,
                        Animation.RELATIVE_TO_SELF,
                        0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f
                )
        rotateAnimation.duration = 200
        rotateAnimation.fillAfter = true
        navigationArrow.startAnimation(rotateAnimation)
        currentRotation = targetRotation
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.also { mag ->
            sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}

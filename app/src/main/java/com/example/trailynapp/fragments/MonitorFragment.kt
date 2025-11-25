package com.example.trailynapp.fragments

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.R
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.api.ViajeDisponible
import com.example.trailynapp.utils.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MonitorFragment : Fragment(), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var progressBarAddress: ProgressBar
    private lateinit var btnConfirmar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var bottomSheet: LinearLayout
    private lateinit var centerPinContainer: View
    private lateinit var layoutViajesList: LinearLayout
    
    private var googleMap: GoogleMap? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var geocoder: Geocoder
    
    private var selectedLocation: LatLng? = null
    private var selectedAddress: String? = null
    private var selectedViaje: ViajeDisponible? = null

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

        recyclerView = view.findViewById(R.id.recyclerViewViajes)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyView = view.findViewById(R.id.tvEmptyView)
        tvDireccion = view.findViewById(R.id.tvDireccion)
        progressBarAddress = view.findViewById(R.id.progressBarAddress)
        btnConfirmar = view.findViewById(R.id.btnConfirmar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        bottomSheet = view.findViewById(R.id.bottomSheet)
        centerPinContainer = view.findViewById(R.id.centerPinContainer)
        layoutViajesList = view.findViewById(R.id.layoutViajesList)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inicializar el mapa
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Botones
        btnCancelar.setOnClickListener {
            ocultarMapa()
        }

        btnConfirmar.setOnClickListener {
            confirmarViaje()
        }

        // Cargar los viajes disponibles
        cargarViajes()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            
            // Listener cuando el mapa se mueve
            setOnCameraIdleListener {
                val center = cameraPosition.target
                selectedLocation = center
                obtenerDireccion(center)
                
                // El PIN ya está visible con centerPinContainer (ImageView en el centro)
                // No necesitamos agregar marcador programáticamente
            }
        }
    }

    private fun cargarViajes() {
        progressBar.visibility = View.VISIBLE
        tvEmptyView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = RetrofitClient.apiService.getViajesDisponibles("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val viajes = response.body()!! // ahora es List<ViajeDisponible>

                    if (viajes.isEmpty()) {
                        tvEmptyView.visibility = View.VISIBLE
                        tvEmptyView.text = getString(R.string.no_trips_available)
                    } else {
                        val adapter = ViajesDisponiblesAdapter(viajes) { viaje ->
                            mostrarMapaParaViaje(viaje)
                        }
                        recyclerView.adapter = adapter
                    }
                } else {
                    tvEmptyView.visibility = View.VISIBLE
                    tvEmptyView.text = getString(R.string.error_loading_trips)
                }
            } catch (e: Exception) {
                tvEmptyView.visibility = View.VISIBLE
                tvEmptyView.text = "Error: ${e.message}"
                Toast.makeText(requireContext(), getString(R.string.error_loading_trips), Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun mostrarMapaParaViaje(viaje: ViajeDisponible) {
        selectedViaje = viaje
        
        // Ocultar lista y mostrar mapa
        layoutViajesList.visibility = View.GONE
        bottomSheet.visibility = View.VISIBLE
        centerPinContainer.visibility = View.VISIBLE
        
        // Centrar mapa en la escuela del viaje
        googleMap?.let { map ->
            viaje.escuela?.let { escuela ->
                if (escuela.latitud != null && escuela.longitud != null) {
                    val escuelaLocation = LatLng(
                        escuela.latitud,
                        escuela.longitud
                    )
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(escuelaLocation, 15f)
                    )
                }
            }
        }
    }

    private fun ocultarMapa() {
        layoutViajesList.visibility = View.VISIBLE
        bottomSheet.visibility = View.GONE
        centerPinContainer.visibility = View.GONE
        selectedViaje = null
        selectedLocation = null
        selectedAddress = null
    }

    private fun obtenerDireccion(location: LatLng) {
        progressBarAddress.visibility = View.VISIBLE
        tvDireccion.text = getString(R.string.getting_address)

        lifecycleScope.launch {
            try {
                // minSdk = 34, siempre usamos la nueva API
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->
                    activity?.runOnUiThread {
                        progressBarAddress.visibility = View.GONE
                        if (addresses.isNotEmpty()) {
                            val direccion = addresses[0].getAddressLine(0) ?: getString(R.string.address_not_available)
                            selectedAddress = direccion
                            tvDireccion.text = getString(R.string.address_prefix, direccion)
                        } else {
                            tvDireccion.text = getString(R.string.address_error)
                        }
                    }
                }
            } catch (_: Exception) {
                progressBarAddress.visibility = View.GONE
                tvDireccion.text = getString(R.string.address_fetch_error)
            }
        }
    }

    private fun confirmarViaje() {
        if (selectedLocation == null || selectedAddress == null || selectedViaje == null) {
            Toast.makeText(requireContext(), getString(R.string.select_valid_location), Toast.LENGTH_SHORT).show()
            return
        }

        val location = selectedLocation ?: return
        val address = selectedAddress ?: return
        val viaje = selectedViaje ?: return
        
        // Obtener primer hijo (o mostrar diálogo para seleccionar)
        val hijoId = obtenerHijoId()
        if (hijoId == null) {
            Toast.makeText(requireContext(), "No tienes hijos registrados", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón mientras se procesa
        btnConfirmar.isEnabled = false
        btnConfirmar.text = "Confirmando..."
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: throw Exception("No autenticado")
                
                val request = com.example.trailynapp.api.ConfirmacionRequest(
                    hijo_id = hijoId,
                    direccion_recogida = address,
                    latitud = location.latitude,
                    longitud = location.longitude,
                    referencia = null
                )
                
                val response = RetrofitClient.apiService.confirmarViaje(
                    viajeId = viaje.id,
                    token = "Bearer $token",
                    confirmacion = request
                )
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = "Confirmar"
                    
                    if (response.isSuccessful && response.body() != null) {
                        val result = response.body()!!
                        Toast.makeText(
                            requireContext(),
                            "✓ ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        ocultarMapa()
                        cargarViajes() // Recargar lista
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            requireContext(),
                            "Error: ${errorBody ?: "No se pudo confirmar"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = "Confirmar"
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun obtenerHijoId(): Int? {
        // Obtener hijo previamente seleccionado de SharedPreferences
        val savedHijoId = sessionManager.getSelectedHijoId()
        if (savedHijoId != -1) {
            return savedHijoId
        }
        
        // Si no hay hijo guardado, cargar desde API y mostrar selector
        cargarYSeleccionarHijo()
        return null // Retornar null para que el flujo espere selección
    }
    
    private fun cargarYSeleccionarHijo() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = RetrofitClient.apiService.obtenerHijos("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val hijos = response.body()!!
                    
                    when {
                        hijos.isEmpty() -> {
                            Toast.makeText(requireContext(), "No tienes hijos registrados", Toast.LENGTH_LONG).show()
                        }
                        hijos.size == 1 -> {
                            // Si solo tiene un hijo, guardarlo automáticamente
                            sessionManager.saveSelectedHijoId(hijos[0].id)
                            Toast.makeText(requireContext(), "Hijo seleccionado: ${hijos[0].nombre}", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Mostrar selector si tiene múltiples hijos
                            mostrarSelectorHijos(hijos)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al cargar hijos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun mostrarSelectorHijos(hijos: List<com.example.trailynapp.api.Hijo>) {
        val nombres = hijos.map { "${it.nombre} - ${it.grado}° ${it.grupo}" }.toTypedArray()
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Selecciona un hijo")
            .setItems(nombres) { _, which ->
                val hijoSeleccionado = hijos[which]
                sessionManager.saveSelectedHijoId(hijoSeleccionado.id)
                Toast.makeText(
                    requireContext(),
                    "Hijo seleccionado: ${hijoSeleccionado.nombre}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setCancelable(true)
            .show()
    }
}

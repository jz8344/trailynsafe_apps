package com.example.trailynapp.ui.tracking

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.R
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.api.TrackingResponse
import com.example.trailynapp.api.TrackingUbicacion
import com.example.trailynapp.utils.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_RUTA_ID = "ruta_id"
        const val EXTRA_VIAJE_ID = "viaje_id"
        private const val REFRESH_INTERVAL = 5000L
    }

    private var googleMap: GoogleMap? = null
    private var rutaId: Int = 0
    private var viajeId: Int = 0
    private lateinit var sessionManager: SessionManager

    private lateinit var tvEstado: TextView
    private lateinit var tvVelocidad: TextView
    private lateinit var tvChofer: TextView
    private lateinit var tvParadaActual: TextView
    private lateinit var tvEscuela: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvActualizacion: TextView
    private lateinit var tvSaludChofer: TextView

    private var driverMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var stopMarkers = mutableListOf<Marker>()
    private var isFirstLoad = true

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable =
            object : Runnable {
                override fun run() {
                    loadTrackingData()
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL)
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        rutaId = intent.getIntExtra(EXTRA_RUTA_ID, 0)
        viajeId = intent.getIntExtra(EXTRA_VIAJE_ID, 0)
        sessionManager = SessionManager(this)

        tvEstado = findViewById(R.id.tvTrackingEstado)
        tvVelocidad = findViewById(R.id.tvTrackingVelocidad)
        tvChofer = findViewById(R.id.tvTrackingChofer)
        tvParadaActual = findViewById(R.id.tvTrackingParadaActual)
        tvEscuela = findViewById(R.id.tvTrackingEscuela)
        tvDistancia = findViewById(R.id.tvTrackingDistancia)
        tvActualizacion = findViewById(R.id.tvTrackingActualizacion)
        tvSaludChofer = findViewById(R.id.tvSaludChofer)

        findViewById<FloatingActionButton>(R.id.fabBackTracking).setOnClickListener { finish() }

        val mapFragment =
                supportFragmentManager.findFragmentById(R.id.mapTracking) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false

        try {
            // Disabled dark theme style as standard resource isn't present
            // map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))
        } catch (_: Exception) {}

        loadTrackingData()
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL)
    }

    private fun loadTrackingData() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = RetrofitClient.apiService.getTrackingRuta(rutaId, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    updateUI(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error"
                    if (isFirstLoad) {
                        Toast.makeText(
                                        this@TrackingActivity,
                                        "Error cargando tracking: $errorMsg",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            } catch (e: Exception) {
                if (isFirstLoad) {
                    Toast.makeText(this@TrackingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                }
            }
            isFirstLoad = false
        }
    }

    private fun updateUI(data: TrackingResponse) {
        tvChofer.text = "👨‍✈️ ${data.chofer?.nombre ?: "Chofer"}"
        tvEscuela.text = "🏫 Destino: ${data.viaje?.escuela ?: "Escuela"}"

        data.ruta?.let { ruta ->
            val distancia = ruta.distancia_total_km?.let { String.format("%.1f km", it) } ?: "N/A"
            val tiempo = ruta.tiempo_estimado_min?.let { "${it.toInt()} min" } ?: ""
            tvDistancia.text = "📏 $distancia • $tiempo"
        }

        data.parada_actual?.let { parada ->
            tvParadaActual.text =
                    "📍 Próxima: ${parada.hijo ?: "Parada"} - ${parada.direccion ?: ""}"
            parada.hora_estimada?.let { tvParadaActual.append(" (ETA $it)") }
        }
                ?: run { tvParadaActual.text = "📍 Sin parada pendiente" }

        data.ubicacion_actual?.let { ubicacion ->
            tvEstado.text = "🚐 En camino"
            val vel = ubicacion.velocidad_kmh?.let { String.format("%.0f km/h", it) } ?: "0 km/h"
            tvVelocidad.text = vel
            tvActualizacion.text =
                    if (ubicacion.actualizada == true) "📡 En vivo"
                    else "⏳ Última señal: ${ubicacion.timestamp ?: "?"}"

            if (ubicacion.frecuencia_cardiaca != null) {
                val bpm = ubicacion.frecuencia_cardiaca
                val estado = ubicacion.estado_salud ?: "Normal"
                tvSaludChofer.text = "❤️ $bpm BPM · $estado"
                tvSaludChofer.setTextColor(
                        when {
                            bpm < 60 -> Color.parseColor("#2196F3")   // azul: bradicardia
                            bpm <= 100 -> Color.parseColor("#4CAF50")  // verde: normal
                            bpm <= 130 -> Color.parseColor("#FF9800")  // naranja: elevado
                            else -> Color.parseColor("#F44336")        // rojo: taquicardia
                        }
                )
                tvSaludChofer.visibility = View.VISIBLE
            } else {
                tvSaludChofer.visibility = View.GONE
            }

            updateDriverMarker(ubicacion)
        }
                ?: run {
                    tvEstado.text = "⏳ Esperando señal GPS"
                    tvVelocidad.text = "--"
                    tvActualizacion.text = "Sin ubicación reciente"
                    tvSaludChofer.visibility = View.GONE
                }

        if (routePolyline == null) {
            data.ruta?.polyline?.let { encoded -> drawRoute(encoded) }
        }

        if (stopMarkers.isEmpty()) {
            data.paradas?.forEach { parada ->
                val lat = parada.latitud ?: return@forEach
                val lng = parada.longitud ?: return@forEach
                val pos = LatLng(lat, lng)

                val hue =
                        when (parada.estado) {
                            "completada" -> BitmapDescriptorFactory.HUE_GREEN
                            "en_camino" -> BitmapDescriptorFactory.HUE_ORANGE
                            else -> BitmapDescriptorFactory.HUE_RED
                        }

                val marker =
                        googleMap?.addMarker(
                                MarkerOptions()
                                        .position(pos)
                                        .title("${parada.orden}. ${parada.hijo ?: "Parada"}")
                                        .snippet(parada.direccion ?: "")
                                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        )
                marker?.let { stopMarkers.add(it) }
            }

            // Dibujar el pin de destino (la escuela)
            data.viaje?.let { viaje ->
                if (viaje.latitud_destino != null && viaje.longitud_destino != null) {
                    val destPos = LatLng(viaje.latitud_destino, viaje.longitud_destino)
                    val marker =
                            googleMap?.addMarker(
                                    MarkerOptions()
                                            .position(destPos)
                                            .title("🏫 Destino: ${viaje.escuela ?: "Escuela"}")
                                            .snippet(viaje.direccion_destino ?: "")
                                            .icon(
                                                    BitmapDescriptorFactory.defaultMarker(
                                                            BitmapDescriptorFactory.HUE_VIOLET
                                                    )
                                            )
                            )
                    marker?.let { stopMarkers.add(it) }
                }
            }
        }
    }

    private fun updateDriverMarker(ubicacion: TrackingUbicacion) {
        val newPos = LatLng(ubicacion.latitud, ubicacion.longitud)

        if (driverMarker == null) {
            driverMarker =
                    googleMap?.addMarker(
                            MarkerOptions()
                                    .position(newPos)
                                    .title("Transporte escolar")
                                    .icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                    BitmapDescriptorFactory.HUE_BLUE
                                            )
                                    )
                                    .flat(true)
                                    .anchor(0.5f, 0.5f)
                    )
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(newPos, 16f))
        } else {
            val oldPos = driverMarker!!.position
            animateMarker(oldPos, newPos)
            ubicacion.heading?.let { driverMarker?.rotation = it.toFloat() }
        }
    }

    private fun animateMarker(start: LatLng, end: LatLng) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1500
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val t = animation.animatedFraction
            val lat = start.latitude + (end.latitude - start.latitude) * t
            val lng = start.longitude + (end.longitude - start.longitude) * t
            driverMarker?.position = LatLng(lat, lng)
        }
        animator.start()
    }

    private fun drawRoute(encodedPolyline: String) {
        try {
            val points = decodePolyline(encodedPolyline)
            routePolyline =
                    googleMap?.addPolyline(
                            PolylineOptions()
                                    .addAll(points)
                                    .color(Color.parseColor("#4285F4"))
                                    .width(10f)
                                    .geodesic(true)
                    )
        } catch (e: Exception) {
            android.util.Log.e("TrackingActivity", "Error decodificando polyline: ${e.message}")
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onDestroy()
    }
}

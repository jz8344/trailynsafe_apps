package com.example.trailynapp.driver.ui.trips

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.api.RetrofitClient
import com.example.trailynapp.driver.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment para mostrar viajes del chofer con lógica tipo alarma
 * 
 * - Separa viajes en "HOY" y "OTROS"
 * - Muestra estado efectivo calculado en tiempo real
 * - Auto-refresca cada 30 segundos para actualizar estados
 */
class TripsFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var rvViajesHoy: RecyclerView
    private lateinit var rvViajesOtros: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvFechaHoy: TextView
    private lateinit var tvSeccionHoy: TextView
    private lateinit var tvSeccionOtros: TextView
    
    private lateinit var tripsAdapterHoy: TripsAdapter
    private lateinit var tripsAdapterOtros: TripsAdapter
    
    // Handler para auto-refresh cada 30 segundos
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadTrips(silent = true)
            refreshHandler.postDelayed(this, 30_000) // 30 segundos
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trips, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        
        // Inicializar vistas
        // Nota: Necesitarás agregar estos IDs al layout o usar los existentes
        rvViajesHoy = view.findViewById(R.id.rvViajes) // RecyclerView principal para viajes de hoy
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
        
        // Configurar RecyclerView para viajes de HOY
        rvViajesHoy.layoutManager = LinearLayoutManager(requireContext())
        tripsAdapterHoy = TripsAdapter { viaje ->
            openTripDetail(viaje)
        }
        rvViajesHoy.adapter = tripsAdapterHoy
        
        // Configurar swipe refresh
        swipeRefresh.setOnRefreshListener {
            loadTrips()
        }
        
        // Cargar viajes inicial
        loadTrips()
    }
    
    override fun onResume() {
        super.onResume()
        // Iniciar auto-refresh cuando el fragment está visible
        refreshHandler.postDelayed(refreshRunnable, 30_000)
        // Recargar al volver al fragment
        loadTrips(silent = true)
    }
    
    override fun onPause() {
        super.onPause()
        // Detener auto-refresh cuando el fragment no está visible
        refreshHandler.removeCallbacks(refreshRunnable)
    }
    
    private fun loadTrips(silent: Boolean = false) {
        val token = sessionManager.getToken() ?: return
        
        if (!silent && !swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        
        if (!silent) {
            rvViajesHoy.visibility = View.GONE
            emptyState.visibility = View.GONE
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getViajesChofer("Bearer $token")
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful && response.body() != null) {
                        val viajesResponse = response.body()!!
                        val viajesHoy = viajesResponse.viajes_hoy
                        val viajesOtros = viajesResponse.viajes_otros
                        
                        // Log para debug
                        android.util.Log.d("TripsFragment", """
                            📅 ${viajesResponse.dia_nombre} ${viajesResponse.fecha_actual} ${viajesResponse.hora_actual}
                            🚌 Viajes HOY: ${viajesHoy.size}
                            📋 Viajes OTROS: ${viajesOtros.size}
                        """.trimIndent())
                        
                        if (viajesHoy.isEmpty() && viajesOtros.isEmpty()) {
                            emptyState.visibility = View.VISIBLE
                        } else {
                            rvViajesHoy.visibility = View.VISIBLE
                            
                            // Mostrar viajes de HOY (priorizados)
                            // Si no hay viajes hoy, mostrar los otros
                            val viajesAMostrar = if (viajesHoy.isNotEmpty()) viajesHoy else viajesOtros
                            tripsAdapterHoy.submitList(viajesAMostrar)
                            
                            // Mostrar toast si hay viajes interactuables
                            val viajesInteractuables = viajesHoy.filter { it.interactuable }
                            if (viajesInteractuables.isNotEmpty() && !silent) {
                                Toast.makeText(
                                    requireContext(),
                                    "🟢 ${viajesInteractuables.size} viaje(s) listos para interactuar",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        if (!silent) {
                            Toast.makeText(
                                requireContext(),
                                "Error al cargar viajes",
                                Toast.LENGTH_SHORT
                            ).show()
                            emptyState.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    if (!silent) {
                        Toast.makeText(
                            requireContext(),
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        emptyState.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    
    private fun openTripDetail(viaje: Viaje) {
        val intent = Intent(requireContext(), TripDetailActivity::class.java)
        intent.putExtra("VIAJE_ID", viaje.id)
        intent.putExtra("ESTADO_EFECTIVO", viaje.estado)
        intent.putExtra("INTERACTUABLE", viaje.interactuable)
        startActivity(intent)
    }
}

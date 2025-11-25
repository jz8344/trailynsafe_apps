package com.example.trailynapp.driver.ui.trips

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
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

class TripsFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var rvViajes: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tripsAdapter: TripsAdapter
    
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
        rvViajes = view.findViewById(R.id.rvViajes)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
        
        // Configurar RecyclerView
        rvViajes.layoutManager = LinearLayoutManager(requireContext())
        tripsAdapter = TripsAdapter { viaje ->
            openTripDetail(viaje)
        }
        rvViajes.adapter = tripsAdapter
        
        // Configurar swipe refresh
        swipeRefresh.setOnRefreshListener {
            loadTrips()
        }
        
        // Cargar viajes inicial
        loadTrips()
    }
    
    private fun loadTrips() {
        val token = sessionManager.getToken() ?: return
        
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        rvViajes.visibility = View.GONE
        emptyState.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getViajesChofer("Bearer $token")
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful && response.body() != null) {
                        val viajes = response.body()!!
                        if (viajes.isEmpty()) {
                            emptyState.visibility = View.VISIBLE
                        } else {
                            rvViajes.visibility = View.VISIBLE
                            tripsAdapter.submitList(viajes)
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error al cargar viajes",
                            Toast.LENGTH_SHORT
                        ).show()
                        emptyState.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
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
    
    private fun openTripDetail(viaje: Viaje) {
        val intent = Intent(requireContext(), TripDetailActivity::class.java)
        intent.putExtra("VIAJE_ID", viaje.id)
        startActivity(intent)
    }
}

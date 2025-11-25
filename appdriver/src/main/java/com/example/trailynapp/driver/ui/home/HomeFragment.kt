package com.example.trailynapp.driver.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.trailynapp.driver.MainActivity
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.utils.SessionManager

class HomeFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        
        // Cargar datos del chofer
        val tvNombreChofer = view.findViewById<TextView>(R.id.tvNombreChofer)
        val nombre = sessionManager.getChoferNombre()
        val apellidos = sessionManager.getChoferApellidos()
        tvNombreChofer.text = "$nombre $apellidos"
        
        // Configurar accesos rápidos
        val cardMisViajes = view.findViewById<CardView>(R.id.cardMisViajes)
        val cardMiPerfil = view.findViewById<CardView>(R.id.cardMiPerfil)
        
        cardMisViajes.setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )?.selectedItemId = R.id.nav_trips
        }
        
        cardMiPerfil.setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )?.selectedItemId = R.id.nav_profile
        }
    }
}

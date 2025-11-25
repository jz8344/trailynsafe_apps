package com.example.trailynapp.driver.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.trailynapp.driver.MainActivity
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.utils.SessionManager
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        
        // Cargar datos del perfil
        loadProfileData(view)
        
        // Configurar botón de logout
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }
    
    private fun loadProfileData(view: View) {
        val nombre = sessionManager.getChoferNombre()
        val apellidos = sessionManager.getChoferApellidos()
        val correo = sessionManager.getChoferCorreo()
        val telefono = sessionManager.getChoferTelefono()
        
        view.findViewById<TextView>(R.id.tvNombreChofer).text = "$nombre $apellidos"
        view.findViewById<TextView>(R.id.tvEmailChofer).text = correo
        view.findViewById<TextView>(R.id.tvTelefono).text = telefono.ifEmpty { "No registrado" }
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                (activity as? MainActivity)?.logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

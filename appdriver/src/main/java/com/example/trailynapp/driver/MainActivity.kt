package com.example.trailynapp.driver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.trailynapp.driver.ui.auth.LoginActivity
import com.example.trailynapp.driver.ui.home.HomeFragment
import com.example.trailynapp.driver.ui.profile.ProfileFragment
import com.example.trailynapp.driver.ui.trips.TripsFragment
import com.example.trailynapp.driver.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sessionManager = SessionManager(this)
        
        // Verificar si hay sesión activa
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        // Inicializar vistas
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Cargar fragment inicial
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
        
        // Configurar navegación
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_trips -> {
                    loadFragment(TripsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    fun logout() {
        sessionManager.logout()
        navigateToLogin()
    }
}

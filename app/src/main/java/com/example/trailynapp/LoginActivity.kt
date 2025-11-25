package com.example.trailynapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.api.GoogleAuthRequest
import com.example.trailynapp.api.LoginRequest
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.auth.GoogleAuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: ImageButton
    private lateinit var progressBar: LinearProgressIndicator
    
    private lateinit var googleAuthManager: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        googleAuthManager = GoogleAuthManager(this)

        // Inicializar vistas
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        progressBar = findViewById(R.id.progressBar)

        // Botón de login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                login(email, password)
            }
        }

        // Navegar a registro
        findViewById<View>(R.id.registerContainer).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        // Olvidé mi contraseña
        findViewById<View>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, com.example.trailynapp.ui.auth.RecuperarPasswordActivity::class.java))
        }

        // Botón de Google Sign-In
        btnGoogle.setOnClickListener {
            performGoogleSignIn()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Ingresa tu contraseña", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun login(email: String, password: String) {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = LoginRequest(email, password)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    
                    // Guardar sesión
                    val sessionManager = com.example.trailynapp.utils.SessionManager(this@LoginActivity)
                    sessionManager.saveLoginSession(
                        token = loginResponse.token,
                        userId = loginResponse.usuario.id,
                        nombre = loginResponse.usuario.nombre,
                        apellidos = loginResponse.usuario.apellidos,
                        correo = loginResponse.usuario.correo,
                        telefono = loginResponse.usuario.telefono
                    )

                    Toast.makeText(this@LoginActivity, "¡Bienvenido ${loginResponse.usuario.nombre}!", Toast.LENGTH_SHORT).show()
                    
                    // Navegar a MainActivity
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales inválidas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
            }
        }
    }
    
    /**
     * Inicia el flujo de autenticación con Google
     */
    private fun performGoogleSignIn() {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        btnGoogle.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Primero intentar con cuentas autorizadas (usuarios recurrentes)
                val result = googleAuthManager.signInWithGoogle(
                    filterByAuthorizedAccounts = true,
                    autoSelectEnabled = true
                )
                
                handleGoogleAuthResult(result)
                
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error en Google Sign-In", e)
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(
                    this@LoginActivity,
                    "Error al iniciar sesión con Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Maneja el resultado de la autenticación con Google
     */
    private fun handleGoogleAuthResult(result: GoogleAuthManager.GoogleAuthResult) {
        when (result) {
            is GoogleAuthManager.GoogleAuthResult.Success -> {
                // Enviar el ID token al backend
                sendGoogleTokenToBackend(result.idToken)
            }
            
            is GoogleAuthManager.GoogleAuthResult.Error -> {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                btnGoogle.isEnabled = true
                
                // Si no hay cuentas autorizadas, mostrar todas las cuentas
                if (result.message.contains("No credentials available") || 
                    result.message.contains("no credentials")) {
                    retryGoogleSignInWithAllAccounts()
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            is GoogleAuthManager.GoogleAuthResult.Cancelled -> {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Reintenta el sign-in mostrando todas las cuentas (para nuevos usuarios)
     */
    private fun retryGoogleSignInWithAllAccounts() {
        lifecycleScope.launch {
            try {
                val result = googleAuthManager.signInWithGoogle(
                    filterByAuthorizedAccounts = false,
                    autoSelectEnabled = false
                )
                handleGoogleAuthResult(result)
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error en retry Google Sign-In", e)
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(
                    this@LoginActivity,
                    "Error al iniciar sesión con Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Envía el ID token de Google al backend para validación y autenticación
     */
    private fun sendGoogleTokenToBackend(idToken: String) {
        lifecycleScope.launch {
            try {
                val request = GoogleAuthRequest(
                    id_token = idToken,
                    device_name = "android-${android.os.Build.MODEL}"
                )
                
                val response = RetrofitClient.apiService.loginWithGoogle(request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()?.data
                    
                    if (authData != null) {
                        // Guardar sesión
                        val sessionManager = com.example.trailynapp.utils.SessionManager(this@LoginActivity)
                        sessionManager.saveLoginSession(
                            token = authData.token,
                            userId = authData.usuario.id,
                            nombre = authData.usuario.nombre,
                            apellidos = authData.usuario.apellido ?: "",
                            correo = authData.usuario.correo,
                            telefono = authData.usuario.telefono ?: "",
                            authProvider = authData.usuario.auth_provider ?: "google",
                            googleId = authData.usuario.google_id
                        )
                        
                        // Mostrar mensaje de bienvenida
                        val mensaje = if (response.body()?.is_new_user == true) {
                            "¡Bienvenido, ${authData.usuario.nombre}!"
                        } else {
                            "Bienvenido de nuevo, ${authData.usuario.nombre}!"
                        }
                        
                        Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_SHORT).show()
                        
                        // Navegar a MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        showError("Error al obtener datos del usuario")
                    }
                } else {
                    val errorMessage = response.body()?.message ?: "Error desconocido"
                    showError(errorMessage)
                }
                
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error enviando token al backend", e)
                showError("Error de conexión con el servidor")
            } finally {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                btnGoogle.isEnabled = true
            }
        }
    }
    
    /**
     * Muestra un mensaje de error
     */
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
        btnGoogle.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

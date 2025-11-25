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
import com.example.trailynapp.api.RegisterRequest
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.auth.GoogleAuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellidos: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoogle: ImageButton
    private lateinit var progressBar: LinearProgressIndicator
    
    private lateinit var googleAuthManager: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        googleAuthManager = GoogleAuthManager(this)

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre)
        etApellidos = findViewById(R.id.etApellidos)
        etTelefono = findViewById(R.id.etTelefono)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoogle = findViewById(R.id.btnGoogle)
        progressBar = findViewById(R.id.progressBar)

        // Botón de registro
        btnRegister.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(nombre, apellidos, telefono, email, password)) {
                register(nombre, apellidos, telefono, email, password)
            }
        }

        // Navegar a login
        findViewById<View>(R.id.loginContainer).setOnClickListener {
            finish()
        }

        // Botón de Google Sign-In
        btnGoogle.setOnClickListener {
            performGoogleSignIn()
        }
    }

    private fun validateInput(nombre: String, apellidos: String, telefono: String, email: String, password: String): Boolean {
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa tu nombre", Toast.LENGTH_SHORT).show()
            return false
        }
        if (apellidos.isEmpty()) {
            Toast.makeText(this, "Ingresa tus apellidos", Toast.LENGTH_SHORT).show()
            return false
        }
        if (telefono.isEmpty()) {
            Toast.makeText(this, "Ingresa tu teléfono", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun register(nombre: String, apellidos: String, telefono: String, email: String, password: String) {
        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = RegisterRequest(nombre, apellidos, telefono, email, password)
                val response = RetrofitClient.apiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@RegisterActivity, "¡Registro exitoso! Inicia sesión", Toast.LENGTH_SHORT).show()
                    finish() // Volver a login
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@RegisterActivity, "Error: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true
            }
        }
    }
    
    /**
     * Inicia el flujo de autenticación con Google
     */
    private fun performGoogleSignIn() {
        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false
        btnGoogle.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Primero intentar con cuentas autorizadas
                val result = googleAuthManager.signInWithGoogle(
                    filterByAuthorizedAccounts = true,
                    autoSelectEnabled = true
                )
                
                handleGoogleAuthResult(result)
                
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error en Google Sign-In", e)
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(
                    this@RegisterActivity,
                    "Error al registrar con Google",
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
                sendGoogleTokenToBackend(result.idToken)
            }
            
            is GoogleAuthManager.GoogleAuthResult.Error -> {
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true
                btnGoogle.isEnabled = true
                
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
                btnRegister.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(this, "Registro cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Reintenta el sign-in mostrando todas las cuentas
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
                Log.e("RegisterActivity", "Error en retry Google Sign-In", e)
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(
                    this@RegisterActivity,
                    "Error al registrar con Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Envía el ID token de Google al backend
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
                        val sessionManager = com.example.trailynapp.utils.SessionManager(this@RegisterActivity)
                        sessionManager.saveLoginSession(
                            token = authData.token,
                            userId = authData.usuario.id,
                            nombre = authData.usuario.nombre,
                            apellidos = authData.usuario.apellido ?: "",
                            correo = authData.usuario.correo,
                            telefono = authData.usuario.telefono ?: ""
                        )
                        
                        Toast.makeText(
                            this@RegisterActivity,
                            "¡Bienvenido, ${authData.usuario.nombre}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
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
                Log.e("RegisterActivity", "Error enviando token al backend", e)
                showError("Error de conexión con el servidor")
            } finally {
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true
                btnGoogle.isEnabled = true
            }
        }
    }
    
    /**
     * Muestra un mensaje de error
     */
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        btnRegister.isEnabled = true
        btnGoogle.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

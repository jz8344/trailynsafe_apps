package com.example.trailynapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.MainActivity
import com.example.trailynapp.R
import com.example.trailynapp.api.GoogleAuthRequest
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.auth.GoogleAuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: ImageButton
    private lateinit var tvForgotPassword: MaterialTextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorCard: MaterialCardView
    private lateinit var registerContainer: View
    
    private lateinit var googleAuthManager: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        googleAuthManager = GoogleAuthManager(this)
        
        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)
        errorCard = findViewById(R.id.errorCard)
        registerContainer = findViewById(R.id.registerContainer)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }
        
        btnGoogle.setOnClickListener {
            performGoogleSignIn()
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Funcionalidad próximamente", Toast.LENGTH_SHORT).show()
        }

        registerContainer.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validar email
        if (email.isEmpty()) {
            tilEmail.error = "El correo es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Correo inválido"
            isValid = false
        } else {
            tilEmail.error = null
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es requerida"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Mínimo 6 caracteres"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin() {
        // Ocultar teclado
        currentFocus?.let { view ->
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        // Mostrar loading
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        errorCard.visibility = View.GONE

        // Simular login (aquí integrarías tu backend)
        etEmail.postDelayed({
            // Login exitoso
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 1500)
    }
    
    /**
     * Inicia el flujo de autenticación con Google
     */
    private fun performGoogleSignIn() {
        // Mostrar loading
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        btnGoogle.isEnabled = false
        errorCard.visibility = View.GONE
        
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
                if (result.message.contains("No credentials available")) {
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
                        // Guardar sesión con información de Google
                        val sessionManager = com.example.trailynapp.utils.SessionManager(this@LoginActivity)
                        sessionManager.saveLoginSession(
                            token = authData.token,
                            userId = authData.usuario.id,
                            nombre = authData.usuario.nombre,
                            apellidos = authData.usuario.apellidos ?: authData.usuario.apellido ?: "",
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

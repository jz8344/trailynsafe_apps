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

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilApellidos: TextInputLayout
    private lateinit var tilTelefono: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellidos: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoogle: ImageButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorCard: MaterialCardView
    private lateinit var successCard: MaterialCardView
    private lateinit var tvError: MaterialTextView
    private lateinit var loginContainer: View
    
    private lateinit var googleAuthManager: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        googleAuthManager = GoogleAuthManager(this)
        
        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilNombre = findViewById(R.id.tilNombre)
        tilApellidos = findViewById(R.id.tilApellidos)
        tilTelefono = findViewById(R.id.tilTelefono)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etNombre = findViewById(R.id.etNombre)
        etApellidos = findViewById(R.id.etApellidos)
        etTelefono = findViewById(R.id.etTelefono)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoogle = findViewById(R.id.btnGoogle)
        progressBar = findViewById(R.id.progressBar)
        errorCard = findViewById(R.id.errorCard)
        successCard = findViewById(R.id.successCard)
        tvError = findViewById(R.id.tvError)
        loginContainer = findViewById(R.id.loginContainer)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegister()
            }
        }
        
        btnGoogle.setOnClickListener {
            performGoogleSignUp()
        }

        loginContainer.setOnClickListener {
            finish() // Volver a la pantalla de login
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validar nombre
        if (nombre.isEmpty()) {
            tilNombre.error = "El nombre es requerido"
            isValid = false
        } else {
            tilNombre.error = null
        }

        // Validar apellidos
        if (apellidos.isEmpty()) {
            tilApellidos.error = "Los apellidos son requeridos"
            isValid = false
        } else {
            tilApellidos.error = null
        }

        // Validar teléfono
        if (telefono.isEmpty()) {
            tilTelefono.error = "El teléfono es requerido"
            isValid = false
        } else if (telefono.length != 10) {
            tilTelefono.error = "Debe tener 10 dígitos"
            isValid = false
        } else {
            tilTelefono.error = null
        }

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

    private fun performRegister() {
        // Ocultar teclado
        currentFocus?.let { view ->
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        // Mostrar loading
        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false
        errorCard.visibility = View.GONE
        successCard.visibility = View.GONE

        // Simular registro (aquí integrarías tu backend)
        etEmail.postDelayed({
            // Registro exitoso
            successCard.visibility = View.VISIBLE
            btnRegister.isEnabled = true
            progressBar.visibility = View.GONE
            
            // Redirigir al login después de 2 segundos
            etEmail.postDelayed({
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }, 2000)
        }, 1500)
    }
    
    /**
     * Inicia el flujo de registro con Google
     * Muestra todas las cuentas disponibles (no filtra por autorizadas)
     */
    private fun performGoogleSignUp() {
        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false
        btnGoogle.isEnabled = false
        errorCard.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                // Para registro, mostramos TODAS las cuentas (filterByAuthorizedAccounts = false)
                val result = googleAuthManager.signInWithGoogle(
                    filterByAuthorizedAccounts = false,
                    autoSelectEnabled = false
                )
                
                handleGoogleAuthResult(result)
                
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error en Google Sign-Up", e)
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(
                    this@RegisterActivity,
                    "Error al registrarse con Google",
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
                btnRegister.isEnabled = true
                btnGoogle.isEnabled = true
                Toast.makeText(
                    this,
                    "Error: ${result.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
     * Envía el ID token de Google al backend para registro
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
                        // Guardar token en SharedPreferences
                        saveUserSession(authData.token, authData.usuario.nombre)
                        
                        // Mostrar mensaje de bienvenida
                        val mensaje = if (response.body()?.is_new_user == true) {
                            "¡Cuenta creada exitosamente!"
                        } else {
                            "Ya tienes una cuenta. ¡Bienvenido de nuevo!"
                        }
                        
                        Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_SHORT).show()
                        
                        // Navegar a MainActivity
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
     * Guarda la sesión del usuario en SharedPreferences
     */
    private fun saveUserSession(token: String, userName: String) {
        val prefs = getSharedPreferences("TrailynSafePrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("auth_token", token)
            putString("user_name", userName)
            putBoolean("is_logged_in", true)
            apply()
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

package com.example.trailynapp.driver.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.driver.MainActivity
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.api.LoginRequest
import com.example.trailynapp.driver.api.RetrofitClient
import com.example.trailynapp.driver.utils.SessionManager
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
    private lateinit var tvForgotPassword: MaterialTextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorCard: MaterialCardView
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)
        errorCard = findViewById(R.id.errorCard)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Contacta al administrador para recuperar tu contraseña", Toast.LENGTH_LONG).show()
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

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        lifecycleScope.launch {
            try {
                val request = LoginRequest(
                    correo = email,
                    password = password
                )
                
                val response = RetrofitClient.apiService.loginChofer(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!
                    
                    // Guardar sesión
                    val sessionManager = SessionManager(this@LoginActivity)
                    sessionManager.saveLoginSession(
                        token = loginData.token,
                        choferId = loginData.chofer.id,
                        nombre = loginData.chofer.nombre,
                        apellidos = loginData.chofer.apellidos,
                        correo = loginData.chofer.correo,
                        telefono = loginData.chofer.telefono ?: ""
                    )
                    
                    // Mostrar mensaje de bienvenida
                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido, ${loginData.chofer.nombre}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navegar a MainActivity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    
                } else {
                    // Manejar errores de respuesta
                    val errorBody = response.errorBody()?.string()
                    Log.e("LoginActivity", "Error response: $errorBody")
                    
                    val errorMessage = when (response.code()) {
                        403 -> "Tu cuenta no está activa. Contacta al administrador."
                        422 -> "Las credenciales proporcionadas son incorrectas."
                        else -> "Error al iniciar sesión. Intenta de nuevo."
                    }
                    
                    showError(errorMessage)
                }
                
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error en login", e)
                showError("Error de conexión. Verifica tu internet e intenta de nuevo.")
            } finally {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
            }
        }
    }
    
    private fun showError(message: String) {
        tvError.text = message
        errorCard.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
    }
}

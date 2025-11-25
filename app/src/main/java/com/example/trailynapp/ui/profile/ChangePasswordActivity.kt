package com.example.trailynapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.R
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.ui.welcome.WelcomeActivity
import com.example.trailynapp.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import android.content.Intent

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var toolbar: MaterialToolbar
    
    // Paso 1: Validar contraseña actual
    private lateinit var step1Card: MaterialCardView
    private lateinit var tilPasswordActual: TextInputLayout
    private lateinit var etPasswordActual: TextInputEditText
    private lateinit var btnValidatePassword: MaterialButton
    
    // Paso 2: Nueva contraseña
    private lateinit var step2Card: MaterialCardView
    private lateinit var tilNuevaPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etNuevaPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnBack: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    
    private lateinit var progressBar: LinearProgressIndicator
    
    private var tokenValidacion: String? = null
    private var currentStep = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        sessionManager = SessionManager(this)
        initViews()
        setupListeners()
        showStep(1)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        step1Card = findViewById(R.id.step1Card)
        step2Card = findViewById(R.id.step2Card)
        tilPasswordActual = findViewById(R.id.tilPasswordActual)
        tilNuevaPassword = findViewById(R.id.tilNuevaPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etPasswordActual = findViewById(R.id.etPasswordActual)
        etNuevaPassword = findViewById(R.id.etNuevaPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnValidatePassword = findViewById(R.id.btnValidatePassword)
        btnBack = findViewById(R.id.btnBack)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        progressBar = findViewById(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cambiar Contraseña"

        toolbar.setNavigationOnClickListener {
            if (currentStep == 2) {
                showStep(1)
            } else {
                finish()
            }
        }
    }

    private fun setupListeners() {
        btnValidatePassword.setOnClickListener {
            validateCurrentPassword()
        }

        btnBack.setOnClickListener {
            showStep(1)
        }

        btnChangePassword.setOnClickListener {
            if (validateNewPassword()) {
                changePassword()
            }
        }
    }

    private fun showStep(step: Int) {
        currentStep = step
        when (step) {
            1 -> {
                step1Card.visibility = View.VISIBLE
                step2Card.visibility = View.GONE
                etPasswordActual.text?.clear()
                tilPasswordActual.error = null
            }
            2 -> {
                step1Card.visibility = View.GONE
                step2Card.visibility = View.VISIBLE
                etNuevaPassword.text?.clear()
                etConfirmPassword.text?.clear()
                tilNuevaPassword.error = null
                tilConfirmPassword.error = null
            }
        }
    }

    private fun validateCurrentPassword() {
        val password = etPasswordActual.text.toString().trim()

        if (password.isEmpty()) {
            tilPasswordActual.error = "La contraseña es requerida"
            return
        }

        if (password.length < 6) {
            tilPasswordActual.error = "Contraseña muy corta"
            return
        }

        tilPasswordActual.error = null
        progressBar.visibility = View.VISIBLE
        btnValidatePassword.isEnabled = false

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    showError("Sesión expirada")
                    return@launch
                }

                val requestBody = mapOf("password_actual" to password)

                val response = RetrofitClient.apiService.validarPasswordActual(
                    token = "Bearer $token",
                    body = requestBody
                )

                if (response.isSuccessful && response.body() != null) {
                    tokenValidacion = response.body()!!.token_validacion
                    showStep(2)
                } else {
                    tilPasswordActual.error = "Contraseña incorrecta"
                }

            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnValidatePassword.isEnabled = true
            }
        }
    }

    private fun validateNewPassword(): Boolean {
        val nueva = etNuevaPassword.text.toString().trim()
        val confirmar = etConfirmPassword.text.toString().trim()

        var isValid = true

        if (nueva.isEmpty()) {
            tilNuevaPassword.error = "La contraseña es requerida"
            isValid = false
        } else if (nueva.length < 6) {
            tilNuevaPassword.error = "Mínimo 6 caracteres"
            isValid = false
        } else {
            tilNuevaPassword.error = null
        }

        if (confirmar.isEmpty()) {
            tilConfirmPassword.error = "Confirma tu contraseña"
            isValid = false
        } else if (confirmar != nueva) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun changePassword() {
        progressBar.visibility = View.VISIBLE
        btnChangePassword.isEnabled = false
        btnBack.isEnabled = false

        val nuevaPassword = etNuevaPassword.text.toString().trim()

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    showError("Sesión expirada")
                    return@launch
                }

                val requestBody = mapOf(
                    "nueva_contrasena" to nuevaPassword,
                    "confirmar_contrasena" to nuevaPassword,
                    "token_validacion" to (tokenValidacion ?: "")
                )

                val response = RetrofitClient.apiService.cambiarContrasena(
                    token = "Bearer $token",
                    body = requestBody
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Contraseña actualizada. Por favor inicia sesión nuevamente",
                        Toast.LENGTH_LONG
                    ).show()

                    // Cerrar sesión y redirigir a login
                    sessionManager.logout()
                    val intent = Intent(this@ChangePasswordActivity, WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Leer mensaje de error del backend
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("ChangePassword", "Error ${response.code()}: $errorBody")
                    
                    val errorMessage = when {
                        errorBody?.contains("token_validacion") == true -> 
                            "Token de validación inválido. Intenta nuevamente desde el paso 1"
                        errorBody?.contains("expirado") == true -> 
                            "El token ha expirado. Intenta nuevamente desde el paso 1"
                        errorBody?.contains("contraseña") == true -> 
                            "Error con la contraseña. Verifica que cumple los requisitos"
                        response.code() == 422 -> 
                            "Datos inválidos. Verifica tu información"
                        else -> 
                            "Error al cambiar contraseña (${response.code()})"
                    }
                    showError(errorMessage)
                }

            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnChangePassword.isEnabled = true
                btnBack.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        if (currentStep == 2) {
            showStep(1)
        } else {
            super.onBackPressed()
        }
    }
}

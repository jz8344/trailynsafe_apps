package com.example.trailynapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.R
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class ChangeEmailActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvCurrentEmail: MaterialTextView
    private lateinit var tilNuevoEmail: TextInputLayout
    private lateinit var etNuevoEmail: TextInputEditText
    private lateinit var btnContinue: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator

    private val allowedDomains = listOf(
        "gmail.com", "hotmail.com", "outlook.com",
        "outlook.es", "proton.me", "protonmail.com",
        "utzmg.edu.mx"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_email)

        sessionManager = SessionManager(this)
        initViews()
        setupListeners()
        loadCurrentEmail()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvCurrentEmail = findViewById(R.id.tvCurrentEmail)
        tilNuevoEmail = findViewById(R.id.tilNuevoEmail)
        etNuevoEmail = findViewById(R.id.etNuevoEmail)
        btnContinue = findViewById(R.id.btnContinue)
        progressBar = findViewById(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cambiar Correo"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        btnContinue.setOnClickListener {
            if (validateEmail()) {
                showPasswordDialog()
            }
        }

        etNuevoEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmail()
        }
    }

    private fun loadCurrentEmail() {
        tvCurrentEmail.text = sessionManager.getCorreo()
    }

    private fun validateEmail(): Boolean {
        val email = etNuevoEmail.text.toString().trim()
        val currentEmail = sessionManager.getCorreo()

        return when {
            email.isEmpty() -> {
                tilNuevoEmail.error = "El correo es requerido"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilNuevoEmail.error = "Formato de correo inválido"
                false
            }
            !isAllowedDomain(email) -> {
                tilNuevoEmail.error = "Dominio de correo no permitido"
                false
            }
            email.equals(currentEmail, ignoreCase = true) -> {
                tilNuevoEmail.error = "El nuevo correo debe ser diferente al actual"
                false
            }
            else -> {
                tilNuevoEmail.error = null
                true
            }
        }
    }

    private fun isAllowedDomain(email: String): Boolean {
        val domain = email.substringAfter("@").lowercase()
        return allowedDomains.any { it.equals(domain, ignoreCase = true) }
    }

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_confirm, null)
        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.etPassword)

        AlertDialog.Builder(this)
            .setTitle("Confirma tu identidad")
            .setMessage("Por seguridad, ingresa tu contraseña actual")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val password = etPassword.text.toString().trim()
                if (password.isEmpty()) {
                    Toast.makeText(this, "La contraseña es requerida", Toast.LENGTH_SHORT).show()
                } else if (password.length < 6) {
                    Toast.makeText(this, "Contraseña muy corta", Toast.LENGTH_SHORT).show()
                } else {
                    changeEmail(password)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun changeEmail(password: String) {
        progressBar.visibility = View.VISIBLE
        btnContinue.isEnabled = false

        val nuevoEmail = etNuevoEmail.text.toString().trim()

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    showError("Sesión expirada")
                    return@launch
                }

                val requestBody = mapOf(
                    "nuevo_correo" to nuevoEmail,
                    "contrasena_actual" to password
                )

                val response = RetrofitClient.apiService.cambiarCorreo(
                    token = "Bearer $token",
                    body = requestBody
                )

                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!.usuario

                    // Actualizar sesión
                    sessionManager.saveLoginSession(
                        token = token,
                        userId = usuario.id,
                        nombre = usuario.nombre,
                        apellidos = usuario.apellidos ?: "",
                        correo = usuario.correo,
                        telefono = usuario.telefono ?: ""
                    )

                    Toast.makeText(
                        this@ChangeEmailActivity,
                        "Correo actualizado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    when {
                        errorBody?.contains("ya está registrado") == true -> {
                            tilNuevoEmail.error = "Este correo ya está en uso"
                        }
                        response.code() == 401 -> {
                            showError("Contraseña incorrecta")
                        }
                        else -> {
                            showError("Error al cambiar correo")
                        }
                    }
                }

            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnContinue.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

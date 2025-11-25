package com.example.trailynapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilApellidos: TextInputLayout
    private lateinit var tilTelefono: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellidos: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        initViews()
        setupListeners()
        loadCurrentData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tilNombre = findViewById(R.id.tilNombre)
        tilApellidos = findViewById(R.id.tilApellidos)
        tilTelefono = findViewById(R.id.tilTelefono)
        etNombre = findViewById(R.id.etNombre)
        etApellidos = findViewById(R.id.etApellidos)
        etTelefono = findViewById(R.id.etTelefono)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar Perfil"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            if (validateInputs()) {
                saveProfile()
            }
        }

        // Validación en tiempo real
        etNombre.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateNombre()
        }

        etApellidos.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateApellidos()
        }

        etTelefono.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateTelefono()
        }
    }

    private fun loadCurrentData() {
        etNombre.setText(sessionManager.getNombre())
        etApellidos.setText(sessionManager.getApellidos())
        etTelefono.setText(sessionManager.getTelefono())
    }

    private fun validateNombre(): Boolean {
        val nombre = etNombre.text.toString().trim()
        return when {
            nombre.isEmpty() -> {
                tilNombre.error = "El nombre es requerido"
                false
            }
            nombre.length < 2 -> {
                tilNombre.error = "El nombre debe tener al menos 2 caracteres"
                false
            }
            !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                tilNombre.error = "El nombre solo puede contener letras"
                false
            }
            else -> {
                tilNombre.error = null
                true
            }
        }
    }

    private fun validateApellidos(): Boolean {
        val apellidos = etApellidos.text.toString().trim()
        return when {
            apellidos.isEmpty() -> {
                tilApellidos.error = "Los apellidos son requeridos"
                false
            }
            apellidos.length < 2 -> {
                tilApellidos.error = "Los apellidos deben tener al menos 2 caracteres"
                false
            }
            !apellidos.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                tilApellidos.error = "Los apellidos solo pueden contener letras"
                false
            }
            else -> {
                tilApellidos.error = null
                true
            }
        }
    }

    private fun validateTelefono(): Boolean {
        val telefono = etTelefono.text.toString().trim()
        return when {
            telefono.isEmpty() -> {
                tilTelefono.error = "El teléfono es requerido"
                false
            }
            telefono.length != 10 -> {
                tilTelefono.error = "El teléfono debe tener 10 dígitos"
                false
            }
            !telefono.matches(Regex("^\\d+$")) -> {
                tilTelefono.error = "El teléfono solo puede contener números"
                false
            }
            else -> {
                tilTelefono.error = null
                true
            }
        }
    }

    private fun validateInputs(): Boolean {
        val nombreValid = validateNombre()
        val apellidosValid = validateApellidos()
        val telefonoValid = validateTelefono()
        return nombreValid && apellidosValid && telefonoValid
    }

    private fun saveProfile() {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    showError("Sesión expirada")
                    return@launch
                }

                val requestBody = mapOf(
                    "nombre" to nombre,
                    "apellidos" to apellidos,
                    "telefono" to telefono
                )

                val response = RetrofitClient.apiService.editarPerfil(
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
                        this@EditProfileActivity,
                        "Perfil actualizado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                } else {
                    showError("Error al actualizar perfil")
                }

            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        progressBar.visibility = View.GONE
        btnSave.isEnabled = true
    }
}

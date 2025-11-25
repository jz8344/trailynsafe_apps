package com.example.trailynapp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.R
import com.example.trailynapp.api.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class RecuperarPasswordActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: LinearProgressIndicator
    
    // Paso 1: Ingresar email
    private lateinit var step1Card: MaterialCardView
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendCode: MaterialButton
    
    // Paso 2: Ingresar código
    private lateinit var step2Card: MaterialCardView
    private lateinit var tilCodigo: TextInputLayout
    private lateinit var etCodigo: TextInputEditText
    private lateinit var btnVerifyCode: MaterialButton
    private lateinit var btnBack1: MaterialButton
    
    // Paso 3: Nueva contraseña
    private lateinit var step3Card: MaterialCardView
    private lateinit var tilNuevaPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etNuevaPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var btnBack2: MaterialButton
    
    private var currentStep = 1
    private var userEmail = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_password)

        initViews()
        setupListeners()
        showStep(1)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        
        // Paso 1
        step1Card = findViewById(R.id.step1Card)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnSendCode = findViewById(R.id.btnSendCode)
        
        // Paso 2
        step2Card = findViewById(R.id.step2Card)
        tilCodigo = findViewById(R.id.tilCodigo)
        etCodigo = findViewById(R.id.etCodigo)
        btnVerifyCode = findViewById(R.id.btnVerifyCode)
        btnBack1 = findViewById(R.id.btnBack1)
        
        // Paso 3
        step3Card = findViewById(R.id.step3Card)
        tilNuevaPassword = findViewById(R.id.tilNuevaPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etNuevaPassword = findViewById(R.id.etNuevaPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnBack2 = findViewById(R.id.btnBack2)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recuperar Contraseña"

        toolbar.setNavigationOnClickListener {
            when (currentStep) {
                2 -> showStep(1)
                3 -> showStep(2)
                else -> finish()
            }
        }
    }

    private fun setupListeners() {
        btnSendCode.setOnClickListener {
            sendVerificationCode()
        }

        btnVerifyCode.setOnClickListener {
            verifyCode()
        }

        btnResetPassword.setOnClickListener {
            if (validateNewPassword()) {
                resetPassword()
            }
        }

        btnBack1.setOnClickListener {
            showStep(1)
        }

        btnBack2.setOnClickListener {
            showStep(2)
        }
    }

    private fun showStep(step: Int) {
        currentStep = step
        when (step) {
            1 -> {
                step1Card.visibility = View.VISIBLE
                step2Card.visibility = View.GONE
                step3Card.visibility = View.GONE
                etEmail.text?.clear()
                tilEmail.error = null
            }
            2 -> {
                step1Card.visibility = View.GONE
                step2Card.visibility = View.VISIBLE
                step3Card.visibility = View.GONE
                etCodigo.text?.clear()
                tilCodigo.error = null
            }
            3 -> {
                step1Card.visibility = View.GONE
                step2Card.visibility = View.GONE
                step3Card.visibility = View.VISIBLE
                etNuevaPassword.text?.clear()
                etConfirmPassword.text?.clear()
                tilNuevaPassword.error = null
                tilConfirmPassword.error = null
            }
        }
    }

    private fun sendVerificationCode() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            tilEmail.error = "El correo es requerido"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Formato de correo inválido"
            return
        }

        tilEmail.error = null
        progressBar.visibility = View.VISIBLE
        btnSendCode.isEnabled = false

        lifecycleScope.launch {
            try {
                val requestBody = mapOf("correo" to email)
                val response = RetrofitClient.apiService.enviarCodigoRecuperacion(requestBody)

                if (response.isSuccessful && response.body() != null) {
                    userEmail = email
                    Toast.makeText(
                        this@RecuperarPasswordActivity,
                        "Si el correo está registrado, recibirás un código de verificación",
                        Toast.LENGTH_LONG
                    ).show()
                    showStep(2)
                } else {
                    Toast.makeText(this@RecuperarPasswordActivity, "Error al enviar código", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RecuperarPasswordActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnSendCode.isEnabled = true
            }
        }
    }

    private fun verifyCode() {
        val codigo = etCodigo.text.toString().trim()

        if (codigo.isEmpty()) {
            tilCodigo.error = "El código es requerido"
            return
        }

        if (codigo.length != 6) {
            tilCodigo.error = "El código debe tener 6 dígitos"
            return
        }

        tilCodigo.error = null
        progressBar.visibility = View.VISIBLE
        btnVerifyCode.isEnabled = false

        lifecycleScope.launch {
            try {
                val requestBody = mapOf(
                    "correo" to userEmail,
                    "codigo" to codigo
                )
                val response = RetrofitClient.apiService.validarCodigoRecuperacion(requestBody)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@RecuperarPasswordActivity,
                        "Código verificado",
                        Toast.LENGTH_SHORT
                    ).show()
                    showStep(3)
                } else {
                    tilCodigo.error = "Código incorrecto o expirado"
                }
            } catch (e: Exception) {
                Toast.makeText(this@RecuperarPasswordActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnVerifyCode.isEnabled = true
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

    private fun resetPassword() {
        progressBar.visibility = View.VISIBLE
        btnResetPassword.isEnabled = false
        btnBack2.isEnabled = false

        val nuevaPassword = etNuevaPassword.text.toString().trim()
        val codigo = etCodigo.text.toString().trim()

        lifecycleScope.launch {
            try {
                val requestBody = mapOf(
                    "correo" to userEmail,
                    "codigo" to codigo.toInt().toString(),
                    "nueva_contrasena" to nuevaPassword,
                    "nueva_contrasena_confirmation" to nuevaPassword
                )

                val response = RetrofitClient.apiService.restablecerPassword(requestBody)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RecuperarPasswordActivity,
                        "Contraseña restablecida correctamente",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(this@RecuperarPasswordActivity, "Error al restablecer contraseña", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RecuperarPasswordActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnResetPassword.isEnabled = true
                btnBack2.isEnabled = true
            }
        }
    }

    override fun onBackPressed() {
        when (currentStep) {
            2 -> showStep(1)
            3 -> showStep(2)
            else -> super.onBackPressed()
        }
    }
}

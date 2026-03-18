package com.trailynsafe.app.ui.hijos

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.trailynsafe.app.R
import com.trailynsafe.app.api.EscuelaSimple
import com.trailynsafe.app.api.RetrofitClient
import com.trailynsafe.app.utils.InputValidator
import com.trailynsafe.app.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class EditarHijoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HIJO_ID = "hijo_id"
        const val EXTRA_HIJO_NOMBRE = "hijo_nombre"
        const val EXTRA_HIJO_GRADO = "hijo_grado"
        const val EXTRA_HIJO_GRUPO = "hijo_grupo"
        const val EXTRA_HIJO_ESCUELA_ID = "hijo_escuela_id"
        const val EXTRA_HIJO_EMERGENCIA_1 = "hijo_emergencia_1"
        const val EXTRA_HIJO_EMERGENCIA_2 = "hijo_emergencia_2"
    }

    private lateinit var etNombre: TextInputEditText
    private lateinit var spinnerEscuela: AutoCompleteTextView
    private lateinit var spinnerGrado: AutoCompleteTextView
    private lateinit var spinnerGrupo: AutoCompleteTextView
    private lateinit var etEmergencia1: TextInputEditText
    private lateinit var etEmergencia2: TextInputEditText
    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilEmergencia1: TextInputLayout
    private lateinit var tilEmergencia2: TextInputLayout
    private lateinit var btnGuardar: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private var escuelasLista: List<EscuelaSimple> = emptyList()
    private var hijoId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_hijo)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarEditarHijo)
        toolbar.setNavigationOnClickListener { finish() }

        etNombre = findViewById(R.id.etNombreHijo)
        spinnerEscuela = findViewById(R.id.spinnerEscuela)
        spinnerGrado = findViewById(R.id.spinnerGrado)
        spinnerGrupo = findViewById(R.id.spinnerGrupo)
        etEmergencia1 = findViewById(R.id.etEmergencia1)
        etEmergencia2 = findViewById(R.id.etEmergencia2)
        tilNombre = findViewById(R.id.tilNombreHijo)
        tilEmergencia1 = findViewById(R.id.tilEmergencia1)
        tilEmergencia2 = findViewById(R.id.tilEmergencia2)
        btnGuardar = findViewById(R.id.btnGuardar)
        progressBar = findViewById(R.id.progressBarEditar)

        applyInputFilters()
        configurarSpinners()
        cargarEscuelas()
        cargarDatosDesdeIntent()

        btnGuardar.setOnClickListener { guardarCambios() }
    }

    private fun applyInputFilters() {
        etNombre.filters = arrayOf(InputValidator.nameFilter(60), InputValidator.lengthFilter(60))
        etEmergencia1.filters =
                arrayOf(InputValidator.phoneFilter(), InputValidator.lengthFilter(10))
        etEmergencia2.filters =
                arrayOf(InputValidator.phoneFilter(), InputValidator.lengthFilter(10))
    }

    private fun configurarSpinners() {
        spinnerGrado.setAdapter(
                ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        (1..8).map { it.toString() }
                )
        )
        spinnerGrupo.setAdapter(
                ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        ('A'..'K').map { it.toString() }
                )
        )
    }

    private fun cargarDatosDesdeIntent() {
        hijoId = intent.getIntExtra(EXTRA_HIJO_ID, -1)
        etNombre.setText(intent.getStringExtra(EXTRA_HIJO_NOMBRE) ?: "")
        spinnerGrado.setText(intent.getStringExtra(EXTRA_HIJO_GRADO) ?: "", false)
        spinnerGrupo.setText(intent.getStringExtra(EXTRA_HIJO_GRUPO) ?: "", false)
        etEmergencia1.setText(intent.getStringExtra(EXTRA_HIJO_EMERGENCIA_1) ?: "")
        etEmergencia2.setText(intent.getStringExtra(EXTRA_HIJO_EMERGENCIA_2) ?: "")
    }

    private fun cargarEscuelas() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getEscuelasActivas()
                if (response.isSuccessful && response.body() != null) {
                    escuelasLista = response.body()!!
                    val nombres = escuelasLista.map { it.nombre }
                    spinnerEscuela.setAdapter(
                            ArrayAdapter(
                                    this@EditarHijoActivity,
                                    android.R.layout.simple_dropdown_item_1line,
                                    nombres
                            )
                    )
                    val escuelaId = intent.getIntExtra(EXTRA_HIJO_ESCUELA_ID, -1)
                    escuelasLista.find { it.id == escuelaId }?.let {
                        spinnerEscuela.setText(it.nombre, false)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun guardarCambios() {
        if (hijoId == -1) {
            Toast.makeText(this, "Error: ID de hijo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val nombre = InputValidator.sanitizeName(etNombre.text.toString())
        val escuelaNombre = spinnerEscuela.text.toString()
        val grado = spinnerGrado.text.toString()
        val grupo = spinnerGrupo.text.toString()
        val contacto1 = InputValidator.sanitizePhone(etEmergencia1.text.toString())
        val contacto2 = InputValidator.sanitizePhone(etEmergencia2.text.toString())

        InputValidator.validateName(nombre).let {
            tilNombre.error = it
            if (it != null) return
        }
        tilNombre.error = null

        if (escuelaNombre.isEmpty() || grado.isEmpty() || grupo.isEmpty()) {
            Toast.makeText(this, "Completa escuela, grado y grupo", Toast.LENGTH_SHORT).show()
            return
        }

        InputValidator.validatePhone(contacto1).let {
            tilEmergencia1.error = it
            if (it != null) return
        }
        tilEmergencia1.error = null

        if (contacto2.isNotEmpty()) {
            InputValidator.validatePhone(contacto2).let {
                tilEmergencia2.error = it
                if (it != null) return
            }
        }
        tilEmergencia2.error = null

        val escuelaId = escuelasLista.find { it.nombre == escuelaNombre }?.id
        if (escuelaId == null) {
            Toast.makeText(this, "Selecciona una escuela válida", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardar.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val body =
                mutableMapOf<String, Any>(
                        "nombre" to nombre,
                        "escuela_id" to escuelaId,
                        "grado" to grado,
                        "grupo" to grupo,
                        "contacto_emergencia_1" to contacto1
                )
        if (contacto2.isNotEmpty()) body["contacto_emergencia_2"] = contacto2

        lifecycleScope.launch {
            try {
                val token =
                        sessionManager.getToken()
                                ?: run {
                                    Toast.makeText(
                                                    this@EditarHijoActivity,
                                                    "Sesión expirada",
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    return@launch
                                }
                val response =
                        RetrofitClient.apiService.actualizarHijo(hijoId, "Bearer $token", body)

                progressBar.visibility = View.GONE
                btnGuardar.isEnabled = true

                if (response.isSuccessful) {
                    Toast.makeText(
                                    this@EditarHijoActivity,
                                    "✓ Datos actualizados correctamente",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val err = response.errorBody()?.string() ?: "Error del servidor"
                    Toast.makeText(this@EditarHijoActivity, "Error: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnGuardar.isEnabled = true
                Toast.makeText(
                                this@EditarHijoActivity,
                                "Error de red: ${e.message}",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }
    }
}


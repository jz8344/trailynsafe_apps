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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistroHijoActivity : AppCompatActivity() {

    private lateinit var etNombre: TextInputEditText
    private lateinit var spinnerEscuela: AutoCompleteTextView
    private lateinit var spinnerGrado: AutoCompleteTextView
    private lateinit var spinnerGrupo: AutoCompleteTextView
    private lateinit var etEmergencia1: TextInputEditText
    private lateinit var etEmergencia2: TextInputEditText
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private var escuelasLista: List<EscuelaSimple> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_hijo)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarRegistroHijo)
        toolbar.setNavigationOnClickListener { finish() }

        etNombre = findViewById(R.id.etNombreHijo)
        spinnerEscuela = findViewById(R.id.spinnerEscuela)
        spinnerGrado = findViewById(R.id.spinnerGrado)
        spinnerGrupo = findViewById(R.id.spinnerGrupo)
        etEmergencia1 = findViewById(R.id.etEmergencia1)
        etEmergencia2 = findViewById(R.id.etEmergencia2)
        btnRegistrar = findViewById(R.id.btnRegistrarHijo)
        progressBar = findViewById(R.id.progressBarRegistro)

        configurarSpinnersDuros()
        cargarEscuelas()
        applyInputFilters()

        btnRegistrar.setOnClickListener { registrarHijo() }
    }

    private fun applyInputFilters() {
        etNombre.filters = arrayOf(InputValidator.nameFilter(60), InputValidator.lengthFilter(60))
        etEmergencia1.filters =
                arrayOf(InputValidator.phoneFilter(), InputValidator.lengthFilter(10))
        etEmergencia2.filters =
                arrayOf(InputValidator.phoneFilter(), InputValidator.lengthFilter(10))
    }

    private fun configurarSpinnersDuros() {
        val grados = (1..8).map { it.toString() }
        spinnerGrado.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, grados)
        )

        val grupos = ('A'..'K').map { it.toString() }
        spinnerGrupo.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, grupos)
        )
    }

    private fun cargarEscuelas() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getEscuelasActivas()
                if (response.isSuccessful && response.body() != null) {
                    escuelasLista = response.body()!!
                    val nombresEscuelas = escuelasLista.map { it.nombre }
                    spinnerEscuela.setAdapter(
                            ArrayAdapter(
                                    this@RegistroHijoActivity,
                                    android.R.layout.simple_dropdown_item_1line,
                                    nombresEscuelas
                            )
                    )
                } else {
                    Toast.makeText(
                                    this@RegistroHijoActivity,
                                    "No se pudieron cargar las escuelas",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                                this@RegistroHijoActivity,
                                "Error de red al cargar escuelas",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun registrarHijo() {
        val nombre = InputValidator.sanitizeName(etNombre.text.toString())
        val escuelaNombre = spinnerEscuela.text.toString()
        val grado = spinnerGrado.text.toString()
        val grupo = spinnerGrupo.text.toString()
        val contacto1 = InputValidator.sanitizePhone(etEmergencia1.text.toString())
        val contacto2 = InputValidator.sanitizePhone(etEmergencia2.text.toString())

        val tilNombre =
                findViewById<com.google.android.material.textfield.TextInputLayout>(
                        R.id.tilNombreHijo
                )
        InputValidator.validateName(nombre).let {
            tilNombre?.error = it
            if (it != null) return
        }
        if (escuelaNombre.isEmpty() || grado.isEmpty() || grupo.isEmpty()) {
            Toast.makeText(
                            this,
                            "Por favor completa todos los campos obligatorios",
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }
        val tilEmergencia1 =
                findViewById<com.google.android.material.textfield.TextInputLayout>(
                        R.id.tilEmergencia1
                )
        InputValidator.validatePhone(contacto1).let {
            tilEmergencia1?.error = it
            if (it != null) return
        }
        if (contacto2.isNotEmpty()) {
            val tilEmergencia2 =
                    findViewById<com.google.android.material.textfield.TextInputLayout>(
                            R.id.tilEmergencia2
                    )
            InputValidator.validatePhone(contacto2).let {
                tilEmergencia2?.error = it
                if (it != null) return
            }
        }

        val escuelaId = escuelasLista.find { it.nombre == escuelaNombre }?.id
        if (escuelaId == null) {
            Toast.makeText(this, "Selecciona una escuela válida", Toast.LENGTH_SHORT).show()
            return
        }

        val codigoQrGenerado = "QR_${System.currentTimeMillis()}_${(1000..9999).random()}"

        val requestBody =
                mutableMapOf<String, Any>(
                        "nombre" to nombre,
                        "escuela_id" to escuelaId,
                        "grado" to grado,
                        "grupo" to grupo,
                        "contacto_emergencia_1" to contacto1,
                        "codigo_qr" to codigoQrGenerado
                )
        if (contacto2.isNotEmpty()) requestBody["contacto_emergencia_2"] = contacto2

        progressBar.visibility = View.VISIBLE
        btnRegistrar.isEnabled = false

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = RetrofitClient.apiService.registrarHijo("Bearer $token", requestBody)

                if (response.isSuccessful && response.body() != null) {
                    val qrBytes =
                            withContext(Dispatchers.Default) { generarQrBytes(codigoQrGenerado) }
                    val intent =
                            android.content.Intent(
                                            this@RegistroHijoActivity,
                                            GafeteActivity::class.java
                                    )
                                    .apply {
                                        putExtra(GafeteActivity.EXTRA_NOMBRE, nombre)
                                        putExtra(GafeteActivity.EXTRA_GRADO, grado)
                                        putExtra(GafeteActivity.EXTRA_GRUPO, grupo)
                                        putExtra(GafeteActivity.EXTRA_ESCUELA, escuelaNombre)
                                        if (qrBytes != null)
                                                putExtra(GafeteActivity.EXTRA_QR_BYTES, qrBytes)
                                    }
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error de servidor"
                    Toast.makeText(
                                    this@RegistroHijoActivity,
                                    com.trailynsafe.app.utils.InputValidator.parseServerError(
                                            errorMsg
                                    ),
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegistroHijoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
            } finally {
                progressBar.visibility = View.GONE
                btnRegistrar.isEnabled = true
            }
        }
    }

    private fun generarQrBytes(qrContent: String): ByteArray? {
        return try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap =
                    android.graphics.Bitmap.createBitmap(
                            width,
                            height,
                            android.graphics.Bitmap.Config.RGB_565
                    )
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                            x,
                            y,
                            if (bitMatrix[x, y]) android.graphics.Color.BLACK
                            else android.graphics.Color.WHITE
                    )
                }
            }
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}


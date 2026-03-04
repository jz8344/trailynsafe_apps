package com.example.trailynapp.ui.hijos

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.trailynapp.R
import com.example.trailynapp.api.EscuelaSimple
import com.example.trailynapp.api.RetrofitClient
import com.example.trailynapp.utils.InputValidator
import com.example.trailynapp.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream
import java.util.*
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

    private lateinit var layoutQrPreview: LinearLayout
    private lateinit var ivQrPreview: ImageView
    private lateinit var btnDescargarGafete: MaterialButton

    private lateinit var sessionManager: SessionManager
    private var escuelasLista: List<EscuelaSimple> = emptyList()
    private var currentGafeteBitmap: Bitmap? = null
    private var nombreEstudiante = ""

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    guardarEnGaleria()
                } else {
                    Toast.makeText(this, "Permiso denegado para guardar imagen", Toast.LENGTH_SHORT)
                            .show()
                }
            }

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

        layoutQrPreview = findViewById(R.id.layoutQrPreview)
        ivQrPreview = findViewById(R.id.ivQrPreview)
        btnDescargarGafete = findViewById(R.id.btnDescargarGafete)

        configurarSpinnersDuros()
        cargarEscuelas()
        applyInputFilters()

        btnRegistrar.setOnClickListener { registrarHijo() }
        btnDescargarGafete.setOnClickListener { solicitarPermisoGuardado() }
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
                    Toast.makeText(
                                    this@RegistroHijoActivity,
                                    "Estudiante registrado con éxito",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                    nombreEstudiante = nombre
                    generarYMostrarGafete(codigoQrGenerado, nombre, grado, grupo, escuelaNombre)

                    // Deshabilitar formulario
                    btnRegistrar.visibility = View.GONE
                    etNombre.isEnabled = false
                    spinnerEscuela.isEnabled = false
                    spinnerGrado.isEnabled = false
                    spinnerGrupo.isEnabled = false
                    etEmergencia1.isEnabled = false
                    etEmergencia2.isEnabled = false
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error de servidor"
                    Toast.makeText(this@RegistroHijoActivity, "Error: $errorMsg", Toast.LENGTH_LONG)
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

    private suspend fun generarYMostrarGafete(
            qrContent: String,
            nombre: String,
            grado: String,
            grupo: String,
            escuela: String
    ) {
        withContext(Dispatchers.Default) {
            try {
                // 1. Generate QR Code Bitmap
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        qrBitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }

                // 2. Draw ID Card (Gafete) using Canvas
                val cardWidth = 800
                val cardHeight = 1200
                val gafeteBitmap =
                        Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(gafeteBitmap)

                // Background
                canvas.drawColor(Color.WHITE)

                // Header (Color Primary)
                val paintHeader =
                        Paint().apply {
                            color = Color.parseColor("#4285F4")
                            style = Paint.Style.FILL
                        }
                canvas.drawRect(0f, 0f, cardWidth.toFloat(), 200f, paintHeader)

                // Header Text
                val paintTitle =
                        Paint().apply {
                            color = Color.WHITE
                            textSize = 60f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                canvas.drawText("TRAILYN SAFE", cardWidth / 2f, 130f, paintTitle)

                // Student Name
                val paintName =
                        Paint().apply {
                            color = Color.BLACK
                            textSize = 55f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                canvas.drawText(nombre, cardWidth / 2f, 300f, paintName)

                // School Name
                val paintSchool =
                        Paint().apply {
                            color = Color.parseColor("#5F6368")
                            textSize = 40f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }
                canvas.drawText(escuela, cardWidth / 2f, 370f, paintSchool)

                // Grade & Group
                val paintInfo =
                        Paint().apply {
                            color = Color.BLACK
                            textSize = 45f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }
                canvas.drawText("Grado: $grado | Grupo: $grupo", cardWidth / 2f, 440f, paintInfo)

                // Draw QR Code in the middle
                val qrSize = 500
                val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, false)
                canvas.drawBitmap(scaledQr, (cardWidth - qrSize) / 2f, 520f, null)

                // Footer Text
                val paintFooter =
                        Paint().apply {
                            color = Color.parseColor("#9AA0A6")
                            textSize = 30f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }
                canvas.drawText("Gafete Escolar Exclusivo", cardWidth / 2f, 1100f, paintFooter)
                canvas.drawText(
                        "Escanea este código al abordar la unidad",
                        cardWidth / 2f,
                        1150f,
                        paintFooter
                )

                currentGafeteBitmap = gafeteBitmap

                withContext(Dispatchers.Main) {
                    ivQrPreview.setImageBitmap(gafeteBitmap)
                    layoutQrPreview.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                                    this@RegistroHijoActivity,
                                    "Error generando QR: ${e.message}",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }
        }
    }

    private fun solicitarPermisoGuardado() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            guardarEnGaleria() // No necesita permiso REQ
        } else {
            if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                guardarEnGaleria()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun guardarEnGaleria() {
        val bitmap = currentGafeteBitmap ?: return

        val filename =
                "Gafete_TrailynSafe_${nombreEstudiante.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                            put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES + "/TrailynSafe"
                            )
                        }
                val imageUri: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                @Suppress("DEPRECATION")
                val imagesDir =
                        Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES
                                )
                                .toString() + "/TrailynSafe"
                val file = java.io.File(imagesDir)
                if (!file.exists()) file.mkdir()
                val image = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(
                                this,
                                "Gafete guardado en Galería (Pictures/TrailynSafe)",
                                Toast.LENGTH_LONG
                        )
                        .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

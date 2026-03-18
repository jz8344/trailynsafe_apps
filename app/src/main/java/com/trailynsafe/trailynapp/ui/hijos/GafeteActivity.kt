package com.trailynsafe.app.ui.hijos

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.trailynsafe.app.R
import com.google.android.material.button.MaterialButton
import java.io.OutputStream

class GafeteActivity : AppCompatActivity() {

    private lateinit var ivGafeteQR: ImageView
    private lateinit var tvGafeteNombreHijo: TextView
    private lateinit var tvGafeteGradoGrupo: TextView
    private lateinit var tvGafeteNombreEscuela: TextView
    private lateinit var btnDescargar: MaterialButton
    private lateinit var btnFinalizar: MaterialButton
    private lateinit var btnCerrar: ImageButton

    private var gafeteBitmap: Bitmap? = null
    private var nombreEstudiante = ""

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_GRADO = "extra_grado"
        const val EXTRA_GRUPO = "extra_grupo"
        const val EXTRA_ESCUELA = "extra_escuela"
        const val EXTRA_GAFETE_PATH = "extra_gafete_path"
        const val EXTRA_QR_BYTES = "extra_qr_bytes"
    }

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) guardarEnGaleria()
                else
                        Toast.makeText(
                                        this,
                                        "Permiso denegado para guardar imagen",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gafete)

        ivGafeteQR = findViewById(R.id.ivGafeteQR)
        tvGafeteNombreHijo = findViewById(R.id.tvGafeteNombreHijo)
        tvGafeteGradoGrupo = findViewById(R.id.tvGafeteGradoGrupo)
        tvGafeteNombreEscuela = findViewById(R.id.tvGafeteNombreEscuela)
        btnDescargar = findViewById(R.id.btnDescargarGafeteNuevo)
        btnFinalizar = findViewById(R.id.btnFinalizarGafete)
        btnCerrar = findViewById(R.id.btnCerrarGafete)

        val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: ""
        val grado = intent.getStringExtra(EXTRA_GRADO) ?: ""
        val grupo = intent.getStringExtra(EXTRA_GRUPO) ?: ""
        val escuela = intent.getStringExtra(EXTRA_ESCUELA) ?: ""
        val qrBytes = intent.getByteArrayExtra(EXTRA_QR_BYTES)

        nombreEstudiante = nombre
        tvGafeteNombreHijo.text = nombre
        tvGafeteGradoGrupo.text =
                if (grado.isNotBlank() && grupo.isNotBlank()) "$grado° $grupo" else ""
        tvGafeteNombreEscuela.text = escuela

        if (qrBytes != null) {
            gafeteBitmap = BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.size)
            ivGafeteQR.setImageBitmap(gafeteBitmap)
        }

        btnCerrar.setOnClickListener { finish() }
        btnFinalizar.setOnClickListener { finish() }
        btnDescargar.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(
                                    this,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                guardarEnGaleria()
            }
        }
    }

    private fun guardarEnGaleria() {
        val bitmap =
                gafeteBitmap
                        ?: run {
                            Toast.makeText(this, "Gafete no disponible", Toast.LENGTH_SHORT).show()
                            return
                        }
        val filename =
                "Gafete_TrailynSafe_${nombreEstudiante.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                            put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES + "/TrailynSafe"
                            )
                        }
                val imageUri =
                        contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        )
                if (imageUri != null) fos = contentResolver.openOutputStream(imageUri)
            } else {
                @Suppress("DEPRECATION")
                val dir =
                        Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES
                                )
                                .toString() + "/TrailynSafe"
                val file = java.io.File(dir).also { if (!it.exists()) it.mkdir() }
                fos = java.io.FileOutputStream(java.io.File(file, filename))
            }
            fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(this, "✓ Gafete guardado en Galería", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}


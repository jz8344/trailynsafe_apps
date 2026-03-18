package com.trailynsafe.app.fragments

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.trailynsafe.app.R
import com.trailynsafe.app.api.Hijo
import com.trailynsafe.app.ui.hijos.EditarHijoActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream

class HijoDetalleDialog : BottomSheetDialogFragment() {

    private var hijo: Hijo? = null
    private var qrBitmap: Bitmap? = null

    companion object {
        private const val ARG_HIJO_ID = "hijo_id"
        private const val ARG_HIJO_NOMBRE = "hijo_nombre"
        private const val ARG_HIJO_GRADO = "hijo_grado"
        private const val ARG_HIJO_GRUPO = "hijo_grupo"
        private const val ARG_HIJO_ESCUELA = "hijo_escuela"
        private const val ARG_HIJO_ESCUELA_ID = "hijo_escuela_id"
        private const val ARG_HIJO_EMERGENCIA_1 = "hijo_emergencia_1"
        private const val ARG_HIJO_EMERGENCIA_2 = "hijo_emergencia_2"

        fun newInstance(hijo: Hijo): HijoDetalleDialog {
            val dialog = HijoDetalleDialog()
            val args = Bundle()
            args.putInt(ARG_HIJO_ID, hijo.id)
            args.putString(ARG_HIJO_NOMBRE, hijo.nombre)
            args.putString(ARG_HIJO_GRADO, hijo.grado)
            args.putString(ARG_HIJO_GRUPO, hijo.grupo)
            args.putString(ARG_HIJO_ESCUELA, hijo.escuela)
            args.putInt(ARG_HIJO_ESCUELA_ID, hijo.escuela_id)
            args.putString(ARG_HIJO_EMERGENCIA_1, hijo.emergencia_1 ?: "")
            args.putString(ARG_HIJO_EMERGENCIA_2, hijo.emergencia_2 ?: "")
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_hijo_detalle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hijoId = arguments?.getInt(ARG_HIJO_ID) ?: 0
        val nombre = arguments?.getString(ARG_HIJO_NOMBRE) ?: ""
        val grado = arguments?.getString(ARG_HIJO_GRADO) ?: ""
        val grupo = arguments?.getString(ARG_HIJO_GRUPO) ?: ""
        val escuela = arguments?.getString(ARG_HIJO_ESCUELA) ?: ""
        val escuelaId = arguments?.getInt(ARG_HIJO_ESCUELA_ID) ?: -1
        val emergencia1 = arguments?.getString(ARG_HIJO_EMERGENCIA_1) ?: ""
        val emergencia2 = arguments?.getString(ARG_HIJO_EMERGENCIA_2) ?: ""

        view.findViewById<TextView>(R.id.tvNombreHijo).text = nombre
        view.findViewById<TextView>(R.id.tvEscuelaHijo).text =
                escuela.ifEmpty { "Sin escuela asignada" }
        view.findViewById<TextView>(R.id.tvGradoChip).text = "${grado}° Grado"
        view.findViewById<TextView>(R.id.tvGrupoChip).text = "Grupo $grupo"
        view.findViewById<TextView>(R.id.tvQrId).text = "hijo_$hijoId"

        qrBitmap = generateQRCode("hijo_$hijoId", 400)
        view.findViewById<ImageView>(R.id.ivQrCode).setImageBitmap(qrBitmap)

        view.findViewById<MaterialButton>(R.id.btnDescargarCredencial).setOnClickListener {
            descargarCredencial(hijoId, nombre, grado, grupo, escuela)
        }

        view.findViewById<MaterialButton>(R.id.btnEditarHijo).setOnClickListener {
            val intent =
                    Intent(requireContext(), EditarHijoActivity::class.java).apply {
                        putExtra(EditarHijoActivity.EXTRA_HIJO_ID, hijoId)
                        putExtra(EditarHijoActivity.EXTRA_HIJO_NOMBRE, nombre)
                        putExtra(EditarHijoActivity.EXTRA_HIJO_GRADO, grado)
                        putExtra(EditarHijoActivity.EXTRA_HIJO_GRUPO, grupo)
                        putExtra(EditarHijoActivity.EXTRA_HIJO_ESCUELA_ID, escuelaId)
                        putExtra(EditarHijoActivity.EXTRA_HIJO_EMERGENCIA_1, emergencia1)
                        putExtra(EditarHijoActivity.EXTRA_HIJO_EMERGENCIA_2, emergencia2)
                    }
            startActivity(intent)
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnCerrar).setOnClickListener { dismiss() }
    }

    private fun generateQRCode(content: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun descargarCredencial(
            hijoId: Int,
            nombre: String,
            grado: String,
            grupo: String,
            escuela: String
    ) {
        val width = 800
        val height = 500
        val credencialBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(credencialBitmap)

        canvas.drawColor(Color.WHITE)

        val paintBorder =
                Paint().apply {
                    color = Color.parseColor("#8AB4F8")
                    style = Paint.Style.STROKE
                    strokeWidth = 8f
                }
        canvas.drawRect(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat(), paintBorder)

        val paintTitle =
                Paint().apply {
                    color = Color.parseColor("#8AB4F8")
                    textSize = 36f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
        canvas.drawText("Credencial Transporte Escolar", 40f, 70f, paintTitle)

        val paintNombre =
                Paint().apply {
                    color = Color.parseColor("#222222")
                    textSize = 30f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
        canvas.drawText(nombre, 40f, 130f, paintNombre)

        val paintInfo =
                Paint().apply {
                    color = Color.parseColor("#444444")
                    textSize = 24f
                    isAntiAlias = true
                }
        canvas.drawText("Grado: $grado", 40f, 180f, paintInfo)
        canvas.drawText("Grupo: $grupo", 40f, 220f, paintInfo)
        canvas.drawText("Escuela: ${escuela.take(40)}", 40f, 260f, paintInfo)

        val paintInstrucciones =
                Paint().apply {
                    color = Color.parseColor("#666666")
                    textSize = 20f
                    isAntiAlias = true
                }
        canvas.drawText(
                "Escanea el código QR al abordar el transporte",
                40f,
                320f,
                paintInstrucciones
        )

        val paintId =
                Paint().apply {
                    color = Color.parseColor("#8AB4F8")
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
        canvas.drawText("ID: hijo_$hijoId", 40f, 360f, paintId)

        qrBitmap?.let { qr ->
            val scaledQr = Bitmap.createScaledBitmap(qr, 200, 200, true)
            canvas.drawBitmap(scaledQr, 560f, 130f, null)
        }

        val paintFooter =
                Paint().apply {
                    color = Color.parseColor("#8AB4F8")
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
        canvas.drawText("TrailynSafe - Transporte Seguro", 40f, 470f, paintFooter)

        guardarEnGaleria(credencialBitmap, "credencial_${nombre.replace(" ", "_")}_$hijoId")
    }

    private fun guardarEnGaleria(bitmap: Bitmap, fileName: String) {
        try {
            val resolver = requireContext().contentResolver
            val contentValues =
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES + "/TrailynSafe"
                            )
                        }
                    }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                val outputStream: OutputStream? = resolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Toast.makeText(
                                requireContext(),
                                "✅ Credencial guardada en Galería",
                                Toast.LENGTH_LONG
                        )
                        .show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
        }
    }
}


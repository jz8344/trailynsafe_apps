package com.example.trailynapp.driver.ui.qr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.trailynapp.driver.R
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat

class QRScannerActivity : AppCompatActivity() {
    
    private lateinit var barcodeView: DecoratedBarcodeView
    private var isScanComplete = false
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002
        const val EXTRA_QR_CODE = "qr_code"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        barcodeView = findViewById(R.id.barcodeScanner)
        
        // Configurar escáner solo para códigos QR
        val formats = listOf(BarcodeFormat.QR_CODE)
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        
        // Configurar callback de escaneo
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null && !isScanComplete) {
                    isScanComplete = true
                    barcodeView.pause()
                    
                    val qrCode = result.text
                    
                    // Validar formato del QR (hijo_{id})
                    if (qrCode.startsWith("hijo_")) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_QR_CODE, qrCode)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@QRScannerActivity,
                            "⚠️ Código QR inválido. Escanea el QR del hijo.",
                            Toast.LENGTH_SHORT
                        ).show()
                        isScanComplete = false
                        barcodeView.resume()
                    }
                }
            }
            
            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // No necesitamos procesar puntos de resultado
            }
        })
        
        // Solicitar permisos de cámara
        if (checkCameraPermission()) {
            barcodeView.resume()
        } else {
            requestCameraPermission()
        }
    }
    
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                barcodeView.resume()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de cámara requerido para escanear QR",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (checkCameraPermission() && !isScanComplete) {
            barcodeView.resume()
        }
    }
    
    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}

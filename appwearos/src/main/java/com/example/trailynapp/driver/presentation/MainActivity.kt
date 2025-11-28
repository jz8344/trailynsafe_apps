/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.trailynapp.driver.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.presentation.theme.TrailynAppTheme
import com.example.trailynapp.driver.services.HealthDataSyncService
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.BODY_SENSORS] == true) {
            startHealthMonitoring()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        
        // Solicitar permisos de sensores corporales
        requestBodySensorsPermission()

        setContent {
            WearApp()
        }
    }
    
    private fun requestBodySensorsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                startHealthMonitoring()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(Manifest.permission.BODY_SENSORS)
                )
            }
        }
    }
    
    private fun startHealthMonitoring() {
        val intent = Intent(this, HealthDataSyncService::class.java)
        startService(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, HealthDataSyncService::class.java)
        stopService(intent)
    }
}

@Composable
fun WearApp() {
    val dataClient = Wearable.getDataClient(androidx.compose.ui.platform.LocalContext.current)
    
    TrailynAppTheme {
        HealthMonitorScreen(dataClient = dataClient)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun LargeRoundPreview() {
    WearApp()
}
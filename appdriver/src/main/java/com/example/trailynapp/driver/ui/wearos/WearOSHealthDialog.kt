package com.example.trailynapp.driver.ui.wearos

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.services.WearableDataListenerService
import com.example.trailynapp.driver.utils.WearOSHealthManager
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class WearOSHealthDialog : DialogFragment() {
    
    private lateinit var tvHeartRate: TextView
    private lateinit var tvHeartRateStatus: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var cardHeartRate: CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutDisconnected: LinearLayout
    private lateinit var layoutConnected: LinearLayout
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val healthDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WearableDataListenerService.ACTION_HEALTH_DATA_UPDATE) {
                updateHealthUI()
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_wearos_health, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hacer el diálogo más grande
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        initViews(view)
        updateHealthUI()
        startAutoRefresh()
    }
    
    private fun initViews(view: View) {
        tvHeartRate = view.findViewById(R.id.tvHeartRate)
        tvHeartRateStatus = view.findViewById(R.id.tvHeartRateStatus)
        tvSteps = view.findViewById(R.id.tvSteps)
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus)
        tvLastUpdate = view.findViewById(R.id.tvLastUpdate)
        cardHeartRate = view.findViewById(R.id.cardHeartRate)
        progressBar = view.findViewById(R.id.progressBar)
        layoutDisconnected = view.findViewById(R.id.layoutDisconnected)
        layoutConnected = view.findViewById(R.id.layoutConnected)
        
        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }
        
        view.findViewById<View>(R.id.btnRetry).setOnClickListener {
            requestHealthData()
            updateHealthUI()
        }
    }
    
    private fun requestHealthData() {
        val context = requireContext()
        Toast.makeText(context, "Buscando reloj...", Toast.LENGTH_SHORT).show()
        
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)
        
        scope.launch(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.await()
                withContext(Dispatchers.Main) {
                    if (nodes.isEmpty()) {
                        Log.w(TAG, "No nodes found")
                        Toast.makeText(context, "No se encontró reloj conectado via Bluetooth", Toast.LENGTH_LONG).show()
                        updateConnectionStatus(false)
                    } else {
                        Log.d(TAG, "Nodes found: ${nodes.size}")
                        Toast.makeText(context, "Reloj encontrado. Solicitando datos...", Toast.LENGTH_SHORT).show()
                        updateConnectionStatus(true)
                        nodes.forEach { node ->
                            Log.d(TAG, "Sending request to node: ${node.displayName} (${node.id})")
                            messageClient.sendMessage(node.id, "/request_health_data", null).await()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting health data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            layoutDisconnected.visibility = View.GONE
            layoutConnected.visibility = View.VISIBLE
            tvConnectionStatus.text = "✓ Conectado"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            layoutDisconnected.visibility = View.VISIBLE
            layoutConnected.visibility = View.GONE
            tvConnectionStatus.text = "⚠️ Desconectado"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }
    
    private fun updateHealthUI() {
        if (!isAdded) return
        
        val context = requireContext()
        val healthData = WearOSHealthManager.getHealthData(context)
        
        // Si tenemos datos recientes (menos de 1 min), asumimos conectado
        val isDataRecent = (System.currentTimeMillis() - healthData.timestamp) < 60000L
        
        if (healthData.isConnected || isDataRecent) {
            layoutDisconnected.visibility = View.GONE
            layoutConnected.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            
            // Frecuencia cardíaca
            tvHeartRate.text = healthData.heartRate.toString()
            
            val (statusText, colorRes) = WearOSHealthManager.getHeartRateStatus(healthData.heartRate)
            tvHeartRateStatus.text = statusText
            tvHeartRateStatus.setTextColor(resources.getColor(colorRes, null))
            
            // Pasos
            tvSteps.text = healthData.steps.toString()
            
            // Estado de conexión
            tvConnectionStatus.text = "✓ Conectado"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            
            // Última actualización
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            tvLastUpdate.text = "Actualizado: ${dateFormat.format(Date(healthData.timestamp))}"
            
        } else {
            // Solo mostrar desconectado si no hay datos recientes
            // Pero no forzamos la vista de desconectado si acabamos de pedir datos
        }
    }
    
    private fun startAutoRefresh() {
        scope.launch {
            while (isActive) {
                delay(2000L) // Actualizar cada 2 segundos
                updateHealthUI()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(WearableDataListenerService.ACTION_HEALTH_DATA_UPDATE)
        requireContext().registerReceiver(healthDataReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        // Intentar conectar al abrir
        requestHealthData()
    }
    
    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(healthDataReceiver)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
    
    companion object {
        const val TAG = "WearOSHealthDialog"
        
        fun newInstance(): WearOSHealthDialog {
            return WearOSHealthDialog()
        }
    }
}

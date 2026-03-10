package com.example.trailynapp.driver.ui.profile

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.driver.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class SoporteActivity : AppCompatActivity() {

    private lateinit var viewModel: SoporteViewModel
    private lateinit var adapter: SoporteAdapter
    private lateinit var rvChat: RecyclerView
    private lateinit var etMensaje: TextInputEditText
    private lateinit var btnEnviar: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var historialAdapter: HistorialAdapter
    private lateinit var recyclerHistorial: RecyclerView
    private lateinit var btnNuevoChat: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soporte)

        val factory =
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(SoporteViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return SoporteViewModel(this@SoporteActivity) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                    }
                }
        viewModel = ViewModelProvider(this, factory)[SoporteViewModel::class.java]

        initViews()
        setupObservers()

        viewModel.initSoporte()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSoporte)
        
        toolbar.setNavigationOnClickListener { finish() }

        toolbar.menu.add(0, 1, 0, "Info").setIcon(android.R.drawable.ic_dialog_info).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        toolbar.menu.add(0, 2, 0, "Chats").setIcon(android.R.drawable.ic_menu_sort_by_size).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Acerca de Soporte")
                        .setMessage("Este chat es atendido principalmente por nuestro Asistente de Soporte Virtual IA. Sin embargo, nuestros administradores y equipo de soporte también pueden revisar e intervenir si requieres atención humana.")
                        .setPositiveButton("Aceptar", null)
                        .show()
                    true
                }
                2 -> {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START)
                    }
                    true
                }
                else -> false
            }
        }

        rvChat = findViewById(R.id.recyclerChat)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        progressBar = findViewById(R.id.progressBar)

        adapter = SoporteAdapter()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvChat.layoutManager = layoutManager
        rvChat.adapter = adapter

        btnEnviar.setOnClickListener {
            val msj = etMensaje.text.toString().trim()
            if (msj.isNotEmpty()) {
                viewModel.enviarMensaje(msj)
                etMensaje.text?.clear()
            }
        }
        
        recyclerHistorial = findViewById(R.id.recyclerHistorial)
        btnNuevoChat = findViewById(R.id.btnNuevoChat)
        
        historialAdapter = HistorialAdapter { ticket ->
            viewModel.cargarTicket(ticket.id)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        recyclerHistorial.adapter = historialAdapter
        
        btnNuevoChat.setOnClickListener {
            viewModel.nuevoTicket()
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Nuevo chat iniciado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.mensajes.observe(this) { mensajes ->
            adapter.submitList(mensajes)
            if (mensajes.isNotEmpty()) {
                rvChat.scrollToPosition(mensajes.size - 1)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnEnviar.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.tickets.observe(this) { listaTickets ->
            historialAdapter.submitList(listaTickets)
        }
    }
}

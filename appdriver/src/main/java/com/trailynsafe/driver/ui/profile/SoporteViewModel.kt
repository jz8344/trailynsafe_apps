package com.trailynsafe.driver.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailynsafe.driver.api.CrearTicketRequest
import com.trailynsafe.driver.api.EnviarMensajeRequest
import com.trailynsafe.driver.api.RetrofitClient
import com.trailynsafe.driver.api.SoporteMensaje
import com.trailynsafe.driver.utils.SessionManager
import kotlinx.coroutines.launch

class SoporteViewModel(private val context: Context) : ViewModel() {

    private val sessionManager = SessionManager(context)
    private val apiService = RetrofitClient.apiService

    private val _mensajes = MutableLiveData<List<SoporteMensaje>>()
    val mensajes: LiveData<List<SoporteMensaje>>
        get() = _mensajes

    private val _tickets = MutableLiveData<List<com.trailynsafe.driver.api.SoporteTicket>>()
    val tickets: LiveData<List<com.trailynsafe.driver.api.SoporteTicket>>
        get() = _tickets

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var activeTicketId: Int? = null

    fun initSoporte() {
        obtenerTickets()
    }

    private fun obtenerTickets() {
        val token = "Bearer ${sessionManager.getToken()}"
        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val response = apiService.getTicketsSoporte(token)
                if (response.isSuccessful && response.body() != null) {
                    val tickets = response.body()!!
                    _tickets.postValue(tickets)

                    val openTicket =
                            tickets.firstOrNull {
                                it.estado != "cerrado" && it.estado != "resuelto"
                            }

                    if (openTicket != null) {
                        activeTicketId = openTicket.id
                        _mensajes.postValue(openTicket.mensajes ?: emptyList())
                    } else {
                        // No hay ticket abierto
                        _mensajes.postValue(emptyList())
                    }
                } else {
                    _error.postValue("Error al obtener tickets")
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun cargarTicket(ticketId: Int) {
        val token = "Bearer ${sessionManager.getToken()}"
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val response = apiService.getDetalleTicket(token, ticketId)
                if (response.isSuccessful && response.body() != null) {
                    val ticket = response.body()!!
                    activeTicketId = ticket.id
                    _mensajes.postValue(ticket.mensajes ?: emptyList())
                } else {
                    _error.postValue("Error al cargar ticket")
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun nuevoTicket() {
        activeTicketId = null
        _mensajes.postValue(emptyList())
    }

    fun enviarMensaje(texto: String) {
        if (texto.isBlank()) return

        val token = "Bearer ${sessionManager.getToken()}"
        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                if (activeTicketId != null) {
                    // Enviar mensaje a ticket existente
                    val request = EnviarMensajeRequest(mensaje = texto)
                    val response = apiService.enviarMensajeSoporte(token, activeTicketId!!, request)

                    if (response.isSuccessful && response.body() != null) {
                        _mensajes.postValue(response.body()!!.ticket.mensajes ?: emptyList())
                    } else {
                        _error.postValue("Error al enviar mensaje")
                    }
                } else {
                    // Crear nuevo ticket
                    val request =
                            CrearTicketRequest(
                                    asunto = "Asistencia desde App Driver",
                                    descripcion = texto,
                                    categoria = "tecnico",
                                    prioridad = "media"
                            )
                    val response = apiService.crearTicketSoporte(token, request)
                    if (response.isSuccessful && response.body() != null) {
                        activeTicketId = response.body()!!.id
                        _mensajes.postValue(response.body()!!.mensajes ?: emptyList())
                        obtenerTickets() // Actualizar el cajón de historial
                    } else {
                        _error.postValue("Error al iniciar chat de soporte")
                    }
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun eliminarTicketActivo() {
        if (activeTicketId == null) return

        val token = "Bearer ${sessionManager.getToken()}"
        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val response = apiService.eliminarTicketSoporte(token, activeTicketId!!)
                if (response.isSuccessful) {
                    activeTicketId = null
                    _mensajes.postValue(emptyList())
                    obtenerTickets() // Refrescar lista
                } else {
                    _error.postValue("Error al eliminar el chat")
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}

package com.example.trailynapp.driver.api

import retrofit2.Response
import retrofit2.http.*

// Data classes para las requests/responses
data class LoginRequest(
    val correo: String,
    val password: String
)

data class Chofer(
    val id: Int,
    val nombre: String,
    val apellidos: String,
    val correo: String,
    val telefono: String?,
    val licencia: String?,
    val estado: String
)

data class LoginResponse(
    val chofer: Chofer,
    val token: String,
    val message: String
)

data class ErrorResponse(
    val error: String,
    val messages: Map<String, List<String>>? = null
)

interface ApiService {
    
    @POST("api/chofer/login")
    suspend fun loginChofer(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("api/chofer/profile")
    suspend fun getChoferProfile(@Header("Authorization") token: String): Response<LoginResponse>
    
    @POST("api/chofer/logout")
    suspend fun logoutChofer(@Header("Authorization") token: String): Response<Map<String, String>>
    
    @GET("api/chofer/rutas")
    suspend fun getRutasChofer(@Header("Authorization") token: String): Response<List<com.example.trailynapp.driver.ui.trips.Viaje>>
    
    @POST("api/chofer/rutas/{ruta_id}/iniciar")
    suspend fun iniciarRuta(@Header("Authorization") token: String, @Path("ruta_id") rutaId: Int): Response<Map<String, Any>>
    
    @POST("api/chofer/rutas/{ruta_id}/completar")
    suspend fun completarRuta(@Header("Authorization") token: String, @Path("ruta_id") rutaId: Int): Response<Map<String, Any>>
    
    @GET("api/chofer/viajes")
    suspend fun getViajesChofer(@Header("Authorization") token: String): Response<List<com.example.trailynapp.driver.ui.trips.Viaje>>
    
    @POST("api/chofer/viajes/{viaje_id}/abrir-confirmaciones")
    suspend fun abrirConfirmaciones(@Header("Authorization") token: String, @Path("viaje_id") viajeId: Int): Response<Map<String, Any>>
    
    @POST("api/chofer/viajes/{viaje_id}/programar")
    suspend fun programarViaje(@Header("Authorization") token: String, @Path("viaje_id") viajeId: Int): Response<Map<String, Any>>
    
    @POST("api/chofer/viajes/{viaje_id}/cancelar")
    suspend fun cancelarViaje(@Header("Authorization") token: String, @Path("viaje_id") viajeId: Int, @Body body: Map<String, String>): Response<Map<String, Any>>
    
    @POST("api/chofer/viajes/{viaje_id}/cerrar-confirmaciones")
    suspend fun cerrarConfirmaciones(@Header("Authorization") token: String, @Path("viaje_id") viajeId: Int): Response<Map<String, Any>>
    
    @POST("api/chofer/viajes/{viaje_id}/confirmar-viaje")
    suspend fun confirmarViaje(@Header("Authorization") token: String, @Path("viaje_id") viajeId: Int): Response<Map<String, Any>>
}

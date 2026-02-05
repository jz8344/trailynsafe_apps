package com.example.trailynapp.api

import retrofit2.Response
import retrofit2.http.Query
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<Usuario>
    
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("hijos")
    suspend fun getHijos(@Header("Authorization") token: String): Response<List<Hijo>>
    
    @GET("hijos")
    suspend fun obtenerHijos(@Header("Authorization") token: String): Response<List<Hijo>>
    
    @POST("hijos")
    suspend fun createHijo(
        @Header("Authorization") token: String,
        @Body hijo: HijoRequest
    ): Response<Hijo>
    
    @GET("sesion")
    suspend fun getSesion(@Header("Authorization") token: String): Response<List<Sesion>>
    
    @POST("sesiones/cerrar-actual")
    suspend fun cerrarSesion(@Header("Authorization") token: String): Response<MessageResponse>
    
    // Google Authentication
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): Response<GoogleAuthResponse>
    
    // Profile Management
    @POST("editar-perfil")
    suspend fun editarPerfil(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<EditarPerfilResponse>
    
    @POST("cambiar-correo")
    suspend fun cambiarCorreo(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<EditarPerfilResponse>
    
    @POST("validar-password-actual")
    suspend fun validarPasswordActual(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<ValidarPasswordResponse>
    
    @POST("cambiar-contrasena-autenticado")
    suspend fun cambiarContrasena(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<MessageResponse>
    
    // Password Recovery (public endpoints)
    @POST("enviar-codigo")
    suspend fun enviarCodigoRecuperacion(
        @Body body: Map<String, String>
    ): Response<MessageResponse>
    
    @POST("validar-codigo")
    suspend fun validarCodigoRecuperacion(
        @Body body: Map<String, String>
    ): Response<MessageResponse>
    
    @POST("cambiar-contrasena")
    suspend fun restablecerPassword(
        @Body body: Map<String, String>
    ): Response<MessageResponse>
    
    // Viajes - Confirmaciones
    @GET("viajes/disponibles")
    suspend fun getViajesDisponibles(
        @Header("Authorization") token: String
    ): Response<List<ViajeDisponible>>

    // Variante raw para debugging: devuelve JsonElement para permitir leer
    // la clave `debug` devuelta por el servidor cuando se solicita ?debug=1
    @GET("viajes/disponibles")
    suspend fun getViajesDisponiblesRaw(
        @Header("Authorization") token: String,
        @Query("debug") debug: Int? = null
    ): Response<JsonElement>
    
    @GET("confirmaciones/mis-confirmaciones")
    suspend fun getMisConfirmaciones(
        @Header("Authorization") token: String
    ): Response<MisConfirmacionesResponse>
    
    @POST("viajes/{viajeId}/confirmar")
    suspend fun confirmarViaje(
        @retrofit2.http.Path("viajeId") viajeId: Int,
        @Header("Authorization") token: String,
        @Body confirmacion: ConfirmacionRequest
    ): Response<ConfirmacionResponse>
    
}

// Request para confirmar viaje
data class ConfirmacionRequest(
    val hijo_id: Int,
    val direccion_recogida: String,
    val latitud: Double,
    val longitud: Double,
    val referencia: String? = null
)

// Response de confirmación
data class ConfirmacionResponse(
    val message: String,
    val confirmacion: ConfirmacionCreada
)

data class ConfirmacionCreada(
    val id: Int,
    val viaje_id: Int,
    val hijo_id: Int,
    val padre_id: Int,
    val direccion_recogida: String,
    val latitud: Double,
    val longitud: Double,
    val referencia: String?,
    val estado: String,
    val hora_confirmacion: String
)

data class RegisterRequest(
    val nombre: String,
    val apellidos: String,
    val telefono: String,
    val correo: String,
    val contrasena: String
)

data class LoginRequest(
    val correo: String,
    val contrasena: String
)

data class Usuario(
    val id: Int,
    val nombre: String,
    val apellidos: String,
    val telefono: String,
    val correo: String,
    val rol: String,
    val fecha_registro: String
)

data class LoginResponse(
    val usuario: Usuario,
    val token: String
)

data class Hijo(
    val id: Int,
    val nombre: String,
    val apellido: String?,
    val grado: String?,
    val grupo: String?,
    val codigo_qr: String?,
    val padre_id: Int,
    val escuela_id: Int,
    val created_at: String?,
    val updated_at: String?,
    val escuela: String?,
    val emergencia_1: String?,
    val emergencia_2: String?,
    val direccion_alterna: String?,
    val latitud_alterna: String?,
    val longitud_alterna: String?,
    val notas_direccion: String?
)

data class Escuela(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    val activa: Boolean
)

data class HijoRequest(
    val nombre: String,
    val apellidos: String,
    val fecha_nacimiento: String,
    val escuela_id: Int?
)

data class Sesion(
    val id: Int,
    val usuario_id: Int,
    val token: String,
    val user_agent: String?,
    val ip_address: String?,
    val inicio: String,
    val fin: String?,
    val estado: String
)

data class MessageResponse(
    val message: String
)

// Google Authentication
data class GoogleAuthRequest(
    val id_token: String,
    val device_name: String = "android-device"
)

data class GoogleAuthResponse(
    val success: Boolean,
    val message: String,
    val is_new_user: Boolean,
    val data: GoogleAuthData?
)

data class GoogleAuthData(
    val usuario: GoogleUsuario,
    val token: String
)

data class GoogleUsuario(
    val id: Int,
    val nombre: String,
    val apellido: String?,
    val apellidos: String?,
    val correo: String,
    val telefono: String?,
    val rol: String,
    val avatar: String?,
    val google_id: String?,
    val email_verified: Boolean,
    val auth_provider: String?
)

// Profile Management
data class EditarPerfilResponse(
    val message: String,
    val usuario: PerfilUsuario
)

data class PerfilUsuario(
    val id: Int,
    val nombre: String,
    val apellidos: String?,
    val correo: String,
    val telefono: String?,
    val auth_provider: String?,
    val google_id: String?
)

data class ValidarPasswordResponse(
    val message: String,
    val token_validacion: String
)

data class UnidadInfo(
    val matricula: String,
    val modelo: String?,
    val imagen: String?
)

data class ChoferInfo(
    val nombre: String,
    val apellidos: String?,
    val telefono: String?
)

data class EscuelaInfo(
    val nombre: String,
    val direccion: String?
)

data class HijoViajeInfo(
    val id: Int,
    val nombre: String,
    val escuela: String?
)

data class SolicitudViajeResponse(
    val message: String,
    val solicitud: SolicitudViaje
)

data class SolicitudViaje(
    val id: Int,
    val viaje_id: Int,
    val hijo_id: Int,
    val padre_id: Int,
    val estado_confirmacion: String,
    val latitud: Double,
    val longitud: Double,
    val direccion_formateada: String,
    val fecha_confirmacion: String?,
    val created_at: String
)

data class MisSolicitudesResponse(
    val solicitudes: List<SolicitudViajeDetalle>
)

data class SolicitudViajeDetalle(
    val id: Int,
    val viaje_id: Int,
    val estado_confirmacion: String,
    val latitud: Double,
    val longitud: Double,
    val direccion_formateada: String,
    val fecha_confirmacion: String?,
    val viaje: ViajeInfo,
    val hijo: Hijo
)

data class ViajeInfo(
    val id: Int,
    val nombre_ruta: String,
    val horario_salida: String,
    val turno: String,
    val unidad: UnidadInfo?,
    val chofer: ChoferInfo?,
    val escuela: EscuelaInfo?
)

data class ViajesDisponiblesResponse(
    val viajes: List<ViajeDisponible>
)

data class ViajeDisponible(
    val id: Int,
    val escuela_id: Int,
    val chofer_id: Int?,
    val unidad_id: Int?,
    val tipo_viaje: String,
    val fecha_viaje: String,
    val hora_salida_programada: String,
    val hora_salida_real: String?,
    val hora_llegada_estimada: String?,
    val hora_llegada_real: String?,
    val estado: String,
    val escuela: EscuelaDetalle?,
    val chofer: ChoferDetalle?,
    val unidad: UnidadDetalle?
)

data class EscuelaDetalle(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val latitud: Double?,
    val longitud: Double?,
    val telefono: String?
)

data class ChoferDetalle(
    val id: Int,
    val nombre: String,
    val apellidos: String?,
    val telefono: String?
)

data class UnidadDetalle(
    val id: Int,
    val matricula: String,
    val modelo: String?,
    val capacidad: Int?
)

data class MisConfirmacionesResponse(
    val confirmaciones: List<ConfirmacionDetalle>
)

data class ConfirmacionDetalle(
    val id: Int,
    val viaje_id: Int,
    val hijo_id: Int,
    val padre_id: Int,
    val estado_confirmacion: String,
    val latitud: Double?,
    val longitud: Double?,
    val direccion_formateada: String?,
    val fecha_confirmacion: String?,
    val created_at: String,
    val viaje: ViajeDisponible?,
    val hijo: Hijo?
)


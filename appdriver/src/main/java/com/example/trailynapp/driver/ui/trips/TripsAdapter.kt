package com.example.trailynapp.driver.ui.trips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.driver.R
import com.google.android.material.button.MaterialButton

/**
 * Respuesta del endpoint /api/chofer/viajes con lógica tipo alarma
 */
data class ViajesResponse(
    val fecha_actual: String,
    val hora_actual: String,
    val dia_semana: Int, // 0=Dom, 1=Lun...6=Sab
    val dia_nombre: String,
    val viajes_hoy: List<Viaje>,
    val viajes_otros: List<Viaje>,
    val total_viajes: Int
)

/**
 * Viaje con estado efectivo calculado en tiempo real (lógica tipo alarma)
 */
data class Viaje(
    val id: Int,
    val nombre: String?,
    val tipo_viaje: String,
    val turno: String?,
    val hora_salida_programada: String?,
    val cupo_minimo: Int?,
    val cupo_maximo: Int?,
    
    // Estado en BD vs Estado Efectivo (calculado en tiempo real)
    val estado_bd: String?,          // Estado guardado en la base de datos
    val estado: String,              // Estado efectivo calculado (tipo alarma)
    val estado_mensaje: String?,     // Mensaje descriptivo del estado
    val interactuable: Boolean,      // ¿Se puede interactuar ahora?
    val estado_datos: EstadoDatos?,  // Datos adicionales del estado
    
    // Confirmaciones
    val confirmaciones_hoy: Int?,    // Confirmaciones para HOY
    val confirmaciones_actuales: Int?, // Total histórico
    val puede_generar_ruta: Boolean?,
    val puede_iniciar: Boolean?,
    
    // ¿Aplica hoy?
    val aplica_hoy: Boolean,
    
    // Relaciones
    val escuela: Escuela?,
    val unidad: Unidad?,
    val ruta: Ruta?,
    
    // Para compatibilidad legacy
    val escuela_id: Int? = null,
    val chofer_id: Int? = null,
    val unidad_id: Int? = null,
    val fecha_viaje: String? = null
)

data class EstadoDatos(
    val hora_salida: String?,
    val minutos_para_salida: Int?,
    val confirmaciones_hoy: Int?,
    val cupo_minimo: Int?,
    val cupo_maximo: Int?,
    val cupo_disponible: Int?,
    val ventana_cierra_en: String?,
    val minutos_para_interactuar: Int?,
    val confirmaciones_abren: String?,
    val minutos_para_abrir: Int?,
    val cumple_minimo: Boolean?,
    val proxima_fecha: String?,
    val motivo: String?,
    val ruta_id: Int?,
    val expiro_hace: String?
)

data class Unidad(
    val id: Int,
    val matricula: String?,
    val capacidad: Int?
)

data class Ruta(
    val id: Int,
    val estado: String?,
    val distancia_total_km: Double?,
    val tiempo_estimado_minutos: Int?,
    val polyline: String?,
    val paradas: List<Parada>?,
    // Legacy
    val nombre: String? = null,
    val viaje_id: Int? = null,
    val escuela_id: Int? = null
)

data class Parada(
    val id: Int,
    val orden: Int,
    val direccion: String,
    val latitud: String?,
    val longitud: String?,
    val hora_estimada: String?,
    val estado: String,
    // Legacy
    val ruta_id: Int? = null,
    val confirmacion_id: Int? = null,
    val distancia_desde_anterior_km: String? = null,
    val tiempo_desde_anterior_min: Int? = null,
    val cluster_asignado: Int? = null
)

data class Escuela(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val latitud: String?,
    val longitud: String?
)

class TripsAdapter(
    private val onTripClick: (Viaje) -> Unit
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {
    
    private var trips = listOf<Viaje>()
    
    fun submitList(newTrips: List<Viaje>) {
        trips = newTrips
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_viaje, parent, false)
        return TripViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }
    
    override fun getItemCount() = trips.size
    
    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEscuela: TextView = itemView.findViewById(R.id.tvEscuela)
        private val tvHorario: TextView = itemView.findViewById(R.id.tvHorario)
        private val tvTipoViaje: TextView = itemView.findViewById(R.id.tvTipoViaje)
        private val badgeEstado: TextView = itemView.findViewById(R.id.badgeEstado)
        private val btnAccion: MaterialButton = itemView.findViewById(R.id.btnAccion)
        
        fun bind(viaje: Viaje) {
            val escuela = viaje.escuela
            val context = itemView.context
            
            tvEscuela.text = escuela?.nombre ?: "Escuela"
            
            // Mostrar información contextual según tipo de viaje
            val tipoInfo = when {
                viaje.turno == "matutino" -> "🌅 Matutino"
                viaje.turno == "vespertino" -> "🌆 Vespertino"
                viaje.tipo_viaje == "unico" -> "📅 Único"
                else -> "🔄 Recurrente"
            }
            tvTipoViaje.text = tipoInfo
            
            // Formato de horario con confirmaciones
            val horario = viaje.hora_salida_programada ?: "Sin horario"
            val horaCorta = if (horario.length >= 5) horario.substring(0, 5) else horario
            val confirmaciones = viaje.confirmaciones_hoy ?: 0
            val cupoMax = viaje.cupo_maximo ?: 0
            tvHorario.text = "⏰ $horaCorta  👥 $confirmaciones/$cupoMax"
            
            // ========== ESTADO EFECTIVO (LÓGICA TIPO ALARMA) ==========
            // Usar el estado calculado en tiempo real, no el de la BD
            val estadoEfectivo = viaje.estado
            val estadoBd = viaje.estado_bd ?: viaje.estado
            val mensaje = viaje.estado_mensaje ?: ""
            
            badgeEstado.text = when (estadoEfectivo) {
                "no_aplica" -> "📅 No aplica hoy"
                "programado" -> "⏳ ${viaje.estado_datos?.confirmaciones_abren ?: "Esperando"}"
                "en_confirmaciones" -> "📝 Confirmando (${viaje.estado_datos?.minutos_para_salida ?: ""}min)"
                "confirmado" -> "✅ Listo - ${viaje.estado_datos?.minutos_para_interactuar ?: 0}min"
                "interactuable" -> "🟢 ¡LISTO!"
                "en_curso" -> "🚌 En Progreso"
                "expirado" -> "⏰ Expirado"
                "finalizado" -> "✓ Completado"
                "cancelado" -> "❌ Cancelado"
                else -> estadoEfectivo
            }
            
            // Colores según estado
            val colorRes = when (estadoEfectivo) {
                "interactuable" -> R.color.success_green
                "en_curso" -> R.color.warning_orange
                "en_confirmaciones" -> R.color.warning_yellow
                "expirado", "cancelado" -> R.color.error_red
                "no_aplica" -> R.color.disabled_gray
                else -> R.color.primary_blue
            }
            
            // Aplicar estilo visual según interactuabilidad
            val alpha = if (viaje.interactuable || estadoEfectivo == "en_confirmaciones") 1.0f else 0.6f
            itemView.alpha = alpha
            
            when (estadoBd) {
                "pendiente" -> {
                    btnAccion.text = "📋 Programar Viaje"
                    btnAccion.isEnabled = true
                }
                "programado" -> {
                    btnAccion.text = "🔔 Abrir Confirmaciones"
                    btnAccion.isEnabled = true
                }
                "en_confirmaciones" -> {
                    val confirmacionesHoy = viaje.confirmaciones_hoy ?: 0
                    val cupoMinimo = viaje.cupo_minimo ?: 1
                    if (confirmacionesHoy >= cupoMinimo) {
                        btnAccion.text = "🔒 Cerrar Confirmaciones"
                        btnAccion.isEnabled = true
                    } else {
                        btnAccion.text = "⏳ Esperando confirmaciones ($confirmacionesHoy/$cupoMinimo)"
                        btnAccion.isEnabled = false
                    }
                }
                "confirmado" -> {
                    btnAccion.text = "📍 Generar Ruta"
                    btnAccion.isEnabled = true
                }
                "generando_ruta" -> {
                    btnAccion.text = "⚙️ Generando ruta..."
                    btnAccion.isEnabled = false
                }
                "ruta_generada" -> {
                    btnAccion.text = "🚀 INICIAR VIAJE"
                    btnAccion.isEnabled = true
                }
                "en_curso" -> {
                    btnAccion.text = "🗺️ Abrir Navegación"
                    btnAccion.isEnabled = true
                }
                "finalizado" -> {
                    btnAccion.text = "✓ Completado"
                    btnAccion.isEnabled = false
                }
                "cancelado" -> {
                    btnAccion.text = "❌ Cancelado"
                    btnAccion.isEnabled = false
                }
                else -> {
                    btnAccion.text = "Ver Detalles"
                    btnAccion.isEnabled = true
                }
            }
            itemView.setOnClickListener {
                onTripClick(viaje)
            }
            
            btnAccion.setOnClickListener {
                onTripClick(viaje)
            }
        }
    }
}

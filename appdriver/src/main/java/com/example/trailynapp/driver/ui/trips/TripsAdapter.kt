package com.example.trailynapp.driver.ui.trips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.driver.R
import com.google.android.material.button.MaterialButton

data class Viaje(
    val id: Int,
    val escuela_id: Int,
    val chofer_id: Int?,
    val unidad_id: Int?,
    val tipo_viaje: String,
    val fecha_viaje: String?,
    val hora_salida_programada: String?,
    val estado: String,
    val cupo_minimo: Int?,
    val cupo_maximo: Int?,
    val confirmaciones_actuales: Int?,
    val escuela: Escuela?,
    val ruta: Ruta?
)

data class Ruta(
    val id: Int,
    val nombre: String?,
    val viaje_id: Int,
    val escuela_id: Int,
    val estado: String?,
    val distancia_total_km: Double?,
    val tiempo_estimado_minutos: Int?,
    val polyline: String?, // Polyline codificada de Google Maps
    val paradas: List<Parada>?
)

data class Parada(
    val id: Int,
    val ruta_id: Int,
    val confirmacion_id: Int,
    val orden: Int,
    val direccion: String,
    val latitud: String?,
    val longitud: String?,
    val hora_estimada: String,
    val distancia_desde_anterior_km: String?,
    val tiempo_desde_anterior_min: Int?,
    val cluster_asignado: Int?,
    val estado: String
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
            
            tvEscuela.text = escuela?.nombre ?: "Escuela"
            tvTipoViaje.text = when (viaje.tipo_viaje) {
                "ida" -> "Viaje de Ida"
                "vuelta" -> "Viaje de Vuelta"
                else -> "Viaje"
            }
            
            // Formato de horario simple
            val horario = viaje.hora_salida_programada ?: "Sin horario"
            tvHorario.text = if (horario.length >= 5) {
                horario.substring(0, 5)
            } else {
                horario
            }
            
            // Estado del viaje
            badgeEstado.text = when (viaje.estado) {
                "pendiente" -> "Pendiente"
                "programado" -> "Programado"
                "en_confirmaciones" -> "En confirmaciones"
                "confirmado" -> "Confirmado"
                "ruta_generada" -> "Listo para iniciar"
                "en_curso" -> "En Progreso"
                "completado" -> "Completado"
                "finalizado" -> "Finalizado"
                "cancelado" -> "Cancelado"
                else -> viaje.estado
            }
            
            // Botón de acción según estado
            when (viaje.estado) {
                "pendiente" -> {
                    btnAccion.text = "Programar"
                    btnAccion.isEnabled = true
                }
                "programado" -> {
                    btnAccion.text = "Abrir Confirmaciones"
                    btnAccion.isEnabled = true
                }
                "en_confirmaciones" -> {
                    btnAccion.text = "En Confirmaciones"
                    btnAccion.isEnabled = false
                }
                "confirmado" -> {
                    btnAccion.text = "Confirmado"
                    btnAccion.isEnabled = false
                }
                "ruta_generada" -> {
                    btnAccion.text = "Comenzar Viaje"
                    btnAccion.isEnabled = true
                }
                "en_curso" -> {
                    btnAccion.text = "En Progreso..."
                    btnAccion.isEnabled = true
                }
                "completado", "finalizado" -> {
                    btnAccion.text = "Completado"
                    btnAccion.isEnabled = false
                }
                "cancelado" -> {
                    btnAccion.text = "Cancelado"
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

package com.trailynsafe.app.fragments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trailynsafe.app.R
import com.trailynsafe.app.api.ViajeDisponible

class ViajesDisponiblesAdapter(
        private val viajes: List<ViajeDisponible>,
        private val viajesConfirmados: Set<Int> = emptySet(),
        private val viajesEnCurso: Set<Int> = emptySet(),
        private val onItemClick: (ViajeDisponible) -> Unit
) : RecyclerView.Adapter<ViajesDisponiblesAdapter.ViajeViewHolder>() {

    inner class ViajeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEscuela: TextView = itemView.findViewById(R.id.tvEscuela)
        val tvHorario: TextView = itemView.findViewById(R.id.tvHorario)
        val tvChofer: TextView = itemView.findViewById(R.id.tvChofer)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvBadgeConfirmado: TextView = itemView.findViewById(R.id.tvBadgeConfirmado)

        init {
            itemView.setOnClickListener { onItemClick(viajes[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViajeViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_viaje_disponible, parent, false)
        return ViajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViajeViewHolder, position: Int) {
        val viaje = viajes[position]
        val context = holder.itemView.context

        val turnoLabel =
                when (viaje.turno) {
                    "matutino" -> "Matutino"
                    "vespertino" -> "Vespertino"
                    "nocturno" -> "Nocturno"
                    else -> viaje.turno?.replaceFirstChar { it.uppercase() } ?: ""
                }
        val nombreViaje =
                viaje.nombre?.takeIf { it.isNotBlank() }
                        ?: viaje.escuela?.nombre ?: context.getString(R.string.school_not_specified)
        holder.tvEscuela.text =
                if (turnoLabel.isNotBlank()) "$nombreViaje · $turnoLabel" else nombreViaje

        holder.tvHorario.text = "🕐 ${viaje.hora_salida_programada ?: "—"}"
        holder.tvChofer.text =
                "👨‍✈️ ${viaje.chofer?.nombre ?: context.getString(R.string.driver_not_assigned)}"
        holder.tvFecha.text = viaje.fecha_viaje ?: ""

        when {
            viajesEnCurso.contains(viaje.id) -> {
                holder.tvBadgeConfirmado.visibility = View.VISIBLE
                holder.tvBadgeConfirmado.text = "🚐 En camino"
                holder.tvBadgeConfirmado.setBackgroundColor(Color.parseColor("#1B5E20"))
            }
            viajesConfirmados.contains(viaje.id) -> {
                holder.tvBadgeConfirmado.visibility = View.VISIBLE
                holder.tvBadgeConfirmado.text = "✓ Confirmado"
                holder.tvBadgeConfirmado.setBackgroundColor(Color.parseColor("#2E7D32"))
            }
            else -> {
                holder.tvBadgeConfirmado.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = viajes.size
}


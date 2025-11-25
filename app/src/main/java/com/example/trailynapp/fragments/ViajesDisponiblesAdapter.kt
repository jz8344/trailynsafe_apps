package com.example.trailynapp.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.R
import com.example.trailynapp.api.ViajeDisponible

class ViajesDisponiblesAdapter(
    private val viajes: List<ViajeDisponible>,
    private val onItemClick: (ViajeDisponible) -> Unit
) : RecyclerView.Adapter<ViajesDisponiblesAdapter.ViajeViewHolder>() {

    inner class ViajeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEscuela: TextView = itemView.findViewById(R.id.tvEscuela)
        val tvHorario: TextView = itemView.findViewById(R.id.tvHorario)
        val tvChofer: TextView = itemView.findViewById(R.id.tvChofer)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)

        init {
            itemView.setOnClickListener {
                onItemClick(viajes[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViajeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_viaje_disponible, parent, false)
        return ViajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViajeViewHolder, position: Int) {
        val viaje = viajes[position]
        val context = holder.itemView.context
        holder.tvEscuela.text = viaje.escuela?.nombre ?: context.getString(R.string.school_not_specified)
        holder.tvHorario.text = "🕐 ${viaje.hora_salida_programada}"
        holder.tvChofer.text = "👨‍✈️ ${viaje.chofer?.nombre ?: context.getString(R.string.driver_not_assigned)}"
        holder.tvFecha.text = viaje.fecha_viaje
    }

    override fun getItemCount() = viajes.size
}

package com.example.trailynapp.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.R
import com.example.trailynapp.api.ConfirmacionDetalle

class ViajesAdapter(
    private val confirmaciones: List<ConfirmacionDetalle>,
    private val onItemClick: (ConfirmacionDetalle) -> Unit
) : RecyclerView.Adapter<ViajesAdapter.ViajeViewHolder>() {

    class ViajeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvEscuela: TextView = view.findViewById(R.id.tvEscuela)
        val tvHijo: TextView = view.findViewById(R.id.tvHijo)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvHorario: TextView = view.findViewById(R.id.tvHorario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViajeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_viaje_usuario, parent, false)
        return ViajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViajeViewHolder, position: Int) {
        val confirmacion = confirmaciones[position]
        
        holder.tvFecha.text = confirmacion.viaje?.fecha_viaje ?: "Fecha no disponible"
        holder.tvEscuela.text = confirmacion.viaje?.escuela?.nombre ?: "Escuela no asignada"
        holder.tvHijo.text = confirmacion.hijo?.nombre ?: "Hijo no especificado"
        holder.tvEstado.text = when (confirmacion.estado_confirmacion) {
            "confirmado" -> "✓ Confirmado"
            "pendiente" -> "⏳ Pendiente"
            "cancelado" -> "✗ Cancelado"
            else -> confirmacion.estado_confirmacion
        }
        holder.tvHorario.text = confirmacion.viaje?.hora_salida_programada ?: "Horario no disponible"
        
        holder.itemView.setOnClickListener {
            onItemClick(confirmacion)
        }
    }

    override fun getItemCount() = confirmaciones.size
}

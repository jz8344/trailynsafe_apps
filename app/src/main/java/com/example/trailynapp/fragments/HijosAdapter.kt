package com.example.trailynapp.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.R
import com.example.trailynapp.api.Hijo

class HijosAdapter(private val hijos: List<Hijo>) : RecyclerView.Adapter<HijosAdapter.HijoViewHolder>() {

    class HijoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvEscuela: TextView = view.findViewById(R.id.tvEscuela)
        val tvEdad: TextView = view.findViewById(R.id.tvEdad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HijoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hijo, parent, false)
        return HijoViewHolder(view)
    }

    override fun onBindViewHolder(holder: HijoViewHolder, position: Int) {
        val hijo = hijos[position]
        holder.tvNombre.text = hijo.nombre
        holder.tvEscuela.text = hijo.escuela ?: "Sin escuela asignada"
        
        // Mostrar grado y grupo
        val gradoGrupo = if (hijo.grado != null && hijo.grupo != null) {
            "Grado ${hijo.grado} - Grupo ${hijo.grupo}"
        } else {
            "Grado no asignado"
        }
        holder.tvEdad.text = gradoGrupo
    }

    override fun getItemCount() = hijos.size
}

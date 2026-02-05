package com.example.trailynapp.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trailynapp.R
import com.example.trailynapp.api.Hijo

class HijosAdapter(
    private val hijos: List<Hijo>,
    private val onHijoClick: ((Hijo) -> Unit)? = null
) : RecyclerView.Adapter<HijosAdapter.HijoViewHolder>() {

    class HijoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvEscuela: TextView = view.findViewById(R.id.tvEscuela)
        val tvGrado: TextView = view.findViewById(R.id.tvGrado)
        val tvGrupo: TextView = view.findViewById(R.id.tvGrupo)
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
        holder.tvGrado.text = "${hijo.grado ?: "-"}° Grado"
        holder.tvGrupo.text = "Grupo ${hijo.grupo ?: "-"}"

        holder.itemView.setOnClickListener {
            onHijoClick?.invoke(hijo)
        }
    }

    override fun getItemCount() = hijos.size
}

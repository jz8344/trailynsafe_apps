package com.trailynsafe.driver.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trailynsafe.driver.R
import com.trailynsafe.driver.api.SoporteTicket
import java.text.SimpleDateFormat
import java.util.Locale

class HistorialAdapter(
    private val onTicketSelected: (SoporteTicket) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    private val tickets = mutableListOf<SoporteTicket>()
    private val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
    private val sdfOut = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun submitList(newList: List<SoporteTicket>) {
        tickets.clear()
        tickets.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ticket = tickets[position]
        
        var dateFormatted = ticket.created_at
        try {
            val date = sdfIn.parse(ticket.created_at)
            if (date != null) {
                dateFormatted = sdfOut.format(date)
            }
        } catch (e: Exception) {
            // keep original
        }

        // Title text
        val title = if (ticket.asunto.isNotEmpty()) ticket.asunto else "Chat #${ticket.id}"
        holder.tvTitle.text = title
        
        // Subtitle text
        val estadoStr = if (ticket.estado == "abierto" || ticket.estado == "en_proceso") "Activo" else "Cerrado"
        holder.tvSubtitle.text = "$dateFormatted • $estadoStr"
        
        holder.itemView.setOnClickListener {
            onTicketSelected(ticket)
        }
    }

    override fun getItemCount(): Int = tickets.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(android.R.id.text1)
        val tvSubtitle: TextView = view.findViewById(android.R.id.text2)
        
        init {
            tvTitle.setTextColor(view.resources.getColor(com.trailynsafe.driver.R.color.m3_on_surface, null))
            tvSubtitle.setTextColor(view.resources.getColor(com.trailynsafe.driver.R.color.m3_on_surface_variant, null))
        }
    }
}

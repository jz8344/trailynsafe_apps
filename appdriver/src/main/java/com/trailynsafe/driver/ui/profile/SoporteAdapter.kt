package com.trailynsafe.driver.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.trailynsafe.driver.R
import com.trailynsafe.driver.api.SoporteMensaje
import java.text.SimpleDateFormat
import java.util.Locale

class SoporteAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mensajes = mutableListOf<SoporteMensaje>()
    private val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
    private val sdfOut = SimpleDateFormat("HH:mm", Locale.getDefault())

    companion object {
        const val VIEW_TYPE_USUARIO = 1
        const val VIEW_TYPE_BOT = 2
    }

    fun submitList(newList: List<SoporteMensaje>) {
        mensajes.clear()
        mensajes.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (mensajes[position].tipo == "usuario") {
            VIEW_TYPE_USUARIO
        } else {
            VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USUARIO) {
            val view = inflater.inflate(R.layout.item_mensaje_usuario, parent, false)
            UsuarioViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_mensaje_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = mensajes[position]

        // Formatear hora
        var horaFormat = ""
        try {
            val date = sdfIn.parse(mensaje.created_at)
            if (date != null) {
                horaFormat = sdfOut.format(date)
            }
        } catch (e: Exception) {
            horaFormat = ""
        }

        val copyAction = View.OnLongClickListener {
            val clipboard = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Mensaje", mensaje.mensaje)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(holder.itemView.context, "Mensaje copiado", Toast.LENGTH_SHORT).show()
            true
        }

        if (holder is UsuarioViewHolder) {
            holder.tvMensaje.text = mensaje.mensaje
            holder.tvHora.text = horaFormat
            holder.itemView.setOnLongClickListener(copyAction)
        } else if (holder is BotViewHolder) {
            holder.tvMensaje.text = mensaje.mensaje
            holder.tvHora.text = horaFormat
            holder.itemView.setOnLongClickListener(copyAction)
        }
    }

    override fun getItemCount(): Int = mensajes.size

    class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
        val tvHora: TextView = view.findViewById(R.id.tvHora)
    }

    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
        val tvHora: TextView = view.findViewById(R.id.tvHora)
    }
}

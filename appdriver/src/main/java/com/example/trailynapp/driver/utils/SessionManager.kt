package com.example.trailynapp.driver.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "TrailynAppDriver"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_TOKEN = "token"
        private const val KEY_CHOFER_ID = "chofer_id"
        private const val KEY_NOMBRE = "nombre"
        private const val KEY_APELLIDOS = "apellidos"
        private const val KEY_CORREO = "correo"
        private const val KEY_TELEFONO = "telefono"
    }
    
    fun saveLoginSession(
        token: String,
        choferId: Int,
        nombre: String,
        apellidos: String = "",
        correo: String,
        telefono: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_TOKEN, token)
            putInt(KEY_CHOFER_ID, choferId)
            putString(KEY_NOMBRE, nombre)
            putString(KEY_APELLIDOS, apellidos)
            putString(KEY_CORREO, correo)
            putString(KEY_TELEFONO, telefono)
            apply()
        }
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun getChoferId(): Int {
        return prefs.getInt(KEY_CHOFER_ID, -1)
    }
    
    fun getNombre(): String {
        return prefs.getString(KEY_NOMBRE, "Conductor") ?: "Conductor"
    }
    
    fun getApellidos(): String {
        return prefs.getString(KEY_APELLIDOS, "") ?: ""
    }
    
    fun getCorreo(): String {
        return prefs.getString(KEY_CORREO, "") ?: ""
    }
    
    fun getTelefono(): String {
        return prefs.getString(KEY_TELEFONO, "") ?: ""
    }
    
    // Métodos con prefijo getChofer (alias para compatibilidad)
    fun getChoferNombre(): String = getNombre()
    fun getChoferApellidos(): String = getApellidos()
    fun getChoferCorreo(): String = getCorreo()
    fun getChoferTelefono(): String = getTelefono()
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}

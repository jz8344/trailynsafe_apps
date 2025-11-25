package com.example.trailynapp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "TrailynApp"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "usuario_id"
        private const val KEY_NOMBRE = "nombre"
        private const val KEY_APELLIDOS = "apellidos"
        private const val KEY_CORREO = "correo"
        private const val KEY_TELEFONO = "telefono"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_GOOGLE_ID = "google_id"
        private const val KEY_SELECTED_HIJO_ID = "selected_hijo_id"
    }
    
    fun saveLoginSession(
        token: String,
        userId: Int,
        nombre: String,
        apellidos: String = "",
        correo: String,
        telefono: String = "",
        authProvider: String? = null,
        googleId: String? = null
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_NOMBRE, nombre)
            putString(KEY_APELLIDOS, apellidos)
            putString(KEY_CORREO, correo)
            putString(KEY_TELEFONO, telefono)
            putString(KEY_AUTH_PROVIDER, authProvider)
            putString(KEY_GOOGLE_ID, googleId)
            apply()
        }
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }
    
    fun getNombre(): String {
        return prefs.getString(KEY_NOMBRE, "Usuario") ?: "Usuario"
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
    
    fun getAuthProvider(): String? {
        return prefs.getString(KEY_AUTH_PROVIDER, null)
    }
    
    fun getGoogleId(): String? {
        return prefs.getString(KEY_GOOGLE_ID, null)
    }
    
    fun isGoogleAccount(): Boolean {
        return getAuthProvider() == "google" || getGoogleId() != null
    }
    
    fun saveSelectedHijoId(hijoId: Int) {
        prefs.edit().putInt(KEY_SELECTED_HIJO_ID, hijoId).apply()
    }
    
    fun getSelectedHijoId(): Int {
        return prefs.getInt(KEY_SELECTED_HIJO_ID, -1)
    }
    
    fun clearSelectedHijo() {
        prefs.edit().remove(KEY_SELECTED_HIJO_ID).apply()
    }
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}

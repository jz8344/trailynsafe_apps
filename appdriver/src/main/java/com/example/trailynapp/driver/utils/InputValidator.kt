package com.example.trailynapp.driver.utils

import android.text.InputFilter
import android.util.Patterns

object InputValidator {

    private val NAME_REGEX = Regex("^[a-zA-ZáéíóúüñÁÉÍÓÚÜÑ ]+$")
    private val PHONE_REGEX = Regex("^[0-9]{10}$")

    fun sanitizeName(input: String): String =
            input.trim().replace(Regex("[^a-zA-ZáéíóúüñÁÉÍÓÚÜÑ ]"), "").take(60)

    fun sanitizePhone(input: String): String = input.trim().replace(Regex("[^0-9]"), "").take(10)

    fun sanitizeEmail(input: String): String = input.trim().lowercase().take(254)

    fun sanitizePassword(input: String): String = input.take(128)

    fun validateName(input: String): String? {
        val s = sanitizeName(input)
        if (s.length < 2) return "Mínimo 2 caracteres"
        if (!NAME_REGEX.matches(s)) return "Solo letras y acentos permitidos"
        return null
    }

    fun validatePhone(input: String): String? {
        val s = sanitizePhone(input)
        if (!PHONE_REGEX.matches(s)) return "Debe tener exactamente 10 dígitos"
        return null
    }

    fun validateEmail(input: String): String? {
        val s = sanitizeEmail(input)
        if (s.isEmpty()) return "El correo es requerido"
        if (!Patterns.EMAIL_ADDRESS.matcher(s).matches()) return "Correo inválido"
        return null
    }

    fun validatePassword(input: String): String? {
        if (input.length < 8) return "Mínimo 8 caracteres"
        return null
    }

    fun nameFilter(maxLen: Int = 60): InputFilter = InputFilter { source, _, _, _, _, _ ->
        source.replace(Regex("[^a-zA-ZáéíóúüñÁÉÍÓÚÜÑ ]"), "").take(maxLen)
    }

    fun phoneFilter(): InputFilter = InputFilter { source, _, _, _, _, _ ->
        source.replace(Regex("[^0-9]"), "").take(10)
    }

    fun lengthFilter(max: Int) = InputFilter.LengthFilter(max)
}

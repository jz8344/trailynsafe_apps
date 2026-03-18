package com.trailynsafe.app.utils

import android.text.InputFilter
import android.util.Patterns

object InputValidator {

        private val NAME_REGEX = Regex("^[a-zA-ZáéíóúüñÁÉÍÓÚÜÑ ]+$")
        private val PHONE_REGEX = Regex("^[0-9]{10}$")

        fun sanitizeName(input: String): String =
                input.trim()
                        .replace(Regex("[^a-zA-ZáéíóúüñÁÉÍÓÚÜÑ ]"), "")
                        .replace(Regex(" {2,}"), " ")
                        .take(60)

        fun sanitizePhone(input: String): String =
                input.trim().replace(Regex("[^0-9]"), "").take(10)

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

        private val VALID_TLDS =
                setOf(
                        "com",
                        "net",
                        "org",
                        "edu",
                        "mx",
                        "io",
                        "co",
                        "us",
                        "info",
                        "biz",
                        "gov",
                        "gob",
                        "gg",
                        "app",
                        "dev",
                        "ai",
                        "cloud",
                        "online",
                        "store"
                )

        fun validateEmail(input: String): String? {
                val s = sanitizeEmail(input)
                if (s.isEmpty()) return "El correo es requerido"
                if (!Patterns.EMAIL_ADDRESS.matcher(s).matches()) return "Correo inválido"
                val tld = s.substringAfterLast(".").lowercase()
                if (tld !in VALID_TLDS) return "Dominio de correo no reconocido"
                return null
        }

        fun parseServerError(rawError: String?): String {
                if (rawError.isNullOrBlank()) return "Error desconocido"
                try {
                        val json = org.json.JSONObject(rawError)
                        val errors = json.optJSONObject("errors")
                        if (errors != null) {
                                val firstKey = errors.keys().next()
                                val firstMsg = errors.getJSONArray(firstKey).getString(0)
                                val lower = firstMsg.lowercase()
                                return when {
                                        lower.contains("already been taken") ||
                                                lower.contains("ya fue tomado") ->
                                                "Este correo ya ha sido registrado"
                                        lower.contains("required") -> "Este campo es requerido"
                                        lower.contains("invalid") ->
                                                "El valor ingresado no es válido"
                                        lower.contains("min") || lower.contains("least") ->
                                                "El valor es demasiado corto"
                                        else -> firstMsg
                                }
                        }
                        val message = json.optString("message", "")
                        if (message.isNotBlank()) {
                                val lower = message.lowercase()
                                return when {
                                        lower.contains("already been taken") ->
                                                "Este correo ya ha sido registrado"
                                        lower.contains("throttle") || lower.contains("too many") ->
                                                "Demasiados intentos. Espera un momento"
                                        lower.contains("unauthenticated") -> "Sesión expirada"
                                        else -> message
                                }
                        }
                } catch (_: Exception) {}
                val lower = rawError.lowercase()
                return when {
                        lower.contains("already been taken") || lower.contains("ya fue tomado") ->
                                "Este correo ya ha sido registrado"
                        lower.contains("throttle") || lower.contains("too many") ->
                                "Demasiados intentos. Espera un momento e intenta de nuevo"
                        lower.contains("unauthenticated") || lower.contains("unauthorized") ->
                                "Sesión expirada. Por favor inicia sesión de nuevo"
                        lower.contains("server error") || lower.contains("500") ->
                                "Error en el servidor. Intenta de nuevo más tarde"
                        lower.contains("network") || lower.contains("timeout") ->
                                "Error de conexión. Verifica tu internet"
                        else -> "Ocurrió un error. Inténtalo de nuevo"
                }
        }

        fun validatePassword(input: String): String? {
                if (input.length < 8) return "Mínimo 8 caracteres"
                return null
        }

        fun nameFilter(maxLen: Int = 60): InputFilter =
                InputFilter { source, start, end, dest, dStart, dEnd ->
                        val cleaned =
                                source.subSequence(start, end)
                                        .replace(
                                                Regex(
                                                        "[^a-zA-Z\u00e1\u00e9\u00ed\u00f3\u00fa\u00fc\u00f1\u00c1\u00c9\u00cd\u00d3\u00da\u00dc\u00d1 ]"
                                                ),
                                                ""
                                        )
                        val destText = dest.toString()
                        val lastChar = if (dStart > 0) destText[dStart - 1] else ' '
                        val result = StringBuilder()
                        for (ch in cleaned) {
                                if (ch == ' ' && lastChar == ' ') continue
                                result.append(ch)
                        }
                        result.toString().take(maxLen)
                }

        fun phoneFilter(): InputFilter = InputFilter { source, _, _, _, _, _ ->
                source.replace(Regex("[^0-9]"), "").take(10)
        }

        fun lengthFilter(max: Int) = InputFilter.LengthFilter(max)
}


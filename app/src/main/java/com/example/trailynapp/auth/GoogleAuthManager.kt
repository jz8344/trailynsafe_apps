package com.example.trailynapp.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Gestor de autenticación con Google usando Credential Manager
 * Implementa el flujo moderno de Google Sign-In según las guías oficiales
 */
class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    
    companion object {
        private const val TAG = "GoogleAuthManager"
        
        // Web Client ID de Google Cloud Console
        // Credencial tipo "Web application"
        private const val WEB_CLIENT_ID = "1072914013560-vs8tip87mu7u3235mlo1dbpjghoslt6j.apps.googleusercontent.com"
    }

    /**
     * Resultado de la autenticación con Google
     */
    sealed class GoogleAuthResult {
        data class Success(
            val idToken: String,
            val displayName: String?,
            val givenName: String?,
            val familyName: String?,
            val email: String?,
            val profilePictureUri: String?
        ) : GoogleAuthResult()
        
        data class Error(val exception: Exception, val message: String) : GoogleAuthResult()
        object Cancelled : GoogleAuthResult()
    }

    /**
     * Inicia el flujo de Sign-In con Google
     * 
     * @param filterByAuthorizedAccounts Si true, solo muestra cuentas previamente autorizadas (login)
     *                                    Si false, permite seleccionar cualquier cuenta (registro)
     * @param autoSelectEnabled Habilita el inicio de sesión automático para usuarios recurrentes
     * @return GoogleAuthResult con el resultado de la autenticación
     */
    suspend fun signInWithGoogle(
        filterByAuthorizedAccounts: Boolean = true,
        autoSelectEnabled: Boolean = true
    ): GoogleAuthResult = withContext(Dispatchers.IO) {
        try {
            // Generar nonce para mejorar la seguridad
            val nonce = generateNonce()
            
            // Crear opción de Google ID
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(autoSelectEnabled)
                .setNonce(nonce)
                .build()

            // Crear solicitud de credenciales
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Obtener credencial
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Procesar respuesta
            handleSignInResult(result)

        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error obteniendo credencial: ${e.message}", e)
            GoogleAuthResult.Error(e, "Error al obtener credenciales: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.message}", e)
            GoogleAuthResult.Error(e, "Error inesperado: ${e.message}")
        }
    }

    /**
     * Muestra el botón de Sign-In con Google (flujo alternativo)
     * Este flujo es para cuando el usuario hace clic explícitamente en "Iniciar con Google"
     */
    suspend fun signInWithGoogleButton(): GoogleAuthResult = withContext(Dispatchers.IO) {
        try {
            val nonce = generateNonce()
            
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = WEB_CLIENT_ID
            )
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResult(result)

        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error en botón de Google: ${e.message}", e)
            GoogleAuthResult.Error(e, "Error al iniciar sesión con Google: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.message}", e)
            GoogleAuthResult.Error(e, "Error inesperado: ${e.message}")
        }
    }

    /**
     * Procesa el resultado de la autenticación
     */
    private fun handleSignInResult(result: GetCredentialResponse): GoogleAuthResult {
        return try {
            when (val credential = result.credential) {
                is androidx.credentials.CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        
                        Log.d(TAG, "Autenticación exitosa para: ${googleIdTokenCredential.id}")
                        
                        GoogleAuthResult.Success(
                            idToken = googleIdTokenCredential.idToken,
                            displayName = googleIdTokenCredential.displayName,
                            givenName = googleIdTokenCredential.givenName,
                            familyName = googleIdTokenCredential.familyName,
                            email = googleIdTokenCredential.id,
                            profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    } else {
                        Log.e(TAG, "Tipo de credencial no reconocido: ${credential.type}")
                        GoogleAuthResult.Error(
                            Exception("Tipo de credencial no reconocido"),
                            "Credencial no soportada"
                        )
                    }
                }
                else -> {
                    Log.e(TAG, "Tipo de credencial inesperado")
                    GoogleAuthResult.Error(
                        Exception("Credencial inesperada"),
                        "Tipo de credencial no esperado"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando credencial: ${e.message}", e)
            GoogleAuthResult.Error(e, "Error al procesar la credencial de Google")
        }
    }

    /**
     * Genera un nonce único para la solicitud
     * El nonce ayuda a prevenir ataques de repetición
     */
    private fun generateNonce(): String {
        val ranNonce = UUID.randomUUID().toString()
        val bytes = ranNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Limpia el estado de credenciales almacenadas
     * Debe llamarse cuando el usuario cierre sesión explícitamente
     */
    suspend fun clearCredentialState() {
        try {
            withContext(Dispatchers.IO) {
                credentialManager.clearCredentialState(
                    androidx.credentials.ClearCredentialStateRequest()
                )
            }
            Log.d(TAG, "Estado de credenciales limpiado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando estado de credenciales: ${e.message}", e)
        }
    }
}

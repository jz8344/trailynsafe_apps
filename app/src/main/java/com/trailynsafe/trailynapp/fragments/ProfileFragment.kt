package com.trailynsafe.app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.trailynsafe.app.R
import com.trailynsafe.app.ui.profile.ChangeEmailActivity
import com.trailynsafe.app.ui.profile.ChangePasswordActivity
import com.trailynsafe.app.ui.profile.EditProfileActivity
import com.trailynsafe.app.ui.welcome.WelcomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

class ProfileFragment : Fragment() {

    private lateinit var sessionManager: com.trailynsafe.app.utils.SessionManager
    private lateinit var themeManager: com.trailynsafe.app.utils.ThemeManager

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = com.trailynsafe.app.utils.SessionManager(requireContext())
        themeManager = com.trailynsafe.app.utils.ThemeManager(requireContext())

        loadUserData(view)
        setupButtons(view)
        setupThemeSwitch(view)
    }

    private fun loadUserData(view: View) {
        val nombre = sessionManager.getNombre()
        val apellidos = sessionManager.getApellidos()
        val correo = sessionManager.getCorreo()
        val telefono = sessionManager.getTelefono()
        val isGoogleAccount = sessionManager.isGoogleAccount()

        view.findViewById<TextView>(R.id.tvUserName).text = "$nombre $apellidos".trim()
        view.findViewById<TextView>(R.id.tvUserEmail).text = correo
        view.findViewById<TextView>(R.id.tvUserPhone)?.text =
                if (telefono.isNotEmpty()) telefono else "Sin teléfono"

        // Mostrar badge si es cuenta de Google
        val tvAccountType = view.findViewById<TextView>(R.id.tvAccountType)
        if (isGoogleAccount) {
            tvAccountType?.visibility = View.VISIBLE
            tvAccountType?.text = "🔒 Cuenta de Google"
        } else {
            tvAccountType?.visibility = View.GONE
        }
    }

    private fun setupButtons(view: View) {
        val isGoogleAccount = sessionManager.isGoogleAccount()

        // Botón editar perfil (siempre disponible)
        view.findViewById<MaterialCardView>(R.id.btnEditProfile)?.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // Card de gestión para cuentas normales
        val cardNormalAccount = view.findViewById<MaterialCardView>(R.id.cardNormalAccount)
        val btnChangeEmail = view.findViewById<MaterialCardView>(R.id.btnChangeEmail)
        val btnChangePassword = view.findViewById<MaterialCardView>(R.id.btnChangePassword)

        // Card de gestión para cuentas de Google
        val cardGoogleAccount = view.findViewById<MaterialCardView>(R.id.cardGoogleAccount)
        val btnOpenGoogleAccount = view.findViewById<MaterialButton>(R.id.btnOpenGoogleAccount)
        val btnOpenAndroidSettings = view.findViewById<MaterialButton>(R.id.btnOpenAndroidSettings)

        if (isGoogleAccount) {
            // Mostrar opciones de Google
            cardNormalAccount?.visibility = View.GONE
            cardGoogleAccount?.visibility = View.VISIBLE

            // Abrir cuenta de Google en navegador
            btnOpenGoogleAccount?.setOnClickListener {
                try {
                    val intent =
                            Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://myaccount.google.com/security")
                            )
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                                    requireContext(),
                                    "No se pudo abrir el navegador",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }

            // Abrir configuración de cuentas de Android
            btnOpenAndroidSettings?.setOnClickListener {
                try {
                    val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                                    requireContext(),
                                    "No se pudo abrir la configuración",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }
        } else {
            // Mostrar opciones normales
            cardNormalAccount?.visibility = View.VISIBLE
            cardGoogleAccount?.visibility = View.GONE

            btnChangeEmail?.setOnClickListener {
                android.util.Log.d("ProfileFragment", "Click en btnChangeEmail")
                startActivity(Intent(requireContext(), ChangeEmailActivity::class.java))
            }

            btnChangePassword?.setOnClickListener {
                android.util.Log.d("ProfileFragment", "Click en btnChangePassword")
                startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
            }
        }

        // Botón de cerrar sesión
        view.findViewById<MaterialButton>(R.id.btnLogout)?.setOnClickListener { logout() }
    }

    private fun setupThemeSwitch(view: View) {
        val switchTheme = view.findViewById<MaterialSwitch>(R.id.switchTheme)
        val tvThemeStatus = view.findViewById<TextView>(R.id.tvThemeStatus)

        val isDarkMode = themeManager.isDarkModeEnabled()
        switchTheme?.isChecked = isDarkMode
        tvThemeStatus?.text = if (isDarkMode) "Actualmente activo" else "Actualmente inactivo"

        switchTheme?.setOnCheckedChangeListener { _, isChecked ->
            themeManager.setDarkMode(isChecked)
            tvThemeStatus?.text = if (isChecked) "Actualmente activo" else "Actualmente inactivo"
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos al volver de otras activities
        view?.let { loadUserData(it) }
    }

    private fun logout() {
        sessionManager.logout()

        val intent = Intent(requireContext(), WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}


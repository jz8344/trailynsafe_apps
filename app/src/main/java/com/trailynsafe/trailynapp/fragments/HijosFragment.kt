package com.trailynsafe.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trailynsafe.app.R
import com.trailynsafe.app.api.Hijo
import com.trailynsafe.app.api.RetrofitClient
import com.trailynsafe.app.utils.SessionManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.launch

class HijosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: HijosAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hijos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        recyclerView = view.findViewById(R.id.recyclerViewHijos)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.tvEmptyView)
        fabAdd = view.findViewById(R.id.fabAddHijo)

        adapter = HijosAdapter(emptyList()) { hijo -> showHijoDetalle(hijo) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            val intent =
                    android.content.Intent(
                            requireContext(),
                            com.trailynsafe.app.ui.hijos.RegistroHijoActivity::class.java
                    )
            startActivity(intent)
        }

        loadHijos()
    }

    private fun showHijoDetalle(hijo: Hijo) {
        val dialog = HijoDetalleDialog.newInstance(hijo)
        dialog.show(childFragmentManager, "HijoDetalleDialog")
    }

    private fun loadHijos() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = "Bearer ${sessionManager.getToken()}"
                val response = RetrofitClient.apiService.getHijos(token)

                if (!isAdded) return@launch

                if (response.isSuccessful && response.body() != null) {
                    val hijos = response.body()!!
                    if (hijos.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text =
                                "No tienes hijos registrados.\nPresiona + para agregar uno."
                    } else {
                        adapter = HijosAdapter(hijos) { hijo -> showHijoDetalle(hijo) }
                        recyclerView.adapter = adapter
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(
                                        requireContext(),
                                        "Error al cargar hijos",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                }
            } finally {
                if (isAdded) progressBar.visibility = View.GONE
            }
        }
    }
}


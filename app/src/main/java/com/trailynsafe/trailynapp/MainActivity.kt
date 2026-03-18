package com.trailynsafe.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.trailynsafe.app.fragments.HijosFragment
import com.trailynsafe.app.fragments.HomeFragment
import com.trailynsafe.app.fragments.MonitorFragment
import com.trailynsafe.app.fragments.ProfileFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themeManager = com.trailynsafe.app.utils.ThemeManager(this)
        themeManager.applyTheme()

        setContentView(R.layout.activity_main)

        initViews()
        setupToolbar()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        setupBottomNavigation()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    toolbar.title = "TrailynSafe"
                    true
                }
                R.id.nav_monitor -> {
                    loadFragment(MonitorFragment())
                    toolbar.title = "Monitor"
                    true
                }
                R.id.nav_hijos -> {
                    loadFragment(HijosFragment())
                    toolbar.title = "Mis Hijos"
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    toolbar.title = "Perfil"
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }
}


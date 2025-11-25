package com.example.trailynapp.driver.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.trailynapp.driver.R
import com.example.trailynapp.driver.ui.auth.LoginActivity
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var btnSkip: TextView
    private lateinit var btnNext: MaterialButton
    private lateinit var btnGetStarted: MaterialButton
    private lateinit var indicator0: View
    private lateinit var indicator1: View
    private lateinit var indicator2: View
    private lateinit var indicator3: View
    private lateinit var onboardingItems: List<OnboardingItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        
        supportActionBar?.hide()
        
        onboardingItems = listOf(
            OnboardingItem(
                R.drawable.logodvr,
                getString(R.string.onboarding_title_0),
                getString(R.string.onboarding_desc_0)
            ),
            OnboardingItem(
                R.drawable.gps,
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1)
            ),
            OnboardingItem(
                R.drawable.bus,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2)
            ),
            OnboardingItem(
                R.drawable.notificaciones,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3)
            )
        )
        
        initViews()
        setupViewPager()
        setupListeners()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)
        btnGetStarted = findViewById(R.id.btnGetStarted)
        indicator0 = findViewById(R.id.indicator0)
        indicator1 = findViewById(R.id.indicator1)
        indicator2 = findViewById(R.id.indicator2)
        indicator3 = findViewById(R.id.indicator3)
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                
                if (position == onboardingItems.size - 1) {
                    btnNext.visibility = View.GONE
                    btnGetStarted.visibility = View.VISIBLE
                    btnSkip.visibility = View.GONE
                } else {
                    btnNext.visibility = View.VISIBLE
                    btnGetStarted.visibility = View.GONE
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupListeners() {
        btnSkip.setOnClickListener {
            navigateToLogin()
        }

        btnNext.setOnClickListener {
            val nextItem = viewPager.currentItem + 1
            if (nextItem < onboardingItems.size) {
                viewPager.currentItem = nextItem
            }
        }

        btnGetStarted.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun updateIndicators(position: Int) {
        indicator0.setBackgroundResource(
            if (position == 0) R.drawable.indicator_selected else R.drawable.indicator_unselected
        )
        indicator1.setBackgroundResource(
            if (position == 1) R.drawable.indicator_selected else R.drawable.indicator_unselected
        )
        indicator2.setBackgroundResource(
            if (position == 2) R.drawable.indicator_selected else R.drawable.indicator_unselected
        )
        indicator3.setBackgroundResource(
            if (position == 3) R.drawable.indicator_selected else R.drawable.indicator_unselected
        )
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

package com.example.trailynapp.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.trailynapp.MainActivity
import com.example.trailynapp.R
import com.example.trailynapp.ui.welcome.WelcomeActivity
import com.example.trailynapp.utils.SessionManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    private val splashTimeOut: Long = 3500 // 2.5 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Obtener las vistas
        val circle1 = findViewById<View>(R.id.circle1)
        val circle2 = findViewById<View>(R.id.circle2)
        val logoContainer = findViewById<CardView>(R.id.logoContainer)
        
        // Animaciones de los círculos orbitando alrededor del centro
        logoContainer.post {
            val centerX = logoContainer.x + logoContainer.width / 2
            val centerY = logoContainer.y + logoContainer.height / 2
            
            animateCircleOrbit(circle1, centerX, centerY, 12000, 0f)
            animateCircleOrbit(circle2, centerX, centerY, 12000, 180f)
        }

        // Animación del logo (fade in inicial)
        logoContainer.alpha = 0f
        logoContainer.scaleX = 0.3f
        logoContainer.scaleY = 0.3f
        
        val scaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.3f, 1f)
        val alpha = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f)
        
        val initialAnimatorSet = AnimatorSet()
        initialAnimatorSet.playTogether(scaleX, scaleY, alpha)
        initialAnimatorSet.duration = 800
        initialAnimatorSet.interpolator = DecelerateInterpolator()
        initialAnimatorSet.start()
        
        // Animación de latido (heartbeat)
        logoContainer.postDelayed({
            animateHeartbeat(logoContainer)
        }, 900)
        
        // Verificar sesión y navegar
        logoContainer.postDelayed({
            val sessionManager = SessionManager(this)
            val intent = if (sessionManager.isLoggedIn()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, WelcomeActivity::class.java)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, splashTimeOut)
    }
    
    private fun animateCircleOrbit(view: View, centerX: Float, centerY: Float, duration: Long, startAngle: Float) {
        val originalCenterX = view.x + view.width / 2
        val originalCenterY = view.y + view.height / 2
        
        // Calcular el radio de la órbita basado en la distancia inicial al centro y aumentarlo
        val baseRadius = Math.sqrt(
            Math.pow((originalCenterX - centerX).toDouble(), 2.0) +
            Math.pow((originalCenterY - centerY).toDouble(), 2.0)
        ).toFloat()
        
        val radius = baseRadius * 1.3f // 30% más alejados
        
        val animator = android.animation.ValueAnimator.ofFloat(startAngle, startAngle + 360f)
        animator.duration = duration
        animator.repeatCount = android.animation.ValueAnimator.INFINITE
        animator.interpolator = android.view.animation.LinearInterpolator()
        
        animator.addUpdateListener { animation ->
            val angle = Math.toRadians((animation.animatedValue as Float).toDouble())
            
            // Calcular nueva posición en la órbita circular
            val newX = centerX + (radius * Math.cos(angle)).toFloat() - view.width / 2
            val newY = centerY + (radius * Math.sin(angle)).toFloat() - view.height / 2
            
            view.x = newX
            view.y = newY
        }
        
        animator.start()
    }
    
    private fun animateHeartbeat(view: View) {
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f)
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.08f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.08f, 1f)
        
        scaleUpX.duration = 150
        scaleUpY.duration = 150
        scaleDownX.duration = 150
        scaleDownY.duration = 150
        
        val beatSet = AnimatorSet()
        beatSet.play(scaleUpX).with(scaleUpY)
        beatSet.play(scaleDownX).with(scaleDownY).after(scaleUpX)
        
        beatSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Repetir el latido después de una pausa
                view.postDelayed({ animateHeartbeat(view) }, 800)
            }
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
        
        beatSet.start()
    }
}

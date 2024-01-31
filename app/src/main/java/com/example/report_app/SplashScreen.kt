package com.example.report_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.bumptech.glide.Glide
import com.example.report_app.activities.LogInActivity
import com.example.report_app.databinding.ActivitySplashScreenBinding

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var leftAnim: Animation
    private lateinit var rightAnim: Animation
    //private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences
    private var isLoggedIn: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize SharedPreferences
        sharedPreferences = this.getSharedPreferences("UserData", Context.MODE_PRIVATE)

        // Check if the user is already logged in
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)


        Glide.with(this).load(R.drawable.loading).into(binding.imageView2)
        leftAnim = AnimationUtils.loadAnimation(this, R.anim.left_to_right)
        rightAnim = AnimationUtils.loadAnimation(this, R.anim.right_to_left)
        binding.textView1.startAnimation(leftAnim)
        binding.textView2.startAnimation(rightAnim)
        delayTimer()
    }

    private fun delayTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            goToNext()
        }, 3500)
    }

    private fun goToNext() {
        //val user = auth.currentUser
        if (isLoggedIn) {
            // User is logged in, go to MainActivity
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        } else {
            // User is not logged in, go to LogInActivity
            val loginIntent = Intent(this, LogInActivity::class.java)
            startActivity(loginIntent)
        }
        finish()
    }
}

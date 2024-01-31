package com.example.report_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.report_app.databinding.ActivityForgetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {
    private lateinit var binding : ActivityForgetPasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSubmit.setOnClickListener {
            val email = binding.email.text.toString() // replace with the user's email

            // Check if the email is not empty
            if (email.isNotEmpty()) {
                // Send password reset email
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Password reset email sent successfully
                            Toast.makeText(
                                this,
                                "Password reset email sent. Check your email.",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, LogInActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // If the task fails, display a message to the user.
                            Toast.makeText(
                                this,
                                "Failed to send password reset email.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                // Email is empty
                Toast.makeText(
                    this,
                    "Please enter your email.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.signInText.setOnClickListener {
            val intent = Intent(this, LogInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
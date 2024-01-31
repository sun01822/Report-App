package com.example.report_app.activities


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.report_app.MainActivity
import com.example.report_app.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseAuth

class LogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize
        auth = FirebaseAuth.getInstance()
        // Initialize SharedPreferences
        sharedPreferences = this.getSharedPreferences("UserData", Context.MODE_PRIVATE)


        binding.signupText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.textViewForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.buttonLogin.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonLogin.visibility = View.GONE

            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Check if the user's email is verified
                            val user = auth.currentUser
                            if (user?.isEmailVerified == true) {
                                // Email is verified, set the "loggedIn" value to true
                                // Proceed to the main activity
                                sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                binding.progressBar.visibility = View.GONE
                                binding.buttonLogin.visibility = View.VISIBLE
                                // Email is not verified, show a message and prevent login
                                Toast.makeText(
                                    this,
                                    "Please verify your email before logging in",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            binding.progressBar.visibility = View.GONE
                            binding.buttonLogin.visibility = View.VISIBLE
                            binding.email.setText("")
                            binding.password.setText("")
                            // Login failed, show an error message
                            val errorMessage = task.exception?.message
                            Toast.makeText(this, "Wrong Email or Password", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                binding.progressBar.visibility = View.GONE
                binding.buttonLogin.visibility = View.VISIBLE
                // Fields are not filled, show a message
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

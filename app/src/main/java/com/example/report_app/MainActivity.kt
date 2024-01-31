package com.example.report_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.report_app.databinding.ActivityMainBinding  // Update with your actual package name
import com.example.report_app.fragment.HomeFragment
import com.example.report_app.fragment.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the initial fragment to HomeFragment
        setFragment(HomeFragment())

        binding.bottomNavigation.setOnItemSelectedListener {
            val selectedFragment = when (it.itemId) {
                R.id.profile -> ProfileFragment()
                else -> HomeFragment()
            }
            setFragment(selectedFragment)
            true
        }

        binding.editProfile.setOnClickListener {
            setFragment(ProfileFragment())
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        loadUserData()
    }

    private fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid
        val userRef = database.reference.child("users").child(uid!!)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").value.toString()
                val email = snapshot.child("email").value.toString()
                val imageUrl = snapshot.child("imageUrl").value.toString()

                binding.name.text = name
                binding.email.text = email

                Glide.with(this@MainActivity)
                    .load(imageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.logo)
                    .into(binding.circularImageView)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}

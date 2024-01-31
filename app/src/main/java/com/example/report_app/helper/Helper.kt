package com.example.report_app.helper

import android.graphics.Bitmap
import android.view.View
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.example.report_app.R
import com.example.report_app.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Helper {
    fun compressBitmap(bitmap: Bitmap): Bitmap {
        // Calculate new dimensions based on your desired resolution
        val maxWidth = 800 // Adjust as needed
        val maxHeight = 600 // Adjust as needed

        val scale = (maxWidth.toFloat() / bitmap.width).coerceAtMost(maxHeight.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun loadUserData(uid: String, binding: ActivityMainBinding, database: FirebaseDatabase) {
        binding.progressBar.visibility = View.VISIBLE
        binding.profileLayout.visibility = View.GONE

        val userRef = database.reference.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").value.toString()
                val email = snapshot.child("email").value.toString()
                val imageUrl = snapshot.child("imageUrl").value.toString()

                binding.name.text = name
                binding.email.text = email

                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.logo)
                    .into(binding.circularImageView)

                // Hide ProgressBar and show other views when data retrieval is successful
                binding.progressBar.visibility = View.GONE
                binding.profileLayout.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
                Toast.makeText(binding.root.context, error.message, Toast.LENGTH_SHORT).show()

                // If there's an error, also hide ProgressBar and show other views
                binding.progressBar.visibility = View.GONE
                binding.profileLayout.visibility = View.VISIBLE
            }
        })
    }
}


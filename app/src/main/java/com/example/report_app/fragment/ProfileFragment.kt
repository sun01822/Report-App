package com.example.report_app.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.report_app.activities.LogInActivity
import com.example.report_app.databinding.ActivityMainBinding
import com.example.report_app.databinding.FragmentProfileBinding
import com.example.report_app.helper.Helper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var binding2: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userReference: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private var selectedImageUri: Uri? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        binding2 = ActivityMainBinding.inflate(layoutInflater)
        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userReference = database.reference.child("users").child(auth.currentUser!!.uid)
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        retrieveUserData()

        binding.update.setOnClickListener {
            val updatedName = binding.name.text.toString()
            val updatedPhone = binding.phone.text.toString()
            val updatedAddress = binding.address.text.toString()

            /* Update user data in the database */
            updateUserData(updatedName, updatedPhone, updatedAddress)

            // Check if a new image is selected
            selectedImageUri?.let { uri ->
                // Upload the image to Firebase Storage
                uploadProfileImage(uri)
            }
        }

        binding.logout.setOnClickListener {
            // Set loggedIn preference to false
            sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
            auth.signOut()
            Toast.makeText(requireContext(), "Logout successfully!!!", Toast.LENGTH_LONG).show()
            // Redirect to the login screen after logout
            val intent = Intent(requireContext(), LogInActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }


        binding.profileImage.setOnClickListener { openImagePicker() }

        return binding.root
    }

    private fun openImagePicker() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose an option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
        builder.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_CAMERA)
    }

    private fun openGallery() {
        val galleryIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_GALLERY)
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun loadProfileImage(uri: Uri?) {
        uri?.let { Glide.with(this).load(it).into(binding.profileImage) }
    }

    private fun retrieveUserData() {
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userData = snapshot.getValue(UserData::class.java)
                    userData?.let {
                        with(binding) {
                            name.setText(it.name)
                            phone.setText(it.phone)
                            address.setText(it.address)
                            loadProfileImage(Uri.parse(it.imageUrl))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileFragment", "Error retrieving user data: $error")
            }
        })
    }


    private fun updateUserData(name: String, phone: String, address: String) {
        with(userReference) {
            child("name").setValue(name)
            child("phone").setValue(phone)
            child("address").setValue(address)
        }
        showToast("Profile updated successfully")
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val originalBitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
        val compressedBitmap = Helper().compressBitmap(originalBitmap)

        // Create a byte array from the compressed bitmap
        val byteArrayOutputStream = ByteArrayOutputStream()
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        // Define the storage reference
        val imageRef = storageReference.child("profile_images/${auth.currentUser!!.uid}")

        // Upload the compressed image data to Firebase Storage
        imageRef.putBytes(data)
            .addOnSuccessListener { taskSnapshot ->
                // Get the download URL for the uploaded image
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Update the user's profile image URL in the database
                    userReference.child("imageUrl").setValue(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error uploading profile image: $e")
            }
    }


    data class UserData(val name: String = "", val email: String = "", val passport: String = "", val phone: String = "", val gender: String = "", val address: String = "", val imageUrl: String = "")

    companion object {
        private const val REQUEST_CAMERA = 101
        private const val REQUEST_GALLERY = 102
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    if (photo != null) {
                        selectedImageUri = getImageUri(photo)
                        loadProfileImage(selectedImageUri)
                    } else {
                        showToast("Error capturing image from camera")
                    }
                }
                REQUEST_GALLERY -> {
                    selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        Glide.with(this@ProfileFragment).load(selectedImageUri).into(binding.profileImage)
                    } else {
                        showToast("Error selecting image from gallery")
                    }
                }
            }
        }
    }
    private fun getImageUri(inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver,
            inImage,
            "Title",
            null
        )
        return Uri.parse(path)
    }
}
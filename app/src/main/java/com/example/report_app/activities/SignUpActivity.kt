package com.example.report_app.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.report_app.R
import com.example.report_app.databinding.ActivitySignUpBinding
import com.example.report_app.helper.Helper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var image: String
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.genderSpinner.adapter = adapter
        }

        binding.loginText.setOnClickListener {
            startActivity(Intent(this, LogInActivity::class.java))
            finish()
        }

        binding.profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.signUp.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.signUp.visibility = View.GONE

            val name = binding.name.text.toString()
            val passport = binding.passport.text.toString()
            val email = binding.email.text.toString()
            val phone = binding.phone.text.toString()
            val password = binding.password.text.toString()
            val selectedGender = binding.genderSpinner.selectedItem.toString()
            val address = binding.address.text.toString()
            if(password.isEmpty()||email.isEmpty()){
                Toast.makeText(this, "Fill Up email and password", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.signUp.visibility = View.VISIBLE
            }
            else{
                if (selectedImageUri != null) {
                    uploadImageAndCreateAccount(name, passport, email, phone, password, selectedGender, address)
                } else {
                    // If no image is selected, create an account without uploading an image
                    image = "https://firebasestorage.googleapis.com/v0/b/aegis-17642.appspot.com/o/default.jpg?alt=media&token=60e1c165-8227-4c62-bb09-b7cace0e1510"
                    createAccount(name, passport, email, phone, password, selectedGender, address)
                }
            }
        }
    }

    private fun uploadImageAndCreateAccount(
        name: String, passport: String, email: String,
        phone: String, password: String, selectedGender: String, address: String
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("profile_images/${System.currentTimeMillis()}.jpg")

        val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
        val compressedBitmap = Helper().compressBitmap(originalBitmap)

        // Create a byte array from the compressed bitmap
        val byteArrayOutputStream = ByteArrayOutputStream()
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        // Upload the compressed image data to Firebase Storage
        val uploadTask = imageRef.putBytes(data)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                image = downloadUri.toString()
                createAccount(name, passport, email, phone, password, selectedGender, address)
            } else {
                handleUploadImageError()
            }
        }
    }

    private fun createAccount(
        name: String, passport: String, email: String,
        phone: String, password: String, selectedGender: String, address: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { authTask ->
                if (authTask.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    if (uid != null) {
                        val userRef = database.reference.child("users").child(uid)
                        val userData = mapOf(
                            "name" to name,
                            "passport" to passport,
                            "email" to email,
                            "imageUrl" to image,
                            "address" to address,
                            "gender" to selectedGender,
                            "phone" to phone
                        )

                        userRef.setValue(userData)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    sendEmailVerification()
                                } else {
                                    handleSaveUserDataError()
                                }
                            }
                    }
                } else {
                    handleCreateUserError(authTask.exception?.message)
                }
            }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LogInActivity::class.java))
                finish()
            } else {
                handleSendEmailVerificationError(task.exception?.message)
            }
        }
    }

    private fun handleUploadImageError() {
        binding.progressBar.visibility = View.GONE
        binding.signUp.visibility = View.VISIBLE
        Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show()
    }

    private fun handleCreateUserError(errorMessage: String?) {
        binding.progressBar.visibility = View.GONE
        binding.signUp.visibility = View.VISIBLE
        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
    }

    private fun handleSaveUserDataError() {
        binding.progressBar.visibility = View.GONE
        binding.signUp.visibility = View.VISIBLE
        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
    }

    private fun handleSendEmailVerificationError(errorMessage: String?) {
        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option")
            .setItems(options) { _: DialogInterface?, which: Int ->
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    val photo = data?.extras?.get("data") as Bitmap
                    selectedImageUri = getImageUri(photo)
                    loadProfileImage()
                }
                REQUEST_GALLERY -> {
                    selectedImageUri = data?.data
                    loadProfileImage()
                }
            }
        }
    }

    private fun getImageUri(inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            inImage,
            "Title",
            null
        )
        return Uri.parse(path)
    }

    private fun loadProfileImage() {
        Glide.with(this@SignUpActivity).load(selectedImageUri).into(binding.profileImage)
    }

    companion object {
        private const val REQUEST_CAMERA = 101
        private const val REQUEST_GALLERY = 102
    }
}

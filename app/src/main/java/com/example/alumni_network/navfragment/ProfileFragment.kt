package com.example.alumni_network.navfragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.alumni_network.R
import com.example.alumni_network.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File


class ProfileFragment : Fragment() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.mobileditbutton.setOnClickListener {
            binding.mobileLayout.boxStrokeWidth = 2
            binding.mobileEdit.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                requestFocus()
            }
        }
        binding.jobeditbutton.setOnClickListener {
            binding.jobLayout.boxStrokeWidth = 2
            binding.jobEdit.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                requestFocus()
            }
        }
        binding.companyeditbutton.setOnClickListener {
            binding.companyLayout.boxStrokeWidth = 2
            binding.companyEdit.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                requestFocus()
            }
        }
        binding.cityeditButton.setOnClickListener {
            binding.CityLayout.boxStrokeWidth = 2
            binding.cityEdit.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                requestFocus()
            }
        }
        binding.industryeditbutton.setOnClickListener {
            binding.industryLayout.boxStrokeWidth = 2
            binding.industryEdit.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                requestFocus()
            }
        }
        binding.gendereditButton.setOnClickListener {
            binding.genderLayout.boxStrokeWidth = 2
            binding.genderEdit.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                requestFocus()
            }
        }
        binding.bioeditbutton.setOnClickListener {
            binding.bioLayout.boxStrokeWidth = 2
            binding.bioEdit.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                requestFocus()
            }
        }
        profileGetter()
        binding.saveChanges.setOnClickListener {
            profileUpdate()
            disableAllFields()

        }
        profileGetter()
        binding.editprofileimage.setOnClickListener {
            openGallery()
        }
        binding.uploadButtonforimage.setOnClickListener {
            if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri!!)
            } else {
                Toast.makeText(requireContext(), "Please select an image first!", Toast.LENGTH_SHORT).show()
            }

        }



    }
    private fun uploadImageToCloudinary(imageUri: Uri) {
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }
        val filePath = getFilePathFromUri(requireContext(), imageUri)
        MediaManager.get().upload(filePath)
            .option("folder", "profile_im")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {

                    Toast.makeText(requireContext(), "start", Toast.LENGTH_SHORT).show()
                }
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    Toast.makeText(requireContext(), "progress", Toast.LENGTH_SHORT).show()
                }
                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"].toString()
                    val uid = mAuth.currentUser?.uid.toString()
                    val userDocumentRef = firestore.collection("users").document(uid)
                    userDocumentRef.update("imageUrl", imageUrl)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Image URL added successfully! $imageUrl", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to add image URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("CloudinaryUpload", "Upload error: ${error?.description}")
                    Toast.makeText(requireContext(), "Upload failed: ${error?.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val photoUri = data?.data
            selectedImageUri = data?.data
            binding.profileimage.setImageURI(selectedImageUri)
        }
    }

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val fileName = if (columnIndex != -1) cursor.getString(columnIndex) else "temp_file"
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } else null
    }





    private fun disableAllFields() {
        binding.mobileLayout.isEnabled = false
        binding.jobLayout.isEnabled = false
        binding.companyLayout.isEnabled = false
        binding.CityLayout.isEnabled = false
        binding.industryLayout.isEnabled = false
        binding.genderLayout.isEnabled = false
        binding.bioLayout.isEnabled = false

        binding.mobileEdit.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

        }
        binding.jobEdit.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

        }
        binding.companyEdit.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

        }
        binding.cityEdit.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

        }
        binding.industryEdit.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

        }
        binding.genderEdit.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

        }
        binding.bioEdit.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

        }
    }


    private fun profileUpdate() {
        val uid = mAuth.currentUser?.uid.toString()
        val userDocumentRef = firestore.collection("users").document(uid)
        val updatedValues = hashMapOf<String, Any>(
            "phoneNo" to (binding.mobileEdit.text.toString().takeIf { it.isNotEmpty() } ?: ""),
            "jobTitle" to (binding.jobEdit.text.toString().takeIf { it.isNotEmpty() } ?: ""),
            "companyName" to (binding.companyEdit.text.toString().takeIf { it.isNotEmpty() } ?: ""),
            "city" to (binding.cityEdit.text.toString().takeIf { it.isNotEmpty() } ?: ""),
            "industry" to (binding.industryEdit.text.toString().takeIf { it.isNotEmpty() } ?: ""),
            "gender" to (binding.genderEdit.text.toString().takeIf { it.isNotEmpty() } ?: ""),
            "bio" to (binding.bioEdit.text.toString().takeIf { it.isNotEmpty() } ?: "")
        )
        userDocumentRef.update(updatedValues)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Failed to update profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun profileGetter() {
        val uid = mAuth.currentUser?.uid
        val email = mAuth.currentUser?.email.toString()
        binding.userEmail.setText(email)
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val firstName = documentSnapshot.getString("firstName")?.toUpperCase()
                        val lastName = documentSnapshot.getString("lastName")?.toUpperCase()
                        binding.userName.setText("$firstName $lastName")
                        binding.mobileEdit.setText(
                            documentSnapshot.getString("phoneNo")?.toUpperCase()
                        )
                        binding.jobEdit.setText(
                            documentSnapshot.getString("jobTitle")?.toUpperCase()
                        )
                        binding.companyEdit.setText(
                            documentSnapshot.getString("companyName")?.toUpperCase()
                        )
                        binding.cityEdit.setText(documentSnapshot.getString("city")?.toUpperCase())
                        binding.industryEdit.setText(
                            documentSnapshot.getString("industry")?.toUpperCase()
                        )
                        binding.genderEdit.setText(
                            documentSnapshot.getString("gender")?.toUpperCase()
                        )
                        binding.bioEdit.setText(documentSnapshot.getString("bio"))
                        val profilePhotoUrl = documentSnapshot.getString("imageUrl")
                        Glide.with(this)
                            .load(profilePhotoUrl)
                            .placeholder(R.drawable.profiledummy)
                            .error(R.drawable.profiledummy)
                            .into(binding.profileimage)




                    } else {
                        Toast.makeText(context, "Error:close and open", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

}

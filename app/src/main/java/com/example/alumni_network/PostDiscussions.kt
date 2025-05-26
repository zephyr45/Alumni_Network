package com.example.alumni_network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class PostDiscussions : Fragment() {
    private lateinit var networkId:String
    private lateinit var mAuth:FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedImageUri: Uri? = null
    private lateinit var messageForDiscussion:TextView
    private lateinit var descriptionForDiscussion:EditText
    private lateinit var addedMedia:ImageView
    private var selectedCategory: TextView? = null
    private var selectedCategoryText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore=FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_discussions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        networkId = arguments?.getString("networkId").toString()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Post Discussions"
        descriptionForDiscussion = view.findViewById(R.id.DescritionforDiscussion)
        val postDiscussion = view.findViewById<Button>(R.id.ButtonForDiscussion)
        messageForDiscussion = view.findViewById(R.id.messageforDiscussion)
        val addmediaForDiscssuion=view.findViewById<TextView>(R.id.addmediafordiscussion)
        addedMedia=view.findViewById(R.id.addedmediafordiscussion)
        val categoryForDiscussion=view.findViewById<TextView>(R.id.categoryfordiscussion)
        val categoryLayout1=view.findViewById<LinearLayout>(R.id.categoryLayout1)
        val categoryLayout2=view.findViewById<LinearLayout>(R.id.categorylayout2)
        val allCategory=view.findViewById<TextView>(R.id.AllCategories)
        val workExperience=view.findViewById<TextView>(R.id.WorkExperience)
        val knowledgeBase=view.findViewById<TextView>(R.id.knowledgebase)
        val inNews=view.findViewById<TextView>(R.id.inNews)
        val studyExperience=view.findViewById<TextView>(R.id.studyexperience)
        val interviewExperience=view.findViewById<TextView>(R.id.interviewExperience)
        var flag=false


        setSelectedCategory(allCategory)

        // Set click listeners for all categories
        allCategory.setOnClickListener { setSelectedCategory(allCategory)
        }
        workExperience.setOnClickListener {
            setSelectedCategory(workExperience) }
        knowledgeBase.setOnClickListener { setSelectedCategory(knowledgeBase) }
        inNews.setOnClickListener { setSelectedCategory(inNews) }
        studyExperience.setOnClickListener { setSelectedCategory(studyExperience) }
        interviewExperience.setOnClickListener { setSelectedCategory(interviewExperience) }

        categoryForDiscussion.setOnClickListener {
            if (flag==false){
                categoryLayout1.visibility=View.VISIBLE
                categoryLayout2.visibility=View.VISIBLE
                flag=true
            }else{
                categoryLayout1.visibility=View.GONE
                categoryLayout2.visibility=View.GONE
                flag=false
            }
        }
        addmediaForDiscssuion.setOnClickListener {
            openGallery()
        }


        // Initially hide the message
        messageForDiscussion.visibility = View.GONE

        // Listen for text changes in the description field
        descriptionForDiscussion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Hide the message if the user starts typing
                if (!s.isNullOrEmpty()) {
                    messageForDiscussion.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        postDiscussion.setOnClickListener {
            val discussionText = descriptionForDiscussion.text.toString().trim()

            if (discussionText.isEmpty()) {
                messageForDiscussion.visibility = View.VISIBLE
                messageForDiscussion.text = "Description cannot be empty"
            } else {
                if (selectedImageUri != null) {
                    uploadImageToCloudinary(selectedImageUri!!) { uploadedImageUrl ->
                        saveDiscussionToFirestore(discussionText, uploadedImageUrl)
                    }
                } else {
                    saveDiscussionToFirestore(discussionText, null)
                }
            }
        }

        // Function to handle the Cloudinary image upload and pass the URL back


        // Function to save discussion data to Firestore




    }
    private fun setSelectedCategory(newSelected: TextView) {
        // Reset previously selected state
        selectedCategory?.isSelected = false

        // Mark the newly selected category
        newSelected.isSelected = true

        // Update reference
        selectedCategory = newSelected
        selectedCategoryText = newSelected.text.toString()
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
            addedMedia.setImageURI(selectedImageUri)
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
    private fun uploadImageToCloudinary(imageUri: Uri, callback: (String?) -> Unit) {
        val filePath = getFilePathFromUri(requireContext(), imageUri)
        MediaManager.get().upload(filePath)
            .option("folder", "discussions")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    messageForDiscussion.visibility = View.VISIBLE
                    messageForDiscussion.text = "Uploading Discussion"
                }

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    callback(resultData["secure_url"]?.toString())
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("CloudinaryUpload", "Upload error: ${error?.description}")
                    callback(null)
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    private fun saveDiscussionToFirestore(description: String, imageUrl: String?) {
        mAuth = FirebaseAuth.getInstance()
        val userId = mAuth.currentUser?.uid.toString()
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName").orEmpty()
                val lastName = document.getString("lastName").orEmpty()

                val discussionData = hashMapOf<String, Any>(
                    "description" to description,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "name" to "$firstName $lastName",
                    "senderId" to userId,
                    "likes" to emptyList<String>(),
                    "comments" to emptyList<Map<String, String>>(),
                    "noOfLikes" to 0,
                    "imageUrl" to (imageUrl ?: ""),
                    "category" to selectedCategoryText.toString()
                )

                firestore.collection("networks").document(networkId)
                    .collection("discussions")
                    .add(discussionData)
                    .addOnSuccessListener {
                        messageForDiscussion.visibility = View.VISIBLE
                        messageForDiscussion.text = "Discussion Uploaded Successfully"
                        descriptionForDiscussion.text.clear()
                        addedMedia.setImageDrawable(null)
                        selectedImageUri=null
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to post discussion: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}




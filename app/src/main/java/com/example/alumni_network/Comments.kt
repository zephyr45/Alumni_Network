package com.example.alumni_network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alumni_network.databinding.ActivityCommentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit

class Comments : AppCompatActivity() {
    private lateinit var binding: ActivityCommentsBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var nameofCommenter: String
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = ArrayList<CommentDataClass>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.toolbarComment)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        val networkId = intent.getStringExtra("networkId") ?: ""
        val discussionId = intent.getStringExtra("discussionId") ?: ""
        val typeOfPost=intent.getStringExtra("typeOfPost")?: ""
        Toast.makeText(this,typeOfPost,Toast.LENGTH_SHORT).show()
        val userId = mAuth.currentUser?.uid ?: ""

        // Fetch user's name for the comment
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName").orEmpty()
                val lastName = document.getString("lastName").orEmpty()
                nameofCommenter = "$firstName $lastName"
            }

        // Setup RecyclerView
        commentAdapter = CommentAdapter(commentList)
        binding.recyclerForComment.apply {
            layoutManager = LinearLayoutManager(this@Comments)
            adapter = commentAdapter
        }

        // Load existing comments
        loadComments(networkId, discussionId,typeOfPost)

        // Handle send button click
        binding.commentsendbutton.setOnClickListener {
            val commentText = binding.commentmessage.text.toString()
            if (commentText.isEmpty()) {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                postComment(networkId, discussionId, commentText,typeOfPost)
            }
        }
    }

    private fun loadComments(networkId: String, discussionId: String,typeOfPost:String) {
        firestore.collection("networks")
            .document(networkId)
            .collection(typeOfPost)
            .document(discussionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val comments = document.get("comments") as? List<Map<String, Any>> ?: emptyList()
                    commentList.clear()
                    for (comment in comments) {
                        val name = comment["name"] as? String ?: "Unknown"
                        val timestamp = comment["timestamp"] as? Long ?: System.currentTimeMillis()
                        val description = comment["description"] as? String ?: ""
                        commentList.add(
                            CommentDataClass(
                                profilimage = R.drawable.profiledummy,
                                name = name,
                                timestamp = formatTimeAgo(timestamp),
                                commentDescription = description
                            )
                        )
                    }
                    commentAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load comments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun postComment(networkId: String, discussionId: String, commentText: String,typeOfPost:String) {
        val commentData = mapOf(
            "name" to nameofCommenter,
            "description" to commentText,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("networks")
            .document(networkId)
            .collection(typeOfPost
            )
            .document(discussionId)
            .update("comments", FieldValue.arrayUnion(commentData))
            .addOnSuccessListener {
                binding.commentmessage.text.clear()
                Toast.makeText(this, "Comment posted successfully", Toast.LENGTH_SHORT).show()
                loadComments(networkId, discussionId, typeOfPost) // Refresh comments
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val currentTimeMillis = System.currentTimeMillis()
        val diffMillis = currentTimeMillis - timestamp
        return when {
            TimeUnit.MILLISECONDS.toMinutes(diffMillis) < 60 -> {
                "-${TimeUnit.MILLISECONDS.toMinutes(diffMillis)} minutes ago"
            }
            TimeUnit.MILLISECONDS.toHours(diffMillis) < 24 -> {
                "-${TimeUnit.MILLISECONDS.toHours(diffMillis)} hours ago"
            }
            else -> {
                "-${TimeUnit.MILLISECONDS.toDays(diffMillis)} days ago"
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

data class CommentDataClass(
    val profilimage: Int,
    val name: String,
    val timestamp: String,
    val commentDescription: String
)

class CommentAdapter(private val dataList: ArrayList<CommentDataClass>) :
    RecyclerView.Adapter<CommentAdapter.ViewHolderClass>() {

    class ViewHolderClass(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileimageforcomment)
        val commenterName: TextView = itemView.findViewById(R.id.nameforcomment)
        val commentTimestamp: TextView = itemView.findViewById(R.id.timeforcomment)
        val commentDescription: TextView = itemView.findViewById(R.id.commentdescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item_layout, parent, false)
        return ViewHolderClass(view)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentComment = dataList[position]
        holder.profileImage.setImageResource(currentComment.profilimage)
        holder.commenterName.text = currentComment.name
        holder.commentDescription.text = currentComment.commentDescription
        holder.commentTimestamp.text = currentComment.timestamp
    }
}

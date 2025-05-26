package com.example.alumni_network

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MemberProfile : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_member_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarMemberProfile)
        val toolbarTitle = findViewById<TextView>(R.id.toolbar_title)
        val memberProfile = findViewById<ImageView>(R.id.memberProfileImage)
        val memberName = findViewById<TextView>(R.id.MemberName)
        val memberWork = findViewById<TextView>(R.id.memberWork)
        val memberCity = findViewById<TextView>(R.id.MemberCity)
        val memberMobile = findViewById<TextView>(R.id.MemberMobile)
        val memberJobTitle = findViewById<TextView>(R.id.MemberJobTitle)
        val memberCompanyName = findViewById<TextView>(R.id.MemberCompanyName)
        val memberCourse = findViewById<TextView>(R.id.MemberCourse)
        val memberCollege = findViewById<TextView>(R.id.MemberCollege)
        val chatWithMember = findViewById<Button>(R.id.chatwithmemberbutton)
        val connectWithMember = findViewById<TextView>(R.id.memberconnect)
        val memberData = intent.getStringExtra("member_data")
        val gson = Gson()
        val member = gson.fromJson(memberData, ForMember::class.java)

        Glide.with(this)
            .load(member.profileImage)
            .placeholder(R.drawable.profiledummy)
            .error(R.drawable.profiledummy)
            .into(memberProfile)

        memberName.text = member.name
        memberWork.text = "${member.jobTitle} At ${member.companyName}"
        memberCity.text = member.city
        memberMobile.text = member.phoneNo
        memberJobTitle.text = member.jobTitle
        memberCompanyName.text = member.companyName
        memberCourse.text = member.course
        firestore = FirebaseFirestore.getInstance()

        lifecycleScope.launch {
            val collegeName = firestore.collection("networks")
                .document(member.networkId)
                .get()
                .await()
                .getString("title") ?: "Unknown College"
            memberCollege.text = collegeName
        }

        toolbarTitle.text = "${member.name.split(" ")[0]}'s Profile"
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        mAuth = FirebaseAuth.getInstance()
        val currentUserId = mAuth.currentUser?.uid.toString()
        val recipientUserId = member.id

        checkRelationshipStatus(currentUserId, recipientUserId, connectWithMember, chatWithMember)

        connectWithMember.setOnClickListener {
            when (connectWithMember.text) {
                "Connect" -> sendConnectionRequest(currentUserId, recipientUserId,member.networkId) {
                    connectWithMember.text = "Requested"
                }
                "Confirm" -> confirmConnection(currentUserId, recipientUserId) {
                    connectWithMember.text = "Connected"
                    chatWithMember.visibility = View.VISIBLE
                }
                else -> {
                    Toast.makeText(this, "You are already requested or connected.", Toast.LENGTH_SHORT).show()
                }
            }
        }



        chatWithMember.setOnClickListener {
            val relationId = generateRelationId(currentUserId, recipientUserId)
            val intent = Intent(this, UserChats::class.java).apply {
                putExtra("relationId", relationId)
                putExtra("recipientName", member.name)
                putExtra("recipientProfile",member.profileImage)
                intent.putExtra("recipientId",recipientUserId)

            }
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }



    private fun sendConnectionRequest(currentUserId: String, recipientUserId: String,networkId:String, onSuccess: () -> Unit) {
        val relationId = generateRelationId(currentUserId, recipientUserId)
        val relationRef = firestore.collection("userRelation").document(relationId)

        relationRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val data = hashMapOf(
                    "users" to listOf(currentUserId),
                    "status" to "requested",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "requestedTo" to recipientUserId,
                    "networkId" to networkId,
                )
                relationRef.set(data).addOnSuccessListener {
                    Log.d("Relationship", "Connection request sent.")
                    onSuccess()
                }.addOnFailureListener { e ->
                    Log.e("Relationship", "Failed to send request: ${e.message}")
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Relationship", "Error checking request: ${e.message}")
        }
    }

    private fun confirmConnection(currentUserId: String, recipientUserId: String, onSuccess: () -> Unit) {
        val relationId = generateRelationId(currentUserId, recipientUserId)
        val relationRef = firestore.collection("userRelation").document(relationId)

        relationRef.update(
            "users", FieldValue.arrayUnion(currentUserId),
            "status", "connected",
            "timestamp", FieldValue.serverTimestamp()
        ).addOnSuccessListener {
            Log.d("Relationship", "Connection confirmed.")
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e("Relationship", "Failed to confirm connection: ${e.message}")
        }
    }

    private fun checkRelationshipStatus(
        currentUserId: String,
        recipientUserId: String,
        connectButton: TextView,
        chatButton: Button
    ) {
        val relationId = generateRelationId(currentUserId, recipientUserId)
        val relationRef = firestore.collection("userRelation").document(relationId)

        relationRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val users = document.get("users") as? List<String> ?: emptyList()


                when {
                    users.contains(currentUserId) && users.contains(recipientUserId) -> {
                        connectButton.text = "Connected"
                        chatButton.visibility = View.VISIBLE
                    }
                    users.contains(recipientUserId) -> {
                        connectButton.text = "Confirm"
                    }
                    users.contains(currentUserId) -> {
                        connectButton.text = "Requested"
                    }
                    else -> {
                        connectButton.text = "Connect"
                    }
                }
            } else {
                connectButton.text = "Connect"
            }
        }.addOnFailureListener { e ->
            Log.e("Relationship", "Error checking relationship status: ${e.message}")
        }
    }

    private fun generateRelationId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_$user2" else "${user2}_$user1"
    }
}


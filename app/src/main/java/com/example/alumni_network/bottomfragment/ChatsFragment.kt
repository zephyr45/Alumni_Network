package com.example.alumni_network.bottomfragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alumni_network.R
import com.example.alumni_network.UserChats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatsAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private val relationshipList = mutableListOf<Relationship>()
    private val filteredList = mutableListOf<Relationship>()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase references
        firestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerviewforchatuser)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ChatsAdapter(filteredList) { relationship ->
            val intent = Intent(requireContext(), UserChats::class.java).apply {
                putExtra("relationId", relationship.chatId)
                putExtra("recipientName", relationship.recipientName)
                putExtra("recipientProfile", relationship.recipientprofile)
                putExtra("recipientId", relationship.recipientId)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Initialize SearchView
        val searchView = view.findViewById<SearchView>(R.id.searchViewForUserChats)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterList(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })

        return view
    }

    private fun fetchRelationships() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        listenerRegistration?.remove() // Remove any previous listener
        listenerRegistration = firestore.collection("userRelation")
            .whereArrayContains("users", currentUserId)
            .whereEqualTo("status", "connected")
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    Log.e("ChatsFragment", "Listen failed.", e)
                    Toast.makeText(requireContext(), "Error fetching chats", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documents != null) {
                    val newRelationshipList = mutableListOf<Relationship>()
                    for (document in documents) {
                        val chatId = document.id
                        val users = document.get("users") as? List<String> ?: continue
                        val lastMessage = document.getString("lastMessage").orEmpty()
                        val recipientId = users.find { it != currentUserId } ?: continue
                        val lastMessageTime = (document.get("lastUpdated") as? com.google.firebase.Timestamp)?.toDate()
                            ?: (document.get("lastUpdated") as? Long)?.let { Date(it) }

                        val lastMessageTimeMillis = lastMessageTime?.time ?: 0L
                        val currentTime = System.currentTimeMillis()

                        val displayString = if (lastMessageTimeMillis == 0L) {
                            "" // Show empty string if lastMessageTimeMillis is 0
                        } else {
                            val timeDiff = currentTime - lastMessageTimeMillis
                            if (timeDiff < 24 * 60 * 60 * 1000) {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMessageTimeMillis))
                            } else {
                                val days = timeDiff / (1000 * 60 * 60 * 24)
                                "$days days ago"
                            }
                        }


                        firestore.collection("users").document(recipientId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val recipientName = "${userDoc.getString("firstName")} ${userDoc.getString("lastName")}"
                                val imageUrl = userDoc.getString("imageUrl").orEmpty()

                                val relationship = Relationship(
                                    chatId,
                                    recipientId,
                                    recipientName,
                                    imageUrl,
                                    lastMessage,
                                    displayString,
                                    lastMessageTimeMillis
                                )
                                newRelationshipList.add(relationship)

                                // Update UI only after all user details are fetched
                                if (newRelationshipList.size == documents.size()) {
                                    updateRelationshipList(newRelationshipList)
                                }
                            }
                            .addOnFailureListener {
                                Log.e("ChatsFragment", "Failed to fetch user details: ${it.message}")
                            }
                    }
                }
            }
    }

    private fun updateRelationshipList(newList: List<Relationship>) {
        relationshipList.clear()
        relationshipList.addAll(newList.distinctBy { it.chatId }) // Remove duplicates
        relationshipList.sortByDescending { it.lastMessageTimeMillis }

        filteredList.clear()
        filteredList.addAll(relationshipList)
        adapter.notifyDataSetChanged()
    }

    private fun filterList(query: String?) {
        filteredList.clear()
        if (!query.isNullOrEmpty()) {
            val lowerCaseQuery = query.lowercase()
            val results = relationshipList.filter { it.recipientName.lowercase().contains(lowerCaseQuery) }
            filteredList.addAll(results)
        } else {
            filteredList.addAll(relationshipList)
        }
        adapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        fetchRelationships() // Fetch relationships when the fragment becomes visible
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove() // Remove listener when fragment stops
        listenerRegistration = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove() // Cleanup listener
        listenerRegistration = null
    }
}

data class Relationship(
    val chatId: String,
    val recipientId: String,
    val recipientName: String,
    val recipientprofile: String,
    val lastmessage: String,
    val lastUpdatedTime: String,
    val lastMessageTimeMillis: Long
)

class ChatsAdapter(
    private val relationships: List<Relationship>,
    private val onItemClick: (Relationship) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImageforchats)
        private val nameTextView: TextView = itemView.findViewById(R.id.userNameforchats)
        private val lastmessage: TextView = itemView.findViewById(R.id.userSubText)
        private val lastUpdated: TextView = itemView.findViewById(R.id.lastUpdated)

        fun bind(relationship: Relationship) {
            nameTextView.text = relationship.recipientName
            lastmessage.text = if (relationship.lastmessage.isEmpty()) "Tap to Chat" else relationship.lastmessage
            if(!relationship.lastUpdatedTime.isEmpty()){
                lastUpdated.text = relationship.lastUpdatedTime
            }


            Glide.with(itemView.context)
                .load(relationship.recipientprofile)
                .placeholder(R.drawable.profiledummy)
                .error(R.drawable.profiledummy)
                .into(profileImage)

            itemView.setOnClickListener {
                onItemClick(relationship)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(relationships[position])
    }

    override fun getItemCount(): Int = relationships.size
}

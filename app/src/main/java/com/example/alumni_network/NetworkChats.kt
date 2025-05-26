package com.example.alumni_network

import ChatItem
import ChatMessage
import CommunityChatsAdapter
import android.os.Bundle
import android.text.format.DateUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NetworkChats : Fragment() {

    private var networkId: String? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: CommunityChatsAdapter
    private val messages = mutableListOf<ChatItem>()
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_network_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Network Chats"

        networkId = arguments?.getString("networkId")
        if (networkId == null) {
            Toast.makeText(requireContext(), "Invalid network ID", Toast.LENGTH_SHORT).show()
            return
        }

        recyclerView= view.findViewById(R.id.recyclerForNetworkChats)
        val editTextMessage: EditText = view.findViewById(R.id.chatmessage)
        val buttonSend: ImageView = view.findViewById(R.id.chatsendbutton)

        val currentUserId = mAuth.currentUser?.uid.toString()
        var currentUserName:String="Anonymous"
        val userRef=firestore.collection("users").document(currentUserId).get().addOnSuccessListener { document->
            val firstName=document.getString("firstName").toString()
            val lastName=document.getString("lastName").toString()
            currentUserName="$firstName $lastName"

        }



        adapter = CommunityChatsAdapter(messages, currentUserId) { messageId, senderId ->
            deleteMessage(messageId, senderId)  // Pass both messageId and senderId to deleteMessage
        }


        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.scrollToPosition(adapter.itemCount - 1)
        fetchMessages()

        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText, currentUserId, currentUserName)
                editTextMessage.text.clear()
            } else {
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMessages() {
        if (networkId == null) return

        firestore.collection("networks")
            .document(networkId!!)
            .collection("chats")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Failed to fetch messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val items = mutableListOf<ChatItem>()
                    var lastDateLabel: String? = null

                    for (doc in snapshot.documents) {
                        val message = doc.toObject(ChatMessage::class.java)
                        if (message != null) {
                            message.messageId = doc.id
                            val dateLabel = formatDate(message.timestamp)

                            if (dateLabel != lastDateLabel) {
                                items.add(ChatItem.DateLabel(dateLabel))
                                lastDateLabel = dateLabel
                            }

                            items.add(ChatItem.Message(message))
                        }
                    }
                    messages.clear()
                    messages.addAll(items)
                    adapter.notifyDataSetChanged()
                    scrollToBottom()
                }
            }
    }
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(date, today.time) -> "Today"
            isSameDay(date, yesterday.time) -> "Yesterday"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        }
    }


    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }


    private fun sendMessage(messageText: String, senderId: String, senderName: String) {
        if (networkId == null) return

        val message = ChatMessage(
            senderId = senderId,
            senderName = senderName,
            messageText = messageText,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("networks")
            .document(networkId!!)
            .collection("chats")
            .add(message)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Message sent", Toast.LENGTH_SHORT).show()
                scrollToBottom() // Ensure scrolling to the latest message
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scrollToBottom() {
        recyclerView.post {
            recyclerView.smoothScrollToPosition(messages.size - 1)
        }
    }


    // Handle message deletion from Firestore
    private fun deleteMessage(messageId: String, senderId: String) {
        val currentUserId = mAuth.currentUser?.uid ?: ""

        // Check if the current user is the sender of the message
        if (senderId == currentUserId) {
            if (networkId == null) return

            firestore.collection("networks")
                .document(networkId!!)
                .collection("chats")
                .document(messageId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Message deleted", Toast.LENGTH_SHORT).show()

                    // Remove the message from the UI after deletion
                    adapter.deleteMessage(messageId)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete message", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "You can only delete your own messages", Toast.LENGTH_SHORT).show()
        }
        scrollToBottom()
    }




}


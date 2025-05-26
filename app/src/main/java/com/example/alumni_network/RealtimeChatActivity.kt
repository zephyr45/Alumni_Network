package com.example.alumni_network

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alumni_network.adapters.RealtimeChatAdapter
import com.example.alumni_network.models.ChatMessage
import com.example.alumni_network.repository.ChatRepository
import com.example.alumni_network.utils.FirebaseConfig

class RealtimeChatActivity : AppCompatActivity() {
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatRepository: ChatRepository
    private lateinit var chatAdapter: RealtimeChatAdapter
    private val chatRoomId = "global_chat" // For demonstration, using a single global chat room

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime_chat)

        // Initialize views
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.messagesRecyclerView)

        // Initialize repository and adapter
        chatRepository = ChatRepository()
        chatAdapter = RealtimeChatAdapter(FirebaseConfig.auth.currentUser?.uid ?: "")

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        // Setup send button
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageInput.text.clear()
            }
        }

        // Start listening to messages
        listenToMessages()
    }

    private fun sendMessage(messageText: String) {
        val currentUser = FirebaseConfig.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to send messages", Toast.LENGTH_SHORT).show()
            return
        }

        val message = ChatMessage(
            senderId = currentUser.uid,
            senderName = currentUser.displayName ?: "Anonymous",
            message = messageText
        )

        chatRepository.sendMessage(
            chatRoomId = chatRoomId,
            message = message,
            onSuccess = {
                // Message sent successfully
            },
            onError = { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun listenToMessages() {
        chatRepository.listenToMessages(
            chatRoomId = chatRoomId,
            onMessagesUpdated = { messages ->
                // Update the RecyclerView with new messages
                chatAdapter.setMessages(messages)
                recyclerView.scrollToPosition(messages.size - 1)
            },
            onError = { e ->
                Toast.makeText(this, "Error loading messages: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        chatRepository.stopListening()
    }
} 
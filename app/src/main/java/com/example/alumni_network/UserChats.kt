package com.example.alumni_network

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserChats : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var chatAdapter: UserChatAdapter
    private lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_chats)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        mAuth=FirebaseAuth.getInstance()
        firestore=FirebaseFirestore.getInstance()
        val chatId = intent.getStringExtra("relationId") ?: return
        val recipientName = intent.getStringExtra("recipientName")
        val recipientProfile = intent.getStringExtra("recipientProfile")
        val recipientId=intent.getStringExtra("recipientId").toString()
        val recipientprofileimage:ImageView=findViewById(R.id.recipientprofileimage)
        val recipientNametxt:TextView=findViewById(R.id.recipientName)
        val backButton:ImageView=findViewById(R.id.backbutton)
        val toolbar = findViewById<Toolbar>(R.id.toolbarUserChats)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
        }



        backButton.setOnClickListener {
            onBackPressed()
        }
        Glide.with(this)
            .load(recipientProfile)
            .placeholder(R.drawable.profiledummy)
            .into(recipientprofileimage)

        recipientNametxt.text=recipientName

        recyclerView = findViewById(R.id.recyclerviewforchatting)
        chatAdapter = UserChatAdapter(mAuth.currentUser?.uid ?: "")



        recyclerView.adapter = chatAdapter

        // Observe messages
        observeMessages(chatId) { messages ->
            chatAdapter.setMessages(messages)
            recyclerView.scrollToPosition(messages.size - 1) // Scroll to latest message
        }

        // Handle send button click
        val sendButton:ImageView= findViewById(R.id.chatsendbuttonForUserChats)
        val messageInput: TextView = findViewById(R.id.chatmessageForUserChats)

        sendButton.setOnClickListener {
            val messageText = messageInput.text.trim().toString()
            if (messageText.isNotBlank()) {
                sendMessage(chatId, mAuth.currentUser?.uid ?: "", recipientId, messageText)
                messageInput.text = ""
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd=false
        }


    }
    private fun sendMessage(chatId: String, senderId: String, recipientId: String, message: String) {
        val chatRef = firestore.collection("userRelation").document(chatId)
        val messageData = hashMapOf(
            "senderId" to senderId,
            "recipientId" to recipientId,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )

        chatRef.collection("messages").add(messageData).addOnSuccessListener {
            Log.d("Chat", "Message sent successfully.")
            chatRef.update(
                "lastMessage", message,
                "lastUpdated", System.currentTimeMillis()
            )
        }.addOnFailureListener {
            Log.e("Chat", "Failed to send message: ${it.message}")
        }
    }

    private fun observeMessages(chatId: String, onMessagesFetched: (List<Message>) -> Unit) {
        val chatRef = firestore.collection("userRelation").document(chatId).collection("messages")

        chatRef.orderBy("timestamp").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Chat", "Failed to fetch messages: ${e.message}")
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Message::class.java)
            } ?: emptyList()

            onMessagesFetched(messages)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}
data class Message(
    val senderId: String = "",
    val recipientId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false
)
class UserChatAdapter(private val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<Message>()

    // Define constants for view types
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT // Sent message
        } else {
            VIEW_TYPE_RECEIVED // Received message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.send_message, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recieve_message_from_user, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    // ViewHolder for sent messages
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.sendmessagetext) // From send_message layout
        private val timestampText: TextView = itemView.findViewById(R.id.sentTime) // From send_message layout

        fun bind(message: Message) {
            messageText.text = message.message
            timestampText.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
        }
    }

    // ViewHolder for received messages
    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.receivemessagetextFromuser) // From receive_message layout
        private val timestampText: TextView = itemView.findViewById(R.id.receiveTimeFromuser) // From receive_message layout

        fun bind(message: Message) {
            messageText.text = message.message
            timestampText.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
        }
    }
}

package com.example.alumni_network.repository

import com.example.alumni_network.models.ChatMessage
import com.example.alumni_network.utils.FirebaseConfig
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatRepository {
    private val firestore = FirebaseConfig.firestore
    private var messageListener: ListenerRegistration? = null

    fun sendMessage(chatRoomId: String, message: ChatMessage, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        firestore.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener { documentRef ->
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun listenToMessages(
        chatRoomId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        messageListener?.remove()

        messageListener = firestore.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(messageId = doc.id)
                } ?: emptyList()

                onMessagesUpdated(messages)
            }
    }

    fun stopListening() {
        messageListener?.remove()
        messageListener = null
    }
} 
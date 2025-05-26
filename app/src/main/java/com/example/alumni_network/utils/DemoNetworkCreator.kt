package com.example.alumni_network.utils

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object DemoNetworkCreator {
    private val firestore = FirebaseFirestore.getInstance()

    fun createDemoNetwork(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val networkData = hashMapOf(
            "title" to "Demo Alumni Network",
            "address" to "123 Demo Street, Tech City",
            "image" to "https://picsum.photos/200",  // Random placeholder image
            "description" to "A demo network for testing and learning",
            "gallery" to listOf(
                "https://picsum.photos/800/400",
                "https://picsum.photos/800/401",
                "https://picsum.photos/800/402"
            )
        )

        firestore.collection("networks")
            .add(networkData)
            .addOnSuccessListener { documentReference ->
                // Add a sample internship
                val internshipData = hashMapOf(
                    "companyName" to "Tech Corp",
                    "jobTitle" to "Software Developer Intern",
                    "duration" to "6 months",
                    "stipend" to "30000",
                    "location" to "Remote",
                    "applyLink" to "techcorp@example.com",
                    "description" to "Looking for passionate developers to join our team",
                    "senderName" to "Demo Admin",
                    "senderId" to "demo_admin",
                    "timeStamp" to FieldValue.serverTimestamp(),
                    "likes" to emptyList<String>(),
                    "noOfLikes" to 0
                )

                documentReference.collection("internships")
                    .add(internshipData)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
} 
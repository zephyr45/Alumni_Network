package com.example.alumni_network.bottomfragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alumni_network.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit


class NotificationFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notificationRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerviewfornotification)
        val noNotificationsText = view.findViewById<TextView>(R.id.noNotificationsText)

        notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val currentUserId = mAuth.currentUser?.uid.toString()

        firestore.collection("userRelation")
            .whereEqualTo("requestedTo", currentUserId)
            .whereEqualTo("status", "requested")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val notifications = mutableListOf<NotificationItem>()

                if (querySnapshot.isEmpty) {
                    // No notifications
                    noNotificationsText.visibility = View.VISIBLE
                    notificationRecyclerView.visibility = View.GONE
                    return@addOnSuccessListener
                }

                querySnapshot.documents.forEach { document ->
                    val userArray = document.get("users") as List<String>
                    val notificationSenderId = userArray.first { it != currentUserId }
                    val timestamp = document.getTimestamp("timestamp")?.toDate()?.time ?: 0L

                    firestore.collection("users").document(notificationSenderId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val img = userDoc.getString("imageUrl") ?: ""
                            val firstName = userDoc.getString("firstName") ?: ""
                            val lastName = userDoc.getString("lastName") ?: ""
                            notifications.add(
                                NotificationItem(
                                    img = img,
                                    name = "$firstName $lastName",
                                    message = "requested to connect with you",
                                    time = timestamp.toString(),
                                    notificationSenderId = notificationSenderId
                                )
                            )

                            // Update visibility and set adapter after processing notifications
                            if (notifications.isNotEmpty()) {
                                noNotificationsText.visibility = View.GONE
                                notificationRecyclerView.visibility = View.VISIBLE
                                notificationRecyclerView.adapter =
                                    NotificationAdapter(notifications) { senderId ->
                                        confirmConnection(senderId, notifications, notificationRecyclerView)
                                    }
                            }
                        }
                }
            }
            .addOnFailureListener {
                // Handle error case (optional)
                noNotificationsText.visibility = View.VISIBLE
                notificationRecyclerView.visibility = View.GONE
            }
    }


    private fun confirmConnection(
        notificationSenderId: String,
        notifications: MutableList<NotificationItem>,
        notificationRecyclerView: RecyclerView
    ) {
        val currentUserId = mAuth.currentUser?.uid ?: return
        val relationId = generateRelationId(currentUserId, notificationSenderId)

        val relationRef = firestore.collection("userRelation").document(relationId)

        firestore.runBatch { batch ->
            batch.update(relationRef, "status", "connected")
            batch.update(relationRef, "users", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
        }.addOnSuccessListener {
            notifications.removeAll { it.notificationSenderId == notificationSenderId }
            notificationRecyclerView.adapter?.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            println("Error confirming connection: ${e.message}")
        }
    }






    private fun generateRelationId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_$user2" else "${user2}_$user1"
    }


}


data class NotificationItem(
    val img: String,
    val name: String,
    val message: String,
    val time: String,
    val notificationSenderId: String
)

class NotificationAdapter(
    private var notifications: MutableList<NotificationItem>,
    private val onConfirmClick: (String) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img = itemView.findViewById<ImageView>(R.id.notificationimage)
        val name = itemView.findViewById<TextView>(R.id.notificationame)
        val message = itemView.findViewById<TextView>(R.id.notificationmessage)
        val time = itemView.findViewById<TextView>(R.id.notificationtime)
        val confirmButton = itemView.findViewById<CardView>(R.id.confirmRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notifications[position]
        holder.name.text = item.name
        holder.message.text = item.message
        Glide.with(holder.img.context).load(item.img).placeholder(R.drawable.profiledummy)
            .error(R.drawable.profiledummy).into(holder.img)

        holder.confirmButton.setOnClickListener {
            onConfirmClick(item.notificationSenderId)
        }

        val notificationTime = item.time.toLongOrNull()?.let {
            if (it < 1000000000000L) it * 1000 else it
        } ?: 0L

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - notificationTime

        holder.time.text = when {
            timeDiff < TimeUnit.MINUTES.toMillis(1) -> "-just now"
            timeDiff < TimeUnit.HOURS.toMillis(1) -> "-${TimeUnit.MILLISECONDS.toMinutes(timeDiff)} min ago"
            timeDiff < TimeUnit.DAYS.toMillis(1) -> "-${TimeUnit.MILLISECONDS.toHours(timeDiff)} hours ago"
            else -> "-${TimeUnit.MILLISECONDS.toDays(timeDiff)} days ago"
        }
    }

    override fun getItemCount(): Int = notifications.size
}


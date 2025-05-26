package com.example.alumni_network


import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper

import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit


class Discussions : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var dataList: ArrayList<DiscussionData>
    private lateinit var adapter: DiscussionAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var networkId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_discussions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout = view.findViewById(R.id.refreshForDiscussions)
        recyclerView = view.findViewById(R.id.recyclerViewForDiscussion)
        swipeRefreshLayout.setOnRefreshListener(this)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Discussions"
        networkId = arguments?.getString("networkId").toString()

        dataList = ArrayList()
        addDataToList()
        adapter = DiscussionAdapter(dataList, networkId)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Update the text and stop the refresh indicator
            addDataToList()
            swipeRefreshLayout.isRefreshing = false
        }, 2000)
    }

    private fun addDataToList() {
        mAuth = FirebaseAuth.getInstance()
        val currentUserId = mAuth.currentUser?.uid.toString()
        firestore = FirebaseFirestore.getInstance()
        val networkRef = firestore.collection("networks").document(networkId)
            .collection("discussions")
        Toast.makeText(requireContext(), networkId, Toast.LENGTH_SHORT).show()

        networkRef.get().addOnSuccessListener { documents ->
            val discussionList = ArrayList<DiscussionData>()
            val fetchUserImageTasks = ArrayList<Task<DocumentSnapshot>>() // Store user fetch tasks

            for (document in documents) {
                val id = document.id
                val senderId = document.getString("senderId").toString()
                val name = document.getString("name").toString().toUpperCase()
                val time = document.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                val description = document.getString("description").orEmpty()
                val likes = document.get("likes") as? List<String> ?: emptyList()
                val noOfLikes = document.getLong("noOfLikes")?.toInt() ?: 0
                val discussionImage=document.getString("imageUrl").orEmpty()
                val discussionCategory=document.getString("category").orEmpty()


                // Check if the current user liked the post
                val isLikedByUser = likes.contains(currentUserId)

                // Fetch user image in parallel
                val userImageTask = firestore.collection("users").document(senderId).get()
                fetchUserImageTasks.add(userImageTask)

                // Collect user data after all tasks are complete
                userImageTask.addOnSuccessListener { documentt ->
                    val imageUrl = documentt.getString("imageUrl").toString()
                    discussionList.add(
                        DiscussionData(
                            id = id,
                            img = imageUrl,
                            name = name,
                            time = time,
                            discussionimg = discussionImage, // Replace with actual image source
                            description = description,
                            discussionCategory = discussionCategory,
                            isLikedByUser = isLikedByUser,
                            noOfLikes = noOfLikes
                        )
                    )

                    // Update RecyclerView after processing all data
                    if (discussionList.size == documents.size()) {
                        dataList.clear()
                        dataList.addAll(discussionList)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            // Handle case when some tasks fail
            Tasks.whenAllComplete(fetchUserImageTasks)
                .addOnFailureListener { e ->
                    println("Error fetching user images: ${e.message}")
                }

        }.addOnFailureListener { e ->
            println("Error fetching discussions: ${e.message}")
        }
    }

}

data class DiscussionData(
    val id: String,
    val img: String,
    val name: String,
    val time:Long,
    val discussionimg: String,
    val description: String,
    val discussionCategory:String,
    var isLikedByUser: Boolean,
    var noOfLikes:Int
)

class DiscussionAdapter(
    private val dataList: ArrayList<DiscussionData>,
    private val networkId: String
) : RecyclerView.Adapter<DiscussionAdapter.ViewHolderClass>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    class ViewHolderClass(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uploaderImage: ImageView = itemView.findViewById(R.id.discussionUploaderImage)
        val uploaderName: TextView = itemView.findViewById(R.id.discussionUploaderName)
        val discussionImage: ImageView = itemView.findViewById(R.id.DiscussionImage)
        val discussionDescription: TextView = itemView.findViewById(R.id.DiscussionDescription)
        val discussionCategory:TextView=itemView.findViewById(R.id.DiscussionCategory)
        val likes: ImageView = itemView.findViewById(R.id.DiscussionLikeButton)
        val comments: ImageView = itemView.findViewById(R.id.DiscussionCommentButton)
        val detailedView: LinearLayout =itemView.findViewById(R.id.detailedview)
        val timeofDiscussionPost:TextView=itemView.findViewById(R.id.timeofDiscussionPost)
        val noOfLikes:TextView=itemView.findViewById(R.id.noofLikes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.discussion_itemlayout, parent, false)
        return ViewHolderClass(view)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentItem = dataList[position]

        Glide.with(holder.itemView.context)
            .load(currentItem.img)
            .placeholder(R.drawable.profiledummy)
            .error(R.drawable.profiledummy)
            .into(holder.uploaderImage)

        holder.uploaderName.text = currentItem.name
        Glide.with(holder.itemView.context)
            .load(currentItem.discussionimg)
            .placeholder(R.drawable.loadingimage)
            .error(R.drawable.loadingimage)
            .into(holder.discussionImage)
        holder.discussionDescription.text = currentItem.description
        holder.discussionCategory.text="Category-${currentItem.discussionCategory}"
        holder.noOfLikes.text="${currentItem.noOfLikes} likes"

        //calculating time difference
        val currentTime = System.currentTimeMillis()
        val postedTime = currentItem.time // This should be in milliseconds (from toDate().time)
        val diff = currentTime - postedTime
        val timeText = when {
            TimeUnit.MILLISECONDS.toMinutes(diff) < 60 -> {
                "-${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
            }
            TimeUnit.MILLISECONDS.toHours(diff) < 24 -> {
                "-${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
            }
            else -> {
                "-${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            }
        }
        holder.timeofDiscussionPost.text = timeText


        // Set like button color
        val likeColor = if (currentItem.isLikedByUser) {
            holder.itemView.context.getColor(R.color.lightblueColor)
        } else {
            holder.itemView.context.getColor(R.color.black)
        }
        holder.likes.imageTintList= ColorStateList.valueOf(likeColor)



        // Handle like button click
        holder.likes.setOnClickListener {
            val discussionRef = firestore.collection("networks")
                .document(networkId)
                .collection("discussions")
                .document(currentItem.id)

            if (currentItem.isLikedByUser) {
                // User unlikes the post
                discussionRef.update(
                    mapOf(
                        "likes" to FieldValue.arrayRemove(currentUserId),
                        "noOfLikes" to FieldValue.increment(-1) // Decrease likes count
                    )
                ).addOnSuccessListener {
                    currentItem.isLikedByUser = false
                    currentItem.noOfLikes = (currentItem.noOfLikes - 1)
                    notifyItemChanged(position)
                }.addOnFailureListener { e ->
                    Toast.makeText(holder.itemView.context, "Failed to unlike: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // User likes the post
                discussionRef.update(
                    mapOf(
                        "likes" to FieldValue.arrayUnion(currentUserId),
                        "noOfLikes" to FieldValue.increment(1) // Increase likes count
                    )
                ).addOnSuccessListener {
                    currentItem.isLikedByUser = true
                    currentItem.noOfLikes = (currentItem.noOfLikes + 1)
                    notifyItemChanged(position)
                }.addOnFailureListener { e ->
                    Toast.makeText(holder.itemView.context, "Failed to like: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Handle comment button click
        holder.comments.setOnClickListener {
           val intent=Intent(holder.itemView.context,Comments::class.java)
            intent.putExtra("discussionId", currentItem.id) // Pass discussion ID
            intent.putExtra("networkId", networkId)
            intent.putExtra("typeOfPost", "discussions")
            holder.itemView.context.startActivity(intent)
            // Navigate to comments screen
        }

        // Handle card click
        holder.detailedView.setOnClickListener {
            Toast.makeText(
                holder.itemView.context,
                "Open detailed view for discussion: ${currentItem.name}",
                Toast.LENGTH_SHORT
            ).show()
            // Navigate to detailed discussion screen
        }
    }

}

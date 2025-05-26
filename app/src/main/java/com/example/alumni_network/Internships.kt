package com.example.alumni_network

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot

class Internships : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var dataList: ArrayList<InternshipData>
    private lateinit var adapter: InternshipAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var networkId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_internships, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout = view.findViewById(R.id.refreshforinternship)
        recyclerView = view.findViewById(R.id.recyclerViewForInternships)
        swipeRefreshLayout.setOnRefreshListener(this)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Internships"

        networkId = arguments?.getString("networkId").toString()
        dataList = ArrayList()
        adapter = InternshipAdapter(dataList, networkId)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadInternships()
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            loadInternships()
            swipeRefreshLayout.isRefreshing = false
        }, 2000)
    }

    private fun loadInternships() {
        firestore = FirebaseFirestore.getInstance()
        val internshipRef = firestore.collection("networks").document(networkId).collection("internships")
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        internshipRef.get().addOnSuccessListener { documents ->
            val internshipList = ArrayList<InternshipData>()
            val userFetchTasks = ArrayList<Task<DocumentSnapshot>>()

            for (document in documents) {
                val id = document.id
                val senderId = document.getString("senderId").orEmpty()
                val name = document.getString("senderName").orEmpty()
                val time = document.getTimestamp("timeStamp")?.toDate()?.time ?: 0L
                val companyName = document.getString("companyName").orEmpty()
                val duration = document.getString("duration").orEmpty()
                val role = document.getString("jobTitle").orEmpty()
                val location = document.getString("location").orEmpty()
                val stipend = document.getString("stipend")?.toInt() ?: 0
                val applyLink = document.getString("applyLink").orEmpty()
                val description = document.getString("description").orEmpty()
                val likes = document.get("likes") as? List<String> ?: emptyList()
                val noOfLikes = document.getLong("noOfLikes")?.toInt() ?: 0
                val isLikedByUser = likes.contains(currentUserId)

                // Fetch user image in parallel
                val userFetchTask = firestore.collection("users").document(senderId).get()
                userFetchTasks.add(userFetchTask)

                userFetchTask.addOnSuccessListener { userDoc ->
                    val imageUrl = userDoc.getString("imageUrl").orEmpty()
                    internshipList.add(
                        InternshipData(
                            id, imageUrl, name, time, companyName, duration, role,
                            location, stipend, applyLink, description, isLikedByUser, noOfLikes
                        )
                    )

                    // Update RecyclerView once all internships are fetched
                    if (internshipList.size == documents.size()) {
                        dataList.clear()
                        dataList.addAll(internshipList)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            // Handle potential failures in user image fetch
            Tasks.whenAllComplete(userFetchTasks).addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading some user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error loading internships: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}

data class InternshipData(
    val id: String,
    val img: String,
    val name: String,
    val time: Long,
    val companyName: String,
    val duration: String,
    val role: String,
    val location: String,
    val stipend: Int,
    val applyLink: String,
    val description: String,
    var isLikedByUser: Boolean,
    var noOfLikes: Int
)

class InternshipAdapter(
    private val dataList: ArrayList<InternshipData>,
    private val networkId: String
) : RecyclerView.Adapter<InternshipAdapter.ViewHolderClass>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    class ViewHolderClass(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uploaderImage: ImageView = itemView.findViewById(R.id.userimageforInternship)
        val uploaderName: TextView = itemView.findViewById(R.id.userNameForInternship)
        val internshipCompany: TextView = itemView.findViewById(R.id.InternshipCompany)
        val internshipRole: TextView = itemView.findViewById(R.id.InternshipRole)
        val time:TextView=itemView.findViewById(R.id.timeStampForInternship)
        val internshipLocation: TextView = itemView.findViewById(R.id.InternshipLocation)
        val internshipStipend: TextView = itemView.findViewById(R.id.InternhipStipend)
        val internshipDuration: TextView = itemView.findViewById(R.id.InternshipDuration)
        val internshipDescription: TextView = itemView.findViewById(R.id.InternshipDescription)
        val internshipApplyLink: TextView = itemView.findViewById(R.id.InternshipApplyLink)
        val likes: ImageView = itemView.findViewById(R.id.InternshipLikeButton)
        val noOfLikes: TextView = itemView.findViewById(R.id.InternshipLikes)
        val applyLink:TextView=itemView.findViewById(R.id.InternshipApplyLink)
        val insternshipCommentButton=itemView.findViewById<ImageView>(R.id.InternshipCommentButton)
        val postTypelabel=itemView.findViewById<TextView>(R.id.postTypeLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.internship_item_layout, parent, false)
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
        holder.postTypelabel.text="Internships"
        holder.uploaderName.text = currentItem.name
        holder.internshipCompany.text = "Company Name: ${currentItem.companyName}"
        holder.internshipRole.text = "Role: ${currentItem.role}"
        holder.internshipLocation.text = "Locationtion: ${currentItem.location}"
        holder.internshipStipend.text = "Stipend: ${currentItem.stipend}"
        holder.internshipDuration.text = "Duration: ${currentItem.duration}"
        holder.internshipDescription.text = "Description: ${currentItem.description}"
        holder.noOfLikes.text = "${currentItem.noOfLikes} likes"
        holder.applyLink.text="Apply link: ${currentItem.applyLink}"
        val currentTime = System.currentTimeMillis()
        val postedTime = currentItem.time
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
        holder.time.text = timeText


        val likeColor = if (currentItem.isLikedByUser) {
            holder.itemView.context.getColor(R.color.lightblueColor)
        } else {
            holder.itemView.context.getColor(R.color.black)
        }
        holder.likes.imageTintList = ColorStateList.valueOf(likeColor)

        holder.likes.setOnClickListener {
            val internshipRef = firestore.collection("networks")
                .document(networkId)
                .collection("internships")
                .document(currentItem.id)

            if (currentItem.isLikedByUser) {
                internshipRef.update(
                    "likes", FieldValue.arrayRemove(currentUserId),
                    "noOfLikes", FieldValue.increment(-1)
                ).addOnSuccessListener {
                    currentItem.isLikedByUser = false
                    currentItem.noOfLikes -= 1
                    notifyItemChanged(position)
                }
            } else {
                internshipRef.update(
                    "likes", FieldValue.arrayUnion(currentUserId),
                    "noOfLikes", FieldValue.increment(1)
                ).addOnSuccessListener {
                    currentItem.isLikedByUser = true
                    currentItem.noOfLikes += 1
                    notifyItemChanged(position)
                }
            }
        }

        holder.internshipApplyLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentItem.applyLink))
            holder.itemView.context.startActivity(intent)
        }
        holder.insternshipCommentButton.setOnClickListener {
            val intent=Intent(holder.itemView.context,Comments::class.java)
            intent.putExtra("discussionId", currentItem.id) // Pass discussion ID
            intent.putExtra("networkId", networkId)
            intent.putExtra("typeOfPost", "internships")
            holder.itemView.context.startActivity(intent)
        }
    }
}

package com.example.alumni_network
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit
import android.content.res.ColorStateList
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot

class AllPosts : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var combinedList: ArrayList<PostItem>
    private lateinit var adapter: CombinedPostAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var networkId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        loadAllPosts()
    }

    private fun setupViews(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.refreshForAllPosts)
        recyclerView = view.findViewById(R.id.recyclerviewForAllPosts)
        swipeRefreshLayout.setOnRefreshListener(this)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "All Posts"

        networkId = arguments?.getString("networkId").toString()
        firestore = FirebaseFirestore.getInstance()
        combinedList = ArrayList()
        adapter = CombinedPostAdapter(combinedList, networkId)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            loadAllPosts()
            swipeRefreshLayout.isRefreshing = false
        }, 2000)
    }

    private fun loadAllPosts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        combinedList.clear()

        // Load Internships
        firestore.collection("networks").document(networkId)
            .collection("internships")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val senderId = document.getString("senderId") ?: ""
                    firestore.collection("users").document(senderId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val internship = PostItem.InternshipItem(
                                InternshipData(
                                    id = document.id,
                                    img = userDoc.getString("imageUrl") ?: "",
                                    name = document.getString("senderName")?:"",
                                    time = document.getTimestamp("timeStamp")?.toDate()?.time ?: 0L,
                                    companyName = document.getString("companyName") ?: "",
                                    duration = document.getString("duration") ?: "",
                                    role = document.getString("jobTitle") ?: "",
                                    location = document.getString("location") ?: "",
                                    stipend = document.getString("stipend")?.toIntOrNull() ?: 0,
                                    applyLink = document.getString("applyLink") ?: "",
                                    description = document.getString("description") ?: "",
                                    isLikedByUser = (document.get("likes") as? List<String>)?.contains(currentUserId) ?: false,
                                    noOfLikes = document.getLong("noOfLikes")?.toInt() ?: 0
                                )
                            )
                            combinedList.add(internship)
                            updateRecyclerView()
                        }
                }
            }

        // Load Jobs
        firestore.collection("networks").document(networkId)
            .collection("jobs")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val id = document.id
                    val senderId = document.getString("senderId").orEmpty()
                    val name = document.getString("senderName").orEmpty()
                    val time = document.getTimestamp("timeStamp")?.toDate()?.time ?: 0L
                    val companyName = document.getString("companyName").orEmpty()
                    val minimumExperience = document.getString("minimumExperience").orEmpty()
                    val role = document.getString("jobTitle").orEmpty()
                    val location = document.getString("location").orEmpty()
                    val salary = document.getString("salary")?:""
                    val applyLink = document.getString("applyLink").orEmpty()
                    val description = document.getString("description").orEmpty()
                    val likes = document.get("likes") as? List<String> ?: emptyList()
                    val noOfLikes = document.getLong("noOfLikes")?.toInt() ?: 0
                    val isLikedByUser = likes.contains(currentUserId)

                    // Fetch user image
                    firestore.collection("users").document(senderId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val imageUrl = userDoc.getString("imageUrl").orEmpty()
                            val job = PostItem.JobItem(
                                JobData(
                                    id = id,
                                    img = imageUrl,
                                    name = name,
                                    time = time,
                                    companyName = companyName,
                                    minimumExperience = minimumExperience,
                                    role = role,
                                    location = location,
                                    salary = salary,
                                    applyLink = applyLink,
                                    description = description,
                                    isLikedByUser = isLikedByUser,
                                    noOfLikes = noOfLikes
                                )
                            )
                            combinedList.add(job)
                            updateRecyclerView()
                        }
                }
            }

        // Load Discussions
        firestore.collection("networks").document(networkId)
            .collection("discussions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val discussionList = ArrayList<PostItem.DiscussionItem>()
                val fetchUserImageTasks = ArrayList<Task<DocumentSnapshot>>()

                for (document in documents) {
                    val id = document.id
                    val senderId = document.getString("senderId").toString()
                    val name = document.getString("name").toString().toUpperCase()
                    val time = document.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    val description = document.getString("description").orEmpty()
                    val likes = document.get("likes") as? List<String> ?: emptyList()
                    val noOfLikes = document.getLong("noOfLikes")?.toInt() ?: 0
                    val discussionImage = document.getString("imageUrl").orEmpty()
                    val discussionCategory = document.getString("category").orEmpty()
                    val isLikedByUser = likes.contains(currentUserId)

                    // Fetch user image in parallel
                    val userImageTask = firestore.collection("users").document(senderId).get()
                    fetchUserImageTasks.add(userImageTask)

                    userImageTask.addOnSuccessListener { userDoc ->
                        val imageUrl = userDoc.getString("imageUrl").toString()
                        val discussion = PostItem.DiscussionItem(
                            DiscussionData(
                                id = id,
                                img = imageUrl,
                                name = name,
                                time = time,
                                discussionimg = discussionImage,
                                description = description,
                                discussionCategory = discussionCategory,
                                isLikedByUser = isLikedByUser,
                                noOfLikes = noOfLikes
                            )
                        )
                        combinedList.add(discussion)
                        updateRecyclerView()
                    }
                }
            }}



    private fun updateRecyclerView() {
        combinedList.sortByDescending {
            when (it) {
                is PostItem.InternshipItem -> it.data.time
                is PostItem.JobItem -> it.data.time
                is PostItem.DiscussionItem -> it.data.time
            }
        }
        adapter.notifyDataSetChanged()
    }
}

sealed class PostItem {
    data class InternshipItem(val data: InternshipData) : PostItem()
    data class JobItem(val data: JobData) : PostItem()
    data class DiscussionItem(val data: DiscussionData) : PostItem()
}

class CombinedPostAdapter(
    private val items: List<PostItem>,
    private val networkId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    companion object {
        private const val TYPE_INTERNSHIP = 1
        private const val TYPE_JOB = 2
        private const val TYPE_DISCUSSION = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PostItem.InternshipItem -> TYPE_INTERNSHIP
            is PostItem.JobItem -> TYPE_JOB
            is PostItem.DiscussionItem -> TYPE_DISCUSSION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_INTERNSHIP -> InternshipAdapter.ViewHolderClass(
                inflater.inflate(R.layout.internship_item_layout, parent, false)
            )
            TYPE_JOB -> JobAdapter.ViewHolderClass(
                inflater.inflate(R.layout.internship_item_layout, parent, false)
            )
            TYPE_DISCUSSION -> DiscussionAdapter.ViewHolderClass(
                inflater.inflate(R.layout.discussion_itemlayout, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PostItem.InternshipItem -> bindInternshipPost(holder as InternshipAdapter.ViewHolderClass, item.data)
            is PostItem.JobItem -> bindJobPost(holder as JobAdapter.ViewHolderClass, item.data)
            is PostItem.DiscussionItem -> bindDiscussionPost(holder as DiscussionAdapter.ViewHolderClass, item.data)
        }
    }

    private fun bindInternshipPost(holder: InternshipAdapter.ViewHolderClass, data: InternshipData) {
        with(holder) {
            Glide.with(itemView.context).load(data.img)
                .placeholder(R.drawable.profiledummy)
                .error(R.drawable.profiledummy).into(uploaderImage)
            postTypelabel.text = "Internships"
            uploaderName.text = data.name+" -"
            internshipCompany.text = "Company Name: ${data.companyName}"
            internshipRole.text = "Role: ${data.role}"
            internshipLocation.text = "Location: ${data.location}"
            internshipStipend.text = "Stipend: ${data.stipend}"
            internshipDuration.text = "Duration: ${data.duration}"
            internshipDescription.text = "Description: ${data.description}"
            noOfLikes.text = "${data.noOfLikes} likes"
            applyLink.text = "Apply link: ${data.applyLink}"
            time.text = formatTimeAgo(data.time)

            // Set like button color based on like status
            if (data.isLikedByUser) {
                likes.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.lightblueColor))
            } else {
                likes.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.black))
            }

            // Handle likes
            likes.setOnClickListener {
                val postRef = firestore.collection("networks")
                    .document(networkId)
                    .collection("internships")
                    .document(data.id)

                if (data.isLikedByUser) {
                    postRef.update(
                        "likes", FieldValue.arrayRemove(currentUserId),
                        "noOfLikes", FieldValue.increment(-1)
                    ).addOnSuccessListener {
                        data.isLikedByUser = false
                        data.noOfLikes -= 1
                        notifyDataSetChanged()
                    }
                } else {
                    postRef.update(
                        "likes", FieldValue.arrayUnion(currentUserId),
                        "noOfLikes", FieldValue.increment(1)
                    ).addOnSuccessListener {
                        data.isLikedByUser = true
                        data.noOfLikes += 1
                        notifyDataSetChanged()
                    }
                }
            }

            // Handle comments
            insternshipCommentButton.setOnClickListener {  // Changed to commentButton
                val intent = Intent(itemView.context, Comments::class.java)
                intent.putExtra("networkId", networkId)
                intent.putExtra("discussionId", data.id)
                intent.putExtra("typeOfPost", "internships")
                itemView.context.startActivity(intent)
            }
            holder.internshipApplyLink.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.applyLink))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    private fun bindJobPost(holder: JobAdapter.ViewHolderClass, data: JobData) {
        with(holder) {
            Glide.with(itemView.context).load(data.img)
                .placeholder(R.drawable.profiledummy).
                error(R.drawable.profiledummy).into(uploaderImage)
            postTypelabel.text = "Jobs"
            uploaderName.text = data.name+" -"
            jobCompany.text = "Company Name: ${data.companyName}"
            jobRole.text = "Role: ${data.role}"
            jobLocation.text = "Location: ${data.location}"
            jobSalary.text = "Expected Salary: ${data.salary}"
            minimumExperience.text = "Minimum Experience: ${data.minimumExperience}"
            jobDescription.text = "Description: ${data.description}"
            noOfLikes.text = "${data.noOfLikes} likes"
            applyLink.text = "Apply link: ${data.applyLink}"
            time.text = formatTimeAgo(data.time)

            // Set like button color based on like status
            if (data.isLikedByUser) {
                likes.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.lightblueColor))
            } else {
                likes.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.black))
            }

            // Handle likes
            likes.setOnClickListener {
                val postRef = firestore.collection("networks")
                    .document(networkId)
                    .collection("jobs")
                    .document(data.id)

                if (data.isLikedByUser) {
                    postRef.update(
                        "likes", FieldValue.arrayRemove(currentUserId),
                        "noOfLikes", FieldValue.increment(-1)
                    ).addOnSuccessListener {
                        data.isLikedByUser = false
                        data.noOfLikes -= 1
                        notifyDataSetChanged()
                    }
                } else {
                    postRef.update(
                        "likes", FieldValue.arrayUnion(currentUserId),
                        "noOfLikes", FieldValue.increment(1)
                    ).addOnSuccessListener {
                        data.isLikedByUser = true
                        data.noOfLikes += 1
                        notifyDataSetChanged()
                    }
                }
            }

            // Handle comments
            jobCommentButton.setOnClickListener {  // Changed to commentButton
                val intent = Intent(itemView.context, Comments::class.java)
                intent.putExtra("networkId", networkId)
                intent.putExtra("discussionId", data.id)
                intent.putExtra("typeOfPost", "jobs")
                itemView.context.startActivity(intent)
            }
            holder.jobApplyLink.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.applyLink))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    private fun bindDiscussionPost(holder: DiscussionAdapter.ViewHolderClass, data: DiscussionData) {
        with(holder) {
            Glide.with(itemView.context).load(data.img)
                .placeholder(R.drawable.profiledummy).error(R.drawable.profiledummy).into(uploaderImage)
            uploaderName.text = data.name+" -"
            discussionDescription.text = data.description
            discussionCategory.text = data.discussionCategory
            noOfLikes.text = "${data.noOfLikes} likes"
            timeofDiscussionPost.text = formatTimeAgo(data.time)


            discussionImage.visibility = View.VISIBLE
            Glide.with(itemView.context).load(data.discussionimg)
                .placeholder(R.drawable.loadingimage).error(R.drawable.loadingimage).into(discussionImage)


            // Set like button color based on like status
            if (data.isLikedByUser) {
                likes.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.lightblueColor))
            } else {
                likes.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.black))
            }

            // Handle likes
            likes.setOnClickListener {
                val postRef = firestore.collection("networks")
                    .document(networkId)
                    .collection("discussions")
                    .document(data.id)

                if (data.isLikedByUser) {
                    postRef.update(
                        mapOf(
                            "likes" to FieldValue.arrayRemove(currentUserId),
                            "noOfLikes" to FieldValue.increment(-1)
                        )
                    ).addOnSuccessListener {
                        data.isLikedByUser = false
                        data.noOfLikes -= 1
                        notifyDataSetChanged()
                    }
                } else {
                    postRef.update(
                        mapOf(
                            "likes" to FieldValue.arrayUnion(currentUserId),
                            "noOfLikes" to FieldValue.increment(1)
                        )
                    ).addOnSuccessListener {
                        data.isLikedByUser = true
                        data.noOfLikes += 1
                        notifyDataSetChanged()
                    }
                }
            }

            // Handle comments
            comments.setOnClickListener {  // Changed to commentButton
                val intent = Intent(itemView.context, Comments::class.java)
                intent.putExtra("networkId", networkId)
                intent.putExtra("discussionId", data.id)
                intent.putExtra("typeOfPost", "discussions")
                itemView.context.startActivity(intent)
            }
        }
    }

    private fun formatTimeAgo(time: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - time
        return when {
            TimeUnit.MILLISECONDS.toMinutes(diff) < 60 -> {
                "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
            }
            TimeUnit.MILLISECONDS.toHours(diff) < 24 -> {
                "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
            }
            else -> {
                "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            }
        }
    }

    override fun getItemCount() = items.size
}
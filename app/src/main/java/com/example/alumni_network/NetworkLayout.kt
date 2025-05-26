package com.example.alumni_network

import android.os.Bundle
import com.example.alumni_network.News
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.alumni_network.databinding.ActivityNetworkLayoutBinding
import com.google.firebase.firestore.FirebaseFirestore

class NetworkLayout : AppCompatActivity() {
    private lateinit var binding: ActivityNetworkLayoutBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedCategory: TextView? = null
    private lateinit var viewPager2: ViewPager2
    private lateinit var handler: Handler
    private lateinit var pageChangeListener: ViewPager2.OnPageChangeCallback
    private val params = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(8, 0, 8, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarnetwork)
        setSupportActionBar(toolbar)

        val networkIdmsg = intent.getStringExtra("networkId").toString()
        val networkLogoMsg = intent.getStringExtra("networkLogo")

        viewPager2 = findViewById(R.id.viewpager2)
        val tabLayout: LinearLayout = findViewById(R.id.slideDotLL)
        val imageAdapter = ImageAdapter()
        viewPager2.adapter = imageAdapter
        supportActionBar?.title = "Network"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enables back button
        supportActionBar?.setDisplayShowHomeEnabled(true)
        firestore = FirebaseFirestore.getInstance()

        // Auto-scroll handler
        handler = Handler(Looper.getMainLooper())
        val autoScrollRunnable = object : Runnable {
            override fun run() {
                val currentItem = viewPager2.currentItem
                val nextItem = (currentItem + 1) % (imageAdapter.itemCount)
                viewPager2.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 2500) // Auto-scroll every 2.5 seconds
            }
        }

        firestore.collection("networks").document(networkIdmsg)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val gallery = document.get("gallery") as? List<String> ?: emptyList()
                    val imageList = gallery.mapIndexed { index, url ->
                        ImageItem(id = index.toString(), url = url)
                    }
                    imageAdapter.submitList(imageList)

                    // Update dots dynamically based on the number of images
                    val dotsImage = Array(imageList.size) { ImageView(this) }
                    tabLayout.removeAllViews() // Clear previous dots
                    dotsImage.forEach {
                        it.setImageResource(R.drawable.non_active_dot)
                        tabLayout.addView(it, params)
                    }
                    if (dotsImage.isNotEmpty()) dotsImage[0].setImageResource(R.drawable.active_dot)

                    pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            dotsImage.forEachIndexed { index, imageView ->
                                if (position == index) {
                                    imageView.setImageResource(R.drawable.active_dot)
                                } else {
                                    imageView.setImageResource(R.drawable.non_active_dot)
                                }
                            }
                            super.onPageSelected(position)
                        }
                    }
                    viewPager2.registerOnPageChangeCallback(pageChangeListener)

                    // Start auto-scroll
                    handler.postDelayed(autoScrollRunnable, 3000)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }

        val networkLogo = findViewById<ImageView>(R.id.networklogo)
        Glide.with(this)
            .load(networkLogoMsg)
            .placeholder(R.drawable.bmscelogo)
            .error(R.drawable.bmscelogo)
            .into(networkLogo)

        val networkTitle = findViewById<TextView>(R.id.networkTitle)
        networkTitle.text = intent.getStringExtra("networktitle")
        val bundle = Bundle()
        bundle.putString("networkId", networkIdmsg)

        // Handle button clicks
        binding.postDiscussions.setOnClickListener { replaceFragment1(PostDiscussions(), bundle) }
        binding.postJob.setOnClickListener { replaceFragment1(PostJobs(), bundle) }
        binding.postInternship.setOnClickListener { replaceFragment1(PostInternships(), bundle) }
        binding.Membershiplogin.setOnClickListener { /* Handle Membership Login */ }
        binding.eventTickets.setOnClickListener { /* Handle Event Tickets */ }
        binding.members.setOnClickListener { replaceFragment1(Members(), bundle) }
        binding.nearMe.setOnClickListener { replaceFragment1(NearmeUsers(), bundle) }

        // Additional category selections
        binding.Discussions.setOnClickListener { setSelectedCategory(binding.Discussions); replaceFragment(Discussions(), bundle) }
        binding.Events.setOnClickListener { setSelectedCategory(binding.Events); replaceFragment(Events(), bundle) }
        binding.jobs.setOnClickListener { setSelectedCategory(binding.jobs); replaceFragment(Jobs(), bundle) }
        binding.news.setOnClickListener { setSelectedCategory(binding.news); replaceFragment(News(), bundle) }
        binding.internships.setOnClickListener { setSelectedCategory(binding.internships); replaceFragment(Internships(), bundle) }
        binding.networkChat.setOnClickListener { replaceFragment1(NetworkChats(), bundle) }
        binding.Allposts.setOnClickListener { setSelectedCategory(binding.Allposts); replaceFragment(AllPosts(), bundle) }
    }

    private fun setSelectedCategory(newSelected: TextView?) {
        selectedCategory?.isSelected = false
        newSelected?.isSelected = true
        selectedCategory = newSelected
    }

    private fun replaceFragment1(fragment: Fragment, bundle: Bundle) {
        fragment.arguments = bundle
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.fullContent.visibility = View.GONE
        binding.informationCard.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment, bundle: Bundle) {
        fragment.arguments = bundle
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.mainContent.visibility = View.GONE
        binding.informationCard.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (currentFragment is Internships ||
            currentFragment is Jobs ||
            currentFragment is Events ||
            currentFragment is Discussions ||
            currentFragment is News) {

            binding.mainContent.visibility = View.VISIBLE
            binding.informationCard.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
            supportActionBar?.title = "Network"
            setSelectedCategory(null)

            supportFragmentManager.beginTransaction()
                .remove(currentFragment)
                .commit()
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            binding.fullContent.visibility = View.VISIBLE
            binding.informationCard.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
            supportActionBar?.title = "Network"
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

data class ImageItem(val id: String, val url: String)

class ImageAdapter : ListAdapter<ImageItem, ImageAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem == newItem
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bindData(item: ImageItem) {
            Glide.with(itemView)
                .load(item.url)
                .into(imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.image_item_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageItem = getItem(position)
        holder.bindData(imageItem)
    }
}

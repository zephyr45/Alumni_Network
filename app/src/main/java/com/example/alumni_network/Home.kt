package com.example.alumni_network

import android.content.ClipData.Item
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.webauthn.Cbor
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.alumni_network.bottomfragment.CategoryFragment
import com.example.alumni_network.navfragment.Homefragment
import com.example.alumni_network.navfragment.ProfileFragment
import com.example.alumni_network.navfragment.SettingFragment
import com.example.alumni_network.navfragment.chatBot
import com.example.alumni_network.navfragment.video
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class Home : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,Homefragment.HomeFragmentListener {


    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mAuth:FirebaseAuth
    private lateinit var mGoogleSignInClient:GoogleSignInClient
    private lateinit var firestore:FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        drawerLayout = findViewById(R.id.main)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Navigation Drawer setup
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.setCheckedItem(R.id.nav_home)
        replaceFragment(Homefragment())

        navigationView.setNavigationItemSelectedListener(this)

        // Toggle for Drawer
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        ///fetching username and useremail
        fetchForProfile()
    }
    override fun updateTitle(title: String) {
        // This method will be called from Homefragment to update the title
        supportActionBar?.title = title
    }

    private fun fetchForProfile() {
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0) // Get the first (and only) header view

        val username = headerView.findViewById<TextView>(R.id.userName)
        val useremail = headerView.findViewById<TextView>(R.id.userEmail)
        val userProfile=headerView.findViewById<ImageView>(R.id.profileimagefornavheader)

        val user = mAuth.currentUser
        if (user == null) {
            Log.e("fetchForProfile", "No user logged in.")
            return
        }

        val userRef = firestore.collection("users").document(user.uid)
        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    val userfirstName = document.getString("firstName") ?.toUpperCase()?: "First"
                    val userlastName = document.getString("lastName")?.toUpperCase()?: "Last"
                    username.text = "$userfirstName $userlastName"
                    useremail.text = user.email ?: "No Email"
                    val profileUrl=document.getString("imageUrl").toString()
                    Glide.with(this)
                        .load(profileUrl)
                        .placeholder(R.drawable.profiledummy)
                        .error(R.drawable.profiledummy)
                        .into(userProfile)
                    Log.d("fetchForProfile", "Profile loaded successfully")
                } else {
                    Log.d("Firestore", "No such document")
                }
            } else {
                Log.e("Firestore", "Fetch failed: ", task.exception)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                replaceFragment(Homefragment())
                supportActionBar?.title="My Network"

            }
            R.id.nav_profile -> {
                replaceFragment(ProfileFragment())
                supportActionBar?.title = "My Profile"
            }
            R.id.nav_chatbot -> {
                replaceFragment(chatBot())
                supportActionBar?.title = "Chatbot"
            }
            R.id.nav_video -> {
                replaceFragment(video())
                supportActionBar?.title = "Videos"
            }
            R.id.nav_share -> {
                shareApp()

            }
            R.id.nav_logout -> {
                logout_fn()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun shareApp() {
        val appPackageName = packageName
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        val appLink = "https://play.google.com/store/apps/details?id=$appPackageName"
        intent.putExtra(Intent.EXTRA_TEXT, "Check out this app: $appLink")
        val chooser = Intent.createChooser(intent, "Share app with")
        startActivity(chooser)
    }



    private fun logout_fn() {
        val builder=AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout")
        builder.setIcon(R.drawable.logout_24)

        builder.setPositiveButton("Yes"){dialogInterface, which ->
            Firebase.auth.signOut()

            // Sign out from Google Sign-In
            mGoogleSignInClient.signOut().addOnCompleteListener {
                // After sign out, go to the login screen
                val intent = Intent(this@Home, Login::class.java)
                startActivity(intent)
                finish() // Ensure user cannot go back to Home activity
            }
        }
        builder.setNeutralButton("No"){dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }


    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.navfragment, fragment).commit()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}


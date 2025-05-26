package com.example.alumni_network

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.alumni_network.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : AppCompatActivity() {
    companion object {
        private const val RC_SIGN_IN = 9001
    }
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        
        firebaseAuth = FirebaseAuth.getInstance()

        var signupvar = findViewById<TextView>(R.id.signup)
        var loginvar = findViewById<TextView>(R.id.login)
        var signuplay = findViewById<LinearLayout>(R.id.signuplayout)
        var loginlay = findViewById<LinearLayout>(R.id.loginlayout)
        var loginbutton = findViewById<Button>(R.id.loginbutton)
        var msg = "loging"
        val forgotPass = findViewById<TextView>(R.id.forgot_pass)

        // Check if user is already logged in
        if (firebaseAuth.currentUser != null) {
            val userId = firebaseAuth.currentUser?.uid
            val sharedPreferences = getSharedPreferences("user_pref", MODE_PRIVATE)
            val userExists = sharedPreferences.getBoolean("userExists", false)
            
            if (userExists) {
                navigateToHomePage()
            } else {
                checkIfUserExists(userId.toString()) { exists ->
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("userExists", exists)
                    editor.apply()

                    if (exists) {
                        navigateToHomePage()
                    } else {
                        navigateToProfilePage()
                    }
                }
            }
            return
        }

        binding.loginbutton.setOnClickListener {
            if (msg == "signing") {
                signingWithEmail()
            } else if (msg == "loging") {
                logingWithEmail()
            }
        }

        signupvar.setOnClickListener {
            signupvar.background = resources.getDrawable(R.drawable.switch_trcks, null)
            signupvar.setTextColor(resources.getColor(R.color.textColor, null))
            loginvar.background = null
            signuplay.visibility = View.VISIBLE
            loginlay.visibility = View.GONE
            loginvar.setTextColor(resources.getColor(R.color.darkblueColor, null))
            loginbutton.text = "Sign Up"
            msg = "signing"
        }

        loginvar.setOnClickListener {
            loginvar.background = resources.getDrawable(R.drawable.switch_trcks, null)
            loginvar.setTextColor(resources.getColor(R.color.textColor, null))
            signupvar.background = null
            loginlay.visibility = View.VISIBLE
            signuplay.visibility = View.GONE
            signupvar.setTextColor(resources.getColor(R.color.darkblueColor, null))
            loginbutton.text = "Log In"
            msg = "loging"
        }

        //google button login
        val GooglesignInButton = findViewById<LinearLayout>(R.id.google_btn)
        GooglesignInButton.setOnClickListener {
            signIn()
        }

        //forgetting password
        forgotPass.setOnClickListener {
            try {
                goToResetPage()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logingWithEmail() {
        val email=binding.emaill.text.toString().trim()
        val pass = binding.passwordl.text.toString()
        if(email.isNotEmpty() && pass.isNotEmpty()){
            firebaseAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener{
                if(it.isSuccessful){
                    checkIfUserExists(firebaseAuth.currentUser?.uid.toString()) { userExists ->
                        if (userExists) {
                            navigateToHomePage()
                        } else {
                            val user=firebaseAuth.currentUser
                            val intentToProfile=Intent(this@Login,profilepage::class.java)
                            intentToProfile.putExtra("uid",user?.uid)
                            intentToProfile.putExtra("email",user?.email)
                            startActivity(intentToProfile)
                        }
                    }
                    //email sending
                    welcomeAgainMessage(email);
                }else{

                    Toast.makeText(this,it.exception.toString(),Toast.LENGTH_LONG).show()

                }

            }

        }else{
            Toast.makeText(this,"email and password cannot be empty",Toast.LENGTH_LONG).show()
        }


    }

    private fun signingWithEmail() {
        val email=binding.logins.text.toString().trim()
        val pass = binding.passwords.text.toString()
        val confirmpass=binding.passwordsConfirm.text.toString()
        if(email.isNotEmpty() && pass.isNotEmpty() && confirmpass.isNotEmpty()){
            if(pass==confirmpass){
                firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener{
                    if(it.isSuccessful){
//                                loginvar.background=resources.getDrawable(R.drawable.switch_trcks,null)
//                                loginvar.setTextColor(resources.getColor(R.color.textColor,null))
//                                signupvar.background=null;
//                                loginlay.visibility= View.VISIBLE
//                                signuplay.visibility=View.GONE
//                                signupvar.setTextColor(resources.getColor(R.color.darkblueColor,null))
//                                loginbutton.text="Log In"
//                                msg="loging"
                        val intentToProfile=Intent(this@Login,profilepage::class.java)
                        intentToProfile.putExtra("uid",firebaseAuth.currentUser?.uid)
                        intentToProfile.putExtra("email",email)
                        firebaseAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener{
                            if(it.isSuccessful){
                                Toast.makeText(this,"Signin successfull",Toast.LENGTH_LONG).show()
                            }else{
                                Toast.makeText(this,"Signin error",Toast.LENGTH_LONG).show()
                            }
                        }
                        startActivity(intentToProfile)
                        //sending messages to email
                        welcomeMessage(email)
                    }else{
                        Toast.makeText(this,it.exception.toString(),Toast.LENGTH_LONG).show()
                    }
                }
            }
            else{
                Toast.makeText(this,"Password and confirm password should be same",Toast.LENGTH_LONG).show()

            }
        }else{
            Toast.makeText(this,"email and password cannot be empty",Toast.LENGTH_LONG).show()
        }
    }

    private fun welcomeMessage(email:String){
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.sendgrid.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val sendGridService = retrofit.create(SendGridService::class.java)
        val signupsubject=getString(R.string.subjectemailforsignup)
        val signupmessage=getString(R.string.emailmessageforsignup)
        val emailRequest = EmailRequest(
            personalizations = listOf(Personalization(listOf(To(email)))),
            from = From("gouravagarwal014@gmail.com"),
            subject = signupsubject,
            content = listOf(Content("text/plain", signupmessage))
        )

        val call = sendGridService.sendEmail(emailRequest)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@Login, "successful email signup", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@Login, "Un successful ${response.code()} - ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                println("Error: ${t.message}")
            }
        })

    }
    private fun welcomeAgainMessage(email:String){
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.sendgrid.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val sendGridService = retrofit.create(SendGridService::class.java)
        val loginsubject=getString(R.string.subjectforlogin)
        val loginmessage=getString(R.string.messageforlogin)
        val emailRequest = EmailRequest(
            personalizations = listOf(Personalization(listOf(To(email)))),
            from = From("gouravagarwal014@gmail.com"),
            subject = loginsubject,
            content = listOf(Content("text/plain", loginmessage))
        )

        val call = sendGridService.sendEmail(emailRequest)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@Login, "successful", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@Login, "Un successful ${response.code()} - ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                println("Error: ${t.message}")
            }
        })
    }
    private fun signIn(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener(this) {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
//        val signInIntent = googleSignInClient.signInIntent
//        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    val email=user?.email.toString()
                    if (isNewUser) {
                        welcomeMessage(email)
                        val intentToProfile=Intent(this@Login,profilepage::class.java)
                        intentToProfile.putExtra("uid",user?.uid)
                        intentToProfile.putExtra("email",user?.email)
                        startActivity(intentToProfile)
                    } else {
                       welcomeAgainMessage(email)
                        checkIfUserExists(user?.uid.toString()) { userExists ->
                            if (userExists) {
                                val intent=Intent(this@Login,Home::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                val user=firebaseAuth.currentUser
                                val intentToProfile=Intent(this@Login,profilepage::class.java)
                                intentToProfile.putExtra("uid",user?.uid)
                                intentToProfile.putExtra("email",user?.email)
                                startActivity(intentToProfile)
                            }
                        }

                    }
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToResetPage() {
        val intent = Intent(this@Login,ForgotPassword::class.java)
        startActivity(intent)
    }




    private fun checkIfUserExists(uid: String, callback: (Boolean) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(uid)

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                callback(document != null && document.exists())
            } else {
                callback(false)
            }
        }
    }

    private fun navigateToHomePage() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }
    private fun navigateToProfilePage() {
        val intent = Intent(this, profilepage::class.java)
        startActivity(intent)
        finish()
    }
//    private fun navigateToLoginPage() {
//        val intent = Intent(this, Login::class.java)
//        startActivity(intent)
//        finish()
//    }
    }





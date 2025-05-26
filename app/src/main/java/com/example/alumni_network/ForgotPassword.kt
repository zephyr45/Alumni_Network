package com.example.alumni_network

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {
    private lateinit var btnReset:Button
    private lateinit var forgot_email:EditText
    private lateinit var auth:FirebaseAuth
    private lateinit var msg_forget:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        msg_forget=findViewById(R.id.message_forget)
        btnReset=findViewById(R.id.forgot_btn)
        forgot_email=findViewById(R.id.forgot_email)
        auth=FirebaseAuth.getInstance()

        btnReset.setOnClickListener{
            resetPassword()
        }

        val back_to_login=findViewById<Button>(R.id.back_to_login)
        back_to_login.setOnClickListener{
            backToLogin()
        }

    }
    private fun backToLogin(){
        val intent= Intent(this@ForgotPassword,Login::class.java)
        startActivity(intent)
    }
    private fun resetPassword(){
        val resetEmail = forgot_email.text.toString().trim()
        auth.sendPasswordResetEmail(resetEmail).addOnSuccessListener {
            msg_forget.text="Check your Email"
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                backToLogin()
            }, 5000)
        }
            .addOnFailureListener{
                msg_forget.text=it.toString()
            }
    }
}
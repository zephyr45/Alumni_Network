package com.example.alumni_network.bottomfragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.alumni_network.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BottomfloatingbuttonView : BottomSheetDialogFragment() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore:FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth=FirebaseAuth.getInstance()
        firestore=FirebaseFirestore.getInstance()
    }

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?


    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bottomfloatingbutton, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Access a view using view.findViewById
        val joinNetwork = view.findViewById<LinearLayout>(R.id.joiningLayout)
        val joinNewNetwork=view.findViewById<TextView>(R.id.joinOption)
        val joinButton=view.findViewById<Button>(R.id.joinNetworkButton)

        joinNewNetwork.setOnClickListener{
            joinNetwork.visibility=View.VISIBLE
            joinNewNetwork.visibility=View.GONE

        }
       getNameOfUser()
        joinButton.setOnClickListener{
            dismiss()
            replaceFragment(NetworkJoin())
            activity?.title="Network"
        }


    }
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.navfragment, fragment)
            .addToBackStack(null)
            .commit()
    }
    private fun getNameOfUser() {
        val userName=view?.findViewById<TextView>(R.id.userName)
        val uid = mAuth.currentUser?.uid.toString()
        val userRef = firestore.collection("users").document(uid)
        userRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Get the fields
                val firstName = documentSnapshot.getString("firstName")?.toUpperCase()?: " "
                val lastName = documentSnapshot.getString("lastName")?.toUpperCase()?: " "
                userName?.text="${firstName} ${lastName}"

            } else {
                Log.d("FirestoreData", "No such document!")
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error getting document: ", exception)
        }
    }
}

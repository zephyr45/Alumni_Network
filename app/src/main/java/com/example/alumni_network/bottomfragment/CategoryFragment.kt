package com.example.alumni_network.bottomfragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alumni_network.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.alumni_network.NetworkLayout
import com.google.firebase.firestore.FieldValue

class CategoryFragment : Fragment(),LanguageAdapterForJoinedNetwork.OnItemClickListener {
    private lateinit var recyclerView: RecyclerView
    private var dataList = ArrayList<LanguageData>()
    private lateinit var adapter: LanguageAdapterForJoinedNetwork
    private lateinit var mAuth:FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth=FirebaseAuth.getInstance()
        firestore=FirebaseFirestore.getInstance()
        addDataToList()
    }

    private fun addDataToList() {
        val uid=mAuth.uid.toString()
        val userDocRef = firestore.collection("users").document(uid)
        val networksCollectionRef =firestore.collection("networks")
        dataList.clear()
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val networkArray = document.get("networks") as? List<String>
                    if(networkArray.isNullOrEmpty()){
                        val welcomeLayout=view?.findViewById<LinearLayout>(R.id.WelcomeLayout)
                        welcomeLayout?.visibility=View.VISIBLE


                    }
                    else if (!networkArray.isNullOrEmpty()) {
                        // Step 2: Loop through each network ID
                        for (networkId in networkArray) {
                            // Step 3: Look for the corresponding document in the networks collection
                            networksCollectionRef.document(networkId).get()
                                .addOnSuccessListener { networkDoc ->
                                    if (networkDoc.exists()) {
                                        // Step 4: Extract title and address, and add to the list
                                        val title = networkDoc.getString("title") ?: "Unknown Title"
                                        val address = networkDoc.getString("address") ?: "Unknown Address"
                                        val networkImage=networkDoc.getString("image").toString()
                                        dataList.add(LanguageData(networkId, title, networkImage, address))

                                        // Notify adapter of data change (if applicable)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FirestoreError", "Error fetching network details: ${e.message}")
                                }
                        }
                    } else {
                        Log.d("FirestoreInfo", "No networks found for the user.")
                    }
                } else {
                    Log.d("FirestoreInfo", "User document does not exist.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching user document: ${e.message}")
            }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_category, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId=mAuth.currentUser?.uid.toString()
        val userRef=firestore.collection("users").document(userId).get().addOnSuccessListener { document->
            val name=document.getString("firstName").orEmpty()
            val nameOfUser=view.findViewById<TextView>(R.id.nameofuser)
            nameOfUser.text="Hello ${name}!"
        }
        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recyclerviewforJoinedNetwork)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with static data
        adapter = LanguageAdapterForJoinedNetwork(dataList,this)
        recyclerView.adapter = adapter
    }
    override fun NetworkClick(position: Int) {
        val network = dataList[position]
        val intent=Intent(requireContext(),NetworkLayout::class.java)
        intent.putExtra("networktitle",network.title)
        intent.putExtra("networkAddress",network.address)
        intent.putExtra("networkId",network.networkId)
        intent.putExtra("networkLogo",network.logo)
        startActivity(intent)

    }

    override fun onMembershipClick(position: Int) {
        val network = dataList[position]
        Toast.makeText(context,"dhhf",Toast.LENGTH_SHORT).show()
        // Example: Handle membership action
    }

    override fun onClick2(position: Int) {
        val network = dataList[position]
        Toast.makeText(context,"dhhf",Toast.LENGTH_SHORT).show()
        // Handle click 2
    }

    override fun onClick3(position: Int) {
        val network = dataList[position]
        Toast.makeText(context,"dhhf",Toast.LENGTH_SHORT).show()
        // Handle click 3
    }

    override fun ondeleteClick(position: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Do you want to leave the network?")
        builder.setMessage("Your all data associated with the network will be deleted")
        builder.setPositiveButton("Yes") { dialogInterface, which ->
            val network = dataList[position]
            val uid = mAuth.uid.toString()
            val userRef = firestore.collection("users").document(uid)
            val networkUserRef = firestore.collection("networks")
                .document(network.networkId).collection("networkusers").document(uid)
            userRef.update("networks", FieldValue.arrayRemove(network.networkId))
                .addOnSuccessListener {
                    networkUserRef.delete()
                        .addOnSuccessListener {
                            dataList.removeAt(position)
                            adapter.notifyItemRemoved(position)
                            Toast.makeText(context, "Network deleted successfully", Toast.LENGTH_SHORT).show()
                            if (dataList.isEmpty()) {
                                val welcomeLayout = view?.findViewById<LinearLayout>(R.id.WelcomeLayout)
                                welcomeLayout?.visibility = View.VISIBLE
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error deleting network user: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error updating user's networks: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNeutralButton("No") { dialogInterface, which ->
            dialogInterface.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }

}

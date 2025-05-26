package com.example.alumni_network.bottomfragment

import android.media.RouteListingPreference
import android.media.RouteListingPreference.Item
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.credentials.webauthn.Cbor
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.example.alumni_network.R
import androidx.appcompat.widget.SearchView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale


class NetworkJoin : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView:SearchView
    private  var mList=ArrayList<LanguageData>()
    private lateinit var adapter:LanguageAdapter
    private lateinit var finalNewtorkJoinButton:Button
    private lateinit var firestore:FirebaseFirestore
    private lateinit var mAuth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore=FirebaseFirestore.getInstance()
        mAuth=FirebaseAuth.getInstance()
        addDataToList()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_network_join, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        finalNewtorkJoinButton=view.findViewById<Button>(R.id.finalNetworkJoinButton)
        recyclerView=view.findViewById(R.id.recyclerviewforsearch)
        searchView=view.findViewById(R.id.searchfornetwork)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager=LinearLayoutManager(context)
        //bundle for sending message to target frame
        val specificNetworkJoinFragment = SpecificNetworkJoin()
        val bundle = Bundle()

        adapter = LanguageAdapter(mList, object : LanguageAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val clickedItem = adapter.mList[position]
                //sending network address
                bundle.putString("selectedNetworkAddress",clickedItem.address)
                bundle.putString("selectedNetworkId",clickedItem.networkId)
                Toast.makeText(context, "Clicked: ${clickedItem.title}", Toast.LENGTH_SHORT).show()
                finalNewtorkJoinButton.visibility = View.VISIBLE
                searchView.setQuery(clickedItem.title, false)
            }
        })

        recyclerView.adapter=adapter
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
               return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    finalNewtorkJoinButton.visibility = View.GONE
                }
                filterList(newText)
                return true
            }


        })
        finalNewtorkJoinButton.setOnClickListener{
            //sending title to target fragment
            bundle.putString("selectedNetworkTitle", searchView.query.toString())
            specificNetworkJoinFragment.arguments = bundle
            replaceFragment(specificNetworkJoinFragment)
            activity?.title="Network"
        }

    }
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.navfragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun filterList(query: String?) {
        if (query != null) {
            val filteredList = ArrayList<LanguageData>()
            for (item in mList) {
                if (item.title.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT))) {
                    filteredList.add(item)
                }
            }

            if (filteredList.isEmpty()) {
                if (adapter.itemCount != 0) { // Check if the adapter already has 0 items to avoid repeated toasts
                    Toast.makeText(context, "No results found for \"$query\"", Toast.LENGTH_LONG).show()
                }
                finalNewtorkJoinButton.visibility = View.GONE
            }

            adapter.setFilteredList(filteredList) // Update the adapter's list
        }
    }




    private fun addDataToList() {
        val userId = mAuth.currentUser?.uid.toString()
        val userRef = firestore.collection("users").document(userId)
        val networksCollection = firestore.collection("networks")

        // Fetch the user's document to get the networks array
        userRef.get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    // Get the user's networks array (or empty list if not present)
                    val userNetworks = userDocument.get("networks") as? List<String> ?: emptyList()

                    // Fetch all networks
                    networksCollection.get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                val networkId = document.id

                                // Only add network details if the network ID is not in the user's networks array
                                if (!userNetworks.contains(networkId)) {
                                    val networkName = document.getString("title").toString().toUpperCase()
                                    val networkAddress = document.getString("address").toString()
                                    val networkLogo=document.getString("image").toString()
                                    mList.add(LanguageData(networkId, networkName, networkLogo, networkAddress))
                                    Log.d("FirestoreData", "Added Network ID: $networkId, Name: $networkName")
                                } else {
                                    Log.d("FirestoreData", "Skipped Network ID: $networkId, already in user's networks")
                                }
                            }
                            // Notify the adapter after updating the list
                            adapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { exception ->
                            Log.w("FirestoreError", "Error getting networks: ", exception)
                        }
                } else {
                    Log.w("FirestoreError", "User document does not exist.")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error getting user document: ", exception)
            }
    }


}
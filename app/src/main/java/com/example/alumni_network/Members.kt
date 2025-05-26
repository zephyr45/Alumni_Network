package com.example.alumni_network

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class Members : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private var mList = ArrayList<ForMember>()
    private lateinit var adapter: AdapterForMembers
    private lateinit var firestore: FirebaseFirestore
    private lateinit var networkId: String
    private lateinit var mAuth: FirebaseAuth
    private var selectedCriteria = "Name" // Default search criteria

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkId = arguments?.getString("networkId").toString()
        Toast.makeText(requireContext(), networkId, Toast.LENGTH_SHORT).show()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Members"

        val cardName: CardView = view.findViewById(R.id.cardName)
        val cardGraduationYear: CardView = view.findViewById(R.id.cardGraduationYear)
        val cardCompanyName: CardView = view.findViewById(R.id.cardCompanyName)
        val cardAssociateType: CardView = view.findViewById(R.id.cardAssociateType)
        val cardProfession:CardView=view.findViewById(R.id.cardProfession)

        // RecyclerView setup
        recyclerView = view.findViewById(R.id.recyclerForSearchingMember)
        searchView = view.findViewById(R.id.searchformember)
        searchView.queryHint = "Search by Name"
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Card click listeners
        updateSearchCriteria("Name", cardName, searchView)
        cardName.setOnClickListener {
            updateSearchCriteria("Name", cardName, searchView)
        }
        cardGraduationYear.setOnClickListener {
            updateSearchCriteria("Graduation Year", cardGraduationYear, searchView)
        }
        cardCompanyName.setOnClickListener {
            updateSearchCriteria("Company Name", cardCompanyName, searchView)
        }
        cardAssociateType.setOnClickListener {
            updateSearchCriteria("Associate Type", cardAssociateType, searchView)
        }
        cardProfession.setOnClickListener{
            updateSearchCriteria("Job Title", cardProfession, searchView)
        }

        // Initialize the adapter
        val gson = Gson()
        adapter = AdapterForMembers(mList, object : AdapterForMembers.OnItemClick {
            override fun onMemberClick(position: Int) {
                val clickedItem = adapter.filteredList[position]
                val json = gson.toJson(clickedItem)
                Toast.makeText(context, "Clicked on: ${clickedItem.name}", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), MemberProfile::class.java)
                intent.putExtra("member_data", json)
                startActivity(intent)
            }
        })
        recyclerView.adapter = adapter

        // Add initial data to the list
        addDataToList()

        // SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    private fun updateSearchCriteria(criteria: String, selectedCard: CardView, searchView: SearchView) {
        selectedCriteria = criteria
        searchView.queryHint = "Search by $criteria"

        // Reset all cards to gray and highlight selected card
        resetCardColors()
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bestgray))
    }

    private fun resetCardColors() {
        val grayColor = ContextCompat.getColor(requireContext(), R.color.lightgrey)
        view?.findViewById<CardView>(R.id.cardName)?.setCardBackgroundColor(grayColor)
        view?.findViewById<CardView>(R.id.cardGraduationYear)?.setCardBackgroundColor(grayColor)
        view?.findViewById<CardView>(R.id.cardCompanyName)?.setCardBackgroundColor(grayColor)
        view?.findViewById<CardView>(R.id.cardAssociateType)?.setCardBackgroundColor(grayColor)
        view?.findViewById<CardView>(R.id.cardProfession)?.setCardBackgroundColor(grayColor)
    }

    private fun filterList(query: String?) {
        val filteredList = ArrayList<ForMember>()
        if (!query.isNullOrEmpty()) {
            for (member in mList) {
                when (selectedCriteria) {
                    "Name" -> if (member.name.contains(query, ignoreCase = true)) filteredList.add(member)
                    "Graduation Year" -> if (member.graduationYear.contains(query, ignoreCase = true)) filteredList.add(member)
                    "Company Name" -> if (member.companyName.contains(query, ignoreCase = true)) filteredList.add(member)
                    "Associate Type" -> if (member.associateType.contains(query, ignoreCase = true)) filteredList.add(member)
                    "Job Title" -> if (member.jobTitle.contains(query, ignoreCase = true)) filteredList.add(member)
                }
            }
        } else {
            filteredList.addAll(mList)
        }
        adapter.updateFilteredList(filteredList)
    }

    private fun addDataToList() {
        val userRefInNetwork = firestore.collection("networks").document(networkId).collection("networkusers")
        val userRef = firestore.collection("users")

        userRefInNetwork.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userIds = documents.mapNotNull {
                        it.id.takeIf { id -> id != mAuth.currentUser?.uid }
                    }

                    val userTasks = userIds.map { userId ->
                        userRef.document(userId).get()
                    }

                    Tasks.whenAllSuccess<DocumentSnapshot>(userTasks)
                        .addOnSuccessListener { results ->
                            val tempList = ArrayList<ForMember>()
                            results.forEach { userDocument ->
                                val id = userDocument.id
                                val firstName = userDocument.getString("firstName").orEmpty().toUpperCase()
                                val lastName = userDocument.getString("lastName").orEmpty().toUpperCase()
                                val phoneNo = userDocument.getString("phoneNo").orEmpty()
                                val jobTitle = userDocument.getString("jobTitle").orEmpty().toUpperCase()
                                val companyName = userDocument.getString("companyName").orEmpty().toUpperCase()
                                val cityName = userDocument.getString("city").orEmpty().toUpperCase()
                                val imageUrl = userDocument.getString("imageUrl").orEmpty()
                                val currentlyWorking = userDocument.getString("currentWorking").toString()

                                val userDoc = documents.find { it.id == userDocument.id }
                                val graduationYear = userDoc?.getString("GraduationYear").orEmpty()
                                val associateType = userDoc?.getString("associateType").orEmpty()
                                val course = userDoc?.getString("course").orEmpty()

                                tempList.add(
                                    ForMember(
                                        id,
                                        imageUrl,
                                        "$firstName $lastName",
                                        graduationYear,
                                        phoneNo,
                                        currentlyWorking,
                                        jobTitle,
                                        companyName,
                                        associateType,
                                        course,
                                        cityName,
                                        networkId
                                    )
                                )
                            }
                            mList.clear()
                            mList.addAll(tempList)
                            adapter.updateFilteredList(mList)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error fetching user documents: ", exception)
                        }
                } else {
                    Log.d("Firestore", "No users found in networkusers collection.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching networkusers: ", exception)
            }
    }
}

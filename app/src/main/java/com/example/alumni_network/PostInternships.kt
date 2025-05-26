package com.example.alumni_network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.alumni_network.databinding.FragmentPostInternshipsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PostInternships : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private var _binding: FragmentPostInternshipsBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkId: String
    private lateinit var mAuth:FirebaseAuth
    private lateinit var userName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        mAuth=FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostInternshipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Post Internships"
        networkId = arguments?.getString("networkId").toString()
        binding.ButtonForInternship.setOnClickListener {
            if (validateFields()) {
                uploadInternshipDetails()
            }
        }
    }

    private fun validateFields(): Boolean {
        val companyName = binding.companyNameForInternship.text.toString().trim()
        val jobTitle = binding.JobtitleforInternship.text.toString().trim()
        val duration = binding.DurationForInternship.text.toString().trim()
        val stipend = binding.StipendforInternship.text.toString().trim()
        val location = binding.locationforInternships.text.toString().trim()
        val contactEmail = binding.ContactEmailforInternship.text.toString().trim()
        val description = binding.InternshipDescriptionForInternship.text.toString().trim()

        if (companyName.isEmpty()) {
            binding.companyNameForInternship.error = "Company name is required"
            return false
        }
        if (jobTitle.isEmpty()) {
            binding.JobtitleforInternship.error = "Job title is required"
            return false
        }
        if (duration.isEmpty()) {
            binding.DurationForInternship.error = "Duration is required"
            return false
        }
        if (stipend.isEmpty()) {
            binding.StipendforInternship.error = "Expected stipend is required"
            return false
        }
        if (location.isEmpty()) {
            binding.locationforInternships.error = "Location is required"
            return false
        }
        if (contactEmail.isEmpty()) {
            binding.ContactEmailforInternship.error = "Contact email is required"
            return false
        }
        if (description.isEmpty()) {
            binding.InternshipDescriptionForInternship.error = "Internship description is required"
            return false
        }
        return true
    }

    private fun uploadInternshipDetails() {
        val userId = mAuth.currentUser?.uid.toString()

        // Fetch user's first and last name
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName").orEmpty()
                val lastName = document.getString("lastName").orEmpty()
                userName = "$firstName $lastName"

                // Proceed to post the internship after fetching the username
                val internshipDetails = mapOf(
                    "companyName" to binding.companyNameForInternship.text.toString().trim(),
                    "jobTitle" to binding.JobtitleforInternship.text.toString().trim(),
                    "duration" to binding.DurationForInternship.text.toString().trim(),
                    "stipend" to binding.StipendforInternship.text.toString().trim(),
                    "location" to binding.locationforInternships.text.toString().trim(),
                    "applyLink" to binding.ContactEmailforInternship.text.toString().trim(),
                    "description" to binding.InternshipDescriptionForInternship.text.toString().trim(),
                    "senderName" to userName, // Use the fetched username
                    "likes" to emptyList<String>(),
                    "comments" to emptyList<Map<String, String>>(),
                    "timeStamp" to FieldValue.serverTimestamp(),
                    "senderId" to userId,
                )

                firestore.collection("networks")
                    .document(networkId)
                    .collection("internships")
                    .add(internshipDetails)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Internship posted successfully!", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch user details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun clearFields() {
        binding.companyNameForInternship.text?.clear()
        binding.JobtitleforInternship.text?.clear()
        binding.DurationForInternship.text?.clear()
        binding.StipendforInternship.text?.clear()
        binding.locationforInternships.text?.clear()
        binding.ContactEmailforInternship.text?.clear()
        binding.InternshipDescriptionForInternship.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

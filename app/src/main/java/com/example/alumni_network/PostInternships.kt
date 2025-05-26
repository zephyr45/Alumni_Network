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
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()
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
        
        // Safely get networkId from arguments with a default value
        networkId = arguments?.getString("networkId") ?: ""
        if (networkId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Network ID not found", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

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
        val userId = mAuth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        binding.ButtonForInternship.isEnabled = false
        binding.ButtonForInternship.text = "Posting..."

        // Create internship details with default username if user details fetch fails
        fun postInternshipWithDetails(posterName: String) {
            val internshipDetails = hashMapOf(
                "companyName" to binding.companyNameForInternship.text.toString().trim(),
                "jobTitle" to binding.JobtitleforInternship.text.toString().trim(),
                "duration" to binding.DurationForInternship.text.toString().trim(),
                "stipend" to binding.StipendforInternship.text.toString().trim(),
                "location" to binding.locationforInternships.text.toString().trim(),
                "applyLink" to binding.ContactEmailforInternship.text.toString().trim(),
                "description" to binding.InternshipDescriptionForInternship.text.toString().trim(),
                "senderName" to posterName,
                "senderId" to userId,
                "timeStamp" to FieldValue.serverTimestamp(),
                "likes" to emptyList<String>(),
                "noOfLikes" to 0
            )

            firestore.collection("networks")
                .document(networkId)
                .collection("internships")
                .add(internshipDetails)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Internship posted successfully!", Toast.LENGTH_SHORT).show()
                    clearFields()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    binding.ButtonForInternship.isEnabled = true
                    binding.ButtonForInternship.text = "Post Internship"
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Try to fetch user details, but proceed with posting even if it fails
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val userName = if (firstName.isEmpty() && lastName.isEmpty()) {
                    "Anonymous User"
                } else {
                    "$firstName $lastName".trim()
                }
                postInternshipWithDetails(userName)
            }
            .addOnFailureListener {
                // If we can't get the user's name, post with a default name
                postInternshipWithDetails("Anonymous User")
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

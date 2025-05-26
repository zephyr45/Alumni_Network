package com.example.alumni_network

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alumni_network.databinding.FragmentPostInternshipsBinding
import com.example.alumni_network.databinding.FragmentPostJobsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class PostJobs : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private var _binding: FragmentPostJobsBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkId: String
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userName:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        mAuth=FirebaseAuth.getInstance()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPostJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Post Jobs"
        networkId = arguments?.getString("networkId").toString()
        binding.ButtonForJob.setOnClickListener {
            if (validateFields()) {
                uploadInternshipDetails()
            }
        }
    }
    private fun validateFields(): Boolean {
        val companyName = binding.companyNameForJob.text.toString().trim()
        val jobTitle = binding.JobtitleforJob.text.toString().trim()
        val minimumExperience = binding.MinimumExperienceForJob.text.toString().trim()
        val salary = binding.SalaryforJob.text.toString().trim()
        val location = binding.locationforJob.text.toString().trim()
        val contactEmail = binding.ContactEmailforJob.text.toString().trim()
        val description = binding.JobDescriptionforJob.text.toString().trim()

        if (companyName.isEmpty()) {
            binding.companyNameForJob.error = "Company name is required"
            return false
        }
        if (jobTitle.isEmpty()) {
            binding.JobtitleforJob.error = "Job title is required"
            return false
        }
        if (minimumExperience.isEmpty()) {
            binding.MinimumExperienceForJob.error = "MinimumExperience is required"
            return false
        }
        if (salary.isEmpty()) {
            binding.SalaryforJob.error = "Expected Salary is required"
            return false
        }
        if (location.isEmpty()) {
            binding.locationforJob.error = "Location is required"
            return false
        }
        if (contactEmail.isEmpty()) {
            binding.ContactEmailforJob.error = "Contact email is required"
            return false
        }
        if (description.isEmpty()) {
            binding.JobDescriptionforJob.error = "description is required"
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
                    "companyName" to binding.companyNameForJob.text.toString().trim(),
                    "jobTitle" to binding.JobtitleforJob.text.toString().trim(),
                    "minimumExperience" to binding.MinimumExperienceForJob.text.toString().trim(),
                    "salary" to binding.SalaryforJob.text.toString().trim(),
                    "location" to binding.locationforJob.text.toString().trim(),
                    "applyLink" to binding.ContactEmailforJob.text.toString().trim(),
                    "description" to binding.JobDescriptionforJob.text.toString().trim(),
                    "senderName" to userName, // Use the fetched username
                    "likes" to emptyList<String>(),
                    "comments" to emptyList<Map<String, String>>(),
                    "timeStamp" to FieldValue.serverTimestamp(),
                    "senderId" to userId,
                )

                firestore.collection("networks")
                    .document(networkId)
                    .collection("jobs")
                    .add(internshipDetails)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Job posted successfully!", Toast.LENGTH_SHORT).show()
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
        binding.companyNameForJob.text?.clear()
        binding.JobtitleforJob.text?.clear()
        binding.MinimumExperienceForJob.text?.clear()
        binding.SalaryforJob.text?.clear()
        binding.locationforJob.text?.clear()
        binding.ContactEmailforJob.text?.clear()
        binding.JobDescriptionforJob.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
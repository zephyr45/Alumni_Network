package com.example.alumni_network

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.alumni_network.databinding.ActivityProfilepageBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class profilepage : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var binding: ActivityProfilepageBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up ViewBinding
        binding = ActivityProfilepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Setup Dropdown Menus
        dropDownForCountyCode()
        cityDropDown()

        // Get UID from Intent
        val uidmessage = intent.getStringExtra("uid")
        val emailmessage=intent.getStringExtra("email")
        val profilemessage=findViewById<TextView>(R.id.loginidwillbe)
        profilemessage.setTextColor(ContextCompat.getColor(this, R.color.lightblueColor))
        profilemessage.text = "${profilemessage.text} $emailmessage"

        if (uidmessage == null) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        // RadioGroup listener for working details visibility
        binding.radiogroup.setOnCheckedChangeListener { _, checkedId ->
            binding.Workingdetails.visibility = if (checkedId == R.id.radio_yes) View.VISIBLE else View.GONE
        }

        // Set up Submit Button
        binding.profileSubmitButton.setOnClickListener {
            submitProfile(uidmessage)
        }
    }

    private fun submitProfile(uid: String) {
        // Initialize isvalid flag to true
        var isValid = true

        // Clear any previous error messages
        binding.firstname.error = null
        binding.lastname.error = null
        binding.countryCodeDropdown.error = null
        binding.mobileNumber.error = null
        binding.jobTitle.error = null
        binding.companyName.error = null
        binding.cityDrop.error = null
        binding.Industry.error = null

        // Clear previous color changes for borders and hint text
        clearErrorStyles()

        // Retrieve data from input fields
        val firstName = binding.firstname.text.toString().trim()
        if (firstName.isEmpty()) {
            binding.firstname.error = "First Name is required"
            applyErrorStyles(binding.firstnameLayout)
            isValid = false
        }

        val lastName = binding.lastname.text.toString().trim()
        if (lastName.isEmpty()) {
            binding.lastname.error = "Last Name is required"
            applyErrorStyles(binding.lastnameLayout)
            isValid = false
        }

        val countryCode = binding.countryCodeDropdown.text.toString()
        if (countryCode.isEmpty()) {
            binding.countryCodeDropdown.error = "Country Code is required"
            applyErrorStyles(binding.countryCodeLayout)
            isValid = false
        }

        val phoneNo = binding.mobileNumber.text.toString().trim()
        if (phoneNo.isEmpty()) {
            binding.mobileNumber.error = "Phone no. is required"
            applyErrorStyles(binding.mobileNumberLayout)
            isValid = false
        }

        var jobTitle = binding.jobTitle.text.toString().trim()
        var companyName = binding.companyName.text.toString().trim()

        // Check if the user is working (radio button is "Yes")
        val isWorking = binding.radiogroup.checkedRadioButtonId == R.id.radio_yes
        val currentlyWorkingID=binding.radiogroup.checkedRadioButtonId
        val currentWorking=findViewById<RadioButton>(currentlyWorkingID)
        val currentlyWorkingtext=currentWorking.text.toString()
        // If the user is working, validate the job title and company name
        if (isWorking) {
            if (jobTitle.isEmpty()) {
                binding.jobTitle.error = "Job title is required"
                applyErrorStyles(binding.jobTitleLayout)
                isValid = false
            }
            if (companyName.isEmpty()) {
                binding.companyName.error = "Company Name is required"
                applyErrorStyles(binding.companyNameLayout)
                isValid = false
            }
        }else{
            jobTitle=""
            companyName=""

        }

        val cityName = binding.cityDrop.text.toString()
        if (cityName.isEmpty()) {
            binding.cityDrop.error = "City is required"
            applyErrorStyles(binding.cityDropLayout)
            isValid = false
        }

        val industry = binding.Industry.text.toString().trim()
        if (industry.isEmpty()) {
            binding.Industry.error = "Industry is required"
            applyErrorStyles(binding.industryLayout)
            isValid = false
        }

        // If all fields are valid, create a User object and save it to Firestore
        if (isValid) {
            val user = User(firstName, lastName, "$countryCode$phoneNo", currentlyWorkingtext, jobTitle, companyName, cityName, industry)

            // Save user to Firestore
            firestore.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Home::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Function to apply error styles to the TextInputLayouts (red border and hint)
    // Function to apply error styles (turn stroke and hint text red)
    private fun applyErrorStyles(layout: TextInputLayout) {
        layout.boxStrokeColor = ContextCompat.getColor(this, R.color.red)
        layout.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red)))
    }

    // Function to clear any previous error styles (restores original colors)
    private fun clearErrorStyles() {
        val defaultColor = ContextCompat.getColor(this, R.color.black)

        // Apply default colors for all TextInputLayouts (stroke color)
        binding.firstnameLayout.boxStrokeColor = defaultColor
        binding.lastnameLayout.boxStrokeColor = defaultColor
        binding.countryCodeLayout.boxStrokeColor = defaultColor
        binding.mobileNumberLayout.boxStrokeColor = defaultColor
        binding.jobTitleLayout.boxStrokeColor = defaultColor
        binding.companyNameLayout.boxStrokeColor = defaultColor
        binding.cityDropLayout.boxStrokeColor = defaultColor
        binding.industryLayout.boxStrokeColor = defaultColor

        // Restore default hint colors using ColorStateList
        binding.firstnameLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
        binding.lastnameLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
        binding.countryCodeLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
        binding.mobileNumberLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
        binding.jobTitleLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
        binding.companyNameLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
        binding.cityDropLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
        binding.industryLayout.setHintTextColor(ColorStateList.valueOf(defaultColor))
    }




    private fun cityDropDown() {
        val cityList = arrayOf("Visakhapatnam", "Vijayawada", "Guntur", "Tirupati", "Kakinada","Itanagar", "Tawang", "Pasighat", "Naharlagun",
            "Guwahati", "Dibrugarh", "Jorhat", "Silchar","Patna", "Gaya", "Bhagalpur", "Muzaffarpur","Raipur", "Bilaspur", "Durg-Bhilai", "Korba",
            "Panaji", "Margao", "Vasco da Gama", "Mapusa","Ahmedabad", "Surat", "Vadodara", "Rajkot","Gurgaon", "Faridabad", "Panipat", "Ambala",
            "Shimla", "Manali", "Dharamshala", "Mandi","Ranchi", "Jamshedpur", "Dhanbad", "Bokaro","Bengaluru", "Mysuru", "Mangaluru", "Hubballi-Dharwad",
            "Thiruvananthapuram", "Kochi", "Kozhikode", "Kollam","Bhopal", "Indore", "Gwalior", "Jabalpur","Mumbai", "Pune", "Nagpur", "Nashik",
            "Imphal", "Thoubal", "Churachandpur", "Ukhrul","Shillong", "Tura", "Nongstoin", "Jowai","Aizawl", "Lunglei", "Champhai", "Serchhip",
            "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cityList)
        binding.cityDrop.setAdapter(adapter)
    }

    private fun dropDownForCountyCode() {
        val countryCodes = arrayOf("+1", "+91", "+61")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countryCodes)
        binding.countryCodeDropdown.setAdapter(adapter)

        // Show dropdown on click or focus
        binding.countryCodeDropdown.setKeyListener(null)
        binding.countryCodeDropdown.setOnClickListener { binding.countryCodeDropdown.showDropDown() }
        binding.countryCodeDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.countryCodeDropdown.showDropDown()
        }
    }
}

package com.example.alumni_network.bottomfragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.example.alumni_network.R
import com.example.alumni_network.navfragment.Homefragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


class SpecificNetworkJoin : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var mAuth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore=FirebaseFirestore.getInstance()
        mAuth=FirebaseAuth.getInstance()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_specific_network_join, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val associateLayout=view.findViewById<LinearLayout>(R.id.associateLayout)
        val courseLayout=view.findViewById<LinearLayout>(R.id.CourseLayout)
        val collegeTitle=view.findViewById<TextView>(R.id.AlumniTitle)
        val collegeAdress=view.findViewById<TextView>(R.id.AlumniAddress)
        val alumniSelection=view.findViewById<TextView>(R.id.AlumniSelect)
        val studentSelection=view.findViewById<TextView>(R.id.StudentSelect)
        val facultySelection=view.findViewById<TextView>(R.id.FacultySelect)
        var graduationYearSelection=view.findViewById<Spinner>(R.id.GraduationYearSelection)
        val courseSelection=view.findViewById<AutoCompleteTextView>(R.id.courseSelection)
        val submitButton=view.findViewById<Button>(R.id.sumbitButton)
        val submitLayout=view.findViewById<LinearLayout>(R.id.SubmitLayout)
        val selectedNetworkTitle = arguments?.getString("selectedNetworkTitle")
        val selectedNetworkAddress=arguments?.getString("selectedNetworkAddress")
        val selectedNetworkId=arguments?.getString("selectedNetworkId").toString()
        collegeTitle.text=selectedNetworkTitle.toString()
        collegeAdress.text=selectedNetworkAddress.toString()
        var associateSelectedType:String="Error"
        var selectedYear:String="Error"
        var temp:Boolean=false
        var temp1:Boolean=false






        //association choosing and clicking
        val clickListener = View.OnClickListener { clickedView ->
            when (clickedView.id) {
                R.id.AlumniSelect -> {
                    associateSelectedType="Alumni"
                    Toast.makeText(context, "Alumni selected", Toast.LENGTH_SHORT).show()
                }
                R.id.StudentSelect -> {
                    associateSelectedType="Student"
                    Toast.makeText(context, "Student selected", Toast.LENGTH_SHORT).show()
                }
                R.id.FacultySelect -> {
                    associateSelectedType="Faculty"
                    Toast.makeText(context, "Faculty selected", Toast.LENGTH_SHORT).show()
                }
            }
            courseLayout.visibility=View.VISIBLE
            associateLayout.visibility=View.GONE
            submitLayout.visibility=View.VISIBLE

        }
        alumniSelection.setOnClickListener(clickListener)
        studentSelection.setOnClickListener(clickListener)
        facultySelection.setOnClickListener(clickListener)







        //setting up autocomplete listener

        // Generate years dynamically

        val arrayOfCourse= arrayOf("Bachelor of Arts (BA)", "Bachelor of Science (BSc)",
            "Bachelor of Commerce (BCom)", "Bachelor of Technology (BTech)",
            "Bachelor of Engineering (BE)", "Bachelor of Business Administration (BBA)",
            "Bachelor of Computer Applications (BCA)",
            "Bachelor of Fine Arts (BFA)", "Bachelor of Architecture (BArch)",
            "Bachelor of Medicine, Bachelor of Surgery (MBBS)", "Bachelor of Pharmacy (BPharm)",
            "Bachelor of Dental Surgery (BDS)", "Bachelor of Law (LLB)",
            "Bachelor of Education (BEd)", "Bachelor of Design (BDes)",
            "Bachelor of Veterinary Science (BVSc)", "Bachelor of Science in Nursing (BSc Nursing)",
            "Bachelor of Hotel Management (BHM)", "Bachelor of Social Work (BSW)",
            "Bachelor of Physiotherapy (BPT)", "Bachelor of Journalism and Mass Communication (BJMC)")
        val arrayAdapter=ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item,arrayOfCourse)
        courseSelection.setAdapter(arrayAdapter)
        courseSelection.setOnItemClickListener { parent, _, position, _ ->
            val selectedCourse = parent.getItemAtPosition(position).toString()
            Toast.makeText(requireContext(), "Selected: $selectedCourse", Toast.LENGTH_SHORT).show()
            temp=true
            if(temp==true && temp1==true){
                submitButton.isEnabled=true
                submitButton.isClickable=true
            }

        }

        courseSelection.threshold=0
        val startYear = 2010
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (startYear..currentYear + 5).toList().map { it.toString() }

        val arrayAdapterForYear = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            years
        )
        graduationYearSelection.adapter=arrayAdapterForYear

        graduationYearSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = years[position]
                Toast.makeText(requireContext(), "Selected: $selectedYear", Toast.LENGTH_SHORT).show()

                // Your existing logic
                temp1 = true
                if (temp == true && temp1 == true) {
                    submitButton.isEnabled = true
                    submitButton.isClickable = true
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where no item is selected
            }
            }






        //submitting and doing all firstore operation

        submitButton.setOnClickListener {
            val uid = mAuth.uid.toString()
            val userDocRef = firestore.collection("users").document(uid)
            val networkDocRef = firestore.collection("networks").document(selectedNetworkId)
                .collection("networkusers").document(uid)

            val courseSelected = courseSelection.text.toString()
            val dataToSave = mapOf(
                "course" to courseSelected,
                "associateType" to associateSelectedType,
                "GraduationYear" to selectedYear,
                "joinedAt" to FieldValue.serverTimestamp()
            )

            // Track success of operations
            var userNetworkUpdateSuccess = false
            var networkUserDetailsAdded = false

            // Update user's networks array
            userDocRef.update("networks", FieldValue.arrayUnion(selectedNetworkId))
                .addOnSuccessListener {
                    userNetworkUpdateSuccess = true
                    // Check if all operations are successful
                    if (userNetworkUpdateSuccess && networkUserDetailsAdded) {
                        Toast.makeText(context, "Successfully joined and user details added.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update user networks: ${e.message}", Toast.LENGTH_SHORT).show()
                    replaceFragment(Homefragment())
                }

            // Add network user details
            networkDocRef.set(dataToSave)
                .addOnSuccessListener {
                    networkUserDetailsAdded = true
                    // Check if all operations are successful
                    if (userNetworkUpdateSuccess && networkUserDetailsAdded) {
                        Toast.makeText(context, "Successfully joined and user details added.", Toast.LENGTH_SHORT).show()
                        replaceFragment(Homefragment())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add network user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }



    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        parentFragmentManager.beginTransaction()
            .replace(R.id.navfragment, fragment)
            .commit()



    }
}
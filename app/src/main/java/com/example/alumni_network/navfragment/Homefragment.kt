package com.example.alumni_network.navfragment


import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.alumni_network.R
import com.example.alumni_network.bottomfragment.BottomfloatingbuttonView
import com.example.alumni_network.bottomfragment.CategoryFragment
import com.example.alumni_network.bottomfragment.ChatsFragment
import com.example.alumni_network.bottomfragment.HistoryFragment
import com.example.alumni_network.bottomfragment.NotificationFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Homefragment : Fragment() {

    private var currentFragment: Fragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_homefragment, container, false)
        val bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Bottom Navigation Item Selection Listener
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bottom_category -> {
                    updateFragment(CategoryFragment(), "My Network")
                }
                R.id.bottom_notification -> {
                    updateFragment(NotificationFragment(), "Notification")
                }
                R.id.bottom_history -> {
                    updateFragment(HistoryFragment(), "History")
                }
                R.id.bottom_profile -> {
                    updateFragment(ChatsFragment(), "Chats")
                }
            }
            true // Returning true after handling each item
        }

        // Ensure the default fragment and title are set when the fragment is created
        if (savedInstanceState == null) {
            updateFragment(CategoryFragment(), "My Network")
            bottomNavigationView.selectedItemId = R.id.bottom_category
        }

        // Configure Floating Action Button
        val fabButton: FloatingActionButton = view.findViewById(R.id.addFabButton)
        fabButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.darkgreyColor))
        fabButton.setOnClickListener {
            val bottomSheetFragment = BottomfloatingbuttonView()
            bottomSheetFragment.show(requireActivity().supportFragmentManager, bottomSheetFragment.tag)
        }

        // Configure BottomNavigationView Appearance
        bottomNavigationView.itemBackground = ContextCompat.getDrawable(requireContext(), android.R.color.transparent)
        bottomNavigationView.setBackgroundColor(Color.TRANSPARENT)
        bottomNavigationView.itemRippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomNavigationView.itemIconTintList = ContextCompat.getColorStateList(requireContext(), R.color.icontextcolorstate)
        bottomNavigationView.itemTextColor = ContextCompat.getColorStateList(requireContext(), R.color.icontextcolorstate)

        return view
    }

    private fun updateFragment(fragment: Fragment, title: String) {
        // Check if the fragment is already displayed
        if (currentFragment != fragment) {
            currentFragment = fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.bottomFragment, fragment)
                .commit()

            // Update the title of the activity
            activity?.let {
                if (it is HomeFragmentListener) {
                    it.updateTitle(title)
                }
            }
        }
    }

    // Listener Interface to communicate with Activity
    interface HomeFragmentListener {
        fun updateTitle(title: String)
    }
}

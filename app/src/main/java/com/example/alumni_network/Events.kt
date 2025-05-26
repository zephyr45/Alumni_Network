package com.example.alumni_network

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class Events : Fragment(),SwipeRefreshLayout.OnRefreshListener {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var txt:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout=view.findViewById(R.id.refreshForEvents)
        txt=view.findViewById(R.id.txtForEvents)
        swipeRefreshLayout.setOnRefreshListener(this)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Events"
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Update the text and stop the refresh indicator
            txt.text = "Refreshed"
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(requireContext(), "Content refreshed!", Toast.LENGTH_SHORT).show()
        }, 2000)

    }


}
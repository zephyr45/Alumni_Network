package com.example.alumni_network

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CircleOptions
import okhttp3.*
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.io.IOException

class NearmeUsers : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var mapLibreMap: MapLibreMap
    private val db = FirebaseFirestore.getInstance()
    private lateinit var networkId: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mAuth: FirebaseAuth

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext())
        mAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nearme_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        networkId = arguments?.getString("networkId").toString()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        getUserLocation()

        (activity as? AppCompatActivity)?.supportActionBar?.title = "Near Me"
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map

        // Use OpenStreetMap tile service
        mapLibreMap.setStyle("https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json") {
            fetchUsersLocations()
        }

    }


    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                saveUserLocation(latitude, longitude)
            }
        }
    }

    private fun saveUserLocation(latitude: Double, longitude: Double) {
        val userId = mAuth.currentUser?.uid
        val userLocation = mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )

        userId?.let {
            db.collection("users")
                .document(it)
                .set(userLocation, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("LocationSave", "User location saved successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("LocationSave", "Error saving location", e)
                }
        }
    }

    private fun fetchUsersLocations() {
        db.collection("networks")
            .document(networkId)
            .collection("networkusers")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val userId = document.id
                    fetchUserLocation(userId)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error fetching user locations", exception)
            }
    }

    private fun fetchUserLocation(userId: String) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val latitude = document.getDouble("latitude")
                val longitude = document.getDouble("longitude")
                val userName = document.getString("firstName") // Assuming "name" field exists in Firestore
                if (latitude != null && longitude != null && userName != null) {
                    addMarkerToMap(latitude, longitude, userId, userName)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error fetching location for user: $userId", exception)
            }
    }


    private fun addMarkerToMap(latitude: Double, longitude: Double, userId: String, userName: String) {
        val position = LatLng(latitude, longitude)
        val currentUserId = mAuth.currentUser?.uid
        val markerTitle = if (userId == currentUserId) "You" else userName

        mapLibreMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(markerTitle)
        )
        if (userId == currentUserId) {
            addCircleToMap(latitude, longitude, 20000.0, userId) // Unique circle per user
        }

        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 10.0))
    }



    private fun addCircleToMap(latitude: Double, longitude: Double, radius: Double, userId: String) {
        val sourceId = "circle-source-$userId"
        val layerId = "circle-layer-$userId"
        val geoJsonCircle = createGeoJsonCircle(latitude, longitude, radius)

        mapLibreMap.getStyle { style ->
            if (style.getSource(sourceId) == null) {
                style.addSource(GeoJsonSource(sourceId, geoJsonCircle))
            }

            if (style.getLayer(layerId) == null) {
                val fillLayer = FillLayer(layerId, sourceId)
                fillLayer.setProperties(
                    PropertyFactory.fillColor("#55FF0000"),
                    PropertyFactory.fillOpacity(0.4f)
                )
                style.addLayer(fillLayer)
            }
        }
    }

    private fun createGeoJsonCircle(latitude: Double, longitude: Double, radius: Double): String {
        val points = mutableListOf<List<Double>>()
        val earthRadius = 6378137.0 // Earth's radius in meters
        val latRadians = Math.toRadians(latitude)

        // Generate points for the circle
        val numberOfPoints = 64 // More points = smoother circle
        for (i in 0 until numberOfPoints) {
            val angle = Math.PI * 2 * i / numberOfPoints
            val dx = radius * Math.cos(angle) / earthRadius
            val dy = radius * Math.sin(angle) / earthRadius
            val newLat = latitude + Math.toDegrees(dy)
            val newLng = longitude + Math.toDegrees(dx / Math.cos(latRadians))
            points.add(listOf(newLng, newLat))
        }

        // Close the circle
        points.add(points[0])

        // Return GeoJSON as a String
        return """
        {
          "type": "Feature",
          "geometry": {
            "type": "Polygon",
            "coordinates": [${points}]
          }
        }
    """.trimIndent()
    }


    // Handle MapView lifecycle
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

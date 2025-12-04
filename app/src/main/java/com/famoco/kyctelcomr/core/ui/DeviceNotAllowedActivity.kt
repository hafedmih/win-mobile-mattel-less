package com.famoco.kyctelcomr.core.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.mattel.model.DeviceLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.DecimalFormat
import java.util.*

class DeviceNotAllowedActivity : AppCompatActivity() {

    companion object {
        private const val UI_STATE_KEY = "UI_STATE"
        private const val STATE_DEFAULT = 0
        private const val STATE_AUTH = 1
        private const val STATE_EDIT = 2
    }

    private lateinit var map: MapView
    private lateinit var distanceInfoTextView: TextView
    private lateinit var currentLocationTextView: TextView
    private lateinit var allowedLocationTextView: TextView
    private lateinit var authorizedLocationCard: CardView
    private lateinit var authLayout: LinearLayout
    private lateinit var editLocationLayout: LinearLayout
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordButton: Button
    private lateinit var coordinatesEditText: EditText
    private lateinit var updateLocationButton: Button

    private var deviceId: String? = null
    private var deviceFranchise: String? = null
    private var deviceMsisdn: Long = 0L
    private var currentPoint: GeoPoint? = null
    private var allowedPoint: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_device_not_allowed)

        initializeViews()
        processIntentData()
        setupListeners()

        if (savedInstanceState != null) {
            restoreUiState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentState = when {
            authLayout.visibility == View.VISIBLE -> STATE_AUTH
            editLocationLayout.visibility == View.VISIBLE -> STATE_EDIT
            else -> STATE_DEFAULT
        }
        outState.putInt(UI_STATE_KEY, currentState)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun initializeViews() {
        map = findViewById(R.id.map)
        distanceInfoTextView = findViewById(R.id.distanceInfoTextView)
        currentLocationTextView = findViewById(R.id.currentLocationTextView)
        allowedLocationTextView = findViewById(R.id.allowedLocationTextView)
        authorizedLocationCard = findViewById(R.id.authorizedLocationCard)
        authLayout = findViewById(R.id.authLayout)
        editLocationLayout = findViewById(R.id.editLocationLayout)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordButton = findViewById(R.id.confirmPasswordButton)
        coordinatesEditText = findViewById(R.id.coordinatesEditText)
        updateLocationButton = findViewById(R.id.updateLocationButton)
        map.setMultiTouchControls(true)
    }

    private fun processIntentData() {
        val intent = intent ?: return

        val currentLat = intent.getDoubleExtra("current_lat", 0.0)
        val currentLng = intent.getDoubleExtra("current_lng", 0.0)
        val allowedLat = intent.getDoubleExtra("allowed_lat", 0.0)
        val allowedLng = intent.getDoubleExtra("allowed_lng", 0.0)
        val distance = intent.getStringExtra("distance")

        currentPoint = GeoPoint(currentLat, currentLng)
        allowedPoint = GeoPoint(allowedLat, allowedLng)

        deviceId = intent.getStringExtra("device_id")
        deviceFranchise = intent.getStringExtra("device_franchise")
        deviceMsisdn = intent.getLongExtra("device_msisdn", 0L)

        val df = DecimalFormat("#,###")
        val distanceInt = distance!!.toDoubleOrNull()?.toInt() ?: 0
        distanceInfoTextView.text = "Vous êtes en dehors de la zone autorisée de $distanceInt mètres"
        currentLocationTextView.text = String.format(Locale.US, "Lat: %.6f, Lng: %.6f", currentLat, currentLng)
        allowedLocationTextView.text = String.format(Locale.US, "Lat: %.6f, Lng: %.6f", allowedLat, allowedLng)

        updateMapAndZoom()
    }

    private fun setupListeners() {
//        authorizedLocationCard.setOnClickListener {
//            if (authLayout.visibility == View.GONE && editLocationLayout.visibility == View.GONE) {
//                showAuthUI()
//            }
//        }

        confirmPasswordButton.setOnClickListener {
            if (passwordEditText.text.toString() == "2520") {
                showEditUI()
            } else {
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
            }
        }

        updateLocationButton.setOnClickListener {
            val coordinatesText = coordinatesEditText.text.toString()
            val parts = coordinatesText.split(",")

            if (parts.size == 2) {
                val newLat = parts[0].trim().toDoubleOrNull()
                val newLng = parts[1].trim().toDoubleOrNull()

                if (newLat != null && newLng != null) {
                    saveNewLocationToDatabase(newLat, newLng)
                } else {
                    Toast.makeText(this, "Invalid coordinates format.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid input. Use 'latitude,longitude' format.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreUiState(savedInstanceState: Bundle) {
        when (savedInstanceState.getInt(UI_STATE_KEY)) {
            STATE_AUTH -> showAuthUI()
            STATE_EDIT -> showEditUI()
        }
    }

    private fun showAuthUI() {
        allowedLocationTextView.visibility = View.GONE
        authLayout.visibility = View.VISIBLE
        editLocationLayout.visibility = View.GONE
        passwordEditText.setText("")
        passwordEditText.requestFocus()
    }

    private fun showEditUI() {
        val currentLoc = currentPoint ?: return
        authLayout.visibility = View.GONE
        editLocationLayout.visibility = View.VISIBLE
        coordinatesEditText.setText(String.format(Locale.US, "%.6f,%.6f", currentLoc.latitude, currentLoc.longitude))
        coordinatesEditText.requestFocus()
        coordinatesEditText.selectAll()
    }

    private fun resetToDisplayUI(updatedLocation: DeviceLocation) {
        val newAllowedLat = updatedLocation.Latitude.replace(",", ".").toDouble()
        val newAllowedLng = updatedLocation.Longtitude.replace(",", ".").toDouble()
        allowedPoint = GeoPoint(newAllowedLat, newAllowedLng)
        allowedLocationTextView.text = String.format(Locale.US, "Lat: %.6f, Lng: %.6f", newAllowedLat, newAllowedLng)
        updateMapAndZoom()
    }

    private fun saveNewLocationToDatabase(newLat: Double, newLng: Double) {
        if (deviceId == null) {
            Toast.makeText(this, "Error: Device ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedLocation = DeviceLocation(
            Device = deviceId!!,
            Franchise = deviceFranchise ?: "",
            MSISDN = deviceMsisdn,
            Latitude = newLat.toString().replace(".", ","),
            Longtitude = newLng.toString().replace(".", ",")
        )

        GlobalScope.launch(Dispatchers.IO) {
            // Uncomment this line when database is available:
         //   KycDatabase.getInstance(applicationContext).getDao().updateDeviceLocation(updatedLocation)
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Location Updated Successfully", Toast.LENGTH_LONG).show()
                // Start MainActivity and finish the current activity
                val intent = Intent(this@DeviceNotAllowedActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun updateMapAndZoom() {
        val current = currentPoint ?: return
        val allowed = allowedPoint ?: return

        map.overlays.clear()
        addMarker(current, "Your Location")
        addMarker(allowed, "Allowed Zone")

        val points = arrayListOf(current, allowed)
        val boundingBox = BoundingBox.fromGeoPoints(points)

        map.post {
            map.zoomToBoundingBox(boundingBox, true, 150)
        }
    }

    private fun addMarker(point: GeoPoint, title: String) {
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        map.overlays.add(marker)
    }
}
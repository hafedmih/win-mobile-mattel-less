package com.famoco.kyctelcomr.core.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.famoco.famocodialog.DialogType
import com.famoco.famocodialog.FamocoDialog
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.core.repositories.UploadRepository
import com.famoco.kyctelcomr.core.ui.fragments.SmartCardFragment
import com.famoco.kyctelcomr.core.ui.fragments.SmartCardFragment.Companion
import com.famoco.kyctelcomr.core.ui.viewmodels.MainViewModel
import com.famoco.kyctelcomr.core.utils.DeviceLocationManager
import com.famoco.kyctelcomr.core.utils.SMSListener
import com.famoco.kyctelcomr.core.utils.SMSReceiver
import com.famoco.kyctelcomr.databinding.ActivityMainBinding
import com.famoco.kyctelcomr.mattel.model.DeviceLocation
// import com.famoco.kyctelcomr.mattel.ui.fragments.PhoneFragment // This import seems unused
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SMSListener {


    // Using instance TAG, consistent with original MainActivity
    private val TAG: String = MainActivity::class.java.getSimpleName()

    // Constants for location updates, consistent with original MainActivity and LocationTestActivity
    private val MIN_UPDATE_INTERVAL_IN_MS = (10 * 1000).toLong() // 10 seconds
    private val MIN_UPDATE_DISTANCE_IN_M = 10f // 10 meters

    private var mLocationManager: LocationManager? = null
    private var mLocationListener: LocationListener? = null

    // Permission request codes
    private val PHONE_PERMISSION_REQUEST_CODE = 10
    private val LOCATION_PERMISSION_REQUEST_CODE = 20


    private lateinit var binding: ActivityMainBinding

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    private lateinit var detachSnackbar: Snackbar

    private val mainViewModel: MainViewModel by viewModels()

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var pendingIsoDep: IsoDep? = null

    @Inject
    lateinit var uploadRepository: UploadRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request phone permissions
        if (!hasPhonePermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE),
                PHONE_PERMISSION_REQUEST_CODE
            )
        }

        // Request location permissions
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        SMSReceiver.bindListener(this)

        // Initialize LocationManager and LocationListener
        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        mLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
               // Log.d(TAG, "onLocationChanged: $location")
                // You could update a ViewModel or UI here as well
                // mainViewModel.updateCurrentLocation(location)
                uploadRepository.updateLastKnownLocation(location)
                checkDeviceLocationDb(location.latitude, location.longitude, applicationContext)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
                Log.d(TAG, "Location provider status changed: $s, status: $i")
            }
            override fun onProviderEnabled(s: String) {
                Log.d(TAG, "Location provider enabled: $s")
            }
            override fun onProviderDisabled(s: String) {
                Log.d(TAG, "Location provider disabled: $s")
            }
        }

        setupNavigation()
        detachSnackbar = Snackbar.make(binding.root, getString(R.string.external_devices_deconnection), Snackbar.LENGTH_INDEFINITE)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
//        mainViewModel.cardNumber.observe(this) { cardNumber ->
//            if (cardNumber != null && pendingIsoDep != null) {
//                // Call getIdentity only AFTER card number is received
//                mainViewModel.getIdentity(pendingIsoDep!!)
//                mainViewModel.matchOnCard(pendingIsoDep!!)
//            }
//        }
    }

    override fun onResume() {
        super.onResume()

//        mainViewModel.smartCardReaderPlugged.observe(this) {
//            if (it == false) {
//             //   detachSnackbar.show()
//                backToHome()
//                mainViewModel.destroy()
//            }
//
//            if (it == true) {
//                detachSnackbar.dismiss()
//            }
//        }
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)


        // Start location updates if permissions are granted
        if (hasLocationPermission()) {
            startLocationUpdates()
            getLastKnownLocationAndCheck()
        } else {
          //  Log.w(TAG, "Location permission not granted. Cannot start location updates in onResume.")
            // Optionally, prompt the user again or explain why location is needed
        }

    }
    private fun isFP200(): Boolean {
        return Build.MODEL.uppercase(Locale.ROOT).contains("FP200")
    }
    private fun hasPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
            Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationUpdates() {
        // Ensure LocationManager and LocationListener are initialized
        if (mLocationManager == null || mLocationListener == null) {
            Log.e(TAG, "LocationManager or LocationListener not initialized. Cannot start updates.")
            // Attempt re-initialization if needed, though they should be set in onCreate
            mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
            if (mLocationListener == null && mLocationManager != null) { // Re-create listener if null
                mLocationListener = object : LocationListener {
                    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
                    override fun onLocationChanged(location: Location) { Log.d(TAG, "onLocationChanged (re-init): $location")
                        uploadRepository.updateLastKnownLocation(location)
                        checkDeviceLocationDb(location.latitude, location.longitude, applicationContext)
                    }
                    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
                    override fun onProviderEnabled(s: String) {}
                    override fun onProviderDisabled(s: String) {}
                }
            }
            if (mLocationManager == null || mLocationListener == null) return // Still not initialized, exit
        }


        // Double-check permissions before requesting updates (paranoid check)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission check failed inside startLocationUpdates. This should be caught by hasLocationPermission().")
            return
        }

        try {
            // Retrieve and log last known location from Network Provider
            val lastKnownLocationNetwork = mLocationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnownLocationNetwork?.let {
                Log.d(TAG, "Last known location (Network): $it")
                // mainViewModel.updateCurrentLocation(it) // Optionally update with this initial location
            }

            // Retrieve and log last known location from GPS Provider
            val lastKnownLocationGps = mLocationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocationGps?.let {
                Log.d(TAG, "Last known location (GPS): $it")
                // if (lastKnownLocationNetwork == null || it.time > lastKnownLocationNetwork.time) {
                //    mainViewModel.updateCurrentLocation(it) // Update if GPS is more recent/only one
                // }
            }

            if (lastKnownLocationNetwork == null && lastKnownLocationGps == null) {
                Log.i(TAG, "No last known location available from Network or GPS providers.")
            }

            // Request location updates from Network Provider
            mLocationListener?.let { listener ->
                if (mLocationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                    mLocationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_UPDATE_INTERVAL_IN_MS,
                        MIN_UPDATE_DISTANCE_IN_M,
                        listener
                    )
                    Log.d(TAG, "Requested location updates from Network provider.")
                } else {
                    Log.w(TAG, "Network provider is not enabled.")
                }

                // Request location updates from GPS Provider
                if (mLocationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    mLocationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_UPDATE_INTERVAL_IN_MS,
                        MIN_UPDATE_DISTANCE_IN_M,
                        listener
                    )
                    Log.d(TAG, "Requested location updates from GPS provider.")
                } else {
                    Log.w(TAG, "GPS provider is not enabled.")
                }
            }
        } catch (ex: SecurityException) {
            Log.e(TAG, "SecurityException in startLocationUpdates: ${ex.message}", ex)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startLocationUpdates: ${e.message}", e)
        }
    }


    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onPause() {
        super.onPause()
        // Stop location updates while the app is in background
        mLocationListener?.let { listener ->
            mLocationManager?.removeUpdates(listener)
            Log.d(TAG, "Location updates removed in onPause.")
        }
        nfcAdapter?.disableForegroundDispatch(this)
      //  backToHome() // Existing logic
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.destroy()
        // It's good practice to ensure listeners are unbound, though removeUpdates in onPause should handle LocationListener
        //SMSReceiver.unbindListener() // Assuming SMSReceiver has an unbindListener or similar cleanup
        mLocationListener = null
        mLocationManager = null
    }

    private fun backToHome() {
        if (::navController.isInitialized && navController.currentDestination?.id != R.id.homeFragment) {
            // Check if nav_host_fragment is still valid before trying to find NavController
            try {
                findNavController(R.id.nav_host_fragment).popBackStack(R.id.homeFragment, false)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Could not pop back to home: NavController not found or not attached.", e)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Toast.makeText(applicationContext,"back",Toast.LENGTH_LONG).show()
        // Default behavior is typically desired unless specific override is needed.
        // If you want to prevent back navigation or do custom logic, implement it here.
        // super.onBackPressed() // Call this if you want the default system back press behavior
    }

    override fun onSMSReceived(msg: String) {
        Log.i("smsTag", "onSMSReceived =>  $msg")
        val dialog = FamocoDialog(this)
        dialog.setDialogType(DialogType.INFO)
            .setTitle("SMS")
            .setContent(msg)
            .setOnNegativeButtonClicked(getString(R.string.dialog_ok_btn)) {
                dialog.dismiss()
            }
            .showPositiveButton(false)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PHONE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.i(TAG, "Phone permissions granted.")
                } else {
                    Log.w(TAG, "Phone permissions denied.")
                    Toast.makeText(this, "Phone permissions are required for some features.", Toast.LENGTH_LONG).show()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED || (grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED))) {
                    Log.i(TAG, "Location permissions granted.")
                    // Location updates will attempt to start in onResume if permissions are now granted.
                    // Or, you could call startLocationUpdates() here directly:
                    // if (isResumed) { // Ensure activity is in a state to start updates
                    //    startLocationUpdates()
                    // }
                } else {
                    Log.w(TAG, "Location permissions denied.")
                    Toast.makeText(this, "Location permission is required to get your current location.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    fun calculateDistanceInMeters(
        currentLat: Double, currentLng: Double,
        deviceLat: Double, deviceLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(currentLat, currentLng, deviceLat, deviceLng, results)

        // Logging the coordinates and the result
        Log.d("DistanceCalc", "Current Location: ($currentLat, $currentLng)")
        Log.d("DistanceCalc", "Device Location: ($deviceLat, $deviceLng)")
        Log.d("DistanceCalc", "Distance: ${results[0]} meters")

        return results[0] // distance in meters
    }
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    @SuppressLint("HardwareIds")
    fun checkDeviceLocationDb(
        currentLat: Double,
        currentLng: Double,
        context: Context
    ) {
        // 1. Get locations from JSON file instead of hardcoded list
        val deviceList = DeviceLocationManager.getLocations(context)

        val famocoId: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                try { Build.getSerial() } catch (e: Exception) { "UNKNOWN" }
            } else { "UNAUTHORIZED" }
        } else {
            Build.SERIAL ?: "UNKNOWN"
        }

       // val shortId = famocoId.takeLast(4).trim().uppercase()
        val shortId = famocoId.takeLast(4)
            .uppercase()
            .trim()
            .replace("O", "0") // Change letter O to zero
            .replace("I", "1")
        Log.d("DEBUG_LOC", "My Device Short ID: '$shortId'")

        val matchingDevice = deviceList.find {
            it.Device.trim().equals(shortId, ignoreCase = true)

        }
        if (matchingDevice != null) {
            val deviceLat = matchingDevice.Latitude.replace(",", ".").toDouble()
            val deviceLng = matchingDevice.Longtitude.replace(",", ".").toDouble()

            val distance = calculateDistanceInMeters(currentLat, currentLng, deviceLat, deviceLng)

            if (distance > 250) {
                val intent = Intent(context, DeviceNotAllowedActivity::class.java).apply {
                    putExtra("current_lat", currentLat)
                    putExtra("current_lng", currentLng)
                    putExtra("allowed_lat", deviceLat)
                    putExtra("allowed_lng", deviceLng)
                    putExtra("distance", distance.toString())
                    putExtra("device_id", matchingDevice.Device)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
    private fun getLastKnownLocationAndCheck() {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            if (bestLocation == null) {
                Log.w(TAG, "No last known location available.")
                return
            }

            // Retrieve Famoco ID based on Android version and permissions


            // Log.d(TAG, "Famoco ID (serial): $famocoId")
            Log.d(TAG, "Immediate last known location: $bestLocation")

            checkDeviceLocationDb( bestLocation.latitude, bestLocation.longitude, applicationContext)

        } catch (e: SecurityException) {
            Log.e(TAG, "Missing required permission: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location or Famoco ID: ${e.message}")
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent == null) return

        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag == null) return

        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.connect()
                pendingIsoDep = isoDep

                // 👉 CALL YOUR VIEWMODEL FUNCTION HERE
                //mainViewModel.getCardNumber(isoDep)
                mainViewModel.setDetectedIsoDep(isoDep)



            } catch (e: Exception) {
                Log.e("NFC", "IsoDep error: ${e.message}")
            }
        }
    }


    }
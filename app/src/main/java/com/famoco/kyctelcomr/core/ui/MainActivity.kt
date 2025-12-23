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
       // val database = KycDatabase.getInstance(context)
       // val deviceLocationDao = database.getDao()

        // Check if the device location table is empty
//        if (deviceLocationDao.getDeviceLocationCount() == 0) {
//            // If empty, fetch from the source and insert into the database
//            val locationsToInsert = getDeviceLocations()
//            deviceLocationDao.insertAllDeviceLocations(locationsToInsert)
//        }
        getDeviceLocations()

        // Now, get the locations from the database
        val deviceList =getDeviceLocations()
            //deviceLocationDao.getAllDeviceLocations()
        val famocoId: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8+ requires READ_PHONE_STATE permission for getSerial()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    Build.getSerial()
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException getting serial: ${e.message}")
                    "UNKNOWN"
                } catch (e: Exception) {
                    Log.e(TAG, "Exception getting serial: ${e.message}")
                    "UNKNOWN"
                }
            } else {
                Log.e(TAG, "READ_PHONE_STATE permission not granted.")
                "UNAUTHORIZED"
            }
        } else {
            // Android 6 & 7 - Build.SERIAL accessible without permission
            Build.SERIAL ?: "UNKNOWN"
        }

        val shortId = famocoId.takeLast(4)

        val matchingDevice = deviceList.find { it.Device.equals(shortId, ignoreCase = true) }

        if (matchingDevice != null) {
            val deviceLat = matchingDevice.Latitude.replace(",", ".").toDouble()
            val deviceLng = matchingDevice.Longtitude.replace(",", ".").toDouble()

            val distance = calculateDistanceInMeters(currentLat, currentLng, deviceLat, deviceLng)

            //   Toast.makeText(context, "Distance: " + distance, Toast.LENGTH_LONG).show()
            if (distance > 100) {
                // Show screen or dialog that device can't work
                val intent = Intent(context, DeviceNotAllowedActivity::class.java).apply {
                    putExtra("current_lat", currentLat)
                    putExtra("current_lng", currentLng)
                    putExtra("allowed_lat", deviceLat)
                    putExtra("allowed_lng", deviceLng)
                    putExtra("distance", distance.toString())
                    putExtra("device_id", matchingDevice.Device)
                    putExtra("device_franchise", matchingDevice.Franchise)
                    putExtra("device_msisdn", matchingDevice.MSISDN)

                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // <--- Add this flag
                }
                context.startActivity(intent)
            } else {
                // Allow the device to work
            }
        } else {
         //   Log.e("loc", "$shortId Device not founded");
            // Device not found
        }
    }
//18.092272,-15.977404 appareil 0103760291062411211GC1
    fun getDeviceLocations(): List<DeviceLocation> {
    return listOf(
        DeviceLocation("hadj pdv", 30497486L, "6789", "18.092173636189035", "-15.977379421888395"),
        DeviceLocation("PDV Immeuble Rasidi", 30497326L, "1BXV", "18.108680", "-15.982508"),
        DeviceLocation("PDV Ould Ebbadou", 34777309L, "1ETL", "18.114167", "-15.920556"),
        DeviceLocation("PDV Ain Talh", 31267351L, "1DEV", "18.135057", "-15.93282"),
        DeviceLocation("PDV Marché Eletehad", 34734801L, "1I9C", "18.084743", "-15.980883"),
        DeviceLocation("PDV Carrafor aziz", 32339183L, "1I9D", "18.119437", "-15.93635"),
        DeviceLocation("PDV Point chaud", 31341748L, "1FEL", "18.089560", "-15.974903"),
        DeviceLocation("PDV Hospital militaire", 34716330L, "1IQY", "18.110402", "-15.951751"),
        DeviceLocation("PDV Toujounine", 34634578L, "1IAY", "18.066855", "-15.895839"),
        DeviceLocation("PDV  Poteau 18", 34747666L, "1HW8", "18.064191", "-15.952563"),
        DeviceLocation("PDV Poteau 6", 31407954L, "1XPK", "18.052229", "-15.965542"),
        DeviceLocation("PDV Ksar Citernat", 30497385L, "1GX9", "18.102376", "-15.960251"),
        DeviceLocation("PDV Soukouk", 32518379L, "1IJU", "18.128688", "-15.938968"),
        DeviceLocation("PDV Teyaret Marche", 30497390L, "1BXI", "18.118813", "-15.945526"),
        DeviceLocation("PDV Bana Blanc", 31634533L, "1E89", "18.103061", "-15.976565"),
        DeviceLocation("PDV Melleh Sect 2", 32529819L, "1JH5", "18.055237", "-15.938957"),
        DeviceLocation("PDV Mauritanie Coleu", 32142008L, "1ILQ", "18.081516", "-15.977123"),
        DeviceLocation("PDV Msid Ennour", 34226762L, "1J36", "18.038940", "-15.962556"),
        DeviceLocation("PDV Poteau 4", 30497353L, "1GB5", "18.046915", "-15.971472"),
        DeviceLocation("PDV 6eme G. Melleh", 30404755L, "1IW9", "18.067253", "-15.98709"),
        DeviceLocation("PDV dar Elbeidha", 32925659L, "1IE6", "18.011921", "-15.987487"),
        DeviceLocation("PDV Bouhdida R.3", 30405164L, "1JFL", "18.064964", "-15.922416"),
        DeviceLocation("PDV Carrefour Adrar", 32998461L, "1IG2", "18.045701", "-15.931388"),
        DeviceLocation("PDV Mbeiet 10", 34144382L, "1I9N", "18.068738", "-15.901931"),
        DeviceLocation("PDV Carrf. Madrid", 34741850L, "1XXA", "18.079038", "-15.96623"),
        DeviceLocation("PDV  Itihadiya", 30403043L, ")HJL", "18.131761", "-15.926816"),
        DeviceLocation("PDV Arret Bus", 34114542L, "1CJE", "18.067907", "-15.993758"),
        DeviceLocation("PDV Basra", 34722950L, "1IYX", "18.071543", "-16.003414"),
        DeviceLocation("PDV Immeuble Rasidi", 30497195L, "1GTO", "18.108680", "-15.982508"),
        DeviceLocation("PDV Ould Ebbadou", 34784772L, "1E82", "18.114167", "-15.920556"),
        DeviceLocation("PDV Ain Talh", 32392593L, "1IES", "18.135057", "-15.93282"),
        DeviceLocation("PDV Marché Eletehad", 32234297L, "1IPW", "18.084743", "-15.980883"),
        DeviceLocation("PDV Carrafor aziz", 32349636L, "1HYD", "18.119437", "-15.93635"),
        DeviceLocation("PDV Point chaud", 31767266L, "1DRL", "18.089560", "-15.974903"),
        DeviceLocation("PDV Hospital militaire", 34652362L, "1I4T", "18.110402", "-15.951751"),
        DeviceLocation("PDV Toujounine", 32921290L, "1IMU", "18.066855", "-15.895839"),
        DeviceLocation("PDV  Poteau 18", 32074135L, "1IJI", "18.064191", "-15.952563"),
        DeviceLocation("PDV Poteau 6", 31735253L, "1CU6", "18.052229", "-15.965542"),
        DeviceLocation("PDV Ksar Citernat", 31360565L, "1CU8", "18.102376", "-15.960251"),
        DeviceLocation("PDV Soukouk", 34768463L, "1IC6", "18.128688", "-15.938968"),
        DeviceLocation("PDV Teyaret Marche", 31227046L, "1HPT", "18.118775", "-15.945548"),
        DeviceLocation("PDV Bana Blanc", 34781376L, "1JFC", "18.103061", "-15.976565"),
        DeviceLocation("PDV Melleh Sect 2", 31758335L, "1F6V", "18.055237", "-15.938957"),
        DeviceLocation("PDV Mauritanie Coleu", 31423772L, "1F3D", "18.081516", "-15.977123"),
        DeviceLocation("PDV Msid Ennour", 30497383L, "1G7Y", "18.038940", "-15.962556"),
        DeviceLocation("PDV Poteau 4", 32706039L, "1ID2", "18.046915", "-15.971472"),
        DeviceLocation("PDV 6eme G. Melleh", 30497368L, "1GMM", "18.067253", "-15.98709"),
        DeviceLocation("PDV dar Elbeidha", 32251798L, "1HOG", "18.011921", "-15.987487"),
        DeviceLocation("PDV Bouhdida R.3", 31307864L, "1IJL", "18.064964", "-15.922416"),
        DeviceLocation("PDV Carrefour Adrar", 30405082L, "1JFN", "18.045701", "-15.931388"),
        DeviceLocation("PDV Mbeiet 10", 32643893L, "1IIO", "18.068738", "-15.901931"),
        DeviceLocation("PDV Carrf. Madrid", 30403108L, "1J7D", "18.079038", "-15.96623"),
        DeviceLocation("PDV  Itihadiya", 30403784L, "1HW6", "18.131761", "-15.926816"),
        DeviceLocation("PDV Arret Bus", 31550752L, "1DAA", "18.067907", "-15.993758"),
        DeviceLocation("PDV Basra", 32349746L, "1I43", "18.071333", "-16.003136"),
        DeviceLocation("PDV Marche Seyidi", 30400031L, "1IFM", "18.123805", "-15.938076"),
        DeviceLocation("PDV Marche Seyidi", 34389701L, "1IP9", "18.123805", "-15.938076"),
        DeviceLocation("PDV Carrefour bamako", 34202683L, "1J4D", "17.992360", "-15.974526"),
        DeviceLocation("PDV Carrefour bamako", 30405107L, "1JF6", "17.992360", "-15.974526"),
        DeviceLocation("PDV Tintan", 37476532L, "1XS4", "16.383279", "-10.166752"),
        DeviceLocation("PDV Point chaud 18", 31056025L, "1XSG", "20.916512", "-17.049477"),
        DeviceLocation("PDV Ain Talh rue Tintane", 37476536L, "1XNT", "18.142249", "-15.936383"),
        DeviceLocation("PDV Carrefour Djimbo", 37476506L, "1XTT", "18.101619", "-15.928017"),
        DeviceLocation("PDV  Robinet 3", 31056021L, "1XT2", "20.945286", "-17.038835"),
        DeviceLocation("PDV kiffa", 37476533L, "1XNY", "16.621005", "-11.403726"),
        DeviceLocation("PDV Aioun", 37408457L, "1XQG", "16.662005", "-9.603083"),
        DeviceLocation("PDV Atar", 31056016L, "1XU3", "20.518271", "-13.053631"),
        DeviceLocation("PDV Tintan", 34751253L, "1IBZ", "16.383279", "-10.166752"),
        DeviceLocation("PDV Point chaud 18", 31056027L, "1XRK", "20.916512", "-17.049477"),
        DeviceLocation("PDV Ain Talh rue Tintane", 31056026L, "1XOL", "18.142249", "-15.936383"),
        DeviceLocation("PDV Carrefour Djimbo", 34784939L, "1IPA", "18.101619", "-15.928017"),
        DeviceLocation("PDV  Robinet 3", 34188792L, "1IJ8", "20.945286", "-17.038835"),
        DeviceLocation("PDV kiffa", 31595869L, "1DR3", "16.621005", "-11.403726"),
        DeviceLocation("PDV Aioun", 30497398L, "1BXK", "16.662005", "-9.603083"),
        DeviceLocation("PDV Atar", 32224534L, "1J0W", "20.518271", "-13.053631"),
        DeviceLocation("PDV Tarhil 05", 30497226L, "1G6T", "18.015763", "-15.944553"),
        DeviceLocation("PDV Nema", 37408468L, "1XSZ", "16.613241", "-7.256207"),
        DeviceLocation("PDV Timbedre", 31375179L, "1IJO", "16.240852", "-8.171155"),
        DeviceLocation("PDV Tarhil 05", 33399718L, "1XUW", "18.015763", "-15.944553"),
        DeviceLocation("PDV Nema", 37408460L, "1XL3", "16.613241", "-7.256207"),
        DeviceLocation("PDV Timbedre", 37408466L, "1XU9", "16.2407833", "-8.171156"),
        DeviceLocation("PDV Hopital Espagnol", 33399704L, "1XV4", "20.957336", "-17.034433"),
        DeviceLocation("PDV Tarhil", 33399705L, "1XN0", "20.979083, ", " -17.030226"),
        DeviceLocation("PDV Socogim", 33399706L, "1XVM", "20.939735,", " -17.042056"),
        DeviceLocation("PDV Hopital Espagnol", 34739927L, "1HYS", "20.956171,", " -17.034191"),
        DeviceLocation("PDV Tarhil", 30497313L, "1BXN", "20.979083, ", " -17.030226"),
        DeviceLocation("PDV Socogim", 30497214L, "1GIS", "20.939735,", " -17.042056"),
        DeviceLocation("PDV Chami", 31544595L, "1E31", "20.164651", "-15.972878"),
        DeviceLocation("PDV Ould Yanja", 31764374L, "1EFR", "15.545011", "-11.710453"),
        DeviceLocation("PDV Chami", 31544595L, "1E31", "20.164651", "-15.972878"),
        DeviceLocation("PDV Ould Yanja", 31764374L, "1EFR", "15.545011", "-11.710453"),
        DeviceLocation("PDV MBout", 34771664L, "1IAF", "16.017753,", "-12.580563"),
        DeviceLocation("PDV kamour", 31056019L, "1GVY", "17.078083", "-12.0418"),
        DeviceLocation("PDV Kiffa", 34708562L, "1HQY", "16.614384", "-11.383005"),
        DeviceLocation("PDV Tintan", 32993770L, "1IK9", "16.384232", "-10.161354"),
        DeviceLocation("PDV Chami", 30497367L, "1FG2", "20.164651", "-15.972878"),
        DeviceLocation("PDV Ould Yanja", 32754703L, "1IWL", "15.545011", "-11.710453"),
        DeviceLocation("PDV MBout", 32258624L, "1EB6", "16.017753,", "-12.580563"),
        DeviceLocation("PDV kamour", 32542063L, "1IVB", "17.078083", "-12.0418"),
        DeviceLocation("PDV Kiffa", 34402205L, "1IP8", "16.614384", "-11.383005"),
        DeviceLocation("PDV Tintan", 34704567L, "1XXN", "16.384232", "-10.161354"),
        DeviceLocation("PDV  kankoussa", 30497375L, "1BH1", "15.936118", "-11.518201"),
        DeviceLocation("PDV Aweynat", 32993898L, "1JEG", "16.383396,", "-8.882246"),
        DeviceLocation("PDV  kankoussa", 34140312L, "1J35", "15.936118", "-11.518201"),
        DeviceLocation("PDV Aweynat", 32912598L, "1J1X", "16.383396,", "-8.882246"),
        DeviceLocation("PDV Djiguenni ", 31454181L, "1DV4", "15.728931,", "-8.665607"),
        DeviceLocation("PDV Ouad Naga", 31656425L, "1E5B", "17.962875", "-15.520802"),
        DeviceLocation("PDV R Kiz", 34629982L, "1IOK", "16.915726,", "-15.232599"),
        DeviceLocation("PDV Djiguenni ", 32458482L, "1JB5", "15.728931,", "-8.665607"),
        DeviceLocation("PDV Ouad Naga", 30498280L, "1XUO", "17.962875", "-15.520802"),
        DeviceLocation("PDV R Kiz", 30498286L, "1XNG", "16.915726,", "-15.232599"),
        DeviceLocation("PDV Akjoujt", 37408455L, "1XQB", "19.739054,", "-14.387937"),
        DeviceLocation("PDV Nema", 34863763L, "1J43", "16.615095,", "-7.255994"),
        DeviceLocation("PDV Rosso", 37476558L, "1DYI", "16.508644, ", "-15.805384"),
        DeviceLocation("PDV Zeouratt", 31056022L, "1XKO", "22.735437,", "-12.469078"),
        DeviceLocation("PDV Twil", 37408462L, "1XUX", "15.523246,", "-10.142773"),
        DeviceLocation("PDV Bababé", 32091971L, "1HR2", "16.343254,", "-13.947799"),
        DeviceLocation("PDV Akjoujt", 30497394L, "1BXB", "19.739054,", "-14.387937"),
        DeviceLocation("PDV Nema", 31056029L, "1IIK", "16.615095,", "-7.255994"),
        DeviceLocation("PDV Rosso", 32254030L, "1IL8", "16.508644, ", "-15.805384"),
        DeviceLocation("PDV Zeouratt", 32875966L, "1IKW", "22.735437,", "-12.469078"),
        DeviceLocation("PDV Twil", 30497150L, "1BCE", "15.523246,", "-10.142773"),
        DeviceLocation("PDV Bababé", 31473329L, "1ENQ", "16.343254,", "-13.947799"),
        DeviceLocation("PDV Elvoulaniye", 30497162L, "1FIS", "15.504578,", "-9.822726"),
        DeviceLocation("PDV Zouerate", 30497135L, "1CQ4", "22.735342,", "-12.463900"),
        DeviceLocation("PDV Elvoulaniye", 37408459L, "1XNJ", "15.504578,", "-9.822726"),
        DeviceLocation("PDV Zouerate", 33399710L, "1XMW", "22.735342,", "-12.463900"),
        DeviceLocation("PDV Magtalahjar", 30497155L, "1BCB", "17.511127,", "-13.092668"),
        DeviceLocation("PDV Vassala", 30497160L, "1GON", "15.557069,", "-5.521210"),
        DeviceLocation("PDV Magtalahjar", 37476601L, "1XVI", "17.511127,", "-13.092668"),
        DeviceLocation("PDV Vassala", 33399716L, "1XV0", "15.557069,", "-5.521210"),
        DeviceLocation("PDV Bassiknou", 37476540L, "1XR1", "15.867513", "-5.954466"),
        DeviceLocation("PDV Velouja", 37476470L, "1XVL", "18.059828,", "-15.945716"),
        DeviceLocation("PDV Carrf.Charm el Cheikh ", 30497307L, "1G23", "18.048303", "-15.953926"),
        DeviceLocation("PDV Hayatt Jedida", 37476521L, "1XMD", "18.032173,", "-15.907780"),
        DeviceLocation("PDV Tarhil 16", 37476504L, "1XQJ", "18.014389,", "-15.958967"),
        DeviceLocation("PDV Bassiknou", 30497157L, "1GL6", "15.867513", "-5.954466"),
        DeviceLocation("PDV Velouja", 30497167L, "1GWG", "18.059828,", "-15.945716"),
        DeviceLocation("PDV Carrf.Charm el Cheikh ", 30497274L, "1G74", "18.048303, ", "-15.953926"),
        DeviceLocation("PDV Hayatt Jedida", 30497302L, "1G85", "18.032173,", "-15.907780"),
        DeviceLocation("PDV Tarhil 16", 31386965L, "1HQL", "18.014389,", "-15.958967"),
        DeviceLocation("PDV Melleh Secteur 4", 30497166L, "1JB5", "18.043420", "-15.944413"),
        DeviceLocation("PDV Adel Bagrou", 30497159L, "1GJX", "15.535449", "-7.031250"),
        DeviceLocation("PDV Melleh Secteur 4", 32961262L, "1IRD", "18.043420", "-15.944413"),
        DeviceLocation("PDV Adel Bagrou", 37476572L, "1XXF", "15.535449", "-7.031250"),
        DeviceLocation("PDV Riyadh PK 7", 30498285L, "1XLG", "18.020926", "-15.975856"),
        DeviceLocation("PDV Riyadh PK 7", 30497340L, "1IEL", "18.020163", "-15.974629"),
        DeviceLocation("PDV Amourj", 37476582L, "1XXB", "16.109299", "-7.214112"),
        DeviceLocation("PDV Amourj", 30497164L, ")GOL", "16.109299", "-7.214112"),
        DeviceLocation("PDV Poteau 3", 30498289L, "1XRN", "18.043938", "-15.973793"),
        DeviceLocation("PDV Poteau 3", 30498281L, "1DV4", "18.043938", "-15.973793"),
        DeviceLocation("PDV Carrefour Bakar", 30498290L, "1XRW", "18.073333", " -15.954189"),
        DeviceLocation("PDV Carrefour Bakar", 30497139L, "1CQ5", "18.073333", " -15.954189"),
        DeviceLocation("PDV Kandahar", 37476620L, "1FG2", "18.033450", "-15.961623"),
        DeviceLocation("PDV Kandahar", 30497140L, "1J35", "18.033450", "-15.961623"),
        DeviceLocation("PDV Big Market", 37476568L, "1XPR", "18.107956", "-15.998511"),
        DeviceLocation("PDV Big Market", 30497269L, "1BQT", "18.107956", "-15.998511"),
        DeviceLocation("PDV Tarhil Radwan ", 37476527L, "1XXC", "18.032165", "-15.927734"),
        DeviceLocation("PDV Tarhil Radwan ", 37408461L, "1GO4", "18.032165", "-15.927734"),
        DeviceLocation("PDV TVZ Tata ", 30497351L, "1XOY", "18.119261", "-15.992944"),
        DeviceLocation("PDV TVZ Tata ", 34750042L, "1IJO", "18.119261", "-15.992944"),
        DeviceLocation("PDV Ould Ebbadou", 30497154L, "1BCI", "18.114167", "-15.920556"),
        DeviceLocation("PDV Toujounine", 30497303L, "1G8T", "18.066855", "-15.895839"),
        DeviceLocation("PDV Poteau 6", 30497213L, "1XYM", "18.052229", "-15.965542"),
        DeviceLocation("PDV Teyaret Marche", 30497252L, "1XLQ", "18.118798", "-15.945499"),
        DeviceLocation("PDV Carrefour Adrar", 30497158L, "1GU3", "18.045701", "-15.931388"),
        DeviceLocation("PDV Mbeiet 10", 32638164L, "1JEG", "18.068738", "-15.901931"),
        DeviceLocation("PDV Carrefour Djimbo", 30497305L, "1GTJ", "18.101619", "-15.928017"),
        DeviceLocation("PDV Robinet 5", 30400262L, "1J97", "20.950748", "-17.036890"),
        DeviceLocation("PDV Robinet 5", 30404589L, "1IWT", "20.950748", "-17.036890"),
        DeviceLocation("PDV  Robinet 4", 30497161L, "1XNI", "20.948143", "-17.039464"),
        DeviceLocation("PDV Nebaghiye", 30497280L, "1XYC", "17.569000", "-15.041667"),
        DeviceLocation("PDV  Robinet 4", 32764804L, "1GPB", "20.948143", "-17.039464"),
        DeviceLocation("PDV Nebaghiye", 32163473L, "1GVA", "17.569000", "-15.041667"),
        DeviceLocation("PDV Aoujeft ", 37476523L, "1I7Y", "20.030468", "-13.048477"),
        DeviceLocation("PDV Aoujeft ", 30497347L, "1I1U", "20.030468", "-13.048477"),
        DeviceLocation("PDV Kaédi", 37476621L, "1XW1", "16.145648", "-13.501949"),
        DeviceLocation("PDV Kaédi", 31665987L, "1BH1", "16.145648", "-13.501949"),
        DeviceLocation("PDV Kiffa", 31056028L, "1XQ1", "16.623159", "-11.398478"),
        DeviceLocation("PDV Kiffa", 31189285L, "1E1B", "16.623159", "-11.398478"),
        DeviceLocation("Guichet Point chaud", 37408456L, "1XPT", "18.090710", "-15.974838"),
        DeviceLocation("Guichet Agence Siege", 37408465L, "1XQA", "18.106543", "-15.966998"),
        DeviceLocation("PDV 5eme cinéma  saada", 30497310L, "1BZH", "18.077747", "-16.000408"),
        DeviceLocation("PDV 5eme cinéma  saada", 30404669L, "1J6M", "18.077747", "-16.000408"),
        DeviceLocation("PDV 5eme cinéma  saada", 30497310L, "1BZH", "18.077747", "-16.000408"),
        DeviceLocation("PDV 5eme cinéma  saada", 30404669L, "1J6M", "18.077747", "-16.000408"),
        DeviceLocation("PDV 6eme  chateau deau", 30497217L, "1GVE", "18.058268", "-15.985309"),
        DeviceLocation("PDV 6eme  chateau deau", 30497244L, "1BU0", "18.058268", "-15.985309"),
        DeviceLocation("PDV Sélibaby", 30497295L, "1FDS", "15.159620", "-12.183106"),
        DeviceLocation("PDV Sélibaby", 32770797L, "1J4A", "15.159620", "-12.183106"),
        DeviceLocation("PDV 5eme G. SINIGAL", 32240577L, "1IP2", "18.074873", "-15.996110"),
        DeviceLocation("PDV 5eme G. SINIGAL", 34705189L, "1IN4", "18.074873", "-15.996110"),
        DeviceLocation("PDV Ain Vrbah", 30497138L, "1CJ5", "15.915725", " -10.389392"),
        DeviceLocation("hadj pdv", 33411686L, "6789", "18.092173636189035", "-15.977379421888395"),
        DeviceLocation("PDV Basra", 32529109L, "1I8K", "18.071543", " -16.003414"),
        DeviceLocation("PDV Ouadane", 30497324L, "1GO4", "20.933262", "-11.616660"),
        DeviceLocation("PDV Ouadane", 37476466L, "1BQO", "20.933262", "-11.616660"),
        DeviceLocation("PDV Chinguett", 30498306L, "1IN7", "20.460835", " -12.362282"),
        DeviceLocation("PDV Chinguett", 37476548L, "1XY7", "20.460835", " -12.362282"),
        DeviceLocation("PDV Nbeika", 30497319L, "1G6D", "17.978016", "-12.252596"),
        DeviceLocation("PDV Nbeika", 37476604L, "1XTR", "17.978016", "-12.252596"),
        DeviceLocation("PDV Toujounine Neyeb", 37476584L, "1XPP", "18.080370", " -15.913141"),
        DeviceLocation("PDV Toujounine Neyeb", 30497203L, "1I2Q", "18.080370", " -15.913141"),
        DeviceLocation("PDV carafou 24", 37476537L, "1XNM", "18.071217", "-15.936090"),
        DeviceLocation("PDV carafou 24", 37476511L, "1XR5", "18.071217", "-15.936090"),
   //     DeviceLocation("PDV carafou 24", 37476511L, "1OJ5", "18.106663", "-15.966983"),

        )
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
package com.famoco.kyctelcomr.core.repositories

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.Uri
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.famoco.kyctelcomr.DBHelper
import com.famoco.kyctelcomr.mattel.model.USSDKind
import com.famoco.kyctelcomrtlib.PeripheralAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AppMode { MATTEL, FRANCHISE }

@Singleton
class UploadRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val peripheralAccess: PeripheralAccess
) {

    // Global variable to control the behavior
    var currentMode: AppMode = AppMode.MATTEL

    companion object {
        private val TAG = UploadRepository::class.java.simpleName

        private const val SMS_NUMBER_FRANCHISE = "1606"
        private const val SMS_NUMBER_MATTEL = "1607"

        private const val SMS_NUMBER_CLASSIC = "128"
        private const val SMS_NUMBER_CLASSIC_2 = "1213"

        private const val SMS_SENT_ACTION = "SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    }

    /**
     * Helper to get the correct SMS number based on current mode
     */
    private val activeSmsNumber: String
        get() = if (currentMode == AppMode.MATTEL) SMS_NUMBER_MATTEL else SMS_NUMBER_FRANCHISE

    /**
     * Helper to get the location string part.
     * If MATTEL: Returns empty string.
     * If FRANCHISE: Returns "*lat*long" (or "*0*0" if no location available).
     */
    private val locationPart: String
        get() = if (currentMode == AppMode.MATTEL) {
            ""
        } else {
            lastKnownLocation?.let { "*${it.latitude}*${it.longitude}" } ?: "*0*0"
        }

    private var lastKnownLocation: Location? = null
    private val _smsState = MutableLiveData<Pair<Boolean, String>?>().apply { value = null }
    val smsState: LiveData<Pair<Boolean, String>?> = _smsState

    fun updateLastKnownLocation(location: Location) {
        this.lastKnownLocation = location
    }

    private val sendSMSBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (resultCode) {
                Activity.RESULT_OK -> { _smsState.postValue(Pair(true, "SMS_SENT")) }
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> { _smsState.postValue(Pair(false, "RESULT_ERROR_GENERIC_FAILURE")) }
                SmsManager.RESULT_ERROR_NO_SERVICE -> { _smsState.postValue(Pair(false, "RESULT_ERROR_NO_SERVICE")) }
                SmsManager.RESULT_ERROR_NULL_PDU -> { _smsState.postValue(Pair(false, "RESULT_ERROR_NULL_PDU")) }
                SmsManager.RESULT_ERROR_RADIO_OFF -> { _smsState.postValue(Pair(false, "RESULT_ERROR_RADIO_OFF")) }
            }
        }
    }

    private val deliverSMSBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (resultCode) {
                Activity.RESULT_OK -> { _smsState.postValue(Pair(true, "SMS_DELIVERED")) }
                Activity.RESULT_CANCELED -> { _smsState.postValue(Pair(false, "RESULT_CANCELED")) }
            }
        }
    }

    init {
        context.registerReceiver(sendSMSBroadcastReceiver, IntentFilter(SMS_SENT_ACTION))
        context.registerReceiver(deliverSMSBroadcastReceiver, IntentFilter(SMS_DELIVERED_ACTION))
    }

    fun clear() {
        _smsState.postValue(null)
    }

    // --- MATTEL / FRANCHISE METHODS ---

    fun sendUSSDMattel(msisdnText: String, imsi: String, isFinger: Boolean) {
        val msisdn = msisdnText.replace(" ", "")
        val personalNumber = peripheralAccess.identity.value?.personalNumber
        val optionStr = if (isFinger) "" else "0000"

        val sentPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        // ussdContent: uses activeSmsNumber and appends locationPart only if not Mattel
        val ussdContent = "*152*4${optionStr}*${msisdn}*${imsi}*${personalNumber}${locationPart}#"

        Log.d(TAG, "sendUSSDMattel (Mode: $currentMode): $ussdContent to $activeSmsNumber")
       // SmsManager.getDefault().sendTextMessage(activeSmsNumber, null, ussdContent, sentPI2, deliveredPI2)
        val mmi = Uri.Builder()
        mmi.scheme("tel")
        mmi.opaquePart(ussdContent)

        val intent = Intent(Intent.ACTION_CALL, mmi.build())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun sendSMSMattel(phoneNumberText: String, imsi: String, isFinger: Boolean) {
        val phoneNumber = phoneNumberText.replace(" ", "")
        val nni = peripheralAccess.identity.value?.personalNumber
        val nom = CleanString(peripheralAccess.identity.value?.firstName.toString())
        val prenom = CleanString(peripheralAccess.identity.value?.lastName.toString())
        val sexeFull = CleanString(peripheralAccess.identity.value?.sex.toString())
        val sexe = if (sexeFull.isNotEmpty()) sexeFull.take(1).uppercase() else ""
        val date_naissFull = CleanString(peripheralAccess.identity.value?.dateOfBirth.toString())
        val lieu_naissance = CleanString(peripheralAccess.identity.value?.placeOfBirth.toString())

        // yyyy-mm-dd format
        val date_naiss = if (date_naissFull.length >= 11) {
            date_naissFull.takeLast(4) + "-" + getNumberMonth(date_naissFull.subSequence(4, 8).toString().trim()) + "-" + date_naissFull.subSequence(8, 11).toString().trim()
        } else ""

        val db = DBHelper(context, null)
        db.addAbonne(nni.toString(), phoneNumber)

        val optionStr = if (isFinger) "" else "0000"
        val sentPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        val smsContent = if (imsi.length > 6) {
            "1${optionStr}*${phoneNumber}*${imsi}*${nni}*${nom}*${prenom}*${date_naiss}*${lieu_naissance}*${sexe}${locationPart}"
        } else {
            "2${optionStr}*${phoneNumber}*${nni}*${nom}*${prenom}*${date_naiss}*${lieu_naissance}*${sexe}${locationPart}"
        }

        Log.d(TAG, "sendSMSMattel (Mode: $currentMode): $smsContent to $activeSmsNumber")
        SmsManager.getDefault().sendTextMessage(activeSmsNumber, null, smsContent, sentPI2, deliveredPI2)
    }

    fun sendModificationMattel(phoneNumberText: String, isFinger: Boolean) {
        val phoneNumber = phoneNumberText.replace(" ", "")
        val nni = peripheralAccess.identity.value?.personalNumber
        val nom = CleanString(peripheralAccess.identity.value?.firstName.toString())
        val prenom = CleanString(peripheralAccess.identity.value?.lastName.toString())
        val sexeFull = CleanString(peripheralAccess.identity.value?.sex.toString())
        val sexe = if (sexeFull.isNotEmpty()) sexeFull.take(1).uppercase() else ""
        val date_naissFull = CleanString(peripheralAccess.identity.value?.dateOfBirth.toString())
        val lieu_naissance = CleanString(peripheralAccess.identity.value?.placeOfBirth.toString())

        val date_naiss = if (date_naissFull.length >= 11) {
            date_naissFull.takeLast(4) + "-" + getNumberMonth(date_naissFull.subSequence(4, 8).toString().trim()) + "-" + date_naissFull.subSequence(8, 11).toString().trim()
        } else ""

        val optionStr = if (isFinger) "" else "0000"
        val sentPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        val smsContent = "3${optionStr}*${phoneNumber}*${nni}*${nom}*${prenom}*${date_naiss}*${lieu_naissance}*${sexe}${locationPart}"

        Log.d(TAG, "sendModificationMattel (Mode: $currentMode): $smsContent to $activeSmsNumber")
        SmsManager.getDefault().sendTextMessage(activeSmsNumber, null, smsContent, sentPI2, deliveredPI2)
    }

    fun sendOTP(phoneNumberText: String, otp: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")
        val sentPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        val smsContent = "100*${phoneNumber}*${otp}"
        SmsManager.getDefault().sendTextMessage(activeSmsNumber, null, smsContent, sentPI, deliveredPI)
    }

    // --- CLASSIC METHODS (CHINGUITEL / OTHER) ---

    fun sendUSSD(phoneNumberText: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")
        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber

        val ussdContent = "*128*${phoneNumber}*${cardNumber}0${personalNumber}#"
        val mmi = Uri.Builder().scheme("tel").opaquePart(ussdContent).build()

        context.startActivity(Intent(Intent.ACTION_CALL, mmi).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun sendUSSD(msisdnText: String, imsi: String, kind: USSDKind) {
        val msisdn = msisdnText.replace(" ", "")
        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber
        val locationStr = lastKnownLocation?.let { "${it.latitude}*${it.longitude}" } ?: "0*0"

        val ussdContent = when (kind) {
            USSDKind.CLASSIC -> "*153*1*${msisdn}*${imsi}*${cardNumber}0${personalNumber}*${locationStr}#"
            else -> "*153*2*${msisdn}*${cardNumber}0${personalNumber}*${locationStr}#"
        }

        val mmi = Uri.Builder().scheme("tel").opaquePart(ussdContent).build()
        context.startActivity(Intent(Intent.ACTION_CALL, mmi).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun sendSMS(phoneNumberText: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")
        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber

        val sentPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        val smsContent = "${phoneNumber}*${cardNumber}0${personalNumber}"
        SmsManager.getDefault().sendTextMessage(SMS_NUMBER_CLASSIC, null, smsContent, sentPI, deliveredPI)
    }

    fun sendSMS(phoneNumberText: String, imsi: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")
        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber

        val sentPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        if (imsi.isNotEmpty()) {
            val smsContent = "*0000*${phoneNumber}*${imsi}*${cardNumber}0${personalNumber}#"
            SmsManager.getDefault().sendTextMessage(SMS_NUMBER_CLASSIC_2, null, smsContent, sentPI, deliveredPI)
        } else {
            val smsContent = "${phoneNumber}*${cardNumber}0${personalNumber}*1328579403*355288103080737"
            SmsManager.getDefault().sendTextMessage(SMS_NUMBER_CLASSIC, null, smsContent, sentPI, deliveredPI)
        }
    }

    // --- UTILS ---

    fun CleanString(str: String): String {
        val re = Regex("[^A-Za-z0-9|à|â|é|è|ê|ë|ï|î|ô|ù|û|ç|œ|æ|' ]")
        return re.replace(str, "").trim()
    }

    fun getNumberMonth(str: String): String {
        return when (str) {
            "Jan" -> "01" "Feb" -> "02" "Mar" -> "03" "Apr" -> "04"
            "May" -> "05" "Jun" -> "06" "Jul" -> "07" "Aug" -> "08"
            "Sep" -> "09" "Oct" -> "10" "Nov" -> "11" "Dec" -> "12"
            else -> ""
        }
    }

    fun getIMEI(activity: Activity): String? {
        val telephonyManager = activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try { telephonyManager.deviceId } catch (e: Exception) { null }
    }
}
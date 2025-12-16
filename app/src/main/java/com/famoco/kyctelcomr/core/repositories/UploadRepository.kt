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


@Singleton
class UploadRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val peripheralAccess: PeripheralAccess) {

    companion object {
        private val TAG = UploadRepository::class.java.simpleName

        private const val SMS_NUMBER_MATTEL ="1606" //"1607"
        //  private const val SMS_NUMBER_MATTEL = "36606730"

        //private const val SMS_NUMBER = "153"
        private const val SMS_NUMBER = "128"
        private const val SMS_NUMBER_2 = "1213"

        private const val SMS_SENT_ACTION = "SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    }
    private var lastKnownLocation: Location? = null

    private val _smsState = MutableLiveData<Pair<Boolean, String>?>().apply { value = null }
    val smsState: LiveData<Pair<Boolean, String>?> = _smsState
    fun updateLastKnownLocation(location: Location) {
        this.lastKnownLocation = location
        //  Log.d(TAG, "Updated lastKnownLocation in UploadRepository: $location")
    }
    private val sendSMSBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when(resultCode) {
                Activity.RESULT_OK -> { _smsState.postValue(Pair(true, "SMS_SENT")) }
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> { _smsState.postValue(Pair(false, "RESULT_ERROR_GENERIC_FAILURE")) }
                SmsManager.RESULT_ERROR_NO_SERVICE -> { _smsState.postValue(Pair(false, "RESULT_ERROR_NO_SERVICE")) }
                SmsManager.RESULT_ERROR_NULL_PDU -> { _smsState.postValue(Pair(false, "RESULT_ERROR_NULL_PDU")) }
                SmsManager.RESULT_ERROR_RADIO_OFF -> { _smsState.postValue(Pair(false, "RESULT_ERROR_RADIO_OFF")) }
            }
        }
    }

    private val deliverSMSBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when(resultCode) {
                Activity.RESULT_OK -> { _smsState.postValue(Pair(true, "SMS_DELIVERED")) }
                Activity.RESULT_CANCELED -> { _smsState.postValue(Pair(false, "RESULT_CANCELED")) }
            }
        }
    }

    init {
        context.registerReceiver(sendSMSBroadcastReceiver, IntentFilter(SMS_SENT_ACTION));
        context.registerReceiver(deliverSMSBroadcastReceiver, IntentFilter(SMS_DELIVERED_ACTION));
    }

    fun clear() {
        _smsState.postValue(null)
    }

    // MDN -> Phone number added in the application on SIMFragment level
    // MRT -> CardNumber + 0 + PersonalNumber get from the identity object (populated in SmartCardFragment)
    fun sendUSSD(phoneNumberText: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")

        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber

        //USSD content : "*128*MDN*MRT#"
        val ussdContent = "*128*${phoneNumber}*${cardNumber}0${personalNumber}#"
        Log.d(TAG, "sendUSSD: content -> $ussdContent")

        val mmi = Uri.Builder()
        mmi.scheme("tel")
        mmi.opaquePart(ussdContent)

        val intent = Intent(Intent.ACTION_CALL, mmi.build())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // MSISDN -> Unique Id for phone communication (Phone Number)
    // IMSI -> SIM Unique Id
    fun sendUSSD(msisdnText: String, imsi: String, kind: USSDKind) {
        val msisdn = msisdnText.replace(" ", "")

        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber

        val locationString = lastKnownLocation?.let { "${it.latitude}*${it.longitude}" } ?: "0*0"

        val ussdContent = when (kind) {


            //USSD content : "153*1*MSISDN*IMSI*MRT#"
            USSDKind.CLASSIC -> "*153*1*${msisdn}*${imsi}*${cardNumber}0${personalNumber}*${locationString}#"
            //USSD content : "153*2*MSISDN*codeMRT#"
            else -> "*153*2*${msisdn}*${cardNumber}0${personalNumber}*${locationString}#"
        }

        Log.d(TAG, "sendUSSD: content -> $ussdContent")

        val mmi = Uri.Builder()
        mmi.scheme("tel")
        mmi.opaquePart(ussdContent)

        val intent = Intent(Intent.ACTION_CALL, mmi.build())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    fun sendUSSDMattel(msisdnText: String, imsi: String,isFinger:Boolean) {
        val msisdn = msisdnText.replace(" ", "")

        //val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber
        val locationString = lastKnownLocation?.let { "${it.latitude}*${it.longitude}" } ?: "0*0"


        //val nbr =  peripheralAccess.capturedTemplateList.value?.nbTemplate
        val optionStr = if(isFinger) "" else "0000"

        // FIXED: Added FLAG_IMMUTABLE
        val sentPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        val ussdContent = "4${optionStr}*${msisdn}*${imsi}*${personalNumber}*${locationString}#"
        SmsManager.getDefault().sendTextMessage(SMS_NUMBER_MATTEL, null, ussdContent, sentPI2, deliveredPI2)

        //        Log.d(TAG, "sendUSSD: content -> $ussdContent")
//
//        val mmi = Uri.Builder()
//        mmi.scheme("tel")
//        mmi.opaquePart(ussdContent)
//
//        val intent = Intent(Intent.ACTION_CALL, mmi.build())
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        context.startActivity(intent)
    }

    // MDN -> Phone number added in the application on SIMFragment level
    // MRT -> CardNumber + 0 + PersonalNumber get from the identity object (populated in SmartCardFragment)
    fun sendSMS(phoneNumberText: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")

        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber

        // FIXED: Added FLAG_IMMUTABLE
        val sentPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        //Message to send : "$MDN*$MRT"
        val smsContent = "${phoneNumber}*${cardNumber}0${personalNumber}"

        Log.d(TAG, "sendSMS: content -> $smsContent")

        SmsManager.getDefault().sendTextMessage(SMS_NUMBER, null, smsContent, sentPI, deliveredPI)
    }
    fun sendSMS(phoneNumberText: String, imsi: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")

        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber

        // FIXED: Added FLAG_IMMUTABLE
        val sentPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        //Message to send : "$MDN*$MRT"
        if(imsi.length >0){
            val smsContent = "*0000*${phoneNumber}*${imsi}*${cardNumber}0${personalNumber}#"
            Log.d(TAG, "sendSMS: content -> $smsContent")
            SmsManager.getDefault().sendTextMessage(SMS_NUMBER_2, null, smsContent, sentPI, deliveredPI)

        }
        else{
            // val activity = context as Activity
            // val imei:String? = getIMEI(activity)
            //*1328579403*${imei}
            val smsContent = "${phoneNumber}*${cardNumber}0${personalNumber}*1328579403*355288103080737"

            Log.d(TAG, "sendSMS: content -> $smsContent")

            SmsManager.getDefault().sendTextMessage(SMS_NUMBER, null, smsContent, sentPI, deliveredPI)
        }
    }

    fun sendSMSMattel(phoneNumberText: String, imsi: String,isFinger : Boolean) {
        val phoneNumber = phoneNumberText.replace(" ", "")

        val nni = peripheralAccess.identity.value?.personalNumber;
        val cardNumber = peripheralAccess.cardNumber.value
        val nom = CleanString(peripheralAccess.identity.value?.firstName.toString());
        val prenom =CleanString(peripheralAccess.identity.value?.lastName.toString());

        val sexeFull = CleanString(peripheralAccess.identity.value?.sex.toString());
        val sexe = sexeFull.subSequence(0,1).toString().uppercase();

        val date_naissFull = CleanString(peripheralAccess.identity.value?.dateOfBirth.toString());
        //Log.d(TAG, "sendSMS: content -> dd"+date_naiss.subSequence(8,2))
        //Log.d(TAG, "sendSMS: content -> dd"+date_naiss.subSequence(5,3))
        //Log.d(TAG, "sendSMS: content -> yyyy"+date_naiss.takeLast(4))
        val db = DBHelper(context, null)

        // creating variables for values
        // in name and age edit texts

        // calling method to add
        // name to our database
        db.addAbonne(nni.toString(), phoneNumber.toString())

        // Toast to message on the screen
        // Toast.makeText(this, name + " added to database", Toast.LENGTH_LONG).show()
        val locationString = lastKnownLocation?.let { "${it.latitude}*${it.longitude}" } ?: "0*0"


        Log.d(TAG, "savedDb: content -> ${nni} tel ${phoneNumber} ")

        //  val nbr =  peripheralAccess.capturedTemplateList.value?.nbTemplate
        val optionStr = if(isFinger) "" else "0000"

        val lieu_naissance = CleanString(peripheralAccess.identity.value?.placeOfBirth.toString());
        //yyyy-mm-dd
        val date_naiss =date_naissFull.takeLast(4)+"-"+getNumberMonth(date_naissFull.subSequence(4,8).toString().trim())+"-"+date_naissFull.subSequence(8,11).toString().trim()

        val localisation="0,0"

        // FIXED: Added FLAG_IMMUTABLE
        val sentPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)
        var smsContent="";
        if(imsi.length >6)
            smsContent ="1${optionStr}*${phoneNumber}*${imsi}*${nni}*${nom}*${prenom}*${date_naiss}*${lieu_naissance}*${sexe}*${locationString}";  //*${id_appareil}

        else
            smsContent ="2${optionStr}*${phoneNumber}*${nni}*${nom}*${prenom}*${date_naiss}*${lieu_naissance}*${sexe}*${locationString}"; //*${id_appareil}




        Log.d(TAG, "sendSMS: content -> $smsContent")


        SmsManager.getDefault().sendTextMessage(SMS_NUMBER_MATTEL, null, smsContent, sentPI2, deliveredPI2)
    }

    fun sendModificationMattel(phoneNumberText: String,isFinger:Boolean) {
        val phoneNumber = phoneNumberText.replace(" ", "")

        val locationString = lastKnownLocation?.let { "${it.latitude}*${it.longitude}" } ?: "0*0"
        val nni = peripheralAccess.identity.value?.personalNumber;
        val cardNumber = peripheralAccess.cardNumber.value
        val nom = CleanString(peripheralAccess.identity.value?.firstName.toString());
        val prenom =CleanString(peripheralAccess.identity.value?.lastName.toString());

        val sexeFull = CleanString(peripheralAccess.identity.value?.sex.toString());
        val sexe = sexeFull.subSequence(0,1).toString().uppercase();

        val date_naissFull = CleanString(peripheralAccess.identity.value?.dateOfBirth.toString());
        val lieu_naissance = CleanString(peripheralAccess.identity.value?.placeOfBirth.toString());
        //yyyy-mm-dd
        val date_naiss =date_naissFull.takeLast(4)+"-"+getNumberMonth(date_naissFull.subSequence(4,8).toString().trim())+"-"+date_naissFull.subSequence(8,11).toString().trim()


        // FIXED: Added FLAG_IMMUTABLE
        val sentPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI2 = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)
        // val nbr =  peripheralAccess.capturedTemplateList.value?.nbTemplate
        val optionStr = if(isFinger) "" else "0000"

        var smsContent="";
        //3*msisdn*mrz
        smsContent ="3${optionStr}*${phoneNumber}*${nni}*${nom}*${prenom}*${date_naiss}*${lieu_naissance}*${sexe}*${locationString}"; //*${id_appareil}






        Log.d(TAG, "sendSMS: content -> $smsContent")


        SmsManager.getDefault().sendTextMessage(SMS_NUMBER_MATTEL, null, smsContent, sentPI2, deliveredPI2)
    }

    fun  CleanString(str:String):String{
        var answer = str
        //println(answer)
        //à|â|é|è|ê|ë|ï|î|ô|ù|û|ç|œ|æ|'
        answer = answer.replace("[^A-Za-z0-9|à|â|é|è|ê|ë|ï|î|ô|ù|û|ç|œ|æ|' ]", "") // doesn't work
        //println(answer)
        val re = Regex("[^A-Za-z0-9|à|â|é|è|ê|ë|ï|î|ô|ù|û|ç|œ|æ|' ]")
        answer = re.replace(answer, "") // works
        return answer.trim()
        //println(answer);
    }
    fun getNumberMonth(str:String):String{
        when (str) {
            "Jan" -> return "01"
            "Feb" -> return "02"
            "Mar" -> return "03"
            "Apr" -> return "04"
            "May" -> return "05"
            "Jun" -> return "06"
            "Jul" -> return "07"
            "Aug" -> return "08"
            "Sep" -> return "09"
            "Oct" -> return "10"
            "Nov" -> return "11"
            "Dec" -> return "12"

        }
        return ""
    }
    fun getIMEI(activity: Activity): String? {
        val telephonyManager = activity
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.deviceId
    }
    fun sendOTP(phoneNumberText: String, otp: String) {
        val phoneNumber = phoneNumberText.replace(" ", "")

        // FIXED: Added FLAG_IMMUTABLE
        val sentPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE)
        val deliveredPI = PendingIntent.getBroadcast(context, 0, Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE)

        val smsContent = "100*${phoneNumberText}*${otp}"
        SmsManager.getDefault().sendTextMessage(SMS_NUMBER_MATTEL, null, smsContent, sentPI, deliveredPI)

    }
}
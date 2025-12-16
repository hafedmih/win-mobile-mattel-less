package com.famoco.kyctelcomr.core.repositories

import android.graphics.Bitmap
import android.graphics.Matrix
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.famoco.kyctelcomr.core.model.Customer
import com.famoco.kyctelcomr.core.utils.CustomerBuilder
import com.famoco.kyctelcomr.face.FaceViewModel
import com.famoco.kyctelcomr.face.services.facenet.FaceNetService
import com.famoco.kyctelcomr.face.services.mtcnn.MTCNN
import com.famoco.kyctelcomrtlib.PeripheralAccess
import com.famoco.kyctelcomrtlib.smartcard.FingerEnum
import com.morpho.morphosmart.sdk.TemplateList
import fr.sabry.wsq.WSQDecoder
import fr.sabry.wsq.WSQEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val peripheralAccess: PeripheralAccess
) {

    companion object {
        private val TAG = MainRepository::class.java.simpleName
    }

    val sensorQuality = peripheralAccess.sensorQuality
    val sensorImage = peripheralAccess.sensorImage
    val sensorMessage = peripheralAccess.sensorMessage
    val onCaptureCompleted = peripheralAccess.onCaptureCompleted
    val morphoInternalError = peripheralAccess.morphoInternalError
    val capturedTemplateList = peripheralAccess.capturedTemplateList

    val currentOperation = peripheralAccess.currentOperation
    val readerState = peripheralAccess.readerState
    val cardState = peripheralAccess.cardState
    val cardNumber = peripheralAccess.cardNumber
    val matchLiveData = peripheralAccess.matchLiveData
    val attemptLeft = peripheralAccess.attemptLeft
    val apduErrorMessage = peripheralAccess.apduErrorMessage
    val identity = peripheralAccess.identity

    private val _customer = MutableLiveData<Customer>().apply { value = CustomerBuilder.build() }
    val customer: LiveData<Customer?> = _customer

    fun setCustomer(customer: Customer) {
        _customer.postValue(customer)
    }

    fun setCustomerTemplates(templateList: TemplateList) {
        _customer.value?.templates = templateList
    }

    fun reset() {
        _customer.postValue(CustomerBuilder.build())
        peripheralAccess.reset()
    }

    fun reboot() {
        _customer.postValue(CustomerBuilder.build())
        peripheralAccess.reboot()
    }

    fun destroy() {
        _customer.postValue(CustomerBuilder.build())
        peripheralAccess.destroy()
    }

    fun startFingerCapture() {
        peripheralAccess.startFingerCapture()
    }

    fun stopFingerCapture() {
        peripheralAccess.stopFingerCapture()
    }

    fun askCardNumber(isoDep: IsoDep) {
        peripheralAccess.askCardNumber(isoDep)
    }

    fun askIdentity(isoDep: IsoDep) {
        peripheralAccess.askIdentity(isoDep)
    }
//
    fun matchOnCardCurrentCustomer(isoDep: IsoDep,chosenFinger: FingerEnum) {
        customer.value?.templates?.getTemplate(0)?.data?.let{
            peripheralAccess.matchOnCard(isoDep,it, chosenFinger)
            //peripheralAccess.mockMatchOnCard(chosenFinger)
        } ?: Log.w(TAG, "no biometric data catch from client => can't do match")

    }
    fun matchFace() {
      val photo=  identity.value?.photo


//        customer.value?.templates?.getTemplate(0)?.data?.let{
//            peripheralAccess.matchOnCard(it, chosenFinger)
//            //peripheralAccess.mockMatchOnCard(chosenFinger)
//        } ?: Log.w(TAG, "no biometric data catch from client => can't do match")

    }

//    fun matchOnCardCurrentCustomer(isoDep: IsoDep, chosenFinger: FingerEnum) {
//        customer.value?.templates?.getTemplate(0)?.data?.let { originalTemplateBytes ->
//            try {
//                // 1. Decode WSQ to Bitmap
//                val morphoImageBitmap = WSQDecoder.decode(originalTemplateBytes).bitmap
//
//                // 2. Rotate Bitmap 180 degrees using Matrix
//                val matrix = Matrix()
//                matrix.postRotate(180f)
//
//                val rotateBitmap = Bitmap.createBitmap(
//                    morphoImageBitmap,
//                    0,
//                    0,
//                    morphoImageBitmap.width,
//                    morphoImageBitmap.height,
//                    matrix,
//                    true
//                )
//
//                // 3. Encode back to WSQ
//                val rotatedWsqData: ByteArray = WSQEncoder(rotateBitmap)
//                    .setBitrate(WSQEncoder.BITRATE_5_TO_1)
//                    .encode()
//
//                // 4. Send to card
//                peripheralAccess.matchOnCard(isoDep, rotatedWsqData, chosenFinger)
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error rotating/matching fingerprint: ${e.message}")
//            }
//
//        } ?: Log.w(TAG, "no biometric data catch from client => can't do match")
//    }

}
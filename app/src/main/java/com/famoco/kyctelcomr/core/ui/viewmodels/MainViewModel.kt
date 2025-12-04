package com.famoco.kyctelcomr.core.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.nfc.tech.IsoDep
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famoco.kyctelcomr.core.model.Customer
import com.famoco.kyctelcomr.core.repositories.*
import com.famoco.kyctelcomrtlib.smartcard.FingerEnum
import com.morpho.morphosmart.sdk.TemplateList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainRepository: MainRepository,
    private val usbRepository: USBRepository,
    private val uploadRepository: UploadRepository) : ViewModel() {

    companion object {
        private val TAG = MainViewModel::class.java.simpleName
    }

    private val dispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher()
    private val coroutineScope = CustomCoroutineScope()

    private val _capturedImage = MutableLiveData<Bitmap?>()
    val capturedImage: LiveData<Bitmap?> = _capturedImage

    inner class CustomCoroutineScope internal constructor() : CoroutineScope {
        override val coroutineContext: CoroutineContext =
            dispatcher + Job() + CoroutineExceptionHandler { _, throwable: Throwable ->
                GlobalScope.launch { println("Caught $throwable") }
            }
    }

    val smartCardReaderPlugged = usbRepository.smartCardReaderPluggedLiveData

    private val _applyMatchingResult = MutableLiveData<Boolean?>().apply { value = null }
    val applyMatchingResult: LiveData<Boolean?> = _applyMatchingResult

    private val _chosenFinger = MutableLiveData<FingerEnum>().apply { value = FingerEnum.RIGHT }

    //Morpho LiveDatas
    val sensorQuality = mainRepository.sensorQuality

    val sensorImage = mainRepository.sensorImage

    val sensorMessage = mainRepository.sensorMessage

    val onCaptureCompleted = mainRepository.onCaptureCompleted

    val morphoInternalError = mainRepository.morphoInternalError

    val capturedTemplateList = mainRepository.capturedTemplateList

    //SmartCard LiveDatas
    val readerState = mainRepository.readerState

    val cardState = mainRepository.cardState

    val cardNumber = mainRepository.cardNumber

    val matchLiveData = mainRepository.matchLiveData

    val attemptLeft = mainRepository.attemptLeft

    val apduErrorMessage = mainRepository.apduErrorMessage

    val identity = mainRepository.identity

    val customer: LiveData<Customer?> = mainRepository.customer

    val smartCardReaderScanState = mainRepository.currentOperation


    private var lastProcessedIdentityNumber: String? = null



    fun reset() {
        _applyMatchingResult.postValue(null)
        _capturedImage.postValue(null)

        lastProcessedIdentityNumber = null


        mainRepository.reset()
        uploadRepository.clear()
    }

    fun setCustomerTemplates(templateList: TemplateList) {
        mainRepository.setCustomerTemplates(templateList)
    }

    fun captureFingerprint() {
        coroutineScope.launch(dispatcher) {
            mainRepository.startFingerCapture()
        }
        lastProcessedIdentityNumber = null

    }

    fun stopCapture() {
        mainRepository.stopFingerCapture()
    }

    fun rebootSoft() {
        _applyMatchingResult.postValue(null)
        mainRepository.reboot()
    }

    fun destroy() {
        _applyMatchingResult.postValue(null)
        coroutineScope.launch(dispatcher) {
            mainRepository.destroy()
        }
    }

    fun updateChosenFinger(chosenFinger: FingerEnum) {

        lastProcessedIdentityNumber = null

        _chosenFinger.postValue(chosenFinger)
    }

    fun getCardNumber(isoDep: IsoDep) {
        mainRepository.askCardNumber(isoDep)
    }

    fun getIdentity(isoDep: IsoDep) {
        mainRepository.askIdentity(isoDep)
    }

    fun matchOnCard(isoDep: IsoDep) {
        val currentIdentity = identity.value

        if (currentIdentity == null || currentIdentity.personalNumber.isEmpty()) {
            return
        }

        // 6. SET FLAG: Mark as performed so subsequent LiveData updates don't trigger it again
        if (currentIdentity.personalNumber == lastProcessedIdentityNumber) {
            // We already matched for this person.
            // This call is likely due to Fragment recreation/Navigation. Ignore it.
            return
        }
        lastProcessedIdentityNumber = currentIdentity.personalNumber

        mainRepository.matchOnCardCurrentCustomer(isoDep,_chosenFinger.value!!)
    }

    fun notifyMatchResult(res: Boolean) {
        viewModelScope.launch {
            delay(3500)
            _applyMatchingResult.postValue(res)
        }
    }
    fun updateCapturedImage(bitmap: Bitmap) {
        _capturedImage.postValue(bitmap)
    }

    fun clearCapturedImage() {
        _capturedImage.postValue(null)
    }
    fun matchFace() {
        mainRepository.matchFace()
    }
    private val _detectedIsoDep = MutableLiveData<IsoDep?>()
    val detectedIsoDep: LiveData<IsoDep?> = _detectedIsoDep

    // Function to set it from MainActivity
    fun setDetectedIsoDep(isoDep: IsoDep) {
        _detectedIsoDep.postValue(isoDep)
    }
}


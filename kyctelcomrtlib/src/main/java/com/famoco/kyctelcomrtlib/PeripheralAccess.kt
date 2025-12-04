package com.famoco.kyctelcomrtlib

import android.content.Context
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.famoco.kyctelcomrtlib.biometrics.MorphoConnectionState
import com.famoco.kyctelcomrtlib.biometrics.MorphoController
import com.famoco.kyctelcomrtlib.smartcard.FingerEnum
import com.famoco.kyctelcomrtlib.smartcard.ReaderState
import com.famoco.kyctelcomrtlib.smartcard.SmartCardController
import com.famoco.kyctelcomrtlib.utils.CombinedLiveData
import com.famoco.kyctelcomrtlib.utils.MergedLiveData
import kotlinx.coroutines.delay

class PeripheralAccess(context: Context) {
    companion object {
        private val TAG = PeripheralAccess::class.simpleName
    }

    private val morphoController = MorphoController(context)
    private val smartCardController = SmartCardController(context)

    //Main LiveDatas
    private val _connectedLiveData = CombinedLiveData(morphoController.connectionState,
        smartCardController.readerStateLiveData) {
                s1, s2 ->
        s1 == MorphoConnectionState.MORPHO_READY && s2 == ReaderState.INITIALIZED
    }.apply { value = false }
    val connectedLiveData: LiveData<Boolean> = _connectedLiveData

    private val _initializationLiveData = MutableLiveData<InitializationState>().apply { value = InitializationState.NONE }
    val initializationLiveData: LiveData<InitializationState> = _initializationLiveData

    private val _connectionErrorLiveData = MergedLiveData(morphoController.connectionError,
        smartCardController.connectionError).apply { value = "" }
    val connectionErrorLiveData: LiveData<String> = _connectionErrorLiveData

    //Morpho LiveDatas
    val morphoConnectionState = morphoController.connectionState

    val morphoConnectionError = morphoController.connectionError

    val sensorQuality = morphoController.sensorQuality

    val sensorImage = morphoController.sensorImage

    val sensorMessage = morphoController.sensorMessage

    val onCaptureCompleted = morphoController.onCaptureCompleted

    val morphoInternalError = morphoController.morphoInternalError

    val capturedTemplateList = morphoController.capturedTemplateList

    //SmartCard LiveDatas
    val readerState = smartCardController.readerStateLiveData

    val cardState = smartCardController.cardStateLiveData

    val currentOperation = smartCardController.currentOperation

    val cardNumber = smartCardController.cardNumberLiveData

    val matchLiveData = smartCardController.matchLiveData

    val attemptLeft = smartCardController.attemptLeft

    val apduErrorMessage = smartCardController.apduErrorMessageLiveData

    val identity = smartCardController.identityLiveData

    suspend fun init(permission: String, slow: Boolean) {
        Log.i(TAG, "init")
        _initializationLiveData.postValue(InitializationState.LOADING_MORPHO)
        if (slow) {
            delay(1000)
        }
        if (!morphoController.init(permission)) {
            _initializationLiveData.postValue(InitializationState.NONE)
            return
        }
        _initializationLiveData.postValue(InitializationState.LOADING_SMARTCARD_READER)
        if (slow) {
            delay(1000)
        }
        smartCardController.init()
        _initializationLiveData.postValue(InitializationState.NONE)
    }

    fun destroy() {
        Log.i(TAG, "destroy")
        _connectedLiveData.postValue(false)
        smartCardController.unInit()
        //morphoController.clo()
    }

    fun reset() {
        Log.i(TAG, "reset")
        _connectedLiveData.postValue(false)
        smartCardController.resetLiveData()
        morphoController.reset()
    }

    fun reboot() {
        Log.i(TAG, "reboot")
        _connectedLiveData.postValue(false)
        smartCardController.unInit()
        smartCardController.init()
      //  morphoController.rebootSoftware()
    }

    fun startFingerCapture() {
        Log.i(TAG, "startFingerCapture")
        morphoController.morphoDeviceCapture()
    }

    fun stopFingerCapture() {
        Log.i(TAG, "stopFingerCapture")
        morphoController.stopCapture()
    }

    fun askCardNumber(isoDep: IsoDep) {
        Log.i(TAG, "askCardNumber")
        smartCardController.askCardNumber(isoDep)
    }

    fun askIdentity(isoDep: IsoDep) {
        Log.i(TAG, "askIdentity")
        smartCardController.askIdentity(isoDep)
    }

    fun matchOnCard(isoDep: IsoDep,template: ByteArray, chosenFinger: FingerEnum) {
        Log.i(TAG, "matchOnCard")
        smartCardController.askBiometry(isoDep,template, chosenFinger)
    }

    fun mockMatchOnCard(chosenFinger: FingerEnum) {
        Log.i(TAG, "mockMatchOnCard")
        smartCardController.askMockBiometry(chosenFinger)
    }
}
package com.famoco.kyctelcomrtlib.biometrics

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.morpho.android.usb.USBManager
import com.morpho.morphosmart.sdk.*
import java.nio.ByteBuffer
import java.util.*

class MorphoController(private val context: Context) : Observer {
    companion object {
        private val TAG = MorphoController::class.simpleName

        // Constants usually defined in Famoco documentation
        // For FP200, the port is typically "/dev/ttyS1" and speed 115200
    //   private const val UART_PORT = "/dev/ttyS1"
       //  private const val UART_PORT = "/dev/ttyS0"
// private const val UART_PORT = "/dev/ttyS2"
 private const val UART_PORT = "/dev/ttyMT1"
        private const val UART_SPEED = 57600
        //    private const val UART_SPEED = 115200
    }

    private val morphoDevice = MorphoDevice()

    private val TEMPLATE_TYPE = TemplateType.MORPHO_PK_ISO_FMC_CS
    private val TEMPLATE_FVP_TYPE = TemplateFVPType.MORPHO_NO_PK_FVP
    private val ENROLL_TYPE = EnrollmentType.ONE_ACQUISITIONS
    private val MAX_SIZE_TEMPLATE = 40
    private val LATENT_DETECTION = LatentDetection.LATENT_DETECT_ENABLE
    private val NB_FINGER = 1

    // LiveData for UI updates
    private val _connectionState = MutableLiveData<MorphoConnectionState?>().apply { value = null }
    val connectionState: LiveData<MorphoConnectionState?> = _connectionState

    private val _connectionError = MutableLiveData<String>().apply { value = "" }
    val connectionError: LiveData<String> = _connectionError

    private val _sensorQuality = MutableLiveData<Int>().apply { value = 0 }
    val sensorQuality: LiveData<Int> = _sensorQuality

    private val _sensorImage = MutableLiveData<Bitmap>().apply { value = null }
    val sensorImage: LiveData<Bitmap> = _sensorImage

    private val _sensorMessage = MutableLiveData<Int>().apply { value = 0 }
    val sensorMessage: LiveData<Int> = _sensorMessage

    private val _onCaptureCompleted = MutableLiveData<Boolean>().apply { value = false }
    val onCaptureCompleted: LiveData<Boolean> = _onCaptureCompleted

    private val _morphoInternalError = MutableLiveData<Pair<Int, Int>>().apply { value = Pair(0, 0) }
    val morphoInternalError: LiveData<Pair<Int, Int>> = _morphoInternalError

    private val _capturedTemplateList = MutableLiveData<TemplateList?>().apply { value = null }
    val capturedTemplateList: LiveData<TemplateList?> = _capturedTemplateList

    init {
        ProcessInfo.getInstance().morphoDevice = morphoDevice
    }

    /**
     * Helper from Utils.java
     */
    private fun isFP200(): Boolean {
        return Build.MODEL.uppercase(Locale.ROOT).contains("FP200")
    }

    fun init(permission: String): Boolean {
        // Based on HomeActivity / HomePresenter logic:
        if (isFP200()) {
            Log.d(TAG, "FP200 Detected: Skipping USB Init, using UART")
            // FP200 uses UART, so we don't need USB Permissions.
            // Go straight to connection.
            wakeUpSensor()
            return connection()
        } else {
            // FX200 or other USB devices
            Log.d(TAG, "Standard Device Detected: Using USB Manager")
            processUSBPermission(permission)
            if (USBManager.getInstance().isDevicesHasPermission) {
                return connection()
            }
        }
        return false
    }

    fun reset() {
        stopCapture()
        _connectionError.postValue("")
        _sensorQuality.postValue(0)
        _sensorImage.postValue(null)
        _sensorMessage.postValue(0)
        _onCaptureCompleted.postValue(false)
        _morphoInternalError.postValue(Pair(0, 0))
        _capturedTemplateList.postValue(null)
    }

    fun stopCapture() {
        try {
            morphoDevice.cancelLiveAcquisition()
            morphoDevice.closeDevice()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Logic from ConnectionPresenter.connection()
     */
    private fun connection(): Boolean {
        val ret: Int

        if (isFP200()) {
            Log.d(TAG, "Attempting UART Connection on $UART_PORT at $UART_SPEED")
            // The method from your snippet: openDeviceWithUart
            ret = morphoDevice.openDeviceWithUart(UART_PORT, UART_SPEED)
        } else {
            Log.d(TAG, "Attempting USB Connection")
            // For non-FP200 (USB), we use index 0
            ret = morphoDevice.openUsbDevice(0.toString(), 2000)
        }

        Log.d(TAG, "\t--> OpenDevice returned: $ret")

        if (ret == ErrorCodes.MORPHO_OK) {
            // Success! Set up data
            initMorphoDeviceData()
            postConnectionInfo(MorphoConnectionState.CONNECTION_ESTABLISHED)

            val productDescriptor = morphoDevice.productDescriptor
            if (productDescriptor.contains("FINGER VP") || productDescriptor.contains("FVP")) {
                com.famoco.kyctelcomrtlib.biometrics.MorphoInfo.setM_b_fvp(true)
            }

            // Note: The snippet closes the device immediately after checking connection.
            // We usually want to close it so it's ready for the Capture call.
            morphoDevice.closeDevice()

            postConnectionInfo(MorphoConnectionState.MORPHO_READY)
            return true
        } else {
            postConnectionInfo(MorphoConnectionState.MORPHO_INTERNAL_ERROR, ErrorCodes.getError(ret, morphoDevice.internalError))
        }
        return false
    }

    /**
     * Logic from ConnectionPresenter.initMorphoDeviceData()
     * Includes the specific RS232 config for FP200
     */
    private fun initMorphoDeviceData() {
        val sensorName = if (isFP200()) UART_PORT else "USB:0"
        ProcessInfo.getInstance().msoSerialNumber = sensorName
        ProcessInfo.getInstance().msoBus = -1
        ProcessInfo.getInstance().msoAddress = -1
        ProcessInfo.getInstance().msofd = -1
        ProcessInfo.getInstance().msoDetectionMode = DeviceDetectionMode.SdkDetection

        // SPECIFIC FP200 CONFIGURATION FROM SNIPPET
        if (isFP200()) {
            Log.d(TAG, "Applying FP200 RS232 Configuration...")

            // Set CONFIG_RS232_PREVIEW_BPP = 4
            var ret = morphoDevice.setConfigParam(MorphoDevice.CONFIG_RS232_PREVIEW_BPP, byteArrayOf(4))
            if (ret != ErrorCodes.MORPHO_OK) {
                Log.w(TAG, "Failed to set RS232_PREVIEW_BPP: $ret")
            }

            // Set CONFIG_RS232_PREVIEW_DR = 2
            ret = morphoDevice.setConfigParam(MorphoDevice.CONFIG_RS232_PREVIEW_DR, byteArrayOf(2))
            if (ret != ErrorCodes.MORPHO_OK) {
                Log.w(TAG, "Failed to set RS232_PREVIEW_DR: $ret")
            }
            Log.d(TAG, "FP200 Configuration Applied.")
        }
    }

    fun morphoDeviceCapture() {
        _onCaptureCompleted.postValue(false)

        val retOpen: Int = if (isFP200()) {
            morphoDevice.openDeviceWithUart(UART_PORT, UART_SPEED)
        } else {
            morphoDevice.openUsbDevice(0.toString(), 2000)
        }

        if (retOpen == ErrorCodes.MORPHO_OK) {
            // Apply config again just to be safe if connection was closed
            if (isFP200()) initMorphoDeviceData()

            // ... Start Capture logic (Standard) ...
            val processInfo = ProcessInfo.getInstance()
            val templateList = TemplateList()

            // Use defaults or values from ProcessInfo
            val timeout = processInfo.timeout
            val acquisitionThreshold = 0
            val advancedSecurityLevelsRequired = 0xFF
            val callbackCmd = processInfo.callbackCmd
            val coderChoice = processInfo.coder
            val detectModeChoice = DetectionMode.MORPHO_ENROLL_DETECT_MODE.value

            val ret = morphoDevice.capture(
                timeout,
                acquisitionThreshold,
                advancedSecurityLevelsRequired,
                NB_FINGER,
                TEMPLATE_TYPE,
                TEMPLATE_FVP_TYPE,
                MAX_SIZE_TEMPLATE,
                ENROLL_TYPE,
                LATENT_DETECTION,
                coderChoice,
                detectModeChoice,
                CompressionAlgorithm.MORPHO_NO_COMPRESS,
                0,
                templateList,
                callbackCmd,
                this
            )

            val internalError = morphoDevice.internalError

            // IMPORTANT: Close device after capture attempt
            stopCapture()

            if (ret == ErrorCodes.MORPHO_OK) {
                _onCaptureCompleted.postValue(true)
                _capturedTemplateList.postValue(templateList)
            } else {
                _morphoInternalError.postValue(Pair(ret, internalError))
            }

        } else {
            Log.e(TAG, "Capture failed: Could not open device. Ret: $retOpen")
            _connectionError.postValue("Could not open sensor")
        }
    }

    private fun processUSBPermission(permission: String) {
        if (!isFP200()) {
            USBManager.getInstance().initialize(context, permission)
        }
    }

    private fun postConnectionInfo(state: MorphoConnectionState, error: String = "") {
        Log.i(TAG, "post connection info: ${state.name}, error: $error")
        _connectionState.postValue(state)
        if (error.isNotEmpty()) {
            _connectionError.postValue(error)
        }
    }

    // Callbacks
    override fun update(p0: Observable?, arg: Any?) {
        val message = arg as CallbackMessage?
        message?.let {
            when (it.messageType) {
                1 -> handleCommand(message.message as Int)
                2 -> handleImage(message.message as ByteArray)
                3 -> handleQuality(message.message as Int)
            }
        }
    }

    private fun handleQuality(quality: Int) { _sensorQuality.postValue(quality) }
    private fun handleImage(image: ByteArray) { _sensorImage.postValue(toBitmap(image)) }
    private fun handleCommand(command: Int) { _sensorMessage.postValue(command) }

    private fun toBitmap(image: ByteArray): Bitmap {
        val morphoImage = MorphoImage.getMorphoImageFromLive(image)
        val imageRowNumber = morphoImage.morphoImageHeader.nbRow
        val imageColumnNumber = morphoImage.morphoImageHeader.nbColumn
        val imageBmp = Bitmap.createBitmap(imageColumnNumber, imageRowNumber, Bitmap.Config.ALPHA_8)
        imageBmp.copyPixelsFromBuffer(ByteBuffer.wrap(morphoImage.image, 0, morphoImage.image.size))
        return imageBmp
    }

    private fun wakeUpSensor() {
        if (isFP200()) {
            try {
                // This is the standard GPIO path for FP200 sensor power
                // Note: This might require root/system permissions too
                Runtime.getRuntime().exec("echo 1 > /sys/class/gpio/gpio122/value") // Example GPIO
                Thread.sleep(200) // Wait for boot
            } catch (e: Exception) {
                Log.e(TAG, "Failed to wake up sensor GPIO", e)
            }
        }
    }
}
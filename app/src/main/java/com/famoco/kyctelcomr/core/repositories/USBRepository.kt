package com.famoco.kyctelcomr.core.repositories

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.morpho.android.usb.USBManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class USBRepository @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private val TAG = USBRepository::class.java.simpleName

        private const val MORPHO_VID = 8797
        private const val SMARTCARD_READER_VID = 9222
    }

    private val _smartCardReaderPluggedLiveData = MutableLiveData<Boolean?>().apply { value = null }
    val smartCardReaderPluggedLiveData: LiveData<Boolean?> = _smartCardReaderPluggedLiveData

    private var onUSBPermissionGrantedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (USBManager.ACTION_USB_PERMISSION == intent.action) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null && device.vendorId == SMARTCARD_READER_VID) {
                        Log.i(TAG, "\t --> Permission granted for device$device")
                        scanPluggedUSBDevices()
                    }
                } else {
                    // nothing to do
                }
            }
        }
    }

    private var onAttachUSBBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (device != null && device.vendorId == SMARTCARD_READER_VID) {
                    Log.i(TAG, "\t--> onReceive: USB Reader Attached $device")
                    _smartCardReaderPluggedLiveData.postValue(true)
                }
            }
        }
    }

    private var onDetachUSBBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (device != null && device.vendorId == SMARTCARD_READER_VID) {
                    Log.i(TAG, "\t--> onReceive: USB Reader Detached $device")
                    _smartCardReaderPluggedLiveData.postValue(false)
                }
            }
        }
    }

    init {
        initUSBBroadcastReceiver(onUSBPermissionGrantedReceiver, onAttachUSBBroadcastReceiver, onDetachUSBBroadcastReceiver)
        scanPluggedUSBDevices()
    }

    private fun scanPluggedUSBDevices() {
        val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        var found = false
        for (device in usbManager.deviceList.values) {
            if (device != null && device.vendorId == SMARTCARD_READER_VID) {
                Log.i(TAG, "\t--> USB Reader Attached $device")
                _smartCardReaderPluggedLiveData.postValue(true)
                found = true
                break
            }
        }
        if (!found) {
            Log.i(TAG, "\t--> USB Reader Not Attached")
            _smartCardReaderPluggedLiveData.postValue(false)
        }
    }

    private fun initUSBBroadcastReceiver(onPermissionGranted: BroadcastReceiver, onAttach: BroadcastReceiver, onDetach: BroadcastReceiver) {
        // Detach events are sent as a system-wide broadcast
        Log.i(TAG, "\t--> Register Receiver")
        context.registerReceiver(onPermissionGranted, IntentFilter(USBManager.ACTION_USB_PERMISSION))
        context.registerReceiver(onAttach, IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        context.registerReceiver(onDetach, IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))
    }

    class CustomCoroutineScope internal constructor() : CoroutineScope {
        private val dispatcher = Executors.newSingleThreadExecutor()
            .asCoroutineDispatcher()

        override val coroutineContext: CoroutineContext =
            dispatcher + Job() + CoroutineExceptionHandler { coroutineContext: CoroutineContext, throwable: Throwable ->
                GlobalScope.launch { println("Caught $throwable") }
            }
    }
}
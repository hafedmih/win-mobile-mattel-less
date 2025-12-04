package com.famoco.kyctelcomr.core.repositories

import android.content.Context
import android.util.Log
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomrtlib.PeripheralAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class SplashRepository @Inject constructor(@ApplicationContext private val context: Context, private val peripheralAccess: PeripheralAccess) {

    companion object {
        private val TAG = SplashRepository::class.java.simpleName
    }

    val connectedLiveData = peripheralAccess.connectedLiveData
    val initializationLiveData = peripheralAccess.initializationLiveData
    val connectionErrorLiveData = peripheralAccess.connectionErrorLiveData

    private val commandCoroutineScope = CustomCoroutineScope()

    fun init() {
        Log.i(TAG, "init")
        commandCoroutineScope.launch {
            peripheralAccess.init(context.resources.getString(R.string.ACTION_USB_PERMISSION), true)
        }
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
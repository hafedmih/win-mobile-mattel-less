package com.famoco.kyctelcomr.core.ui.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.core.repositories.SplashRepository
import com.famoco.kyctelcomr.core.repositories.USBRepository
import com.famoco.kyctelcomrtlib.InitializationState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(@ApplicationContext private val context: Context,
                                          private val splashRepository: SplashRepository,
                                          private val usbRepository: USBRepository
) :
    ViewModel() {

    companion object {
        private val TAG = SplashViewModel::class.java.simpleName
    }

    val loadingTxt: LiveData<String> = splashRepository.initializationLiveData.map {
        when (it) {
            InitializationState.NONE -> ""
            InitializationState.LOADING_SMARTCARD_READER -> context.getString(R.string.splash_smartcard_init)
            InitializationState.LOADING_MORPHO -> context.getString(R.string.splash_morpho_init)
        }
    }
    val smartCardReaderPlugged = usbRepository.smartCardReaderPluggedLiveData
    val initStateLiveData = splashRepository.connectedLiveData
    val connectionErrorLiveData = splashRepository.connectionErrorLiveData

    fun initModules() {
        viewModelScope.launch {
            splashRepository.init()
        }
    }
}
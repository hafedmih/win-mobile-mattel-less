package com.famoco.kyctelcomr.mattel2.ui.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.famoco.kyctelcomr.core.repositories.MainRepository
import com.famoco.kyctelcomr.core.repositories.NetworkRepository
import com.famoco.kyctelcomr.core.repositories.UploadRepository
import com.famoco.kyctelcomr.core.utils.SendDataEnum
import com.famoco.kyctelcomr.mattel.model.Customer
import com.famoco.kyctelcomr.mattel.model.USSDKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Level.INFO
import javax.inject.Inject

@HiltViewModel
class
MattelSummaryFragmentViewModel @Inject constructor(private val mainRepository: MainRepository,
                                                         private val uploadRepository: UploadRepository,
                                                         private val networkRepository:NetworkRepository
) :
    ViewModel() {

    companion object {
        private val TAG = MattelSummaryFragmentViewModel::class.java.simpleName
    }

    val customer = mainRepository.customer.map { if (it is Customer) it else null}
    val identity = mainRepository.identity
    val cardNumber = mainRepository.cardNumber



    private val _sendData = MutableLiveData<SendDataEnum>().apply { value = SendDataEnum.USSD }
    val sendData: LiveData<SendDataEnum> = _sendData
    val smsState = uploadRepository.smsState

    private val _messageSendResult = MutableLiveData<Boolean?>().apply { value = null }
    val messageSendResult: LiveData<Boolean?> = _messageSendResult

    fun notifyMessageSendResult(result: Boolean) {
        if (result) {
            viewModelScope.launch {
                delay(3500)
                _messageSendResult.postValue(true)
            }
        } else {
            _messageSendResult.postValue(false)
        }
    }

    fun sendData() {
        val msisdn = (mainRepository.customer.value as com.famoco.kyctelcomr.mattel.model.Customer?)?.msisdn
        val imsi = (mainRepository.customer.value as com.famoco.kyctelcomr.mattel.model.Customer?)?.imsi
        if (msisdn != null && imsi != null) {
            Log.d("mattel", "num msisdn"+msisdn)

           // uploadRepository.sendSMSMattel(msisdn,imsi);
     //      networkRepository.sendIdentity(msisdn, imsi)
        }

//        when (_sendData.value) {
//
//            SendDataEnum.USSD -> {
//                if (msisdn != null && imsi != null) {
//                    uploadRepository.sendUSSD(msisdn, imsi,
//                        if (imsi.isNullOrEmpty()) USSDKind.PRECREATED_IMSI else USSDKind.CLASSIC)
//                }
//            }
//        }
    }
}
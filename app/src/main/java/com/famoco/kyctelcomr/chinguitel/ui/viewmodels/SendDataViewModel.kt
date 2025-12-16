package com.famoco.kyctelcomr.chinguitel.ui.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.famoco.kyctelcomr.chinguitel.model.Customer
import com.famoco.kyctelcomr.core.repositories.MainRepository
import com.famoco.kyctelcomr.core.repositories.NetworkRepository
import com.famoco.kyctelcomr.core.repositories.UploadRepository
import com.famoco.kyctelcomr.core.utils.SendDataEnum
import com.famoco.kyctelcomr.mattel.model.USSDKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SendDataViewModel @Inject constructor(private val mainRepository: MainRepository,
                                            private val uploadRepository: UploadRepository,
                                            private val networkRepository: NetworkRepository) :
    ViewModel() {

    companion object {
        private val TAG = SendDataViewModel::class.java.simpleName
    }

    private val customer = mainRepository.customer.map { if (it is Customer) it else null}

    private val _sendData = MutableLiveData<SendDataEnum>().apply { value = SendDataEnum.SMS }
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

    fun updateSendDataWay(sendData: SendDataEnum) {
        _sendData.postValue(sendData)
    }

    fun sendData() {
        when (_sendData.value) {
            SendDataEnum.SMS -> {
                val msisdn = (mainRepository.customer.value as com.famoco.kyctelcomr.chinguitel.model.Customer?)?.phoneNumber
                val imsi = (mainRepository.customer.value as com.famoco.kyctelcomr.chinguitel.model.Customer?)?.imsi
                if (msisdn != null && imsi != null) {
                    uploadRepository.sendSMS(msisdn, imsi);
                }
//                }else if(msisdn != null){
//                    uploadRepository.sendSMS(msisdn);
//                }

//                (mainRepository.customer.value as Customer?)?.phoneNumber?.let {
//                    uploadRepository.sendSMS(it)
//                } ?: Log.w(TAG, "no phoneNumber, cannot send SMS")
            }
            SendDataEnum.USSD -> {
                (mainRepository.customer.value as Customer?)?.phoneNumber?.let {
                    uploadRepository.sendUSSD(it)
                } ?: Log.w(TAG, "no phoneNumber, cannot send USSD")
            }
            SendDataEnum.WEB -> {
             //   val result = networkRepository.sendIdentity()
              //  Log.d(TAG, "sendData: ${result.toString()}")
            }
            else -> {
                Log.w(TAG, "SendData value is null or invalid")
            }
        }
    }
    fun sendOTP(msisdn:String,otp:String){
        // val msisdn = (mainRepository.customer.value as com.famoco.kyctelcomr.chinguitel.model.Customer?)?.phoneNumber

        uploadRepository.sendOTP(msisdn,otp);
    }
}
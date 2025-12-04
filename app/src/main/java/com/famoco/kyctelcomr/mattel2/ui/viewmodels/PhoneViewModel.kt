package com.famoco.kyctelcomr.mattel2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.famoco.kyctelcomr.core.repositories.MainRepository
import com.famoco.kyctelcomr.core.repositories.UploadRepository
import com.famoco.kyctelcomr.mattel.model.Customer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PhoneViewModel @Inject constructor(private val mainRepository: MainRepository,
                                         private val uploadRepository: UploadRepository,
) :
    ViewModel() {

    companion object {
        private val TAG = PhoneViewModel::class.java.simpleName
    }

    private val customer = mainRepository.customer.map { if (it is Customer) it else null}


    fun setCustomerMSISDN(msisdn: String) {

        mainRepository.customer.value?.let {
            if (it is Customer) {
                it.msisdn = msisdn
                mainRepository.setCustomer(it)
            }
        }
    }

    fun setCustomerIMSI(imsi: String) {
        mainRepository.customer.value?.let {
            if (it is Customer) {
                it.imsi = imsi
                mainRepository.setCustomer(it)
            }
        }
    }
}
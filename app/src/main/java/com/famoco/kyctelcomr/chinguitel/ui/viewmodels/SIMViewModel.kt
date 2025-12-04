package com.famoco.kyctelcomr.chinguitel.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.famoco.kyctelcomr.chinguitel.model.Customer
import com.famoco.kyctelcomr.core.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SIMViewModel @Inject constructor(private val mainRepository: MainRepository) :
    ViewModel() {

    companion object {
        private val TAG = SIMViewModel::class.java.simpleName
    }

    private val customer = mainRepository.customer.map { if (it is Customer) it else null}

    fun setCustomerPhoneNumber(phoneNumber: String) {
        mainRepository.customer.value?.let {
            if (it is Customer) {
                it.phoneNumber = phoneNumber
                mainRepository.setCustomer(it)
            }
        }
    }
    fun setCustomerIMSI(imsi: String) {
        mainRepository.customer.value?.let {
            if (it is com.famoco.kyctelcomr.chinguitel.model.Customer) {
                it.imsi = imsi
                mainRepository.setCustomer(it)
            }
        }
    }
}
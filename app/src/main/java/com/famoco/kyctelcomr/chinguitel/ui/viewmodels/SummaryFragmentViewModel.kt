package com.famoco.kyctelcomr.chinguitel.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.famoco.kyctelcomr.chinguitel.model.Customer
import com.famoco.kyctelcomr.core.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SummaryFragmentViewModel @Inject constructor(private val mainRepository: MainRepository) :
    ViewModel() {

    companion object {
        private val TAG = SIMViewModel::class.java.simpleName
    }

    val customer = mainRepository.customer.map { if (it is Customer) it else null}
    val identity = mainRepository.identity
    val cardNumber = mainRepository.cardNumber

}
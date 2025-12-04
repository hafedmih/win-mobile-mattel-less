package com.famoco.kyctelcomrtlib.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class MergedLiveData<T> constructor(source1: LiveData<T>, source2: LiveData<T>)
    : MediatorLiveData<T>() {
    init {
        addSource(source1) {
            postValue(it)
        }

        addSource(source2) {
            postValue(it)
        }
    }
}
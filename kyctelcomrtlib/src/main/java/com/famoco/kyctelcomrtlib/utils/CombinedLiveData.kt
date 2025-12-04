package com.famoco.kyctelcomrtlib.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class CombinedLiveData<T, U, V> constructor(source1: LiveData<U>, source2: LiveData<V>,
                                            private val combine:(u: U?, v: V?) -> T)
    : MediatorLiveData<T>() {

    private var value1: U? = null
    private var value2: V? = null

        init {
            addSource(source1) {
                value1 = it
                postValue(combine(value1, value2))
            }

            addSource(source2) {
                value2 = it
                postValue(combine(value1, value2))
            }
        }

}
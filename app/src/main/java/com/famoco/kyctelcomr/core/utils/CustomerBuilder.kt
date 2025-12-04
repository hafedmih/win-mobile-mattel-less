package com.famoco.kyctelcomr.core.utils

import com.famoco.kyctelcomr.BuildConfig
import com.famoco.kyctelcomr.core.model.Customer

object CustomerBuilder {
    fun build(): Customer {
        return when (BuildConfig.FLAVOR) {
            "chinguitel" -> com.famoco.kyctelcomr.chinguitel.model.Customer()
            "mattel" -> com.famoco.kyctelcomr.mattel.model.Customer()
            else -> com.famoco.kyctelcomr.chinguitel.model.Customer()
        }
    }
}
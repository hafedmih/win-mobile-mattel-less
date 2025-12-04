package com.famoco.kyctelcomr.chinguitel.model

import com.morpho.morphosmart.sdk.TemplateList


data class Customer(override var templates: TemplateList = TemplateList(),
                    var phoneNumber: String = "", var imsi: String = "")
    : com.famoco.kyctelcomr.core.model.Customer(templates)
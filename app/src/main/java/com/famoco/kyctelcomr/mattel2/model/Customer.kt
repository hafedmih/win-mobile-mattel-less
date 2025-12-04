package com.famoco.kyctelcomr.mattel2.model

import com.morpho.morphosmart.sdk.TemplateList

data class Customer(override var templates: TemplateList = TemplateList(),
                    var msisdn: String = "", var imsi: String = "")
    : com.famoco.kyctelcomr.core.model.Customer(templates)
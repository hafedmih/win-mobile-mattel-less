package com.famoco.kyctelcomrtlib.smartcard

import android.graphics.Bitmap

data class Identity(val personalNumber: String, val photo: Bitmap?, val firstnameLoc: String,
                    val firstName: String, val fatherFirstNameLoc: String,
                    val fatherFirstName: String, val lastNameLoc: String, val lastName: String,
                    val sexLoc: String, val sex: String, val dateOfBirth: String,
                    val placeOfBirthLoc: String, val placeOfBirth: String, val expiryDate: String)

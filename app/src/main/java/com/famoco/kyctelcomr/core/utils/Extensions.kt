package com.famoco.kyctelcomr.core.utils

import android.content.Context
import com.famoco.kyctelcomr.R
import com.morpho.morphosmart.sdk.ErrorCodes
import java.text.SimpleDateFormat
import java.util.*

fun String.toDate(): Date? {
    var res: Date? = null
    val formatter = SimpleDateFormat("E MMM d HH:mm:ss z yyyy", Locale.ENGLISH)
    try {
        res = formatter.parse(this)
    } catch (e: Exception) {
        // nothing to do
    }
    return res
}

fun Date.toDateString(): String {
    val formatter = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
    return formatter.format(this)
}

fun String.formatToDateString(): String {
    Date(this)
    val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    return formatter.format(this)
}
fun Int.toSensorMessage(context: Context): String {
    return when (this) {
        0 -> context.getString(R.string.morpho_instruction_0)
        1 -> context.getString(R.string.morpho_instruction_1)
        2 -> context.getString(R.string.morpho_instruction_2)
        3 -> context.getString(R.string.morpho_instruction_3)
        4 -> context.getString(R.string.morpho_instruction_4)
        5 -> context.getString(R.string.morpho_instruction_5)
        6 -> context.getString(R.string.morpho_instruction_6)
        7 -> context.getString(R.string.morpho_instruction_7)
        8 -> context.getString(R.string.morpho_instruction_8)
        else -> context.getString(R.string.EMPTY)
    }
}

fun Int.toInternalErrorMessage(): Int {
    return when (this) {
        ErrorCodes.MORPHO_OK -> R.string.MORPHO_OK
        ErrorCodes.MORPHOERR_INTERNAL -> R.string.MORPHOERR_INTERNAL
        ErrorCodes.MORPHOERR_PROTOCOLE -> R.string.MORPHOERR_PROTOCOLE
        ErrorCodes.MORPHOERR_CONNECT -> R.string.MORPHOERR_CONNECT
        ErrorCodes.MORPHOERR_CLOSE_COM -> R.string.MORPHOERR_CLOSE_COM
        ErrorCodes.MORPHOERR_BADPARAMETER -> R.string.MORPHOERR_BADPARAMETER
        ErrorCodes.MORPHOERR_MEMORY_PC -> R.string.MORPHOERR_MEMORY_PC
        ErrorCodes.MORPHOERR_MEMORY_DEVICE -> R.string.MORPHOERR_MEMORY_DEVICE
        ErrorCodes.MORPHOERR_NO_HIT -> R.string.MORPHOERR_NO_HIT
        ErrorCodes.MORPHOERR_STATUS -> R.string.MORPHOERR_STATUS
        ErrorCodes.MORPHOERR_DB_FULL -> R.string.MORPHOERR_DB_FULL
        ErrorCodes.MORPHOERR_DB_EMPTY -> R.string.MORPHOERR_DB_EMPTY
        ErrorCodes.MORPHOERR_ALREADY_ENROLLED -> R.string.MORPHOERR_ALREADY_ENROLLED
        ErrorCodes.MORPHOERR_BASE_NOT_FOUND -> R.string.MORPHOERR_BASE_NOT_FOUND
        ErrorCodes.MORPHOERR_BASE_ALREADY_EXISTS -> R.string.MORPHOERR_BASE_ALREADY_EXISTS
        ErrorCodes.MORPHOERR_NO_ASSOCIATED_DB -> R.string.MORPHOERR_NO_ASSOCIATED_DB
        ErrorCodes.MORPHOERR_NO_ASSOCIATED_DEVICE -> R.string.MORPHOERR_NO_ASSOCIATED_DEVICE
        ErrorCodes.MORPHOERR_INVALID_TEMPLATE -> R.string.MORPHOERR_INVALID_TEMPLATE
        ErrorCodes.MORPHOERR_NOT_IMPLEMENTED -> R.string.MORPHOERR_NOT_IMPLEMENTED
        ErrorCodes.MORPHOERR_TIMEOUT -> R.string.MORPHOERR_TIMEOUT
        ErrorCodes.MORPHOERR_NO_REGISTERED_TEMPLATE -> R.string.MORPHOERR_NO_REGISTERED_TEMPLATE
        ErrorCodes.MORPHOERR_FIELD_NOT_FOUND -> R.string.MORPHOERR_FIELD_NOT_FOUND
        ErrorCodes.MORPHOERR_CORRUPTED_CLASS -> R.string.MORPHOERR_CORRUPTED_CLASS
        ErrorCodes.MORPHOERR_TO_MANY_TEMPLATE -> R.string.MORPHOERR_TO_MANY_TEMPLATE
        ErrorCodes.MORPHOERR_TO_MANY_FIELD -> R.string.MORPHOERR_TO_MANY_FIELD
        ErrorCodes.MORPHOERR_MIXED_TEMPLATE -> R.string.MORPHOERR_MIXED_TEMPLATE
        ErrorCodes.MORPHOERR_CMDE_ABORTED -> R.string.MORPHOERR_CMDE_ABORTED
        ErrorCodes.MORPHOERR_INVALID_PK_FORMAT -> R.string.MORPHOERR_INVALID_PK_FORMAT
        ErrorCodes.MORPHOERR_SAME_FINGER -> R.string.MORPHOERR_SAME_FINGER
        ErrorCodes.MORPHOERR_OUT_OF_FIELD -> R.string.MORPHOERR_OUT_OF_FIELD
        ErrorCodes.MORPHOERR_INVALID_USER_ID -> R.string.MORPHOERR_INVALID_USER_ID
        ErrorCodes.MORPHOERR_INVALID_USER_DATA -> R.string.MORPHOERR_INVALID_USER_DATA
        ErrorCodes.MORPHOERR_FIELD_INVALID -> R.string.MORPHOERR_FIELD_INVALID
        ErrorCodes.MORPHOERR_USER_NOT_FOUND -> R.string.MORPHOERR_USER_NOT_FOUND
        ErrorCodes.MORPHOERR_COM_NOT_OPEN -> R.string.MORPHOERR_COM_NOT_OPEN
        ErrorCodes.MORPHOERR_ELT_ALREADY_PRESENT -> R.string.MORPHOERR_ELT_ALREADY_PRESENT
        ErrorCodes.MORPHOERR_NOCALLTO_DBQUERRYFIRST -> R.string.MORPHOERR_NOCALLTO_DBQUERRYFIRST
        ErrorCodes.MORPHOERR_USER -> R.string.MORPHOERR_USER
        ErrorCodes.MORPHOERR_BAD_COMPRESSION -> R.string.MORPHOERR_BAD_COMPRESSION
        ErrorCodes.MORPHOERR_SECU -> R.string.MORPHOERR_SECU
        ErrorCodes.MORPHOERR_CERTIF_UNKNOW -> R.string.MORPHOERR_CERTIF_UNKNOW
        ErrorCodes.MORPHOERR_INVALID_CLASS -> R.string.MORPHOERR_INVALID_CLASS
        ErrorCodes.MORPHOERR_USB_DEVICE_NAME_UNKNOWN -> R.string.MORPHOERR_USB_DEVICE_NAME_UNKNOWN
        ErrorCodes.MORPHOERR_CERTIF_INVALID -> R.string.MORPHOERR_CERTIF_INVALID
        ErrorCodes.MORPHOERR_SIGNER_ID -> R.string.MORPHOERR_SIGNER_ID
        ErrorCodes.MORPHOERR_SIGNER_ID_INVALID -> R.string.MORPHOERR_SIGNER_ID_INVALID
        ErrorCodes.MORPHOERR_FFD -> R.string.MORPHOERR_FFD
        ErrorCodes.MORPHOERR_MOIST_FINGER -> R.string.MORPHOERR_MOIST_FINGER
        ErrorCodes.MORPHOERR_NO_SERVER -> R.string.MORPHOERR_NO_SERVER
        ErrorCodes.MORPHOERR_OTP_NOT_INITIALIZED -> R.string.MORPHOERR_OTP_NOT_INITIALIZED
        ErrorCodes.MORPHOERR_OTP_PIN_NEEDED -> R.string.MORPHOERR_OTP_PIN_NEEDED
        ErrorCodes.MORPHOERR_OTP_REENROLL_NOT_ALLOWED -> R.string.MORPHOERR_OTP_REENROLL_NOT_ALLOWED
        ErrorCodes.MORPHOERR_OTP_ENROLL_FAILED -> R.string.MORPHOERR_OTP_ENROLL_FAILED
        ErrorCodes.MORPHOERR_OTP_IDENT_FAILED -> R.string.MORPHOERR_OTP_IDENT_FAILED
        ErrorCodes.MORPHOERR_NO_MORE_OTP -> R.string.MORPHOERR_NO_MORE_OTP
        ErrorCodes.MORPHOERR_OTP_NO_HIT -> R.string.MORPHOERR_OTP_NO_HIT
        ErrorCodes.MORPHOERR_OTP_ENROLL_NEEDED -> R.string.MORPHOERR_OTP_ENROLL_NEEDED
        ErrorCodes.MORPHOERR_DEVICE_LOCKED -> R.string.MORPHOERR_DEVICE_LOCKED
        ErrorCodes.MORPHOERR_DEVICE_NOT_LOCK -> R.string.MORPHOERR_DEVICE_NOT_LOCK
        ErrorCodes.MORPHOERR_OTP_LOCK_GEN_OTP -> R.string.MORPHOERR_OTP_LOCK_GEN_OTP
        ErrorCodes.MORPHOERR_OTP_LOCK_SET_PARAM -> R.string.MORPHOERR_OTP_LOCK_SET_PARAM
        ErrorCodes.MORPHOERR_OTP_LOCK_ENROLL -> R.string.MORPHOERR_OTP_LOCK_ENROLL
        ErrorCodes.MORPHOERR_FVP_MINUTIAE_SECURITY_MISMATCH -> R.string.MORPHOERR_FVP_MINUTIAE_SECURITY_MISMATCH
        ErrorCodes.MORPHOERR_FVP_FINGER_MISPLACED_OR_WITHDRAWN -> R.string.MORPHOERR_FVP_FINGER_MISPLACED_OR_WITHDRAWN
        ErrorCodes.MORPHOERR_LICENSE_MISSING -> R.string.MORPHOERR_LICENSE_MISSING
        ErrorCodes.MORPHOERR_CANT_GRAN_PERMISSION_USB -> R.string.MORPHOERR_CANT_GRAN_PERMISSION_USB
        else -> R.string.EMPTY
    }
}
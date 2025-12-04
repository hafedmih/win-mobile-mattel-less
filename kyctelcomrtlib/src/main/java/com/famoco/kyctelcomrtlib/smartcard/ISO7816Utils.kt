package com.famoco.kyctelcomrtlib.smartcard

class ISO7816Utils {
    companion object {
        const val OFFSET_CLA = 0
        const val OFFSET_INS = 1
        const val OFFSET_P1 = 2
        const val OFFSET_P2 = 3
        const val OFFSET_LC = 4
        const val OFFSET_CDATA = 5
        const val CLA_ISO7816 = 0x00.toByte()
        const val CLA_COMMAND_CHAINING = 0x10.toByte()
        const val INVALIDATE_CHV = 0x04.toByte()
        const val INS_ERASE_BINARY = 0x0E.toByte()
        const val INS_VERIFY = 0x20.toByte()
        const val INS_CHANGE_CHV = 0x24.toByte()
        const val INS_UNBLOCK_CHV = 0x2C.toByte()
        const val INS_DECREASE = 0x30.toByte()
        const val INS_INCREASE = 0x32.toByte()
        const val INS_DECREASE_STAMPED = 0x34.toByte()
        const val INS_REHABILITATE_CHV = 0x44.toByte()
        const val INS_MANAGE_CHANNEL = 0x70.toByte()
        const val INS_EXTERNAL_AUTHENTICATE = 0x82.toByte()
        const val INS_MUTUAL_AUTHENTICATE = 0x82.toByte()
        const val INS_GET_CHALLENGE = 0x84.toByte()
        const val INS_ASK_RANDOM = 0x84.toByte()
        const val INS_GIVE_RANDOM = 0x86.toByte()
        const val INS_INTERNAL_AUTHENTICATE = 0x88.toByte()
        const val INS_SEEK = 0xA2.toByte()
        const val INS_SELECT = 0xA4.toByte()
        const val INS_SELECT_FILE = 0xA4.toByte()
        const val INS_CLOSE_APPLICATION = 0xAC.toByte()
        const val INS_READ_BINARY = 0xB0.toByte()
        const val INS_READ_BINARY2 = 0xB1.toByte()
        const val INS_READ_RECORD = 0xB2.toByte()
        const val INS_READ_RECORD2 = 0xB3.toByte()
        const val INS_READ_RECORDS = 0xB2.toByte()
        const val INS_READ_BINARY_STAMPED = 0xB4.toByte()
        const val INS_READ_RECORD_STAMPED = 0xB6.toByte()
        const val INS_GET_RESPONSE = 0xC0.toByte()
        const val INS_ENVELOPE = 0xC2.toByte()
        const val INS_GET_DATA = 0xCA.toByte()
        const val INS_WRITE_BINARY = 0xD0.toByte()
        const val INS_WRITE_RECORD = 0xD2.toByte()
        const val INS_UPDATE_BINARY = 0xD6.toByte()
        const val INS_LOAD_KEY_FILE = 0xD8.toByte()
        const val INS_PUT_DATA = 0xDA.toByte()
        const val INS_UPDATE_RECORD = 0xDC.toByte()
        const val INS_CREATE_FILE = 0xE0.toByte()
        const val INS_APPEND_RECORD = 0xE2.toByte()
        const val INS_DELETE_FILE = 0xE4.toByte()
        const val INS_PSO = 0x2A.toByte()
        const val INS_MSE = 0x22.toByte()
    }
}
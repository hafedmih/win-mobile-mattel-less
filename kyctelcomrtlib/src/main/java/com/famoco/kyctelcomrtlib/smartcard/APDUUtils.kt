package com.famoco.kyctelcomrtlib.smartcard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.gemalto.jp2.JP2Decoder
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import kotlin.experimental.and

class APDUUtils {
    companion object {
        private val TAG = APDUUtils::class.simpleName

        const val AID_PKI = "A000000000494445414C"
        const val AID_ICAO = "A0000002471001"
        const val AID_IDENTITE = "414446204944454E44495459"
        const val AID_VOTE = "41444620564F5445"
        const val AID_PERMIS = "414446205045524D4953"
        const val AID_SANTE = "4144462053414E5445"
        const val AID_BIOMETRY = "4d41535445525f46494c45"

        const val IDENTITE_EF_FIX = "0101"
        const val CARD_NUMBER_EF_FIX = "d003"
        const val BIOMETRY_CARD_ID = "d003"

        //Right Hand
        const val BIOMETRY_FINGER_11 = 0x11

        //Left Hand
        const val BIOMETRY_FINGER_12 = 0x12

        fun formatSelectAidApdu(aid: ByteArray): ByteArray {
            val apdu = ByteArray(5)
            apdu[ISO7816Utils.OFFSET_CLA] = ISO7816Utils.CLA_ISO7816
            apdu[ISO7816Utils.OFFSET_INS] = ISO7816Utils.INS_SELECT
            apdu[ISO7816Utils.OFFSET_P1] = 0x04
            apdu[ISO7816Utils.OFFSET_P2] = 0x00
            apdu[ISO7816Utils.OFFSET_LC] = aid.size.toByte()
            return apdu + aid + ByteArray(1) { 0x00 }
        }

        fun formatSelectDataSetApdu(fid: ByteArray): ByteArray {
            val apdu = ByteArray(5)
            apdu[ISO7816Utils.OFFSET_CLA] = ISO7816Utils.CLA_ISO7816
            apdu[ISO7816Utils.OFFSET_INS] = ISO7816Utils.INS_SELECT_FILE
            apdu[ISO7816Utils.OFFSET_P1] = 0x02
            apdu[ISO7816Utils.OFFSET_P2] = 0x0C
            apdu[ISO7816Utils.OFFSET_LC] = fid.size.toByte()
            return apdu + fid
        }

        fun formatReadBinaryFileApdu(offset: Int): ByteArray {
            val apdu = ByteArray(5)
            apdu[ISO7816Utils.OFFSET_CLA] = ISO7816Utils.CLA_ISO7816
            apdu[ISO7816Utils.OFFSET_INS] = ISO7816Utils.INS_READ_BINARY
            apdu[ISO7816Utils.OFFSET_P1] = (offset shr 8).toByte() and 0xFF.toByte()
            apdu[ISO7816Utils.OFFSET_P2] = offset.toByte() and 0xFF.toByte()
            apdu[ISO7816Utils.OFFSET_LC] = 0x00
            return apdu
        }

        fun formatMatchApdu(template: ByteArray, slot: Byte): ByteArray {
            if (template.size > 250) {
                Log.w(TAG, "too big template data")
                return ByteArray(0)
            }

            val apdu = ByteArray(10)
            apdu[ISO7816Utils.OFFSET_CLA] = ISO7816Utils.CLA_ISO7816
            apdu[ISO7816Utils.OFFSET_INS] = 0x21
            apdu[ISO7816Utils.OFFSET_P1] = 0x00
            apdu[ISO7816Utils.OFFSET_P2] = slot
            apdu[ISO7816Utils.OFFSET_LC] = (template.size + 5).toByte()
            apdu[5] = 0x7F
            apdu[6] = 0x2E
            apdu[7] = (template.size + 2).toByte()
            apdu[8] = 0x81.toByte()
            apdu[9] = template.size.toByte()
            return apdu + template
        }

        fun parseCardNumber(byteArray: ByteArray): String {
            if (byteArray.size < 2) {
                return ""
            }
            val size = byteArray[1].toUByte().toInt()
            if (byteArray.size < 2 + size) {
                return ""
            }

            val bytes = byteArray.sliceArray(2 until 2 + size)
            // remove extra 0 header
            var index = 0
            for (byte in bytes) {
                if (byte.toInt() != 0) {
                    break
                }
                Log.i(TAG, "card number - index $index is empty, remove it")
                index++
            }

            return HexUtils.hexStringToASCII(HexUtils.byteArrayToHexString(bytes.sliceArray(index until bytes.size)))
        }

        fun parseIdentity(byteArray: ByteArray): Identity {
            if (byteArray.size < 4486) {
                return Identity("", null, "","",
                    "", "","", "", "",
                    "", "", "","", "")
            }
            val personalNumber = String(byteArray.sliceArray(0 until 10), StandardCharsets.UTF_8)
            val photo = byteArray.sliceArray(10 until 4010)
            val firstnameLoc = String(byteArray.sliceArray(4010 until 4090), StandardCharsets.UTF_8)
            val firstName = String(byteArray.sliceArray(4090 until 4130), StandardCharsets.UTF_8)
            val fatherFirstNameLoc = String(byteArray.sliceArray(4130 until 4210), StandardCharsets.UTF_8)
            val fatherFirstName = String(byteArray.sliceArray(4210 until 4250), StandardCharsets.UTF_8)
            val lastNameLoc = String(byteArray.sliceArray(4250 until 4330), StandardCharsets.UTF_8)
            val lastName = String(byteArray.sliceArray(4330 until 4370), StandardCharsets.UTF_8)
            val sexLoc = String(byteArray.sliceArray(4370 until 4380), StandardCharsets.UTF_8)
            val sex = String(byteArray.sliceArray(4380 until 4388), StandardCharsets.UTF_8)
            var dateOfBirth = ""
            try {
                dateOfBirth = SimpleDateFormat("yyyyMMdd").parse(
                    HexUtils.byteArrayToHexString(
                        byteArray.sliceArray(4388 until 4392)
                    )
                )?.toString() ?: ""
            } catch (e: Exception) {
                // nothing to do
            }
            val placeOfBirthLoc = String(byteArray.sliceArray(4392 until 4452), StandardCharsets.UTF_8)
            val placeOfBirth = String(byteArray.sliceArray(4452 until 4482), StandardCharsets.UTF_8)
            var expiryDate = ""
            try {
                expiryDate = SimpleDateFormat("yyyyMMdd").parse(
                    HexUtils.byteArrayToHexString(
                        byteArray.sliceArray(4482 until 4486)
                    )
                )?.toString()?: ""
            } catch (e: Exception) {
                // nothing to do
            }

            var bmp: Bitmap? = null
            try {
                bmp = JP2Decoder(photo).decode()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return Identity(personalNumber, bmp, firstnameLoc, firstName, fatherFirstNameLoc,
                fatherFirstName, lastNameLoc, lastName, sexLoc, sex, dateOfBirth, placeOfBirthLoc,
                placeOfBirth, expiryDate)
        }

        fun parseIdentityNewCard(byteArray: ByteArray): Identity {
            if (byteArray.size < 4636) {
                return Identity("", null, "","",
                    "", "","", "", "",
                    "", "", "","", "")
            }
            val personalNumber = String(byteArray.sliceArray(0 until 10), StandardCharsets.UTF_8)
            val photo = byteArray.sliceArray(10 until 4010)
            val firstnameLoc = String(byteArray.sliceArray(4010 until 4090), StandardCharsets.UTF_8)
            val firstName = String(byteArray.sliceArray(4090 until 4170), StandardCharsets.UTF_8)
            val fatherFirstNameLoc = String(byteArray.sliceArray(4170 until 4250), StandardCharsets.UTF_8)
            val fatherFirstName = String(byteArray.sliceArray(4250 until 4330), StandardCharsets.UTF_8)
            val lastNameLoc = String(byteArray.sliceArray(4330 until 4410), StandardCharsets.UTF_8)
            val lastName = String(byteArray.sliceArray(4410 until 4490), StandardCharsets.UTF_8)
            val sexLoc = String(byteArray.sliceArray(4490 until 4500), StandardCharsets.UTF_8)
            val sex = String(byteArray.sliceArray(4500 until 4508), StandardCharsets.UTF_8)
            var dateOfBirth = ""
            try {
                dateOfBirth = SimpleDateFormat("yyyyMMdd").parse(
                    HexUtils.byteArrayToHexString(
                        byteArray.sliceArray(4508 until 4512)
                    )
                )?.toString() ?: ""
            } catch (e: Exception) {
                // nothing to do
            }
            val placeOfBirthLoc = String(byteArray.sliceArray(4512 until 4572), StandardCharsets.UTF_8)
            val placeOfBirth = String(byteArray.sliceArray(4572 until 4632), StandardCharsets.UTF_8)
            var expiryDate = ""
            try {
                expiryDate = SimpleDateFormat("yyyyMMdd").parse(
                    HexUtils.byteArrayToHexString(
                        byteArray.sliceArray(4632 until 4636)
                    )
                )?.toString()?: ""
            } catch (e: Exception) {
                // nothing to do
            }

            var bmp: Bitmap? = null
            try {
                bmp = BitmapFactory.decodeByteArray(photo, 0, photo.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return Identity(personalNumber, bmp, firstnameLoc, firstName, fatherFirstNameLoc,
                fatherFirstName, lastNameLoc, lastName, sexLoc, sex, dateOfBirth, placeOfBirthLoc,
                placeOfBirth, expiryDate)
        }
    }
}
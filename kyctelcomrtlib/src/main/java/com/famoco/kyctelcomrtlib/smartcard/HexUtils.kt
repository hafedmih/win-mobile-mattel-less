package com.famoco.kyctelcomrtlib.smartcard

import android.util.Log

class HexUtils {
    companion object {
        private var TAG = HexUtils::class.java.simpleName
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

        fun integerToHexString(integer: Int): String {
            return Integer.toHexString(integer)
        }

        fun integerToByteArray (integer: Int) : ByteArray =
            ByteArray (4) {i -> (integer shr (i * 8)).toByte()}

        fun hexStringToByteArray(s: String): ByteArray {
            var length = s.length
            if (length % 2 != 0) {
                Log.w(TAG, "wrong hex string length: $s")
                return ByteArray(0)
            }
            val data = ByteArray(length / 2)
            for (i in 0 until length step 2) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                        + Character.digit(s[i+1], 16)).toByte()
            }
            return data
        }

        fun byteArrayToHexString(byteArray: ByteArray): String {
            val charArray = CharArray(byteArray.size * 2)
            for (i in byteArray.indices) {
                val v  = byteArray[i].toInt().and(0x00FF)
                charArray[i * 2] = HEX_ARRAY[v / 16]
                charArray[i * 2 + 1] = HEX_ARRAY[v % 16]
            }
            return String(charArray)
        }

        fun concatHexArray(array1: ByteArray?, array2: ByteArray?): ByteArray? {
            if (array1 == null) {
                return array2
            }
            if (array2 == null) {
                return array1
            }
            return array1 + array2
        }

        fun concatHexArray(array1: ByteArray?, beginIndex1: Int, length1: Int,
                           array2: ByteArray?, beginIndex2: Int, length2: Int): ByteArray? {
            val res = ByteArray(length1 + length2)
            for (i in 0 until length1) {
                res[i] = array1!![beginIndex1 + i]
            }
            for (i in 0 until length2) {
                res[length1 + i] = array2!![beginIndex2 + i]
            }
            return res
        }

        fun hexStringToASCII(hexString: String): String {
            val builder = StringBuilder()
            for (i in hexString.indices step 2) {
                builder.append(Integer.parseInt(hexString.substring(i, i + 2), 16).toChar())
            }
            return builder.toString()
        }

        fun asciiToHexString(ascii: String): String {
            val array = ascii.toCharArray()
            val hex = StringBuffer()
            for (c in array) {
                hex.append(Integer.toHexString(c.toInt()))
            }
            return hex.toString()
        }
    }
}
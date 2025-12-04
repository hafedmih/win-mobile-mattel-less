package com.ws.idcheck.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gemalto.jp2.JP2Decoder

object ImageTools {

    private var isInitialized = false

    fun jp200ToBitmap(data: ByteArray): Bitmap? {

        var magicByte = 0
        if(data[0] == magicByte.toByte()) {
            //Probably jpeg2000
            return JP2Decoder(data).decode()
        } else {
            //Probably jpeg
            return BitmapFactory.decodeByteArray(data, 0, data.size)
        }

    }

    init {
        if (!isInitialized) {
            try {
                System.loadLibrary("openjpeg")
                isInitialized = true
            } catch (t: Throwable) {
                throw ExceptionInInitializerError("OpenJPEG Java Decoder: probably impossible to find the C library")
            }
        }
    }
}
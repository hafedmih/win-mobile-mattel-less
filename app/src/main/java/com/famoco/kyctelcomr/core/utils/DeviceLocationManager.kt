package com.famoco.kyctelcomr.core.utils

import android.content.Context
import android.util.Log
import com.famoco.kyctelcomr.mattel.model.DeviceLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

object DeviceLocationManager {
    private const val FILE_NAME = "device_locations.json"
    private val gson = Gson()

    /**
     * Retrieves the list of locations from Internal Storage.
     * If the file doesn't exist, it copies the initial one from Assets.
     */
    fun getLocations(context: Context): List<DeviceLocation> {
        val file = File(context.filesDir, FILE_NAME)

        // 1. Copy from assets if first time
        if (!file.exists()) {
            copyAssetsToInternal(context)
        }

        // 2. Read from internal storage
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<DeviceLocation>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("LocationManager", "Error reading JSON: ${e.message}")
            emptyList()
        }
    }

    /**
     * Updates an existing device or adds a new one to the list.
     * It automatically sanitizes IDs to prevent O/0 confusion.
     */
    @JvmStatic
    fun updateOrAddLocation(context: Context, newLoc: DeviceLocation) {
        val currentList = getLocations(context).toMutableList()
        val sanitizedNewId = sanitize(newLoc.Device)

        // Remove the old entry if it exists (Match by sanitized ID)
        currentList.removeAll { sanitize(it.Device) == sanitizedNewId }

        // Add the new location
        currentList.add(newLoc)
        saveLocations(context, currentList)
    }

    /**
     * Deletes a device from the JSON list.
     */
    @JvmStatic
    fun deleteLocation(context: Context, deviceId: String) {
        val currentList = getLocations(context).toMutableList()
        val sanitizedId = sanitize(deviceId)

        val removed = currentList.removeAll { sanitize(it.Device) == sanitizedId }

        if (removed) {
            saveLocations(context, currentList)
            Log.d("LocationManager", "Device $deviceId removed.")
        } else {
            Log.d("LocationManager", "Device $deviceId not found to delete.")
        }
    }

    /**
     * Saves the list back to the internal JSON file.
     */
    private fun saveLocations(context: Context, locations: List<DeviceLocation>) {
        try {
            val jsonString = gson.toJson(locations)
            context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("LocationManager", "Error saving JSON: ${e.message}")
        }
    }

    /**
     * Copies the initial JSON file from the assets folder to internal storage.
     */
    private fun copyAssetsToInternal(context: Context) {
        try {
            context.assets.open(FILE_NAME).use { input ->
                val file = File(context.filesDir, FILE_NAME)
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("LocationManager", "Default JSON copied from assets.")
        } catch (e: Exception) {
            Log.e("LocationManager", "Failed to copy assets: ${e.message}")
        }
    }

    /**
     * Helper to clean hardware IDs: Trim, Uppercase, and Letter O -> Number 0
     */
    private fun sanitize(id: String?): String {
        return id?.trim()?.uppercase()?.replace("O", "0") ?: ""
    }
}
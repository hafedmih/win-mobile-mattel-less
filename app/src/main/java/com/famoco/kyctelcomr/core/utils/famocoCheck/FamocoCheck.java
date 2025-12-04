package com.famoco.kyctelcomr.core.utils.famocoCheck;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

public class FamocoCheck {
    private final String TAG = FamocoCheck.class.getSimpleName();

    private final String FAMOCO = "FAMOCO";
    private final String MODELS[] = {"PX400", "PX320", "SM-T515"};
    private final String FAMOCO_LAYER = "com.famoco.fms";
    private FamocoListener listener;
    /**
     * As of now, we only need the deviceManufacturer,
     * but we might need other information later,
     * so they are already made available here.
     */
    private String deviceModel;
    private String deviceManufacturer;
    private int deviceAndroidVersion;
    private Context context;

    public FamocoCheck(Context context, FamocoListener listener) {
        this.deviceModel = Build.MODEL;
        this.deviceManufacturer = Build.MANUFACTURER;
        this.deviceAndroidVersion = Build.VERSION.SDK_INT;
        this.context = context;
        this.listener = listener;
    }

    /**
     * The following function checks if is a Famoco device
     * and call the appropriate callback whether the device is from Famoco or not.
     */
    public void verify() {
        if (isFamocoDevice() || hasFamocoLayer()) {
            listener.onFamocoDevice();
        } else {
            Log.i(TAG, "Is Not Famoco Device: " + deviceManufacturer + " / " + deviceModel);
            listener.onNotFamocoDevice();
        }
    }

    /**
     * The following function retrieves the device's manufacturer
     */
    private boolean isFamocoDevice() {
        Log.i(TAG, "Famoco Device: " + deviceManufacturer + " / " + deviceModel);
        return deviceManufacturer.equals(FAMOCO) || Arrays.asList(MODELS).contains(deviceModel);
    }

    /**
     * The following function retrieves the Famoco layer
     */
    private boolean hasFamocoLayer() {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(FAMOCO_LAYER, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
package com.famoco.kyctelcomr.core.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.famoco.kyctelcomr.mattel.model.DeviceLocation;

public class SMSReceiver extends BroadcastReceiver {
    private static SMSListener smsListener;

    public static void bindListener(SMSListener listener) {
        smsListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                String sender = sms.getDisplayOriginatingAddress();

                // Whitelist check
                if (sender.equals("MattelBiom") || sender.equals("128") ||
                        sender.equals("+22233411686") || sender.equals("+22236101000")) {

                    String message = sms.getMessageBody();
                    Log.d("SMS_UPDATE", "Received: " + message);

                    // Logic: MSISDN*Lat*Lng*DeviceID*Franchise
                    // Example: 36606730*18.133755*-15.931929*1r6l*Note
                    String[] parts = message.split("\\*");

                    if (parts.length >= 5) {
                        try {
                            long msisdn = Long.parseLong(parts[0].trim());
                            String lat = parts[1].trim();
                            String lng = parts[2].trim();
                            // Sanitize Device ID (O -> 0)
                            String deviceId = parts[3].trim().toUpperCase().replace("O", "0");
                            String franchise = parts[4].trim();

                            if (franchise.equalsIgnoreCase("delete")) {
                                // Action: Delete
                                DeviceLocationManager.deleteLocation(context, deviceId);
                                Log.d("SMS_UPDATE", "Device " + deviceId + " deleted via SMS.");
                            } else {
                                // Action: Add or Update
                                DeviceLocation loc = new DeviceLocation(franchise, msisdn, deviceId, lat, lng);
                                DeviceLocationManager.updateOrAddLocation(context, loc);
                                Log.d("SMS_UPDATE", "Device " + deviceId + " updated via SMS.");
                            }
                        } catch (Exception e) {
                            Log.e("SMS_UPDATE", "Error parsing fields: " + e.getMessage());
                        }
                    } else {
                        // Not a coordinate update, pass to original listener if needed
                        if (smsListener != null) {
                            smsListener.onSMSReceived(message);
                        }
                    }
                }
            }
        }
    }
}
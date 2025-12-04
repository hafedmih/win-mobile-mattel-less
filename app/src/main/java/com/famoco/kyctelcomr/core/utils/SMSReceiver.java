package com.famoco.kyctelcomr.core.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
    private static SMSListener smsListener;

    public static void bindListener(SMSListener listener){
        smsListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.e(Constants.TAG, "SMSReceiver => onReceive ");
       // String  id_appareil= Build.SERIAL.toUpperCase();
       // Log.e("smsTag", id_appareil);

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                if(
                        sms.getDisplayOriginatingAddress().equals("MattelBiom")||
                        sms.getDisplayOriginatingAddress().equals("128")
                ) {
            //    if(sms.getDisplayOriginatingAddress().equals("+22249527566")) {
                String message = sms.getMessageBody();
                    Log.e("smsTag", message);

                    smsListener.onSMSReceived(message);
            }

            }
        }
    }
}

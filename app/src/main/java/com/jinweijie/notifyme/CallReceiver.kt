package com.jinweijie.notifyme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallReceiver : BroadcastReceiver() {
    private var incomingNumber: String? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("CallReceiver", "onReceive called")

        if (intent?.action.equals("android.intent.action.PHONE_STATE")) {
            val stateStr = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)

            // Capture incoming number only when the phone is ringing
            if (TelephonyManager.EXTRA_STATE_RINGING == stateStr) {
                incomingNumber = intent?.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                // Log and show the incoming number when phone is ringing
                if (!incomingNumber.isNullOrEmpty()) {
                    Log.d("CallReceiver", "Incoming call from: $incomingNumber")
                    Toast.makeText(context, "Incoming call from: $incomingNumber", Toast.LENGTH_LONG).show()

                    val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault())
                    val currentTime: String = sdf.format(Date())
                    // Post the data
                    Utils.sendNotification(incomingNumber!!, "Call from $incomingNumber @ $currentTime" , "Call", context)
                } else {
                    Log.d("CallReceiver", "Incoming call number is null or empty.")
                }
            }
        }
    }
}
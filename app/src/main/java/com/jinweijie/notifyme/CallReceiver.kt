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

            // Get subscription ID to check which SIM is active
            val subId = intent?.getIntExtra("subscription", -1)
            val simSlotIndex = Utils.getSimSlotIndex(context, subId!!)

            // Check if the device has dual SIM
            val isDualSim = Utils.isDualSimDevice(context)

            // Capture incoming number only when the phone is ringing
            if (TelephonyManager.EXTRA_STATE_RINGING == stateStr) {
                incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                // Log and show the incoming number when phone is ringing
                if (!incomingNumber.isNullOrEmpty()) {
                    // Add SIM slot prefix only if the device is dual SIM
                    val prefix = if (isDualSim) "SIM $simSlotIndex: " else ""
                    val displayMessage = "$prefix$incomingNumber"

                    Log.d("CallReceiver", "Incoming call from $displayMessage")
                    Toast.makeText(context, "Incoming call from $displayMessage", Toast.LENGTH_LONG).show()

                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val currentTime = sdf.format(Date())

                    // Post the data
                    Utils.sendNotification(incomingNumber!!, "Call from $displayMessage @ $currentTime", "Call", context)
                } else {
                    Log.d("CallReceiver", "Incoming call number is null or empty.")
                }
            }
        }
    }
}

package com.jinweijie.notifyme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
            val bundle = intent?.extras
            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<*>
                val msgs: Array<SmsMessage?> = pdus.map {
                    SmsMessage.createFromPdu(it as ByteArray)
                }.toTypedArray()
                for (msg in msgs) {
                    val sender = msg?.originatingAddress ?: "Unknown Sender"
                    val message = msg?.messageBody ?: "No Message Content"

                    Toast.makeText(context, "SMS received from $sender: $message", Toast.LENGTH_LONG).show()

                    // Post the data
                    Utils.sendNotification(sender, message, "SMS", context)
                }
            }
        }
    }
}
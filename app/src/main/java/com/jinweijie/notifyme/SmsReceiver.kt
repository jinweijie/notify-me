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

                // Grouping messages by sender
                val messageMap = mutableMapOf<String, StringBuilder>()

                for (msg in msgs) {
                    val sender = msg?.originatingAddress ?: "Unknown Sender"
                    val message = msg?.messageBody ?: "No Message Content"

                    // Append message to the sender in the map
                    if (messageMap.containsKey(sender)) {
                        messageMap[sender]?.append(message)
                    } else {
                        messageMap[sender] = StringBuilder(message)
                    }
                }

                // Post the concatenated messages per sender
                for ((sender, messageBuilder) in messageMap) {
                    val concatenatedMessage = messageBuilder.toString()
                    Toast.makeText(context, "SMS received from $sender: $concatenatedMessage", Toast.LENGTH_LONG).show()

                    // Post the data
                    Utils.sendNotification(sender, concatenatedMessage, "SMS", context)
                }
            }
        }
    }
}
package com.jinweijie.notifyme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
            val bundle = intent?.extras
            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<*>
                val format = bundle.getString("format") // for CDMA

                val msgs: Array<SmsMessage?> = pdus.map {
                    SmsMessage.createFromPdu(it as ByteArray, format)
                }.toTypedArray()

                val messageMap = mutableMapOf<String, StringBuilder>()

                // Retrieve the subscription ID to determine the SIM slot
                val subId = intent.getIntExtra("subscription", -1)
                val simSlotIndex = Utils.getSimSlotIndex(context, subId)

                // Check if the device has dual SIM
                val isDualSim = Utils.isDualSimDevice(context)

                for (msg in msgs) {
                    val sender = msg?.originatingAddress ?: "Unknown Sender"
                    val message = msg?.messageBody ?: "No Message Content"

                    if (messageMap.containsKey(sender)) {
                        messageMap[sender]?.append(message)
                    } else {
                        messageMap[sender] = StringBuilder(message)
                    }
                }

                for ((sender, messageBuilder) in messageMap) {
                    val concatenatedMessage = messageBuilder.toString()

                    // Add SIM slot prefix only if the device is dual SIM
                    val prefix = if (isDualSim) "SIM $simSlotIndex: " else ""
                    val displayMessage = "$prefix$concatenatedMessage"

                    Toast.makeText(context, displayMessage, Toast.LENGTH_LONG).show()

                    Utils.sendNotification(sender, displayMessage, "SMS", context)
                }
            }
        }
    }
}

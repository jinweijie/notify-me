package com.jinweijie.notifyme

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.telephony.SubscriptionManager
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object Utils {

    fun sendNotification(sender: String, message: String, type: String, context: Context?){
        if (context == null) {
            Log.e("sendNotification", "Context is null")
            return
        }

        val sharedPreferences = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val isBarkEnabled = sharedPreferences.getBoolean("enable_bark", false)
        val isEmailEnabled = sharedPreferences.getBoolean("enable_email", false)
        val isWebhookEnabled = sharedPreferences.getBoolean("enable_webhook", false)
        val isHttpEnabled = sharedPreferences.getBoolean("enable_http", false)

        if(isBarkEnabled)
            sendBark(sender, message, type, context)

        if(isEmailEnabled)
            sendEmail(sender, message, type, context)

        if(isWebhookEnabled)
            sendWebhook(sender, message, type, context)

        if (isHttpEnabled)
            sendHttp(sender, message, type, context)
    }

    fun sendBark(sender: String, message: String, type: String, context: Context?) {
        // Run network operation in a background thread
        Thread {
            try {
                // Retrieve the stored POST endpoint from SharedPreferences
                val sharedPreferences = context?.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                val endpoint = sharedPreferences?.getString("endpoint", "http://default-url.com")?.let {
                    if (!it.endsWith("/")) "$it/" else it
                }

                // Check if the endpoint is valid before proceeding
                if (endpoint.isNullOrEmpty()) {
                    Log.e("postToServer", "Endpoint is not configured.")
                    return@Thread
                }

                val deviceKey = sharedPreferences.getString("device_key", "")
                if (deviceKey.isNullOrEmpty()) {
                    Log.e("postToServer", "Device Key is not configured.")
                    return@Thread
                }

                // Construct the URL for the Bark server
                val barkUrl = Uri.parse(endpoint).buildUpon().appendPath("push").build().toString()

                // Prepare the payload for the Bark server
                val jsonPayload = """
                    {
                        "title": "${sender ?: "Unknown Sender"}",
                        "body": "${message ?: "No message content"}",
                        "category": "$type",
                        "device_key": "$deviceKey"
                    }
                """.trimIndent()

                Log.i("postToServer", "Bark URL: $barkUrl")
                Log.i("postToServer", "Json Payload: $jsonPayload")

                // Send POST request
                val url = URL(barkUrl)
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                urlConnection.doOutput = true

                val outputStream: OutputStream = BufferedOutputStream(urlConnection.outputStream)
                outputStream.use {
                    it.write(jsonPayload.toByteArray())
                    it.flush()
                }

                // Check the response
                val responseCode = urlConnection.responseCode
                val response = StringBuilder()

                Log.i("postToServer", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                    reader.use {
                        it.lines().forEach { line -> response.append(line) }
                    }
                    Log.i("postToServer", "Notification sent successfully: $response")
                } else {
                    val errorReader = BufferedReader(InputStreamReader(urlConnection.errorStream))
                    errorReader.use {
                        it.lines().forEach { line -> response.append(line) }
                    }
                    Log.e("postToServer", "Failed to send notification: $responseCode, Response: $response")
                }

                urlConnection.disconnect()

            } catch (e: FileNotFoundException) {
                Log.e("postToServer", "File not found error", e)
            } catch (e: IOException) {
                Log.e("postToServer", "IO error", e)
            } catch (e: Exception) {
                Log.e("postToServer", "Unexpected error", e)
            }
        }.start() // Starting the thread
    }

    fun sendEmail(mailSubject: String, message: String, type: String, context: Context?) {
        if (context == null) {
            Log.e("sendEmail", "Context is null")
            return
        }

        // Get email settings from SharedPreferences
        val sharedPreferences = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val smtpServer = sharedPreferences.getString("email_smtp", null)
        val useSSL = sharedPreferences.getBoolean("email_use_ssl", false)
        val port = sharedPreferences.getString("email_port", null)
        val username = sharedPreferences.getString("email_username", null)
        val password = sharedPreferences.getString("email_password", null)
        val recipient = sharedPreferences.getString("email_recipient", null)

        if (smtpServer.isNullOrEmpty() || port.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty()) {
            Log.e("sendEmail", "Email settings are not properly configured.")
            return
        }

        // Set up properties for SMTP
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.host", smtpServer)
            put("mail.smtp.port", port)

            if (useSSL) {
                put("mail.smtp.socketFactory.port", port)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.socketFactory.fallback", "false")
            } else {
                put("mail.smtp.starttls.enable", "true")  // Use STARTTLS if SSL is not enabled
            }
        }

        // Run the email sending operation in a new thread
        Thread {
            try {
                // Create a session with the provided username and password
                val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(username, password)
                    }
                })

                // Create a new email message
                val mimeMessage = MimeMessage(session).apply {
                    setFrom(InternetAddress(username))
                    setRecipient(Message.RecipientType.TO, InternetAddress(recipient))
                    subject = "[$type] $mailSubject"
                    setText(message)
                }

                // Send the email
                Transport.send(mimeMessage)
                Log.i("sendEmail", "Email sent successfully.")
            } catch (e: MessagingException) {
                Log.e("sendEmail", "Error sending email: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    fun sendWebhook(sender: String, message: String, type: String, context: Context?) {
        // Run network operation in a background thread
        Thread {
            try {
                // Retrieve the webhook URL from SharedPreferences
                val sharedPreferences = context?.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                val webhookUrl = sharedPreferences?.getString("webhook_endpoint", null)?.let {
                    if (!it.endsWith("/")) "$it/" else it
                }

                // Retrieve webhook headers from SharedPreferences (optional)
                val webhookHeadersJson = sharedPreferences?.getString("webhook_headers", "")

                // Check if the webhook URL is valid before proceeding
                if (webhookUrl.isNullOrEmpty()) {
                    Log.e("sendWebhook", "Webhook URL is not configured.")
                    return@Thread
                }

                // Prepare the payload for the webhook
                val jsonPayload = """
                {
                    "sender": "${sender.ifEmpty { "Unknown Sender" }}",
                    "message": "${message.ifEmpty { "No message content" }}",
                    "type": "$type"
                }
            """.trimIndent()

                Log.i("sendWebhook", "Webhook URL: $webhookUrl")
                Log.i("sendWebhook", "Json Payload: $jsonPayload")

                // Send POST request
                val url = URL(webhookUrl)
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                urlConnection.doOutput = true

                // Parse webhook headers from JSON and add them to the request (if provided)
                webhookHeadersJson?.let {
                    if (it.isNotEmpty()) {
                        try {
                            val headers = JSONObject(it)
                            headers.keys().forEach { key ->
                                val value = headers.getString(key)
                                urlConnection.setRequestProperty(key, value)
                            }
                        } catch (e: JSONException) {
                            Log.e("sendWebhook", "Invalid JSON for headers: $webhookHeadersJson", e)
                        }
                    }
                }

                val outputStream: OutputStream = BufferedOutputStream(urlConnection.outputStream)
                outputStream.use {
                    it.write(jsonPayload.toByteArray())
                    it.flush()
                }

                // Check the response
                val responseCode = urlConnection.responseCode
                val response = StringBuilder()

                Log.i("sendWebhook", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                    reader.use {
                        it.lines().forEach { line -> response.append(line) }
                    }
                    Log.i("sendWebhook", "Webhook sent successfully: $response")
                } else {
                    val errorReader = BufferedReader(InputStreamReader(urlConnection.errorStream))
                    errorReader.use {
                        it.lines().forEach { line -> response.append(line) }
                    }
                    Log.e("sendWebhook", "Failed to send webhook: $responseCode, Response: $response")
                }

                urlConnection.disconnect()

            } catch (e: FileNotFoundException) {
                Log.e("sendWebhook", "File not found error", e)
            } catch (e: IOException) {
                Log.e("sendWebhook", "IO error", e)
            } catch (e: Exception) {
                Log.e("sendWebhook", "Unexpected error", e)
            }
        }.start() // Starting the thread
    }

    fun sendHttp(sender: String, message: String, type: String, context: Context?) {
        // Run network operation in a background thread
        Thread {
            try {
                val sharedPreferences = context?.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                val httpEndpoint = sharedPreferences?.getString("http_endpoint", null)
                val httpHeadersJson = sharedPreferences?.getString("http_headers", "")
                val httpBodyTemplate = sharedPreferences?.getString("http_body_template", "{}")
                    ?: "{}"

                // Check if the HTTP endpoint is configured
                if (httpEndpoint.isNullOrEmpty()) {
                    Log.e("sendHttp", "HTTP endpoint is not configured.")
                    return@Thread
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentTime = sdf.format(Date())

                // Prepare the JSON payload by replacing placeholders
                val httpBody = httpBodyTemplate
                    .replace("<TYPE>", type)
                    .replace("<SENDER>", sender)
                    .replace("<MESSAGE>", message)
                    .replace("<TIMESTAMP>", currentTime)

                Log.i("sendHttp", "HTTP Endpoint: $httpEndpoint")
                Log.i("sendHttp", "HTTP Body: $httpBody")

                // Send POST request
                val url = URL(httpEndpoint)
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                urlConnection.doOutput = true

                // Parse and add headers
                httpHeadersJson?.let {
                    if (it.isNotEmpty()) {
                        try {
                            val headers = JSONObject(it)
                            headers.keys().forEach { key ->
                                val value = headers.getString(key)
                                urlConnection.setRequestProperty(key, value)
                            }
                        } catch (e: JSONException) {
                            Log.e("sendHttp", "Invalid JSON for headers: $httpHeadersJson", e)
                        }
                    }
                }

                // Write the body to the request
                val outputStream: OutputStream = BufferedOutputStream(urlConnection.outputStream)
                outputStream.use {
                    it.write(httpBody.toByteArray())
                    it.flush()
                }

                // Check the response
                val responseCode = urlConnection.responseCode
                val response = StringBuilder()

                Log.i("sendHttp", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                    reader.use {
                        it.lines().forEach { line -> response.append(line) }
                    }
                    Log.i("sendHttp", "HTTP request sent successfully: $response")
                } else {
                    val errorReader = BufferedReader(InputStreamReader(urlConnection.errorStream))
                    errorReader.use {
                        it.lines().forEach { line -> response.append(line) }
                    }
                    Log.e("sendHttp", "Failed to send HTTP request: $responseCode, Response: $response")
                }

                urlConnection.disconnect()

            } catch (e: FileNotFoundException) {
                Log.e("sendHttp", "File not found error", e)
            } catch (e: IOException) {
                Log.e("sendHttp", "IO error", e)
            } catch (e: Exception) {
                Log.e("sendHttp", "Unexpected error", e)
            }
        }.start() // Starting the thread
    }

    @SuppressLint("MissingPermission")
    fun getSimSlotIndex(context: Context?, subId: Int): Int {
        val subscriptionManager = context?.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(subId)
        return subscriptionInfo?.simSlotIndex ?: -1
    }

    @SuppressLint("MissingPermission")
    fun isDualSimDevice(context: Context?): Boolean {
        val subscriptionManager = context?.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
        return (activeSubscriptionInfoList?.size ?: 0) > 1
    }
}

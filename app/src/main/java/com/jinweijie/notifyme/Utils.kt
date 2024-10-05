package com.jinweijie.notifyme

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

object Utils {

    fun postToServer(sender: String?, message: String?, type: String, context: Context?) {
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
}

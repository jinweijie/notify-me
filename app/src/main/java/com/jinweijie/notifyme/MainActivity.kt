package com.jinweijie.notifyme

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jinweijie.notifyme.ui.theme.NotifyMeTheme
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : ComponentActivity() {

    private val SMS_PERMISSION_CODE = 101
    private val PHONE_PERMISSION_CODE = 102

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvCurrentEndpoint: TextView
    private lateinit var etEndpoint: EditText
    private lateinit var tvCurrentDeviceKey: TextView
    private lateinit var etDeviceKey: EditText
    private lateinit var btnSave: Button
    private lateinit var btnTest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestSmsPermission()
        checkAndRequestCallPermissions()

        initializeUi()
    }

    private fun initializeUi() {
        // Set the content view to the layout you created
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        tvCurrentEndpoint = findViewById(R.id.tv_current_endpoint)
        etEndpoint = findViewById(R.id.et_endpoint)
        tvCurrentDeviceKey = findViewById(R.id.tv_current_device_key)
        etDeviceKey = findViewById(R.id.et_device_key)
        btnSave = findViewById(R.id.btn_save)
        btnTest = findViewById(R.id.btn_test)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        // Load and display the current endpoint
        val savedEndpoint = sharedPreferences.getString("endpoint", "http://default-url.com")
        val savedDeviceKey = sharedPreferences.getString("device_key", "<your_device_key>")
        tvCurrentEndpoint.text = "Current Endpoint: $savedEndpoint"
        tvCurrentDeviceKey.text =  "Current DeviceKey: $savedDeviceKey"

        // Set click listener on the Save button
        btnSave.setOnClickListener {
            val newEndpoint = etEndpoint.text.toString().trim()
            if (newEndpoint.isNotEmpty()) {
                saveConfig("endpoint", newEndpoint)
                tvCurrentEndpoint.text = "Current Endpoint: $newEndpoint"
            }

            val newDeviceKey = etDeviceKey.text.toString().trim()
            if (newDeviceKey.isNotEmpty()) {
                saveConfig("device_key", newDeviceKey)
                tvCurrentDeviceKey.text = "Current Device Key: $newDeviceKey"
            }

            Toast.makeText(this, "Settings Saved.", Toast.LENGTH_LONG).show()
        }

        btnTest.setOnClickListener {
            Utils.postToServer("Tester", "This is Test message", "SMS", this)
            Toast.makeText(this, "Test notification sent.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveConfig(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {

            // Request the permissions
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE)
        }
    }

    private fun checkAndRequestCallPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG),
                PHONE_PERMISSION_CODE)
        }
    }

}
package com.jinweijie.notifyme

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    companion object {
        private const val SMS_PERMISSION_CODE = 101
        private const val PHONE_PERMISSION_CODE = 102
    }

    private lateinit var sharedPreferences: SharedPreferences

    // Bark UI elements
    private lateinit var cbEnableBark: CheckBox
    private lateinit var layoutBarkContent: LinearLayout
    private lateinit var etEndpoint: EditText
    private lateinit var etDeviceKey: EditText
    private lateinit var btnSaveBark: Button
    private lateinit var btnTestBark: Button

    // Email UI elements
    private lateinit var cbEnableEmail: CheckBox
    private lateinit var layoutEmailContent: LinearLayout
    private lateinit var etEmailSmtp: EditText
    private lateinit var etEmailPort: EditText
    private lateinit var etEmailUsername: EditText
    private lateinit var etEmailPassword: EditText
    private lateinit var etEmailRecipient: EditText
    private lateinit var cbUseSSL: CheckBox
    private lateinit var btnSaveEmail: Button
    private lateinit var btnTestEmail: Button

    // Webhook UI elements
    private lateinit var cbEnableWebhook: CheckBox
    private lateinit var layoutWebhookContent: LinearLayout
    private lateinit var etWebhookEndpoint: EditText
    private lateinit var etWebhookHeaders: EditText
    private lateinit var btnSaveWebhook: Button
    private lateinit var btnTestWebhook: Button

    // HTTP UI elements
    private lateinit var cbEnableHttp: CheckBox
    private lateinit var layoutHttpContent: LinearLayout
    private lateinit var etHttpEndpoint: EditText
    private lateinit var etHttpHeaders: EditText
    private lateinit var etHttpBodyTemplate: EditText
    private lateinit var btnSaveHttp: Button
    private lateinit var btnTestHttp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestSmsPermission()
        checkAndRequestCallPermissions()

        initializeUi()
    }

    private fun initializeUi() {
        setContentView(R.layout.activity_main)

        // Initialize Bark UI elements
        cbEnableBark = findViewById(R.id.cb_enable_bark)
        layoutBarkContent = findViewById(R.id.layout_bark_content)
        etEndpoint = findViewById(R.id.et_endpoint)
        etDeviceKey = findViewById(R.id.et_device_key)
        btnSaveBark = findViewById(R.id.btn_save_bark)
        btnTestBark = findViewById(R.id.btn_test_bark)

        // Initialize Email UI elements
        cbEnableEmail = findViewById(R.id.cb_enable_email)
        layoutEmailContent = findViewById(R.id.layout_email_content)
        etEmailSmtp = findViewById(R.id.et_email_smtp)
        etEmailPort = findViewById(R.id.et_email_port)
        etEmailUsername = findViewById(R.id.et_email_username)
        etEmailPassword = findViewById(R.id.et_email_password)
        etEmailRecipient = findViewById(R.id.et_email_recipient)
        cbUseSSL = findViewById(R.id.cb_use_ssl)
        btnSaveEmail = findViewById(R.id.btn_save_email)
        btnTestEmail = findViewById(R.id.btn_test_email)

        // Initialize Webhook UI elements
        cbEnableWebhook = findViewById(R.id.cb_enable_webhook)
        layoutWebhookContent = findViewById(R.id.layout_webhook_content)
        etWebhookEndpoint = findViewById(R.id.et_webhook_endpoint)
        etWebhookHeaders = findViewById(R.id.et_webhook_headers)
        btnSaveWebhook = findViewById(R.id.btn_save_webhook)
        btnTestWebhook = findViewById(R.id.btn_test_webhook)

        // Initialize HTTP UI elements
        cbEnableHttp = findViewById(R.id.cb_enable_http)
        layoutHttpContent = findViewById(R.id.layout_http_content)
        etHttpEndpoint = findViewById(R.id.et_http_endpoint)
        etHttpHeaders = findViewById(R.id.et_http_headers)
        etHttpBodyTemplate = findViewById(R.id.et_http_body_template)
        btnSaveHttp = findViewById(R.id.btn_save_http)
        btnTestHttp = findViewById(R.id.btn_test_http)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        // Load and display the current settings
        loadSavedSettings()

        // Set click listener on the Save button for Bark configuration
        btnSaveBark.setOnClickListener {
            saveBarkSettings()

            Toast.makeText(this, "Bark settings saved.", Toast.LENGTH_LONG).show()
        }

        // Set click listener on the Test button for Bark
        btnTestBark.setOnClickListener {
            saveBarkSettings()

            Utils.sendBark("Tester", "This is a test message", "SMS", this)
            Toast.makeText(this, "Test notification sent.", Toast.LENGTH_LONG).show()
        }

        // Set click listener on the Save button for Email configuration
        btnSaveEmail.setOnClickListener {
            saveEmailSettings()
            Toast.makeText(this, "Email settings saved.", Toast.LENGTH_LONG).show()
        }

        // Set click listener on the Test button for Email
        btnTestEmail.setOnClickListener {
            saveEmailSettings()

            Utils.sendEmail("Tester", "This is a test message", "SMS", this)
            Toast.makeText(this, "Test email sent.", Toast.LENGTH_LONG).show()
        }

        // Set click listener on the Save button for Webhook configuration
        btnSaveWebhook.setOnClickListener {
            saveWebhookSettings()
            Toast.makeText(this, "Webhook settings saved.", Toast.LENGTH_LONG).show()
        }

        // Set click listener on the Test button for Webhook
        btnTestWebhook.setOnClickListener {
            saveWebhookSettings()

            Utils.sendWebhook("Tester", "This is a test message", "SMS", this)
            Toast.makeText(this, "Test webhook sent.", Toast.LENGTH_LONG).show()
        }

        // Set click listener on Save and Test buttons for HTTP configuration
        btnSaveHttp.setOnClickListener {
            saveHttpSettings()
            Toast.makeText(this, "HTTP settings saved.", Toast.LENGTH_LONG).show()
        }

        btnTestHttp.setOnClickListener {
            saveHttpSettings()
            Utils.sendHttp("Tester", "This is a test message", "SMS", this)
            Toast.makeText(this, "Test HTTP sent.", Toast.LENGTH_LONG).show()
        }

        // Set change listeners for checkboxes
        cbEnableBark.setOnCheckedChangeListener { _, isChecked ->
            layoutBarkContent.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            saveBooleanConfig("enable_bark", isChecked)
        }

        cbEnableEmail.setOnCheckedChangeListener { _, isChecked ->
            layoutEmailContent.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            saveBooleanConfig("enable_email", isChecked)
        }

        cbEnableWebhook.setOnCheckedChangeListener { _, isChecked ->
            layoutWebhookContent.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            saveBooleanConfig("enable_webhook", isChecked)
        }

        cbEnableHttp.setOnCheckedChangeListener { _, isChecked ->
            layoutHttpContent.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            saveBooleanConfig("enable_http", isChecked)
        }
    }

    private fun saveBarkSettings() {
        val newEndpoint = etEndpoint.text.toString().trim()
        if (newEndpoint.isNotEmpty()) {
            saveConfig("endpoint", newEndpoint)
        }

        val newDeviceKey = etDeviceKey.text.toString().trim()
        if (newDeviceKey.isNotEmpty()) {
            saveConfig("device_key", newDeviceKey)
        }
    }

    private fun saveEmailSettings() {
        val smtpServer = etEmailSmtp.text.toString().trim()
        val useSSL = cbUseSSL.isChecked
        val port = etEmailPort.text.toString().trim()
        val username = etEmailUsername.text.toString().trim()
        val password = etEmailPassword.text.toString().trim()
        val recipient = etEmailRecipient.text.toString().trim()

        saveConfig("email_smtp", smtpServer)
        saveBooleanConfig("email_use_ssl", useSSL)
        saveConfig("email_port", port)
        saveConfig("email_username", username)
        saveConfig("email_password", password)
        saveConfig("email_recipient", recipient)
    }

    private fun saveWebhookSettings() {
        val webhookEndpoint = etWebhookEndpoint.text.toString().trim()
        if (webhookEndpoint.isNotEmpty()) {
            saveConfig("webhook_endpoint", webhookEndpoint)
        }

        val webhookHeaders = etWebhookHeaders.text.toString().trim()
        saveConfig("webhook_headers", webhookHeaders)
    }

    private fun saveHttpSettings() {
        val httpEndpoint = etHttpEndpoint.text.toString().trim()
        if (httpEndpoint.isNotEmpty()) {
            saveConfig("http_endpoint", httpEndpoint)
        }

        val httpHeaders = etHttpHeaders.text.toString().trim()
        saveConfig("http_headers", httpHeaders)

        val httpBodyTemplate = etHttpBodyTemplate.text.toString().trim()
        saveConfig("http_body_template", httpBodyTemplate)
    }
    private fun loadSavedSettings() {
        // Load Bark settings
        val isBarkEnabled = sharedPreferences.getBoolean("enable_bark", false)
        cbEnableBark.isChecked = isBarkEnabled
        layoutBarkContent.visibility = if (isBarkEnabled) LinearLayout.VISIBLE else LinearLayout.GONE

        val savedEndpoint = sharedPreferences.getString("endpoint", "")
        val savedDeviceKey = sharedPreferences.getString("device_key", "")
        etEndpoint.setText(savedEndpoint)
        etDeviceKey.setText(savedDeviceKey)

        // Load Email settings
        val isEmailEnabled = sharedPreferences.getBoolean("enable_email", false)
        cbEnableEmail.isChecked = isEmailEnabled
        layoutEmailContent.visibility = if (isEmailEnabled) LinearLayout.VISIBLE else LinearLayout.GONE

        val smtpServer = sharedPreferences.getString("email_smtp", "")
        val useSSL = sharedPreferences.getBoolean("email_use_ssl", false)
        val port = sharedPreferences.getString("email_port", "")
        val username = sharedPreferences.getString("email_username", "")
        val password = sharedPreferences.getString("email_password", "")
        val recipient = sharedPreferences.getString("email_recipient", "")

        etEmailSmtp.setText(smtpServer)
        cbUseSSL.isChecked = useSSL
        etEmailPort.setText(port)
        etEmailUsername.setText(username)
        etEmailPassword.setText(password)
        etEmailRecipient.setText(recipient)

        // Load Webhook settings
        val isWebhookEnabled = sharedPreferences.getBoolean("enable_webhook", false)
        cbEnableWebhook.isChecked = isWebhookEnabled
        layoutWebhookContent.visibility = if (isWebhookEnabled) LinearLayout.VISIBLE else LinearLayout.GONE

        val webhookEndpoint = sharedPreferences.getString("webhook_endpoint", "")
        val webhookHeaders = sharedPreferences.getString("webhook_headers", "")

        etWebhookEndpoint.setText(webhookEndpoint)
        etWebhookHeaders.setText(webhookHeaders)

        // Load HTTP settings
        val isHttpEnabled = sharedPreferences.getBoolean("enable_http", false)
        cbEnableHttp.isChecked = isHttpEnabled
        layoutHttpContent.visibility = if (isHttpEnabled) LinearLayout.VISIBLE else LinearLayout.GONE

        val httpEndpoint = sharedPreferences.getString("http_endpoint", "")
        val httpHeaders = sharedPreferences.getString("http_headers", "")
        val httpBodyTemplate = sharedPreferences.getString("http_body_template", """{"type": "<TYPE>", "sender":"<SENDER>", "message":"<MESSAGE>", "timestamp":"<TIMESTAMP>"}""")

        etHttpEndpoint.setText(httpEndpoint)
        etHttpHeaders.setText(httpHeaders)
        etHttpBodyTemplate.setText(httpBodyTemplate)
    }

    private fun saveConfig(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun saveBooleanConfig(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    private fun checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }

    private fun checkAndRequestCallPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG),
                PHONE_PERMISSION_CODE
            )
        }
    }
}

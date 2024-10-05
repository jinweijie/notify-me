package com.jinweijie.notifyme

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

class AppBackgroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        Toast.makeText(this, "NotifyMe Started.", Toast.LENGTH_LONG).show()
    }
}

package com.zjdavis.volimiter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        // Use device-protected storage — accessible before user unlocks the device
        val deviceContext = context.createDeviceProtectedStorageContext()
        val prefs = deviceContext.getSharedPreferences("volimiter_boot", Context.MODE_PRIVATE)
        val maxVolume = prefs.getInt("max_volume", 5)

        // Only start if Volimiter was previously configured
        if (prefs.getBoolean("was_running", false)) {
            val serviceIntent = Intent(context, VolimiterService::class.java)
            serviceIntent.putExtra("MAX_VOLUME", maxVolume)
            context.startForegroundService(serviceIntent)
        }
    }
}
package com.zjdavis.volimiter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        if (VolimiterSettings.wasRunningAtShutdown(context)) {
            val serviceIntent = Intent(context, VolimiterService::class.java).apply {
                putExtra(
                    VolimiterSettings.EXTRA_MAX_VOLUME,
                    VolimiterSettings.getBootConfig(context).maxVolume
                )
            }
            context.startForegroundService(serviceIntent)
        }
    }
}

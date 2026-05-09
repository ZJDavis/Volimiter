package com.example.volimiter

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.*
import androidx.core.app.NotificationCompat

class VolimiterService : Service() {

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var maxVolume = 5

    private val checkVolumeRunnable = object : Runnable {
        override fun run() {
            if (!isHeadsetConnected()) {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (current > maxVolume) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        maxVolume,
                        0
                    )
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    private fun isHeadsetConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { device ->
            device.type in listOf(
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_USB_HEADSET
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        maxVolume = intent?.getIntExtra("MAX_VOLUME", 5) ?: 5
        handler.post(checkVolumeRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkVolumeRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "volimiter")
            .setContentTitle("Volimiter Active")
            .setContentText("Max volume: $maxVolume / ${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "volimiter",
            "Volimiter",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
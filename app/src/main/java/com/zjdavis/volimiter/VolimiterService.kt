package com.zjdavis.volimiter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import androidx.core.app.NotificationCompat

class VolimiterService : Service() {

    private lateinit var audioManager: AudioManager
    private lateinit var monitorThread: HandlerThread
    private lateinit var monitorHandler: Handler
    @Volatile
    private var maxVolume = VolimiterSettings.DEFAULT_MAX_VOLUME
    private var headsetConnected = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshAudioRouteAndCheckVolume()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refreshAudioRouteAndCheckVolume()
        }
    }

    private val checkVolumeRunnable = object : Runnable {
        override fun run() {
            if (!headsetConnected) {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume > maxVolume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
                }
            }
            monitorHandler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        monitorThread = HandlerThread("VolimiterMonitor").apply { start() }
        monitorHandler = Handler(monitorThread.looper)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, monitorHandler)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        maxVolume = intent?.getIntExtra(
            VolimiterSettings.EXTRA_MAX_VOLUME,
            VolimiterSettings.DEFAULT_MAX_VOLUME
        ) ?: VolimiterSettings.getBootMaxVolume(this)

        startForeground(NOTIFICATION_ID, buildNotification())
        monitorHandler.post { refreshAudioRouteAndCheckVolume() }
        return START_STICKY
    }

    override fun onDestroy() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        monitorHandler.removeCallbacksAndMessages(null)
        monitorThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun refreshAudioRouteAndCheckVolume() {
        headsetConnected = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.type in HEADSET_DEVICE_TYPES }

        // A service can receive multiple start commands; always keep one monitoring loop.
        monitorHandler.removeCallbacks(checkVolumeRunnable)
        monitorHandler.post(checkVolumeRunnable)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Volimiter Active")
            .setContentText(
                "Max volume: $maxVolume / " +
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            )
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Volimiter",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 500L
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "volimiter"

        val HEADSET_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET
        )
    }
}

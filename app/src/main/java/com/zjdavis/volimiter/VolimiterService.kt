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
import java.time.LocalTime

class VolimiterService : Service() {
    private lateinit var audioManager: AudioManager
    private lateinit var monitorThread: HandlerThread
    private lateinit var monitorHandler: Handler
    private lateinit var config: LimiterConfig
    private var effectiveMaxVolume = VolimiterSettings.DEFAULT_MAX_VOLUME
    private var quietHoursActive = false
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
            enforceVolumeLimit()
            monitorHandler.postDelayed(this, VOLUME_CHECK_INTERVAL_MS)
        }
    }

    private val refreshScheduleRunnable = object : Runnable {
        override fun run() {
            val wasQuietHoursActive = quietHoursActive
            val previousLimit = effectiveMaxVolume
            refreshEffectiveLimit()
            if (quietHoursActive != wasQuietHoursActive || effectiveMaxVolume != previousLimit) {
                enforceVolumeLimit()
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            monitorHandler.postDelayed(this, SCHEDULE_CHECK_INTERVAL_MS)
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
        config = VolimiterSettings.getBootConfig(this)
        if (intent != null && intent.hasExtra(VolimiterSettings.EXTRA_MAX_VOLUME)) {
            config = config.copy(
                maxVolume = intent.getIntExtra(
                    VolimiterSettings.EXTRA_MAX_VOLUME,
                    config.maxVolume
                )
            )
        }
        refreshEffectiveLimit()
        startForeground(NOTIFICATION_ID, buildNotification())

        monitorHandler.post {
            refreshAudioRouteAndCheckVolume()
            monitorHandler.removeCallbacks(refreshScheduleRunnable)
            monitorHandler.post(refreshScheduleRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        monitorHandler.removeCallbacksAndMessages(null)
        monitorThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun refreshEffectiveLimit() {
        val now = LocalTime.now()
        val currentMinutes = now.hour * MINUTES_PER_HOUR + now.minute
        quietHoursActive = config.quietHoursEnabled && QuietHoursSchedule.isActive(
            currentMinutes,
            config.quietHoursStartMinutes,
            config.quietHoursEndMinutes
        )
        effectiveMaxVolume = if (quietHoursActive) {
            minOf(config.maxVolume, config.quietHoursVolume)
        } else {
            config.maxVolume
        }
    }

    private fun refreshAudioRouteAndCheckVolume() {
        headsetConnected = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.type in HEADSET_DEVICE_TYPES }
        monitorHandler.removeCallbacks(checkVolumeRunnable)
        monitorHandler.post(checkVolumeRunnable)
    }

    private fun enforceVolumeLimit() {
        if (!headsetConnected) {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVolume > effectiveMaxVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, effectiveMaxVolume, 0)
            }
        }
    }

    private fun buildNotification(): Notification {
        val mode = if (quietHoursActive) {
            getString(R.string.notification_quiet_hours)
        } else {
            getString(R.string.notification_standard_mode)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(
                getString(
                    R.string.notification_limit_format,
                    mode,
                    effectiveMaxVolume,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                )
            )
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val VOLUME_CHECK_INTERVAL_MS = 500L
        const val SCHEDULE_CHECK_INTERVAL_MS = 30_000L
        const val MINUTES_PER_HOUR = 60
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

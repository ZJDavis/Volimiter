package com.zjdavis.volimiter

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private val deviceAdminLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startLimiter(VolimiterSettings.getPendingVolume(this))
            } else {
                Toast.makeText(this, R.string.device_admin_required, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, VolimiterDeviceAdmin::class.java)
        showOnboardingIfNeeded()

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxSystemVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val label = findViewById<TextView>(R.id.label)
        val savedVolume = VolimiterSettings.getMaxVolume(this)

        seekBar.max = maxSystemVolume
        seekBar.progress = savedVolume
        label.text = getString(R.string.max_volume_format, savedVolume, maxSystemVolume)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                label.text = getString(R.string.max_volume_format, progress, maxSystemVolume)
            }

            override fun onStartTrackingTouch(sb: SeekBar) = Unit

            override fun onStopTrackingTouch(sb: SeekBar) {
                VolimiterSettings.setMaxVolume(this@MainActivity, sb.progress)
            }
        })

        findViewById<Button>(R.id.toggleBtn).setOnClickListener {
            if (!PinManager.hasPin(this)) {
                showSetPinDialog { requestDeviceAdminAndStart(seekBar.progress) }
            } else {
                requestDeviceAdminAndStart(seekBar.progress)
            }
        }

        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            showStopPinPrompt {
                devicePolicyManager.removeActiveAdmin(adminComponent)
                stopService(Intent(this, VolimiterService::class.java))
                saveBootState(false)
                Toast.makeText(this, R.string.volimiter_stopped, Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.privacyBtn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.privacy_policy_title)
                .setMessage(R.string.privacy_summary)
                .setPositiveButton(R.string.ok_button, null)
                .show()
        }
    }

    private fun requestDeviceAdminAndStart(volume: Int) {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_explanation)
                )
            }
            VolimiterSettings.setPendingVolume(this, volume)
            deviceAdminLauncher.launch(intent)
        } else {
            startLimiter(volume)
        }
    }

    private fun startLimiter(volume: Int) {
        VolimiterSettings.setMaxVolume(this, volume)
        val intent = Intent(this, VolimiterService::class.java).apply {
            putExtra(VolimiterSettings.EXTRA_MAX_VOLUME, volume)
        }
        startForegroundService(intent)
        saveBootState(true)
        Toast.makeText(this, R.string.volimiter_started, Toast.LENGTH_SHORT).show()
    }

    private fun showSetPinDialog(onSuccess: () -> Unit) {
        val input = EditText(this).apply {
            hint = getString(R.string.choose_pin_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.set_pin_title)
            .setMessage(R.string.set_pin_message)
            .setView(input)
            .setPositiveButton(R.string.set_pin_button) { _, _ ->
                val pin = input.text.toString()
                if (pin.length < 4) {
                    Toast.makeText(this, R.string.pin_min_length_error, Toast.LENGTH_SHORT).show()
                } else {
                    PinManager.savePin(this, pin)
                    Toast.makeText(this, R.string.pin_set, Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun showStopPinPrompt(onSuccess: () -> Unit) {
        val input = EditText(this).apply {
            hint = getString(R.string.enter_pin_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.enter_pin_to_stop)
            .setView(input)
            .setPositiveButton(R.string.confirm_button) { _, _ ->
                if (PinManager.checkPin(this, input.text.toString())) {
                    onSuccess()
                } else {
                    Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun saveBootState(running: Boolean) {
        VolimiterSettings.saveBootState(
            this,
            running,
            VolimiterSettings.getMaxVolume(this)
        )
    }

    private fun showOnboardingIfNeeded() {
        if (!VolimiterSettings.hasSeenOnboarding(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.onboarding_title)
                .setMessage(R.string.onboarding_message)
                .setPositiveButton(R.string.i_understand_button) { _, _ ->
                    VolimiterSettings.markOnboardingSeen(this)
                }
                .setNegativeButton(R.string.exit_button) { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }
}

package com.example.volimiter

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    companion object {
        const val REQUEST_DEVICE_ADMIN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("volimiter", Context.MODE_PRIVATE)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, VolimiterDeviceAdmin::class.java)

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxSystemVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val label = findViewById<TextView>(R.id.label)
        val startBtn = findViewById<Button>(R.id.toggleBtn)
        val stopBtn = findViewById<Button>(R.id.stopBtn)
        val hideBtn = findViewById<Button>(R.id.hideBtn)

        val savedVolume = prefs.getInt("max_volume", 5)
        seekBar.max = maxSystemVolume
        seekBar.progress = savedVolume
        label.text = "Max volume: $savedVolume / $maxSystemVolume"
        hideBtn.text = if (IconManager.isIconHidden(this)) "Show App Icon" else "Hide App Icon"
        hideBtn.setOnClickListener {
            if (IconManager.isIconHidden(this)) {
                showPinPrompt("Enter PIN to show app icon") {
                    IconManager.showIcon(this)
                    hideBtn.text = "Hide App Icon"
                    Toast.makeText(this, "Icon restored.", Toast.LENGTH_SHORT).show()
                }
            } else {
                showPinPrompt("Enter PIN to hide app icon") {
                    IconManager.hideIcon(this)
                    hideBtn.text = "Show App Icon"
                    Toast.makeText(this,
                        "Icon hidden. Open via Settings → Apps → Volimiter to restore it.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                label.text = "Max volume: $progress / $maxSystemVolume"
                prefs.edit().putInt("max_volume", progress).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        startBtn.setOnClickListener {
            if (!PinManager.hasPin(this)) {
                showSetPinDialog {
                    requestDeviceAdminAndStart(seekBar.progress)
                }
            } else {
                requestDeviceAdminAndStart(seekBar.progress)
            }
        }

        stopBtn.setOnClickListener {
            showPinPrompt("Enter PIN to stop Volimiter") {
                // Revoke device admin before stopping
                devicePolicyManager.removeActiveAdmin(adminComponent)
                stopService(Intent(this, VolimiterService::class.java))
                saveBootState(false)
                Toast.makeText(this, "Volimiter stopped.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestDeviceAdminAndStart(volume: Int) {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Volimiter needs device admin rights to prevent uninstallation."
                )
            }
            startActivityForResult(intent, REQUEST_DEVICE_ADMIN)
            // Store volume so we can start after the user approves
            prefs.edit().putInt("pending_volume", volume).apply()
        } else {
            startLimiter(volume)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEVICE_ADMIN && resultCode == RESULT_OK) {
            val volume = prefs.getInt("pending_volume", 5)
            startLimiter(volume)
        } else if (requestCode == REQUEST_DEVICE_ADMIN) {
            Toast.makeText(this, "Device admin not granted — uninstall protection inactive.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startLimiter(volume: Int) {
        val intent = Intent(this, VolimiterService::class.java)
        intent.putExtra("MAX_VOLUME", volume)
        startForegroundService(intent)
        saveBootState(true)
        Toast.makeText(this, "Volimiter started!", Toast.LENGTH_SHORT).show()
    }

    private fun showSetPinDialog(onSuccess: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Choose a 4-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Set Volimiter PIN")
            .setMessage("You'll need this PIN to stop Volimiter. Don't forget it!")
            .setView(input)
            .setPositiveButton("Set PIN") { _, _ ->
                val pin = input.text.toString()
                if (pin.length < 4) {
                    Toast.makeText(this, "PIN must be at least 4 digits.", Toast.LENGTH_SHORT).show()
                } else {
                    PinManager.savePin(this, pin)
                    Toast.makeText(this, "PIN set!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPinPrompt(title: String, onSuccess: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Enter PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                if (PinManager.checkPin(this, input.text.toString())) {
                    onSuccess()
                } else {
                    Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBootState(running: Boolean) {
        val deviceContext = createDeviceProtectedStorageContext()
        deviceContext.getSharedPreferences("volimiter_boot", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("was_running", running)
            .putInt("max_volume", prefs.getInt("max_volume", 5))
            .apply()
    }
}
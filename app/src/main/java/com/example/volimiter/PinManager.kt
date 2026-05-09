package com.example.volimiter

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object PinManager {

    private const val KEY_ALIAS = "volimiter_pin_key"
    private const val PREFS_NAME = "volimiter"
    private const val PREFS_PIN = "pin_encrypted"
    private const val PREFS_IV = "pin_iv"

    // Generate (or retrieve existing) AES key in Keystore
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGen.generateKey()
    }

    fun savePin(context: Context, pin: String) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encrypted = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREFS_PIN, Base64.encodeToString(encrypted, Base64.DEFAULT))
            .putString(PREFS_IV, Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()
    }

    fun checkPin(context: Context, candidate: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedB64 = prefs.getString(PREFS_PIN, null) ?: return false
        val ivB64 = prefs.getString(PREFS_IV, null) ?: return false

        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

            val decrypted = cipher.doFinal(Base64.decode(encryptedB64, Base64.DEFAULT))
            String(decrypted, Charsets.UTF_8) == candidate
        } catch (e: Exception) {
            false
        }
    }

    fun hasPin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_PIN, null) != null
    }

    fun clearPin(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(PREFS_PIN)
            .remove(PREFS_IV)
            .apply()
    }
}
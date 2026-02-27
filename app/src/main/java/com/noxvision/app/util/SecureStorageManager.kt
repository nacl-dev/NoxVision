package com.noxvision.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecureStorageManager {
    fun encrypt(data: String): String
    fun decrypt(encryptedData: String): String
}

class KeyStoreSecureStorageManager : SecureStorageManager {
    private val keyAlias = "NoxVisionWifiKey"
    private val keyStoreType = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null)

        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                keyStoreType
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            return keyGenerator.generateKey()
        }
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    override fun encrypt(data: String): String {
        try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ciphertextString = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            return "$ivString:$ciphertextString"
        } catch (e: Exception) {
            // In case of error (e.g. key invalidated), return original data to prevent data loss
            // though it won't be encrypted. Ideally we should log this.
            AppLogger.log("Encryption failed: ${e.message}", AppLogger.LogType.ERROR)
            return data
        }
    }

    override fun decrypt(encryptedData: String): String {
        try {
            val parts = encryptedData.split(":")
            if (parts.size != 2) {
                // Not in our format, assume legacy plaintext
                return encryptedData
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Decryption failed, likely because data was not encrypted or key changed.
            // Return original data (legacy plaintext fallback).
            AppLogger.log("Decryption failed: ${e.message}", AppLogger.LogType.ERROR)
            return encryptedData
        }
    }
}

class TestSecureStorageManager : SecureStorageManager {
    override fun encrypt(data: String): String {
        return "ENC:$data"
    }

    override fun decrypt(encryptedData: String): String {
        return if (encryptedData.startsWith("ENC:")) {
            encryptedData.substring(4)
        } else {
            encryptedData
        }
    }
}

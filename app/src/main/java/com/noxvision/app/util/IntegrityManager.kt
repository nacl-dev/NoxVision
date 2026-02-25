package com.noxvision.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

interface IntegrityManager {
    fun computeChecksum(value: Int): String
}

/**
 * Legacy checksum implementation using hardcoded salt (weak).
 * Used for migration and fallback.
 */
class LegacyIntegrityManager : IntegrityManager {
    private val securitySalt = "noxvision_bounty_salt_v1"

    override fun computeChecksum(value: Int): String {
        val input = "$value$securitySalt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Secure checksum implementation using Android KeyStore HMAC-SHA256.
 * Ensures the key cannot be extracted from the device.
 */
class KeyStoreIntegrityManager : IntegrityManager {
    private val keyAlias = "BountyIntegrityKey"
    private val keyStoreType = "AndroidKeyStore"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null)

        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                keyStoreType
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_SIGN)
                    .build()
            )
            return keyGenerator.generateKey()
        }
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    override fun computeChecksum(value: Int): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(getSecretKey())
            val bytes = mac.doFinal(value.toString().toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback in case of KeyStore error (should be rare)
            // returning empty string will fail validation, resetting to 0 (safe default)
            ""
        }
    }
}

/**
 * Simple checksum implementation for unit tests.
 * Avoids Android KeyStore dependencies.
 */
class TestIntegrityManager : IntegrityManager {
    override fun computeChecksum(value: Int): String {
        // Deterministic but simple checksum for testing
        return "test_checksum_$value"
    }
}

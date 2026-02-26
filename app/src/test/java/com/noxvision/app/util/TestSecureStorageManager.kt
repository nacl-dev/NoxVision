package com.noxvision.app.util

/**
 * Simple implementation for unit testing that doesn't rely on Android KeyStore or Base64.
 * Prefixes data with "ENC:" to simulate encryption.
 */
class TestSecureStorageManager : SecureStorageManager {
    override fun encrypt(data: String): String {
        return "ENC:$data"
    }

    override fun decrypt(encryptedData: String): String {
        if (encryptedData.startsWith("ENC:")) {
            return encryptedData.substring(4)
        }
        throw IllegalArgumentException("Not encrypted with TestSecureStorageManager")
    }
}

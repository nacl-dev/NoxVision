package com.noxvision.app.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Test double for [KeyStoreIntegrityManager].
 * Uses a fixed HMAC key so checksums are deterministic in unit tests without Android KeyStore.
 */
class TestIntegrityManager : IntegrityManager {
    private val testKey = SecretKeySpec("TestBountyIntegrityKey!!".toByteArray(), "HmacSHA256")

    override fun computeChecksum(value: Int): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(testKey)
        val bytes = mac.doFinal(value.toString().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

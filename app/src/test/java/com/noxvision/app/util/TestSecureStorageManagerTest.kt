package com.noxvision.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TestSecureStorageManagerTest {

    @Test
    fun testEncryption() {
        val manager = TestSecureStorageManager()
        val original = "mySecretPassword"
        val encrypted = manager.encrypt(original)
        assertEquals("ENC:$original", encrypted)
    }

    @Test
    fun testDecryption() {
        val manager = TestSecureStorageManager()
        val encrypted = "ENC:mySecretPassword"
        val decrypted = manager.decrypt(encrypted)
        assertEquals("mySecretPassword", decrypted)
    }

    @Test
    fun testLegacyFallback() {
        val manager = TestSecureStorageManager()
        val legacy = "12345678"
        // Should return as-is because it doesn't start with ENC:
        val decrypted = manager.decrypt(legacy)
        assertEquals("12345678", decrypted)
    }
}

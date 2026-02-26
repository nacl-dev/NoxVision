package com.noxvision.app

import android.content.SharedPreferences
import com.noxvision.app.util.TestSecureStorageManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CameraSettingsTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var testSecureStorageManager: TestSecureStorageManager

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        testSecureStorageManager = TestSecureStorageManager()
        CameraSettings.setSecureStorageManager(testSecureStorageManager)
    }

    @Test
    fun `test setWifiPassword encrypts password`() {
        val password = "mySecretPassword"

        // Save using the new method (simulating CameraSettings behavior)
        CameraSettings.setWifiPassword(fakePrefs, password)

        // Check what's stored in prefs directly
        val storedValue = fakePrefs.getString("wifi_password", null)

        // It should be encrypted (TestSecureStorageManager prefixes with "ENC:")
        assertEquals("ENC:$password", storedValue)
    }

    @Test
    fun `test getWifiPassword decrypts password`() {
        val password = "mySecretPassword"
        val encrypted = testSecureStorageManager.encrypt(password)

        // Setup prefs with encrypted value
        fakePrefs.put("wifi_password", encrypted)

        // Retrieve using CameraSettings
        val retrieved = CameraSettings.getWifiPassword(fakePrefs)

        assertEquals(password, retrieved)
    }

    @Test
    fun `test getWifiPassword handles legacy plaintext`() {
        val legacyPassword = "legacyPassword123"

        // Setup prefs with plaintext value (simulating old version data)
        fakePrefs.put("wifi_password", legacyPassword)

        // Retrieve using CameraSettings
        val retrieved = CameraSettings.getWifiPassword(fakePrefs)

        // Should return the plaintext because decryption fails (doesn't start with ENC:)
        assertEquals(legacyPassword, retrieved)
    }

    @Test
    fun `test setWifiPassword handles encryption failure by saving plaintext`() {
        // Create a faulty storage manager
        val faultyManager = object : com.noxvision.app.util.SecureStorageManager {
            override fun encrypt(data: String): String {
                throw RuntimeException("Encryption broken")
            }
            override fun decrypt(encryptedData: String): String {
                return encryptedData
            }
        }
        CameraSettings.setSecureStorageManager(faultyManager)

        val password = "fallbackPassword"
        CameraSettings.setWifiPassword(fakePrefs, password)

        // Should be stored as plaintext due to fallback logic
        val storedValue = fakePrefs.getString("wifi_password", null)
        assertEquals(password, storedValue)
    }
}

// Minimal FakeSharedPreferences implementation for this test
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = data

    override fun getString(key: String?, defValue: String?): String? {
        return data[key] as? String ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return data[key] as? MutableSet<String> ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return data[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return data[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return data[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return data[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    fun put(key: String, value: Any?) {
        if (value == null) {
            data.remove(key)
        } else {
            data[key] = value
        }
    }
}

class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
    private val changes = mutableMapOf<String, Any?>()

    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        key?.let { changes[it] = values }
        return this
    }

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun remove(key: String?): SharedPreferences.Editor {
        key?.let { changes[it] = null }
        return this
    }

    override fun clear(): SharedPreferences.Editor {
        // Not implemented for fake
        return this
    }

    override fun commit(): Boolean {
        apply()
        return true
    }

    override fun apply() {
        changes.forEach { (k, v) ->
            prefs.put(k, v)
        }
        changes.clear()
    }
}

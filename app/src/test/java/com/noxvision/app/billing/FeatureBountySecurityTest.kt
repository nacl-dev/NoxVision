package com.noxvision.app.billing

import android.content.SharedPreferences
import com.noxvision.app.util.LegacyIntegrityManager
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class FeatureBountySecurityTest {

    private val securitySalt = "noxvision_bounty_salt_v1"

    private fun computeChecksum(value: Int): String {
        val input = "$value$securitySalt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testSecureSaveAndLoad() {
        val fakePrefs = FakeSharedPreferencesSecurity()
        // Use LegacyIntegrityManager to match the test's expectation of SHA-256 + Salt
        val integrityManager = LegacyIntegrityManager()
        val repo = FeatureBountyRepository(fakePrefs, integrityManager)

        repo.addCredits(100)
        assertEquals(100, repo.userCredits.value)

        // Verify it was saved with checksum
        val savedValue = fakePrefs.getInt("user_credits", -1)
        val savedChecksum = fakePrefs.getString("user_credits_checksum", null)

        assertEquals(100, savedValue)
        assertEquals(computeChecksum(100), savedChecksum)

        // Reload repo
        val repo2 = FeatureBountyRepository(fakePrefs, integrityManager)
        assertEquals(100, repo2.userCredits.value)
    }

    @Test
    fun testMigration_TrustFirstUse() {
        val fakePrefs = FakeSharedPreferencesSecurity()
        // Simulate legacy data (value without checksum)
        fakePrefs.put("user_credits", 500)

        // Load repo
        val integrityManager = LegacyIntegrityManager()
        val repo = FeatureBountyRepository(fakePrefs, integrityManager)

        // Should trust the value
        assertEquals(500, repo.userCredits.value)

        // Should have added checksum
        val savedChecksum = fakePrefs.getString("user_credits_checksum", null)
        assertEquals(computeChecksum(500), savedChecksum)
    }

    @Test
    fun testTampering_ResetToZero() {
        val fakePrefs = FakeSharedPreferencesSecurity()
        val integrityManager = LegacyIntegrityManager()
        val repo = FeatureBountyRepository(fakePrefs, integrityManager)

        repo.addCredits(100)

        // Tamper with value: Change 100 to 9999
        fakePrefs.put("user_credits", 9999)
        // Checksum remains for 100

        // Reload repo
        val repo2 = FeatureBountyRepository(fakePrefs, integrityManager)

        // Should detect tampering and reset to 0
        assertEquals(0, repo2.userCredits.value)

        // Should have updated checksum for 0
        val savedChecksum = fakePrefs.getString("user_credits_checksum", null)
        assertEquals(computeChecksum(0), savedChecksum)
        assertEquals(0, fakePrefs.getInt("user_credits", -1))
    }
}

// Copied and adapted FakeSharedPreferences for this test file
class FakeSharedPreferencesSecurity : SharedPreferences {
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

    override fun edit(): SharedPreferences.Editor = FakeEditorSecurity(this)

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

class FakeEditorSecurity(private val prefs: FakeSharedPreferencesSecurity) : SharedPreferences.Editor {
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

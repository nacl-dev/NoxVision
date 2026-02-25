package com.noxvision.app.billing

import android.content.SharedPreferences
import com.noxvision.app.util.LegacyIntegrityManager
import com.noxvision.app.util.TestIntegrityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureBountyRepositoryTest {

    @Test
    fun testTransactionPersistence() {
        val fakePrefs = FakeSharedPreferences()
        val repository = FeatureBountyRepository(fakePrefs, TestIntegrityManager())

        // Initial state
        assertTrue(repository.transactions.value.isEmpty())

        // Save a transaction
        val transaction = CreditTransaction(
            id = "t1",
            timestamp = 1000L,
            amount = 100,
            description = "Test Purchase",
            type = TransactionType.PURCHASE
        )
        repository.saveTransaction(transaction)

        // Verify in memory
        assertEquals(1, repository.transactions.value.size)
        assertEquals(transaction, repository.transactions.value[0])

        // Verify persistence (reloading)
        val repo2 = FeatureBountyRepository(fakePrefs, TestIntegrityManager())
        assertEquals(1, repo2.transactions.value.size)
        val loadedTransaction = repo2.transactions.value[0]
        assertEquals(transaction.id, loadedTransaction.id)
        assertEquals(transaction.timestamp, loadedTransaction.timestamp)
        assertEquals(transaction.amount, loadedTransaction.amount)
        assertEquals(transaction.description, loadedTransaction.description)
        assertEquals(transaction.type, loadedTransaction.type)
    }

    @Test
    fun testAddCredits() {
        val fakePrefs = FakeSharedPreferences()
        val repository = FeatureBountyRepository(fakePrefs, TestIntegrityManager())

        repository.addCredits(500)

        assertEquals(500, repository.userCredits.value)
        assertEquals(1, repository.transactions.value.size)
        val transaction = repository.transactions.value[0]
        assertEquals(500, transaction.amount)
        assertEquals(TransactionType.PURCHASE, transaction.type)
        assertEquals("Purchased credits", transaction.description)
    }

    @Test
    fun testDonateToBounty() {
        val fakePrefs = FakeSharedPreferences()
        val repository = FeatureBountyRepository(fakePrefs, TestIntegrityManager())

        // Setup initial credits
        repository.addCredits(1000)

        // Get a bounty ID
        val bountyId = repository.bounties.value[0].id
        val bountyTitle = repository.bounties.value[0].title

        // Donate
        val success = repository.donateToBounty(bountyId, 200)

        assertTrue(success)
        assertEquals(800, repository.userCredits.value)

        // Check transactions (should be 2 now: purchase and donation)
        assertEquals(2, repository.transactions.value.size)

        val donationTransaction = repository.transactions.value[0] // Most recent
        assertEquals(-200, donationTransaction.amount)
        assertEquals(TransactionType.DONATION, donationTransaction.type)
        assertEquals("Donated to $bountyTitle", donationTransaction.description)
    }

    @Test
    fun testMigration() {
        val fakePrefs = FakeSharedPreferences()

        // Setup legacy data manually using LegacyIntegrityManager
        val legacyManager = LegacyIntegrityManager()
        val value = 100
        val key = "user_credits"
        fakePrefs.put(key, value)
        fakePrefs.put("${key}_checksum", legacyManager.computeChecksum(value))

        // Initialize repo with TestIntegrityManager (simulating new secure manager)
        val testManager = TestIntegrityManager()
        val repository = FeatureBountyRepository(fakePrefs, testManager)

        // Verify value is loaded (migration successful)
        assertEquals(value, repository.userCredits.value)

        // Verify checksum is updated in prefs to the new format
        val updatedChecksum = fakePrefs.getString("${key}_checksum", null)
        assertEquals(testManager.computeChecksum(value), updatedChecksum)
    }
}

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

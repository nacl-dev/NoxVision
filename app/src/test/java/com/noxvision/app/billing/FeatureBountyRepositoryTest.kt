package com.noxvision.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FeatureBountyRepositoryTest {

    class FakeBountyStorage : BountyStorage {
        var credits = 0
        val bountyProgress = mutableMapOf<String, Int>()
        private val _activities = mutableListOf<BountyActivity>()

        override fun getUserCredits(): Int = credits

        override fun setUserCredits(credits: Int) {
            this.credits = credits
        }

        override fun getBountyProgress(bountyId: String): Int = bountyProgress[bountyId] ?: 0

        override fun setBountyProgress(bountyId: String, progress: Int) {
            bountyProgress[bountyId] = progress
        }

        override fun getActivities(): List<BountyActivity> = _activities

        override fun addActivity(activity: BountyActivity) {
            _activities.add(0, activity)
        }
    }

    @Test
    fun testAddCredits() {
        val storage = FakeBountyStorage()
        val repository = FeatureBountyRepository(storage)

        repository.addCredits(100)

        assertEquals(100, storage.getUserCredits())
        assertEquals(100, repository.userCredits.value)

        val activities = storage.getActivities()
        assertEquals(1, activities.size)
        assertEquals(100, activities[0].amount)
        assertEquals("Credits Purchased", activities[0].description)
    }

    @Test
    fun testDonateToBounty() {
        val storage = FakeBountyStorage()
        val repository = FeatureBountyRepository(storage)

        repository.addCredits(100)

        // Find a valid bounty ID from the default list
        val bountyId = repository.bounties.value.first().id

        val success = repository.donateToBounty(bountyId, 50)

        assertTrue(success)
        assertEquals(50, storage.getUserCredits())
        assertEquals(50, repository.userCredits.value)
        assertEquals(50, storage.getBountyProgress(bountyId))

        val activities = storage.getActivities()
        assertEquals(2, activities.size)

        // Latest activity should be donation
        val donationActivity = activities[0]
        assertEquals(-50, donationActivity.amount)
        assertTrue(donationActivity.description.startsWith("Donated to"))
    }
}

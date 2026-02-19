package com.noxvision.app.billing

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class BountyStatus {
    ACTIVE, IN_DEV, SHIPPED
}

data class FeatureBounty(
    val id: String,
    val title: String,
    val description: String,
    val currentCredits: Int,
    val goalCredits: Int,
    val status: BountyStatus = BountyStatus.ACTIVE
)

data class BountyActivity(
    val id: String,
    val timestamp: Long,
    val description: String,
    val amount: Int
)

interface BountyStorage {
    fun getUserCredits(): Int
    fun setUserCredits(credits: Int)
    fun getBountyProgress(bountyId: String): Int
    fun setBountyProgress(bountyId: String, progress: Int)
    fun getActivities(): List<BountyActivity>
    fun addActivity(activity: BountyActivity)
}

class SharedPreferencesBountyStorage(context: Context) : BountyStorage {
    private val prefs = context.getSharedPreferences("feature_bounties_prefs", Context.MODE_PRIVATE)

    override fun getUserCredits(): Int {
        return prefs.getInt("user_credits", 0)
    }

    override fun setUserCredits(credits: Int) {
        prefs.edit { putInt("user_credits", credits) }
    }

    override fun getBountyProgress(bountyId: String): Int {
        return prefs.getInt("bounty_progress_$bountyId", 0)
    }

    override fun setBountyProgress(bountyId: String, progress: Int) {
        prefs.edit { putInt("bounty_progress_$bountyId", progress) }
    }

    override fun getActivities(): List<BountyActivity> {
        val jsonString = prefs.getString("activities", "[]") ?: "[]"
        val activities = mutableListOf<BountyActivity>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                activities.add(
                    BountyActivity(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        description = obj.getString("description"),
                        amount = obj.getInt("amount")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Sort by timestamp descending
        return activities.sortedByDescending { it.timestamp }
    }

    override fun addActivity(activity: BountyActivity) {
        val currentActivities = getActivities().toMutableList()
        currentActivities.add(0, activity) // Add to top

        // Limit to 50 activities to avoid huge JSON string
        if (currentActivities.size > 50) {
            currentActivities.removeAt(currentActivities.size - 1)
        }

        val jsonArray = JSONArray()
        currentActivities.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("timestamp", it.timestamp)
            obj.put("description", it.description)
            obj.put("amount", it.amount)
            jsonArray.put(obj)
        }
        prefs.edit { putString("activities", jsonArray.toString()) }
    }
}

class FeatureBountyRepository(private val storage: BountyStorage) {

    constructor(context: Context) : this(SharedPreferencesBountyStorage(context))

    // User's available credits
    private val _userCredits = MutableStateFlow(storage.getUserCredits())
    val userCredits = _userCredits.asStateFlow()

    // Bounties
    private val _bounties = MutableStateFlow(loadInitialBounties())
    val bounties = _bounties.asStateFlow()

    // Activities
    private val _activities = MutableStateFlow(storage.getActivities())
    val activities = _activities.asStateFlow()

    private fun loadInitialBounties(): List<FeatureBounty> {
        // In a real app, fetch from backend. Here we simulate.
        
        val bountyDefinitions = listOf(
            FeatureBounty("bounty_infiray", "Infiray Support", "Native support for Infiray thermal cameras (e.g., T2 Pro, P2 Pro). Funding helps purchase devices for development.", 0, 25000, BountyStatus.ACTIVE),
            FeatureBounty("bounty_hikmicro", "Hikmicro Support", "Full integration for Hikmicro devices. Funding covers device acquisition and SDK implementation.", 0, 30000, BountyStatus.ACTIVE),
            FeatureBounty("bounty_fliir", "FLIR Support", "Support for FLIR One and other FLIR thermal cameras.", 0, 35000, BountyStatus.ACTIVE),
            FeatureBounty("bounty_guide_new", "Guide Sensmart New Gen", "Support for latest Guide Sensmart models (TB, TD series).", 0, 20000, BountyStatus.SHIPPED),
            FeatureBounty("bounty_topdon", "Topdon TC Support", "Support for Topdon thermal cameras.", 0, 15000, BountyStatus.IN_DEV)
        )

        return bountyDefinitions.map { bounty ->
            val savedProgress = storage.getBountyProgress(bounty.id)
            bounty.copy(currentCredits = savedProgress)
        }
    }

    fun addCredits(amount: Int) {
        _userCredits.update { current ->
            val newBalance = current + amount
            storage.setUserCredits(newBalance)
            newBalance
        }

        // Log Activity
        val activity = BountyActivity(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            description = "Credits Purchased",
            amount = amount
        )
        storage.addActivity(activity)
        _activities.value = storage.getActivities()
    }

    fun donateToBounty(bountyId: String, amount: Int): Boolean {
        if (_userCredits.value < amount) return false

        // Deduct from user
        _userCredits.update { current ->
            val newBalance = current - amount
            storage.setUserCredits(newBalance)
            newBalance
        }

        // Add to bounty
        var bountyTitle = "Unknown Bounty"
        _bounties.update { currentList ->
            currentList.map { bounty ->
                if (bounty.id == bountyId) {
                    bountyTitle = bounty.title
                    val newProgress = bounty.currentCredits + amount
                    storage.setBountyProgress(bounty.id, newProgress)
                    bounty.copy(currentCredits = newProgress)
                } else {
                    bounty
                }
            }
        }

        // Log Activity
        val activity = BountyActivity(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            description = "Donated to $bountyTitle",
            amount = -amount
        )
        storage.addActivity(activity)
        _activities.value = storage.getActivities()

        return true
    }
}

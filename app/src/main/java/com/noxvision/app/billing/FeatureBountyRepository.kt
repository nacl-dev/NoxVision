package com.noxvision.app.billing

import android.content.Context
import android.content.SharedPreferences
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

enum class TransactionType {
    PURCHASE, DONATION
}

data class CreditTransaction(
    val id: String,
    val timestamp: Long,
    val amount: Int,
    val description: String,
    val type: TransactionType
)

data class FeatureBounty(
    val id: String,
    val title: String,
    val description: String,
    val currentCredits: Int,
    val goalCredits: Int,
    val status: BountyStatus = BountyStatus.ACTIVE
)

class FeatureBountyRepository(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("feature_bounties_prefs", Context.MODE_PRIVATE))

    // User's available credits
    private val _userCredits = MutableStateFlow(prefs.getInt("user_credits", 0))
    val userCredits = _userCredits.asStateFlow()

    // Transactions
    private val _transactions = MutableStateFlow(loadTransactions())
    val transactions = _transactions.asStateFlow()

    // Bounties
    private val _bounties = MutableStateFlow(loadInitialBounties())
    val bounties = _bounties.asStateFlow()

    private fun loadTransactions(): List<CreditTransaction> {
        val jsonString = prefs.getString("credit_transactions", "[]") ?: "[]"
        val list = mutableListOf<CreditTransaction>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CreditTransaction(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        amount = obj.getInt("amount"),
                        description = obj.getString("description"),
                        type = TransactionType.valueOf(obj.getString("type"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    internal fun saveTransaction(transaction: CreditTransaction) {
        _transactions.update { currentList ->
            val newList = currentList.toMutableList()
            newList.add(0, transaction)
            newList
        }

        val listToSave = _transactions.value
        val jsonArray = JSONArray()
        listToSave.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("timestamp", t.timestamp)
                put("amount", t.amount)
                put("description", t.description)
                put("type", t.type.name)
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString("credit_transactions", jsonArray.toString()) }
    }

    private fun loadInitialBounties(): List<FeatureBounty> {
        // In a real app, fetch from backend. Here we simulate.
        // We also need to load "currentCredits" for each bounty from local storage if we want them to persist locally.
        
        val bountyDefinitions = listOf(
            FeatureBounty("bounty_infiray", "Infiray Support", "Native support for Infiray thermal cameras (e.g., T2 Pro, P2 Pro). Funding helps purchase devices for development.", 0, 25000, BountyStatus.ACTIVE),
            FeatureBounty("bounty_hikmicro", "Hikmicro Support", "Full integration for Hikmicro devices. Funding covers device acquisition and SDK implementation.", 0, 30000, BountyStatus.ACTIVE),
            FeatureBounty("bounty_fliir", "FLIR Support", "Support for FLIR One and other FLIR thermal cameras.", 0, 35000, BountyStatus.ACTIVE),
            FeatureBounty("bounty_guide_new", "Guide Sensmart New Gen", "Support for latest Guide Sensmart models (TB, TD series).", 0, 20000, BountyStatus.SHIPPED),
            FeatureBounty("bounty_topdon", "Topdon TC Support", "Support for Topdon thermal cameras.", 0, 15000, BountyStatus.IN_DEV)
        )

        return bountyDefinitions.map { bounty ->
            val savedProgress = prefs.getInt("bounty_progress_${bounty.id}", 0)
            bounty.copy(currentCredits = savedProgress)
        }
    }

    fun addCredits(amount: Int) {
        _userCredits.update { current ->
            val newBalance = current + amount
            prefs.edit { putInt("user_credits", newBalance) }
            newBalance
        }
        saveTransaction(
            CreditTransaction(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                amount = amount,
                description = "Purchased credits",
                type = TransactionType.PURCHASE
            )
        )
    }

    fun donateToBounty(bountyId: String, amount: Int): Boolean {
        if (_userCredits.value < amount) return false

        // Deduct from user
        _userCredits.update { current ->
            val newBalance = current - amount
            prefs.edit { putInt("user_credits", newBalance) }
            newBalance
        }

        var bountyTitle = ""
        // Add to bounty
        _bounties.update { currentList ->
            currentList.map { bounty ->
                if (bounty.id == bountyId) {
                    bountyTitle = bounty.title
                    val newProgress = bounty.currentCredits + amount
                    prefs.edit { putInt("bounty_progress_${bounty.id}", newProgress) }
                    bounty.copy(currentCredits = newProgress)
                } else {
                    bounty
                }
            }
        }

        saveTransaction(
            CreditTransaction(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                amount = -amount,
                description = "Donated to $bountyTitle",
                type = TransactionType.DONATION
            )
        )
        return true
    }
}

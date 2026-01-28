package com.noxvision.app.billing

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

class FeatureBountyRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("feature_bounties_prefs", Context.MODE_PRIVATE)

    // User's available credits
    private val _userCredits = MutableStateFlow(prefs.getInt("user_credits", 0))
    val userCredits = _userCredits.asStateFlow()

    // Bounties
    private val _bounties = MutableStateFlow(loadInitialBounties())
    val bounties = _bounties.asStateFlow()

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
    }

    fun donateToBounty(bountyId: String, amount: Int): Boolean {
        if (_userCredits.value < amount) return false

        // Deduct from user
        _userCredits.update { current ->
            val newBalance = current - amount
            prefs.edit { putInt("user_credits", newBalance) }
            newBalance
        }

        // Add to bounty
        _bounties.update { currentList ->
            currentList.map { bounty ->
                if (bounty.id == bountyId) {
                    val newProgress = bounty.currentCredits + amount
                    prefs.edit { putInt("bounty_progress_${bounty.id}", newProgress) }
                    bounty.copy(currentCredits = newProgress)
                } else {
                    bounty
                }
            }
        }
        return true
    }
}

package com.opsc6311.poe.data.models

import java.util.Date

data class QuestCoins(
    val userId: String = "",
    val totalEarned: Int = 0,
    val totalRedeemed: Int = 0,
    val currentBalance: Int = 0,
    val lastUpdated: Date = Date()
) {
    val availableBalance: Int
        get() = currentBalance

    companion object {
        const val CONVERSION_RATE = 0.04 // 250 Quest Coins = R10
        const val MIN_REDEMPTION = 250 // Minimum Quest Coins needed for redemption
    }
}

package vc.prog3c.poe.data.models

import vc.prog3c.poe.core.utils.LevelingHelper
import java.util.Date

data class QuestCoins(
    val userId: String,
    val totalEarned: Int = 0,
    val totalRedeemed: Int = 0,
    val currentBalance: Int = 0,
    val lastUpdated: Date = Date()
) {
    val availableBalance: Int get() = currentBalance
    val xp: Int get() = totalEarned // XP is total earned
    val level: Int get() = LevelingHelper.getLevelFromXP(xp)
    val progressToNextLevel: Pair<Int, Int> get() = LevelingHelper.getProgressToNextLevel(xp)

    companion object {
        const val CONVERSION_RATE = 0.1
        const val MIN_REDEMPTION = 10
    }
}

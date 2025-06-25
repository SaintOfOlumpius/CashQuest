package vc.prog3c.poe.core.utils

import kotlin.math.floor
import kotlin.math.pow

/**
 * Dynamic leveling system where required XP increases per level.
 * Uses exponential formula for XP required per level.
 */
object LevelManager {

    // Base XP for level 1
    private const val BASE_XP = 100

    // Exponential factor for XP increase per level
    private const val XP_MULTIPLIER = 1.5

    /**
     * Calculates current level given total XP (questCoins).
     */
    fun calculateLevel(totalXP: Int): Int {
        var level = 1
        var xpNeededForNextLevel = BASE_XP.toDouble()

        var accumulatedXP = 0.0

        while (totalXP >= accumulatedXP + xpNeededForNextLevel) {
            accumulatedXP += xpNeededForNextLevel
            xpNeededForNextLevel *= XP_MULTIPLIER
            level++
        }
        return level
    }

    /**
     * Calculates progress percentage (0.0 to 1.0) towards next level.
     */
    fun calculateProgress(totalXP: Int): Float {
        var xpNeededForNextLevel = BASE_XP.toDouble()
        var accumulatedXP = 0.0

        while (totalXP >= accumulatedXP + xpNeededForNextLevel) {
            accumulatedXP += xpNeededForNextLevel
            xpNeededForNextLevel *= XP_MULTIPLIER
        }

        val xpIntoLevel = totalXP - accumulatedXP
        return (xpIntoLevel / xpNeededForNextLevel).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Returns total XP required to reach the next level given the current level.
     */
    fun xpForNextLevel(currentLevel: Int): Int {
        return (BASE_XP * XP_MULTIPLIER.pow(currentLevel - 1)).toInt()
    }
}

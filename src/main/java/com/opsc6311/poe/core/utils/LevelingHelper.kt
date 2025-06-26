package vc.prog3c.poe.core.utils

object LevelingHelper {
    fun getLevelFromXP(xp: Int): Int {
        var level = 1
        var xpThreshold = 100
        var totalXP = 0

        while (xp >= totalXP + xpThreshold) {
            totalXP += xpThreshold
            xpThreshold = getXPThresholdForLevel(level + 1)
            level++
        }

        return level
    }

    fun getXPThresholdForLevel(level: Int): Int {
        return 100 + (level - 1) * 50 // Example: 100, 150, 200, 250...
    }

    fun getProgressToNextLevel(xp: Int): Pair<Int, Int> {
        val level = getLevelFromXP(xp)
        val currentLevelXP = (1 until level).sumOf { getXPThresholdForLevel(it) }
        val nextLevelXP = getXPThresholdForLevel(level)
        val progress = xp - currentLevelXP
        return Pair(progress, nextLevelXP)
    }
}

package com.opsc6311.poe.ui.viewmodels

import com.opsc6311.poe.data.models.Achievement
import com.opsc6311.poe.data.models.QuestCoins

sealed interface AchievementsUiState {
    object Default : AchievementsUiState
    object Loading : AchievementsUiState
    
    data class Updated(
        val achievements: List<Achievement>? = null,
        val questCoins: QuestCoins? = null
    ) : AchievementsUiState
    
    data class Failure(val message: String) : AchievementsUiState
}

package com.opsc6311.poe.data.services

import com.opsc6311.poe.core.services.AchievementEngine
import com.opsc6311.poe.data.models.Account
import com.opsc6311.poe.data.models.Category
import com.opsc6311.poe.data.models.Transaction
import com.opsc6311.poe.ui.viewmodels.AchievementViewModel

class AchievementEvaluator(
    private val userId: String,
    private val achievementViewModel: AchievementViewModel
) {
    fun run(accounts: List<Account>, transactions: List<Transaction>, categories: List<Category>) {
        val engine = AchievementEngine(
            userId = userId,
            accounts = accounts,
            transactions = transactions,
            categories = categories
        ) { id, progress ->
            achievementViewModel.updateAchievementProgress(userId, id, progress)
        }

        engine.evaluateAll()
    }

}

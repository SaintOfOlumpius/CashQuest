package vc.prog3c.poe.ui.viewmodels

import vc.prog3c.poe.data.models.Account
import vc.prog3c.poe.data.models.Budget
import vc.prog3c.poe.data.models.Category
import vc.prog3c.poe.data.models.MonthlyStats
import vc.prog3c.poe.data.models.SavingsGoal

sealed interface DashboardUiState {
    object Default : DashboardUiState // Represents the default state
    object Loading : DashboardUiState // Represents the loading state

    data class Updated(
        val categoryList: List<Category>? = null, // List of categories
        val breakdowns: Map<String, Double>? = null, // Breakdown of expenses
        val statistics: MonthlyStats? = null, // Monthly statistics
        val savingsGoals: List<SavingsGoal>? = null, // List of savings goals
        val budget: Budget? = null, // Current budget
        val accounts: List<Account>? = null // List of accounts
    ) : DashboardUiState

    data class Failure(val message: String) : DashboardUiState // Represents a failure state with an error message
}

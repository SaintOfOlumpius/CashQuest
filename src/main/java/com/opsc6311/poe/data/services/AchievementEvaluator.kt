package com.opsc6311.poe.data.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.opsc6311.poe.data.models.Achievement
import com.opsc6311.poe.data.models.Transaction
import com.opsc6311.poe.data.models.TransactionType
import com.opsc6311.poe.data.models.Budget
import com.opsc6311.poe.data.models.Account
import com.opsc6311.poe.data.models.SavingsGoal
import kotlinx.coroutines.tasks.await
import java.util.*

class AchievementEvaluator(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    suspend fun evaluateUserAchievements() {
        val userId = auth.currentUser?.uid ?: return
        
        // Get user's current achievements
        val userAchievements = getUserAchievements(userId)
        
        // Get user data for evaluation
        val transactions = getTransactions(userId)
        val budgets = getBudgets(userId)
        val accounts = getAccounts(userId)
        val savingsGoals = getSavingsGoals(userId)
        
        // Evaluate different achievement categories
        evaluateUserMilestones(userId, userAchievements, transactions, budgets, accounts, savingsGoals)
        evaluateConsistencyHabits(userId, userAchievements, transactions, budgets)
        evaluateSavingsAchievements(userId, userAchievements, savingsGoals, transactions)
        evaluateBudgetManagement(userId, userAchievements, budgets, transactions)
        evaluateFinancialInsight(userId, userAchievements, transactions, budgets, accounts)
        evaluateLearningGrowth(userId, userAchievements, transactions, budgets, savingsGoals)
    }
    
    private suspend fun evaluateUserMilestones(
        userId: String,
        userAchievements: List<Achievement>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        accounts: List<Account>,
        savingsGoals: List<SavingsGoal>
    ) {
        val achievementIds = userAchievements.map { it.id }.toSet()
        
        // First Budget Created
        if (budgets.isNotEmpty() && !achievementIds.contains("first_budget")) {
            unlockAchievement(userId, "first_budget")
        }
        
        // First Expense Logged
        if (transactions.any { it.type == TransactionType.EXPENSE } && !achievementIds.contains("first_expense")) {
            unlockAchievement(userId, "first_expense")
        }
        
        // First Income Added
        if (transactions.any { it.type == TransactionType.INCOME } && !achievementIds.contains("first_income")) {
            unlockAchievement(userId, "first_income")
        }
        
        // First Savings Goal Set
        if (savingsGoals.isNotEmpty() && !achievementIds.contains("first_savings_goal")) {
            unlockAchievement(userId, "first_savings_goal")
        }
        
        // First Account Added
        if (accounts.isNotEmpty() && !achievementIds.contains("first_account")) {
            unlockAchievement(userId, "first_account")
        }
    }
    
    private suspend fun evaluateConsistencyHabits(
        userId: String,
        userAchievements: List<Achievement>,
        transactions: List<Transaction>,
        budgets: List<Budget>
    ) {
        val achievementIds = userAchievements.map { it.id }.toSet()
        
        // Daily Tracker - Check for 7 consecutive days of expense logging
        if (!achievementIds.contains("daily_tracker")) {
            val consecutiveDays = getConsecutiveDaysWithExpenses(transactions)
            updateAchievementProgress("daily_tracker", consecutiveDays)
        }
        
        // Weekly Warrior - Check for 4 weeks of app usage
        if (!achievementIds.contains("weekly_warrior")) {
            val weeklyUsage = getWeeklyUsageCount(transactions)
            updateAchievementProgress("weekly_warrior", weeklyUsage)
        }
        
        // Budget Streak - Check for 3 consecutive months with budgets
        if (!achievementIds.contains("budget_streak")) {
            val budgetMonths = getConsecutiveBudgetMonths(budgets)
            updateAchievementProgress("budget_streak", budgetMonths)
        }
        
        // Habitual Saver - Check for 4 weeks of savings contributions
        if (!achievementIds.contains("habitual_saver")) {
            val savingsWeeks = getSavingsContributionWeeks(transactions)
            updateAchievementProgress("habitual_saver", savingsWeeks)
        }
        
        // On-Time Logger - Check for 14 consecutive days of same-day logging
        if (!achievementIds.contains("on_time_logger")) {
            val onTimeDays = getOnTimeLoggingDays(transactions)
            updateAchievementProgress("on_time_logger", onTimeDays)
        }
    }
    
    private suspend fun evaluateSavingsAchievements(
        userId: String,
        userAchievements: List<Achievement>,
        savingsGoals: List<SavingsGoal>,
        transactions: List<Transaction>
    ) {
        val achievementIds = userAchievements.map { it.id }.toSet()
        
        // First Goal Reached
        if (savingsGoals.any { it.savedAmount >= it.targetAmount } && !achievementIds.contains("first_goal_reached")) {
            unlockAchievement(userId, "first_goal_reached")
        }
        
        // Savings Streak - Check for 3 months of consistent saving
        if (!achievementIds.contains("savings_streak")) {
            val savingsMonths = getSavingsStreakMonths(transactions)
            updateAchievementProgress("savings_streak", savingsMonths)
        }
        
        // Emergency Fund Builder - Check for R5,000 in emergency fund
        val emergencyFund = savingsGoals.find { it.name.contains("emergency", ignoreCase = true) }
        if (emergencyFund?.savedAmount ?: 0.0 >= 5000 && !achievementIds.contains("emergency_fund")) {
            unlockAchievement(userId, "emergency_fund")
        }
        
        // Big Saver - Check for R10,000 total saved
        val totalSaved = savingsGoals.sumOf { it.savedAmount }
        if (totalSaved >= 10000 && !achievementIds.contains("big_saver")) {
            unlockAchievement(userId, "big_saver")
        }
        
        // Savings Master - Check for 5 different savings goals reached
        if (!achievementIds.contains("savings_master")) {
            val reachedGoals = savingsGoals.count { it.savedAmount >= it.targetAmount }
            updateAchievementProgress("savings_master", reachedGoals)
        }
    }
    
    private suspend fun evaluateBudgetManagement(
        userId: String,
        userAchievements: List<Achievement>,
        budgets: List<Budget>,
        transactions: List<Transaction>
    ) {
        val achievementIds = userAchievements.map { it.id }.toSet()
        
        // Under Budget - Check if stayed under budget for current month
        val currentMonth = getCurrentMonth()
        val currentBudget = budgets.find { it.month == currentMonth }
        if (currentBudget != null) {
            val totalSpent = transactions
                .filter { it.type == TransactionType.EXPENSE && it.date.toDate().month == currentBudget.month }
                .sumOf { it.amount }
            
            if (totalSpent <= currentBudget.target && !achievementIds.contains("under_budget")) {
                unlockAchievement(userId, "under_budget")
            }
        }
        
        // Category Master - Check for 5 custom categories
        if (!achievementIds.contains("category_master")) {
            val customCategories = getCustomCategoriesCount(transactions)
            updateAchievementProgress("category_master", customCategories)
        }
        
        // Spending Analyst - Check for 6 months of spending analysis
        if (!achievementIds.contains("spending_analyst")) {
            val analysisMonths = getSpendingAnalysisMonths(transactions)
            updateAchievementProgress("spending_analyst", analysisMonths)
        }
    }
    
    private suspend fun evaluateFinancialInsight(
        userId: String,
        userAchievements: List<Achievement>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        accounts: List<Account>
    ) {
        val achievementIds = userAchievements.map { it.id }.toSet()
        
        // Top Spender Revealed - Check if user has identified spending patterns
        if (transactions.size >= 10 && !achievementIds.contains("top_spender")) {
            unlockAchievement(userId, "top_spender")
        }
        
        // Trends Analyst - Check for 3 months of trend viewing
        if (!achievementIds.contains("trends_analyst")) {
            val trendMonths = getTrendAnalysisMonths(transactions)
            updateAchievementProgress("trends_analyst", trendMonths)
        }
        
        // Net Worth Tracker - Check for 6 months of net worth tracking
        if (!achievementIds.contains("net_worth_tracker")) {
            val trackingMonths = getNetWorthTrackingMonths(accounts)
            updateAchievementProgress("net_worth_tracker", trackingMonths)
        }
        
        // Cash Flow Master - Check for 3 months of positive cash flow
        if (!achievementIds.contains("cash_flow_master")) {
            val positiveFlowMonths = getPositiveCashFlowMonths(transactions)
            updateAchievementProgress("cash_flow_master", positiveFlowMonths)
        }
        
        // Financial Goals Setter - Check for 5 different financial goals
        if (!achievementIds.contains("financial_goals_setter")) {
            val goalCount = getFinancialGoalsCount(budgets, transactions)
            updateAchievementProgress("financial_goals_setter", goalCount)
        }
    }
    
    private suspend fun evaluateLearningGrowth(
        userId: String,
        userAchievements: List<Achievement>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        savingsGoals: List<SavingsGoal>
    ) {
        val achievementIds = userAchievements.map { it.id }.toSet()
        
        // Goal Setter - Check for 3 or more financial goals
        if (!achievementIds.contains("goal_setter")) {
            val totalGoals = budgets.size + savingsGoals.size
            updateAchievementProgress("goal_setter", totalGoals)
        }
        
        // Financially Fit - Check for 3 months of meeting budget goals
        if (!achievementIds.contains("financially_fit")) {
            val fitMonths = getFinanciallyFitMonths(budgets, transactions)
            updateAchievementProgress("financially_fit", fitMonths)
        }
        
        // App Explorer - Check if user has used all major features
        if (!achievementIds.contains("app_explorer")) {
            val featuresUsed = getFeaturesUsedCount(transactions, budgets, savingsGoals)
            if (featuresUsed >= 5) {
                unlockAchievement(userId, "app_explorer")
            }
        }
    }
    
    // Helper methods for data analysis
    private fun getConsecutiveDaysWithExpenses(transactions: List<Transaction>): Int {
        val expenseDates = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .map { it.date.toDate() }
            .distinct()
            .sorted()
        
        var maxConsecutive = 0
        var currentConsecutive = 0
        var previousDate: Date? = null
        
        for (date in expenseDates) {
            if (previousDate == null || isConsecutiveDay(previousDate, date)) {
                currentConsecutive++
            } else {
                currentConsecutive = 1
            }
            maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            previousDate = date
        }
        
        return maxConsecutive
    }
    
    private fun getWeeklyUsageCount(transactions: List<Transaction>): Int {
        val calendar = Calendar.getInstance()
        val weeks = mutableSetOf<String>()
        
        transactions.forEach { transaction ->
            calendar.time = transaction.date.toDate()
            val week = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
            weeks.add(week)
        }
        
        return weeks.size
    }
    
    private fun getConsecutiveBudgetMonths(budgets: List<Budget>): Int {
        val budgetMonths = budgets.map { it.month }.sorted()
        var maxConsecutive = 0
        var currentConsecutive = 0
        var previousMonth: Int? = null
        
        for (month in budgetMonths) {
            if (previousMonth == null || isConsecutiveMonth(previousMonth, month)) {
                currentConsecutive++
            } else {
                currentConsecutive = 1
            }
            maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            previousMonth = month
        }
        
        return maxConsecutive
    }
    
    private fun getSavingsContributionWeeks(transactions: List<Transaction>): Int {
        val calendar = Calendar.getInstance()
        val savingsWeeks = mutableSetOf<String>()
        
        transactions
            .filter { it.category == "Savings" || it.description.contains("savings", ignoreCase = true) }
            .forEach { transaction ->
                calendar.time = transaction.date.toDate()
                val week = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
                savingsWeeks.add(week)
            }
        
        return savingsWeeks.size
    }
    
    private fun getOnTimeLoggingDays(transactions: List<Transaction>): Int {
        val calendar = Calendar.getInstance()
        var consecutiveDays = 0
        var maxConsecutive = 0
        var previousDate: Date? = null
        
        transactions.sortedBy { it.date.toDate() }.forEach { transaction ->
            calendar.time = transaction.date.toDate()
            val transactionDay = calendar.get(Calendar.DAY_OF_YEAR)
            val transactionYear = calendar.get(Calendar.YEAR)
            
            if (previousDate != null) {
                calendar.time = previousDate
                val previousDay = calendar.get(Calendar.DAY_OF_YEAR)
                val previousYear = calendar.get(Calendar.YEAR)
                
                if (transactionYear == previousYear && transactionDay == previousDay + 1) {
                    consecutiveDays++
                } else {
                    consecutiveDays = 1
                }
            } else {
                consecutiveDays = 1
            }
            
            maxConsecutive = maxOf(maxConsecutive, consecutiveDays)
            previousDate = transaction.date.toDate()
        }
        
        return maxConsecutive
    }
    
    private fun getSavingsStreakMonths(transactions: List<Transaction>): Int {
        val calendar = Calendar.getInstance()
        val savingsMonths = mutableSetOf<String>()
        
        transactions
            .filter { it.category == "Savings" || it.description.contains("savings", ignoreCase = true) }
            .forEach { transaction ->
                calendar.time = transaction.date.toDate()
                val month = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
                savingsMonths.add(month)
            }
        
        return savingsMonths.size
    }
    
    private fun getCustomCategoriesCount(transactions: List<Transaction>): Int {
        return transactions.map { it.category }.distinct().count { 
            !it.equals("Food", ignoreCase = true) && 
            !it.equals("Transport", ignoreCase = true) && 
            !it.equals("Entertainment", ignoreCase = true) && 
            !it.equals("Shopping", ignoreCase = true) && 
            !it.equals("Utilities", ignoreCase = true) && 
            !it.equals("Health", ignoreCase = true) && 
            !it.equals("Income", ignoreCase = true) && 
            !it.equals("Savings", ignoreCase = true)
        }
    }
    
    private fun getSpendingAnalysisMonths(transactions: List<Transaction>): Int {
        val calendar = Calendar.getInstance()
        val analysisMonths = mutableSetOf<String>()
        
        transactions
            .filter { it.type == TransactionType.EXPENSE }
            .forEach { transaction ->
                calendar.time = transaction.date.toDate()
                val month = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
                analysisMonths.add(month)
            }
        
        return analysisMonths.size
    }
    
    private fun getTrendAnalysisMonths(transactions: List<Transaction>): Int {
        // Simplified: count months with significant transaction activity
        val calendar = Calendar.getInstance()
        val activeMonths = mutableSetOf<String>()
        
        transactions.forEach { transaction ->
            calendar.time = transaction.date.toDate()
            val month = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
            activeMonths.add(month)
        }
        
        return activeMonths.size
    }
    
    private fun getNetWorthTrackingMonths(accounts: List<Account>): Int {
        // Simplified: count months since first account creation
        if (accounts.isEmpty()) return 0
        
        // Since Account doesn't have createdAt, we'll use a default value
        return 6 // Default to 6 months
    }
    
    private fun getPositiveCashFlowMonths(transactions: List<Transaction>): Int {
        val calendar = Calendar.getInstance()
        val monthlyCashFlow = mutableMapOf<String, Double>()
        
        transactions.forEach { transaction ->
            calendar.time = transaction.date.toDate()
            val month = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
            
            val currentFlow = monthlyCashFlow[month] ?: 0.0
            val amount = if (transaction.type == TransactionType.INCOME) transaction.amount else -transaction.amount
            monthlyCashFlow[month] = currentFlow + amount
        }
        
        return monthlyCashFlow.count { it.value > 0 }
    }
    
    private fun getFinancialGoalsCount(budgets: List<Budget>, transactions: List<Transaction>): Int {
        val budgetGoals = budgets.size
        val savingsGoals = transactions.count { 
            it.category == "Savings" || it.description.contains("goal", ignoreCase = true) 
        }
        return budgetGoals + savingsGoals
    }
    
    private fun getFinanciallyFitMonths(budgets: List<Budget>, transactions: List<Transaction>): Int {
        var fitMonths = 0
        
        budgets.forEach { budget ->
            val monthTransactions = transactions.filter { 
                it.date.toDate().month == budget.month 
            }
            
            val totalSpent = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            if (totalSpent <= budget.target) {
                fitMonths++
            }
        }
        
        return fitMonths
    }
    
    private fun getFeaturesUsedCount(
        transactions: List<Transaction>, 
        budgets: List<Budget>, 
        savingsGoals: List<SavingsGoal>
    ): Int {
        var count = 0
        if (transactions.isNotEmpty()) count++
        if (budgets.isNotEmpty()) count++
        if (savingsGoals.isNotEmpty()) count++
        if (transactions.any { it.type == TransactionType.INCOME }) count++
        if (transactions.any { it.type == TransactionType.EXPENSE }) count++
        return count
    }
    
    // Utility methods
    private fun isConsecutiveDay(date1: Date, date2: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date1
        val day1 = calendar.get(Calendar.DAY_OF_YEAR)
        val year1 = calendar.get(Calendar.YEAR)
        
        calendar.time = date2
        val day2 = calendar.get(Calendar.DAY_OF_YEAR)
        val year2 = calendar.get(Calendar.YEAR)
        
        return (year2 == year1 && day2 == day1 + 1) || 
               (year2 == year1 + 1 && day2 == 1 && day1 == calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
    }
    
    private fun isConsecutiveMonth(month1: Int, month2: Int): Boolean {
        return month2 == month1 + 1
    }
    
    private fun getCurrentMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH) + 1
    }
    
    // Data retrieval methods
    private suspend fun getUserAchievements(userId: String): List<Achievement> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("achievements")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Achievement::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun getTransactions(userId: String): List<Transaction> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun getBudgets(userId: String): List<Budget> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("budgets")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Budget::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun getAccounts(userId: String): List<Account> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("accounts")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Account::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun getSavingsGoals(userId: String): List<SavingsGoal> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("savings_goals")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(SavingsGoal::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun unlockAchievement(userId: String, achievementId: String) {
        try {
            // Get achievement details
            val achievementDoc = db.collection("achievements").document(achievementId).get().await()
            val achievement = achievementDoc.toObject(Achievement::class.java)
            
            if (achievement != null) {
                // Add to user's achievements
                db.collection("users")
                    .document(userId)
                    .collection("achievements")
                    .document(achievementId)
                    .set(achievement)
                    .await()
                
                // Add quest coins to user
                addQuestCoins(userId, achievement.questCoinsReward)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private suspend fun updateAchievementProgress(achievementId: String, progress: Int) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            // Get achievement details
            val achievementDoc = db.collection("achievements").document(achievementId).get().await()
            val achievement = achievementDoc.toObject(Achievement::class.java)
            
            if (achievement != null && achievement.requiredProgress != null) {
                val userAchievementDoc = db.collection("users")
                    .document(userId)
                    .collection("achievements")
                    .document(achievementId)
                
                val userAchievement = userAchievementDoc.get().await().toObject(Achievement::class.java)
                
                if (userAchievement == null) {
                    // Create new user achievement with progress
                    val newUserAchievement = achievement.copy(
                        progress = progress,
                        isCompleted = progress >= achievement.requiredProgress
                    )
                    
                    userAchievementDoc.set(newUserAchievement).await()
                    
                    // Check if completed
                    if (newUserAchievement.isCompleted) {
                        addQuestCoins(userId, achievement.questCoinsReward)
                    }
                } else if (!userAchievement.isCompleted) {
                    // Update existing progress
                    val updatedProgress = progress.coerceAtMost(achievement.requiredProgress)
                    val isCompleted = updatedProgress >= achievement.requiredProgress
                    
                    userAchievementDoc.update(
                        mapOf(
                            "progress" to updatedProgress,
                            "isCompleted" to isCompleted
                        )
                    ).await()
                    
                    // Check if newly completed
                    if (isCompleted && !userAchievement.isCompleted) {
                        addQuestCoins(userId, achievement.questCoinsReward)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't throw to avoid breaking the evaluation process
            println("Failed to update achievement progress: ${e.message}")
        }
    }
    
    private suspend fun addQuestCoins(userId: String, amount: Int) {
        try {
            val userDoc = db.collection("users").document(userId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val currentCoins = snapshot.getLong("questCoins") ?: 0
                transaction.update(userDoc, "questCoins", currentCoins + amount)
            }.await()
        } catch (e: Exception) {
            // Handle error
        }
    }
}

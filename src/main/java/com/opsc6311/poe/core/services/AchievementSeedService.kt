package com.opsc6311.poe.core.services

import com.google.firebase.firestore.FirebaseFirestore
import com.opsc6311.poe.data.models.Achievement
import com.opsc6311.poe.data.models.AchievementCategory
import kotlinx.coroutines.tasks.await

class AchievementSeedService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun seedAchievements() {
        val achievements = listOf(
            // User Milestones (50-100 coins each)
            Achievement(
                id = "first_budget",
                title = "First Budget Created",
                description = "Successfully created your first monthly budget",
                category = AchievementCategory.USER_MILESTONES,
                questCoinsReward = 75
            ),
            Achievement(
                id = "first_expense",
                title = "First Expense Logged",
                description = "Tracked your first expense transaction",
                category = AchievementCategory.USER_MILESTONES,
                questCoinsReward = 50
            ),
            Achievement(
                id = "first_income",
                title = "First Income Added",
                description = "Logged your first income source",
                category = AchievementCategory.USER_MILESTONES,
                questCoinsReward = 50
            ),
            Achievement(
                id = "first_savings_goal",
                title = "First Savings Goal Set",
                description = "Created your first savings goal",
                category = AchievementCategory.USER_MILESTONES,
                questCoinsReward = 100
            ),
            Achievement(
                id = "first_account",
                title = "First Account Added",
                description = "Added your first financial account",
                category = AchievementCategory.USER_MILESTONES,
                questCoinsReward = 75
            ),
            Achievement(
                id = "profile_complete",
                title = "Profile Complete",
                description = "Completed your user profile setup",
                category = AchievementCategory.USER_MILESTONES,
                questCoinsReward = 100
            ),

            // Consistency & Habits (75-200 coins each)
            Achievement(
                id = "daily_tracker",
                title = "Daily Tracker",
                description = "Logged expenses for 7 days in a row",
                category = AchievementCategory.CONSISTENCY_HABITS,
                questCoinsReward = 150,
                requiredProgress = 7
            ),
            Achievement(
                id = "weekly_warrior",
                title = "Weekly Warrior",
                description = "Used the app every week for a month",
                category = AchievementCategory.CONSISTENCY_HABITS,
                questCoinsReward = 200,
                requiredProgress = 4
            ),
            Achievement(
                id = "budget_streak",
                title = "Budget Streak",
                description = "Maintained a budget for 3 consecutive months",
                category = AchievementCategory.CONSISTENCY_HABITS,
                questCoinsReward = 300,
                requiredProgress = 3
            ),
            Achievement(
                id = "habitual_saver",
                title = "Habitual Saver",
                description = "Added to savings every week for a month",
                category = AchievementCategory.CONSISTENCY_HABITS,
                questCoinsReward = 250,
                requiredProgress = 4
            ),
            Achievement(
                id = "on_time_logger",
                title = "On-Time Logger",
                description = "Logged expenses on the same day for 14 days in a row",
                category = AchievementCategory.CONSISTENCY_HABITS,
                questCoinsReward = 200,
                requiredProgress = 14
            ),
            Achievement(
                id = "monthly_reviewer",
                title = "Monthly Reviewer",
                description = "Reviewed your monthly spending for 3 months",
                category = AchievementCategory.CONSISTENCY_HABITS,
                questCoinsReward = 175,
                requiredProgress = 3
            ),

            // Savings Achievements (100-500 coins each)
            Achievement(
                id = "first_goal_reached",
                title = "First Goal Reached",
                description = "Reached your first savings goal",
                category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
                questCoinsReward = 200
            ),
            Achievement(
                id = "savings_streak",
                title = "Savings Streak",
                description = "Saved money consistently for 3 months",
                category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
                questCoinsReward = 300,
                requiredProgress = 3
            ),
            Achievement(
                id = "emergency_fund",
                title = "Emergency Fund Builder",
                description = "Built an emergency fund of R5,000",
                category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
                questCoinsReward = 400
            ),
            Achievement(
                id = "big_saver",
                title = "Big Saver",
                description = "Saved over R10,000 total",
                category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
                questCoinsReward = 500
            ),
            Achievement(
                id = "debt_destroyer",
                title = "Debt Destroyer",
                description = "Paid off a tracked debt completely",
                category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
                questCoinsReward = 350
            ),
            Achievement(
                id = "savings_master",
                title = "Savings Master",
                description = "Reached 5 different savings goals",
                category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
                questCoinsReward = 450,
                requiredProgress = 5
            ),
            Achievement(
                id = "investment_starter",
                title = "Investment Starter",
                description = "Started your first investment account",
                category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
                questCoinsReward = 250
            ),

            // Budget Management (75-300 coins each)
            Achievement(
                id = "under_budget",
                title = "Under Budget",
                description = "Stayed under budget for the month",
                category = AchievementCategory.BUDGET_MANAGEMENT,
                questCoinsReward = 150
            ),
            Achievement(
                id = "expense_cutter",
                title = "Expense Cutter",
                description = "Reduced a spending category by 20% month-over-month",
                category = AchievementCategory.BUDGET_MANAGEMENT,
                questCoinsReward = 200
            ),
            Achievement(
                id = "zero_based_budgeter",
                title = "Zero-Based Budgeter",
                description = "Assigned every dollar a job for 3 months",
                category = AchievementCategory.BUDGET_MANAGEMENT,
                questCoinsReward = 250,
                requiredProgress = 3
            ),
            Achievement(
                id = "category_master",
                title = "Category Master",
                description = "Created 5 custom spending categories",
                category = AchievementCategory.BUDGET_MANAGEMENT,
                questCoinsReward = 100,
                requiredProgress = 5
            ),
            Achievement(
                id = "flexible_financier",
                title = "Flexible Financier",
                description = "Adjusted budget mid-month to reflect changes",
                category = AchievementCategory.BUDGET_MANAGEMENT,
                questCoinsReward = 125
            ),
            Achievement(
                id = "budget_optimizer",
                title = "Budget Optimizer",
                description = "Optimized your budget allocation for maximum efficiency",
                category = AchievementCategory.BUDGET_MANAGEMENT,
                questCoinsReward = 175
            ),
            Achievement(
                id = "spending_analyst",
                title = "Spending Analyst",
                description = "Analyzed your spending patterns across 6 months",
                category = AchievementCategory.BUDGET_MANAGEMENT,
                questCoinsReward = 300,
                requiredProgress = 6
            ),

            // Financial Insight (100-400 coins each)
            Achievement(
                id = "top_spender",
                title = "Top Spender Revealed",
                description = "Identified your biggest spending category",
                category = AchievementCategory.FINANCIAL_INSIGHT,
                questCoinsReward = 100
            ),
            Achievement(
                id = "trends_analyst",
                title = "Trends Analyst",
                description = "Viewed your spending trends over 3 months",
                category = AchievementCategory.FINANCIAL_INSIGHT,
                questCoinsReward = 150
            ),
            Achievement(
                id = "report_reader",
                title = "Report Reader",
                description = "Generated and reviewed a monthly report",
                category = AchievementCategory.FINANCIAL_INSIGHT,
                questCoinsReward = 125
            ),
            Achievement(
                id = "financial_forecaster",
                title = "Financial Forecaster",
                description = "Used projected income and expenses for planning",
                category = AchievementCategory.FINANCIAL_INSIGHT,
                questCoinsReward = 200
            ),
            Achievement(
                id = "net_worth_tracker",
                title = "Net Worth Tracker",
                description = "Tracked your net worth for 6 months",
                category = AchievementCategory.FINANCIAL_INSIGHT,
                questCoinsReward = 300,
                requiredProgress = 6
            ),
            Achievement(
                id = "cash_flow_master",
                title = "Cash Flow Master",
                description = "Maintained positive cash flow for 3 months",
                category = AchievementCategory.FINANCIAL_INSIGHT,
                questCoinsReward = 250,
                requiredProgress = 3
            ),
            Achievement(
                id = "financial_goals_setter",
                title = "Financial Goals Setter",
                description = "Set and tracked 5 different financial goals",
                category = AchievementCategory.FINANCIAL_INSIGHT,
                questCoinsReward = 400,
                requiredProgress = 5
            ),

            // Learning & Growth (75-350 coins each)
            Achievement(
                id = "budgeting_beginner",
                title = "Budgeting Beginner",
                description = "Completed the budgeting tutorial",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 100
            ),
            Achievement(
                id = "finance_buff",
                title = "Finance Buff",
                description = "Completed all in-app learning modules",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 350
            ),
            Achievement(
                id = "goal_setter",
                title = "Goal Setter",
                description = "Created 3 or more financial goals",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 150,
                requiredProgress = 3
            ),
            Achievement(
                id = "smart_spender",
                title = "Smart Spender",
                description = "Reallocated budget based on priorities",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 125
            ),
            Achievement(
                id = "financially_fit",
                title = "Financially Fit",
                description = "Met all monthly budget goals for 3 months",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 300,
                requiredProgress = 3
            ),
            Achievement(
                id = "money_mindset",
                title = "Money Mindset",
                description = "Developed a healthy relationship with money",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 200
            ),
            Achievement(
                id = "financial_educator",
                title = "Financial Educator",
                description = "Shared financial tips with friends or family",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 175
            ),
            Achievement(
                id = "app_explorer",
                title = "App Explorer",
                description = "Used all major features of the app",
                category = AchievementCategory.LEARNING_GROWTH,
                questCoinsReward = 225
            )
        )

        val batch = db.batch()

        achievements.forEach { achievement ->
            val achievementData = mapOf(
                "id" to achievement.id,
                "title" to achievement.title,
                "description" to achievement.description,
                "category" to achievement.category.name,
                "questCoinsReward" to achievement.questCoinsReward,
                "requiredProgress" to achievement.requiredProgress,
                "isPublic" to true,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            val ref = db.collection("achievements").document(achievement.id)
            batch.set(ref, achievementData)
        }

        batch.commit().await()
    }
} 
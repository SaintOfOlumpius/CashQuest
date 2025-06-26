package com.opsc6311.poe.data.models

import com.google.firebase.Timestamp

data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: AchievementCategory = AchievementCategory.USER_MILESTONES,
    val questCoinsReward: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    val progress: Int = 0,
    val requiredProgress: Int = 1,
    val isPublic: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
)

enum class AchievementCategory {
    USER_MILESTONES,
    CONSISTENCY_HABITS,
    SAVINGS_ACHIEVEMENTS,
    BUDGET_MANAGEMENT,
    FINANCIAL_INSIGHT,
    LEARNING_GROWTH
}

object AchievementDefinitions {
    val achievements = listOf(
        // User Milestones
        Achievement(
            id = "first_budget",
            title = "First Budget Created",
            description = "Successfully created your first monthly budget",
            category = AchievementCategory.USER_MILESTONES,
            questCoinsReward = 50
        ),
        Achievement(
            id = "first_expense",
            title = "First Expense Logged",
            description = "Tracked your first expense",
            category = AchievementCategory.USER_MILESTONES,
            questCoinsReward = 25
        ),
        Achievement(
            id = "first_income",
            title = "First Income Added",
            description = "Logged your first income source",
            category = AchievementCategory.USER_MILESTONES,
            questCoinsReward = 25
        ),
        Achievement(
            id = "first_savings_goal",
            title = "First Savings Goal Set",
            description = "Created your first savings goal",
            category = AchievementCategory.USER_MILESTONES,
            questCoinsReward = 50
        ),
        Achievement(
            id = "first_account",
            title = "First Account Added",
            description = "Added your first financial account",
            category = AchievementCategory.USER_MILESTONES,
            questCoinsReward = 25
        ),

        // Consistency & Habits
        Achievement(
            id = "daily_tracker",
            title = "Daily Tracker",
            description = "Logged expenses for 7 days in a row",
            category = AchievementCategory.CONSISTENCY_HABITS,
            questCoinsReward = 75,
            requiredProgress = 7
        ),
        Achievement(
            id = "weekly_warrior",
            title = "Weekly Warrior",
            description = "Used the app every week for a month",
            category = AchievementCategory.CONSISTENCY_HABITS,
            questCoinsReward = 100,
            requiredProgress = 4
        ),
        Achievement(
            id = "budget_streak",
            title = "Budget Streak",
            description = "Maintained a budget for 3 consecutive months",
            category = AchievementCategory.CONSISTENCY_HABITS,
            questCoinsReward = 150,
            requiredProgress = 3
        ),
        Achievement(
            id = "habitual_saver",
            title = "Habitual Saver",
            description = "Added to savings every week for a month",
            category = AchievementCategory.CONSISTENCY_HABITS,
            questCoinsReward = 100,
            requiredProgress = 4
        ),
        Achievement(
            id = "on_time_logger",
            title = "On-Time Logger",
            description = "Logged expenses on the same day for 14 days in a row",
            category = AchievementCategory.CONSISTENCY_HABITS,
            questCoinsReward = 100,
            requiredProgress = 14
        ),

        // Savings Achievements
        Achievement(
            id = "first_goal_reached",
            title = "First Goal Reached",
            description = "Reached your first savings goal",
            category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
            questCoinsReward = 100
        ),
        Achievement(
            id = "savings_streak",
            title = "Savings Streak",
            description = "Saved money consistently for 3 months",
            category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
            questCoinsReward = 150,
            requiredProgress = 3
        ),
        Achievement(
            id = "emergency_fund",
            title = "Emergency Fund Builder",
            description = "Reached an emergency fund target of R5,000",
            category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
            questCoinsReward = 200
        ),
        Achievement(
            id = "big_saver",
            title = "Big Saver",
            description = "Saved over R10,000 total across all goals",
            category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
            questCoinsReward = 250
        ),
        Achievement(
            id = "savings_master",
            title = "Savings Master",
            description = "Reached 5 different savings goals",
            category = AchievementCategory.SAVINGS_ACHIEVEMENTS,
            questCoinsReward = 300,
            requiredProgress = 5
        ),

        // Budget Management
        Achievement(
            id = "under_budget",
            title = "Under Budget",
            description = "Stayed under budget for the month",
            category = AchievementCategory.BUDGET_MANAGEMENT,
            questCoinsReward = 100
        ),
        Achievement(
            id = "category_master",
            title = "Category Master",
            description = "Used 5 different spending categories",
            category = AchievementCategory.BUDGET_MANAGEMENT,
            questCoinsReward = 50,
            requiredProgress = 5
        ),
        Achievement(
            id = "spending_analyst",
            title = "Spending Analyst",
            description = "Tracked spending for 6 months",
            category = AchievementCategory.BUDGET_MANAGEMENT,
            questCoinsReward = 150,
            requiredProgress = 6
        ),

        // Financial Insight
        Achievement(
            id = "top_spender",
            title = "Top Spender Revealed",
            description = "Identified your biggest spending category",
            category = AchievementCategory.FINANCIAL_INSIGHT,
            questCoinsReward = 50
        ),
        Achievement(
            id = "trends_analyst",
            title = "Trends Analyst",
            description = "Viewed your spending trends over 3 months",
            category = AchievementCategory.FINANCIAL_INSIGHT,
            questCoinsReward = 100,
            requiredProgress = 3
        ),
        Achievement(
            id = "net_worth_tracker",
            title = "Net Worth Tracker",
            description = "Tracked your net worth for 6 months",
            category = AchievementCategory.FINANCIAL_INSIGHT,
            questCoinsReward = 75,
            requiredProgress = 6
        ),
        Achievement(
            id = "cash_flow_master",
            title = "Cash Flow Master",
            description = "Maintained positive cash flow for 3 months",
            category = AchievementCategory.FINANCIAL_INSIGHT,
            questCoinsReward = 200,
            requiredProgress = 3
        ),
        Achievement(
            id = "financial_goals_setter",
            title = "Financial Goals Setter",
            description = "Set 5 different financial goals",
            category = AchievementCategory.FINANCIAL_INSIGHT,
            questCoinsReward = 100,
            requiredProgress = 5
        ),

        // Learning & Growth
        Achievement(
            id = "goal_setter",
            title = "Goal Setter",
            description = "Created 3 or more financial goals",
            category = AchievementCategory.LEARNING_GROWTH,
            questCoinsReward = 100,
            requiredProgress = 3
        ),
        Achievement(
            id = "financially_fit",
            title = "Financially Fit",
            description = "Met all monthly budget goals for 3 months",
            category = AchievementCategory.LEARNING_GROWTH,
            questCoinsReward = 200,
            requiredProgress = 3
        ),
        Achievement(
            id = "app_explorer",
            title = "App Explorer",
            description = "Used all major app features",
            category = AchievementCategory.LEARNING_GROWTH,
            questCoinsReward = 150
        )
    )
}

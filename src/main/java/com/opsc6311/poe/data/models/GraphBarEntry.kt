package com.opsc6311.poe.data.models

data class GraphBarEntry(
    val categoryId: String = "",
    val categoryName: String = "",
    val totalSpent: Double = 0.0,
    val minBudget: Double = 0.0,
    val maxBudget: Double = 0.0
)

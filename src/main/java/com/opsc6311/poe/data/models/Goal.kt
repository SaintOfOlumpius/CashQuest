package com.opsc6311.poe.data.models

import java.util.Date

data class Goal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: String = "",
    val deadline: Date? = null,
    val category: String? = null,
    val description: String? = null,
    val isCompleted: Boolean = false
)

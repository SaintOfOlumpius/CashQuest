package com.opsc6311.poe.data.models

import java.util.Date

data class QuestCoinsTransaction(
    val userId: String = "",
    val amount: Int = 0,
    val type: String = "EARNED",
    val description: String = "",
    val timestamp: Date = Date()
)

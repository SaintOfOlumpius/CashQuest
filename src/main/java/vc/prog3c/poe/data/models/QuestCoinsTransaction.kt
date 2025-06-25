package vc.prog3c.poe.data.models

import java.util.Date

data class QuestCoinsTransaction(
    val userId: String = "",
    val amount: Int = 0,
    val type: TransactionType = TransactionType.EARNED,
    val description: String = "",
    val timestamp: Date = Date()
)

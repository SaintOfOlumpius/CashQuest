package vc.prog3c.poe.data.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Account(
    @PropertyName("id")
    var id: String = "",
    @PropertyName("userId")
    var userId: String = "",
    @PropertyName("name")
    var name: String = "",
    @PropertyName("type")
    var type: String = "",
    @PropertyName("balance")
    var balance: Double = 0.0,
    @PropertyName("transactionsCount")
    var transactionsCount: Int = 0
)

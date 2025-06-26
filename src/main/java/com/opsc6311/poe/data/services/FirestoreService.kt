package com.opsc6311.poe.data.services

import com.google.firebase.firestore.FirebaseFirestore
import com.opsc6311.poe.core.services.AuthService
import com.opsc6311.poe.data.models.SavingsGoal
import com.opsc6311.poe.data.repository.*

object FirestoreService {
    val savingsGoal = SavingsGoalRepository()
    val user = UserRepository()
    val category = CategoryRepository()
    val transaction = TransactionRepository()
    val account = AccountRepository()
    val budget = BudgetRepository()

    fun getGoal(goalId: String, callback: (SavingsGoal?) -> Unit) {
        val userId = AuthService().getCurrentUser()?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection("users").document(userId)
            .collection("savings_goals")
            .document(goalId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val goal = document.toObject(SavingsGoal::class.java)
                    callback(goal)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun updateGoal(goalId: String, updates: Map<String, Any>, callback: (Boolean) -> Unit) {
        val userId = AuthService().getCurrentUser()?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection("users").document(userId)
            .collection("savings_goals")
            .document(goalId)
            .update(updates)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}

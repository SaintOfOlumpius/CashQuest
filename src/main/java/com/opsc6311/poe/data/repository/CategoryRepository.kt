package com.opsc6311.poe.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.opsc6311.poe.data.models.Category
import kotlinx.coroutines.tasks.await

class CategoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun addCategory(category: Category, onComplete: (Boolean) -> Unit) {
        if (userId == null) {
            onComplete(false)
            return
        }

        db.collection("users")
            .document(userId)
            .collection("categories")
            .add(category)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getAllCategories(onComplete: (List<Category>?) -> Unit) {
        if (userId == null) {
            onComplete(null)
            return
        }

        db.collection("users")
            .document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                val categories = result.mapNotNull { it.toObject(Category::class.java) }
                onComplete(categories)
            }
            .addOnFailureListener { onComplete(null) }
    }
    
    suspend fun getAllCategories(): List<Category> {
        if (userId == null) {
            return emptyList()
        }
        
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.toObject(Category::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

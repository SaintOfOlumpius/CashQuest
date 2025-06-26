package com.opsc6311.poe.data.services

import com.google.firebase.firestore.FirebaseFirestore
import com.opsc6311.poe.data.models.Achievement
import com.opsc6311.poe.data.models.AchievementDefinitions
import kotlinx.coroutines.tasks.await

class AchievementSeedService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    suspend fun seedAchievements() {
        try {
            val achievementsCollection = db.collection("achievements")
            
            // Get existing achievements
            val existingSnapshot = achievementsCollection.get().await()
            val existingIds = existingSnapshot.documents.map { it.id }.toSet()
            
            // Add missing achievements
            AchievementDefinitions.achievements.forEach { achievement ->
                if (!existingIds.contains(achievement.id)) {
                    achievementsCollection.document(achievement.id).set(achievement).await()
                    println("Seeded achievement: ${achievement.title}")
                }
            }
            
            println("Achievement seeding completed. Total achievements: ${AchievementDefinitions.achievements.size}")
        } catch (e: Exception) {
            println("Error seeding achievements: ${e.message}")
        }
    }
    
    suspend fun ensureUserAchievements(userId: String) {
        try {
            val userAchievementsCollection = db.collection("users")
                .document(userId)
                .collection("achievements")
            
            // Get existing user achievements
            val existingSnapshot = userAchievementsCollection.get().await()
            val existingIds = existingSnapshot.documents.map { it.id }.toSet()
            
            // Add missing achievements for user (with default progress)
            AchievementDefinitions.achievements.forEach { achievement ->
                if (!existingIds.contains(achievement.id)) {
                    val userAchievement = achievement.copy(
                        progress = 0,
                        isCompleted = false,
                        completedAt = null
                    )
                    userAchievementsCollection.document(achievement.id).set(userAchievement).await()
                }
            }
            
            println("User achievements ensured for user: $userId")
        } catch (e: Exception) {
            println("Error ensuring user achievements: ${e.message}")
        }
    }
    
    suspend fun resetUserAchievements(userId: String) {
        try {
            val userAchievementsCollection = db.collection("users")
                .document(userId)
                .collection("achievements")
            
            // Delete all existing user achievements
            val existingSnapshot = userAchievementsCollection.get().await()
            existingSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            
            // Re-add all achievements with default state
            AchievementDefinitions.achievements.forEach { achievement ->
                val userAchievement = achievement.copy(
                    progress = 0,
                    isCompleted = false,
                    completedAt = null
                )
                userAchievementsCollection.document(achievement.id).set(userAchievement).await()
            }
            
            println("User achievements reset for user: $userId")
        } catch (e: Exception) {
            println("Error resetting user achievements: ${e.message}")
        }
    }
} 
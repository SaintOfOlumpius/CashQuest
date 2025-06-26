package com.opsc6311.poe.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.opsc6311.poe.core.services.AuthService
import com.opsc6311.poe.core.utils.Event
import com.opsc6311.poe.data.models.*
import java.util.Date

class AchievementViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _achievements = MutableLiveData<List<Achievement>>()
    val achievements: LiveData<List<Achievement>> = _achievements

    private val _questCoins = MutableLiveData<QuestCoins>()
    val questCoins: LiveData<QuestCoins> = _questCoins

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _newlyCompleted = MutableLiveData<Event<Achievement>>()
    val newlyCompleted: LiveData<Event<Achievement>> = _newlyCompleted

    // --- New leveling system LiveData ---
    private val _userPoints = MutableLiveData(0)
    val userPoints: LiveData<Int> get() = _userPoints

    private val _userLevel = MutableLiveData(0)
    val userLevel: LiveData<Int> get() = _userLevel

    private val _levelProgress = MutableLiveData(0) // percent 0-100
    val levelProgress: LiveData<Int> get() = _levelProgress

    private val _levelUpEvent = MutableLiveData<Event<Int>>()
    val levelUpEvent: LiveData<Event<Int>> get() = _levelUpEvent

    private val POINTS_PER_LEVEL = 100 // Customize as needed

    init {
        loadAchievements()
        loadQuestCoins()

        // Initialize user points & level based on quest coins
        _userPoints.value = _questCoins.value?.totalEarned ?: 0
        updateLevelProgress()
    }

    private fun updateLevelProgress() {
        val points = _userPoints.value ?: 0
        val level = points / POINTS_PER_LEVEL
        val progress = points % POINTS_PER_LEVEL
        val progressPercent = (progress * 100) / POINTS_PER_LEVEL

        val oldLevel = _userLevel.value ?: 0
        _userLevel.value = level
        _levelProgress.value = progressPercent

        if (level > oldLevel) {
            _levelUpEvent.value = Event(level)
        }
    }

    // Call this when points are awarded (like when achievement completed)
    private fun addPoints(points: Int) {
        val currentPoints = _userPoints.value ?: 0
        _userPoints.value = currentPoints + points
        updateLevelProgress()
    }

    // Existing Firestore loading methods unchanged but renamed for questCoins
    private fun loadAchievements() {
        viewModelScope.launch {
            try {
                val userId = authService.getCurrentUser()?.uid ?: return@launch

                val masterSnapshot = db.collection("achievements").get().await()
                val definitions = masterSnapshot.documents.mapNotNull { doc ->
                    Achievement(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = AchievementCategory.valueOf(doc.getString("category") ?: "USER_MILESTONES"),
                        questCoinsReward = doc.getLong("questCoinsReward")?.let { if (it <= Int.MAX_VALUE) it.toInt() else 0 } ?: 0,
                        requiredProgress = doc.getLong("requiredProgress")?.let { if (it <= Int.MAX_VALUE) it.toInt() else 1 } ?: 1
                    )
                }

                val userSnapshot = db.collection("users")
                    .document(userId)
                    .collection("achievements")
                    .get().await()

                val userData = userSnapshot.documents.associateBy { it.id }

                val merged = definitions.map { definition ->
                    val progressDoc = userData[definition.id]
                    definition.copy(
                        progress = progressDoc?.getLong("progress")?.let { if (it <= Int.MAX_VALUE) it.toInt() else 0 } ?: 0,
                        isCompleted = progressDoc?.getBoolean("isCompleted") ?: false,
                        completedAt = progressDoc?.getTimestamp("completedAt")
                    )
                }

                _achievements.value = merged

            } catch (e: Exception) {
                _error.value = "Failed to load achievements: ${e.message}"
            }
        }
    }

    private fun loadQuestCoins() {
        viewModelScope.launch {
            try {
                val userId = authService.getCurrentUser()?.uid ?: return@launch
                val questCoinsRef =
                    db.collection("users").document(userId).collection("questCoins")
                        .document("balance")

                val snapshot = questCoinsRef.get().await()
                val questCoins = if (snapshot.exists()) {
                    QuestCoins(
                        userId = userId,
                        totalEarned = snapshot.getLong("totalEarned")?.let { if (it <= Int.MAX_VALUE) it.toInt() else 0 } ?: 0,
                        totalRedeemed = snapshot.getLong("totalRedeemed")?.let { if (it <= Int.MAX_VALUE) it.toInt() else 0 } ?: 0,
                        currentBalance = snapshot.getLong("currentBalance")?.let { if (it <= Int.MAX_VALUE) it.toInt() else 0 } ?: 0,
                        lastUpdated = snapshot.getTimestamp("lastUpdated")?.toDate() ?: Date()
                    )
                } else {
                    QuestCoins(
                        userId = userId,
                        totalEarned = 0,
                        totalRedeemed = 0,
                        currentBalance = 0,
                        lastUpdated = Date()
                    )
                }

                _questCoins.value = questCoins

                // Sync points and level on load
                _userPoints.value = questCoins.totalEarned
                updateLevelProgress()

            } catch (e: Exception) {
                _error.value = "Failed to load Quest Coins: ${e.message}"
            }
        }
    }

    fun redeemQuestCoins() {
        val currentQuestCoins = _questCoins.value ?: return
        if (currentQuestCoins.availableBalance < QuestCoins.MIN_REDEMPTION) {
            _error.value = "Insufficient Quest Coins balance"
            return
        }

        viewModelScope.launch {
            try {
                val redeemedAmount = currentQuestCoins.availableBalance
                val updatedQuestCoins = currentQuestCoins.copy(
                    totalRedeemed = currentQuestCoins.totalRedeemed + redeemedAmount,
                    currentBalance = 0,
                    lastUpdated = Date()
                )

                db.collection("users").document(currentQuestCoins.userId)
                    .collection("questCoins").document("balance").set(updatedQuestCoins).await()

                val transaction = QuestCoinsTransaction(
                    userId = currentQuestCoins.userId,
                    amount = redeemedAmount,
                    type = TransactionType.REDEEMED,
                    description = "Redeemed Quest Coins",
                    timestamp = Date()
                )

                db.collection("questCoinsTransactions").add(transaction).await()

                _questCoins.value = updatedQuestCoins
            } catch (e: Exception) {
                _error.value = "Failed to redeem Quest Coins: ${e.message}"
            }
        }
    }

    /**
     * Update achievement progress.
     * Awards quest coins and triggers newlyCompleted event.
     * Also adds points to leveling system.
     */
    fun updateAchievementProgress(userId: String, achievementId: String, progress: Int) {
        viewModelScope.launch {
            try {
                val currentAchievements = _achievements.value ?: return@launch
                val achievement =
                    currentAchievements.find { it.id == achievementId } ?: return@launch

                val isNewlyCompleted = progress >= achievement.requiredProgress && !achievement.isCompleted

                val updatedAchievement = achievement.copy(
                    progress = progress,
                    isCompleted = progress >= achievement.requiredProgress,
                    completedAt = if (progress >= achievement.requiredProgress) Timestamp(Date()) else null
                )

                val achievementData = hashMapOf(
                    "progress" to progress,
                    "isCompleted" to (progress >= achievement.requiredProgress),
                    "completedAt" to (if (progress >= achievement.requiredProgress) Timestamp(Date()) else null)
                )

                db.collection("users").document(userId).collection("achievements")
                    .document(achievementId).set(achievementData).await()

                if (isNewlyCompleted) {
                    val currentQuestCoins = _questCoins.value ?: return@launch
                    val updatedQuestCoins = currentQuestCoins.copy(
                        totalEarned = currentQuestCoins.totalEarned + updatedAchievement.questCoinsReward,
                        currentBalance = currentQuestCoins.currentBalance + updatedAchievement.questCoinsReward,
                        lastUpdated = Date()
                    )

                    db.collection("users").document(userId).collection("questCoins")
                        .document("balance").set(updatedQuestCoins).await()

                    _questCoins.value = updatedQuestCoins
                    _newlyCompleted.value = Event(updatedAchievement)

                    // Add points to level system here!
                    addPoints(updatedAchievement.questCoinsReward)
                }

                _achievements.value = currentAchievements.map {
                    if (it.id == achievementId) updatedAchievement else it
                }
            } catch (e: Exception) {
                _error.value = "Failed to update achievement progress: ${e.message}"
            }
        }
    }
}

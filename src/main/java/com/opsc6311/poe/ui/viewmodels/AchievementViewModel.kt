package com.opsc6311.poe.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.opsc6311.poe.core.services.AuthService
import com.opsc6311.poe.core.utils.Event
import com.opsc6311.poe.data.models.*
import com.opsc6311.poe.data.services.AchievementEvaluator
import com.opsc6311.poe.data.services.AchievementSeedService
import java.util.Date

class AchievementViewModel(
    private val authService: AuthService = AuthService(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val evaluator = AchievementEvaluator(db, auth)
    private val seedService = AchievementSeedService(db)

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

    private val _userAchievements = MutableLiveData<List<Achievement>>()
    val userAchievements: LiveData<List<Achievement>> = _userAchievements

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _newAchievementUnlocked = MutableLiveData<Achievement?>()
    val newAchievementUnlocked: LiveData<Achievement?> = _newAchievementUnlocked

    private val _levelUp = MutableLiveData<Int>()
    val levelUp: LiveData<Int> = _levelUp

    init {
        loadAchievements()
        loadQuestCoins()
        loadUserData()

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
            _levelUp.value = level
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
            _isLoading.value = true
            _error.value = null
            
            try {
                println("🔍 Starting to load achievements...")
                
                // First, ensure achievements are seeded
                println("🌱 Seeding achievements...")
                seedService.seedAchievements()
                
                println("📋 Querying Firestore for achievements...")
                val snapshot = db.collection("achievements")
                    .get()
                    .await()
                
                println("📊 Found ${snapshot.documents.size} achievements in Firestore")
                
                val achievementList = snapshot.documents.mapNotNull { doc ->
                    val achievement = doc.toObject(Achievement::class.java)
                    println("🏆 Achievement: ${achievement?.title} (ID: ${achievement?.id})")
                    achievement
                }.sortedBy { it.category }
                
                println("✅ Loaded ${achievementList.size} achievements successfully")
                _achievements.value = achievementList
                
            } catch (e: Exception) {
                println("❌ Error loading achievements: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load achievements: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadQuestCoins() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
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
                    // Initialize quest coins if they don't exist
                    val initialQuestCoins = QuestCoins(
                        userId = userId,
                        totalEarned = 0,
                        totalRedeemed = 0,
                        currentBalance = 0,
                        lastUpdated = Date()
                    )
                    questCoinsRef.set(initialQuestCoins).await()
                    initialQuestCoins
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

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Ensure user has all achievements initialized
                seedService.ensureUserAchievements(userId)
                
                val snapshot = db.collection("users")
                    .document(userId)
                    .collection("achievements")
                    .get()
                    .await()
                
                val userAchievementList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Achievement::class.java)
                }.sortedBy { it.category }
                
                _userAchievements.value = userAchievementList
                
                // Check for newly completed achievements
                checkForNewlyCompletedAchievements(userAchievementList)
                
            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
            }
        }
    }

    private fun checkForNewlyCompletedAchievements(userAchievements: List<Achievement>) {
        val newlyCompleted = userAchievements.filter {
            it.isCompleted && it.completedAt != null 
        }.filter { achievement ->
            val timeSinceCompletion = System.currentTimeMillis() - (achievement.completedAt?.toDate()?.time ?: 0L)
            timeSinceCompletion < 24 * 60 * 60 * 1000 // Within 24 hours
        }
        
        if (newlyCompleted.isNotEmpty()) {
            _newAchievementUnlocked.value = newlyCompleted.first()
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
                    type = "REDEEMED",
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
    fun updateAchievementProgress(achievementId: String, progress: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Get achievement details
                val achievementDoc = db.collection("achievements").document(achievementId).get().await()
                val achievement = achievementDoc.toObject(Achievement::class.java)
                
                if (achievement != null) {
                    val userAchievementDoc = db.collection("users")
                        .document(userId)
                        .collection("achievements")
                        .document(achievementId)
                    
                    val userAchievement = userAchievementDoc.get().await().toObject(Achievement::class.java)
                    
                    if (userAchievement == null) {
                        // Create new user achievement with progress
                        val newUserAchievement = achievement.copy(
                            progress = progress,
                            isCompleted = progress >= achievement.requiredProgress,
                            completedAt = if (progress >= achievement.requiredProgress) Timestamp.now() else null
                        )
                        
                        userAchievementDoc.set(newUserAchievement).await()
                        
                        // Check if completed
                        if (newUserAchievement.isCompleted) {
                            awardQuestCoins(achievement.questCoinsReward)
                            _newlyCompleted.value = Event(newUserAchievement)
                            addPoints(achievement.questCoinsReward)
                        }
                    } else if (!userAchievement.isCompleted) {
                        // Update existing progress
                        val updatedProgress = progress.coerceAtMost(achievement.requiredProgress)
                        val isCompleted = updatedProgress >= achievement.requiredProgress
                        
                        val updatedUserAchievement = userAchievement.copy(
                            progress = updatedProgress,
                            isCompleted = isCompleted,
                            completedAt = if (isCompleted) Timestamp.now() else null
                        )
                        
                        userAchievementDoc.set(updatedUserAchievement).await()
                        
                        // Check if newly completed
                        if (isCompleted && !userAchievement.isCompleted) {
                            awardQuestCoins(achievement.questCoinsReward)
                            _newlyCompleted.value = Event(updatedUserAchievement)
                            addPoints(achievement.questCoinsReward)
                        }
                    }
                    
                    // Reload user achievements
                    loadUserData()
                }
            } catch (e: Exception) {
                _error.value = "Failed to update achievement progress: ${e.message}"
            }
        }
    }

    private fun awardQuestCoins(amount: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val questCoinsRef = db.collection("users")
                    .document(userId)
                    .collection("questCoins")
                    .document("balance")
                
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(questCoinsRef)
                    val currentBalance = snapshot.getLong("currentBalance") ?: 0
                    val totalEarned = snapshot.getLong("totalEarned") ?: 0
                    
                    transaction.update(questCoinsRef, mapOf(
                        "currentBalance" to (currentBalance + amount),
                        "totalEarned" to (totalEarned + amount),
                        "lastUpdated" to Timestamp.now()
                    ))
                }.await()
                
                // Reload quest coins
                loadQuestCoins()
            } catch (e: Exception) {
                _error.value = "Failed to award quest coins: ${e.message}"
            }
        }
    }

    fun evaluateAchievements() {
        viewModelScope.launch {
            try {
                evaluator.evaluateUserAchievements()
                loadUserData() // Reload user data after evaluation
            } catch (e: Exception) {
                _error.value = "Failed to evaluate achievements: ${e.message}"
            }
        }
    }

    fun refreshData() {
        loadAchievements()
        loadQuestCoins()
        loadUserData()
    }

    fun getAchievementsByCategory(category: String): List<Achievement> {
        return _achievements.value?.filter { it.category.name == category } ?: emptyList()
    }
    
    fun getUserAchievementsByCategory(category: String): List<Achievement> {
        return _userAchievements.value?.filter { it.category.name == category } ?: emptyList()
    }
    
    fun getCompletedAchievements(): List<Achievement> {
        return _userAchievements.value?.filter { it.isCompleted } ?: emptyList()
    }
    
    fun getInProgressAchievements(): List<Achievement> {
        return _userAchievements.value?.filter { !it.isCompleted && it.progress > 0 } ?: emptyList()
    }
    
    fun getLockedAchievements(): List<Achievement> {
        val userAchievementIds = _userAchievements.value?.map { it.id }?.toSet() ?: emptySet()
        return _achievements.value?.filter { !userAchievementIds.contains(it.id) } ?: emptyList()
    }
    
    fun getTotalQuestCoinsEarned(): Int {
        return _userAchievements.value?.sumOf { 
            if (it.isCompleted) it.questCoinsReward else 0 
        } ?: 0
    }
    
    fun getAchievementProgress(): AchievementProgress {
        val total = _achievements.value?.size ?: 0
        val completed = getCompletedAchievements().size
        val inProgress = getInProgressAchievements().size
        
        return AchievementProgress(
            total = total,
            completed = completed,
            inProgress = inProgress,
            locked = total - completed - inProgress
        )
    }
    
    fun clearNewAchievementNotification() {
        _newAchievementUnlocked.value = null
    }
    
    fun clearLevelUpNotification() {
        _levelUp.value = 0
    }
    
    fun clearError() {
        _error.value = null
    }
    
    data class AchievementProgress(
        val total: Int,
        val completed: Int,
        val inProgress: Int,
        val locked: Int
    ) {
        val completionPercentage: Float
            get() = if (total > 0) (completed.toFloat() / total) * 100 else 0f
    }
}

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
import java.util.Date

class AchievementViewModel(
    private val authService: AuthService = AuthService(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val evaluator = AchievementEvaluator(db, auth)

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
                val snapshot = db.collection("achievements")
                    .whereEqualTo("isPublic", true)
                    .get()
                    .await()
                
                val achievementList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Achievement::class.java)
                }.sortedBy { it.category }
                
                _achievements.value = achievementList
            } catch (e: Exception) {
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
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                // Get achievement details
                val achievementDoc = db.collection("achievements").document(achievementId).get().await()
                val achievement = achievementDoc.toObject(Achievement::class.java)
                
                if (achievement != null && achievement.requiredProgress != null) {
                    val userAchievementDoc = db.collection("users")
                        .document(userId)
                        .collection("achievements")
                        .document(achievementId)
                    
                    val userAchievement = userAchievementDoc.get().await().toObject(Achievement::class.java)
                    
                    if (userAchievement == null) {
                        // Create new user achievement with progress
                        val newUserAchievement = achievement.copy(
                            progress = progress,
                            isCompleted = progress >= achievement.requiredProgress
                        )
                        
                        userAchievementDoc.set(newUserAchievement).await()
                        
                        // Check if completed
                        if (newUserAchievement.isCompleted) {
                            addPoints(achievement.questCoinsReward)
                            _newAchievementUnlocked.value = newUserAchievement
                        }
                    } else if (!userAchievement.isCompleted) {
                        // Update existing progress
                        val updatedProgress = progress.coerceAtMost(achievement.requiredProgress)
                        val isCompleted = updatedProgress >= achievement.requiredProgress
                        
                        userAchievementDoc.update(
                            mapOf(
                                "progress" to updatedProgress,
                                "isCompleted" to isCompleted
                            )
                        ).await()
                        
                        // Check if newly completed
                        if (isCompleted && !userAchievement.isCompleted) {
                            addPoints(achievement.questCoinsReward)
                            _newAchievementUnlocked.value = userAchievement.copy(
                                progress = updatedProgress,
                                isCompleted = true
                            )
                        }
                    }
                }
                
                // Reload user data
                loadUserData()
                
            } catch (e: Exception) {
                _error.value = "Failed to update achievement progress: ${e.message}"
            }
        }
    }

    /**
     * Auto-evaluate achievements based on user data
     */
    fun evaluateAchievements() {
        viewModelScope.launch {
            try {
                evaluator.evaluateUserAchievements()
                
                // Reload user data after evaluation
                loadUserData()
                
                // Check for new achievements
                checkForNewAchievements()
                
            } catch (e: Exception) {
                _error.value = "Failed to evaluate achievements: ${e.message}"
            }
        }
    }

    private fun evaluateFirstExpense(userId: String, transactions: List<Transaction>) {
        val hasExpense = transactions.any { it.type == TransactionType.EXPENSE }
        if (hasExpense) {
            updateAchievementProgress("first_expense", 1)
        }
    }

    private fun evaluateFirstIncome(userId: String, transactions: List<Transaction>) {
        val hasIncome = transactions.any { it.type == TransactionType.INCOME }
        if (hasIncome) {
            updateAchievementProgress("first_income", 1)
        }
    }

    private fun evaluateBigSaver(userId: String, accounts: List<Account>) {
        val totalSaved = accounts
            .filter { it.type.equals("Savings", ignoreCase = true) }
            .sumOf { it.balance }
        
        if (totalSaved >= 1000) {
            updateAchievementProgress("big_saver", totalSaved.toInt())
        }
    }

    private fun evaluateEmergencyFund(userId: String, accounts: List<Account>, categories: List<Category>) {
        val emergencyTotal = accounts
            .filter { it.type.equals("Emergency", ignoreCase = true) }
            .sumOf { it.balance }

        val emergencyGoal = categories
            .find { it.name.equals("Emergency", ignoreCase = true) }
            ?.maxBudget ?: 1000.0

        if (emergencyTotal >= emergencyGoal) {
            updateAchievementProgress("emergency_fund", emergencyTotal.toInt())
        }
    }

    private fun evaluateCategoryMaster(userId: String, categories: List<Category>) {
        val customCategories = categories.count { it.type == CategoryType.CUSTOM }
        if (customCategories >= 3) {
            updateAchievementProgress("category_master", customCategories)
        }
    }

    private fun evaluateDailyTracker(userId: String, transactions: List<Transaction>) {
        // This is a simplified version - in a real app you'd track daily usage
        val oneWeekAgo = java.util.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
        val recentTransactions = transactions.filter { 
            it.date.toDate().after(oneWeekAgo)
        }
        val uniqueDays = recentTransactions.map { it.date.toDate() }.distinct().size
        updateAchievementProgress("daily_tracker", uniqueDays)
    }

    private fun evaluateBudgetingBeginner(userId: String, userDoc: com.google.firebase.firestore.DocumentSnapshot) {
        val hasCompletedOnboarding = userDoc.getBoolean("hasCompletedOnboarding") ?: false
        if (hasCompletedOnboarding) {
            updateAchievementProgress("budgeting_beginner", 1)
        }
    }

    fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                // Load user achievements
                val userSnapshot = db.collection("users")
                    .document(userId)
                    .collection("achievements")
                    .get()
                    .await()
                
                val userAchievementList = userSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Achievement::class.java)
                }
                
                _userAchievements.value = userAchievementList
                
                // Load quest coins
                val userDoc = db.collection("users").document(userId).get().await()
                val coins = userDoc.getLong("questCoins") ?: 0
                _questCoins.value = QuestCoins(
                    userId = userId,
                    totalEarned = coins.toInt(),
                    totalRedeemed = 0,
                    currentBalance = coins.toInt(),
                    lastUpdated = Date()
                )
                
            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
            }
        }
    }

    private suspend fun checkForNewAchievements() {
        val userId = auth.currentUser?.uid ?: return
        val previousAchievements = _userAchievements.value?.size ?: 0
        
        val userSnapshot = db.collection("users")
            .document(userId)
            .collection("achievements")
            .get()
            .await()
        
        val currentAchievements = userSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Achievement::class.java)
        }
        
        if (currentAchievements.size > previousAchievements) {
            // Find the newest achievement
            val newAchievement = currentAchievements.maxByOrNull { it.completedAt?.toDate() ?: java.util.Date(0) }
            if (newAchievement != null) {
                _newAchievementUnlocked.value = newAchievement
                
                // Check for level up
                checkForLevelUp()
            }
        }
    }
    
    private suspend fun checkForLevelUp() {
        val userId = auth.currentUser?.uid ?: return
        
        val userDoc = db.collection("users").document(userId).get().await()
        val currentCoins = userDoc.getLong("questCoins") ?: 0
        val currentLevel = userDoc.getLong("level") ?: 1
        
        val newLevel = calculateLevel(currentCoins.toInt())
        
        if (newLevel > currentLevel) {
            // Update user level
            db.collection("users").document(userId)
                .update("level", newLevel)
                .await()
            
            _levelUp.value = newLevel.toInt()
        }
    }
    
    private fun calculateLevel(coins: Int): Long {
        // Level calculation: every 1000 coins = 1 level
        return (coins / 1000 + 1).toLong()
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
    
    fun refresh() {
        loadAchievements()
        loadUserData()
        evaluateAchievements()
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

package com.opsc6311.poe.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.opsc6311.poe.core.services.AuthService
import com.opsc6311.poe.data.models.*
import com.opsc6311.poe.data.services.FirestoreService
import com.opsc6311.poe.ui.viewmodels.DashboardUiState.Failure
import com.opsc6311.poe.ui.viewmodels.DashboardUiState.Loading
import com.opsc6311.poe.ui.viewmodels.DashboardUiState.Updated
import java.util.Calendar
import java.util.Date
import java.util.UUID

class DashboardViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {
    companion object {
        private const val TAG = "DashboardViewModel"
    }

    private val _uiState = MutableLiveData<DashboardUiState>()
    val uiState: LiveData<DashboardUiState> = _uiState

    private var _statistics: MonthlyStats? = null
    private var _breakdowns: Map<String, Double>? = null

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _savingsGoals = MutableLiveData<List<SavingsGoal>>() // LiveData for savings goals
    val savingsGoals: LiveData<List<SavingsGoal>> = _savingsGoals // Expose as LiveData

    private var currentBudget: Budget? = null
    private var currentCategoryList: List<Category>? = null

    private val _accounts = MutableLiveData<List<Account>>()
    val accounts: LiveData<List<Account>> = _accounts

    init {
        loadInitialData()
    }

    fun refreshData() {
        _uiState.value = Loading
        loadInitialData()
    }

    private fun emitUpdatedState() {
        _uiState.value = Updated(
            categoryList = currentCategoryList,
            breakdowns = _breakdowns,
            statistics = _statistics,
            savingsGoals = _savingsGoals.value, // Use LiveData value
            budget = currentBudget,
            accounts = _accounts.value // Include accounts
        )
    }

    private fun loadInitialData() = viewModelScope.launch {
        val userId = authService.getCurrentUser ()?.uid ?: run {
            _uiState.value = Failure("User  not authenticated")
            return@launch
        }

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        loadSavingsGoals()
        loadCurrentBudget(year, month)
        loadMonthlyStats()
        loadCategoryBreakdown(year, month)
        loadCategories()
        loadAccounts()
    }

    private fun loadSavingsGoals() {
        FirestoreService.savingsGoal.fetchGoals { goals ->
            _savingsGoals.value = goals // Update LiveData
            emitUpdatedState()
        }
    }

    private fun loadCurrentBudget(year: Int, month: Int) {
        FirestoreService.budget.getBudgetForMonth(year, month) { bud ->
            currentBudget = bud
            emitUpdatedState()
        }
    }

    private fun loadMonthlyStats() {
        val userId = authService.getCurrentUser ()?.uid ?: return
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        FirestoreService.account.getAllAccounts { accounts ->
            var totalExpenses = 0.0
            var processedAccounts = 0

            accounts.forEach { account ->
                FirestoreService.account.getTransactionsForAccount(account.id) { transactions ->
                    totalExpenses += transactions
                        .filter {
                            val txDate = it.date.toDate()
                            txDate.month == currentMonth &&
                                    txDate.year + 1900 == currentYear &&
                                    it.type == TransactionType.EXPENSE
                        }
                        .sumOf { it.amount }

                    processedAccounts++

                    if (processedAccounts == accounts.size) {
                        FirestoreService.savingsGoal.fetchGoals { goals ->
                            val goal = goals.firstOrNull()
                            val monthlyBudget = goal?.monthlyBudget ?: 0.0

                            val stats = MonthlyStats(
                                totalExpenses = totalExpenses,
                                budget = monthlyBudget
                            )
                            _statistics = stats
                            currentBudget = Budget(
                                max = monthlyBudget,
                                min = goal?.minMonthlyGoal ?: 0.0,
                                month = currentMonth + 1,
                                year = currentYear
                            )
                            emitUpdatedState()
                        }
                    }
                }
            }
        }
    }

    private fun loadCategoryBreakdown(year: Int, month: Int) {
        val userId = authService.getCurrentUser ()?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("accounts").get()
            .addOnSuccessListener { accountDocs ->
                val accountIds = accountDocs.map { it.id }
                val categoryTotals = mutableMapOf<String, Double>()
                val tasks = accountIds.map { accountId ->
                    db.collection("users").document(userId).collection("accounts")
                        .document(accountId).collection("transactions")
                        .whereEqualTo("type", "EXPENSE").get()
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(tasks)
                    .addOnSuccessListener { results ->
                        for ((index, res) in results.withIndex()) {
                            val snap = res as com.google.firebase.firestore.QuerySnapshot
                            for (doc in snap.documents) {
                                val category = doc.getString("category") ?: "Other"
                                val amount = doc.getDouble("amount") ?: 0.0
                                categoryTotals[category] =
                                    (categoryTotals[category] ?: 0.0) + amount
                            }
                        }
                        _breakdowns = categoryTotals
                        emitUpdatedState()
                    }
                    .addOnFailureListener {
                        _breakdowns = emptyMap()
                        emitUpdatedState()
                    }
            }
            .addOnFailureListener {
                _breakdowns = emptyMap()
                emitUpdatedState()
            }
    }

    private fun loadCategories() {
        val userId = authService.getCurrentUser ()?.uid ?: return
        FirestoreService.category.getAllCategories { categories ->
            val finalCategories = categories ?: emptyList()
            _categories.postValue(finalCategories)
            currentCategoryList = finalCategories
            emitUpdatedState()
        }
    }

    fun updateBudget(budget: Budget) {
        FirestoreService.budget.updateBudget(budget) { success ->
            if (success) {
                currentBudget = budget
                emitUpdatedState()
            } else {
                _uiState.value = Failure("Failed to update budget")
            }
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        val updates = mapOf<String, Any>(
            "name" to goal.name,
            "targetAmount" to goal.targetAmount,
            "savedAmount" to goal.savedAmount,
            "targetDate" to (goal.targetDate ?: Date()),
            "minMonthlyGoal" to goal.minMonthlyGoal,
            "maxMonthlyGoal" to goal.maxMonthlyGoal,
            "monthlyBudget" to goal.monthlyBudget
        )

        FirestoreService.savingsGoal.updateGoal(goal.id, updates) { success ->
            if (success) {
                _savingsGoals.value = _savingsGoals.value?.map {
                    if (it.id == goal.id) goal else it
                }
                emitUpdatedState()
            } else {
                _uiState.value = Failure("Failed to update savings goal")
            }
        }
    }

    fun contributeToSavingsGoal(goalId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val goal = _savingsGoals.value?.find { it.id == goalId }
                if (goal == null) {
                    _uiState.value = DashboardUiState.Failure("Savings goal not found")
                    return@launch
                }

                FirestoreService.account.getAllAccounts { accounts ->
                    var totalIncome = 0.0
                    var processedAccounts = 0

                    accounts.forEach { account ->
                        FirestoreService.account.getTransactionsForAccount(account.id) { transactions ->
                            totalIncome += transactions
                                .filter { it.type == TransactionType.INCOME }
                                .sumOf { it.amount }

                            processedAccounts++

                            if (processedAccounts == accounts.size) {
                                if (amount > totalIncome) {
                                    _uiState.value = DashboardUiState.Failure("Contribution amount exceeds available income")
                                    return@getTransactionsForAccount
                                }

                                val contributionTransaction = Transaction(
                                    id = UUID.randomUUID().toString(),
                                    type = TransactionType.EXPENSE,
                                    amount = amount,
                                    description = "Contribution to ${goal.name}",
                                    date = com.google.firebase.Timestamp.now(),
                                    category = "Savings",
                                    accountId = accounts.first().id,
                                    userId = authService.getCurrentUser ()?.uid ?: return@getTransactionsForAccount
                                )

                                FirestoreService.transaction.addTransaction(contributionTransaction) { success ->
                                    if (!success) {
                                        _uiState.value = DashboardUiState.Failure("Failed to record contribution")
                                        return@addTransaction
                                    }

                                    val updatedGoal = goal.copy(
                                        savedAmount = goal.savedAmount + amount,
                                        lastContributionDate = Date()
                                    )

                                    val updates = mapOf<String, Any>(
                                        "savedAmount" to updatedGoal.savedAmount,
                                        "lastContributionDate" to updatedGoal.lastContributionDate!!
                                    )

                                    FirestoreService.savingsGoal.updateGoal(goalId, updates) { success ->
                                        if (success) {
                                            _savingsGoals.value = _savingsGoals.value?.map {
                                                if (it.id == goalId) updatedGoal else it
                                            }
                                            emitUpdatedState()
                                        } else {
                                            _uiState.value = DashboardUiState.Failure("Failed to update savings goal")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Failure("Failed to contribute to savings goal: ${e.message}")
            }
        }
    }

    fun getMonthlyBudgetForGoal(goalId: String): Double {
        return _savingsGoals.value?.find { it.id == goalId }?.monthlyBudget ?: 0.0
    }

    fun getMonthlyContributionForGoal(goalId: String): Double {
        return _savingsGoals.value?.find { it.id == goalId }?.monthlyContribution ?: 0.0
    }

    fun updateCategory(category: Category) {
        val userId = authService.getCurrentUser ()?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("categories")
            .document(category.id)
            .set(category)
            .addOnSuccessListener {
                currentCategoryList = currentCategoryList?.map {
                    if (it.id == category.id) category else it
                }
                emitUpdatedState()
            }
            .addOnFailureListener {
                _uiState.value = Failure("Failed to update category")
            }
    }

    fun deleteCategory(categoryId: String) {
        val userId = authService.getCurrentUser ()?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("categories")
            .document(categoryId)
            .delete()
            .addOnSuccessListener {
                currentCategoryList = currentCategoryList?.filter { it.id != categoryId }
                emitUpdatedState()
            }
            .addOnFailureListener {
                _uiState.value = Failure("Failed to delete category")
            }
    }

    private fun loadAccounts() {
        FirestoreService.account.getAllAccounts { accounts ->
            _accounts.postValue(accounts)
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                FirestoreService.account.getTransactionsForAccount(accountId) { transactions ->
                    var deletedTransactions = 0
                    val totalTransactions = transactions.size

                    if (totalTransactions == 0) {
                        deleteAccountFromFirestore(accountId)
                        return@getTransactionsForAccount
                    }

                    transactions.forEach { transaction ->
                        val userId = authService.getCurrentUser ()?.uid ?: return@getTransactionsForAccount
                        val db = FirebaseFirestore.getInstance()

                        db.collection("users")
                            .document(userId)
                            .collection("accounts")
                            .document(accountId)
                            .collection("transactions")
                            .document(transaction.id)
                            .delete()
                            .addOnSuccessListener {
                                deletedTransactions++
                                if (deletedTransactions == totalTransactions) {
                                    deleteAccountFromFirestore(accountId)
                                }
                            }
                            .addOnFailureListener { exception ->
                                deletedTransactions++
                                if (deletedTransactions == totalTransactions) {
                                    deleteAccountFromFirestore(accountId)
                                }
                            }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = Failure("Failed to delete account: ${e.message}")
            }
        }
    }

    private fun deleteAccountFromFirestore(accountId: String) {
        val userId = authService.getCurrentUser ()?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("accounts")
            .document(accountId)
            .delete()
            .addOnSuccessListener {
                val currentAccounts = _accounts.value?.toMutableList() ?: mutableListOf()
                currentAccounts.removeAll { it.id == accountId }
                _accounts.postValue(currentAccounts)
            }
            .addOnFailureListener { exception ->
                _uiState.value = Failure("Failed to delete account: ${exception.message}")
            }
    }

    fun addAccount(account: Account) {
        val userId = authService.getCurrentUser ()?.uid ?: return
        val accountWithUserId = account.copy(userId = userId)
        
        FirestoreService.account.addAccount(accountWithUserId) { success ->
            if (success) {
                val currentAccounts = _accounts.value?.toMutableList() ?: mutableListOf()
                currentAccounts.add(accountWithUserId)
                _accounts.postValue(currentAccounts)
                emitUpdatedState()
            } else {
                _uiState.value = Failure("Failed to add account")
            }
        }
    }

    fun transferBetweenAccounts(fromAccountId: String, toAccountId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val fromAccount = _accounts.value?.find { it.id == fromAccountId }
                val toAccount = _accounts.value?.find { it.id == toAccountId }

                if (fromAccount == null || toAccount == null) {
                    _uiState.value = Failure("Invalid account selection")
                    return@launch
                }

                if (fromAccount.balance < amount) {
                    _uiState.value = Failure("Insufficient balance in source account")
                    return@launch
                }

                // Create transfer transactions
                val transferOutTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    description = "Transfer to ${toAccount.name}",
                    date = com.google.firebase.Timestamp.now(),
                    category = "Transfer",
                    accountId = fromAccountId,
                    userId = authService.getCurrentUser()?.uid ?: return@launch
                )

                val transferInTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.INCOME,
                    amount = amount,
                    description = "Transfer from ${fromAccount.name}",
                    date = com.google.firebase.Timestamp.now(),
                    category = "Transfer",
                    accountId = toAccountId,
                    userId = authService.getCurrentUser()?.uid ?: return@launch
                )

                // Add both transactions
                FirestoreService.transaction.addTransaction(transferOutTransaction) { success1 ->
                    if (!success1) {
                        _uiState.value = Failure("Failed to record transfer out transaction")
                        return@addTransaction
                    }

                    FirestoreService.transaction.addTransaction(transferInTransaction) { success2 ->
                        if (!success2) {
                            _uiState.value = Failure("Failed to record transfer in transaction")
                            return@addTransaction
                        }

                        // Update account balances
                        val updatedFromAccount = fromAccount.copy(balance = fromAccount.balance - amount)
                        val updatedToAccount = toAccount.copy(balance = toAccount.balance + amount)

                        val currentAccounts = _accounts.value?.toMutableList() ?: mutableListOf()
                        val fromIndex = currentAccounts.indexOfFirst { it.id == fromAccountId }
                        val toIndex = currentAccounts.indexOfFirst { it.id == toAccountId }

                        if (fromIndex != -1) currentAccounts[fromIndex] = updatedFromAccount
                        if (toIndex != -1) currentAccounts[toIndex] = updatedToAccount

                        _accounts.postValue(currentAccounts)
                        emitUpdatedState()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = Failure("Failed to transfer between accounts: ${e.message}")
            }
        }
    }

    fun contributeToSavings(amount: Double, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val currentGoals = _savingsGoals.value
                if (currentGoals.isNullOrEmpty()) {
                    _uiState.value = Failure("No savings goals found")
                    return@launch
                }

                val goal = currentGoals.first()
                val newSavedAmount = goal.savedAmount + amount
                
                // Update the savings goal
                val updatedGoal = goal.copy(savedAmount = newSavedAmount)
                
                val userId = authService.getCurrentUser()?.uid ?: return@launch
                val db = FirebaseFirestore.getInstance()

                db.collection("users").document(userId).collection("savings_goals")
                    .document(goal.id)
                    .set(updatedGoal)
                    .addOnSuccessListener {
                        // Update local state immediately
                        val updatedGoals = currentGoals.map { 
                            if (it.id == goal.id) updatedGoal else it 
                        }
                        _savingsGoals.postValue(updatedGoals)
                        
                        // Force a complete UI refresh
                        emitUpdatedState()
                        
                        // Call success callback
                        onSuccess?.invoke()
                        
                        // Also refresh all data to ensure consistency
                        loadInitialData()
                    }
                    .addOnFailureListener { exception ->
                        _uiState.value = Failure("Failed to contribute to savings: ${exception.message}")
                    }
            } catch (e: Exception) {
                _uiState.value = Failure("Failed to contribute to savings: ${e.message}")
            }
        }
    }
}

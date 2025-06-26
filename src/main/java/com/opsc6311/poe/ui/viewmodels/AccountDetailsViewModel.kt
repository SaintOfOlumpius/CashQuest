package com.opsc6311.poe.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.opsc6311.poe.data.models.Account
import com.opsc6311.poe.data.models.Transaction
import com.opsc6311.poe.data.models.TransactionType
import com.opsc6311.poe.data.repository.AccountRepository

class AccountDetailsViewModel(
    private val repo: AccountRepository = AccountRepository()
) : ViewModel() {

    private val _account = MutableLiveData<Account?>()
    val account: LiveData<Account?> = _account

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _calculatedBalance = MutableLiveData<Double>()
    val calculatedBalance: LiveData<Double> = _calculatedBalance


    private var allTxs: List<Transaction> = emptyList()

    /** ← now needs userId too */
    fun loadAccountDetails(userId: String, accountId: String) {
        android.util.Log.d("AccountDetailsViewModel", "Loading account details for user: $userId, account: $accountId")
        _isLoading.value = true
        _error.value = null

        // 1) load the account document (with its stored aggregates)
        repo.getAccount(accountId) { acct ->
            android.util.Log.d("AccountDetailsViewModel", "Account loaded: ${acct?.name} with ID: ${acct?.id}")
            _account.postValue(acct)

            // 2) load all txs (we'll filter later)
            repo.getTransactionsForAccount(accountId) { txs ->
                android.util.Log.d("AccountDetailsViewModel", "Transactions loaded: ${txs.size}")
                allTxs = txs
                _transactions.postValue(txs)
                _isLoading.postValue(false)
                _calculatedBalance.postValue(calculateBalance(txs))
            }
        }
    }

    fun filterTransactionsByTimePeriod(period: String) {
        val now = System.currentTimeMillis()
        val cutoff = when (period) {
            "1 week" -> now - 7L * 24 * 60 * 60 * 1000
            "1 month" -> now - 30L * 24 * 60 * 60 * 1000
            "3 months" -> now - 90L * 24 * 60 * 60 * 1000
            else -> 0L
        }
        
        _transactions.value = allTxs.filter { 
            it.date.toDate().time >= cutoff 
        }
    }

    fun deleteAccount(accountId: String) {
        _isLoading.value = true
        _error.value = null
        repo.deleteAccount(accountId) { success ->
            if (!success) _error.postValue("Failed to delete account.")
            _isLoading.postValue(false)
        }
    }

    private fun calculateBalance(transactions: List<Transaction>): Double {
        return transactions.sumOf { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> transaction.amount
                TransactionType.EXPENSE -> -transaction.amount
                TransactionType.EARNED -> 0.0
                TransactionType.REDEEMED -> 0.0
            }
        }
    }
}

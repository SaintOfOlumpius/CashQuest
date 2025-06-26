package com.opsc6311.poe.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.opsc6311.poe.data.models.Account
import com.opsc6311.poe.data.models.Transaction
import com.opsc6311.poe.data.models.TransactionType
import com.opsc6311.poe.data.repository.AccountRepository
import com.opsc6311.poe.ui.viewmodels.AccountsUiState.Failure
import com.opsc6311.poe.ui.viewmodels.AccountsUiState.Loading
import com.opsc6311.poe.ui.viewmodels.AccountsUiState.Updated

class AccountsViewModel(
    private val repository: AccountRepository = AccountRepository()
) : ViewModel() {
    companion object {
        private const val TAG = "AccountsViewModel"
    }


    // --- Fields


    private val _uiState = MutableLiveData<AccountsUiState>()
    val uiState: LiveData<AccountsUiState> = _uiState
    
    
    init {
        fetchAccounts()
    }


    // --- Internals


    fun fetchAccounts() {
        _uiState.value = Loading
        repository.getAllAccounts { accountList ->
            if (accountList.isEmpty()) {
                _uiState.value = Updated(
                    accounts = emptyList(), netWorth = 0.0
                )
                return@getAllAccounts
            }

            // For each account, fetch transactions and update balance/count
            val updatedAccounts = mutableListOf<Account>()
            var completed = 0

            accountList.forEach { account ->
                repository.getTransactionsForAccount(account.id) { txs ->
                    // Compute balance and count
                    val balance = calculateBalance(txs)
                    account.balance = balance
                    account.transactionsCount = txs.size

                    updatedAccounts.add(account)
                    completed++

                    // When all accounts have been processed, post the results
                    if (completed == accountList.size) {
                        _uiState.value = Updated(
                            accounts = updatedAccounts, netWorth = updatedAccounts.sumOf {
                                it.balance
                            })
                    }
                }
            }
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

    
    fun addAccount(account: Account) {
        _uiState.value = Loading
        repository.addAccount(account) { success ->
            if (success) fetchAccounts()
            else _uiState.value = Failure("Failed to create account.")
        }
    }

    
    fun updateAccount(account: Account) {
        _uiState.value = Loading
        repository.updateAccount(account) { success ->
            if (success) fetchAccounts()
            else _uiState.value = Failure("Failed to update account.")
        }
    }

    
    fun deleteAccount(accountId: String) {
        _uiState.value = Loading
        repository.deleteAccount(accountId) { success ->
            if (success) fetchAccounts()
            else _uiState.value = Failure("Failed to delete account.")
        }
    }
}

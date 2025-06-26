package com.opsc6311.poe.ui.viewmodels

sealed class TransactionState {
    object Loading : TransactionState()
    data class Success(val message: String = "Transaction saved successfully") : TransactionState()
    data class Error(val message: String) : TransactionState()
} 

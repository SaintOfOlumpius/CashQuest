package com.opsc6311.poe.ui.viewmodels

sealed class TransactionUpsertUiState {
    object Loading : TransactionUpsertUiState()
    data class Success(val message: String) : TransactionUpsertUiState()
    data class Failure(val message: String) : TransactionUpsertUiState()
}

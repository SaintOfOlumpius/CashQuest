package com.opsc6311.poe.ui.viewmodels

sealed interface SignUpUiState {
    object Success : SignUpUiState
    object Loading : SignUpUiState
    data class Failure(val message: String) : SignUpUiState
}

package com.opsc6311.poe.ui.viewmodels

sealed interface SignInUiState {
    object Success : SignInUiState
    object Loading : SignInUiState
    data class Failure(val message: String) : SignInUiState
}

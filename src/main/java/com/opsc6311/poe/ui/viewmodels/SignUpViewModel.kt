package com.opsc6311.poe.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.opsc6311.poe.core.models.SignUpCredentials
import com.opsc6311.poe.core.services.AuthService
import com.opsc6311.poe.core.utils.Blogger
import com.opsc6311.poe.data.models.User
import com.opsc6311.poe.data.services.FirestoreService
import com.opsc6311.poe.ui.viewmodels.SignUpUiState.Failure
import com.opsc6311.poe.ui.viewmodels.SignUpUiState.Loading
import com.opsc6311.poe.ui.viewmodels.SignUpUiState.Success

class SignUpViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {
    companion object {
        private const val TAG = "SignUpViewModel"
    }

    // --- Fields

    private val _uiState = MutableLiveData<SignUpUiState>()
    val uiState: LiveData<SignUpUiState> = _uiState

    // --- Activity Functions

    /**
     * Uses the [AuthService] to register a new user in Firebase.
     */
    fun signUp(
        credentials: SignUpCredentials
    ) = viewModelScope.launch {
        credentials.getValidationErrors()?.let {
            _uiState.value = Failure(it)
            return@launch
        }

        _uiState.value = Loading

        runCatching {
            withTimeout(10000) {
                authService.signUpAsync(
                    usermail = credentials.usermail, // Asserted by now
                    password = credentials.defaultPassword // Validated
                )
            }
        }.apply {
            onSuccess { user ->
                Log.d(TAG, "✅ Firebase Auth user created successfully: ${user.uid}")
                linkUserToDatabase(user.uid, credentials)
            }

            onFailure { throwable ->
                Log.e(TAG, "❌ Firebase Auth sign-up failed: ${throwable.message}")
                Blogger.d(TAG, "Sign-up failed: ${throwable.message}")
                _uiState.value = Failure("Unexpected error during sign-up")
            }
        }
    }

    private fun linkUserToDatabase(
        userId: String, credentials: SignUpCredentials
    ) {
        // Debug: Check authentication status
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d(TAG, "🔍 Debug - Current Firebase Auth user: ${currentUser?.uid}")
        Log.d(TAG, "🔍 Debug - Expected user ID: $userId")
        Log.d(TAG, "🔍 Debug - User authenticated: ${currentUser != null}")
        
        if (currentUser == null) {
            Log.e(TAG, "❌ No authenticated user found when trying to save to Firestore")
            viewModelScope.launch {
                onDatabaseLinkFailure()
                _uiState.value = Failure("Authentication error - please try again")
            }
            return
        }

        val user = User(
            id = userId,
            email = credentials.usermail,
            name = credentials.name,
            surname = credentials.surname
        )

        Log.d(TAG, "📝 Attempting to save user to Firestore: $user")

        FirestoreService.user.addUser(user) { operationSuccessful ->
            when (operationSuccessful) {
                true -> {
                    Log.d(TAG, "✅ User successfully saved to Firestore")
                    onDatabaseLinkSuccess()
                }
                else -> {
                    Log.e(TAG, "❌ Failed to save user to Firestore - permission denied")
                    viewModelScope.launch {
                        onDatabaseLinkFailure()
                        _uiState.value = Failure("Database error - please check your connection")
                    }
                }
            }
        }
    }

    // --- Callbacks

    private fun onDatabaseLinkSuccess() {
        Blogger.i(TAG, "Successfully linked the auth user in the database")
        _uiState.value = Success
    }

    private suspend fun onDatabaseLinkFailure() {
        Log.w(TAG, "🔄 Cleaning up - deleting Firebase Auth user due to Firestore failure")
        runCatching {
            withTimeout(5000) {
                authService.deleteCurrentUserAsync()
            }
        }.apply {
            onSuccess {
                Log.i(TAG, "✅ Successfully deleted the auth user")
                Blogger.i(TAG, "Successfully deleted the auth user")
            }

            onFailure { throwable ->
                Log.w(TAG, "⚠️ Failed to delete the auth user: ${throwable.message}")
                Blogger.i(TAG, "Failed to delete the auth user: ${throwable.message}")
            }
        }
    }
}

package com.opsc6311.poe.core.models

import androidx.biometric.BiometricPrompt

interface BiometricUiHost {
    
    fun onShowBiometrics(uiBuilder: BiometricPrompt.PromptInfo.Builder)
    
    fun onBiometricsSucceeded()
    
    fun onBiometricsDismissed()

    fun onBiometricsException(
        code: Int, message: String
    )
}

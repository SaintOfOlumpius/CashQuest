package com.opsc6311.poe

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp

class BudgetApp : Application() {
    companion object {
        private const val TAG = "BudgetApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        // Check Google Play Services availability
        checkGooglePlayServices()
    }
    
    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        when (resultCode) {
            com.google.android.gms.common.ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services is available")
            }
            else -> {
                Log.e(TAG, "Google Play Services is not available. Result code: $resultCode")
                Log.e(TAG, "Error: ${googleApiAvailability.getErrorString(resultCode)}")
            }
        }
    }
} 

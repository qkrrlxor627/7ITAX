package com.ssafy.seveniTax.util

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricHelper @Inject constructor() {

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        TODO("Implement biometric authentication")
    }

    fun isAvailable(activity: FragmentActivity): Boolean {
        TODO("Implement biometric availability check")
    }
}

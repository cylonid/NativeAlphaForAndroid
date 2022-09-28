package com.cylonid.nativealpha.helper

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt.PromptInfo

internal class BiometricPromptHelper(private val activity: FragmentActivity) {
    fun showPrompt(funSuccess: BiometricPromptCallback, funFail: BiometricPromptCallback, promptTitle: String) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    funFail.execute()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    funSuccess.execute()
                }
            })
        val promptInfo = PromptInfo.Builder()
            .setTitle(promptTitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    internal fun interface BiometricPromptCallback {
        fun execute()
    }
}
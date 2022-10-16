package com.cylonid.nativealpha.helper

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt.PromptInfo
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.util.Utility
import com.google.android.material.snackbar.Snackbar

internal class BiometricPromptHelper(private val activity: FragmentActivity) {
    fun showPrompt(funSuccess: BiometricPromptCallback, funFail: BiometricPromptCallback, promptTitle: String) {
        val supported = isBiometricsSupported(activity);
        if(!supported) return;
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

    private fun isBiometricsSupported(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        var isSupported = false

        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                isSupported = true
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Utility.showInfoSnackbar(activity as AppCompatActivity?, activity.getString(R.string.no_biometric_keys_enrolled), Snackbar.LENGTH_LONG);
            }
            else -> {
                Utility.showInfoSnackbar(activity as AppCompatActivity?, activity.getString(R.string.no_biometric_devices), Snackbar.LENGTH_LONG);
            }
        }
        return isSupported
    }

    internal fun interface BiometricPromptCallback {
        fun execute()
    }
}
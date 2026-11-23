package com.cylonid.nativealpha.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import timber.log.Timber

object IntentUtils {
    @JvmStatic
    fun openExternalUrl(
        context: Context,
        url: String,
    ) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: Exception) {
            Timber.w(e, "Failed to open URL: %s", url)
        }
    }
}

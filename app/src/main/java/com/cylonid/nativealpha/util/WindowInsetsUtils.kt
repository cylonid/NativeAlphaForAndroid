package com.cylonid.nativealpha.util

import android.app.Activity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object WindowInsetsUtils {

    private val INSET_TYPES = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()

    /**
     * Pads the content view by the insets it handles and reports those as zero, so that a WebView
     * further down does not pad them twice. Pass [includeTop] = false to leave the top inset to a
     * fitsSystemWindows app bar drawing behind the status bar.
     */
    @JvmStatic
    @JvmOverloads
    fun applyAsPadding(activity: Activity, includeTop: Boolean = true) {
        apply(activity, INSET_TYPES, includeTop)
    }

    /** For immersive mode, where only the keyboard may overlap the content. */
    @JvmStatic
    fun applyKeyboardPadding(activity: Activity) {
        apply(activity, WindowInsetsCompat.Type.ime(), true)
    }

    private fun apply(activity: Activity, types: Int, includeTop: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(types)
            v.setPadding(insets.left, if (includeTop) insets.top else 0, insets.right, insets.bottom)
            val unhandled = if (includeTop) Insets.NONE else Insets.of(0, insets.top, 0, 0)
            WindowInsetsCompat.Builder(windowInsets).setInsets(types, unhandled).build()
        }
    }
}

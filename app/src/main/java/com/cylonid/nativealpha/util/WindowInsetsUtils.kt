package com.cylonid.nativealpha.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object WindowInsetsUtils {

    private val INSET_TYPES = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()

    private val RGB = Regex("""rgba?\((\d+)[,\s]+(\d+)[,\s]+(\d+)(?:[,/\s]+([\d.]+))?\)""")

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

    @JvmStatic
    fun applyKeyboardPadding(activity: Activity) {
        apply(activity, WindowInsetsCompat.Type.ime(), true)
    }

    /** Paints the area kept free for the system bars in [cssColor] and adapts the icon colour. */
    @JvmStatic
    fun applyBarColor(activity: Activity, cssColor: String?) {
        val color = parseCssColor(cssColor) ?: return
        activity.findViewById<View>(android.R.id.content).setBackgroundColor(color)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
        }
        val light = ColorUtils.calculateLuminance(color) > 0.5
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    private fun parseCssColor(value: String?): Int? {
        val v = value?.trim()?.removeSurrounding("\"")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        RGB.matchEntire(v)?.let { m ->
            if ((m.groupValues[4].toFloatOrNull() ?: 1f) < 0.5f) return null
            return Color.rgb(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        return runCatching { Color.parseColor(v) }.getOrNull()
    }

    private fun apply(activity: Activity, types: Int, includeTop: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(types)
            v.setPadding(insets.left, if (includeTop) insets.top else 0, insets.right, insets.bottom)
            val unhandled = if (includeTop) Insets.NONE else Insets.of(0, insets.top, 0, 0)
            WindowInsetsCompat.Builder(windowInsets).setInsets(types, unhandled).build()
        }
    }
}

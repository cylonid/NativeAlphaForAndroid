package com.cylonid.nativealpha.util

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors

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
    fun applyAsPadding(
        activity: Activity,
        includeTop: Boolean = true,
        types: Int = INSET_TYPES,
    ) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(types)
            v.setPadding(insets.left, if (includeTop) insets.top else 0, insets.right, insets.bottom)
            val unhandled = if (includeTop) Insets.NONE else Insets.of(0, insets.top, 0, 0)
            WindowInsetsCompat.Builder(windowInsets).setInsets(types, unhandled).build()
        }
    }

    @JvmStatic
    fun applyKeyboardPadding(activity: Activity) {
        applyAsPadding(activity, types = WindowInsetsCompat.Type.ime())
    }

    /** Paints each area kept free for the system bars and adapts the icon colour. */
    @JvmStatic
    fun applyBarColors(activity: Activity, topCss: String?, bottomCss: String?) {
        val top = parseCssColor(topCss) ?: parseCssColor(bottomCss) ?: return
        val bottom = parseCssColor(bottomCss) ?: top
        val content = activity.findViewById<View>(android.R.id.content)
        (content.background as? BarBackground)?.setColors(top, bottom)
            ?: run { content.background = BarBackground(top, bottom, content) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = MaterialColors.isColorLight(top)
            isAppearanceLightNavigationBars = MaterialColors.isColorLight(bottom)
        }
    }

    private class BarBackground(
        private var top: Int,
        private var bottom: Int,
        private val host: View,
    ) : Drawable() {
        private val paint = Paint()

        fun setColors(top: Int, bottom: Int) {
            if (top == this.top && bottom == this.bottom) return
            this.top = top
            this.bottom = bottom
            invalidateSelf()
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            paint.color = top
            canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), (b.top + host.paddingTop).toFloat(), paint)
            paint.color = bottom
            canvas.drawRect(b.left.toFloat(), (b.bottom - host.paddingBottom).toFloat(), b.right.toFloat(), b.bottom.toFloat(), paint)
        }

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    private fun parseCssColor(value: String?): Int? {
        val v = value?.trim { it == '"' || it.isWhitespace() }?.takeIf { it.isNotEmpty() } ?: return null
        RGB.matchEntire(v)?.let { m ->
            if ((m.groupValues[4].toFloatOrNull() ?: 1f) < 0.5f) return null
            return Color.rgb(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        return runCatching { Color.parseColor(v) }.getOrNull()
    }
}

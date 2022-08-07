package com.cylonid.nativealpha.helper

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.MenuRes


object IconPopupMenuHelper {
    @JvmStatic
    fun getMenu(v: View, @MenuRes menuRes: Int, c: Context): PopupMenu {
        val popup = PopupMenu(c, v, Gravity.END)
        popup.setForceShowIcon(true)
        popup.menuInflater.inflate(menuRes, popup.menu)

        return popup
    }
}
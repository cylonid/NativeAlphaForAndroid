package com.cylonid.nativealpha.util

import android.content.Context
import android.text.Html.ImageGetter
import android.graphics.drawable.Drawable

class ResourceImageGetter(private val context: Context) : ImageGetter {
    override fun getDrawable(source: String): Drawable {
        val resources = context.resources
        val resId = context.resIdByName(source, "drawable")
        val res = resources.getDrawable(resId)

        res.setBounds(0, 0, res.intrinsicWidth, res.intrinsicHeight)
        return res
    }
}
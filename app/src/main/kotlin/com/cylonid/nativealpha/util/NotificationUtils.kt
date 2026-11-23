package com.cylonid.nativealpha.util

import android.app.Activity
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import com.cylonid.nativealpha.R
import com.google.android.material.snackbar.Snackbar

object NotificationUtils {
    @JvmStatic
    fun showToast(
        a: Activity,
        text: String,
    ) {
        showToast(a, text, Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun showToast(
        a: Activity,
        text: String,
        toastDisplayDuration: Int,
    ) {
        val toast = Toast.makeText(a, text, toastDisplayDuration)
        toast.setGravity(Gravity.TOP, 0, 100)
        toast.show()
    }

    @JvmStatic
    fun showInfoSnackbar(
        activity: Activity,
        msg: String,
        duration: Int,
    ) {
        val snackbar =
            Snackbar.make(
                activity.findViewById(android.R.id.content),
                msg,
                duration,
            )

        snackbar.setAction(
            activity.getString(R.string.ok),
        ) { snackbar.dismiss() }

        val tv =
            snackbar.view.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text,
            )
        tv?.maxLines = 10
        snackbar.show()
    }
}

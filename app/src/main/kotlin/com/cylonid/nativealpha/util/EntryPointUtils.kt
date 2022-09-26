package com.cylonid.nativealpha.util

import android.app.Activity
import com.cylonid.nativealpha.model.DataManager
import android.content.Intent
import com.cylonid.nativealpha.BuildConfig
import com.cylonid.nativealpha.activities.NewsActivity

object EntryPointUtils {
    @JvmStatic
    fun entryPointReached(a: Activity) {
        if (DataManager.getInstance().lastShownUpdate != BuildConfig.VERSION_CODE) {
            a.startActivity(Intent(a, NewsActivity::class.java))
        }
        DataManager.getInstance().loadAppData()
    }
}
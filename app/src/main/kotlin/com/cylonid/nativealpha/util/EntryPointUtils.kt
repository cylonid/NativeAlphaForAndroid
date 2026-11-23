package com.cylonid.nativealpha.util

import com.cylonid.nativealpha.model.DataManager

object EntryPointUtils {
    @JvmStatic
    fun entryPointReached() {
        DataManager.getInstance().loadAppData()
    }
}

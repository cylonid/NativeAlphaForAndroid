package com.cylonid.nativealpha.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.DataManager
import io.github.edsuns.adfilter.AdFilter
import org.json.JSONObject
import timber.log.Timber

class AdblockLifecycleHelper(
    private val activity: Activity,
) {
    fun trySyncOperation(callback: AdblockLifecycleCallback) {
        if (DataManager.getInstance().hasAdblockCrashed) {
            showWarningDialog()
            DataManager.getInstance().hasAdblockCrashed = false
        }
        try {
            callback.execute()
            DataManager.getInstance().hasAdblockCrashed = false
        } catch (t: Throwable) {
            Timber.e(t, "Adblock sync operation failed")
            DataManager.getInstance().hasAdblockCrashed = true
        }
    }

    fun beforeAdblockOperation(callback: AdblockLifecycleCallback) {
        if (DataManager.getInstance().hasAdblockCrashed) {
            showWarningDialog()
            DataManager.getInstance().hasAdblockCrashed = false
        }
        try {
            callback.execute()
        } catch (t: Throwable) {
            Timber.e(t, "Adblock beforeAdblockOperation failed")
            DataManager.getInstance().hasAdblockCrashed = true
        }
    }

    fun afterAdblockOperation() {
        DataManager.getInstance().hasAdblockCrashed = false
    }

    private fun showWarningDialog() {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        try {
            AlertDialog
                .Builder(activity)
                .setMessage(activity.getString(R.string.adblock_warning_text))
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(activity.getString(R.string.warning))
                .setPositiveButton(activity.getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                    DataManager.getInstance().apply {
                        settings.globalWebApp.adBlockSettings.clear()
                        saveGlobalSettings()
                    }
                }.setNegativeButton(activity.getString(R.string.cancel)) { _: DialogInterface?, _: Int -> }
                .create()
                .show()
        } catch (t: Throwable) {
            Timber.e(t, "Error displaying adblock warning dialog")
        }
    }

    fun interface AdblockLifecycleCallback {
        fun execute()
    }

    companion object {
        const val FILTER_PREFS_NAME = "io.github.edsuns.filter"
        const val KEY_FILTER_MAP = "filter_map"
        private val filterMapLock = Any()

        /**
         * Clears any active download states ('ENQUEUED', 'DOWNLOADING', 'INSTALLING')
         * from SharedPreferences before initializing [AdFilter].
         *
         * The underlying [FilterViewModelImpl] executes a synchronous [ListenableFuture.get]
         * on the main thread for any filter where [Filter.downloadState.isRunning] is true.
         * When Android's SystemJobService is simultaneously dispatched on the main thread to run
         * InstallationWorker, this causes an unavoidable main-thread deadlock resulting in an ANR.
         * Sanitizing running states to 'NONE' (if rules already exist) or 'FAILED' prevents
         * the blocking .get() call during constructor initialization.
         */
        @SuppressLint("ApplySharedPref")
        @JvmStatic
        fun sanitizeFilterDownloadStates(context: Context) {
            try {
                synchronized(filterMapLock) {
                    val prefs = context.getSharedPreferences(FILTER_PREFS_NAME, Context.MODE_PRIVATE)
                    val filterMapJson = prefs.getString(KEY_FILTER_MAP, null) ?: return
                    if (!filterMapJson.contains("ENQUEUED") &&
                        !filterMapJson.contains("DOWNLOADING") &&
                        !filterMapJson.contains("INSTALLING")
                    ) {
                        return
                    }

                    val jsonObj = JSONObject(filterMapJson)
                    var modified = false
                    val keys = jsonObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val item = jsonObj.optJSONObject(key) ?: continue
                        val state = item.optString("downloadState", "")
                        if (state == "ENQUEUED" || state == "DOWNLOADING" || state == "INSTALLING") {
                            val count = item.optInt("filtersCount", 0)
                            val newState = if (count > 0) "NONE" else "FAILED"
                            item.put("downloadState", newState)
                            modified = true
                        }
                    }
                    if (modified) {
                        // Synchronous commit is intentional: AdFilter.get() reads filter_map immediately afterward.
                        prefs.edit(commit = true) {
                            putString(KEY_FILTER_MAP, jsonObj.toString())
                        }
                    }
                }
            } catch (t: Throwable) {
                Timber.e(t, "Error sanitizing adblock filter download states")
            }
        }

        /**
         * Safely initializes the [AdFilter] instance on the main thread.
         * Sanitizes any pending download states first to avoid blocking .get() calls in the constructor.
         */
        @JvmStatic
        fun initAdFilter(context: Context): AdFilter {
            sanitizeFilterDownloadStates(context)
            return try {
                AdFilter.get(context)
            } catch (_: Throwable) {
                AdFilter.create(context)
            }
        }
    }
}

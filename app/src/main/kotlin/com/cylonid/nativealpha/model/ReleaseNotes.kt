package com.cylonid.nativealpha.model

import android.content.Context
import com.cylonid.nativealpha.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import timber.log.Timber

data class ReleaseNotes(
    @SerializedName("versionName")
    val versionName: String = "",
    @SerializedName("releaseUrl")
    val releaseUrl: String = "",
    @SerializedName("sections")
    val sections: List<ReleaseNotesSection> = emptyList(),
)

data class ReleaseNotesSection(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("items")
    val items: List<String> = emptyList(),
)

object ReleaseNotesRepository {
    private const val ASSET_FILE = "release_notes.json"
    private val gson = Gson()

    @Volatile
    private var cachedNotes: ReleaseNotes? = null

    fun load(context: Context): ReleaseNotes {
        cachedNotes?.let { return it }
        val loaded =
            try {
                val json =
                    context.assets
                        .open(ASSET_FILE)
                        .bufferedReader()
                        .use { it.readText() }
                gson.fromJson(json, ReleaseNotes::class.java) ?: createFallback()
            } catch (e: Exception) {
                Timber.w(e, "Could not load release notes from assets")
                createFallback()
            }
        cachedNotes = loaded
        return loaded
    }

    private fun createFallback(): ReleaseNotes =
        ReleaseNotes(
            versionName = BuildConfig.VERSION_NAME,
            releaseUrl = "https://github.com/JoestarLabs/NativeAlpha/releases/tag/v${BuildConfig.VERSION_NAME}",
            sections = emptyList(),
        )
}

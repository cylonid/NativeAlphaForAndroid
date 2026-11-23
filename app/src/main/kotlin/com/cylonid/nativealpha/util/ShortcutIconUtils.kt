package com.cylonid.nativealpha.util

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.cylonid.nativealpha.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.math.min

object ShortcutIconUtils {
    private const val ICONS_DIR = "webapp_icons"
    const val MAX_ICON_DIMENSION = 256

    val AVATAR_PALETTE_COLORS =
        intArrayOf(
            0xFF1E88E5.toInt(), // Blue
            0xFF43A047.toInt(), // Green
            0xFFE53935.toInt(), // Red
            0xFFFB8C00.toInt(), // Orange
            0xFF8E24AA.toInt(), // Purple
            0xFF00ACC1.toInt(), // Cyan
            0xFF3949AB.toInt(), // Indigo
            0xFFD81B60.toInt(), // Pink
            0xFF00897B.toInt(), // Teal
        )

    fun avatarColorIndex(label: String): Int {
        val clean = label.trim().ifEmpty { "W" }
        return (clean.hashCode() and Int.MAX_VALUE) % AVATAR_PALETTE_COLORS.size
    }

    // In-memory LRU cache: cache up to 24 icons (~1-2MB RAM maximum)
    private val memoryCache = LruCache<Int, Bitmap>(24)

    private val _iconUpdates = MutableSharedFlow<Int>(extraBufferCapacity = 64)

    @JvmField
    val iconUpdates: SharedFlow<Int> = _iconUpdates.asSharedFlow()

    @JvmStatic
    fun getIconsDir(context: Context): File {
        val dir = File(context.filesDir, ICONS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @JvmStatic
    fun getIconFile(
        context: Context,
        webAppId: Int,
    ): File = File(getIconsDir(context), "$webAppId.png")

    @JvmStatic
    fun downscaleIfNecessary(
        bitmap: Bitmap,
        maxDimension: Int = MAX_ICON_DIMENSION,
    ): Bitmap {
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) {
            return bitmap
        }
        val ratio = min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return bitmap.scale(targetWidth, targetHeight, true)
    }

    @JvmStatic
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = MAX_ICON_DIMENSION,
        reqHeight: Int = MAX_ICON_DIMENSION,
    ): Bitmap? {
        return runCatching {
            val contentResolver = context.contentResolver
            val options =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions =
                BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

            val sampled =
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return null

            downscaleIfNecessary(sampled, maxDimension = min(reqWidth, reqHeight))
        }.getOrNull()
    }

    @JvmStatic
    fun saveIcon(
        context: Context,
        webAppId: Int,
        bitmap: Bitmap,
    ): Boolean {
        if (webAppId < 0) return false
        val downscaled = downscaleIfNecessary(bitmap)
        memoryCache.put(webAppId, downscaled)

        val success =
            runCatching {
                val file = getIconFile(context, webAppId)
                val tmpFile = File(file.parentFile, "$webAppId.tmp")
                FileOutputStream(tmpFile).use { out ->
                    downscaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                if (tmpFile.renameTo(file)) {
                    true
                } else {
                    tmpFile.copyTo(file, overwrite = true)
                    tmpFile.delete()
                    true
                }
            }.getOrDefault(false)

        if (success) {
            _iconUpdates.tryEmit(webAppId)
            updatePinnedShortcuts(context, webAppId, downscaled)
        }
        return success
    }

    @JvmStatic
    fun getIcon(
        context: Context,
        webAppId: Int,
    ): Bitmap? {
        if (webAppId < 0) return null
        memoryCache.get(webAppId)?.let { return it }

        val file = getIconFile(context, webAppId)
        if (!file.exists()) {
            return null
        }
        if (file.length() == 0L) {
            file.delete()
            return null
        }
        val loaded =
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath)
            }.getOrNull()

        return if (loaded != null) {
            val downscaled = downscaleIfNecessary(loaded)
            memoryCache.put(webAppId, downscaled)
            downscaled
        } else {
            // Corrupt file, clean it up
            file.delete()
            null
        }
    }

    @JvmStatic
    fun hasIcon(
        context: Context,
        webAppId: Int,
    ): Boolean {
        if (webAppId < 0) return false
        if (memoryCache.get(webAppId) != null) return true
        val file = getIconFile(context, webAppId)
        return file.exists() && file.length() > 0
    }

    @JvmStatic
    fun deleteIcon(
        context: Context,
        webAppId: Int,
    ): Boolean {
        if (webAppId < 0) return false
        memoryCache.remove(webAppId)
        val success =
            runCatching {
                val file = getIconFile(context, webAppId)
                if (file.exists()) file.delete() else true
            }.getOrDefault(false)
        _iconUpdates.tryEmit(webAppId)
        return success
    }

    @JvmStatic
    fun cleanupOrphanedIcons(
        context: Context,
        validWebAppIds: Collection<Int>,
    ) {
        runCatching {
            val validIdStrings = validWebAppIds.filter { it >= 0 }.map { "$it.png" }.toSet()
            val dir = getIconsDir(context)
            dir.listFiles()?.forEach { file ->
                if (file.isFile && (!validIdStrings.contains(file.name) || file.name.endsWith(".tmp"))) {
                    file.delete()
                    val id = file.name.substringBefore(".").toIntOrNull()
                    if (id != null) {
                        memoryCache.remove(id)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    @JvmStatic
    fun deleteShortcuts(
        removableWebAppIds: List<Int>,
        context: Context,
    ) {
        if (removableWebAppIds.isEmpty()) return
        val appContext = context.applicationContext ?: context
        for (id in removableWebAppIds) {
            deleteIcon(appContext, id)
        }
        val manager = appContext.getSystemService(ShortcutManager::class.java) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val toDisable = mutableListOf<String>()
                for (info in manager.pinnedShortcuts) {
                    val id =
                        info.intent?.getIntExtra(Const.INTENT_WEBAPPID, -1) ?: -1
                    if (removableWebAppIds.contains(id)) {
                        toDisable.add(info.id)
                    }
                }
                if (toDisable.isNotEmpty()) {
                    manager.disableShortcuts(
                        toDisable,
                        appContext.getString(R.string.webapp_already_deleted),
                    )
                }
            }
        }
    }

    @JvmStatic
    fun updatePinnedShortcuts(
        context: Context,
        webAppId: Int,
        bitmap: Bitmap,
    ) {
        if (webAppId < 0) return
        val appContext = context.applicationContext ?: context
        val downscaled = downscaleIfNecessary(bitmap)
        val scManager = appContext.getSystemService(ShortcutManager::class.java) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val matching =
                    scManager.pinnedShortcuts.filter { info ->
                        info.intent?.getIntExtra(Const.INTENT_WEBAPPID, -1) == webAppId
                    }
                if (matching.isNotEmpty()) {
                    val updates =
                        matching.map { info ->
                            val builder =
                                ShortcutInfo
                                    .Builder(appContext, info.id)
                                    .setIcon(Icon.createWithBitmap(downscaled))
                            info.shortLabel?.let { builder.setShortLabel(it) }
                            info.longLabel?.let { builder.setLongLabel(it) }
                            info.intent?.let { builder.setIntent(it) }
                            builder.build()
                        }
                    scManager.updateShortcuts(updates)
                }
            }
        }
    }

    @JvmStatic
    fun getWidthFromIcon(sizeString: String): Int {
        var xIndex = sizeString.indexOf("x")
        if (xIndex == -1) xIndex = sizeString.indexOf("×")
        if (xIndex == -1) xIndex = sizeString.indexOf("*")

        if (xIndex == -1) return 1
        val width = sizeString.substring(0, xIndex)

        return width.toIntOrNull() ?: 1
    }

    @JvmStatic
    fun getFallbackIconUrls(baseUrl: String): List<String> {
        val host =
            runCatching {
                val uri =
                    URI(
                        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                            "https://$baseUrl"
                        } else {
                            baseUrl
                        },
                    )
                uri.host?.removePrefix("www.")
            }.getOrNull() ?: return emptyList()

        if (host.isBlank()) return emptyList()

        return listOf(
            "https://$host/favicon.ico",
            "https://icons.duckduckgo.com/ip3/$host.ico",
            "https://www.google.com/s2/favicons?domain=$host&sz=128",
        )
    }

    @JvmStatic
    fun createMonogramIcon(
        label: String,
        sizePx: Int = 192,
    ): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cleanLabel = label.trim().ifEmpty { "W" }
        val bgColor = AVATAR_PALETTE_COLORS[avatarColorIndex(cleanLabel)]

        val bgPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgColor
                style = Paint.Style.FILL
            }

        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)

        val initial = cleanLabel.first().uppercaseChar().toString()
        val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = sizePx * 0.48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

        val bounds = Rect()
        textPaint.getTextBounds(initial, 0, initial.length, bounds)
        val yOffset = bounds.exactCenterY()
        canvas.drawText(initial, radius, radius - yOffset, textPaint)

        return bitmap
    }

    @JvmStatic
    fun fetchBitmapFromNetwork(strUrl: String): Bitmap? {
        if (strUrl.isBlank()) return null
        var con: HttpURLConnection? = null
        return try {
            val url = URL(strUrl)
            con =
                (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = true
                    setRequestProperty(
                        "User-Agent",
                        Const.DESKTOP_USER_AGENT,
                    )
                }
            con.inputStream
                .use { stream ->
                    BitmapFactory.decodeStream(stream)
                }?.let { loaded ->
                    val downscaled = downscaleIfNecessary(loaded, MAX_ICON_DIMENSION)
                    if (downscaled.width >= Const.FAVICON_MIN_WIDTH) downscaled else null
                }
        } catch (_: Exception) {
            null
        } finally {
            con?.disconnect()
        }
    }

    @JvmStatic
    fun autoFetchAndSaveFavicon(
        context: Context,
        webAppId: Int,
        baseUrl: String,
    ): Bitmap? {
        if (hasIcon(context, webAppId)) return getIcon(context, webAppId)
        val candidates = getFallbackIconUrls(baseUrl)
        for (url in candidates) {
            val bitmap = fetchBitmapFromNetwork(url)
            if (bitmap != null) {
                saveIcon(context, webAppId, bitmap)
                return bitmap
            }
        }
        return null
    }
}

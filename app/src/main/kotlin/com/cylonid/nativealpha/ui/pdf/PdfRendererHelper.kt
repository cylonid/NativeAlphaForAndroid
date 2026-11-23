package com.cylonid.nativealpha.ui.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

interface PdfRendererEngine : AutoCloseable {
    val pageCount: Int

    suspend fun getPageDimensions(pageIndex: Int): Pair<Int, Int>

    suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
    ): Bitmap?
}

open class PdfRendererHelper(
    val file: File,
) : PdfRendererEngine {
    private val parcelFileDescriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer =
        try {
            PdfRenderer(parcelFileDescriptor)
        } catch (t: Throwable) {
            try {
                parcelFileDescriptor.close()
            } catch (_: Exception) {
            }
            throw t
        }

    @Volatile
    private var isClosed = false

    private val lifecycleLock = Any()

    override val pageCount: Int
        get() =
            synchronized(lifecycleLock) {
                if (isClosed) {
                    0
                } else {
                    try {
                        renderer.pageCount
                    } catch (_: Exception) {
                        0
                    }
                }
            }

    private val mutex = Mutex()
    private val closeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dimensionCache = mutableMapOf<Int, Pair<Int, Int>>()

    private val maxCacheMemory =
        (Runtime.getRuntime().maxMemory() / 8L)
            .coerceIn(16L * 1024 * 1024, 48L * 1024 * 1024)
            .toInt()

    private val bitmapCache =
        object : LruCache<Int, Bitmap>(maxCacheMemory) {
            override fun sizeOf(
                key: Int,
                value: Bitmap,
            ): Int = value.byteCount
        }

    override suspend fun getPageDimensions(pageIndex: Int): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            if (pageIndex < 0 || pageIndex >= pageCount) {
                return@withContext Pair(1, 1)
            }
            mutex.withLock {
                if (isClosed) return@withLock Pair(1, 1)
                dimensionCache[pageIndex]?.let { return@withLock it }
                val page = renderer.openPage(pageIndex)
                val dims =
                    try {
                        Pair(page.width, page.height)
                    } finally {
                        page.close()
                    }
                dimensionCache[pageIndex] = dims
                dims
            }
        }

    override suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            if (pageIndex < 0 || pageIndex >= pageCount || targetWidth <= 0) {
                return@withContext null
            }

            bitmapCache.get(pageIndex)?.let { cached ->
                if (!cached.isRecycled) {
                    return@withContext cached
                }
            }

            mutex.withLock {
                if (isClosed) return@withLock null
                bitmapCache.get(pageIndex)?.let { cached ->
                    if (!cached.isRecycled) {
                        return@withLock cached
                    }
                }

                val page = renderer.openPage(pageIndex)
                try {
                    val pageW = page.width.coerceAtLeast(1)
                    val pageH = page.height.coerceAtLeast(1)
                    var renderWidth = targetWidth
                    val initialScaledHeight = (renderWidth.toDouble() / pageW) * pageH
                    if (initialScaledHeight.isNaN() || initialScaledHeight.isInfinite() || initialScaledHeight <= 0.0) {
                        return@withLock null
                    }

                    val maxPixels = MAX_BITMAP_BYTES / BYTES_PER_PIXEL
                    val maxAllowedWidth = kotlin.math.sqrt(maxPixels.toDouble() * pageW / pageH)
                    if (renderWidth.toDouble() > maxAllowedWidth) {
                        renderWidth = kotlin.math.floor(maxAllowedWidth).toInt()
                    }

                    if (renderWidth < 1) {
                        return@withLock null
                    }

                    var scaledHeight = (renderWidth.toDouble() / pageW) * pageH
                    if (scaledHeight > Int.MAX_VALUE || scaledHeight.isNaN() || scaledHeight.isInfinite()) {
                        return@withLock null
                    }
                    var targetHeightLong =
                        kotlin.math
                            .round(scaledHeight)
                            .toLong()
                            .coerceAtLeast(1L)

                    val maxAllowedWidthForHeight = MAX_BITMAP_BYTES / (targetHeightLong * BYTES_PER_PIXEL)
                    if (renderWidth.toLong() > maxAllowedWidthForHeight) {
                        renderWidth = maxAllowedWidthForHeight.toInt()
                        if (renderWidth < 1) {
                            return@withLock null
                        }
                        scaledHeight = (renderWidth.toDouble() / pageW) * pageH
                        targetHeightLong =
                            kotlin.math
                                .round(scaledHeight)
                                .toLong()
                                .coerceAtLeast(1L)
                    }

                    if (targetHeightLong > Int.MAX_VALUE ||
                        targetHeightLong > MAX_BITMAP_BYTES / (renderWidth.toLong() * BYTES_PER_PIXEL)
                    ) {
                        return@withLock null
                    }
                    val targetHeight = targetHeightLong.toInt()
                    val requiredBytes = renderWidth.toLong() * targetHeightLong * BYTES_PER_PIXEL
                    if (requiredBytes > MAX_BITMAP_BYTES) {
                        return@withLock null
                    }

                    val bitmap = createBitmap(renderWidth, targetHeight)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmapCache.put(pageIndex, bitmap)
                    bitmap
                } finally {
                    page.close()
                }
            }
        }

    override fun close() {
        synchronized(lifecycleLock) {
            if (isClosed) return
            isClosed = true
            try {
                bitmapCache.evictAll()
            } catch (_: Exception) {
            }

            closeScope.launch {
                try {
                    mutex.withLock {
                        try {
                            renderer.close()
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                    try {
                        renderer.close()
                    } catch (_: Exception) {
                    }
                } finally {
                    try {
                        parcelFileDescriptor.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    companion object {
        const val BYTES_PER_PIXEL = 4
        const val MAX_BITMAP_BYTES = 32L * 1024 * 1024 // 32 MB
    }
}

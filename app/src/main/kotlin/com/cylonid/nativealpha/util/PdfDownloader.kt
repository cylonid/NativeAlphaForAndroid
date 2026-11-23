package com.cylonid.nativealpha.util

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object PdfDownloader {
    private const val MAX_REDIRECTS = 5
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 30000
    private const val MAX_CACHE_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB
    private const val CACHE_SUBDIR = "pdf_cache"

    fun getPdfCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun cleanOldCacheFiles(context: Context) {
        try {
            val dir = getPdfCacheDir(context)
            val files = dir.listFiles()?.filter { it.isFile } ?: return
            var currentTotalSize = files.sumOf { it.length() }
            if (currentTotalSize > MAX_CACHE_SIZE_BYTES) {
                // Delete oldest files first until total size is <= MAX_CACHE_SIZE_BYTES
                val sortedFiles = files.sortedBy { it.lastModified() }
                for (file in sortedFiles) {
                    if (currentTotalSize <= MAX_CACHE_SIZE_BYTES) break
                    val len = file.length()
                    if (file.delete()) {
                        currentTotalSize -= len
                    }
                }
            }
        } catch (_: Exception) {
            // Best effort cache trimming
        }
    }

    suspend fun downloadPdf(
        context: Context,
        urlStr: String,
        preferredFileName: String? = null,
        userAgent: String? = null,
        cookieHeader: String? = null,
        allowHttp: Boolean = true,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null,
    ): Result<File> =
        withContext(Dispatchers.IO) {
            cleanOldCacheFiles(context)

            if (!allowHttp && urlStr.startsWith("http://", ignoreCase = true)) {
                return@withContext Result.failure(IOException("Insecure HTTP disallowed by Web App policy"))
            }

            // Handle local content / file URIs directly
            if (urlStr.startsWith("content://") || urlStr.startsWith("file://")) {
                return@withContext copyLocalUriToCache(context, urlStr.toUri(), preferredFileName)
            }

            val cacheDir = getPdfCacheDir(context)
            val safeFileName = sanitizeFileName(preferredFileName ?: deriveFileNameFromUrl(urlStr))
            val targetFile = File(cacheDir, "${System.currentTimeMillis()}_$safeFileName")
            val tempFile = File(cacheDir, "${targetFile.name}.tmp")

            var connection: HttpURLConnection? = null
            var currentUrl = urlStr
            var redirectCount = 0

            try {
                while (redirectCount < MAX_REDIRECTS) {
                    val url = URL(currentUrl)
                    connection =
                        (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = CONNECT_TIMEOUT_MS
                            readTimeout = READ_TIMEOUT_MS
                            instanceFollowRedirects = false
                            requestMethod = "GET"

                            if (!userAgent.isNullOrBlank()) {
                                setRequestProperty("User-Agent", userAgent)
                            }

                            val cookie =
                                if (isSameOrigin(urlStr, currentUrl)) {
                                    if (!cookieHeader.isNullOrBlank()) {
                                        cookieHeader
                                    } else {
                                        CookieManager.getInstance().getCookie(currentUrl)
                                    }
                                } else {
                                    null
                                }
                            if (!cookie.isNullOrBlank()) {
                                setRequestProperty("Cookie", cookie)
                            }
                            setRequestProperty("Accept", "application/pdf,*/*")
                        }

                    val responseCode = connection.responseCode
                    if (responseCode in 300..399) {
                        val location = connection.getHeaderField("Location")
                        if (location.isNullOrBlank()) {
                            throw IOException("Redirect without Location header (HTTP $responseCode)")
                        }
                        currentUrl =
                            if (location.startsWith("http://") || location.startsWith("https://")) {
                                location
                            } else {
                                URL(URL(currentUrl), location).toString()
                            }
                        if (!allowHttp && currentUrl.startsWith("http://", ignoreCase = true)) {
                            connection.disconnect()
                            throw IOException("Redirect to insecure HTTP disallowed by Web App policy: $currentUrl")
                        }
                        connection.disconnect()
                        redirectCount++
                    } else if (responseCode in 200..299) {
                        break
                    } else {
                        throw IOException("HTTP error response code: $responseCode")
                    }
                }

                if (redirectCount >= MAX_REDIRECTS) {
                    throw IOException("Too many redirects: $redirectCount")
                }

                val conn = connection ?: throw IOException("Failed to establish connection")
                val totalBytes = conn.contentLengthLong

                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        copyStreamWithProgress(input, output, totalBytes, onProgress)
                    }
                }

                if (tempFile.renameTo(targetFile)) {
                    Result.success(targetFile)
                } else {
                    // Fallback copy if renameTo fails
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    Result.success(targetFile)
                }
            } catch (e: Exception) {
                tempFile.delete()
                targetFile.delete()
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }

    private const val MAX_PDF_BYTES = 100L * 1024 * 1024 // 100 MB

    private fun copyStreamWithProgress(
        input: InputStream,
        output: FileOutputStream,
        totalBytes: Long,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)?,
    ) {
        val buffer = ByteArray(8192)
        var bytesDownloaded = 0L
        var bytesRead: Int

        while (input.read(buffer).also { bytesRead = it } != -1) {
            bytesDownloaded += bytesRead
            if (bytesDownloaded > MAX_PDF_BYTES) {
                throw IOException("PDF exceeds maximum allowed size of $MAX_PDF_BYTES bytes")
            }
            output.write(buffer, 0, bytesRead)
            onProgress?.invoke(bytesDownloaded, totalBytes)
        }
        output.flush()
    }

    private fun copyLocalUriToCache(
        context: Context,
        uri: Uri,
        fileName: String?,
    ): Result<File> {
        val cacheDir = getPdfCacheDir(context)
        val name = sanitizeFileName(fileName ?: "document.pdf")
        val targetFile = File(cacheDir, "${System.currentTimeMillis()}_$name")
        val tempFile = File(cacheDir, "${targetFile.name}.tmp")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    copyStreamWithProgress(input, output, -1L, null)
                }
            } ?: return Result.failure(IOException("Could not open input stream for URI: $uri"))

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            tempFile.delete()
            targetFile.delete()
            Result.failure(e)
        }
    }

    fun isSameOrigin(
        urlStr1: String,
        urlStr2: String,
    ): Boolean =
        try {
            val u1 = URL(urlStr1)
            val u2 = URL(urlStr2)
            val scheme1 = u1.protocol.lowercase(Locale.ROOT)
            val scheme2 = u2.protocol.lowercase(Locale.ROOT)
            val host1 = u1.host.lowercase(Locale.ROOT)
            val host2 = u2.host.lowercase(Locale.ROOT)
            val port1 = if (u1.port != -1) u1.port else u1.defaultPort
            val port2 = if (u2.port != -1) u2.port else u2.defaultPort
            scheme1 == scheme2 && host1 == host2 && port1 == port2
        } catch (_: Exception) {
            false
        }

    fun deriveFileNameFromUrl(url: String): String =
        try {
            val clean = url.substringBefore('?').substringBefore('#')
            val lastSegment = clean.substringAfterLast('/')
            if (lastSegment.isNotBlank() && lastSegment.contains('.')) {
                lastSegment
            } else {
                "document.pdf"
            }
        } catch (_: Exception) {
            "document.pdf"
        }

    fun sanitizeFileName(name: String): String {
        val sanitized = name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return if (sanitized.lowercase(Locale.ROOT).endsWith(".pdf")) {
            sanitized
        } else {
            "$sanitized.pdf"
        }
    }
}

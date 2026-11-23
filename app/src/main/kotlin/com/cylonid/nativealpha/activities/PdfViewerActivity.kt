package com.cylonid.nativealpha.activities

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.ui.pdf.PdfRendererHelper
import com.cylonid.nativealpha.ui.pdf.PdfViewerScreen
import com.cylonid.nativealpha.ui.pdf.PdfViewerUiState
import com.cylonid.nativealpha.ui.theme.NativeAlphaTheme
import com.cylonid.nativealpha.util.PdfDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfViewerActivity : AppCompatActivity() {
    private var currentHelper: PdfRendererHelper? = null
    private var currentPdfFile: File? = null
    private var loadJob: Job? = null

    companion object {
        const val EXTRA_PDF_URL = "EXTRA_PDF_URL"
        const val EXTRA_FILE_NAME = "EXTRA_FILE_NAME"
        const val EXTRA_USER_AGENT = "EXTRA_USER_AGENT"
        const val EXTRA_WEBAPP_ID = "EXTRA_WEBAPP_ID"
        const val EXTRA_COOKIE = "EXTRA_COOKIE"
        const val EXTRA_ALLOW_HTTP = "EXTRA_ALLOW_HTTP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = intent.getStringExtra(EXTRA_PDF_URL) ?: ""
        val fileName =
            intent.getStringExtra(EXTRA_FILE_NAME)
                ?: PdfDownloader.deriveFileNameFromUrl(url)
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT)

        setContent {
            NativeAlphaTheme {
                var uiState by remember { mutableStateOf<PdfViewerUiState>(PdfViewerUiState.Loading) }
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                fun loadPdf() {
                    loadJob?.cancel()
                    uiState = PdfViewerUiState.Loading
                    currentHelper?.close()
                    currentHelper = null

                    loadJob =
                        lifecycleScope.launch {
                            val cookie = intent.getStringExtra(EXTRA_COOKIE)
                            val allowHttp = intent.getBooleanExtra(EXTRA_ALLOW_HTTP, false)
                            val result =
                                PdfDownloader.downloadPdf(
                                    context = this@PdfViewerActivity,
                                    urlStr = url,
                                    preferredFileName = fileName,
                                    userAgent = userAgent,
                                    cookieHeader = cookie,
                                    allowHttp = allowHttp,
                                )

                            if (!isActive || isDestroyed) return@launch

                            result
                                .onSuccess { file ->
                                    try {
                                        val helper = withContext(Dispatchers.IO) { PdfRendererHelper(file) }
                                        if (!isActive || isDestroyed) {
                                            helper.close()
                                            return@launch
                                        }
                                        currentHelper = helper
                                        currentPdfFile = file
                                        uiState = PdfViewerUiState.Ready(file, helper)
                                    } catch (e: Exception) {
                                        uiState =
                                            PdfViewerUiState.Error(
                                                e.localizedMessage ?: getString(R.string.pdf_viewer_error_loading),
                                            )
                                    }
                                }.onFailure { error ->
                                    uiState =
                                        PdfViewerUiState.Error(
                                            error.localizedMessage ?: getString(R.string.pdf_viewer_error_loading),
                                        )
                                }
                        }
                }

                // Initial load
                androidx.compose.runtime.LaunchedEffect(url) {
                    loadPdf()
                }

                PdfViewerScreen(
                    fileName = fileName,
                    uiState = uiState,
                    onNavigateBack = { finish() },
                    onSaveToDownloads = {
                        coroutineScope.launch {
                            currentPdfFile?.let { file ->
                                val success = savePdfToDownloads(file, fileName)
                                val msg =
                                    if (success) {
                                        getString(R.string.pdf_viewer_saved_success)
                                    } else {
                                        getString(R.string.pdf_viewer_save_failed)
                                    }
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    onShare = {
                        currentPdfFile?.let { file ->
                            sharePdf(file)
                        }
                    },
                    onOpenWith = {
                        currentPdfFile?.let { file ->
                            openPdfWithExternalApp(file)
                        }
                    },
                    onRetry = {
                        loadPdf()
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }

    private suspend fun savePdfToDownloads(
        sourceFile: File,
        targetName: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val sanitizedName = PdfDownloader.sanitizeFileName(targetName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = contentResolver
                    val contentValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    val uri =
                        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            ?: return@withContext false

                    var copySucceeded = false
                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            FileInputStream(sourceFile).use { input ->
                                input.copyTo(out)
                            }
                            copySucceeded = true
                        }
                    } finally {
                        if (!copySucceeded) {
                            try {
                                resolver.delete(uri, null, null)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    if (!copySucceeded) return@withContext false

                    val publishValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                    resolver.update(uri, publishValues, null, null)
                    true
                } else {
                    val perms = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    if (!pub.devrel.easypermissions.EasyPermissions
                            .hasPermissions(this@PdfViewerActivity, *perms)
                    ) {
                        return@withContext false
                    }

                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    var destFile = File(downloadsDir, sanitizedName)
                    var counter = 1
                    val nameWithoutExt = sanitizedName.substringBeforeLast(".pdf")
                    while (destFile.exists()) {
                        destFile = File(downloadsDir, "$nameWithoutExt ($counter).pdf")
                        counter++
                    }

                    val tempFile = File(downloadsDir, "${destFile.name}.${System.currentTimeMillis()}.tmp")
                    try {
                        FileInputStream(sourceFile).use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (!tempFile.renameTo(destFile)) {
                            tempFile.delete()
                            return@withContext false
                        }
                        true
                    } catch (e: Exception) {
                        tempFile.delete()
                        false
                    }
                }
            } catch (_: Exception) {
                false
            }
        }

    private fun sharePdf(file: File) {
        try {
            val uri: Uri =
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file,
                )
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            startActivity(Intent.createChooser(intent, getString(R.string.pdf_viewer_share)))
        } catch (e: Exception) {
            Timber.w(e, "Failed to share PDF")
        }
    }

    private fun openPdfWithExternalApp(file: File) {
        try {
            val uri: Uri =
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            startActivity(Intent.createChooser(intent, getString(R.string.pdf_viewer_open_external)))
        } catch (e: Exception) {
            Timber.w(e, "Failed to open PDF with external app")
        }
    }

    override fun onDestroy() {
        loadJob?.cancel()
        loadJob = null
        currentHelper?.close()
        currentHelper = null
        super.onDestroy()
    }
}

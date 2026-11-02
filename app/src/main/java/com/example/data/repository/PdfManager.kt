package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.example.data.local.PyqDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class PdfManager(private val context: Context) {

    private val db = PyqDatabase.getInstance(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadsDir = File(context.filesDir, "downloaded_papers").apply {
        if (!exists()) mkdirs()
    }

    private val cacheDir = File(context.cacheDir, "pdf_cache").apply {
        if (!exists()) mkdirs()
    }

    fun extractDriveFileId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()

        val patternFileD = Pattern.compile("/file/d/([a-zA-Z0-9_-]+)")
        val matcherFileD = patternFileD.matcher(trimmed)
        if (matcherFileD.find()) return matcherFileD.group(1)

        val patternIdParam = Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)")
        val matcherId = patternIdParam.matcher(trimmed)
        if (matcherId.find()) return matcherId.group(1)

        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{15,}$"))) {
            return trimmed
        }

        return null
    }

    fun buildDriveDownloadUrl(fileId: String): String {
        return "https://drive.google.com/uc?export=download&id=$fileId"
    }

    fun buildDriveViewUrl(fileId: String): String {
        return "https://drive.google.com/file/d/$fileId/view?usp=sharing"
    }

    fun getLocalFile(paperId: String): File? {
        val permFile = File(downloadsDir, "$paperId.pdf")
        if (permFile.exists() && permFile.length() > 0) return permFile
        val tempFile = File(cacheDir, "$paperId.pdf")
        if (tempFile.exists() && tempFile.length() > 0) return tempFile
        return null
    }

    suspend fun downloadPdf(
        paperId: String,
        rawUrl: String,
        isPermanentDownload: Boolean = false,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val existing = getLocalFile(paperId)
            if (existing != null && existing.length() > 1024) {
                if (isPermanentDownload && existing.parentFile != downloadsDir) {
                    val dest = File(downloadsDir, "$paperId.pdf")
                    existing.copyTo(dest, overwrite = true)
                    db.paperDao().updateDownloadStatus(
                        paperId,
                        dest.absolutePath,
                        dest.length(),
                        System.currentTimeMillis()
                    )
                    return@withContext Result.success(dest)
                }
                return@withContext Result.success(existing)
            }

            val fileId = extractDriveFileId(rawUrl)
            val downloadUrl = if (fileId != null) {
                buildDriveDownloadUrl(fileId)
            } else {
                rawUrl
            }

            val targetDir = if (isPermanentDownload) downloadsDir else cacheDir
            val targetFile = File(targetDir, "$paperId.pdf")

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (targetFile.length() < 100) {
                targetFile.delete()
                return@withContext Result.failure(Exception("Downloaded file is empty or invalid."))
            }

            if (isPermanentDownload) {
                db.paperDao().updateDownloadStatus(
                    paperId,
                    targetFile.absolutePath,
                    targetFile.length(),
                    System.currentTimeMillis()
                )
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDownload(paperId: String) = withContext(Dispatchers.IO) {
        val perm = File(downloadsDir, "$paperId.pdf")
        if (perm.exists()) perm.delete()
        val cache = File(cacheDir, "$paperId.pdf")
        if (cache.exists()) cache.delete()
        db.paperDao().clearDownload(paperId)
    }

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        downloadsDir.listFiles()?.forEach { it.delete() }
        db.paperDao().clearAllDownloads()
    }

    fun getUsedStorageBytes(): Long {
        var total = 0L
        downloadsDir.listFiles()?.forEach { total += it.length() }
        cacheDir.listFiles()?.forEach { total += it.length() }
        return total
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }

    fun openWithExternalApp(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun renderPdfPage(file: File, pageIndex: Int, screenWidthPx: Int): Bitmap? {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return null
            }

            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetWidth = screenWidthPx.coerceAtLeast(400)
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(400)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPageCount(file: File): Int {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            1
        }
    }
}

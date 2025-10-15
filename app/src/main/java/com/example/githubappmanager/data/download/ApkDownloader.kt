package com.example.githubappmanager.data.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val error: String? = null
)

class ApkDownloader(private val context: Context) {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    fun downloadApk(downloadUrl: String, fileName: String): Flow<DownloadProgress> = flow {
        try {
            val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                emit(DownloadProgress(0, 0, error = "Download failed: ${response.code}"))
                return@flow
            }
            
            val body = response.body ?: run {
                emit(DownloadProgress(0, 0, error = "Empty response body"))
                return@flow
            }
            
            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(file)
            
            val buffer = ByteArray(8192)
            var bytesDownloaded = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                
                emit(DownloadProgress(bytesDownloaded, totalBytes))
            }
            
            outputStream.close()
            inputStream.close()
            
            emit(DownloadProgress(bytesDownloaded, totalBytes, isComplete = true))
        } catch (e: Exception) {
            emit(DownloadProgress(0, 0, error = e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun getDownloadedApk(fileName: String): File? {
        val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
        val file = File(downloadsDir, fileName)
        return if (file.exists()) file else null
    }
}

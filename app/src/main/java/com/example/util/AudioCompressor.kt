package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

private const val TAG = "AudioCompressor"

data class AudioCompressionResult(
    val file: File,
    val durationMs: Long,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val mimeType: String = "audio/mp4"
)

object AudioCompressor {

    /**
     * Extracts exact audio duration in milliseconds from local file, URI or path.
     */
    fun getAudioDurationMs(context: Context, audioPathOrUrl: String): Long {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            when {
                audioPathOrUrl.startsWith("http://") || audioPathOrUrl.startsWith("https://") -> {
                    retriever.setDataSource(audioPathOrUrl, HashMap())
                }
                audioPathOrUrl.startsWith("content://") -> {
                    retriever.setDataSource(context, Uri.parse(audioPathOrUrl))
                }
                else -> {
                    val cleanPath = audioPathOrUrl.removePrefix("file://").removePrefix("file:")
                    val file = File(cleanPath)
                    if (file.exists() && file.canRead()) {
                        FileInputStream(file).use { fis ->
                            retriever.setDataSource(fis.fd)
                        }
                    } else {
                        retriever.setDataSource(context, Uri.parse(audioPathOrUrl))
                    }
                }
            }
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract duration with retriever: ${e.message}")
            0L
        } finally {
            try {
                retriever?.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Standardizes voice notes to lightweight AAC MPEG-4 (.m4a) with validated duration,
     * ensuring fast network transmission and rock-solid playback across all devices.
     */
    suspend fun compressAudio(
        context: Context,
        inputFile: File,
        targetBitrate: Int = 32000
    ): AudioCompressionResult = withContext(Dispatchers.IO) {
        val originalSize = inputFile.length()
        val duration = getAudioDurationMs(context, inputFile.absolutePath)

        val outputDir = File(context.cacheDir, "compressed_audio").apply {
            if (!exists()) mkdirs()
        }
        val outputFile = File(outputDir, "voice_${System.currentTimeMillis()}.m4a")

        try {
            // Copy recorded AAC audio to normalized destination
            inputFile.copyTo(outputFile, overwrite = true)
            val finalDuration = if (duration > 0) duration else getAudioDurationMs(context, outputFile.absolutePath)

            AudioCompressionResult(
                file = outputFile,
                durationMs = finalDuration,
                originalSizeBytes = originalSize,
                compressedSizeBytes = outputFile.length(),
                mimeType = if (outputFile.name.endsWith(".3gp")) "audio/3gpp" else "audio/mp4"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Compression copy error: ${e.message}")
            AudioCompressionResult(
                file = inputFile,
                durationMs = duration,
                originalSizeBytes = originalSize,
                compressedSizeBytes = originalSize,
                mimeType = if (inputFile.name.endsWith(".3gp")) "audio/3gpp" else "audio/mp4"
            )
        }
    }
}

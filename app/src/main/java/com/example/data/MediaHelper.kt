package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

object MediaHelper {

    const val MAX_INLINE_BYTES: Long = 16L * 1024L * 1024L // 16 MB safety margin under 20 MB limit

    data class UriMetadata(
        val displayName: String,
        val fileSizeBytes: Long,
        val mimeType: String
    )

    fun queryUriMetadata(context: Context, uri: Uri, fallbackMime: String): UriMetadata {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "clinical_media"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) {
                    cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    size = cursor.getLong(sizeIdx)
                }
            }
        }
        val mime = context.contentResolver.getType(uri) ?: fallbackMime
        return UriMetadata(displayName = name, fileSizeBytes = size, mimeType = mime)
    }

    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computeSha256(text: String): String = computeSha256(text.toByteArray(Charsets.UTF_8))

    fun computeSha256FromUri(context: Context, uri: Uri): String {
        return runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("unavailable")
    }

    fun computeSha256FromFile(file: File): String {
        return runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("unavailable")
    }

    fun readBytesFromUri(context: Context, uri: Uri, maxBytes: Long = MAX_INLINE_BYTES): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    total += read
                    if (total > maxBytes) return null
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
        }.getOrNull()
    }

    fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 1440): Bitmap? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            val width = boundsOpts.outWidth
            val height = boundsOpts.outHeight
            var inSampleSize = 1
            if (width > maxDimension || height > maxDimension) {
                val longest = max(width, height)
                while (longest / inSampleSize > maxDimension) {
                    inSampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null
            scaleBitmapIfNeeded(decoded, maxDimension)
        }.getOrNull()
    }

    fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = 1440): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 90): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    data class VideoInfo(
        val durationMillis: Long,
        val width: Int,
        val height: Int,
        val thumbnail: Bitmap?
    )

    fun extractVideoInfo(context: Context, uri: Uri): VideoInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val thumb = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.let { scaleBitmapIfNeeded(it, 720) }
            VideoInfo(duration, width, height, thumb)
        } catch (_: Exception) {
            VideoInfo(0L, 0, 0, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun extractAudioDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun extractVideoKeyFrames(context: Context, uri: Uri, frameCount: Int = 6): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { frames.add(scaleBitmapIfNeeded(it, 1024)) }
            } else {
                val count = frameCount.coerceIn(1, 8)
                for (i in 0 until count) {
                    val timeUs = ((durationMs * 1000L) * (i + 0.5) / count).toLong()
                    val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frame != null) {
                        frames.add(scaleBitmapIfNeeded(frame, 1024))
                    }
                }
            }
            frames
        } catch (_: Exception) {
            frames
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "%.2f MB".format(mb)
            kb >= 1.0 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0L) return "00:00"
        val totalSeconds = (millis / 1000L).toInt()
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun createAudioRecorder(context: Context, outputFile: File): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }
    }
}

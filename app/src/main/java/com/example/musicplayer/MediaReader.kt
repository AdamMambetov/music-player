package com.example.musicplayer

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File

class MediaReader(val context: Context) {
    data class AudioInfo(val uri: String, val durationMs: Long)

    fun scanAudio(uri: Uri): Map<String, AudioInfo> {
        if (uri.toString().isEmpty())
            return emptyMap()

        val queryUri = MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL,
        )
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
        )
        val selectionPath = uri.lastPathSegment!!.substringAfter(delimiter = ":") + "/"
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(
            selectionPath,
        )

        val result = mutableMapOf<String, AudioInfo>()
        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val fileName = cursor.getString(displayNameColumn)
                val durationMs = cursor.getLong(durationColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
                result.put(fileName, AudioInfo(contentUri.toString(), durationMs))
            }
        }
        return result
    }

    fun scanCovers(uri: Uri): Map<String, String> {
        if (uri.toString().isEmpty())
            return emptyMap()

        val coversDir = getPathFromUri(uri)
        val dir = File(coversDir)
        if (!dir.exists() || !dir.isDirectory)
            return emptyMap()

        val result = mutableMapOf<String, String>()
        dir.listFiles()?.filter {
            it.isFile && it.extension.lowercase() in listOf(
                "jpg",
                "jpeg",
                "png",
                "webp"
            )
        }?.forEach { file ->
            val fileUri = Uri.fromFile(file)
            result[file.name] = fileUri.toString()
        }
        return result
    }

    /**
     * Строит маппинг MD5 хеш -> имя файла для существующих обложек.
     */
    private fun buildHashIndex(coversDir: File): MutableMap<String, String> {
        val index = mutableMapOf<String, String>()
        if (!coversDir.exists() || !coversDir.isDirectory) return index
        coversDir.listFiles()?.filter {
            it.isFile && it.extension.lowercase() in listOf(
                "jpg",
                "jpeg",
                "png",
                "webp"
            )
        }?.forEach { file ->
            try {
                val bytes = file.readBytes()
                val hash = java.security.MessageDigest.getInstance("MD5").digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                index[hash] = file.name
            } catch (_: Exception) {
            }
        }
        return index
    }

    /**
     * Извлекает встроенную обложку из аудиофайла.
     * Если обложка с таким же содержимым уже существует — переиспользует её.
     * Возвращает имя файла обложки или null, если обложки нет.
     */
    fun extractCoverFromFile(
        audioFile: File,
        coversDir: File,
        hashIndex: MutableMap<String, String>
    ): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioFile.absolutePath)
            val art = retriever.embeddedPicture
            if (art == null || art.isEmpty()) return null

            val hash = java.security.MessageDigest.getInstance("MD5").digest(art)
                .joinToString("") { "%02x".format(it) }
            val existing = hashIndex[hash]
            if (existing != null) {
                return existing
            }

            val ext = when {
                art.size >= 4 && art[0] == 0xFF.toByte() && art[1] == 0xD8.toByte() -> "jpg"
                art.size >= 8 && art[0] == 0x89.toByte() && art[1] == 0x50.toByte() -> "png"
                else -> "jpg"
            }
            val baseName = audioFile.nameWithoutExtension
            var coverName = "$baseName.$ext"
            if (!coversDir.exists()) coversDir.mkdirs()
            var coverFile = File(coversDir, coverName)
            if (coverFile.exists()) {
                val existingHash = java.security.MessageDigest.getInstance("MD5")
                    .digest(coverFile.readBytes()).joinToString("") { "%02x".format(it) }
                if (existingHash == hash) {
                    return coverName
                }
                var counter = 1
                do {
                    coverName = "${baseName}_${counter}.$ext"
                    coverFile = File(coversDir, coverName)
                    counter++
                } while (coverFile.exists())
            }
            coverFile.writeBytes(art)
            hashIndex[hash] = coverName
            coverName
        } catch (e: Exception) {
            Log.w("MediaReader", "Failed to extract cover from ${audioFile.name}: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Проходит по аудиофайлам в audioDir, извлекает обложки и сохраняет в coversDir.
     * Дедупликация по MD5 хешу содержимого обложки.
     * Возвращает маппинг имя_аудиофайла -> имя_обложки.
     */
    fun extractCoversFromAudioFiles(audioDir: File, coversDir: File): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (!audioDir.exists() || !audioDir.isDirectory) return result

        val hashIndex = buildHashIndex(coversDir)
        val audioExtensions = listOf("mp3", "flac", "m4a", "ogg", "wav", "aac", "opus")
        audioDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in audioExtensions }
            ?.forEach { file ->
                val coverName = extractCoverFromFile(file, coversDir, hashIndex)
                    ?: "_No Album Art.jpg"
                result[file.name] = coverName
            }
        return result
    }

    private fun getPathFromUri(uri: Uri): String {
        val storageDirPath = android.os.Environment.getExternalStorageDirectory().path
        val path = uri.pathSegments.getOrNull(1) ?: return ""
        return path
            .replaceFirst("primary", storageDirPath)
            .replaceFirst(":", "/")
    }
}
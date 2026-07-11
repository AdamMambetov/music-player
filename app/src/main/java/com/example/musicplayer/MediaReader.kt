package com.example.musicplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
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
        dir.listFiles()?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }?.forEach { file ->
            val fileUri = Uri.fromFile(file)
            result[file.name] = fileUri.toString()
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
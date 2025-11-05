package com.example.musicplayer

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri

fun getPathFromUri(uri: Uri?): String {
    // Environment.getStorageDirectory() is "/storage"
    // Environment.getExternalStorageDirectory() is "/storage/emulated/0"
    // it.pathSegments[0] is "tree", [1] is "primary:your/selected/path"

    uri?.let {
        return if (it.path!!.contains("primary"))
            it.pathSegments[1]
                .replaceFirst("primary", Environment.getExternalStorageDirectory().path)
                .replaceFirst(":", "/")
        else
            Environment.getStorageDirectory().path +
                    "/" +
                    it.pathSegments[1].replaceFirst(":", "/")
    }
    return ""
}

fun getMusicPath(context: Context, sourceFile: String): String {
    // Instead of constructing a file path, we should return the sourceFile if it's already a content URI
    // If it's not a content URI, we need to handle it differently
    return if (sourceFile.startsWith("content://")) {
        sourceFile
    } else {
        val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
        val musicPath = sharedPreferences.getString("music_path", "") ?: ""
        
        if (musicPath.isNotEmpty()) {
            "$musicPath/$sourceFile"
        } else {
            ""
        }
    }
}

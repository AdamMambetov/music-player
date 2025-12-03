package com.example.musicplayer.mdreader

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.File

class PathHelper(val context: Context) {
    companion object {
        private const val SP_NAME = "music_player_prefs"
        private const val TRACKS_FOLDER_SP_KEY = "music_path"
        private const val NOTES_FOLDER_SP_KEY = "note_path"
        const val ALBUMS_FOLDER_NAME_IN_NOTES = "Albums"
        const val COVERS_FOLDER_NAME_IN_NOTES = "Covers"
        const val CREATORS_FOLDER_NAME_IN_NOTES = "Creators"
        const val PLAYLISTS_FOLDER_NAME_IN_NOTES = "Playlists"
        const val TRACKS_FOLDER_NAME_IN_NOTES = "Tracks"
    }

    private fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    fun getPathFromUri(uri: Uri?): String {
        // Environment.getStorageDirectory() is "/storage"
        // Environment.getExternalStorageDirectory() is "/storage/emulated/0"
        // it.pathSegments[0] is "tree", [1] is "primary:your/selected/path"

        val storageDirPath = Environment.getStorageDirectory().path
        val externalStorageDirPath = Environment.getExternalStorageDirectory().path
        uri?.let {
            return if (it.path!!.contains("primary"))
                it.pathSegments[1]
                    .replaceFirst(oldValue = "primary", newValue = externalStorageDirPath)
                    .replaceFirst(oldValue = ":", newValue = "/")
            else
                "$storageDirPath/${it.pathSegments[1].replaceFirst(oldValue = ":", newValue = "/")}"
        }
        return ""
    }

    fun getTracksFolderPath(): String {
        return getSharedPreferences()
            .getString(TRACKS_FOLDER_SP_KEY, "")
            ?: ""
    }

    fun setTracksFolderPath(path: String) {
        getSharedPreferences().edit {
            putString(TRACKS_FOLDER_SP_KEY, path)
        }
    }

    fun getNotesFolderPath(): String {
        return getSharedPreferences()
            .getString(NOTES_FOLDER_SP_KEY, "")
            ?: ""
    }

    fun setNotesFolderPath(path: String) {
        getSharedPreferences().edit {
            putString(NOTES_FOLDER_SP_KEY, path)
        }
    }

    fun scanFolderInNotesDir(folderName: String): List<File> {
        val notePath = getNotesFolderPath()
        if (notePath.isEmpty())
            return emptyList()

        val noteDir = File(getPathFromUri(notePath.toUri()))
        val dirs = noteDir.listFiles() ?: emptyArray<File>()
        for (dir in dirs) {
            if (dir.name == folderName) {
                return dir.listFiles()?.toList() ?: emptyList()
            }
        }
        return emptyList()
    }
}
package com.example.musicplayer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.core.content.edit

private const val SP_NAME = "music_player_prefs"
private const val TRACKS_FOLDER_SP_KEY = "music_path"
private const val NOTES_FOLDER_SP_KEY = "note_path"
const val ALBUMS_FOLDER_NAME_IN_NOTES = "Albums"
const val COVERS_FOLDER_NAME_IN_NOTES = "Covers"
const val CREATORS_FOLDER_NAME_IN_NOTES = "Creators"
const val PLAYLISTS_FOLDER_NAME_IN_NOTES = "Playlists"
const val TRACKS_FOLDER_NAME_IN_NOTES = "Tracks"


private fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
}

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

fun getTracksFolderPath(context: Context): String {
    return getSharedPreferences(context).getString(TRACKS_FOLDER_SP_KEY, "") ?: ""
}

fun setTracksFolderPath(context: Context, path: String) {
    getSharedPreferences(context).edit { putString(TRACKS_FOLDER_SP_KEY, path) }
}

fun getNotesFolderPath(context: Context): String {
    return getSharedPreferences(context).getString(NOTES_FOLDER_SP_KEY, "") ?: ""
}

fun setNotesFolderPath(context: Context, path: String) {
    getSharedPreferences(context).edit { putString(NOTES_FOLDER_SP_KEY, path) }
}

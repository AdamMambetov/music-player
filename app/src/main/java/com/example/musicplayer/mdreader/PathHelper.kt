package com.example.musicplayer.mdreader

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.core.content.edit
import androidx.core.net.toUri
import org.json.JSONArray
import java.io.File

data class SavedQueueState(
    val trackIds: List<String>,
    val index: Int,
    val shuffleEnabled: Boolean,
    val shuffleTrackIds: List<String>,
    val shuffleIndex: Int,
    val currentTrackId: String,
    val playbackPositionMs: Long,
)

class PathHelper(val context: Context) {
    companion object {
        private const val SP_NAME = "music_player_prefs"
        private const val TRACKS_FOLDER_SP_KEY = "music_path"
        private const val NOTES_FOLDER_SP_KEY = "note_path"
        private const val REPLAY_GAIN_ENABLED_KEY = "replay_gain_enabled"
        private const val QUEUE_TRACK_IDS_KEY = "queue_track_ids"
        private const val QUEUE_INDEX_KEY = "queue_index"
        private const val SHUFFLE_ENABLED_KEY = "shuffle_enabled"
        private const val SHUFFLE_TRACK_IDS_KEY = "shuffle_track_ids"
        private const val SHUFFLE_INDEX_KEY = "shuffle_index"
        private const val CURRENT_TRACK_ID_KEY = "current_track_id"
        private const val PLAYBACK_POSITION_KEY = "playback_position_ms"
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

    fun isReplayGainEnabled(): Boolean {
        return getSharedPreferences().getBoolean(REPLAY_GAIN_ENABLED_KEY, false)
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        getSharedPreferences().edit {
            putBoolean(REPLAY_GAIN_ENABLED_KEY, enabled)
        }
    }

    fun saveQueueState(
        trackIds: List<String>,
        index: Int,
        shuffleEnabled: Boolean,
        shuffleTrackIds: List<String>,
        shuffleIndex: Int,
        currentTrackId: String,
        playbackPositionMs: Long,
    ) {
        getSharedPreferences().edit {
            putString(QUEUE_TRACK_IDS_KEY, JSONArray(trackIds).toString())
            putInt(QUEUE_INDEX_KEY, index)
            putBoolean(SHUFFLE_ENABLED_KEY, shuffleEnabled)
            putString(SHUFFLE_TRACK_IDS_KEY, JSONArray(shuffleTrackIds).toString())
            putInt(SHUFFLE_INDEX_KEY, shuffleIndex)
            putString(CURRENT_TRACK_ID_KEY, currentTrackId)
            putLong(PLAYBACK_POSITION_KEY, playbackPositionMs)
        }
    }

    fun loadQueueState(): SavedQueueState? {
        val sp = getSharedPreferences()
        val trackIdsJson = sp.getString(QUEUE_TRACK_IDS_KEY, null) ?: return null
        val trackId = sp.getString(CURRENT_TRACK_ID_KEY, null) ?: return null
        return try {
            val ids = JSONArray(trackIdsJson).let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            val shuffleJson = sp.getString(SHUFFLE_TRACK_IDS_KEY, "[]") ?: "[]"
            val shuffleIds = JSONArray(shuffleJson).let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            SavedQueueState(
                trackIds = ids,
                index = sp.getInt(QUEUE_INDEX_KEY, -1),
                shuffleEnabled = sp.getBoolean(SHUFFLE_ENABLED_KEY, false),
                shuffleTrackIds = shuffleIds,
                shuffleIndex = sp.getInt(SHUFFLE_INDEX_KEY, -1),
                currentTrackId = trackId,
                playbackPositionMs = sp.getLong(PLAYBACK_POSITION_KEY, 0L),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearQueueState() {
        getSharedPreferences().edit {
            remove(QUEUE_TRACK_IDS_KEY)
            remove(QUEUE_INDEX_KEY)
            remove(SHUFFLE_ENABLED_KEY)
            remove(SHUFFLE_TRACK_IDS_KEY)
            remove(SHUFFLE_INDEX_KEY)
            remove(CURRENT_TRACK_ID_KEY)
            remove(PLAYBACK_POSITION_KEY)
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
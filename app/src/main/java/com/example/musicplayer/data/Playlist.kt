package com.example.musicplayer.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Serializable
@Stable
data class PlaylistDocument(
    val created: Long = 0L,
    val aliases: List<String> = emptyList(),
    var tracklist: List<TrackDocument> = emptyList(),
    val fileName: String = "",
    val id: String = deterministicId("playlist", fileName),
) {
    override fun equals(other: Any?): Boolean {
        if (other !is PlaylistDocument)
            return false
        return other.fileName == fileName
    }

    override fun hashCode(): Int {
        var result = created.hashCode()
        result = 31 * result + aliases.hashCode()
        result = 31 * result + tracklist.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    fun getCreatedDate(): Calendar {
        return Calendar
            .Builder()
            .setInstant(created)
            .build()
    }

    fun getCreatedString(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        ).format(getCreatedDate().time)
    }

    fun isValid(): Boolean {
        return fileName.isNotEmpty()
    }

    companion object {
        fun createEmpty(): PlaylistDocument {
            return PlaylistDocument()
        }

        const val UNKNOWN = "Unknown Playlist"
    }
}

package com.example.musicplayer.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

fun deterministicId(type: String, fileName: String): String =
    UUID.nameUUIDFromBytes("$type:$fileName".toByteArray()).toString()

@Serializable
@Stable
data class TrackDocument(
    val created: Long = 0L,
    val aliases: List<String> = emptyList(),
    val cover: String = "",
    val year: Long = 0L,
    val album: String = "",
    val creators: List<CreatorDocument> = emptyList(),
    val numberInAlbum: Long = 0L,
    val related: List<String> = emptyList(),
    val sourceFile: String = "",
    val fileName: String = "",
    val id: String = deterministicId("track", fileName),
    var listenInSec: Int = 0,
    var sourceUri: String = "",
    val coverOf: String = "",
    val durationSec: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is TrackDocument) return false
        return other.id == id && other.listenInSec == listenInSec
    }

    override fun hashCode(): Int = id.hashCode() * 31 + listenInSec

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
        return sourceFile.isNotEmpty() and fileName.isNotEmpty()
    }

    companion object {
        fun createEmpty(): TrackDocument {
            return TrackDocument()
        }

        const val UNKNOWN = "Unknown Track"
    }
}

data class TrackListState(
    val trackList: List<TrackDocument> = emptyList(),
    val albumList: List<AlbumDocument> = emptyList(),
    val creatorList: List<CreatorDocument> = emptyList(),
    val searchQuery: String = "",
)

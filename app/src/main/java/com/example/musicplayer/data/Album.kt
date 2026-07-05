package com.example.musicplayer.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Serializable
@Stable
data class AlbumDocument(
    val created: Long = 0L,
    val aliases: List<String> = emptyList(),
    val cover: String = "",
    val year: Long = 0L,
    val creators: List<CreatorDocument> = emptyList(),
    val tracklist: List<TrackDocument> = emptyList(),
    val fileName: String = "",
    val id: String = UUID.randomUUID().toString(),
) {
    override fun equals(other: Any?): Boolean {
        if (other !is AlbumDocument)
            return false
        return other.fileName == fileName
    }

    override fun hashCode(): Int {
        var result = created.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + aliases.hashCode()
        result = 31 * result + cover.hashCode()
        result = 31 * result + creators.hashCode()
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
        fun createEmpty(): AlbumDocument {
            return AlbumDocument()
        }

        const val UNKNOWN = "Unknown Album"
    }
}

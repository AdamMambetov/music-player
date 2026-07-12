package com.example.musicplayer.data

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Serializable
@Stable
data class AlbumDocument(
    val created: Long = 0L,
    val aliases: ImmutableList<String> = persistentListOf(),
    val cover: String = "",
    val year: Long = 0L,
    val creators: ImmutableList<CreatorDocument> = persistentListOf(),
    val tracklist: ImmutableList<TrackDocument> = persistentListOf(),
    val fileName: String = "",
    val id: String = deterministicId("album", fileName),
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

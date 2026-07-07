package com.example.musicplayer.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Serializable
@Stable
data class CreatorDocument(
    val created: Long = 0L,
    val aliases: List<String> = emptyList(),
    val fileName: String = "",
    val id: String = deterministicId("creator", fileName),
    var listenInSec: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is CreatorDocument)
            return false
        return other.fileName == fileName
    }

    override fun hashCode(): Int {
        var result = created.hashCode()
        result = 31 * result + aliases.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + listenInSec.hashCode()
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
        fun createEmpty(): CreatorDocument {
            return CreatorDocument()
        }

        const val UNKNOWN = "Unknown Artist"
    }
}

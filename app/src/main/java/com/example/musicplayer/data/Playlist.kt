package com.example.musicplayer.data

import androidx.appsearch.annotation.Document
import androidx.appsearch.annotation.Document.CreationTimestampMillis
import androidx.appsearch.annotation.Document.DocumentProperty
import androidx.appsearch.annotation.Document.Namespace
import androidx.appsearch.annotation.Document.Id
import androidx.appsearch.annotation.Document.StringProperty
import androidx.appsearch.app.AppSearchSchema
import com.example.musicplayer.MusicPlayerSearchManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Document
data class PlaylistDocument(
    @CreationTimestampMillis
    val created: Long,
    @StringProperty
    val aliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val lowerAliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val upperAliases: List<String>,
    @DocumentProperty
    val tracklist: List<TrackDocument>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val fileName: String,
    @Namespace
    val namespace: String = MusicPlayerSearchManager.NAMESPACE,
    @Id
    val id: String = UUID.randomUUID().toString(),
) {
    fun getCreatedDate(): Calendar {
        return Calendar
            .Builder()
            .setInstant(created)
            .build()
    }

    fun getCreatedString(): String {
        return SimpleDateFormat(
            "yyyy-MM-DD'T'HH:mm:ssZ",
            Locale.getDefault()
        ).format(getCreatedDate().time)
    }

    fun isValid(): Boolean {
        return fileName.isNotEmpty()
    }

    companion object {
        fun createEmpty(): PlaylistDocument {
            return PlaylistDocument(
                created = 0L,
                aliases = emptyList(),
                lowerAliases = emptyList(),
                upperAliases = emptyList(),
                tracklist = emptyList(),
                fileName = "",
            )
        }
    }
}

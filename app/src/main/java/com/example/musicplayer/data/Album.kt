package com.example.musicplayer.data

import androidx.appsearch.annotation.Document
import androidx.appsearch.annotation.Document.CreationTimestampMillis
import androidx.appsearch.annotation.Document.Namespace
import androidx.appsearch.annotation.Document.Id
import androidx.appsearch.annotation.Document.StringProperty
import androidx.appsearch.annotation.Document.DocumentProperty
import androidx.appsearch.annotation.Document.LongProperty
import androidx.appsearch.app.AppSearchSchema
import com.example.musicplayer.MusicPlayerSearchManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Document
data class AlbumDocument(
    @CreationTimestampMillis
    val created: Long,
    @StringProperty
    val aliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val lowerAliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val upperAliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val cover: String,
    @LongProperty(indexingType = AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
    val year: Long,
    @DocumentProperty
    val creators: List<CreatorDocument>,
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
        fun createEmpty(): AlbumDocument {
            return AlbumDocument(
                created = 0L,
                aliases = emptyList(),
                lowerAliases = emptyList(),
                upperAliases = emptyList(),
                cover = "",
                year = 0L,
                creators = emptyList(),
                tracklist = emptyList(),
                fileName = "",
            )
        }
    }
}

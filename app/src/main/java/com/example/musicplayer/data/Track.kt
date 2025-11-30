package com.example.musicplayer.data

import androidx.appsearch.annotation.Document
import androidx.appsearch.annotation.Document.Namespace
import androidx.appsearch.annotation.Document.Id
import androidx.appsearch.annotation.Document.Score
import androidx.appsearch.annotation.Document.StringProperty
import androidx.appsearch.annotation.Document.LongProperty
import androidx.appsearch.annotation.Document.DocumentProperty
import androidx.appsearch.annotation.Document.CreationTimestampMillis
import androidx.appsearch.app.AppSearchSchema
import androidx.compose.runtime.Stable
import com.example.musicplayer.MusicPlayerSearchManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Stable
@Document
data class TrackDocument(
    @CreationTimestampMillis
    val created: Long,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val aliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val lowerAliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val upperAliases: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val cover: String,
    @LongProperty(indexingType = AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
    val year: Long,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val album: String,
    @DocumentProperty
    val creators: List<CreatorDocument>,
    @LongProperty(indexingType = AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
    val numberInAlbum: Long,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val related: List<String>,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val sourceFile: String,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val fileName: String,
    @Namespace
    val namespace: String = MusicPlayerSearchManager.NAMESPACE,
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Score
    var listenInSec: Int = 0,
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    var sourceUri: String = "",
    @StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
    val coverOf: String = "",
) {
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
            return TrackDocument(
                created = 0L,
                aliases = emptyList(),
                lowerAliases = emptyList(),
                upperAliases = emptyList(),
                cover = "",
                year = 0L,
                album = "",
                creators = emptyList(),
                sourceFile = "",
                numberInAlbum = 0L,
                related = emptyList(),
                fileName = "",
            )
        }
    }
}

data class TrackListState(
    val trackList: List<TrackDocument> = emptyList(),
    val searchQuery: String = "",
)

package com.example.musicplayer

import android.content.Context
import android.util.Log
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.RemoveByDocumentIdRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.TrackDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicPlayerSearchManager(
    private val appContext: Context,
) {
    companion object {
        const val DATABASE_NAME = "best_music_player"
        const val NAMESPACE = "adam_music_player"
    }

    private var session: AppSearchSession? = null

    suspend fun init() {
        withContext(Dispatchers.IO) {
            val sessionFuture = LocalStorage.createSearchSessionAsync(
                LocalStorage.SearchContext.Builder(
                    appContext,
                    DATABASE_NAME
                ).build()
            )
            val setSchemaRequest = SetSchemaRequest.Builder()
                .addDocumentClasses(listOf(
                    TrackDocument::class.java,
                    CreatorDocument::class.java,
                    AlbumDocument::class.java,
                    PlaylistDocument::class.java,
                ))
                .build()
            session = sessionFuture.get()
            session?.setSchemaAsync(setSchemaRequest)
        }
    }

    private suspend fun putDocuments(documents: List<Any>): Boolean {
        return withContext(Dispatchers.IO) {
            val success = session?.putAsync(
                PutDocumentsRequest.Builder()
                    .addDocuments(documents)
                    .build()
            )?.get()?.isSuccess == true
            if (success)
                session?.requestFlushAsync()?.get()
            return@withContext success
        }
    }

    private suspend fun removeDocuments(documents: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            val success = session?.removeAsync(
                RemoveByDocumentIdRequest.Builder(NAMESPACE)
                    .addIds(documents)
                    .build()
            )?.get()?.isSuccess == true
            if (success)
                session?.requestFlushAsync()?.get()
            return@withContext success
        }
    }

    private suspend inline fun<reified T : Any> searchAllDocumentsOf(): List<T> {
        return withContext(Dispatchers.IO) {
            val searchSpec = SearchSpec.Builder()
                .setResultCountPerPage(100)
                .addFilterNamespaces(NAMESPACE)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .build()
            val searchResult = session?.search(
                "",
                searchSpec
            ) ?: return@withContext emptyList()
            val result = mutableListOf<T>()

            // Pagination result
            do {
                val page = searchResult.nextPageAsync.get()
                result.addAll(page.mapNotNull {
                    if (it.genericDocument.schemaType == T::class.java.simpleName) {
                        it.getDocument(T::class.java)
                    } else null
                })
            } while (page.isNotEmpty())
            result
        }
    }

    suspend fun putTracks(tracks: List<TrackDocument>): Boolean {
        return putDocuments(tracks)
    }

    suspend fun putCreators(creators: List<CreatorDocument>): Boolean {
        return putDocuments(creators)
    }

    suspend fun putAlbums(albums: List<AlbumDocument>): Boolean {
        return putDocuments(albums)
    }

    suspend fun putPlaylists(playlists: List<PlaylistDocument>): Boolean {
        return putDocuments(playlists)
    }

    suspend fun removeTracks(tracks: List<TrackDocument>): Boolean {
        return removeDocuments(tracks.map { it.id })
    }

    suspend fun removeCreators(creators: List<CreatorDocument>): Boolean {
        return removeDocuments(creators.map { it.id })
    }

    suspend fun removeAlbums(albums: List<AlbumDocument>): Boolean {
        return removeDocuments(albums.map { it.id })
    }

    suspend fun removePlayLists(playlists: List<PlaylistDocument>): Boolean {
        return removeDocuments(playlists.map { it.id })
    }

    suspend fun searchTracks(query: String): List<TrackDocument> {
        return withContext(Dispatchers.IO) {
            val searchSpec = SearchSpec.Builder()
                .setResultCountPerPage(10)
                .addFilterNamespaces(NAMESPACE)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                .build()
            val result = session?.search(
                query,
                searchSpec
            ) ?: return@withContext emptyList()

            // Pagination result
            val page = result.nextPageAsync.get()
            page.mapNotNull {
                if (it.genericDocument.schemaType == TrackDocument::class.java.simpleName) {
                    it.getDocument(TrackDocument::class.java)
                } else null
            }
        }
    }

    suspend fun searchAllTracks(): List<TrackDocument> {
        return searchAllDocumentsOf()
    }

    suspend fun searchAllCreators(): List<CreatorDocument> {
        return searchAllDocumentsOf()
    }

    suspend fun searchAllAlbums(): List<AlbumDocument> {
        return searchAllDocumentsOf()
    }

    suspend fun searchAllPlaylists(): List<PlaylistDocument> {
        return searchAllDocumentsOf()
    }

    fun closeSession() {
        session?.close()
        session = null
    }
}
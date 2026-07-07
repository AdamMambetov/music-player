package com.example.musicplayer.data

import android.util.Log
import androidx.core.net.toUri
import com.example.musicplayer.MediaReader
import com.example.musicplayer.mdreader.MarkdownReader
import com.example.musicplayer.mdreader.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(
    private val markdownReader: MarkdownReader,
    private val postgres: PostgresDataSource,
    private val mediaReader: MediaReader,
    private val pathHelper: PathHelper
) {
    companion object {
        private const val TAG = "MusicRepository"
    }

    suspend fun connectPostgres(): Boolean = withContext(Dispatchers.IO) {
        val connected = postgres.connect()
        if (connected) {
            postgres.rebuildSearchIndex()
        }
        connected
    }

    // ==================== Load ====================

    suspend fun loadAllCreators(): List<CreatorDocument> = withContext(Dispatchers.IO) {
        val pgCreators = postgres.getAllCreators()
        if (pgCreators.isNotEmpty()) {
            Log.d(TAG, "Loaded ${pgCreators.size} creators from PostgreSQL")
            return@withContext pgCreators
        }

        val mdCreators = markdownReader.scanCreators()
        if (mdCreators.isNotEmpty()) {
            Log.d(TAG, "Loaded ${mdCreators.size} creators from markdown, syncing to PostgreSQL")
            postgres.putCreators(mdCreators)
        }
        mdCreators
    }

    suspend fun loadAllTracks(creators: List<CreatorDocument>): List<TrackDocument> = withContext(Dispatchers.IO) {
        val sourceFileUris = mediaReader.scanAudio(
            uri = pathHelper.getTracksFolderPath().toUri()
        )

        val pgTracks = postgres.getAllTracks()
        if (pgTracks.isNotEmpty()) {
            Log.d(TAG, "Loaded ${pgTracks.size} tracks from PostgreSQL")
            val trackCreatorMap = pgTracks.associate { track ->
                track.id to postgres.getTrackCreators(track.id)
            }
            val creatorMap = creators.associateBy { it.id }
            val resolved = pgTracks.map { track ->
                val sourceUri = sourceFileUris[track.sourceFile]
                val trackCreators = trackCreatorMap[track.id]
                    ?.mapNotNull { creatorMap[it] }
                    ?: emptyList()
                val withUri = if (sourceUri != null) track.copy(sourceUri = sourceUri) else track
                withUri.copy(creators = trackCreators)
            }
            return@withContext resolved
        }

        val mdTracks = markdownReader.scanTracks(creators)
            .sortedByDescending { it.created }
            .map { track ->
                val sourceUri = sourceFileUris[track.sourceFile]
                track.copy(sourceUri = sourceUri ?: "")
            }

        if (mdTracks.isNotEmpty()) {
            Log.d(TAG, "Loaded ${mdTracks.size} tracks from markdown, syncing to PostgreSQL")
            postgres.putTracks(mdTracks)
            mdTracks.forEach { track ->
                postgres.putTrackCreators(track.id, track.creators.map { it.id })
            }
        }
        mdTracks
    }

    suspend fun loadAllAlbums(creators: List<CreatorDocument>, tracks: List<TrackDocument>): List<AlbumDocument> = withContext(Dispatchers.IO) {
        val pgAlbums = postgres.getAllAlbums()
        if (pgAlbums.isNotEmpty()) {
            Log.d(TAG, "Loaded ${pgAlbums.size} albums from PostgreSQL")
            val creatorMap = creators.associateBy { it.id }
            val trackMap = tracks.associateBy { it.id }
            return@withContext pgAlbums.map { album ->
                val albumCreatorIds = postgres.getAlbumCreators(album.id)
                val albumTrackIds = postgres.getAlbumTracks(album.id)
                album.copy(
                    creators = albumCreatorIds.mapNotNull { creatorMap[it] },
                    tracklist = albumTrackIds.mapNotNull { trackMap[it] }
                )
            }
        }

        val mdAlbums = markdownReader.scanAlbums(creators, tracks)
        if (mdAlbums.isNotEmpty()) {
            Log.d(TAG, "Loaded ${mdAlbums.size} albums from markdown, syncing to PostgreSQL")
            postgres.putAlbums(mdAlbums)
            mdAlbums.forEach { album ->
                postgres.putAlbumCreators(album.id, album.creators.map { it.id })
                postgres.putAlbumTracks(album.id, album.tracklist.map { it.id })
            }
        }
        mdAlbums
    }

    suspend fun loadAllPlaylists(tracks: List<TrackDocument>): List<PlaylistDocument> = withContext(Dispatchers.IO) {
        val pgPlaylists = postgres.getAllPlaylists()
        if (pgPlaylists.isNotEmpty()) {
            Log.d(TAG, "Loaded ${pgPlaylists.size} playlists from PostgreSQL")
            val trackMap = tracks.associateBy { it.id }
            return@withContext pgPlaylists.map { playlist ->
                val playlistTrackIds = postgres.getPlaylistTracks(playlist.id)
                playlist.copy(tracklist = playlistTrackIds.mapNotNull { trackMap[it] })
            }
        }

        val mdPlaylists = markdownReader.scanPlaylists(tracks)
        if (mdPlaylists.isNotEmpty()) {
            Log.d(TAG, "Loaded ${mdPlaylists.size} playlists from markdown, syncing to PostgreSQL")
            postgres.putPlaylists(mdPlaylists)
            mdPlaylists.forEach { playlist ->
                postgres.putPlaylistTracks(playlist.id, playlist.tracklist.map { it.id })
            }
        }
        mdPlaylists
    }

    // ==================== Save ====================

    suspend fun saveTrack(track: TrackDocument) = withContext(Dispatchers.IO) {
        markdownReader.saveTrack(track)
        postgres.putTracks(listOf(track))
    }

    suspend fun saveCreator(creator: CreatorDocument) = withContext(Dispatchers.IO) {
        markdownReader.saveCreator(creator)
        postgres.putCreators(listOf(creator))
    }

    suspend fun savePlaylist(playlist: PlaylistDocument) = withContext(Dispatchers.IO) {
        markdownReader.savePlaylist(playlist)
        postgres.putPlaylists(listOf(playlist))
        postgres.putPlaylistTracks(playlist.id, playlist.tracklist.map { it.id })
    }

    // ==================== Listen tracking ====================

    suspend fun incrementListen(track: TrackDocument, creators: List<CreatorDocument>) = withContext(Dispatchers.IO) {
        markdownReader.saveTrack(track)
        postgres.updateTrackListenInSec(track)

        creators.forEach { creator ->
            markdownReader.saveCreator(creator)
            postgres.updateCreatorListenInSec(creator.id)
        }
    }

    // ==================== Search ====================

    suspend fun searchTracks(query: String): List<TrackDocument> = withContext(Dispatchers.IO) {
        val results = postgres.search(query)
        val trackIds = results.filter { it.entityType == "track" }.map { it.entityId }.distinct()
        if (trackIds.isEmpty()) return@withContext emptyList()

        val sourceFileUris = mediaReader.scanAudio(
            uri = pathHelper.getTracksFolderPath().toUri()
        )
        val tracksById = postgres.getAllTracks().associateBy { it.id }
        trackIds.mapNotNull { id ->
            tracksById[id]?.let { track ->
                val sourceUri = sourceFileUris[track.sourceFile]
                if (sourceUri != null) track.copy(sourceUri = sourceUri) else track
            }
        }
    }

    suspend fun searchCreators(query: String): List<CreatorDocument> = withContext(Dispatchers.IO) {
        val results = postgres.search(query)
        val creatorIds = results.filter { it.entityType == "creator" }.map { it.entityId }.distinct()
        if (creatorIds.isEmpty()) return@withContext emptyList()

        val creatorsById = postgres.getAllCreators().associateBy { it.id }
        creatorIds.mapNotNull { creatorsById[it] }
    }

    suspend fun searchAlbums(query: String): List<AlbumDocument> = withContext(Dispatchers.IO) {
        val results = postgres.search(query)
        val albumIds = results.filter { it.entityType == "album" }.map { it.entityId }.distinct()
        if (albumIds.isEmpty()) return@withContext emptyList()

        val albumsById = postgres.getAllAlbums().associateBy { it.id }
        albumIds.mapNotNull { albumsById[it] }
    }

    suspend fun searchPlaylists(query: String): List<PlaylistDocument> = withContext(Dispatchers.IO) {
        val results = postgres.search(query)
        val playlistIds = results.filter { it.entityType == "playlist" }.map { it.entityId }.distinct()
        if (playlistIds.isEmpty()) return@withContext emptyList()

        val playlistsById = postgres.getAllPlaylists().associateBy { it.id }
        playlistIds.mapNotNull { playlistsById[it] }
    }

    // ==================== Scan ====================

    suspend fun scanAll(clearCache: Boolean): ScanResult = withContext(Dispatchers.IO) {
        if (clearCache) {
            postgres.clearAll()
        }

        val creators = markdownReader.scanCreators()
        postgres.putCreators(creators)

        val sourceFileUris = mediaReader.scanAudio(
            uri = pathHelper.getTracksFolderPath().toUri()
        )
        val tracks = markdownReader.scanTracks(creators)
            .sortedByDescending { it.created }
            .map { track ->
                val sourceUri = sourceFileUris[track.sourceFile]
                track.copy(sourceUri = sourceUri ?: "")
            }
        postgres.putTracks(tracks)
        tracks.forEach { track ->
            postgres.putTrackCreators(track.id, track.creators.map { it.id })
        }

        val albums = markdownReader.scanAlbums(creators, tracks)
        postgres.putAlbums(albums)
        albums.forEach { album ->
            postgres.putAlbumCreators(album.id, album.creators.map { it.id })
            postgres.putAlbumTracks(album.id, album.tracklist.map { it.id })
        }

        val playlists = markdownReader.scanPlaylists(tracks)
        postgres.putPlaylists(playlists)
        playlists.forEach { playlist ->
            postgres.putPlaylistTracks(playlist.id, playlist.tracklist.map { it.id })
        }

        val favorites = playlists.find {
            it.aliases.getOrElse(0) { false } == "Favorites"
        }

        ScanResult(creators, tracks, albums, playlists, favorites)
    }
}

data class ScanResult(
    val creators: List<CreatorDocument>,
    val tracks: List<TrackDocument>,
    val albums: List<AlbumDocument>,
    val playlists: List<PlaylistDocument>,
    val favorites: PlaylistDocument?
)

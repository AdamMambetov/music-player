package com.example.musicplayer.data

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.example.musicplayer.AnalysisResult
import com.example.musicplayer.MediaReader
import com.example.musicplayer.ReplayGain
import com.example.musicplayer.mdreader.MarkdownReader
import com.example.musicplayer.mdreader.PathHelper
import kotlinx.collections.immutable.toImmutableList
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
                val audioInfo = sourceFileUris[track.sourceFile]
                val trackCreators = trackCreatorMap[track.id]
                    ?.mapNotNull { creatorMap[it] }
                    ?: emptyList()
                val withUri = if (audioInfo != null) track.copy(sourceUri = audioInfo.uri, durationSec = audioInfo.durationMs / 1000) else track
                withUri.copy(creators = trackCreators.toImmutableList())
            }
            return@withContext resolved
        }

        val mdTracks = markdownReader.scanTracks(creators)
            .sortedByDescending { it.created }
            .map { track ->
                val audioInfo = sourceFileUris[track.sourceFile]
                track.copy(sourceUri = audioInfo?.uri ?: "", durationSec = (audioInfo?.durationMs ?: 0L) / 1000)
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
            Log.d(TAG, "Loaded ${pgAlbums.size} albums from PostgreSQL, ${tracks.size} tracks available")
            val creatorMap = creators.associateBy { it.id }
            val trackMap = tracks.associateBy { it.id }
            return@withContext pgAlbums.map { album ->
                val albumCreatorIds = postgres.getAlbumCreators(album.id)
                val albumTrackIds = postgres.getAlbumTracks(album.id)
                val resolvedTracks = albumTrackIds.mapNotNull { trackMap[it] }
                Log.d(TAG, "Album '${album.aliases.getOrElse(0) { album.fileName }}': ${albumTrackIds.size} track IDs from PG, ${resolvedTracks.size} resolved")
                album.copy(
                    creators = albumCreatorIds.mapNotNull { creatorMap[it] }.toImmutableList(),
                    tracklist = resolvedTracks.toImmutableList()
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
            Log.d(TAG, "Loaded ${pgPlaylists.size} playlists from PostgreSQL, ${tracks.size} tracks available")
            val trackMap = tracks.associateBy { it.id }
            return@withContext pgPlaylists.map { playlist ->
                val playlistTrackIds = postgres.getPlaylistTracks(playlist.id)
                val resolvedTracks = playlistTrackIds.mapNotNull { trackMap[it] }
                Log.d(TAG, "Playlist '${playlist.aliases.getOrElse(0) { playlist.fileName }}': ${playlistTrackIds.size} track IDs from PG, ${resolvedTracks.size} resolved")
                playlist.copy(tracklist = resolvedTracks.toImmutableList())
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
            postgres.updateCreatorListenInSec(creator.id)
            val updated = postgres.getCreator(creator.id)
            if (updated != null) {
                markdownReader.saveCreator(updated)
            }
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
        val creators = postgres.getAllCreators()
        val creatorMap = creators.associateBy { it.id }
        val tracksById = postgres.getAllTracks().associateBy { it.id }
        trackIds.mapNotNull { id ->
            tracksById[id]?.let { track ->
                val audioInfo = sourceFileUris[track.sourceFile]
                val trackCreators = postgres.getTrackCreators(track.id)
                    .mapNotNull { creatorMap[it] }
                val withUri = if (audioInfo != null) track.copy(sourceUri = audioInfo.uri, durationSec = audioInfo.durationMs / 1000) else track
                withUri.copy(creators = trackCreators.toImmutableList())
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

        val allCreators = postgres.getAllCreators()
        val creatorMap = allCreators.associateBy { it.id }
        val allTracks = postgres.getAllTracks()
        val trackMap = allTracks.associateBy { it.id }
        val albumsById = postgres.getAllAlbums().associateBy { it.id }
        albumIds.mapNotNull { id ->
            albumsById[id]?.let { album ->
                val albumCreatorIds = postgres.getAlbumCreators(album.id)
                val albumTrackIds = postgres.getAlbumTracks(album.id)
                album.copy(
                    creators = albumCreatorIds.mapNotNull { creatorMap[it] }.toImmutableList(),
                    tracklist = albumTrackIds.mapNotNull { trackMap[it] }.toImmutableList()
                )
            }
        }
    }

    suspend fun searchPlaylists(query: String): List<PlaylistDocument> = withContext(Dispatchers.IO) {
        val results = postgres.search(query)
        val playlistIds = results.filter { it.entityType == "playlist" }.map { it.entityId }.distinct()
        if (playlistIds.isEmpty()) return@withContext emptyList()

        val playlistsById = postgres.getAllPlaylists().associateBy { it.id }
        playlistIds.mapNotNull { playlistsById[it] }
    }

    // ==================== Track Gain ====================

    fun getTrackGain(trackId: String): AnalysisResult? {
        return postgres.getTrackGain(trackId)
    }

    suspend fun analyzeAllTracks(
        tracks: List<TrackDocument>,
        context: Context,
        onProgress: (analyzed: Int, total: Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val unanalyzedIds = postgres.getUnanalyzedTrackIds(tracks.map { it.id })
        if (unanalyzedIds.isEmpty()) {
            Log.d(TAG, "All tracks already analyzed")
            return@withContext
        }

        val replayGain = ReplayGain()
        val unanalyzedTracks = tracks.filter { it.id in unanalyzedIds }
        Log.d(TAG, "Analyzing ${unanalyzedTracks.size} tracks...")

        unanalyzedTracks.forEachIndexed { index, track ->
            try {
                if (track.sourceUri.isNotEmpty()) {
                    val uri = android.net.Uri.parse(track.sourceUri)
                    val result = replayGain.analyzeTrack(context, uri)
                    val gainDb = result.trackGainDb ?: 0f
                    postgres.putTrackGain(track.id, gainDb, result.peakLevelDb)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze track ${track.id}: ${e.message}")
            }
            onProgress(index + 1, unanalyzedTracks.size)
        }

        Log.d(TAG, "Analysis complete: ${unanalyzedTracks.size} tracks analyzed")
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
                val audioInfo = sourceFileUris[track.sourceFile]
                track.copy(sourceUri = audioInfo?.uri ?: "", durationSec = (audioInfo?.durationMs ?: 0L) / 1000)
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
            it.aliases.getOrElse(0) { "" } == "Favorites"
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

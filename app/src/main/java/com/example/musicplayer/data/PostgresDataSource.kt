package com.example.musicplayer.data

import android.util.Log
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

class PostgresDataSource(
    private val host: String = "localhost",
    private val port: Int = 5432,
    private val database: String = "music_player",
    private val user: String = "user",
    private val password: String = "user"
) {
    private var connection: Connection? = null

    companion object {
        private const val TAG = "PostgresDataSource"
        private const val GRAPH_NAME = "music"

        @Volatile
        var instance: PostgresDataSource? = null
            private set

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    fun connect(): Boolean {
        return try {
            Class.forName("org.postgresql.Driver")
            val url = "jdbc:postgresql://$host:$port/$database?connectTimeout=5&socketTimeout=10"
            connection = DriverManager.getConnection(url, user, password)
            instance = this

            val stmt = connection?.createStatement()
            val rs = stmt?.executeQuery("SELECT version()")
            if (rs?.next() == true) {
                Log.d(TAG, "PostgreSQL version: ${rs.getString(1)}")
            }
            rs?.close()
            stmt?.close()

            initAge()
            createTables()
            true
        } catch (e: SQLException) {
            Log.e(TAG, "SQL Error: ${e.message}")
            false
        } catch (e: Throwable) {
            Log.e(
                TAG,
                "Failed to connect to PostgreSQL: ${e.javaClass.simpleName}: ${e.message}",
                e
            )
            false
        }
    }

    fun isConnected(): Boolean {
        return try {
            connection?.isValid(5) == true
        } catch (_: Exception) {
            false
        }
    }

    fun close() {
        try {
            connection?.close()
            connection = null
            Log.d(TAG, "Connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
    }

    // ==================== AGE init ====================

    private fun initAge() {
        try {
            val stmt = connection?.createStatement() ?: return

            val rs = stmt.executeQuery(
                "SELECT 1 FROM pg_extension WHERE extname = 'age'"
            )
            val ageExists = rs.next()
            rs.close()

            if (!ageExists) {
                Log.w(TAG, "Apache AGE extension not installed. Attempting to create...")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS age")
                Log.d(TAG, "AGE extension created")
            } else {
                Log.d(TAG, "AGE extension already installed")
            }

            stmt.execute("LOAD 'age'")
            stmt.execute("SET search_path = ag_catalog, \"$user\", public")

            val graphCheck = stmt.executeQuery(
                "SELECT 1 FROM ag_catalog.ag_graph WHERE name = '$GRAPH_NAME'"
            )
            if (!graphCheck.next()) {
                stmt.execute("SELECT * FROM ag_catalog.create_graph('$GRAPH_NAME')")
                Log.d(TAG, "AGE graph '$GRAPH_NAME' created")
            } else {
                Log.d(TAG, "AGE graph '$GRAPH_NAME' already exists")
            }
            graphCheck.close()
            stmt.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Apache AGE: ${e.message}", e)
            Log.e(TAG, "Make sure Apache AGE extension is installed on PostgreSQL server")
        }
    }

    private fun ageQuery(cypher: String): ResultSet? {
        val stmt = connection?.createStatement() ?: return null
        return stmt.executeQuery(
            $$"SELECT * FROM ag_catalog.cypher('$$GRAPH_NAME', $$$$cypher$$) AS (result agtype)"
        )
    }

    private fun ageExec(cypher: String) {
        val stmt = connection?.createStatement() ?: return
        stmt.executeQuery(
            $$"SELECT * FROM ag_catalog.cypher('$$GRAPH_NAME', $$$$cypher$$) AS (result agtype)"
        )
        stmt.close()
    }

    private fun ageExecWithReturn(cypher: String): ResultSet? {
        val stmt = connection?.createStatement() ?: return null
        return stmt.executeQuery(
            $$"SELECT * FROM ag_catalog.cypher('$$GRAPH_NAME', $$$$cypher$$) AS (result agtype)"
        )
    }

    // ==================== SQL tables ====================

    private fun createTables() {
        val stmt = connection?.createStatement() ?: return

        stmt.executeUpdate("CREATE EXTENSION IF NOT EXISTS pg_trgm")

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS creators (
                id TEXT PRIMARY KEY,
                created BIGINT NOT NULL DEFAULT 0,
                aliases JSONB NOT NULL DEFAULT '[]',
                file_name TEXT NOT NULL DEFAULT '',
                listen_in_sec INT NOT NULL DEFAULT 0
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS tracks (
                id TEXT PRIMARY KEY,
                created BIGINT NOT NULL DEFAULT 0,
                aliases JSONB NOT NULL DEFAULT '[]',
                cover TEXT NOT NULL DEFAULT '',
                year BIGINT NOT NULL DEFAULT 0,
                album TEXT NOT NULL DEFAULT '',
                number_in_album BIGINT NOT NULL DEFAULT 0,
                related JSONB NOT NULL DEFAULT '[]',
                source_file TEXT NOT NULL DEFAULT '',
                file_name TEXT NOT NULL DEFAULT '',
                listen_in_sec INT NOT NULL DEFAULT 0,
                cover_of TEXT NOT NULL DEFAULT '',
                duration_sec BIGINT NOT NULL DEFAULT 0
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS albums (
                id TEXT PRIMARY KEY,
                created BIGINT NOT NULL DEFAULT 0,
                aliases JSONB NOT NULL DEFAULT '[]',
                cover TEXT NOT NULL DEFAULT '',
                year BIGINT NOT NULL DEFAULT 0,
                file_name TEXT NOT NULL DEFAULT ''
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS playlists (
                id TEXT PRIMARY KEY,
                created BIGINT NOT NULL DEFAULT 0,
                aliases JSONB NOT NULL DEFAULT '[]',
                file_name TEXT NOT NULL DEFAULT ''
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS track_creators (
                track_id TEXT NOT NULL,
                creator_id TEXT NOT NULL,
                ord INT NOT NULL DEFAULT 0,
                PRIMARY KEY (track_id, creator_id)
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS album_creators (
                album_id TEXT NOT NULL,
                creator_id TEXT NOT NULL,
                ord INT NOT NULL DEFAULT 0,
                PRIMARY KEY (album_id, creator_id)
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS album_tracks (
                album_id TEXT NOT NULL,
                track_id TEXT NOT NULL,
                ord INT NOT NULL DEFAULT 0,
                PRIMARY KEY (album_id, track_id)
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS playlist_tracks (
                playlist_id TEXT NOT NULL,
                track_id TEXT NOT NULL,
                ord INT NOT NULL DEFAULT 0,
                PRIMARY KEY (playlist_id, track_id)
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS track_gain (
                track_id TEXT PRIMARY KEY,
                gain_db REAL NOT NULL DEFAULT 0,
                peak_level_db REAL NOT NULL DEFAULT -100,
                analyzed_at BIGINT NOT NULL DEFAULT 0
            )
        """
        )

        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS search_index (
                entity_id TEXT PRIMARY KEY,
                entity_type TEXT NOT NULL,
                name TEXT NOT NULL,
                search_vector tsvector
            )
        """
        )
        stmt.executeUpdate(
            """
            CREATE INDEX IF NOT EXISTS idx_search_vector ON search_index USING GIN(search_vector)
        """
        )
        stmt.executeUpdate(
            """
            CREATE INDEX IF NOT EXISTS idx_search_name_trgm ON search_index USING GIN(name gin_trgm_ops)
        """
        )

        stmt.close()
    }

    // ==================== Search index ====================

    private fun updateSearchIndex(entityId: String, entityType: String, names: List<String>) {
        val searchText = names.joinToString(" ")
        val ps = connection?.prepareStatement(
            """
            INSERT INTO search_index (entity_id, entity_type, name, search_vector)
            VALUES (?, ?, ?, to_tsvector('simple', ?))
            ON CONFLICT (entity_id) DO UPDATE SET
                entity_type = EXCLUDED.entity_type,
                name = EXCLUDED.name,
                search_vector = EXCLUDED.search_vector
        """
        ) ?: return
        ps.setString(1, entityId)
        ps.setString(2, entityType)
        ps.setString(3, searchText)
        ps.setString(4, searchText)
        ps.executeUpdate()
        ps.close()
    }

    private fun removeSearchIndex(entityId: String) {
        val ps =
            connection?.prepareStatement("DELETE FROM search_index WHERE entity_id = ?") ?: return
        ps.setString(1, entityId)
        ps.executeUpdate()
        ps.close()
    }

    fun clearAll() {
        val stmt = connection?.createStatement() ?: return
        stmt.executeUpdate("DELETE FROM search_index")
        stmt.executeUpdate("DELETE FROM track_gain")
        stmt.executeUpdate("DELETE FROM playlist_tracks")
        stmt.executeUpdate("DELETE FROM album_tracks")
        stmt.executeUpdate("DELETE FROM album_creators")
        stmt.executeUpdate("DELETE FROM track_creators")
        stmt.executeUpdate("DELETE FROM playlists")
        stmt.executeUpdate("DELETE FROM albums")
        stmt.executeUpdate("DELETE FROM tracks")
        stmt.executeUpdate("DELETE FROM creators")
        stmt.close()
    }

    // ==================== Track Gain ====================

    fun putTrackGain(trackId: String, gainDb: Float, peakLevelDb: Float) {
        val ps = connection?.prepareStatement(
            """
            INSERT INTO track_gain (track_id, gain_db, peak_level_db, analyzed_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (track_id) DO UPDATE SET
                gain_db = EXCLUDED.gain_db,
                peak_level_db = EXCLUDED.peak_level_db,
                analyzed_at = EXCLUDED.analyzed_at
        """
        ) ?: return
        ps.setString(1, trackId)
        ps.setFloat(2, gainDb)
        ps.setFloat(3, peakLevelDb)
        ps.setLong(4, System.currentTimeMillis())
        ps.executeUpdate()
        ps.close()
    }

    fun getTrackGain(trackId: String): com.example.musicplayer.AnalysisResult? {
        val ps =
            connection?.prepareStatement("SELECT gain_db, peak_level_db FROM track_gain WHERE track_id = ?")
                ?: return null
        ps.setString(1, trackId)
        val rs = ps.executeQuery()
        val result = if (rs.next()) {
            com.example.musicplayer.AnalysisResult(
                trackGainDb = rs.getFloat("gain_db"),
                peakLevelDb = rs.getFloat("peak_level_db"),
                analyzedAt = System.currentTimeMillis()
            )
        } else null
        rs.close()
        ps.close()
        return result
    }

    fun getAllTrackGains(): Map<String, com.example.musicplayer.AnalysisResult> {
        val rs = connection?.createStatement()
            ?.executeQuery("SELECT track_id, gain_db, peak_level_db FROM track_gain")
            ?: return emptyMap()
        val map = mutableMapOf<String, com.example.musicplayer.AnalysisResult>()
        while (rs.next()) {
            map[rs.getString("track_id")] = com.example.musicplayer.AnalysisResult(
                trackGainDb = rs.getFloat("gain_db"),
                peakLevelDb = rs.getFloat("peak_level_db"),
                analyzedAt = System.currentTimeMillis()
            )
        }
        rs.close()
        return map
    }

    fun getUnanalyzedTrackIds(allTrackIds: List<String>): List<String> {
        if (allTrackIds.isEmpty()) return emptyList()
        val ps =
            connection?.prepareStatement("SELECT track_id FROM track_gain") ?: return allTrackIds
        val rs = ps.executeQuery()
        val analyzed = mutableSetOf<String>()
        while (rs.next()) {
            analyzed.add(rs.getString("track_id"))
        }
        rs.close()
        ps.close()
        return allTrackIds.filter { it !in analyzed }
    }

    // ==================== Creators ====================

    fun putCreators(creators: List<CreatorDocument>) {
        val ps = connection?.prepareStatement(
            """
            INSERT INTO creators (id, created, aliases, file_name, listen_in_sec)
            VALUES (?, ?, ?::jsonb, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                created = EXCLUDED.created,
                aliases = EXCLUDED.aliases,
                file_name = EXCLUDED.file_name,
                listen_in_sec = EXCLUDED.listen_in_sec
        """
        ) ?: return
        creators.forEach { creator ->
            try {
                ps.setString(1, creator.id)
                ps.setLong(2, creator.created)
                ps.setString(3, listToJson(creator.aliases))
                ps.setString(4, creator.fileName)
                ps.setInt(5, creator.listenInSec)
                ps.executeUpdate()
                updateSearchIndex(
                    creator.id,
                    "creator",
                    creator.aliases.ifEmpty { listOf(creator.fileName) })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting creator ${creator.id}: ${e.message}")
            }
        }
        ps.close()
    }

    fun getAllCreators(): List<CreatorDocument> {
        val rs = connection?.createStatement()?.executeQuery("SELECT * FROM creators")
            ?: return emptyList()
        val list = mutableListOf<CreatorDocument>()
        while (rs.next()) {
            list.add(
                CreatorDocument(
                    id = rs.getString("id"),
                    created = rs.getLong("created"),
                    aliases = jsonToList(rs.getString("aliases")).toImmutableList(),
                    fileName = rs.getString("file_name"),
                    listenInSec = rs.getInt("listen_in_sec")
                )
            )
        }
        rs.close()
        return list
    }

    fun getCreator(id: String): CreatorDocument? {
        val ps = connection?.prepareStatement("SELECT * FROM creators WHERE id = ?") ?: return null
        ps.setString(1, id)
        val rs = ps.executeQuery()
        val creator = if (rs.next()) {
            CreatorDocument(
                id = rs.getString("id"),
                created = rs.getLong("created"),
                aliases = jsonToList(rs.getString("aliases")).toImmutableList(),
                fileName = rs.getString("file_name"),
                listenInSec = rs.getInt("listen_in_sec")
            )
        } else null
        rs.close()
        ps.close()
        return creator
    }

    fun updateCreatorListenInSec(creatorId: String) {
        val ps = connection?.prepareStatement(
            """
            UPDATE creators SET listen_in_sec = (
                SELECT COALESCE(SUM(t.listen_in_sec), 0)
                FROM tracks t
                JOIN track_creators tc ON tc.track_id = t.id
                WHERE tc.creator_id = ?
            ) WHERE id = ?
        """
        ) ?: return
        ps.setString(1, creatorId)
        ps.setString(2, creatorId)
        ps.executeUpdate()
        ps.close()
    }

    fun removeCreators(creators: List<CreatorDocument>) {
        val ps = connection?.prepareStatement("DELETE FROM creators WHERE id = ?") ?: return
        creators.forEach { creator ->
            try {
                ps.setString(1, creator.id)
                ps.executeUpdate()
                removeSearchIndex(creator.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing creator ${creator.id}: ${e.message}")
            }
        }
        ps.close()
    }

    // ==================== Tracks ====================

    fun putTracks(tracks: List<TrackDocument>) {
        val ps = connection?.prepareStatement(
            """
            INSERT INTO tracks (id, created, aliases, cover, year, album, number_in_album, related, source_file, file_name, listen_in_sec, cover_of, duration_sec)
            VALUES (?, ?, ?::jsonb, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                created = EXCLUDED.created,
                aliases = EXCLUDED.aliases,
                cover = EXCLUDED.cover,
                year = EXCLUDED.year,
                album = EXCLUDED.album,
                number_in_album = EXCLUDED.number_in_album,
                related = EXCLUDED.related,
                source_file = EXCLUDED.source_file,
                file_name = EXCLUDED.file_name,
                listen_in_sec = EXCLUDED.listen_in_sec,
                cover_of = EXCLUDED.cover_of,
                duration_sec = EXCLUDED.duration_sec
        """
        ) ?: return
        tracks.forEach { track ->
            try {
                ps.setString(1, track.id)
                ps.setLong(2, track.created)
                ps.setString(3, listToJson(track.aliases))
                ps.setString(4, track.cover)
                ps.setLong(5, track.year)
                ps.setString(6, track.album)
                ps.setLong(7, track.numberInAlbum)
                ps.setString(8, listToJson(track.related))
                ps.setString(9, track.sourceFile)
                ps.setString(10, track.fileName)
                ps.setInt(11, track.listenInSec)
                ps.setString(12, track.coverOf)
                ps.setLong(13, track.durationSec)
                ps.executeUpdate()
                updateSearchIndex(
                    track.id,
                    "track",
                    track.aliases.ifEmpty { listOf(track.fileName) })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting track ${track.id}: ${e.message}")
            }
        }
        ps.close()
    }

    fun getAllTracks(): List<TrackDocument> {
        val rs = connection?.createStatement()?.executeQuery("SELECT * FROM tracks")
            ?: return emptyList()
        val tracks = mutableListOf<TrackDocument>()
        while (rs.next()) {
            tracks.add(
                TrackDocument(
                    id = rs.getString("id"),
                    created = rs.getLong("created"),
                    aliases = jsonToList(rs.getString("aliases")).toImmutableList(),
                    cover = rs.getString("cover"),
                    year = rs.getLong("year"),
                    album = rs.getString("album"),
                    numberInAlbum = rs.getLong("number_in_album"),
                    related = jsonToList(rs.getString("related")).toImmutableList(),
                    sourceFile = rs.getString("source_file"),
                    fileName = rs.getString("file_name"),
                    listenInSec = rs.getInt("listen_in_sec"),
                    coverOf = rs.getString("cover_of"),
                    durationSec = rs.getLong("duration_sec")
                )
            )
        }
        rs.close()
        return tracks
    }

    fun updateTrackListenInSec(track: TrackDocument) {
        val ps = connection?.prepareStatement("UPDATE tracks SET listen_in_sec = ? WHERE id = ?")
            ?: return
        ps.setInt(1, track.listenInSec)
        ps.setString(2, track.id)
        ps.executeUpdate()
        ps.close()
    }

    fun removeTracks(tracks: List<TrackDocument>) {
        val ps = connection?.prepareStatement("DELETE FROM tracks WHERE id = ?") ?: return
        tracks.forEach { track ->
            try {
                ps.setString(1, track.id)
                ps.executeUpdate()
                removeSearchIndex(track.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing track ${track.id}: ${e.message}")
            }
        }
        ps.close()
    }

    // ==================== Track-Creator edges ====================

    fun putTrackCreators(trackId: String, creatorIds: List<String>) {
        try {
            val del = connection?.prepareStatement("DELETE FROM track_creators WHERE track_id = ?")
            del?.setString(1, trackId)
            del?.executeUpdate()
            del?.close()

            val ins =
                connection?.prepareStatement("INSERT INTO track_creators (track_id, creator_id, ord) VALUES (?, ?, ?)")
            creatorIds.forEachIndexed { index, creatorId ->
                ins?.setString(1, trackId)
                ins?.setString(2, creatorId)
                ins?.setInt(3, index)
                ins?.executeUpdate()
            }
            ins?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error putting track-creator edges: ${e.message}")
        }
    }

    fun getTrackCreators(trackId: String): List<String> {
        val ps =
            connection?.prepareStatement("SELECT creator_id FROM track_creators WHERE track_id = ? ORDER BY ord")
                ?: return emptyList()
        ps.setString(1, trackId)
        val rs = ps.executeQuery()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            ids.add(rs.getString("creator_id"))
        }
        rs.close()
        ps.close()
        return ids
    }

    // ==================== Albums ====================

    fun putAlbums(albums: List<AlbumDocument>) {
        val ps = connection?.prepareStatement(
            """
            INSERT INTO albums (id, created, aliases, cover, year, file_name)
            VALUES (?, ?, ?::jsonb, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                created = EXCLUDED.created,
                aliases = EXCLUDED.aliases,
                cover = EXCLUDED.cover,
                year = EXCLUDED.year,
                file_name = EXCLUDED.file_name
        """
        ) ?: return
        albums.forEach { album ->
            try {
                ps.setString(1, album.id)
                ps.setLong(2, album.created)
                ps.setString(3, listToJson(album.aliases))
                ps.setString(4, album.cover)
                ps.setLong(5, album.year)
                ps.setString(6, album.fileName)
                ps.executeUpdate()
                updateSearchIndex(
                    album.id,
                    "album",
                    album.aliases.ifEmpty { listOf(album.fileName) })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting album ${album.id}: ${e.message}")
            }
        }
        ps.close()
    }

    fun getAllAlbums(): List<AlbumDocument> {
        val rs = connection?.createStatement()?.executeQuery("SELECT * FROM albums")
            ?: return emptyList()
        val albums = mutableListOf<AlbumDocument>()
        while (rs.next()) {
            albums.add(
                AlbumDocument(
                    id = rs.getString("id"),
                    created = rs.getLong("created"),
                    aliases = jsonToList(rs.getString("aliases")).toImmutableList(),
                    cover = rs.getString("cover"),
                    year = rs.getLong("year"),
                    fileName = rs.getString("file_name")
                )
            )
        }
        rs.close()
        return albums
    }

    fun removeAlbums(albums: List<AlbumDocument>) {
        val ps = connection?.prepareStatement("DELETE FROM albums WHERE id = ?") ?: return
        albums.forEach { album ->
            try {
                ps.setString(1, album.id)
                ps.executeUpdate()
                removeSearchIndex(album.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing album ${album.id}: ${e.message}")
            }
        }
        ps.close()
    }

    // ==================== Album edges ====================

    fun putAlbumCreators(albumId: String, creatorIds: List<String>) {
        try {
            val del = connection?.prepareStatement("DELETE FROM album_creators WHERE album_id = ?")
            del?.setString(1, albumId)
            del?.executeUpdate()
            del?.close()

            val ins =
                connection?.prepareStatement("INSERT INTO album_creators (album_id, creator_id, ord) VALUES (?, ?, ?)")
            creatorIds.forEachIndexed { index, creatorId ->
                ins?.setString(1, albumId)
                ins?.setString(2, creatorId)
                ins?.setInt(3, index)
                ins?.executeUpdate()
            }
            ins?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error putting album-creator edges: ${e.message}")
        }
    }

    fun putAlbumTracks(albumId: String, trackIds: List<String>) {
        try {
            val del = connection?.prepareStatement("DELETE FROM album_tracks WHERE album_id = ?")
            del?.setString(1, albumId)
            del?.executeUpdate()
            del?.close()

            val ins =
                connection?.prepareStatement("INSERT INTO album_tracks (album_id, track_id, ord) VALUES (?, ?, ?)")
            trackIds.forEachIndexed { index, trackId ->
                ins?.setString(1, albumId)
                ins?.setString(2, trackId)
                ins?.setInt(3, index)
                ins?.executeUpdate()
            }
            ins?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error putting album-track edges: ${e.message}")
        }
    }

    fun getAlbumCreators(albumId: String): List<String> {
        val ps =
            connection?.prepareStatement("SELECT creator_id FROM album_creators WHERE album_id = ? ORDER BY ord")
                ?: return emptyList()
        ps.setString(1, albumId)
        val rs = ps.executeQuery()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            ids.add(rs.getString("creator_id"))
        }
        rs.close()
        ps.close()
        return ids
    }

    fun getAlbumTracks(albumId: String): List<String> {
        val ps =
            connection?.prepareStatement("SELECT track_id FROM album_tracks WHERE album_id = ? ORDER BY ord")
                ?: return emptyList()
        ps.setString(1, albumId)
        val rs = ps.executeQuery()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            ids.add(rs.getString("track_id"))
        }
        rs.close()
        ps.close()
        return ids
    }

    // ==================== Playlists ====================

    fun putPlaylists(playlists: List<PlaylistDocument>) {
        val ps = connection?.prepareStatement(
            """
            INSERT INTO playlists (id, created, aliases, file_name)
            VALUES (?, ?, ?::jsonb, ?)
            ON CONFLICT (id) DO UPDATE SET
                created = EXCLUDED.created,
                aliases = EXCLUDED.aliases,
                file_name = EXCLUDED.file_name
        """
        ) ?: return
        playlists.forEach { playlist ->
            try {
                ps.setString(1, playlist.id)
                ps.setLong(2, playlist.created)
                ps.setString(3, listToJson(playlist.aliases))
                ps.setString(4, playlist.fileName)
                ps.executeUpdate()
                updateSearchIndex(
                    playlist.id,
                    "playlist",
                    playlist.aliases.ifEmpty { listOf(playlist.fileName) })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting playlist ${playlist.id}: ${e.message}")
            }
        }
        ps.close()
    }

    fun getAllPlaylists(): List<PlaylistDocument> {
        val rs = connection?.createStatement()?.executeQuery("SELECT * FROM playlists")
            ?: return emptyList()
        val playlists = mutableListOf<PlaylistDocument>()
        while (rs.next()) {
            playlists.add(
                PlaylistDocument(
                    id = rs.getString("id"),
                    created = rs.getLong("created"),
                    aliases = jsonToList(rs.getString("aliases")).toImmutableList(),
                    fileName = rs.getString("file_name")
                )
            )
        }
        rs.close()
        return playlists
    }

    fun putPlaylistTracks(playlistId: String, trackIds: List<String>) {
        try {
            val del =
                connection?.prepareStatement("DELETE FROM playlist_tracks WHERE playlist_id = ?")
            del?.setString(1, playlistId)
            del?.executeUpdate()
            del?.close()

            val ins =
                connection?.prepareStatement("INSERT INTO playlist_tracks (playlist_id, track_id, ord) VALUES (?, ?, ?)")
            trackIds.forEachIndexed { index, trackId ->
                ins?.setString(1, playlistId)
                ins?.setString(2, trackId)
                ins?.setInt(3, index)
                ins?.executeUpdate()
            }
            ins?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error putting playlist-track edges: ${e.message}")
        }
    }

    fun getPlaylistTracks(playlistId: String): List<String> {
        val ps =
            connection?.prepareStatement("SELECT track_id FROM playlist_tracks WHERE playlist_id = ? ORDER BY ord")
                ?: return emptyList()
        ps.setString(1, playlistId)
        val rs = ps.executeQuery()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            ids.add(rs.getString("track_id"))
        }
        rs.close()
        ps.close()
        return ids
    }

    fun removePlaylists(playlists: List<PlaylistDocument>) {
        val ps = connection?.prepareStatement("DELETE FROM playlists WHERE id = ?") ?: return
        playlists.forEach { playlist ->
            try {
                ps.setString(1, playlist.id)
                ps.executeUpdate()
                removeSearchIndex(playlist.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing playlist ${playlist.id}: ${e.message}")
            }
        }
        ps.close()
    }

    // ==================== Full-text search ====================

    fun search(query: String, limit: Int = 20): List<SearchResult> {
        val ps = connection?.prepareStatement(
            """
            SELECT entity_id, entity_type, name,
                   similarity(name, ?) AS rank
            FROM search_index
            WHERE name ILIKE ?
            ORDER BY rank DESC
            LIMIT ?
        """
        ) ?: return emptyList()
        ps.setString(1, query)
        ps.setString(2, "%$query%")
        ps.setInt(3, limit)
        val rs = ps.executeQuery()
        val results = mutableListOf<SearchResult>()
        while (rs.next()) {
            results.add(
                SearchResult(
                    entityId = rs.getString("entity_id"),
                    entityType = rs.getString("entity_type"),
                    name = rs.getString("name"),
                    rank = rs.getDouble("rank")
                )
            )
        }
        rs.close()
        ps.close()
        return results
    }

    fun rebuildSearchIndex() {
        val stmt = connection?.createStatement() ?: return
        stmt.executeUpdate("DELETE FROM search_index")
        stmt.close()

        getAllCreators().forEach { c ->
            updateSearchIndex(c.id, "creator", c.aliases.ifEmpty { listOf(c.fileName) })
        }
        getAllTracks().forEach { t ->
            updateSearchIndex(t.id, "track", t.aliases.ifEmpty { listOf(t.fileName) })
        }
        getAllAlbums().forEach { a ->
            updateSearchIndex(a.id, "album", a.aliases.ifEmpty { listOf(a.fileName) })
        }
        getAllPlaylists().forEach { p ->
            updateSearchIndex(p.id, "playlist", p.aliases.ifEmpty { listOf(p.fileName) })
        }
    }

    // ==================== JSONB helpers ====================

    private fun listToJson(list: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), list)

    private fun jsonToList(str: String?): List<String> =
        if (str.isNullOrBlank() || str == "null") emptyList()
        else json.decodeFromString(ListSerializer(String.serializer()), str)

    // ==================== AGE helpers (kept for graph queries) ====================

    private fun escapeForCypher(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun extractAgeProperties(jsonStr: String): String {
        val trimmed = jsonStr.trim()
        val start = trimmed.indexOf("\"properties\"")
        if (start == -1) return trimmed

        val colonIdx = trimmed.indexOf(':', start)
        if (colonIdx == -1) return trimmed

        var i = colonIdx + 1
        while (i < trimmed.length && trimmed[i].isWhitespace()) i++
        if (i >= trimmed.length) return trimmed

        if (trimmed[i] != '{') return trimmed
        var depth = 1
        var j = i + 1
        while (j < trimmed.length && depth > 0) {
            when (trimmed[j]) {
                '{' -> depth++
                '}' -> depth--
            }
            j++
        }
        return trimmed.substring(i, j)
    }

    private fun jsonToAgeProps(jsonStr: String): String {
        return jsonStr.replace(Regex("\"([a-zA-Z_][a-zA-Z0-9_]*)\":")) { match ->
            "${match.groupValues[1]}:"
        }
    }
}

data class SearchResult(
    val entityId: String,
    val entityType: String,
    val name: String,
    val rank: Double
)

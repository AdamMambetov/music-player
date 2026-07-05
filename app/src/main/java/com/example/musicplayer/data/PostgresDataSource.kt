package com.example.musicplayer.data

import android.util.Log
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

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

            val stmt = connection?.createStatement()
            val rs = stmt?.executeQuery("SELECT version()")
            if (rs?.next() == true) {
                Log.d(TAG, "PostgreSQL version: ${rs.getString(1)}")
            }
            rs?.close()
            stmt?.close()

            initAge()
            createSearchTable()
            true
        } catch (e: java.sql.SQLException) {
            Log.e(TAG, "SQL Error: ${e.message}")
            false
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to connect to PostgreSQL: ${e.javaClass.simpleName}: ${e.message}", e)
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

            // Проверяем, установлено ли уже расширение age
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
            stmt.execute("SET search_path = ag_catalog, \"\$user\", public")

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
            "SELECT * FROM ag_catalog.cypher('$GRAPH_NAME', \$\$$cypher\$\$) AS (result agtype)"
        )
    }

    private fun ageExec(cypher: String) {
        val stmt = connection?.createStatement() ?: return
        stmt.executeQuery(
            "SELECT * FROM ag_catalog.cypher('$GRAPH_NAME', \$\$$cypher\$\$) AS (result agtype)"
        )
        stmt.close()
    }

    private fun ageExecWithReturn(cypher: String): ResultSet? {
        val stmt = connection?.createStatement() ?: return null
        return stmt.executeQuery(
            "SELECT * FROM ag_catalog.cypher('$GRAPH_NAME', \$\$$cypher\$\$) AS (result agtype)"
        )
    }

    // ==================== Search table ====================

    private fun createSearchTable() {
        val stmt = connection?.createStatement() ?: return
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS search_index (
                entity_id TEXT PRIMARY KEY,
                entity_type TEXT NOT NULL,
                name TEXT NOT NULL,
                search_vector tsvector
            )
        """)
        stmt.executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_search_vector ON search_index USING GIN(search_vector)
        """)
        stmt.close()
    }

    private fun updateSearchIndex(entityId: String, entityType: String, name: String) {
        val ps = connection?.prepareStatement("""
            INSERT INTO search_index (entity_id, entity_type, name, search_vector)
            VALUES (?, ?, ?, to_tsvector('simple', ?))
            ON CONFLICT (entity_id) DO UPDATE SET
                entity_type = EXCLUDED.entity_type,
                name = EXCLUDED.name,
                search_vector = EXCLUDED.search_vector
        """) ?: return
        ps.setString(1, entityId)
        ps.setString(2, entityType)
        ps.setString(3, name)
        ps.setString(4, name)
        ps.executeUpdate()
        ps.close()
    }

    private fun removeSearchIndex(entityId: String) {
        val ps = connection?.prepareStatement("DELETE FROM search_index WHERE entity_id = ?") ?: return
        ps.setString(1, entityId)
        ps.executeUpdate()
        ps.close()
    }

    // ==================== Creators ====================

    fun putCreators(creators: List<CreatorDocument>) {
        creators.forEach { creator ->
            val jsonStr = json.encodeToString(CreatorDocument.serializer(), creator)

            try {
                ageExec("MERGE (c:Creator {id: '${escapeForCypher(creator.id)}'}) SET c = ${jsonToAgeProps(jsonStr)}")
                updateSearchIndex(creator.id, "creator", creator.aliases.getOrElse(0) { creator.fileName })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting creator ${creator.id}: ${e.message}")
            }
        }
    }

    fun getAllCreators(): List<CreatorDocument> {
        val rs = ageExecWithReturn("MATCH (c:Creator) RETURN c") ?: return emptyList()
        val list = mutableListOf<CreatorDocument>()
        while (rs.next()) {
            val jsonStr = rs.getString("result")
            jsonStr?.let {
                try {
                    list.add(json.decodeFromString(CreatorDocument.serializer(), it))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing creator JSON: ${e.message}")
                }
            }
        }
        rs.close()
        return list
    }

    fun updateCreatorListenInSec(creatorId: String) {
        try {
            ageExec("""
                MATCH (c:Creator {id: '$creatorId'})<-[:HAS_CREATOR]-(t:Track)
                WITH c, COALESCE(SUM(t.listenInSec), 0) AS totalListen
                SET c.listenInSec = totalListen
            """.trimIndent())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating creator listen: ${e.message}")
        }
    }

    fun removeCreators(creators: List<CreatorDocument>) {
        creators.forEach { creator ->
            try {
                ageExec("MATCH (c:Creator {id: '${escapeForCypher(creator.id)}'}) DETACH DELETE c")
                removeSearchIndex(creator.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing creator ${creator.id}: ${e.message}")
            }
        }
    }

    // ==================== Tracks ====================

    fun putTracks(tracks: List<TrackDocument>) {
        tracks.forEach { track ->
            val jsonStr = json.encodeToString(TrackDocument.serializer(), track)

            try {
                ageExec("MERGE (t:Track {id: '${escapeForCypher(track.id)}'}) SET t = ${jsonToAgeProps(jsonStr)}")
                updateSearchIndex(track.id, "track", track.aliases.getOrElse(0) { track.fileName })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting track ${track.id}: ${e.message}")
            }
        }
    }

    fun getAllTracks(): List<TrackDocument> {
        val rs = ageExecWithReturn("MATCH (t:Track) RETURN t") ?: return emptyList()
        val tracks = mutableListOf<TrackDocument>()
        while (rs.next()) {
            val jsonStr = rs.getString("result")
            jsonStr?.let {
                try {
                    tracks.add(json.decodeFromString(TrackDocument.serializer(), it))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing track JSON: ${e.message}")
                }
            }
        }
        rs.close()
        return tracks
    }

    fun updateTrackListenInSec(track: TrackDocument) {
        try {
            ageExec("MATCH (t:Track {id: '${escapeForCypher(track.id)}'}) SET t.listenInSec = ${track.listenInSec}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating track listen: ${e.message}")
        }
    }

    fun removeTracks(tracks: List<TrackDocument>) {
        tracks.forEach { track ->
            try {
                ageExec("MATCH (t:Track {id: '${escapeForCypher(track.id)}'}) DETACH DELETE t")
                removeSearchIndex(track.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing track ${track.id}: ${e.message}")
            }
        }
    }

    // ==================== Track-Creator edges ====================

    fun putTrackCreators(trackId: String, creatorIds: List<String>) {
        try {
            ageExec("MATCH (t:Track {id: '$trackId'}) OPTIONAL MATCH (t)-[r:HAS_CREATOR]->() DELETE r")
            creatorIds.forEach { creatorId ->
                ageExec("MATCH (t:Track {id: '$trackId'}), (c:Creator {id: '$creatorId'}) CREATE (t)-[:HAS_CREATOR]->(c)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error putting track-creator edges: ${e.message}")
        }
    }

    fun getTrackCreators(trackId: String): List<String> {
        val rs = ageExecWithReturn(
            "MATCH (t:Track {id: '$trackId'})-[:HAS_CREATOR]->(c:Creator) RETURN c.id"
        ) ?: return emptyList()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            rs.getString("result")?.let { ids.add(it) }
        }
        rs.close()
        return ids
    }

    // ==================== Albums ====================

    fun putAlbums(albums: List<AlbumDocument>) {
        albums.forEach { album ->
            val jsonStr = json.encodeToString(AlbumDocument.serializer(), album)

            try {
                ageExec("MERGE (a:Album {id: '${escapeForCypher(album.id)}'}) SET a = ${jsonToAgeProps(jsonStr)}")
                updateSearchIndex(album.id, "album", album.aliases.getOrElse(0) { album.fileName })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting album ${album.id}: ${e.message}")
            }
        }
    }

    fun getAllAlbums(): List<AlbumDocument> {
        val rs = ageExecWithReturn("MATCH (a:Album) RETURN a") ?: return emptyList()
        val albums = mutableListOf<AlbumDocument>()
        while (rs.next()) {
            val jsonStr = rs.getString("result")
            jsonStr?.let {
                try {
                    albums.add(json.decodeFromString(AlbumDocument.serializer(), it))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing album JSON: ${e.message}")
                }
            }
        }
        rs.close()
        return albums
    }

    fun removeAlbums(albums: List<AlbumDocument>) {
        albums.forEach { album ->
            try {
                ageExec("MATCH (a:Album {id: '${escapeForCypher(album.id)}'}) DETACH DELETE a")
                removeSearchIndex(album.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing album ${album.id}: ${e.message}")
            }
        }
    }

    // ==================== Album edges ====================

    fun putAlbumCreators(albumId: String, creatorIds: List<String>) {
        try {
            ageExec("MATCH (a:Album {id: '$albumId'}) OPTIONAL MATCH (a)-[r:HAS_CREATOR]->() DELETE r")
            creatorIds.forEach { creatorId ->
                ageExec("MATCH (a:Album {id: '$albumId'}), (c:Creator {id: '$creatorId'}) CREATE (a)-[:HAS_CREATOR]->(c)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error putting album-creator edges: ${e.message}")
        }
    }

    fun putAlbumTracks(albumId: String, trackIds: List<String>) {
        try {
            ageExec("MATCH (a:Album {id: '$albumId'}) OPTIONAL MATCH (a)-[r:CONTAINS_TRACK]->() DELETE r")
            trackIds.forEach { trackId ->
                ageExec("MATCH (a:Album {id: '$albumId'}), (t:Track {id: '$trackId'}) CREATE (a)-[:CONTAINS_TRACK]->(t)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error putting album-track edges: ${e.message}")
        }
    }

    fun getAlbumCreators(albumId: String): List<String> {
        val rs = ageExecWithReturn(
            "MATCH (a:Album {id: '$albumId'})-[:HAS_CREATOR]->(c:Creator) RETURN c.id"
        ) ?: return emptyList()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            rs.getString("result")?.let { ids.add(it) }
        }
        rs.close()
        return ids
    }

    fun getAlbumTracks(albumId: String): List<String> {
        val rs = ageExecWithReturn(
            "MATCH (a:Album {id: '$albumId'})-[:CONTAINS_TRACK]->(t:Track) RETURN t.id"
        ) ?: return emptyList()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            rs.getString("result")?.let { ids.add(it) }
        }
        rs.close()
        return ids
    }

    // ==================== Playlists ====================

    fun putPlaylists(playlists: List<PlaylistDocument>) {
        playlists.forEach { playlist ->
            val jsonStr = json.encodeToString(PlaylistDocument.serializer(), playlist)

            try {
                ageExec("MERGE (p:Playlist {id: '${escapeForCypher(playlist.id)}'}) SET p = ${jsonToAgeProps(jsonStr)}")
                updateSearchIndex(playlist.id, "playlist", playlist.aliases.getOrElse(0) { playlist.fileName })
            } catch (e: Exception) {
                Log.e(TAG, "Error putting playlist ${playlist.id}: ${e.message}")
            }
        }
    }

    fun getAllPlaylists(): List<PlaylistDocument> {
        val rs = ageExecWithReturn("MATCH (p:Playlist) RETURN p") ?: return emptyList()
        val playlists = mutableListOf<PlaylistDocument>()
        while (rs.next()) {
            val jsonStr = rs.getString("result")
            jsonStr?.let {
                try {
                    playlists.add(json.decodeFromString(PlaylistDocument.serializer(), it))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing playlist JSON: ${e.message}")
                }
            }
        }
        rs.close()
        return playlists
    }

    fun putPlaylistTracks(playlistId: String, trackIds: List<String>) {
        try {
            ageExec("MATCH (p:Playlist {id: '$playlistId'}) OPTIONAL MATCH (p)-[r:CONTAINS_TRACK]->() DELETE r")
            trackIds.forEach { trackId ->
                ageExec("MATCH (p:Playlist {id: '$playlistId'}), (t:Track {id: '$trackId'}) CREATE (p)-[:CONTAINS_TRACK]->(t)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error putting playlist-track edges: ${e.message}")
        }
    }

    fun getPlaylistTracks(playlistId: String): List<String> {
        val rs = ageExecWithReturn(
            "MATCH (p:Playlist {id: '$playlistId'})-[:CONTAINS_TRACK]->(t:Track) RETURN t.id"
        ) ?: return emptyList()
        val ids = mutableListOf<String>()
        while (rs.next()) {
            rs.getString("result")?.let { ids.add(it) }
        }
        rs.close()
        return ids
    }

    fun removePlaylists(playlists: List<PlaylistDocument>) {
        playlists.forEach { playlist ->
            try {
                ageExec("MATCH (p:Playlist {id: '${escapeForCypher(playlist.id)}'}) DETACH DELETE p")
                removeSearchIndex(playlist.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing playlist ${playlist.id}: ${e.message}")
            }
        }
    }

    // ==================== Full-text search ====================

    fun search(query: String, limit: Int = 20): List<SearchResult> {
        val ps = connection?.prepareStatement("""
            SELECT entity_id, entity_type, name,
                   ts_rank_cd(search_vector, plainto_tsquery('simple', ?)) AS rank
            FROM search_index
            WHERE search_vector @@ plainto_tsquery('simple', ?)
            ORDER BY rank DESC
            LIMIT ?
        """) ?: return emptyList()
        ps.setString(1, query)
        ps.setString(2, query)
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

    // ==================== JSON parsing helpers ====================

    private fun escapeForCypher(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    /**
     * Конвертирует JSON строку в формат свойств AGE.
     * Убирает кавычки с ключей, чтобы запрос работал корректно.
     * Example: {"key":"value"} -> {key:"value"}
     */
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

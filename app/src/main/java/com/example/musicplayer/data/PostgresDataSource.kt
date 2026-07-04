package com.example.musicplayer.data

import android.util.Log
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class PostgresDataSource(
    private val host: String = "localhost",
    private val port: Int = 5432,
    private val database: String = "music_player",
    private val user: String = "postgres",
    private val password: String = "postgres"
) {
    private var connection: Connection? = null

    companion object {
        private const val TAG = "PostgresDataSource"
        private const val GRAPH_NAME = "music"
    }

    fun connect(): Boolean {
        return try {
            Class.forName("org.postgresql.Driver")
            connection = DriverManager.getConnection(
                "jdbc:postgresql://$host:$port/$database",
                user,
                password
            )
            Log.d(TAG, "Connected to PostgreSQL")
            initAge()
            createSearchTable()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to PostgreSQL: ${e.message}")
            false
        }
    }

    fun isConnected(): Boolean {
        return try {
            connection?.isValid(5) == true
        } catch (e: Exception) {
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
        val stmt = connection?.createStatement() ?: return
        stmt.execute("CREATE EXTENSION IF NOT EXISTS age")
        stmt.execute("LOAD 'age'")
        stmt.execute("SET search_path = ag_catalog, \"\$user\", public")

        try {
            stmt.execute("SELECT * FROM ag_catalog.create_graph('$GRAPH_NAME')")
            Log.d(TAG, "AGE graph '$GRAPH_NAME' created")
        } catch (e: Exception) {
            Log.d(TAG, "AGE graph '$GRAPH_NAME' already exists")
        }
        stmt.close()
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
            val aliasesArray = creator.aliases.joinToString(",") { "\"$it\"" }
            val json = """
                {
                    "id": "${escapeJson(creator.id)}",
                    "aliases": [$aliasesArray],
                    "fileName": "${escapeJson(creator.fileName)}",
                    "listenInSec": ${creator.listenInSec},
                    "created": ${creator.created}
                }
            """.trimIndent()

            try {
                ageExec("MERGE (c:Creator {id: '${escapeJson(creator.id)}'}) SET c = $json")
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
            val json = rs.getString("result")
            json?.let { list.add(parseCreatorJson(it)) }
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
                ageExec("MATCH (c:Creator {id: '${escapeJson(creator.id)}'}) DETACH DELETE c")
                removeSearchIndex(creator.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing creator ${creator.id}: ${e.message}")
            }
        }
    }

    // ==================== Tracks ====================

    fun putTracks(tracks: List<TrackDocument>) {
        tracks.forEach { track ->
            val aliasesArray = track.aliases.joinToString(",") { "\"$it\"" }
            val relatedArray = track.related.joinToString(",") { "\"$it\"" }
            val json = """
                {
                    "id": "${escapeJson(track.id)}",
                    "aliases": [$aliasesArray],
                    "cover": "${escapeJson(track.cover)}",
                    "year": ${track.year},
                    "album": "${escapeJson(track.album)}",
                    "numberInAlbum": ${track.numberInAlbum},
                    "related": [$relatedArray],
                    "sourceFile": "${escapeJson(track.sourceFile)}",
                    "fileName": "${escapeJson(track.fileName)}",
                    "sourceUri": "${escapeJson(track.sourceUri)}",
                    "coverOf": "${escapeJson(track.coverOf)}",
                    "listenInSec": ${track.listenInSec},
                    "created": ${track.created}
                }
            """.trimIndent()

            try {
                ageExec("MERGE (t:Track {id: '${escapeJson(track.id)}'}) SET t = $json")
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
            val json = rs.getString("result")
            json?.let { tracks.add(parseTrackJson(it)) }
        }
        rs.close()
        return tracks
    }

    fun updateTrackListenInSec(track: TrackDocument) {
        try {
            ageExec("MATCH (t:Track {id: '${escapeJson(track.id)}'}) SET t.listenInSec = ${track.listenInSec}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating track listen: ${e.message}")
        }
    }

    fun removeTracks(tracks: List<TrackDocument>) {
        tracks.forEach { track ->
            try {
                ageExec("MATCH (t:Track {id: '${escapeJson(track.id)}'}) DETACH DELETE t")
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
            val aliasesArray = album.aliases.joinToString(",") { "\"$it\"" }
            val json = """
                {
                    "id": "${escapeJson(album.id)}",
                    "aliases": [$aliasesArray],
                    "cover": "${escapeJson(album.cover)}",
                    "year": ${album.year},
                    "fileName": "${escapeJson(album.fileName)}",
                    "created": ${album.created}
                }
            """.trimIndent()

            try {
                ageExec("MERGE (a:Album {id: '${escapeJson(album.id)}'}) SET a = $json")
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
            val json = rs.getString("result")
            json?.let { albums.add(parseAlbumJson(it)) }
        }
        rs.close()
        return albums
    }

    fun removeAlbums(albums: List<AlbumDocument>) {
        albums.forEach { album ->
            try {
                ageExec("MATCH (a:Album {id: '${escapeJson(album.id)}'}) DETACH DELETE a")
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
            val aliasesArray = playlist.aliases.joinToString(",") { "\"$it\"" }
            val json = """
                {
                    "id": "${escapeJson(playlist.id)}",
                    "aliases": [$aliasesArray],
                    "fileName": "${escapeJson(playlist.fileName)}",
                    "created": ${playlist.created}
                }
            """.trimIndent()

            try {
                ageExec("MERGE (p:Playlist {id: '${escapeJson(playlist.id)}'}) SET p = $json")
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
            val json = rs.getString("result")
            json?.let { playlists.add(parsePlaylistJson(it)) }
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
                ageExec("MATCH (p:Playlist {id: '${escapeJson(playlist.id)}'}) DETACH DELETE p")
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

    private fun parseCreatorJson(json: String): CreatorDocument {
        val id = extractJsonString(json, "id")
        val aliases = extractJsonArray(json, "aliases")
        val fileName = extractJsonString(json, "fileName")
        val listenInSec = extractJsonInt(json, "listenInSec")
        val created = extractJsonLong(json, "created")

        return CreatorDocument(
            id = id,
            aliases = aliases,
            fileName = fileName,
            listenInSec = listenInSec,
            created = created
        )
    }

    private fun parseTrackJson(json: String): TrackDocument {
        val id = extractJsonString(json, "id")
        val aliases = extractJsonArray(json, "aliases")
        val cover = extractJsonString(json, "cover")
        val year = extractJsonLong(json, "year")
        val album = extractJsonString(json, "album")
        val numberInAlbum = extractJsonLong(json, "numberInAlbum")
        val related = extractJsonArray(json, "related")
        val sourceFile = extractJsonString(json, "sourceFile")
        val fileName = extractJsonString(json, "fileName")
        val sourceUri = extractJsonString(json, "sourceUri")
        val coverOf = extractJsonString(json, "coverOf")
        val listenInSec = extractJsonInt(json, "listenInSec")
        val created = extractJsonLong(json, "created")

        return TrackDocument(
            id = id,
            aliases = aliases,
            cover = cover,
            year = year,
            album = album,
            numberInAlbum = numberInAlbum,
            related = related,
            sourceFile = sourceFile,
            fileName = fileName,
            sourceUri = sourceUri,
            coverOf = coverOf,
            listenInSec = listenInSec,
            created = created,
            creators = emptyList()
        )
    }

    private fun parseAlbumJson(json: String): AlbumDocument {
        val id = extractJsonString(json, "id")
        val aliases = extractJsonArray(json, "aliases")
        val cover = extractJsonString(json, "cover")
        val year = extractJsonLong(json, "year")
        val fileName = extractJsonString(json, "fileName")
        val created = extractJsonLong(json, "created")

        return AlbumDocument(
            id = id,
            aliases = aliases,
            cover = cover,
            year = year,
            fileName = fileName,
            created = created,
            creators = emptyList(),
            tracklist = emptyList()
        )
    }

    private fun parsePlaylistJson(json: String): PlaylistDocument {
        val id = extractJsonString(json, "id")
        val aliases = extractJsonArray(json, "aliases")
        val fileName = extractJsonString(json, "fileName")
        val created = extractJsonLong(json, "created")

        return PlaylistDocument(
            id = id,
            aliases = aliases,
            fileName = fileName,
            created = created,
            tracklist = emptyList()
        )
    }

    // ==================== Simple JSON extraction ====================

    private fun extractJsonString(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun extractJsonArray(json: String, key: String): List<String> {
        val pattern = "\"$key\"\\s*:\\s*\\[([^\\]]*)\\]".toRegex()
        val match = pattern.find(json) ?: return emptyList()
        val content = match.groupValues[1]
        if (content.isBlank()) return emptyList()
        return content.split(",").map { it.trim().removeSurrounding("\"") }
    }

    private fun extractJsonLong(json: String, key: String): Long {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private fun extractJsonInt(json: String, key: String): Int {
        return extractJsonLong(json, key).toInt()
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}

data class SearchResult(
    val entityId: String,
    val entityType: String,
    val name: String,
    val rank: Double
)

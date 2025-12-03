package com.example.musicplayer.mdreader

import android.util.Log
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.TrackDocument

class MarkdownReader(val pathHelper: PathHelper) : MarkdownReaderBase() {
    companion object {
        // Yaml FrontMatter keys
        private const val CREATED_KEY = "created"
        private const val ALIASES_KEY = "aliases"
        private const val COVER_KEY = "Cover"
        private const val YEAR_KEY = "Year"
        private const val ALBUM_KEY = "Album"
        private const val CREATORS_KEY = "Creators"
        private const val NUMBER_IN_ALBUM_KEY = "NumberInAlbum"
        private const val RELATED_KEY = "related"
        private const val SOURCE_FILE_KEY = "SourceFile"
        private const val LISTEN_IN_SEC_KEY = "ListenInSec"
        private const val TRACKLIST_KEY = "tracklist"
        private const val COVER_OF_KEY = "CoverOf"
    }


    fun scanTracks(allCreators: List<CreatorDocument>): List<TrackDocument> {
        val result = mutableListOf<TrackDocument>()
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.TRACKS_FOLDER_NAME_IN_NOTES,
        )

        for (file in files) {
            val track = createTrackFromMarkdown(
                filename = file.name,
                markdownContent = readFile(file),
                allCreators = allCreators,
            )
            if (track.isValid())
                result.add(track)
        }
        return result
    }

    fun scanCreators(): List<CreatorDocument> {
        val result = mutableListOf<CreatorDocument>()
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.CREATORS_FOLDER_NAME_IN_NOTES,
        )

        for (file in files) {
            result.add(
                createCreatorFromMarkdown(
                    filename = file.name,
                    markdownContent = readFile(file),
                )
            )
        }
        return result
    }

    fun scanAlbums(
        allCreators: List<CreatorDocument>,
        allTracks: List<TrackDocument>,
    ): List<AlbumDocument> {
        val result = mutableListOf<AlbumDocument>()
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.ALBUMS_FOLDER_NAME_IN_NOTES,
        )

        for (file in files) {
            result.add(
                createAlbumFromMarkdown(
                    filename = file.name,
                    markdownContent = readFile(file),
                    allCreators = allCreators,
                    allTracks = allTracks,
                )
            )
        }
        return result
    }

    fun scanPlaylists(allTracks: List<TrackDocument>): List<PlaylistDocument> {
        val result = mutableListOf<PlaylistDocument>()
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.PLAYLISTS_FOLDER_NAME_IN_NOTES,
        )

        for (file in files) {
            result.add(
                createPlaylistFromMarkdown(
                    filename = file.name,
                    markdownContent = readFile(file),
                    allTracks = allTracks,
                )
            )
        }
        return result
    }

    fun createTrackFromMarkdown(
        filename: String,
        markdownContent: String,
        allCreators: List<CreatorDocument>,
    ): TrackDocument {
        val yamlData = parseYamlFrontMatter(markdownContent)
        val aliases = stringArrayToList(yamlData[ALIASES_KEY].orEmpty())

        return TrackDocument(
            created = getDateFromString(source = yamlData[CREATED_KEY].orEmpty()).timeInMillis,
            aliases = aliases,
            lowerAliases = aliases.map { it.lowercase() },
            upperAliases = aliases.map { it.uppercase() },
            cover = unLink(yamlData[COVER_KEY].orEmpty()),
            creators = stringArrayToList(yamlData[CREATORS_KEY].orEmpty())
                .mapNotNull { creatorName ->
                    allCreators.find { it.fileName == unLink(creatorName) }
                },
            year = yamlData[YEAR_KEY]?.toLongOrNull() ?: 0L,
            sourceFile = unLink(yamlData[SOURCE_FILE_KEY].orEmpty()),
            listenInSec = yamlData[LISTEN_IN_SEC_KEY]?.toIntOrNull() ?: 0,
            album = unLink(yamlData[ALBUM_KEY].orEmpty()),
            numberInAlbum = yamlData[NUMBER_IN_ALBUM_KEY]?.toLongOrNull() ?: 0L,
            related = stringArrayToList(yamlData[RELATED_KEY].orEmpty()).map { unLink(it) },
            fileName = filename.removeSuffix(".md"),
            coverOf = unLink(yamlData[COVER_OF_KEY].orEmpty())
        )
    }

    fun createCreatorFromMarkdown(
        filename: String,
        markdownContent: String,
    ): CreatorDocument {
        val yamlData = parseYamlFrontMatter(markdownContent)
        val aliases = stringArrayToList(yamlData[ALIASES_KEY].orEmpty())

        return CreatorDocument(
            created = getDateFromString(source = yamlData[CREATED_KEY].orEmpty()).timeInMillis,
            aliases = aliases,
            lowerAliases = aliases.map { it.lowercase() },
            upperAliases = aliases.map { it.uppercase() },
            fileName = filename.removeSuffix(".md"),
            listenInSec = yamlData[LISTEN_IN_SEC_KEY]?.toIntOrNull() ?: 0,
        )
    }

    fun createAlbumFromMarkdown(
        filename: String,
        markdownContent: String,
        allCreators: List<CreatorDocument>,
        allTracks: List<TrackDocument>,
    ): AlbumDocument {
        val yamlData = parseYamlFrontMatter(markdownContent)
        val aliases = stringArrayToList(yamlData[ALIASES_KEY].orEmpty())

        return AlbumDocument(
            created = getDateFromString(source = yamlData[CREATED_KEY].orEmpty()).timeInMillis,
            aliases = aliases,
            lowerAliases = aliases.map { it.lowercase() },
            upperAliases = aliases.map { it.uppercase() },
            cover = unLink(yamlData[COVER_KEY].orEmpty()),
            year = yamlData[YEAR_KEY]?.toLongOrNull() ?: 0L,
            creators = stringArrayToList(yamlData[CREATORS_KEY].orEmpty())
                .mapNotNull { creatorName ->
                    allCreators.find { it.fileName == unLink(creatorName) }
                },
            tracklist = stringArrayToList(yamlData[TRACKLIST_KEY].orEmpty())
                .mapNotNull { trackName ->
                    allTracks.find { it.fileName == trackName }
                },
            fileName = filename.removeSuffix(".md"),
        )
    }

    fun createPlaylistFromMarkdown(
        filename: String,
        markdownContent: String,
        allTracks: List<TrackDocument>,
    ): PlaylistDocument {
        val yamlData = parseYamlFrontMatter(markdownContent)
        val aliases = stringArrayToList(yamlData[ALIASES_KEY].orEmpty())

        return PlaylistDocument(
            created = getDateFromString(source = yamlData[CREATED_KEY].orEmpty()).timeInMillis,
            aliases = aliases,
            lowerAliases = aliases.map { it.lowercase() },
            upperAliases = aliases.map { it.uppercase() },
            tracklist = stringArrayToList(yamlData[TRACKLIST_KEY].orEmpty())
                .mapNotNull { trackName ->
                    allTracks.find { it.fileName == unLink(trackName) }
                },
            fileName = filename.removeSuffix(".md"),
        )
    }

    fun saveTrack(track: TrackDocument) {
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.TRACKS_FOLDER_NAME_IN_NOTES,
        )
        val file = files.find { it.name == track.fileName + ".md" }
        if (file == null) {
            Log.e("TAG", "saveTrack: Not find file '${track.fileName}'")
            return
        }

        val yamlMap = mapOf(
            CREATED_KEY         to  track.getCreatedString(),
            ALIASES_KEY         to  listToStringArray(track.aliases),
            COVER_KEY           to  toLink(track.cover),
            YEAR_KEY            to  track.year.toString(),
            ALBUM_KEY           to  toLink(track.album),
            CREATORS_KEY        to  listToStringArray(
                                        list = track.creators.map { toLink(it.fileName) }),
            NUMBER_IN_ALBUM_KEY to  track.numberInAlbum.toString(),
            RELATED_KEY         to  listToStringArray(
                                        list = track.related.map { toLink(it) }),
            SOURCE_FILE_KEY     to  toLink(track.sourceFile),
            LISTEN_IN_SEC_KEY   to  track.listenInSec.toString(),
            COVER_OF_KEY        to  toLink(track.coverOf),
        )
        saveMarkdown(file, yamlMap)
    }

    fun saveCreator(creator: CreatorDocument) {
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.CREATORS_FOLDER_NAME_IN_NOTES,
        )
        val file = files.find { it.name == creator.fileName + ".md" }
        if (file == null) {
            Log.d("TAG", "saveCreator: Not find file '${creator.fileName}'")
            return
        }

        val yamlMap = mapOf(
            CREATED_KEY         to  creator.getCreatedString(),
            ALIASES_KEY         to  listToStringArray(creator.aliases),
            LISTEN_IN_SEC_KEY   to  creator.listenInSec.toString(),
        )
        saveMarkdown(file, yamlMap)
    }

    fun saveAlbum(album: AlbumDocument) {
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.ALBUMS_FOLDER_NAME_IN_NOTES,
        )
        val file = files.find { it.name == album.fileName + ".md" }
        if (file == null) {
            Log.e("TAG", "saveAlbum: Not find file '${album.fileName}'")
            return
        }

        val yamlMap = mapOf(
            CREATED_KEY     to  album.getCreatedString(),
            ALIASES_KEY     to  listToStringArray(album.aliases),
            COVER_KEY       to  toLink(album.cover),
            YEAR_KEY        to  album.year.toString(),
            CREATORS_KEY    to  listToStringArray(
                                    list = album.creators.map { toLink(it.fileName) }),
            TRACKLIST_KEY   to  listToStringArray(
                                    list = album.tracklist.map { toLink(it.fileName) }),
        )
        saveMarkdown(file, yamlMap)
    }

    fun savePlaylist(playlist: PlaylistDocument) {
        val files = pathHelper.scanFolderInNotesDir(
            folderName = PathHelper.PLAYLISTS_FOLDER_NAME_IN_NOTES,
        )
        val file = files.find { it.name == playlist.fileName + ".md" }
        if (file == null) {
            Log.e("TAG", "savePlaylist: Not find file '${playlist.fileName}'")
            return
        }

        val yamlMap = mapOf(
            CREATED_KEY     to  playlist.getCreatedString(),
            ALIASES_KEY     to  listToStringArray(playlist.aliases),
            TRACKLIST_KEY   to  listToStringArray(
                                    list = playlist.tracklist.map { toLink(it.fileName) }),
        )
        saveMarkdown(file, yamlMap)
    }
}
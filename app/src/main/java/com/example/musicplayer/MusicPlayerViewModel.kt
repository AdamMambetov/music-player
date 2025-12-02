package com.example.musicplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.data.TrackListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.random.Random

class MusicPlayerViewModel(
    context: Context? = null,
    val searchManager: MusicPlayerSearchManager
) : ViewModel() {
    var isPlaying by mutableStateOf(false)
        private set

    var isShuffle by mutableStateOf(false)
        private set

    var isFavorite by mutableStateOf(false)
        private set

    var isScan by mutableStateOf(false)
        private set

    var currentPosition by mutableLongStateOf(0L)
        private set

    var duration by mutableLongStateOf(0L)
        private set

    var allTracks = mutableStateListOf<TrackDocument>()
        private set

    var allCreators = mutableStateListOf<CreatorDocument>()
        private set

    var allAlbums = mutableStateListOf<AlbumDocument>()
        private set

    var allPlaylists = mutableStateListOf<PlaylistDocument>()
        private set

    var currentTrack by mutableStateOf(TrackDocument.createEmpty())
        private set

    var currentAlbum by mutableStateOf(AlbumDocument.createEmpty())
        private set

    var currentPlaylist by mutableStateOf(PlaylistDocument.createEmpty())

    var favorites by mutableStateOf(PlaylistDocument.createEmpty())
        private set

    var currentQueue = mutableListOf<TrackDocument>()

    var currentQueueIndex by mutableIntStateOf(-1)

    var randomQueue = mutableListOf<TrackDocument>()
        private set

    var randomQueueIndex by mutableIntStateOf(-1)
        private set

    var random = Random(Random.nextLong())
        private set

    var musicState by mutableStateOf(TrackListState())
        private set

    private var playerTimer: CountDownTimer? = null

    private var lastListenModifiedTimeInMillis: Long = Long.MAX_VALUE

    private var searchJob: Job? = null
    private var scanJob: Job? = null

    // private var _mediaController: MediaController? = null
    private var _exoPlayer: ExoPlayer? = null

    init {
        viewModelScope.launch {
            searchManager.init()
            if (context != null)
                loadAllFromCache(context)
        }
    }

    override fun onCleared() {
//        _mediaController?.release()
        _exoPlayer?.release()
        searchJob?.cancel()
        searchJob = null
        scanJob?.cancel()
        scanJob = null
        isScan = false
        searchManager.closeSession()
        super.onCleared()
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer(context: Context) {
        _exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // Handle audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        _exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@MusicPlayerViewModel.isPlaying = isPlaying
                if (isPlaying)
                    startTimer(context)
                else
                    playerTimer?.cancel()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                if (playbackState == Player.STATE_READY) {
                    duration = _exoPlayer?.duration ?: 0L
                    lastListenModifiedTimeInMillis = duration
                }
                if (playbackState == Player.STATE_ENDED) {
                    nextTrack()
                    lastListenModifiedTimeInMillis = Long.MAX_VALUE
                    playerTimer?.cancel()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                currentPosition = _exoPlayer?.currentPosition ?: 0L
                Log.d("TAG", "onPositionDiscontinuity: $oldPosition - $newPosition")
            }
        })
        _exoPlayer?.prepare()

        viewModelScope.launch {
//            val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
//            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            
//            controllerFuture.addListener({
//                _mediaController = controllerFuture.get()
//
//                // Add listener to update position and duration
//                _mediaController?.addListener(object : Player.Listener {
//                    override fun onIsPlayingChanged(isPlaying: Boolean) {
//                        this@MusicPlayerViewModel.isPlaying = isPlaying
//                        if (isPlaying)
//                            startTimer(context)
//                        else
//                            playerTimer?.cancel()
//                    }
//
//                    override fun onPlaybackStateChanged(playbackState: Int) {
//                        super.onPlaybackStateChanged(playbackState)
//
//                        if (playbackState == Player.STATE_READY) {
//                            duration = _mediaController?.duration ?: 0L
//                            lastListenModifiedTimeInMillis = duration
//                        }
//                        if (playbackState == Player.STATE_ENDED) {
//                            nextTrack()
//                            lastListenModifiedTimeInMillis = Long.MAX_VALUE
//                            playerTimer?.cancel()
//                        }
//                    }
//
//                    override fun onPositionDiscontinuity(
//                        oldPosition: Player.PositionInfo,
//                        newPosition: Player.PositionInfo,
//                        reason: Int
//                    ) {
//                        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
//                        currentPosition = _mediaController?.currentPosition ?: 0L
//                        Log.d("TAG", "onPositionDiscontinuity: $oldPosition - $newPosition")
//                    }
//                })
//            }, { it.run() })
        }
    }

    fun scanAll(context: Context, clearCache: Boolean) {
        Log.d("TAG", "Rescan start")
        scanJob?.cancel()
        scanJob = viewModelScope.launch(context = Dispatchers.IO) {
            isScan = true
            val markdownReader = MarkdownReader()
            if (clearCache) {
                searchManager.removeTracks(searchManager.searchAllTracks())
                searchManager.removeCreators(searchManager.searchAllCreators())
                searchManager.removeAlbums(searchManager.searchAllAlbums())
                searchManager.removePlayLists(searchManager.searchAllPlaylists())
                allCreators.clear()
                allTracks.clear()
                allAlbums.clear()
                allPlaylists.clear()
            }

            val creators = markdownReader.scanCreators(context).toMutableList()

            for (i in 0..<creators.size) {
                val oldCreator = allCreators.find { it == creators[i] }
                if (oldCreator == null)
                    continue
                creators[i] = creators[i].copy(id = oldCreator.id)
            }

            allCreators.clear()
            searchManager.putCreators(creators)
            allCreators.addAll(creators)
            Log.d("TAG", "Finish scan creators from markdown")

            var tracks = markdownReader
                .scanTracks(context, allCreators)
                .sortedByDescending { it.created }
                .toMutableList()
            Log.d("TAG", "Finish scan tracks from markdown")

            for (i in 0..<tracks.size) {
                val oldTrack = allTracks.find { it == tracks[i] }
                if (oldTrack == null)
                    continue
                tracks[i] = tracks[i].copy(id = oldTrack.id)
            }

            tracks = scanSourceTracks(context, tracks).toMutableList()
            Log.d("TAG", "Finish scan tracks from source")

            allTracks.clear()
            allTracks.addAll(tracks)
            searchManager.putTracks(allTracks)
            Log.d("TAG", "Finish scan tracks")

            val albums = markdownReader.scanAlbums(context, allCreators, allTracks).toMutableList()

            for (i in 0..<albums.size) {
                val oldAlbum = allAlbums.find { it == albums[i] }
                if (oldAlbum == null)
                    continue
                albums[i] = albums[i].copy(id = oldAlbum.id)
            }

            searchManager.putAlbums(albums)
            allAlbums.clear()
            allAlbums.addAll(albums)
            Log.d("TAG", "Finish scan albums from markdown")

            val playlists = markdownReader.scanPlaylists(context, allTracks).toMutableList()

            for (i in 0..<playlists.size) {
                val oldPlaylist = allPlaylists.find { it == playlists[i] }
                if (oldPlaylist == null)
                    continue
                playlists[i] = playlists[i].copy(id = oldPlaylist.id)
            }

            searchManager.putPlaylists(playlists)
            allPlaylists.clear()
            allPlaylists.addAll(playlists)
            favorites = allPlaylists.find {
                it.aliases.getOrElse(0) { false } == "Favorites"
            } ?: favorites
            Log.d("TAG", "Finish scan playlists from markdown")
            Log.d("TAG", "Rescan end")
            isScan = false
        }
    }

    private fun scanSourceTracks(context: Context, tracks: List<TrackDocument>): List<TrackDocument> {
        val musicPathString = getTracksFolderPath(context)
        if (musicPathString.isEmpty())
            return emptyList()
        if (tracks.isEmpty())
            return emptyList()

        val uri = musicPathString.toUri()
        val queryUri = MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selectionPath = uri.lastPathSegment!!.substringAfter(":") + "/"
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(
            selectionPath,
        )

        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val fileName = cursor.getString(displayNameColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
                tracks.find {
                    musicInfo -> musicInfo.sourceFile == fileName
                }?.sourceUri = contentUri.toString()
            }
        }
        return tracks
    }

    fun loadAllFromCache(context: Context) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(context = Dispatchers.IO) {
            isScan = true
            allCreators.clear()
            allCreators.addAll(searchManager.searchAllCreators())
            Log.d("TAG", "Finish scan creators from cache ${allCreators.size}")

            allTracks.clear()
            val tracks = searchManager.searchAllTracks()
            allTracks.addAll(scanSourceTracks(context, tracks))
            Log.d("TAG", "Finish scan tracks from cache ${allTracks.size}")

            allAlbums.clear()
            allAlbums.addAll(searchManager.searchAllAlbums())
            Log.d("TAG", "Finish scan albums from cache ${allAlbums.size}")

            allPlaylists.clear()
            allPlaylists.addAll(searchManager.searchAllPlaylists())
            favorites = allPlaylists.find {
                it.aliases.getOrElse(0) { "" } == "Favorites"
            } ?: favorites
            Log.d("TAG", "Finish scan playlists from cache ${allPlaylists.size} ${favorites.tracklist.size}")
            isScan = false
        }
    }

    fun setMediaSourceWithService(track: TrackDocument) {
        if (track.id != currentTrack.id) {
            Log.d("TAG", "Source Uri: ${track.sourceUri}")
            currentTrack = track
            isFavorite = favorites.tracklist.find { it == track } != null
            currentQueueIndex = currentQueue.indexOf(currentTrack)
            if (isShuffle) {
                randomQueueIndex = randomQueue.indexOf(currentTrack)
            }
            _exoPlayer?.setMediaItem(MediaItem.fromUri(track.sourceUri))
        }
    }

    private fun startTimer(context: Context) {
        if (duration == 0L)
            return
        playerTimer?.cancel()
        playerTimer = object : CountDownTimer(
            duration-currentPosition, 100L
        ) {
            override fun onFinish() {
                currentPosition = duration
                lastListenModifiedTimeInMillis = 0L
            }

            override fun onTick(millisUntilFinished: Long) {
                currentPosition = duration - millisUntilFinished
                if (lastListenModifiedTimeInMillis - millisUntilFinished >= 1000L) {
                    lastListenModifiedTimeInMillis = millisUntilFinished
                    currentTrack.listenInSec++
                    val markdownReader = MarkdownReader()
                    markdownReader.saveTrack(context, currentTrack)
                    currentTrack.creators.forEach {
                        it.listenInSec++
                    }
                    viewModelScope.launch {
                        searchManager.putTracks(listOf(currentTrack))
                        searchManager.putCreators(currentTrack.creators)
                    }
                }
            }

        }.start()
    }

    fun play(context: Context) {
        _exoPlayer?.play()
        isPlaying = true
        startTimer(context)
    }

    fun pause() {
        _exoPlayer?.pause()
        isPlaying = false
        playerTimer?.cancel()
    }

    fun seekTo(context: Context, position: Long) {
        _exoPlayer?.seekTo(position)
        currentPosition = position
        lastListenModifiedTimeInMillis = duration - currentPosition
        if (isPlaying)
            startTimer(context)
    }

    fun nextTrack() {
        val queue = if (isShuffle) randomQueue else currentQueue
        val index = if (isShuffle) randomQueueIndex else currentQueueIndex
        Log.d("TAG", "nextTrack $queue, $index")

        setMediaSourceWithService(
            queue.getOrElse(index + 1) { return }
        )
    }

    fun previousTrack() {
        val queue = if (isShuffle) randomQueue else currentQueue
        val index = if (isShuffle) randomQueueIndex else currentQueueIndex

        setMediaSourceWithService(
            queue.getOrElse(index - 1) { return }
        )
    }

    fun enableShuffle() {
        isShuffle = true
        randomQueue = currentQueue.toMutableList()
        randomQueue.shuffle(random)
        randomQueueIndex = randomQueue.indexOf(currentQueue[currentQueueIndex])
    }

    fun disableShuffle() {
        isShuffle = false
        randomQueue.clear()
        randomQueueIndex = -1
    }

    fun setQueueToDefault() {
        currentQueue = allTracks.toMutableList()
    }

    fun onSearchQueryChange(query: String) {
        musicState = musicState.copy(searchQuery = query)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
//            delay(500L)
            val musicList = searchManager.searchTracks(query)
            musicState = musicState.copy(trackList = musicList)
        }
    }

    fun addToFavorites(context: Context, track: TrackDocument) {
        if (isFavorite)
            return
        isFavorite = true

        val list = favorites.tracklist.toMutableList()
        list.add(track)
        favorites.tracklist = list
        savePlaylist(context, favorites)
    }

    fun removeFromFavorites(context: Context, track: TrackDocument) {
        if (!isFavorite)
            return
        isFavorite = false

        val list = favorites.tracklist.toMutableList()
        list.remove(track)
        favorites.tracklist = list
        savePlaylist(context, favorites)
    }

    fun changeTrackFavoriteState(context: Context, track: TrackDocument) {
        if (isFavorite)
            removeFromFavorites(context, track)
        else
            addToFavorites(context, track)
    }

    fun savePlaylist(context: Context, playlist: PlaylistDocument) {
        Log.d("TAG", "savePlaylist ${playlist.fileName} - ${playlist.tracklist.size}")
        val index = allPlaylists.indexOfFirst { it == playlist }
        if (index == -1)
            return
        allPlaylists[index] = playlist.copy(id = allPlaylists[index].id)
        viewModelScope.launch(Dispatchers.IO) {
            MarkdownReader().savePlaylist(context, allPlaylists[index])
            searchManager.putPlaylists(listOf(allPlaylists[index]))
        }
        Log.d("TAG", "savePlaylist success")
    }
}
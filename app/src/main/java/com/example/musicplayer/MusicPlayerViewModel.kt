package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.PostgresDataSource
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.data.TrackListState
import com.example.musicplayer.mdreader.MarkdownReader
import com.example.musicplayer.mdreader.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.random.Random

class MusicPlayerViewModel(
    @field:SuppressLint("StaticFieldLeak") val context: Context
) : ViewModel() {
    var isPlaying by mutableStateOf(false)
        private set

    var isShuffle by mutableStateOf(false)
        private set

    var isRepeat by mutableStateOf(false)
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

    var currentListenInSec by androidx.compose.runtime.mutableIntStateOf(0)

    var currentAlbum by mutableStateOf(AlbumDocument.createEmpty())
        internal set

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

    private var coverUris = mutableMapOf<String, String>()

    private var playerTimer: CountDownTimer? = null

    private var lastListenModifiedTimeInMillis: Long = Long.MAX_VALUE

    private var searchJob: Job? = null
    private var scanJob: Job? = null

    private var _mediaController: MediaController? = null

    val pathHelper = PathHelper(context = context)
    private val markdownReader = MarkdownReader(pathHelper = pathHelper)
    private val mediaReader = MediaReader(context = context)
    private val postgres = PostgresDataSource()

    val repository = MusicRepository(
        markdownReader = markdownReader,
        postgres = postgres,
        mediaReader = mediaReader,
        pathHelper = pathHelper
    )

    init {
        onNextTrack = { nextTrack() }
        onPreviousTrack = { previousTrack() }
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting PostgreSQL connection...")
                repository.connectPostgres()
                Log.d(TAG, "PostgreSQL connection finished")
            } catch (e: Exception) {
                Log.e(TAG, "PostgreSQL connection failed", e)
            }
            loadAllFromCache()
        }
    }

    override fun onCleared() {
        onNextTrack = null
        onPreviousTrack = null
        _mediaController?.release()
        _mediaController = null
        searchJob?.cancel()
        searchJob = null
        scanJob?.cancel()
        scanJob = null
        isScan = false
        postgres.close()
        super.onCleared()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun initializePlayer() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            _mediaController = controllerFuture.get()

            _mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    this@MusicPlayerViewModel.isPlaying = isPlaying
                    if (isPlaying)
                        startTimer()
                    else
                        playerTimer?.cancel()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)

                    if (playbackState == Player.STATE_READY) {
                        duration = _mediaController?.duration ?: 0L
                        lastListenModifiedTimeInMillis = duration
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        if (isRepeat) {
                            seekTo(0L)
                            play()
                        } else {
                            nextTrack()
                            lastListenModifiedTimeInMillis = Long.MAX_VALUE
                            playerTimer?.cancel()
                        }
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                    currentPosition = _mediaController?.currentPosition ?: 0L
                    Log.d(TAG, "onPositionDiscontinuity: $oldPosition - $newPosition")
                }
            })
        }, { it.run() })
    }

    fun scanAll(clearCache: Boolean) {
        Log.d(TAG, "Rescan start")
        scanJob?.cancel()

        if (clearCache) {
            allCreators.clear()
            allTracks.clear()
            allAlbums.clear()
            allPlaylists.clear()
            favorites = com.example.musicplayer.data.PlaylistDocument.createEmpty()
            currentQueue.clear()
            currentQueueIndex = -1
            randomQueue.clear()
            randomQueueIndex = -1
            currentTrack = com.example.musicplayer.data.TrackDocument.createEmpty()
            musicState = com.example.musicplayer.data.TrackListState()
            coverUris.clear()
            MusicPlayerService.coverUriMap.clear()
        }

        scanJob = viewModelScope.launch {
            isScan = true
            val result = repository.scanAll(clearCache)

            allCreators.clear()
            allCreators.addAll(result.creators)

            allTracks.clear()
            allTracks.addAll(result.tracks)

            allAlbums.clear()
            allAlbums.addAll(result.albums)

            allPlaylists.clear()
            allPlaylists.addAll(result.playlists)

            favorites = result.favorites ?: favorites

            Log.d(TAG, "Rescan end: ${result.tracks.size} tracks, ${result.creators.size} creators, ${result.albums.size} albums, ${result.playlists.size} playlists")
            scanCovers()
            isScan = false
        }
    }

    fun loadAllFromCache() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(context = Dispatchers.IO) {
            isScan = true

            val creators = repository.loadAllCreators()
            allCreators.clear()
            allCreators.addAll(creators)
            Log.d(TAG, "Loaded creators: ${creators.size}")

            val tracks = repository.loadAllTracks(creators)
            allTracks.clear()
            allTracks.addAll(tracks)
            Log.d(TAG, "Loaded tracks: ${tracks.size}")

            val albums = repository.loadAllAlbums(creators, tracks)
            allAlbums.clear()
            allAlbums.addAll(albums)
            Log.d(TAG, "Loaded albums: ${albums.size}")

            val playlists = repository.loadAllPlaylists(tracks)
            allPlaylists.clear()
            allPlaylists.addAll(playlists)
            favorites = allPlaylists.find {
                it.aliases.getOrElse(0) { "" } == "Favorites"
            } ?: favorites
            Log.d(TAG, "Loaded playlists: ${playlists.size}, favorites: ${favorites.tracklist.size}")

            scanCovers()
            isScan = false

            if (pathHelper.isReplayGainEnabled()) {
                analyzeAllTracksInBackground(tracks)
            }
        }
    }

    fun analyzeAllTracksInBackground(tracks: List<TrackDocument> = allTracks.toList()) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting batch analysis of ${tracks.size} tracks...")
            repository.analyzeAllTracks(tracks, context) { analyzed, total ->
                Log.d(TAG, "Analysis progress: $analyzed/$total")
            }
            Log.d(TAG, "Batch analysis complete")
        }
    }

    fun scanCovers() {
        val notePath = pathHelper.getNotesFolderPath()
        val uri = "$notePath%2F${PathHelper.COVERS_FOLDER_NAME_IN_NOTES}".toUri()
        coverUris = mediaReader.scanCovers(uri = uri).toMutableMap()
        coverUris[""] = getCoverUri(coverString = "_No Album Art.jpg")

        MusicPlayerService.coverUriMap.clear()
        allTracks.forEach { track ->
            if (track.sourceUri.isNotEmpty()) {
                val coverPath = coverUris[track.cover]
                if (!coverPath.isNullOrEmpty()) {
                    MusicPlayerService.coverUriMap[track.sourceUri] = coverPath
                }
            }
        }
        Log.d(TAG, "Cover map populated: ${MusicPlayerService.coverUriMap.size} entries")
    }

    fun getCoverUri(coverString: String): String {
        return coverUris[coverString] ?: ""
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun setMediaSourceWithService(track: TrackDocument) {
        if (track.id != currentTrack.id) {
            Log.d(TAG, "Source Uri: ${track.sourceUri}")
            currentTrack = track
            currentListenInSec = track.listenInSec
            isFavorite = favorites.tracklist.find { it == track } != null
            if (track.sourceUri.isEmpty()) {
                Log.w(TAG, "Track ${track.id} has no source URI, cannot play")
                return
            }
            currentQueueIndex = currentQueue.indexOf(currentTrack)
            if (isShuffle) {
                randomQueueIndex = randomQueue.indexOf(currentTrack)
            }
            try {
                val trackName = track.aliases.getOrElse(0) { "" }
                val artistName = track.creators.joinToString(", ") {
                    it.aliases.getOrElse(0) { CreatorDocument.UNKNOWN }
                }
                val albumName = track.album.ifEmpty { AlbumDocument.UNKNOWN }

                val mediaItem = MediaItem.Builder()
                    .setUri(track.sourceUri.toUri())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(trackName)
                            .setArtist(artistName)
                            .setAlbumTitle(albumName)
                            .setExtras(
                                android.os.Bundle().apply {
                                    putString("track_id", track.id)
                                }
                            )
                            .build()
                    )
                    .build()

                _mediaController?.let { controller ->
                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                } ?: run {
                    Toast.makeText(context, "Service not connected", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "SourceFile не валидный!", Toast.LENGTH_SHORT).show()
                nextTrack()
            }
        }
    }

    private fun startTimer() {
        if (duration == 0L)
            return
        playerTimer?.cancel()
        playerTimer = object : CountDownTimer(
            duration - currentPosition, 100L
        ) {
            override fun onFinish() {
                if (isRepeat) {
                    seekTo(0L)
                    play()
                } else {
                    currentPosition = duration
                    lastListenModifiedTimeInMillis = 0L
                }
            }

            override fun onTick(millisUntilFinished: Long) {
                currentPosition = duration - millisUntilFinished
                if (lastListenModifiedTimeInMillis - millisUntilFinished >= 1000L) {
                    lastListenModifiedTimeInMillis = millisUntilFinished
                    val updated = currentTrack.copy(listenInSec = currentTrack.listenInSec + 1)
                    currentTrack = updated
                    currentListenInSec = updated.listenInSec
                    val trackId = updated.id
                    val idx = allTracks.indexOfFirst { it.id == trackId }
                    if (idx >= 0) {
                        allTracks[idx] = updated
                    }
                    allPlaylists.forEach { playlist ->
                        val tIdx = playlist.tracklist.indexOfFirst { it.id == trackId }
                        if (tIdx >= 0) {
                            playlist.tracklist = playlist.tracklist.toMutableList().also {
                                it[tIdx] = updated
                            }
                        }
                    }
                    viewModelScope.launch {
                        repository.incrementListen(updated, updated.creators)
                    }
                }
            }

        }.start()
    }

    fun play() {
        _mediaController?.play()
        isPlaying = true
        startTimer()
    }

    fun pause() {
        _mediaController?.pause()
        isPlaying = false
        playerTimer?.cancel()
    }

    fun seekTo(position: Long) {
        _mediaController?.seekTo(position)
        currentPosition = position
        lastListenModifiedTimeInMillis = duration - currentPosition
        if (isPlaying)
            startTimer()
    }

    fun nextTrack() {
        val queue = if (isShuffle) randomQueue else currentQueue
        val index = if (isShuffle) randomQueueIndex else currentQueueIndex
        Log.d(TAG, "nextTrack $queue, $index")

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

    fun enableRepeat() {
        isRepeat = true
    }

    fun disableRepeat() {
        isRepeat = false
    }

    fun setQueueToDefault() {
        currentQueue = allTracks.toMutableList()
    }

    fun onSearchQueryChange(query: String) {
        musicState = musicState.copy(searchQuery = query)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val musicList = if (query.isBlank()) {
                allTracks.sortedByDescending { it.listenInSec }.take(10)
            } else {
                repository.searchTracks(query)
            }
            musicState = musicState.copy(trackList = musicList)
        }
    }

    fun addToFavorites(track: TrackDocument) {
        if (isFavorite)
            return
        isFavorite = true

        val list = favorites.tracklist.toMutableList()
        list.add(track)
        favorites.tracklist = list
        savePlaylist(favorites)
    }

    fun removeFromFavorites(track: TrackDocument) {
        if (!isFavorite)
            return
        isFavorite = false

        val list = favorites.tracklist.toMutableList()
        list.remove(track)
        favorites.tracklist = list
        savePlaylist(favorites)
    }

    fun changeTrackFavoriteState(track: TrackDocument) {
        if (isFavorite)
            removeFromFavorites(track)
        else
            addToFavorites(track)
    }

    fun savePlaylist(playlist: PlaylistDocument) {
        val index = allPlaylists.indexOfFirst { it == playlist }
        if (index == -1)
            return
        allPlaylists[index].tracklist = playlist.tracklist
        viewModelScope.launch {
            repository.savePlaylist(allPlaylists[index])
        }
    }

    fun adjustListenInSec(multiplier: Int) {
        val durMs = duration
        val durSec = ((durMs / 1000).toInt()).coerceAtLeast(1)
        val track = currentTrack
        val oldListen = track.listenInSec
        val newListen = if (multiplier == 0) 0 else (oldListen + durSec * multiplier)
        val updated = track.copy(listenInSec = newListen)
        currentTrack = updated
        currentListenInSec = newListen

        val idx = allTracks.indexOfFirst { it.id == track.id }
        if (idx >= 0) {
            allTracks[idx] = updated
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTrack(updated)
            updated.creators.forEach { creator ->
                val crIdx = allCreators.indexOfFirst { it.id == creator.id }
                if (crIdx >= 0) {
                    val old = allCreators[crIdx]
                    val newCr = old.copy(listenInSec = old.listenInSec + durSec * multiplier)
                    allCreators[crIdx] = newCr
                    repository.saveCreator(newCr)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MusicPlayerViewModel"

        var onNextTrack: (() -> Unit)? = null
        var onPreviousTrack: (() -> Unit)? = null
    }
}

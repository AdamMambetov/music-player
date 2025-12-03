package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
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
import com.example.musicplayer.mdreader.MarkdownReader
import com.example.musicplayer.mdreader.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.random.Random

class MusicPlayerViewModel(
    @field:SuppressLint("StaticFieldLeak") val context: Context,
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

    val pathHelper = PathHelper(context = context)

    val markdownReader = MarkdownReader(pathHelper = pathHelper)

    val mediaReader = MediaReader(context = context)

    private var coverUris = mutableMapOf<String, String>()

    private var playerTimer: CountDownTimer? = null

    private var lastListenModifiedTimeInMillis: Long = Long.MAX_VALUE

    private var searchJob: Job? = null
    private var scanJob: Job? = null

    // private var _mediaController: MediaController? = null
    private var _exoPlayer: ExoPlayer? = null

    init {
        viewModelScope.launch {
            searchManager.init()
            loadAllFromCache()
            scanCovers()
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
    fun initializePlayer() {
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
                    startTimer()
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

    fun scanAll(clearCache: Boolean) {
        Log.d("TAG", "Rescan start")
        scanJob?.cancel()
        scanJob = viewModelScope.launch(context = Dispatchers.IO) {
            isScan = true
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

            val creators = markdownReader
                .scanCreators()
                .map { creator ->
                    val oldCreator = allCreators.find { it == creator }
                    return@map if (oldCreator == null)
                        creator
                    else
                        creator.copy(id = oldCreator.id)
                }

            allCreators.clear()
            searchManager.putCreators(creators)
            allCreators.addAll(creators)
            Log.d("TAG", "Finish scan creators from markdown ${creators.size}")

            val sourceFileUris = mediaReader.scanAudio(
                uri = pathHelper.getTracksFolderPath().toUri(),
            )
            val tracks = markdownReader
                .scanTracks(allCreators)
                .sortedByDescending { it.created }
                .map { track ->
                    val sourceUri = sourceFileUris[track.sourceFile]
                    val oldTrack = allTracks.find { it == track }
                    return@map if (oldTrack == null)
                        track.copy(sourceUri = sourceUri ?: "")
                    else
                        track.copy(
                            id = oldTrack.id,
                            sourceUri = sourceUri ?: oldTrack.sourceUri,
                        )
                }
            Log.d("TAG", "Finish scan tracks from markdown ${tracks.size}")

            searchManager.putTracks(tracks)
            allTracks.clear()
            allTracks.addAll(tracks)
            Log.d("TAG", "Finish scan tracks")

            val albums = markdownReader
                .scanAlbums(allCreators, allTracks)
                .map { album ->
                    val oldAlbum = allAlbums.find { it == album }
                    return@map if (oldAlbum == null)
                        album
                    else
                        album.copy(id = oldAlbum.id)
                }

            searchManager.putAlbums(albums)
            allAlbums.clear()
            allAlbums.addAll(albums)
            Log.d("TAG", "Finish scan albums from markdown ${albums.size}")

            val playlists = markdownReader
                .scanPlaylists(allTracks)
                .map { playlist ->
                    val oldPlaylist = allPlaylists.find { it == playlist }
                    return@map if (oldPlaylist == null)
                        playlist
                    else
                        playlist.copy(id = oldPlaylist.id)
                }

            searchManager.putPlaylists(playlists)
            allPlaylists.clear()
            allPlaylists.addAll(playlists)
            favorites = allPlaylists.find {
                it.aliases.getOrElse(0) { false } == "Favorites"
            } ?: favorites
            Log.d("TAG", "Finish scan playlists from markdown ${playlists.size}")
            Log.d("TAG", "Rescan end")
            isScan = false
        }
    }

    fun loadAllFromCache() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(context = Dispatchers.IO) {
            isScan = true
            allCreators.clear()
            allCreators.addAll(searchManager.searchAllCreators())
            Log.d("TAG", "Finish scan creators from cache ${allCreators.size}")

            allTracks.clear()
            val sourceFileUris = mediaReader.scanAudio(
                uri = pathHelper.getTracksFolderPath().toUri(),
            )
            val tracks = searchManager
                .searchAllTracks()
                .map { track ->
                    val sourceUri = sourceFileUris[track.sourceFile]
                    return@map if (sourceUri == null)
                        track
                    else
                        track.copy(sourceUri = sourceUri)
                }

            allTracks.addAll(tracks)
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

    fun scanCovers() {
        val notePath = pathHelper.getNotesFolderPath()
        val uri = "$notePath%2F${PathHelper.COVERS_FOLDER_NAME_IN_NOTES}".toUri()
        coverUris = mediaReader.scanCovers(uri = uri).toMutableMap()
        coverUris.put("", getCoverUri(coverString = "_No Album Art.jpg"))
    }

    fun getCoverUri(coverString: String): String {
        Log.d("TAG", "cover = $coverString uri = ${coverUris[coverString]}")
        return coverUris[coverString] ?: ""
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
            try {
                _exoPlayer?.setMediaItem(MediaItem.fromUri(track.sourceUri))
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
                    markdownReader.saveTrack(currentTrack)
                    currentTrack.creators.forEach {
                        it.listenInSec++
                        markdownReader.saveCreator(creator = it)
                    }
                    viewModelScope.launch {
                        searchManager.putTracks(listOf(currentTrack))
                        searchManager.putCreators(currentTrack.creators)
                    }
                }
            }

        }.start()
    }

    fun play() {
        _exoPlayer?.play()
        isPlaying = true
        startTimer()
    }

    fun pause() {
        _exoPlayer?.pause()
        isPlaying = false
        playerTimer?.cancel()
    }

    fun seekTo(position: Long) {
        _exoPlayer?.seekTo(position)
        currentPosition = position
        lastListenModifiedTimeInMillis = duration - currentPosition
        if (isPlaying)
            startTimer()
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
        Log.d("TAG", "savePlaylist ${playlist.fileName} - ${playlist.tracklist.size}")
        val index = allPlaylists.indexOfFirst { it == playlist }
        if (index == -1)
            return
        allPlaylists[index] = playlist.copy(id = allPlaylists[index].id)
        viewModelScope.launch(Dispatchers.IO) {
            markdownReader.savePlaylist(allPlaylists[index])
            searchManager.putPlaylists(listOf(allPlaylists[index]))
        }
        Log.d("TAG", "savePlaylist success")
    }
}
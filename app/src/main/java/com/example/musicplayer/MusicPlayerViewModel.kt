package com.example.musicplayer

import android.content.ComponentName
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
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.data.TrackListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.random.Random

class MusicPlayerViewModel(
    val searchManager: MusicPlayerSearchManager
) : ViewModel() {
    var isPlaying by mutableStateOf(false)

    var isShuffle by mutableStateOf(false)

    var currentPosition by mutableLongStateOf(0L)

    var duration by mutableLongStateOf(0L)

    var allTracks = mutableStateListOf<TrackDocument>()
    var allCreators = mutableStateListOf<CreatorDocument>()

    var currentTrack by mutableStateOf(TrackDocument.createEmpty())

    var currentQueue = mutableListOf<TrackDocument>()

    var currentQueueIndex by mutableIntStateOf(-1)

    var randomQueue = mutableListOf<TrackDocument>()

    var randomQueueIndex by mutableIntStateOf(-1)

    var random = Random(Random.nextLong())

    var musicState by mutableStateOf(TrackListState())
        private set

    private var playerTimer: CountDownTimer? = null

    private var lastListenModifiedTimeInMillis: Long = Long.MAX_VALUE

    private var searchJob: Job? = null
    private var scanJob: Job? = null

    private var _mediaController: MediaController? = null

    init {
        viewModelScope.launch {
            searchManager.init()
            loadAllFromCache()
        }
    }

    override fun onCleared() {
        _mediaController?.release()
        searchJob?.cancel()
        scanJob?.cancel()
        searchManager.closeSession()
        super.onCleared()
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer(context: Context) {
        viewModelScope.launch {
            val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            
            controllerFuture.addListener({
                _mediaController = controllerFuture.get()

                // Add listener to update position and duration
                _mediaController?.addListener(object : Player.Listener {
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
                            duration = _mediaController?.duration ?: 0L
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
                        currentPosition = _mediaController?.currentPosition ?: 0L
                        Log.d("TAG", "onPositionDiscontinuity: $oldPosition - $newPosition")
                    }
                })
            }, { it.run() })
        }
    }

    fun scanAll(context: Context) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(context = Dispatchers.IO) {
            searchManager.removeTracks(searchManager.searchAllTracks())
            searchManager.removeCreators(searchManager.searchAllCreators())
            searchManager.removeAlbums(searchManager.searchAllAlbums())
            searchManager.removePlayLists(searchManager.searchAllPlaylists())
            Log.d("TAG", "Finish clean AppSearch index")

            val markdownReader = MarkdownReader()
            val creators = markdownReader.scanCreators(context)
            allCreators.clear()
            allCreators.addAll(creators)
            searchManager.putCreators(allCreators)
            Log.d("TAG", "Finish scan creators from markdown")
            val tracks = markdownReader.scanTracks(context, allCreators)
            allTracks.clear()
            allTracks.addAll(tracks)
            Log.d("TAG", "Finish scan tracks from markdown")
            scanSourceTracks(context)
            searchManager.putTracks(allTracks)
            Log.d("TAG", "Finish scan tracks from source")
        }
    }

    private fun scanSourceTracks(context: Context) {
        val musicPathString = getTracksFolderPath(context)
        if (musicPathString.isEmpty())
            return
        if (allTracks.isEmpty())
            return

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
                allTracks.find {
                    musicInfo -> musicInfo.sourceFile == fileName
                }?.sourceUri = contentUri.toString()
            }
        }
    }

    fun loadAllFromCache() {
        viewModelScope.launch(context = Dispatchers.IO) {
            allCreators.clear()
            allCreators.addAll(searchManager.searchAllCreators())
            Log.d("TAG", "Finish scan creators from cache ${allCreators.size}")
            allTracks.clear()
            allTracks.addAll(searchManager.searchAllTracks())
            Log.d("TAG", "Finish scan tracks from cache ${allTracks.size}")
        }
    }

    fun setMediaSourceWithService(musicInfo: TrackDocument) {
        _mediaController?.shuffleModeEnabled
        if (musicInfo.sourceUri.isNotEmpty() && musicInfo.sourceFile !== currentTrack.sourceFile) {
            Log.d("TAG", "Source Uri: ${musicInfo.sourceUri}")
            // Use the MediaController to set the media source instead of starting the service again
            // This prevents multiple notifications from being created
            currentTrack = musicInfo
            currentQueueIndex = allTracks.indexOf(currentTrack)
            if (isShuffle) {
                randomQueueIndex = randomQueue.indexOf(currentTrack)
            }
            _mediaController?.setMediaItem(MediaItem.fromUri(musicInfo.sourceUri))
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
        _mediaController?.play()
        isPlaying = true
        startTimer(context)
    }

    fun pause() {
        _mediaController?.pause()
        isPlaying = false
        playerTimer?.cancel()
    }

    fun seekTo(context: Context, position: Long) {
        _mediaController?.seekTo(position)
        currentPosition = position
        lastListenModifiedTimeInMillis = duration - currentPosition
        if (isPlaying)
            startTimer(context)
    }

    fun nextTrack() {
        val queue = if (isShuffle) randomQueue else currentQueue
        val index = if (isShuffle) randomQueueIndex else currentQueueIndex

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
}
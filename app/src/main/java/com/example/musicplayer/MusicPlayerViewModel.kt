package com.example.musicplayer

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayerViewModel : ViewModel() {
    var isPlaying by mutableStateOf(false)

    var currentPosition by mutableLongStateOf(0L)

    var duration by mutableLongStateOf(0L)
    
    // List of music info from markdown files
    var musicInfoList by mutableStateOf<List<MusicInfo>>(emptyList())

    private var _mediaController: MediaController? = null

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
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        if (playbackState == Player.STATE_READY) {
                            duration = _mediaController?.duration ?: 0L
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                        currentPosition = _mediaController?.currentPosition ?: 0L
                    }
                })
            }, { exception ->
                Log.e("TAG", exception.toString())
            })
        }
    }
    
    /**
     * Load music info from markdown files in the notePath
     */
    fun loadMusicInfoFromMarkdown(context: Context) {
        Log.d("TAG", "loadMusicInfoFromMarkdown")
        viewModelScope.launch {
            val markdownMusicReader = MarkdownMusicReader()
            // Clear the list first
            musicInfoList = emptyList()
            
            // Load music info incrementally with batching to prevent UI freezing
            val batchList = mutableListOf<MusicInfo>()
            val batchSize = 5 // Update UI every 5 items
            
            // Process files on a background thread
            withContext(kotlinx.coroutines.Dispatchers.Default) {
                markdownMusicReader.scanNotePathForMusicInfoIncremental(context) { musicInfo ->
                    batchList.add(musicInfo)
                    // Update the UI in batches to prevent freezing
                    if (batchList.size >= batchSize) {
                        // Switch to main thread to update UI
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            musicInfoList = musicInfoList + batchList.toList()
                        }
                        batchList.clear()
                    }
                    Log.d("TAG", "add music item to musicInfoList")
                }
            }
            
            // Add any remaining items that didn't make a full batch
            if (batchList.isNotEmpty()) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    musicInfoList = musicInfoList + batchList.toList()
                }
            }
        }
    }

    fun setMediaSource(musicInfo: MusicInfo) {
        // Update the media source in the service
        if (musicInfo.sourceUri.isNotEmpty()) {
            // We need to use the MediaController to interact with the service
            // The service will handle setting the media source
            // Note: Context should be passed from the UI layer
        }
    }
    
    fun setMediaSourceWithService(context: Context, musicInfo: MusicInfo) {
        if (musicInfo.sourceUri.isNotEmpty()) {
            val serviceIntent = android.content.Intent(context, MusicPlayerService::class.java)
            // Pass the sourceUri which should be a content URI with proper permissions
            serviceIntent.putExtra("media_source", musicInfo.sourceUri)
            context.startService(serviceIntent)
        }
    }

    fun play() {
        _mediaController?.play()
        isPlaying = true
    }

    fun pause() {
        _mediaController?.pause()
        isPlaying = false
    }

    fun seekTo(position: Long) {
        _mediaController?.seekTo(position)
        currentPosition = position
    }

    fun nextTrack() {
        _mediaController?.seekToNext()
    }

    fun previousTrack() {
        _mediaController?.seekToPrevious()
    }

    override fun onCleared() {
        super.onCleared()
        _mediaController?.run {
            release()
        }
    }
}
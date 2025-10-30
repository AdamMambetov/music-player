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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.launch

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
            }, { exception ->
                Log.e("TAG", exception.toString())
            })
        }
    }
    
    /**
     * Load music info from markdown files in the notePath
     */
    fun loadMusicInfoFromMarkdown(context: Context) {
        viewModelScope.launch {
            val markdownMusicReader = MarkdownMusicReader()
            val musicList = markdownMusicReader.scanNotePathForMusicInfo(context)
            musicInfoList = musicList
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
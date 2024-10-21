package com.example.musicplayer.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.musicplayer.data.MusicListRepositoryImpl
import com.example.musicplayer.domain.AnalyzeMusicFiles
import com.example.musicplayer.domain.GetMusicListUseCase

class MainViewModel : ViewModel() {

    private val repository = MusicListRepositoryImpl
    private val getMusicListUseCase = GetMusicListUseCase(repository)
    private val analyzeMusicFiles = AnalyzeMusicFiles(repository)

    val musicList = getMusicListUseCase.getMusicList()


    fun analyzeMusicMarkdownFiles(context: Context) {
        analyzeMusicFiles.analyzeMusicMarkdownFiles(context)
    }
}

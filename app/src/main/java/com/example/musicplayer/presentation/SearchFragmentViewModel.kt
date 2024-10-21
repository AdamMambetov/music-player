package com.example.musicplayer.presentation

import androidx.lifecycle.ViewModel
import com.example.musicplayer.data.MusicListRepositoryImpl
import com.example.musicplayer.domain.GetMusicListUseCase

class SearchFragmentViewModel : ViewModel() {

    private val repository = MusicListRepositoryImpl
    private val getMusicListUseCase = GetMusicListUseCase(repository)

    val musicList = getMusicListUseCase.getMusicList()
}
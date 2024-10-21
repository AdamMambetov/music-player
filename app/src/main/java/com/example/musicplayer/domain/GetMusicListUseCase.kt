package com.example.musicplayer.domain

class GetMusicListUseCase(private val musicListRepository: MusicListRepository) {

    fun getMusicList() = musicListRepository.getMusicList()
    fun getMusicInfo(musicInfoId: Long) = musicListRepository.getMusicInfo(musicInfoId)
}

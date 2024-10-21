package com.example.musicplayer.domain

import android.content.Context

class AnalyzeMusicFiles(private val musicListRepository: MusicListRepository) {

    fun analyzeMusicMarkdownFiles(context: Context) {
        musicListRepository.analyzeMusicMarkdownFiles(context)
    }
}

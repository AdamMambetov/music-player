package com.example.musicplayer.domain

import android.content.Context
import androidx.lifecycle.LiveData

interface MusicListRepository {

    fun getMusicList(): LiveData<List<MusicInfo>>

    fun getMusicInfo(musicInfoId: Long): MusicInfo

    fun analyzeMusicMarkdownFiles(context: Context)

}

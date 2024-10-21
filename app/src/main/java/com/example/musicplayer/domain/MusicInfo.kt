package com.example.musicplayer.domain

data class MusicInfo(
    val id: Long, // id in ContentResolver
    val title: String,
    val album: String,
    val artist: String,
    val extension: String,
    private val text: String
) {

    companion object {
        const val TITLE_KEY = "name"
        const val ARTIST_KEY = "artists"

        const val UNKNOWN_VALUE = "<unknown>"
    }

}

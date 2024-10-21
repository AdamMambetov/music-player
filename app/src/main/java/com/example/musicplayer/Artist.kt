package com.example.musicplayer

data class Artist(
    val id: String,
    val name: String
) {
    companion object {
        private const val TITLE_KEY = "name"
        private const val ARTIST_KEY = "artists"

    }
}

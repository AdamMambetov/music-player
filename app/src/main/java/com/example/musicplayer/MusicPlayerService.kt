package com.example.musicplayer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.net.toUri

class MusicPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Create ExoPlayer instance with proper audio attributes and content URI support
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
        
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // Handle audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Add listener to update position and duration
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                // Update position and duration as needed
            }

            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                // Update position when it changes
            }
        })

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.getStringExtra("media_source")?.let { mediaPath ->
            // Directly use the mediaPath which should be a content URI with proper permissions
            setMediaSource(mediaPath)
        }
        return START_STICKY // Keep the service running
    }

    private fun setMediaSource(mediaPath: String) {
        try {
            val uri = mediaPath.toUri()
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .build()
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error setting media source: ${e.message}")
        }
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.release()
            mediaSession = null
        }
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
        }
        super.onDestroy()
    }
}
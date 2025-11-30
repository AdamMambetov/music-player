package com.example.musicplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import androidx.core.net.toUri
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MusicPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var notificationManager: NotificationManager? = null
    
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        
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
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
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
            .setCallback(object : MediaSession.Callback {
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

                    return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(listOf(
                        mediaSession.player.currentMediaItem ?: MediaItem.fromUri("")), 0, 0))
                }
            })
            .build()
            
        // Ensure PlayerNotificationManager is properly set with the player only once
        playerNotificationManager?.setPlayer(exoPlayer)
            
        // Create PlayerNotificationManager with custom media description adapter
        playerNotificationManager = PlayerNotificationManager
            .Builder(this, NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    // Try to get the current track title if available
                    val mediaMetadata = player.currentMediaItem?.mediaMetadata
                    return mediaMetadata?.title?.toString() ?: "Music Player"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    return PendingIntent.getActivity(
                        this@MusicPlayerService,
                        0,
                        Intent(this@MusicPlayerService, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    val mediaMetadata = player.currentMediaItem?.mediaMetadata
                    return mediaMetadata?.artist?.toString() ?: "Playing music"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): android.graphics.Bitmap? {
                    return null
                }
            })
            .setPauseActionIconResourceId(R.drawable.pause)
            .setNextActionIconResourceId(R.drawable.skip_next)
            .setPlayActionIconResourceId(R.drawable.play_arrow)
            .setPreviousActionIconResourceId(R.drawable.skip_previous)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    super.onNotificationCancelled(notificationId, dismissedByUser)
                    // Restart the notification if it was dismissed by the user
                    if (dismissedByUser) {
                        // Restart the notification by reassigning the player
                        playerNotificationManager?.setPlayer(null)
                        playerNotificationManager?.setPlayer(exoPlayer)
                    }
                }
            })
            .build()
            
        // Configure the PlayerNotificationManager
        playerNotificationManager?.apply {
            setUseRewindAction(false)
            setUseFastForwardAction(false)
            setUseNextAction(true)
            setUseNextActionInCompactView(true)
            setUsePreviousActionInCompactView(true)
            setPlayer(exoPlayer) // Set the player directly in the configuration
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.getStringExtra("media_source")?.let { mediaPath ->
            // Directly use the mediaPath which should be a content URI with proper permissions
            setMediaSource(mediaPath)
        }
        
        // PlayerNotificationManager is already set with the player in onCreate()
        // No need to set it again here to avoid duplicate notifications
        
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
            Log.e("MusicPlayerService", "Error setting media source: ${e.message}")
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
        // Properly clean up all components
        playerNotificationManager?.let {
            it.setPlayer(null)
            playerNotificationManager = null
        }
        
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
    
    private fun createNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Music Player Service",
            NotificationManager.IMPORTANCE_LOW // Use IMPORTANCE_LOW to prevent notification sound but keep notification persistent
        ).apply {
            description = "Shows currently playing music"
            setSound(null, null) // Remove notification sound for media playback notifications
            enableVibration(false) // Disable vibration for media playback notifications
            // Make the notification non-dismissible by setting the importance appropriately
        }

        notificationManager?.createNotificationChannel(channel)
    }
}
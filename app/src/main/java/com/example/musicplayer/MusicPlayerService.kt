package com.example.musicplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var notificationManager: NotificationManager? = null
    private var notificationStarted = false

    private var currentCoverUri: String? = null
    private var currentCover: Bitmap? = null

    private val replayGain = ReplayGain()
    private val gainProcessor = GainAudioProcessor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var pathHelper: com.example.musicplayer.mdreader.PathHelper

    private val prefsListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "replay_gain_enabled") {
                val player = exoPlayer ?: return@OnSharedPreferenceChangeListener
                if (!pathHelper.isReplayGainEnabled()) {
                    gainProcessor.setGainDb(0f)
                } else {
                    val uri = player.currentMediaItem?.localConfiguration?.uri
                        ?: return@OnSharedPreferenceChangeListener
                    val trackId =
                        player.currentMediaItem?.mediaMetadata?.extras?.getString("track_id")
                    serviceScope.launch {
                        val result = getOrAnalyzeGain(trackId, uri)
                        val gainDb = result.trackGainDb ?: 0f
                        gainProcessor.setGainDb(gainDb, result.peakLevelDb)
                    }
                }
            }
        }

    private suspend fun getOrAnalyzeGain(trackId: String?, uri: android.net.Uri): AnalysisResult =
        kotlinx.coroutines.withContext(
            Dispatchers.IO
        ) {
            if (uri.toString().isEmpty()) return@withContext AnalysisResult(trackGainDb = 0f)
            if (trackId != null) {
                val cached =
                    com.example.musicplayer.data.PostgresDataSource.instance?.getTrackGain(trackId)
                if (cached != null) return@withContext cached
            }
            AnalysisResult(trackGainDb = 0f)
        }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1
        const val TAG = "MusicPlayerService"

        const val ACTION_PLAY_PAUSE = "com.example.musicplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.musicplayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.musicplayer.ACTION_PREVIOUS"

        val coverUriMap = mutableMapOf<String, String>()
    }

    private var playPauseUpdateRunnable: Runnable? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playPauseUpdateRunnable?.let { handler.removeCallbacks(it) }
            if (isPlaying) {
                handler.post { updateNotification() }
            } else {
                playPauseUpdateRunnable = Runnable { updateNotification() }
                handler.postDelayed(playPauseUpdateRunnable!!, 200L)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    print("STATE_IDLE")
                }

                Player.STATE_ENDED -> {
                    print("STATE_ENDED")
                }

                Player.STATE_BUFFERING -> {
                    print("STATE_BUFFERING")
                }

                Player.STATE_READY -> {
                    handler.postDelayed({ updateNotification() }, 200L)
                }

                else -> {
                    print("else")
                }
            }
//            handler.post { updateNotification() }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            handler.post { updateNotification() }
            if (!pathHelper.isReplayGainEnabled()) {
                gainProcessor.setGainDb(0f)
                return
            }
            mediaItem?.localConfiguration?.uri?.let { uri ->
                val trackId = mediaItem.mediaMetadata?.extras?.getString("track_id")
                serviceScope.launch {
                    val result = getOrAnalyzeGain(trackId, uri)
                    val gainDb = result.trackGainDb ?: 0f
                    gainProcessor.setGainDb(gainDb, result.peakLevelDb)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        pathHelper = com.example.musicplayer.mdreader.PathHelper(this)
        getSharedPreferences("music_player_prefs", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)

        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                buttons: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                callback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val emptyNotification = android.app.Notification.Builder(
                    this@MusicPlayerService,
                    NOTIFICATION_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build()
                return MediaNotification(NOTIFICATION_ID, emptyNotification)
            }

            override fun handleCustomCommand(
                session: MediaSession,
                customCommand: String,
                args: android.os.Bundle
            ): Boolean = false
        })

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf<AudioProcessor>(gainProcessor))
                    .build()
            }
        }

        exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer?.addListener(playerListener)

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setCallback(object : MediaSession.Callback {
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val currentItem = mediaSession.player.currentMediaItem
                        ?: MediaItem.fromUri("")
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(currentItem), 0, 0
                        )
                    )
                }

                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaButtonIntent: Intent
                ): Boolean {
                    val event = mediaButtonIntent.getParcelableExtra<android.view.KeyEvent>(
                        "android.intent.extra.KEY_EVENT"
                    ) ?: return false
                    if (event.action != android.view.KeyEvent.ACTION_DOWN) return false
                    when (event.keyCode) {
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            MusicPlayerViewModel.onNextTrack?.invoke()
                            return true
                        }

                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            MusicPlayerViewModel.onPreviousTrack?.invoke()
                            return true
                        }
                    }
                    return super.onMediaButtonEvent(session, controller, mediaButtonIntent)
                }
            })
            .build()

        updateNotification()
    }

    private fun buildCustomRemoteViews(
        title: String,
        artist: String,
        isPlaying: Boolean,
        coverBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(packageName, R.layout.notification_player)

        views.setTextViewText(R.id.notif_title, title)
        views.setTextViewText(R.id.notif_artist, artist)

        if (coverBitmap != null) {
            views.setImageViewBitmap(R.id.notif_cover, coverBitmap)
        } else {
            views.setImageViewResource(R.id.notif_cover, R.drawable.play_arrow)
        }

        val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play_arrow
        views.setImageViewResource(R.id.notif_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.notif_prev, createActionIntent(ACTION_PREVIOUS))
        views.setOnClickPendingIntent(R.id.notif_play_pause, createActionIntent(ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.notif_next, createActionIntent(ACTION_NEXT))

        return views
    }

    private fun updateNotification() {
        val player = exoPlayer ?: return

        val title: String
        val artist: String
        val uri: String?

        if (player.currentMediaItem != null) {
            title = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Music Player"
            artist = player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: ""
            val sourceUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
            uri = sourceUri?.let { coverUriMap[it] }
        } else {
            title = "Music Player"
            artist = "Ready to play"
            uri = null
        }

        if (uri.isNullOrEmpty()) {
            currentCoverUri = null
            currentCover = null
            handler.postDelayed(
                { postNotification(title, artist, player.isPlaying, currentCover) },
                200L
            )
        } else if (uri == currentCoverUri) {
            handler.postDelayed(
                { postNotification(title, artist, player.isPlaying, currentCover) },
                200L
            )
        } else {
            currentCoverUri = uri
            Thread {
                val bitmap = loadCoverBitmap(uri)
                currentCover = bitmap
                handler.postDelayed(
                    { postNotification(title, artist, player.isPlaying, bitmap) },
                    200L
                )
            }.start()
        }
    }

    private fun postNotification(
        title: String,
        artist: String,
        isPlaying: Boolean,
        coverBitmap: Bitmap?
    ) {
        val customViews = buildCustomRemoteViews(title, artist, isPlaying, coverBitmap)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(customViews)
            .setCustomBigContentView(customViews)
            .setCustomHeadsUpContentView(customViews)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .build()

        if (!notificationStarted) {
            notificationStarted = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadCoverBitmap(coverUri: String): Bitmap? {
        return try {
            val uri = coverUri.toUri()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cover: ${e.message}")
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                exoPlayer?.let { player ->
                    if (player.isPlaying) player.pause() else player.play()
                }
            }

            ACTION_NEXT -> {
                MusicPlayerViewModel.onNextTrack?.invoke()
            }

            ACTION_PREVIOUS -> {
                MusicPlayerViewModel.onPreviousTrack?.invoke()
            }
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        exoPlayer?.removeListener(playerListener)
        getSharedPreferences("music_player_prefs", MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)

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
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Music Player Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows currently playing music"
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager?.createNotificationChannel(channel)
    }
}

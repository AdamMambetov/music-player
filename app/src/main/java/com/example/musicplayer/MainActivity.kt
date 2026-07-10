package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.ui.screen.AlbumTracks
import com.example.musicplayer.ui.screen.AllAlbums
import com.example.musicplayer.ui.screen.AllCreatorsScreen
import com.example.musicplayer.ui.screen.AllPlaylists
import com.example.musicplayer.ui.screen.AllTracksScreen
import com.example.musicplayer.ui.screen.CreatorTracksScreen
import com.example.musicplayer.ui.screen.MusicPlayerScreen
import com.example.musicplayer.ui.screen.PlaylistTracks
import com.example.musicplayer.ui.screen.QueueTracksScreen
import com.example.musicplayer.ui.screen.SearchScreen
import com.example.musicplayer.ui.screen.SettingsScreen
import com.example.musicplayer.ui.screen.TrackCreatorsScreen
import com.example.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {
    private val fullStorageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val viewModel: MusicPlayerViewModel by viewModels(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MusicPlayerViewModel(
                        context = applicationContext
                    ) as T
                }
            }
        }
    )

    @OptIn(UnstableApi::class)
    @SuppressLint("ViewModelConstructorInComposable")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            MusicPlayerTheme {
                LaunchedEffect(key1 = Unit) {
                    requestStoragePermissions()
                    viewModel.initializePlayer()
                }
                MusicPlayerApp(viewModel = viewModel)
            }
        }
    }

    private fun requestStoragePermissions() {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:$packageName".toUri()
            }
            fullStorageAccessLauncher.launch(intent)
        }
    }
}

@Composable
fun MusicPlayerApp(viewModel: MusicPlayerViewModel) {
    var screen by remember { mutableStateOf("home") }

    BackHandler(enabled = screen != "home") { screen = "home" }

    Box(modifier = Modifier.fillMaxSize().background(com.example.musicplayer.ui.theme.SurfaceDark)) {
    when (screen) {
        "home" -> MusicPlayerScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onMoveTo = { screen = it },
        )

        "tracks" -> AllTracksScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onBack = { screen = "home" },
            onTrackSelected = { track ->
                viewModel.setQueueToDefault()
                viewModel.setMediaSourceWithService(track)
            }
        )

        "queue" -> QueueTracksScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onBack = { screen = "home" },
            onTrackSelected = { track ->
                viewModel.setQueueToDefault()
                viewModel.setMediaSourceWithService(track)
            }
        )

        "albums" -> AllAlbums(
            modifier = Modifier.fillMaxSize(),
            allAlbums = viewModel.allAlbums,
            onAlbumSelected = { album ->
                viewModel.currentAlbum = album
                screen = "album"
            },
            onBack = { screen = "home" }
        )

        "album" -> AlbumTracks(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onTrackSelected = { track ->
                viewModel.currentQueue = viewModel.currentAlbum.tracklist.toMutableList()
                viewModel.currentQueueIndex = viewModel.currentAlbum.tracklist.indexOf(track)
                viewModel.setMediaSourceWithService(track)
            },
            onBack = { screen = "albums" }
        )

        "trackCreators" -> TrackCreatorsScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onBack = { screen = "home" },
            onCreatorSelected = { creator ->
                viewModel.currentCreator = creator
                screen = "creator"
            }
        )

        "settings" -> SettingsScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onBack = { screen = "home" },
        )

        "search" -> SearchScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onTrackSelected = { track ->
                viewModel.setMediaSourceWithService(track)
            },
            onAlbumSelected = { album ->
                viewModel.currentAlbum = album
                screen = "album"
            },
            onCreatorSelected = { creator ->
                viewModel.currentCreator = creator
                screen = "creator"
            },
            onBack = { screen = "home" }
        )

        "playlists" -> AllPlaylists(
            modifier = Modifier.fillMaxSize(),
            allPlaylists = viewModel.allPlaylists,
            onPlaylistSelected = { playlist ->
                viewModel.currentPlaylist = playlist
                screen = "playlist"
            },
            onBack = { screen = "home" }
        )

        "playlist" -> PlaylistTracks(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onTrackSelected = { track ->
                viewModel.currentQueue = viewModel.currentPlaylist.tracklist.toMutableList()
                viewModel.currentQueueIndex = viewModel.currentPlaylist.tracklist.indexOf(track)
                viewModel.setMediaSourceWithService(track)
            },
            onBack = { screen = "playlists" }
        )

        "creators" -> AllCreatorsScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onBack = { screen = "home" },
            onCreatorSelected = { creator ->
                viewModel.currentCreator = creator
                screen = "creator"
            }
        )

        "creator" -> CreatorTracksScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            onBack = { screen = "home" },
            onTrackSelected = { track ->
                viewModel.setMediaSourceWithService(track)
            }
        )
    }
    }
}

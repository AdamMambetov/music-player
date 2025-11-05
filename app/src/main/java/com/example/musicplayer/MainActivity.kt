package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    // Permission request launcher for full storage access (MANAGE_EXTERNAL_STORAGE)
    private val fullStorageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    @SuppressLint("ViewModelConstructorInComposable")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start the music player service
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        startService(serviceIntent)
        
        setContent {
            MusicPlayerTheme {
                // Check and request storage permissions when the app starts
                val viewModel = MusicPlayerViewModel()
                LaunchedEffect(Unit) {
                    Log.d("TAG", "Launched effect")
                    requestStoragePermissions()
                    viewModel.initializePlayer(this@MainActivity)
                    viewModel.loadMusicInfoFromMarkdown(this@MainActivity)
                }
                
                MusicPlayerApp(viewModel = viewModel)
            }
        }
    }

    private fun requestStoragePermissions() {
        if (!Environment.isExternalStorageManager()) {
            // Request full storage access
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:$packageName".toUri()
            }
            fullStorageAccessLauncher.launch(intent)
        }
    }
}

@PreviewScreenSizes
@Composable
fun MusicPlayerApp(viewModel: MusicPlayerViewModel = MusicPlayerViewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var selectedTrack by rememberSaveable { mutableStateOf<MusicInfo?>(null) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painter = painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when {
                selectedTrack != null -> {
                    // Show music info screen for selected track
                    MusicInfoScreen(
                        trackName = selectedTrack!!.aliases.getOrElse(0) { "Empty name" },
                        modifier = Modifier.padding(innerPadding),
                        musicPlayerViewModel = viewModel
                    )
                }
                else -> {
                    when (currentDestination) {
                        AppDestinations.HOME -> {
                            MusicPlayerScreen(
                                modifier = Modifier.padding(innerPadding),
                                musicPlayerViewModel = viewModel,
                                onTrackSelected = { trackName ->
                                    selectedTrack = trackName
                                }
                            )
                        }
                        AppDestinations.SEARCH -> {
                            SearchScreen(
                                modifier = Modifier.padding(innerPadding),
                                onTrackSelected = { selectedTrack = it }
                            )
                        }
                        AppDestinations.SETTINGS -> {
                            SettingsScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel,
                            )
                        }
                        else -> {
                            Greeting(
                                name = when (currentDestination) {
                                    AppDestinations.FAVORITES -> "Favorites"
                                    AppDestinations.SETTINGS -> "Settings"
                                    else -> "Android"
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.home),
    SEARCH("Search", R.drawable.search),
    FAVORITES("Favorites", R.drawable.favorite),
    SETTINGS("Settings", R.drawable.settings),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicPlayerTheme {
        Greeting("Android")
    }
}
package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi

class MainActivity : ComponentActivity() {
    // Permission request launcher for full storage access (MANAGE_EXTERNAL_STORAGE)
    private val fullStorageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val viewModel: MusicPlayerViewModel by viewModels(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MusicPlayerViewModel(
                        MusicPlayerSearchManager(applicationContext)
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
        
        // Start the music player service
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        startService(serviceIntent)

        setContent {
            MusicPlayerTheme {
                LaunchedEffect(key1 = Unit) {
                    requestStoragePermissions()
                    viewModel.initializePlayer(this@MainActivity)
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

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp(viewModel: MusicPlayerViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var isTrackSelected by remember { mutableStateOf(false) }
    var showNavigationRail by remember { mutableStateOf(false) }

    if (showNavigationRail) {
        NavigationSuiteScaffold(
            layoutType = NavigationSuiteType.NavigationRail,
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        selected = it == currentDestination,
                        onClick = {
                            currentDestination = it
                            isTrackSelected = false
                        },
                        icon = {
                            Icon(
                                painter = painterResource(it.icon),
                                contentDescription = it.label
                            )
                        },
                    )
                }
            }
        ) {
            MainScreens(
                onTopBarClicked = { showNavigationRail = !showNavigationRail },
                onTrackSelected = { isTrackSelected = it },
                currentDestination = currentDestination,
                isTrackSelected = isTrackSelected,
                viewModel = viewModel,
            )
        }
    } else {
        MainScreens(
            onTopBarClicked = { showNavigationRail = !showNavigationRail },
            onTrackSelected = { isTrackSelected = it },
            currentDestination = currentDestination,
            isTrackSelected = isTrackSelected,
            viewModel = viewModel,
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreens(
    onTopBarClicked: () -> Unit,
    onTrackSelected: (value: Boolean) -> Unit,
    currentDestination: AppDestinations,
    isTrackSelected: Boolean,
    viewModel: MusicPlayerViewModel,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    IconButton(onClick = onTopBarClicked) {
                        Icon(
                            painter = painterResource(R.drawable.view_headline),
                            contentDescription = "Show Navigation Rail",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (currentDestination) {
            AppDestinations.HOME -> {
                if (isTrackSelected) {
                    // Show music info screen for selected track
                    MusicInfoScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
                        onBackClicked = { onTrackSelected(false) }
                    )
                } else {
                    MusicPlayerScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
                        onTrackSelected = { context, track ->
                            onTrackSelected(true)
                            viewModel.setQueueToDefault()
                            viewModel.setMediaSourceWithService(track)
                        }
                    )
                }
            }
            AppDestinations.SEARCH -> {
                SearchScreen(
                    modifier = Modifier.padding(innerPadding),
                    viewModel = viewModel,
                    onTrackSelected = { track ->
                        onTrackSelected(true)
                        viewModel.setMediaSourceWithService(track)
                    }
                )
            }
            AppDestinations.SETTINGS -> {
                SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    viewModel = viewModel,
                )
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Composable
@PreviewScreenSizes
fun MusicPlayerAppPreview() {
    MusicPlayerApp(MusicPlayerViewModel(
        MusicPlayerSearchManager(LocalContext.current)
    ))
}

enum class AppDestinations(
   val label: String,
   val icon: Int,
) {
   HOME("Home", R.drawable.home),
   SEARCH("Search", R.drawable.search),
   SETTINGS("Settings", R.drawable.settings),
}


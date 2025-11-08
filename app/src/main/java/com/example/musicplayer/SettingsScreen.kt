package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
) {
    val context = LocalContext.current
    var notePath by remember { mutableStateOf("Not set") }
    var musicPath by remember { mutableStateOf("Not set") }
    var fullStorageAccessGranted by remember { mutableStateOf(false) }
    
    // Check current permission status when the screen loads
    LaunchedEffect(Unit) {
        fullStorageAccessGranted = Environment.isExternalStorageManager()
        val storedNotePath = getStoredNotePath(context)
        notePath = if (storedNotePath == "Not set") storedNotePath else getPathFromUri(storedNotePath.toUri())
        val storedMusicPath = getStoredMusicPath(context)
        musicPath = if (storedMusicPath == "Not set") storedMusicPath else getPathFromUri(storedMusicPath.toUri())
    }

    // Launchers for directory picking
    val notePathLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        Log.d("TAG", uri.toString())
        val path = getPathFromUri(uri)
        Log.d("TAG", path)
        if (path.isNotEmpty()) {
            // Take URI permission to persist access across app restarts
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(
                    uri!!,
                    takeFlags,
                )
            } catch (e: SecurityException) {
                Log.e("SettingsScreen", "Failed to take URI permission: ${e.message}")
            }
            
            storeNotePath(context, uri.toString())
            notePath = path
            viewModel.loadMusicInfoFromMarkdown(context)
        }
    }
    
    val musicPathLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val path = getPathFromUri(uri)
        if (path.isNotEmpty()) {
            // Take URI permission to persist access across app restarts
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                Log.d("TAG", "Music Path: $uri")
            } catch (e: SecurityException) {
                Log.e("SettingsScreen", "Failed to take URI permission: ${e.message}")
            }
            
            storeMusicPath(context, uri.toString())
            musicPath = path
            viewModel.loadMusicInfoFromSource(context)
        }
    }

    // Launcher for full storage access (MANAGE_EXTERNAL_STORAGE)
    val fullStorageAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fullStorageAccessGranted = Environment.isExternalStorageManager()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Storage permission setting
        SettingItemWithButton(
            title = "Storage Access",
            currentValue = if (fullStorageAccessGranted) "Granted" else "Not Granted",
            buttonLabel = if (fullStorageAccessGranted) "Granted" else "Request",
            onButtonClick = {
                // For Android 11+, request full storage access
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = "package:${context.packageName}".toUri()
                }
                fullStorageAccessLauncher.launch(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Note Path setting
        SettingItem(
            title = "Note Path",
            currentValue = notePath,
            onButtonClick = { notePathLauncher.launch(null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Music Path setting
        SettingItem(
            title = "Music Path",
            currentValue = musicPath,
            onButtonClick = { musicPathLauncher.launch(null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.musicInfoList = listOf()
                viewModel.cachedMusicInfoList = null
                viewModel.saveMusicInfoListToCache(context, listOf())
                viewModel.loadMusicInfoFromMarkdown(context)
                viewModel.loadMusicInfoFromSource(context)
            },

        ) {
            Text("Rescan")
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    currentValue: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f) // Give the text content a weight so it doesn't take all space
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onButtonClick,
                modifier = Modifier.size(48.dp) // Adding minimum size for visibility
            ) {
                Icon(
                    painter = painterResource(R.drawable.folder),
                    contentDescription = "Select $title"
                )
            }
        }
    }
}

// New composable for settings with button
@Composable
fun SettingItemWithButton(
    title: String,
    currentValue: String,
    buttonLabel: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            Button(onClick = onButtonClick) {
                Text(buttonLabel)
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun SettingsScreenPreview() {
    // Create a mock launcher that doesn't actually launch anything for preview
    SettingsScreen(viewModel = MusicPlayerViewModel())
}

// Helper functions for storing and retrieving paths
private fun getStoredNotePath(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    val result = sharedPreferences.getString("note_path", "Not set") ?: "Not set"
    return result
}

private fun storeNotePath(context: Context, path: String) {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("note_path", path) }
}

private fun getStoredMusicPath(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    val result = sharedPreferences.getString("music_path", "Not set") ?: "Not set"
    return result
}

private fun storeMusicPath(context: Context, path: String) {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("music_path", path) }
}

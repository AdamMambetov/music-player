package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notePath by remember { mutableStateOf(getStoredNotePath(context)) }
    var musicPath by remember { mutableStateOf(getStoredMusicPath(context)) }
    var fullStorageAccessGranted by remember { mutableStateOf(false) }
    
    // Check current permission status when the screen loads
    LaunchedEffect(Unit) {
        fullStorageAccessGranted =
            android.os.Environment.isExternalStorageManager()
    }

    // Launchers for directory picking
    val notePathLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            // Store the URI for future use
            storeNotePath(context, it.toString())
            notePath = getStoredNotePath(context)
        }
    }
    
    val musicPathLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            // Store the URI for future use
            storeMusicPath(context, it.toString())
            musicPath = getStoredMusicPath(context)
        }
    }

    // Launcher for full storage access (MANAGE_EXTERNAL_STORAGE)
    val fullStorageAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fullStorageAccessGranted =
            android.os.Environment.isExternalStorageManager()
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
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SettingsScreenPreview() {
    // Create a mock launcher that doesn't actually launch anything for preview
    SettingsScreen()
}

// Helper functions for storing and retrieving paths
private fun getStoredNotePath(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("note_path", "Not set") ?: "Not set"
}

private fun storeNotePath(context: Context, path: String) {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("note_path", path).apply()
}

private fun getStoredMusicPath(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("music_path", "Not set") ?: "Not set"
}

private fun storeMusicPath(context: Context, path: String) {
    val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("music_path", path).apply()
}
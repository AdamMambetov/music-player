package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.R
import com.example.musicplayer.ui.theme.Blue60
import com.example.musicplayer.ui.theme.OnSurfacePrimary
import com.example.musicplayer.ui.theme.OnSurfaceSecondary
import com.example.musicplayer.ui.theme.SurfaceCard
import com.example.musicplayer.ui.theme.SurfaceDark

private const val EMPTY_PATH = "Not set"

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    var notePath by remember { mutableStateOf(EMPTY_PATH) }
    var musicPath by remember { mutableStateOf(EMPTY_PATH) }
    var fullStorageAccessGranted by remember { mutableStateOf(false) }
    var replayGainEnabled by remember { mutableStateOf(false) }
    val pathHelper = viewModel.pathHelper

    LaunchedEffect(key1 = Unit) {
        fullStorageAccessGranted = Environment.isExternalStorageManager()
        replayGainEnabled = pathHelper.isReplayGainEnabled()
        val storedNotePath = pathHelper.getNotesFolderPath()
        notePath = if (storedNotePath.isEmpty()) EMPTY_PATH else pathHelper.getPathFromUri(uri = storedNotePath.toUri())
        val storedMusicPath = pathHelper.getTracksFolderPath()
        musicPath = if (storedMusicPath.isEmpty()) EMPTY_PATH else pathHelper.getPathFromUri(uri = storedMusicPath.toUri())
    }

    val notePathLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
        val path = pathHelper.getPathFromUri(uri = uri)
        if (path.isNotEmpty()) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri!!, takeFlags)
            } catch (e: SecurityException) { Log.e("SettingsScreen", "Failed to take URI permission: ${e.message}") }
            pathHelper.setNotesFolderPath(path = uri.toString())
            notePath = path
        }
    }

    val trackPathLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
        val path = pathHelper.getPathFromUri(uri = uri)
        if (path.isNotEmpty()) {
            try { context.contentResolver.takePersistableUriPermission(uri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (e: SecurityException) { Log.e("SettingsScreen", "Failed to take URI permission: ${e.message}") }
            pathHelper.setTracksFolderPath(path = uri.toString())
            musicPath = path
        }
    }

    val fullStorageAccessLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        fullStorageAccessGranted = Environment.isExternalStorageManager()
    }

    Column(
        modifier = modifier.fillMaxSize().systemBarsPadding().background(SurfaceDark).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) {
                Icon(painter = painterResource(R.drawable.back), contentDescription = "Back", tint = OnSurfacePrimary, modifier = Modifier.size(32.dp))
            }
            Text(text = "Настройки", color = OnSurfacePrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Storage Access", color = OnSurfacePrimary, style = MaterialTheme.typography.titleMedium)
                    Text(if (fullStorageAccessGranted) "Granted" else "Not Granted", color = OnSurfaceSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { data = "package:${context.packageName}".toUri() }
                    fullStorageAccessLauncher.launch(intent)
                }, colors = ButtonDefaults.buttonColors(containerColor = Blue60)) {
                    Text(if (fullStorageAccessGranted) "Granted" else "Request")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard(title = "Note Path", subtitle = notePath) { notePathLauncher.launch(null) }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard(title = "Music Path", subtitle = musicPath) { trackPathLauncher.launch(null) }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Постоянный уровень громкости", color = OnSurfacePrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = replayGainEnabled,
                    onCheckedChange = {
                        replayGainEnabled = it
                        pathHelper.setReplayGainEnabled(it)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = Blue60),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.fixDefaultCoverValues() },
            colors = ButtonDefaults.buttonColors(containerColor = Blue60)
        ) {
            Text(text = "Очистить cover=\"_No Album Art.jpg\"")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.extractAndAssignCovers() },
            colors = ButtonDefaults.buttonColors(containerColor = Blue60)
        ) {
            Text(text = "Извлечь обложки из аудиофайлов")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.scanAll(true) },
            enabled = !viewModel.isScan,
            colors = ButtonDefaults.buttonColors(containerColor = Blue60)
        ) {
            if (viewModel.isScan) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = OnSurfacePrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Text(text = if (viewModel.isScan) "Scanning..." else "Clean Index and Rescan")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.scanAll(false) },
            enabled = !viewModel.isScan,
            colors = ButtonDefaults.buttonColors(containerColor = Blue60)
        ) {
            if (viewModel.isScan) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = OnSurfacePrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Text(text = if (viewModel.isScan) "Scanning..." else "Rescan")
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnSurfacePrimary, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = OnSurfaceSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onClick) {
                Icon(painter = painterResource(R.drawable.folder), contentDescription = "Select $title", tint = OnSurfaceSecondary)
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(viewModel = MusicPlayerViewModel(LocalContext.current))
}

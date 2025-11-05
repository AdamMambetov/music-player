package com.example.musicplayer

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MusicPlayerScreen(
    modifier: Modifier = Modifier,
    musicPlayerViewModel: MusicPlayerViewModel = viewModel(),
    onTrackSelected: (Context, MusicInfo) -> Unit = { _, _ -> }
) {
    val isPlaying = remember { musicPlayerViewModel.isPlaying }
    val currentPosition = remember { musicPlayerViewModel.currentPosition }
    val duration = remember { musicPlayerViewModel.duration }
    val musicInfoList = remember { musicPlayerViewModel.musicInfoList }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "My Music",
            modifier = Modifier.padding(bottom = 16.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        LazyColumn {
            items(musicInfoList.size) { index ->
                val musicInfo = musicInfoList[index]
                val trackName = if (musicInfo.album.isNotEmpty() && musicInfo.creator.isNotEmpty()) {
                    "${musicInfo.album} - ${musicInfo.creator.first()}"
                } else if (musicInfo.album.isNotEmpty()) {
                    musicInfo.album
                } else if (musicInfo.creator.isNotEmpty()) {
                    musicInfo.creator.first()
                } else {
                    // Use the source file name if no album or creator info
                    val fileName = musicInfo.sourceFile.substringBeforeLast(".")
                    fileName.ifEmpty { "Unknown Track" }
                }
                
                ListItem(
                    headlineContent = { Text(text = trackName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onTrackSelected(context, musicInfo)
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }

        // Now playing section at the bottom
        Spacer(modifier = Modifier.weight(1f))

        // Progress slider and controls
        Column {
            Slider(
                value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) * 10 else 0f,
                onValueChange = {
                    musicPlayerViewModel.seekTo(((it / 100) * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { musicPlayerViewModel.previousTrack() }) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { if (isPlaying) musicPlayerViewModel.pause() else musicPlayerViewModel.play() }) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { musicPlayerViewModel.nextTrack() }) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
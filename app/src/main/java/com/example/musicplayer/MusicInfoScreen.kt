package com.example.musicplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MusicInfoScreen(
    trackName: String,
    modifier: Modifier = Modifier,
    musicPlayerViewModel: MusicPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackClicked: () -> Unit = {},
) {
    val isPlaying = musicPlayerViewModel.isPlaying
    val currentPosition = musicPlayerViewModel.currentPosition
    val duration = musicPlayerViewModel.duration
    
    // Start playing when the screen is shown (if not already playing)
    androidx.compose.runtime.LaunchedEffect(trackName) {
        // The media source should already be set by MainActivity
        // Just make sure to start playing if not already
        if (!isPlaying) {
            musicPlayerViewModel.play()
        }
    }
    
    // Extract artist name from track string (assuming format "Album - Artist Name")
    val parts = trackName.split(" - ")
    val artistName = if (parts.size > 1) {
        parts[1]
    } else {
        "Unknown Artist"
    }
    
    val trackTitle = if (parts.size > 1) {
        parts[0]
    } else {
        trackName
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { onBackClicked() }) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = "Back",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Album art placeholder
        Box(
            modifier = Modifier
                .size(250.dp)
                .padding(16.dp)
                .background(Color.LightGray)
        ) {
            Text(
                text = "Album Art",
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Track info
        Text(
            text = trackTitle,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = artistName,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Progress slider
        Slider(
            value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) * 10 else 0f,
            onValueChange = { 
                musicPlayerViewModel.seekTo(((it / 100) * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Player controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun MusicInfoScreenPreview() {
    MusicInfoScreen(trackName = "Sample Track - Sample Artist")
}
package com.example.musicplayer

import android.annotation.SuppressLint
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MusicInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBackClicked: () -> Unit = {},
) {
    val isPlaying = viewModel.isPlaying
    val isShuffle = viewModel.isShuffle
    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration
    val context = LocalContext.current
    
    // Start playing when the screen is shown (if not already playing)
    LaunchedEffect(key1 = Unit) {
        // The media source should already be set by MainActivity
        // Just make sure to start playing if not already
        if (!isPlaying) {
            viewModel.play(context)
        }
    }

    val name = viewModel
        .currentTrack
        .aliases
        .getOrElse(0) { "Unknown Music" }
    val artists = viewModel
        .currentTrack
        .creators
        .joinToString(", ")
        .ifEmpty { "Unknow Artist" }
    val album = viewModel
        .currentTrack
        .album
        .ifEmpty { "Unknown Album" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
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

        Text(text = viewModel.currentTrack.listenInSec.toString())

        Spacer(modifier = Modifier.weight(1f))

        // Track info
        Text(
            text = name,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = artists,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = album,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Progress slider
        Slider(
            valueRange = 0f..duration.toFloat(),
            value = currentPosition.toFloat(),
            onValueChange = {
                viewModel.seekTo(context, it.toLong())
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
            IconButton(
                onClick = {
                    if (isShuffle) viewModel.disableShuffle()
                    else viewModel.enableShuffle()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(64.dp),
                    tint = if (isShuffle) Color.Unspecified else Color.LightGray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { viewModel.previousTrack() }) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "Previous",
                    modifier = Modifier.size(64.dp)
                )
            }

            IconButton(
                onClick = {
                    if (isPlaying) viewModel.pause()
                    else viewModel.play(context)
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.pause
                             else R.drawable.play_arrow
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(64.dp)
                )
            }

            IconButton(onClick = { viewModel.nextTrack() }) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Next",
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.favorite),
                    contentDescription = "Favorite",
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray,
                )
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun MusicInfoScreenPreview() {
    MusicInfoScreen(
        viewModel = MusicPlayerViewModel(
            MusicPlayerSearchManager(LocalContext.current)
        ),
    )
}
package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.example.musicplayer.MusicPlayerSearchManager
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.R
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.TrackDocument

@Composable
fun MusicInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBackClicked: () -> Unit = {},
) {
    val isPlaying = viewModel.isPlaying
    val isShuffle = viewModel.isShuffle
    val isFavorite = viewModel.isFavorite
    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = Unit) {
        if (!isPlaying) {
            viewModel.play()
        }
    }

    val name = viewModel
        .currentTrack
        .aliases
        .getOrElse(index = 0) { "Unknown Track" }
        .ifEmpty { "Unknown Track" }
    val artists = viewModel
        .currentTrack
        .creators.joinToString(separator = ", ") {
            it.aliases
                .getOrElse(index = 0) { "Unknow Artist" }
                .ifEmpty { "Unknow Artist" }
        }
        .ifEmpty { "Unknow Artist" }
    val album = viewModel
        .currentTrack
        .album
        .ifEmpty { "Unknown Album" }
    val coverUri = viewModel.getCoverUri(
        coverString = viewModel.currentTrack.cover,
    )

    if (showAddToPlaylistDialog)
        AddToPlaylistDialog(
            onExitRequest = { showAddToPlaylistDialog = false },
            onPlaylistChecked = { checked, playlist ->
                if (playlist == viewModel.favorites) {
                    viewModel.changeTrackFavoriteState(viewModel.currentTrack)
                    return@AddToPlaylistDialog
                }

                val list = playlist.tracklist.toMutableList()
                if (checked)
                    list.add(viewModel.currentTrack)
                else
                    list.removeIf { it == viewModel.currentTrack }
                viewModel.savePlaylist(playlist.copy(tracklist = list))
            },
            track = viewModel.currentTrack,
            allPlaylists = viewModel.allPlaylists,
        )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            IconButton(onClick = { onBackClicked() }) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = "Back",
                    modifier = Modifier.size(32.dp)
                )
            }
            // Add to playlist button
            IconButton(onClick = { showAddToPlaylistDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.add_link),
                    contentDescription = "Add to playlist",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        AsyncImage(
            model = coverUri,
            contentDescription = null,
            modifier = Modifier
                .size(250.dp),
        )
        // Album art placeholder
//        Box(
//            modifier = Modifier
//                .size(250.dp)
//                .padding(16.dp)
//                .background(Color.LightGray)
//        ) {
//            Text(
//                text = "Album Art",
//                textAlign = TextAlign.Center,
//                fontSize = 16.sp
//            )
//        }

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
                viewModel.seekTo(it.toLong())
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
                    else viewModel.play()
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

            IconButton(
                onClick = {
                    viewModel.changeTrackFavoriteState(viewModel.currentTrack)
                },
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isFavorite) R.drawable.favorite_filled
                            else R.drawable.favorite_outline
                    ),
                    contentDescription = "Favorite",
                    modifier = Modifier.size(64.dp),
                )
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    onExitRequest: () -> Unit = {},
    onPlaylistChecked: (checked: Boolean, playlist: PlaylistDocument) -> Unit = {_, _ ->},
    track: TrackDocument,
    allPlaylists: List<PlaylistDocument>,
) {
    val playlistCheckedList = allPlaylists.map { playlist ->
        playlist.tracklist.find { it == track } != null
    }.toMutableStateList()

    Dialog(onDismissRequest = onExitRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Text(
                    text = "Add to playlists",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    textAlign = TextAlign.Center
                )
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onExitRequest) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Close dialog",
                            tint = Color.Black
                        )
                    }
                }
            }

            allPlaylists.forEachIndexed { i, playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp)
                        .border(
                            width = 1.dp,
                            color = Color.Black,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            val checked = !playlistCheckedList[i]
                            onPlaylistChecked(checked, playlist)
                            playlistCheckedList[i] = checked
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = playlistCheckedList[i],
                        onCheckedChange = { checked ->
                            playlistCheckedList[i] = checked
                            onPlaylistChecked(checked, playlist)
                        }
                    )
                    Text(
                        text = playlist.aliases.getOrElse(0) { "Unknown Playlist" },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    )
                }
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
            context = LocalContext.current,
            searchManager = MusicPlayerSearchManager(LocalContext.current),
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun AddToPlaylistDialogPreview() {
    val track = TrackDocument.createEmpty().copy(id = "preview")
    AddToPlaylistDialog(
        track = track,
        allPlaylists = listOf(
            PlaylistDocument.createEmpty().copy(tracklist = listOf(track)),
            PlaylistDocument.createEmpty(),
            PlaylistDocument.createEmpty(),
            PlaylistDocument.createEmpty(),
        ),
    )
}
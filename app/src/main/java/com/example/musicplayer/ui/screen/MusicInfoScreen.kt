package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.shadow
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
    val track = viewModel.currentTrack
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = Unit) {
        if (!isPlaying) {
            viewModel.play()
        }
    }

    val name = track
        .aliases
        .getOrElse(index = 0) { "Unknown Track" }
        .ifEmpty { "Unknown Track" }
    val artists = track
        .creators.joinToString(separator = ", ") {
            it.aliases
                .getOrElse(index = 0) { "Unknow Artist" }
                .ifEmpty { "Unknow Artist" }
        }
        .ifEmpty { "Unknow Artist" }
    val album = track
        .album
        .ifEmpty { "Unknown Album" }
    val coverUri = viewModel.getCoverUri(
        coverString = track.cover,
    )

    AnimatedVisibility(showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            onExitRequest = { showAddToPlaylistDialog = false },
            onPlaylistChecked = { checked, playlist ->
                if (playlist == viewModel.favorites) {
                    viewModel.changeTrackFavoriteState(viewModel.currentTrack)
                    return@AddToPlaylistDialog
                }

                val list = playlist.tracklist.toMutableList()
                Log.d("TAG", "onPlaylistChecked checked = $checked, playlist = ${playlist.fileName} ${playlist.tracklist.size}")
                if (checked)
                    list.add(viewModel.currentTrack)
                else
                    list.removeIf { it == viewModel.currentTrack }
                Log.d("TAG", "onPlaylistChecked list size ${list.size}")
                viewModel.savePlaylist(playlist.copy(tracklist = list))
            },
            track = viewModel.currentTrack,
            allPlaylists = viewModel.allPlaylists,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
                .border(
                    width = 1.dp,
                    shape = RoundedCornerShape(size = 30.dp),
                    color = Color.White,
                )
        ) {
            Text(
                text = name,
                modifier = Modifier.padding(top = 15.dp, start = 15.dp),
                color = Color.White,
                fontSize = 16.sp,
            )
            Text(
                text = artists,
                modifier = Modifier.padding(start = 15.dp),
                color = Color.White,
                fontSize = 14.sp,
            )
            Text(
                text = album,
                modifier = Modifier.padding(start = 15.dp, bottom = 5.dp),
                color = Color.White,
                fontSize = 14.sp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .border(
                        width = 1.dp,
                        shape = RoundedCornerShape(size = 30.dp),
                        color = Color.White,
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "00:00", color = Color.White)
                    Slider(
                        valueRange = 0f..duration.toFloat(),
                        value = currentPosition.toFloat(),
                        onValueChange = {
                            viewModel.seekTo(it.toLong())
                        },
                        modifier = Modifier.weight(1F)
                    )
                    Text(text = "04:00", color = Color.White)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F)
                        .border(
                            width = 1.dp,
                            shape = RoundedCornerShape(size = 30.dp),
                            color = Color.White,
                        )
                ) {
                    Row {
                        for (i in 0 until 10) {
                            Text(
                                text = i.toString(),
                                modifier = Modifier
                                    .padding(all = 15.dp),
                                color = Color.White,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize(1F)
                            .padding(50.dp)
                            .background(Color.Cyan)
                            .align(alignment = Alignment.CenterHorizontally),
                    ) {
                        AsyncImage(
                            model = coverUri,
                            contentDescription = null,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { viewModel.previousTrack() },
                        modifier = Modifier.size(50.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            modifier = Modifier.size(50.dp),
                            tint = Color.White,
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isPlaying) viewModel.pause()
                            else viewModel.play()
                        },
                        modifier = Modifier.size(50.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isPlaying) R.drawable.pause
                                else R.drawable.play_arrow
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(50.dp),
                            tint = Color.White,
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextTrack() },
                        modifier = Modifier.size(50.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            modifier = Modifier.size(50.dp),
                            tint = Color.White,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        modifier = Modifier.size(32.dp),
                        tint = if (isShuffle) Color.White else Color.DarkGray,
                    )
                }

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
                        modifier = Modifier.size(32.dp),
                        tint = Color.White,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .size(100.dp)
                .background(color = Color.Blue)
        ) {}
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
@Preview()
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
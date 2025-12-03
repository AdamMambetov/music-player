package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.MusicPlayerSearchManager
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.R
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.TrackDocument
import my.nanihadesuka.compose.LazyColumnScrollbar


@Composable
fun AllPlaylists(
    modifier: Modifier = Modifier,
    onPlaylistSelected: (PlaylistDocument) -> Unit = { _ -> },
    allPlaylists: List<PlaylistDocument>,
) {
    val listState = rememberLazyListState()

    LazyColumnScrollbar(
        state = listState,
        modifier = modifier.padding(bottom = 70.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            items(items = allPlaylists, key = { it.id }) { playlist ->
                val playlistName = playlist
                    .aliases
                    .getOrElse(0) { "Unknown Playlist" }
                    .ifEmpty { "Unknown Playlist" }

                Row(
                    modifier = Modifier
                        .background(color = Color.LightGray)
                        .padding(30.dp)
                        .clickable(onClick = { onPlaylistSelected(playlist) }),
                ) {
                    Text(text = playlistName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = playlist.tracklist.size.toString(), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PlaylistTracks(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onTrackSelected: (TrackDocument) -> Unit = {},
    onBackClicked: () -> Unit = {},
    playlist: PlaylistDocument = remember { viewModel.currentPlaylist },
) {
    val listState = rememberLazyListState()

    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
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
        }
        Box {
            LazyColumnScrollbar(
                state = listState,
                modifier = Modifier.padding(bottom = 70.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                ) {
                    items(
                        items = playlist.tracklist,
                        key = { track -> track.id },
                    ) { track ->
                        TrackListItem(
                            trackInfo = track,
                            viewModel = viewModel,
                            onTrackSelected = onTrackSelected,
                        )
                    }
                }
            }

            BottomScrollControls(listState, viewModel, playlist.tracklist)
            BottomPlayerControls(viewModel)
        }
    }
}



@Preview(showBackground = true)
@Composable
fun PlaylistScreenPreview() {
    AllPlaylists(
        allPlaylists = listOf(
            PlaylistDocument.createEmpty().copy(
                aliases = listOf("Favorites"),
                tracklist = listOf(
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                )
            ),
            PlaylistDocument.createEmpty(),
            PlaylistDocument.createEmpty(),
            PlaylistDocument.createEmpty(),
        )
    )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PlaylistTracksPreview() {
    PlaylistTracks(
        viewModel = MusicPlayerViewModel(
            context = LocalContext.current,
            searchManager = MusicPlayerSearchManager(LocalContext.current),
        ),
        playlist = PlaylistDocument.createEmpty().copy(
            aliases = listOf("Favorites"),
            tracklist = listOf(
                TrackDocument.createEmpty(),
                TrackDocument.createEmpty(),
                TrackDocument.createEmpty(),
                TrackDocument.createEmpty(),
                TrackDocument.createEmpty(),
                TrackDocument.createEmpty(),
            )
        ),
    )
}
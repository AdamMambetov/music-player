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
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.R
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.TrackDocument
import my.nanihadesuka.compose.LazyColumnScrollbar


@Composable
fun AllAlbums(
    modifier: Modifier = Modifier,
    onAlbumSelected: (AlbumDocument) -> Unit = { _ -> },
    allAlbums: List<AlbumDocument>,
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
            items(items = allAlbums, key = { it.id }) { album ->
                val albumName = album
                    .aliases
                    .getOrElse(0) { AlbumDocument.UNKNOWN }
                    .ifEmpty { AlbumDocument.UNKNOWN }

                Row(
                    modifier = Modifier
                        .background(color = Color.LightGray)
                        .padding(30.dp)
                        .clickable(onClick = { onAlbumSelected(album) }),
                ) {
                    Text(
                        text = albumName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = album.tracklist.size.toString(),
                        fontSize = 16.sp,
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumTracks(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onTrackSelected: (TrackDocument) -> Unit = {},
    onBackClicked: () -> Unit = {},
    album: AlbumDocument = remember { viewModel.currentAlbum },
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
                        items = album.tracklist,
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

            BottomScrollControls(listState, viewModel, album.tracklist)
            BottomPlayerControls(viewModel)
        }
    }
}



@Preview(showBackground = true)
@Composable
fun AlbumScreenPreview() {
    AllAlbums(
        allAlbums = listOf(
            AlbumDocument.createEmpty().copy(
                aliases = listOf("Favorite Album"),
                tracklist = listOf(
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                    TrackDocument.createEmpty(),
                )
            ),
            AlbumDocument.createEmpty(),
            AlbumDocument.createEmpty(),
            AlbumDocument.createEmpty(),
        )
    )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun AlbumTracksPreview() {
    AlbumTracks(
        viewModel = MusicPlayerViewModel(
            context = LocalContext.current,
        ),
        album = AlbumDocument.createEmpty().copy(
            aliases = listOf("Favorite Album"),
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

package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.ui.components.AlbumCover
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.theme.OnSurfacePrimary
import com.example.musicplayer.ui.theme.OnSurfaceSecondary
import com.example.musicplayer.ui.theme.SurfaceDark

@Composable
fun AllAlbums(
    modifier: Modifier = Modifier,
    onAlbumSelected: (AlbumDocument) -> Unit = { _ -> },
    allAlbums: List<AlbumDocument>,
    getCoverUri: (String) -> String = { "" },
    onBack: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnSurfacePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                "Альбомы (${allAlbums.size})",
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)) {
            items(items = allAlbums, key = { it.id }) { album ->
                val albumName = album.aliases.getOrElse(0) { AlbumDocument.UNKNOWN }
                    .ifEmpty { AlbumDocument.UNKNOWN }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAlbumSelected(album) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumCover(
                        modifier = Modifier.size(56.dp),
                        label = albumName,
                        coverUri = getCoverUri(album.cover),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            albumName,
                            color = OnSurfacePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(album.creators.joinToString(", ") { it.aliases.getOrElse(0) { "Unknown" } }
                            .ifEmpty { "Unknown" }, color = OnSurfaceSecondary, fontSize = 13.sp)
                    }
                    Text("${album.tracklist.size}", color = OnSurfaceSecondary, fontSize = 14.sp)
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
    onBack: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val currentAlbum = viewModel.currentAlbum
    val albumName = currentAlbum.aliases.getOrElse(0) { AlbumDocument.UNKNOWN }

    Column(modifier = modifier
        .fillMaxSize()
        .systemBarsPadding()
        .background(SurfaceDark)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnSurfacePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                albumName,
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(items = currentAlbum.tracklist, key = { it.id }) { track ->
                    val latestListenInSec = viewModel.allTracks.find { it.id == track.id }?.listenInSec ?: track.listenInSec
                    TrackListItem(
                        track = track,
                        isActive = track.id == viewModel.currentTrack.id,
                        coverUri = viewModel.getCoverUri(coverString = track.cover),
                        listenInSec = latestListenInSec,
                        onClick = { onTrackSelected(track) })
                }
            }
            BottomScrollControls(listState, viewModel, currentAlbum.tracklist)
        }

        BottomPlayerMini(viewModel)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AlbumTracksPreview() {
    AlbumTracks(viewModel = MusicPlayerViewModel(LocalContext.current))
}

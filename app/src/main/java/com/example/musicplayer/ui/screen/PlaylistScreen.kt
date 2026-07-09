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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.ui.components.AlbumCover
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.theme.OnSurfacePrimary
import com.example.musicplayer.ui.theme.OnSurfaceSecondary
import com.example.musicplayer.ui.theme.SurfaceDark

@Composable
fun AllPlaylists(
    modifier: Modifier = Modifier,
    onPlaylistSelected: (PlaylistDocument) -> Unit = { _ -> },
    allPlaylists: List<PlaylistDocument>,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().systemBarsPadding().background(SurfaceDark)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurfacePrimary, modifier = Modifier.size(32.dp)) }
            Text("Плейлисты (${allPlaylists.size})", color = OnSurfacePrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            items(items = allPlaylists, key = { it.id }) { playlist ->
                val playlistName = playlist.aliases.getOrElse(0) { PlaylistDocument.UNKNOWN }.ifEmpty { PlaylistDocument.UNKNOWN }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlaylistSelected(playlist) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumCover(modifier = Modifier.size(48.dp), label = playlistName, shape = RoundedCornerShape(8.dp))
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlistName, color = OnSurfacePrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${playlist.tracklist.size} треков", color = OnSurfaceSecondary, fontSize = 13.sp)
                    }
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
    onBack: () -> Unit = {},
    playlist: PlaylistDocument = remember { viewModel.currentPlaylist },
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().systemBarsPadding().background(SurfaceDark)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurfacePrimary, modifier = Modifier.size(32.dp)) }
            Text(playlist.aliases.getOrElse(0) { PlaylistDocument.UNKNOWN }, color = OnSurfacePrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(items = playlist.tracklist, key = { it.id }) { track ->
                    TrackListItem(track = track, isActive = track == viewModel.currentTrack, coverUri = viewModel.getCoverUri(coverString = track.cover), onClick = { onTrackSelected(track) })
                }
            }
            BottomScrollControls(listState, viewModel, playlist.tracklist)
        }

        BottomPlayerMini(viewModel)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlaylistTracksPreview() {
    PlaylistTracks(viewModel = MusicPlayerViewModel(LocalContext.current), playlist = PlaylistDocument.createEmpty().copy(tracklist = listOf(TrackDocument.createEmpty())))
}

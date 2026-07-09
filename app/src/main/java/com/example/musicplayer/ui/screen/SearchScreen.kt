package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.R
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.ui.components.AlbumCover
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.theme.Amber60
import com.example.musicplayer.ui.theme.DividerColor
import com.example.musicplayer.ui.theme.OnSurfacePrimary
import com.example.musicplayer.ui.theme.OnSurfaceSecondary
import com.example.musicplayer.ui.theme.SurfaceCard
import com.example.musicplayer.ui.theme.SurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onTrackSelected: (TrackDocument) -> Unit,
    onBack: () -> Unit = {},
) {
    val musicState = viewModel.musicState
    val albums = viewModel.allAlbums

    LaunchedEffect(key1 = Unit) { viewModel.onSearchQueryChange("") }

    Scaffold(
        modifier = modifier.systemBarsPadding(),
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(value = musicState.searchQuery, onValueChange = viewModel::onSearchQueryChange, placeholder = { Text("Поиск песен, альбомов, артистов...") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amber60, unfocusedBorderColor = DividerColor, focusedTextColor = OnSurfacePrimary, unfocusedTextColor = OnSurfacePrimary, cursorColor = Amber60, focusedPlaceholderColor = OnSurfaceSecondary, unfocusedPlaceholderColor = OnSurfaceSecondary), modifier = Modifier.fillMaxWidth())
                },
                navigationIcon = { IconButton(onClick = { onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurfacePrimary) } },
                actions = { IconButton(onClick = {}) { Icon(painterResource(R.drawable.search), contentDescription = "Search", tint = OnSurfacePrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(albums) { album ->
                    val albumName = album.aliases.getOrElse(0) { "Unknown" }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AlbumCover(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)), label = albumName, shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(albumName, color = OnSurfacePrimary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(viewModel.allCreators.take(10)) { creator ->
                    val name = creator.aliases.getOrElse(0) { "Unknown" }
                    AssistChip(onClick = { viewModel.onSearchQueryChange(name) }, label = { Text(name, fontSize = 12.sp) }, colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCard))
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(musicState.trackList) { track ->
                    TrackListItem(track = track, isActive = track == viewModel.currentTrack, coverUri = viewModel.getCoverUri(coverString = track.cover), onClick = { viewModel.setQueueToDefault(); viewModel.setMediaSourceWithService(track); onTrackSelected(track) })
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen(modifier = Modifier.fillMaxSize(), viewModel = MusicPlayerViewModel(LocalContext.current), onTrackSelected = {})
}

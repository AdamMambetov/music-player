package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.R
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.ui.components.AlbumCover
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.theme.Blue60
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
    onTrackSelected: (TrackDocument) -> Unit = {},
    onCreatorSelected: (CreatorDocument) -> Unit = {},
    onAlbumSelected: (AlbumDocument) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val musicState = viewModel.musicState
    val isSearching = musicState.searchQuery.isNotBlank()

    LaunchedEffect(key1 = Unit) { viewModel.onSearchQueryChange("") }

    Scaffold(
        modifier = modifier.systemBarsPadding(),
        containerColor = SurfaceDark,
        topBar = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnSurfacePrimary
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = musicState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = OnSurfacePrimary,
                        fontSize = 18.sp
                    ),
                    cursorBrush = SolidColor(Blue60),
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceCard, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (musicState.searchQuery.isEmpty()) {
                                Text(
                                    "Треки, артисты, альбомы...",
                                    color = OnSurfaceSecondary,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                IconButton(onClick = {}) {
                    Icon(
                        painterResource(R.drawable.search),
                        contentDescription = "Search",
                        tint = OnSurfacePrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(horizontal = 12.dp)) {
            val displayAlbums = if (isSearching) musicState.albumList else viewModel.allAlbums
            val displayCreators =
                if (isSearching) musicState.creatorList else viewModel.allCreators.take(10)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(displayAlbums) { album ->
                    val albumName = album.aliases.getOrElse(0) { "Unknown" }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onAlbumSelected(album) }
                    ) {
                        AlbumCover(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            label = albumName,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(albumName, color = OnSurfacePrimary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(displayCreators) { creator ->
                    val name = creator.aliases.getOrElse(0) { "Unknown" }
                    AssistChip(
                        onClick = { onCreatorSelected(creator) },
                        label = { Text(name, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCard)
                    )
                }
            }
            val searchListState = rememberLazyListState()
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = searchListState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(musicState.trackList) { track ->
                        TrackListItem(
                            track = track,
                            isActive = track.id == viewModel.currentTrack.id,
                            coverUri = viewModel.getCoverUri(coverString = track.cover),
                            onClick = {
                                viewModel.setQueueToDefault(); viewModel.setMediaSourceWithService(
                                track
                            ); onTrackSelected(track)
                            })
                    }
                }
                BottomScrollControls(searchListState, viewModel, musicState.trackList)
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = MusicPlayerViewModel(LocalContext.current)
    )
}

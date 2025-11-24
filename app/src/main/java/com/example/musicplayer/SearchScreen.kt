package com.example.musicplayer

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.data.TrackDocument

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onTrackSelected: (TrackDocument) -> Unit = { _ -> },
    viewModel: MusicPlayerViewModel,
) {
    val musicState = viewModel.musicState

    LaunchedEffect(key1 = Unit) {
        viewModel.onSearchQueryChange("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        TextField(
            value = musicState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text("Search tracks...") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Search results
        LazyColumn {
            items(musicState.trackList) { music ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable(onClick = { onTrackSelected(music) })
                ) {
                    Text(text = music.aliases.toString())
                    Text(text = music.creators.toString())
                    Text(text = music.year.toString())
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen(
        viewModel = MusicPlayerViewModel(
            MusicPlayerSearchManager(LocalContext.current)
        ),
    )
}
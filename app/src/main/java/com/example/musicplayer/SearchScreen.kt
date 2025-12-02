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
import com.example.musicplayer.data.TrackDocument
import kotlin.collections.ifEmpty
import kotlin.collections.joinToString
import kotlin.text.ifEmpty

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onTrackSelected: (TrackDocument) -> Unit,
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
            items(musicState.trackList) { track ->
                val aliases = track.aliases
                    .getOrElse(0) { "Unknown Music" }
                    .ifEmpty { "Unknown Music" }
                val creators = track.creators
                    .map { it.aliases.getOrElse(0) { "Unknow Artist" } }
                    .ifEmpty { listOf("Unknow Artist") }
                    .joinToString(", ")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable(onClick = { onTrackSelected(track) })
                ) {
                    Text(text = aliases)
                    Text(text = creators)
                    Text(text = track.year.toString())
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
            searchManager = MusicPlayerSearchManager(LocalContext.current)
        ),
        onTrackSelected = {},
    )
}
package com.example.musicplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onTrackSelected: (MusicInfo) -> Unit = {},
    musicPlayerViewModel: MusicPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    
    val musicInfoList = remember { musicPlayerViewModel.musicInfoList }
    
    // Create track list from music info
    val allTracks = musicInfoList.map { musicInfo ->
        if (musicInfo.album.isNotEmpty() && musicInfo.creator.isNotEmpty()) {
            "${musicInfo.album} - ${musicInfo.creator.first()}"
        } else if (musicInfo.album.isNotEmpty()) {
            musicInfo.album
        } else if (musicInfo.creator.isNotEmpty()) {
            musicInfo.creator.first()
        } else {
            // Use the source file name if no album or creator info
            val fileName = musicInfo.sourceFile.substringBeforeLast(".")
            fileName.ifEmpty { "Unknown Track" }
        }
    }
    
    // Filter tracks based on search query
    val filteredTracks = if (searchQuery.text.isEmpty()) {
        allTracks
    } else {
        allTracks.filter { track ->
            track.contains(searchQuery.text, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
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
            items(filteredTracks) { track ->
                ListItem(
                    headlineContent = { Text(text = track) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackSelected(MusicInfo()) } // TODO
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SearchScreenPreview() {
    SearchScreen(onTrackSelected = { /* Preview action */ })
}
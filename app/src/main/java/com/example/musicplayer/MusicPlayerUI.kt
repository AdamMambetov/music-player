package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.data.TrackDocument
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar

@Composable
fun MusicPlayerScreen(
    modifier: Modifier,
    viewModel: MusicPlayerViewModel,
    onTrackSelected: (Context, TrackDocument) -> Unit = { _, _ -> },
    debugList: List<TrackDocument> = listOf(),
) {
    val context = LocalContext.current
    val isPlaying = remember { viewModel.isPlaying }
    val musicInfoList = remember { viewModel.allTracks }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        LazyColumnScrollbar(
            state = listState,
            modifier = Modifier.padding(bottom = 70.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                items(if (debugList.isEmpty()) musicInfoList.size else debugList.size) { index ->
                    val musicInfo = if (debugList.isEmpty())musicInfoList[index]
                        else debugList[index]
                    val name = musicInfo
                        .aliases
                        .getOrElse(0) { "Unknown Music" }
                        .ifEmpty { "Unknown Music" }
                    val artists = musicInfo
                        .creators
                        .map { it.aliases.getOrElse(0) { "Unknow Artist" } }
                        .ifEmpty { listOf("Unknow Artist") }
                        .joinToString(", ")
                    val album = musicInfo
                        .album
                        .ifEmpty { "Unknown Album" }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTrackSelected(context, musicInfo)
                            }
                            .padding(horizontal = 5.dp)
                            .padding(top = 5.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index == viewModel.currentQueueIndex) Color.LightGray
                                    else Color.Gray
                                )
                                .padding(5.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 14.sp,
                            )
                            Text(
                                text = artists,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                            )
                            Text(
                                text = album,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                            )
                        }
                    }
                }
            }
        }


        // Bottom Scroll Controls
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 75.dp, end = 5.dp)
            ) {
                IconButton(
                    onClick = { coroutineScope.launch { listState.scrollToItem(0) } },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(50)
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.double_arrow_up),
                        contentDescription = "Previous",
                        modifier = Modifier.size(64.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (viewModel.currentQueueIndex < 0)
                            return@IconButton
                        coroutineScope.launch {
                            listState.scrollToItem(viewModel.currentQueueIndex)
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(50)
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrows_input),
                        contentDescription = "Previous",
                        modifier = Modifier.size(64.dp)
                    )
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.scrollToItem(musicInfoList.lastIndex)
                        }
                    },
                    modifier = Modifier
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(50)
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.double_arrow_down),
                        contentDescription = "Previous",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // Bottom Player Controls
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(Color.White),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousTrack() }) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (isPlaying) viewModel.pause()
                        else viewModel.play(context)
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.pause
                            else R.drawable.play_arrow
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(onClick = { viewModel.nextTrack() }) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Composable
@Preview(showBackground = true)
fun MusicPlayerScreenPreview() {
    MusicPlayerScreen(
        viewModel = MusicPlayerViewModel(
            MusicPlayerSearchManager(LocalContext.current)
        ),
        modifier = Modifier,
        debugList = listOf(
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
            TrackDocument.createEmpty(),
        )
    )
}
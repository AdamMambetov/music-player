package com.example.musicplayer.ui.screen

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.musicplayer.MusicPlayerViewModel
import com.example.musicplayer.R
import com.example.musicplayer.data.AlbumDocument
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.PlaylistDocument
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.ui.components.AddToPlaylistDialog
import com.example.musicplayer.ui.components.AlbumCover
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.components.formatListenTime
import com.example.musicplayer.ui.components.formatTime
import com.example.musicplayer.ui.theme.Blue60
import com.example.musicplayer.ui.theme.DividerColor
import com.example.musicplayer.ui.theme.OnSurfacePrimary
import com.example.musicplayer.ui.theme.OnSurfaceSecondary
import com.example.musicplayer.ui.theme.SurfaceCard
import com.example.musicplayer.ui.theme.SurfaceDark
import kotlinx.coroutines.launch

private val BorderColor = Color.White.copy(alpha = 0.25f)

@Composable
fun MusicPlayerScreen(
    modifier: Modifier,
    viewModel: MusicPlayerViewModel,
    onMoveTo: (to: String) -> Unit = {},
) {
    val isPlaying = viewModel.isPlaying
    val isShuffle = viewModel.isShuffle
    val isRepeat = viewModel.isRepeat
    val isFavorite = viewModel.isFavorite
    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration

    val name = viewModel.currentTrack.aliases.getOrElse(0) { TrackDocument.UNKNOWN }
        .ifEmpty { TrackDocument.UNKNOWN }
    val artists = viewModel.currentTrack.creators.joinToString(", ") {
        it.aliases.getOrElse(0) { CreatorDocument.UNKNOWN }
    }.ifEmpty { CreatorDocument.UNKNOWN }
    val currentListen = viewModel.currentListenInSec
    val currentTrackId = viewModel.currentTrack.id

    val coverUri = rememberCoverUri(viewModel)

    val density = LocalDensity.current
    var border1Y by remember { mutableFloatStateOf(0f) }
    var border2Y by remember { mutableFloatStateOf(0f) }
    var border2H by remember { mutableFloatStateOf(0f) }
    var border3Y by remember { mutableFloatStateOf(0f) }
    var border3H by remember { mutableFloatStateOf(0f) }
    var tabsY by remember { mutableFloatStateOf(0f) }

    val stroke = remember { Stroke(width = with(density) { 1.dp.toPx() }) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = BorderColor,
                    topLeft = Offset(0f, border1Y),
                    size = Size(size.width, tabsY - border1Y - with(density) { 4.dp.toPx() }),
                    cornerRadius = CornerRadius(with(density) { 20.dp.toPx() }),
                    style = stroke
                )
                drawRoundRect(
                    color = BorderColor,
                    topLeft = Offset(0f, border2Y),
                    size = Size(size.width, border2H),
                    cornerRadius = CornerRadius(with(density) { 16.dp.toPx() }),
                    style = stroke
                )
                drawRoundRect(
                    color = BorderColor,
                    topLeft = Offset(0f, border3Y),
                    size = Size(size.width, border3H),
                    cornerRadius = CornerRadius(with(density) { 14.dp.toPx() }),
                    style = stroke
                )
            }
            .background(SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                var showAliasesDialog by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.align(Alignment.CenterStart).padding(end = 80.dp)) {
                        Text(
                            name,
                            color = OnSurfacePrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { showAliasesDialog = true }
                        )
                        Text(
                            artists,
                            color = OnSurfaceSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showAliasesDialog) {
                        AlertDialog(
                            onDismissRequest = { showAliasesDialog = false },
                            containerColor = SurfaceCard,
                            title = {
                                Text("Названия трека", color = OnSurfacePrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            },
                            text = {
                                val aliases = viewModel.currentTrack.aliases
                                if (aliases.isEmpty()) {
                                    Text("Нет названий", color = OnSurfaceSecondary, fontSize = 14.sp)
                                } else {
                                    Column {
                                        aliases.forEachIndexed { index, alias ->
                                            Text(
                                                text = alias.ifEmpty { TrackDocument.UNKNOWN },
                                                color = if (index == 0) Blue60 else OnSurfacePrimary,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showAliasesDialog = false }) {
                                    Text("Закрыть", color = Blue60, fontSize = 14.sp)
                                }
                            }
                        )
                    }
                    val rank = remember(currentTrackId, viewModel.currentListenInSec) {
                        viewModel.allTracks.sortedByDescending { it.listenInSec }
                            .indexOfFirst { it.id == currentTrackId } + 1
                    }
                    val trackYear = viewModel.currentTrack.year
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (rank > 0) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        SurfaceCard.copy(alpha = 0.8f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.trophy),
                                    contentDescription = "Rank",
                                    tint = Blue60,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "$rank/${viewModel.allTracks.size}",
                                    color = OnSurfacePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (trackYear > 0) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        SurfaceCard.copy(alpha = 0.8f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.calendar),
                                    contentDescription = "Year",
                                    tint = Blue60,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "$trackYear",
                                    color = OnSurfacePrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))

                // Border 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onPlaced {
                            border2Y = it.positionInParent().y; border2H = it.size.height.toFloat()
                        }) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = OnSurfaceSecondary,
                                fontSize = 10.sp
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                                colors = SliderDefaults.colors(
                                    thumbColor = Blue60,
                                    activeTrackColor = Blue60,
                                    inactiveTrackColor = DividerColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .padding(horizontal = 4.dp)
                            )
                            Text(
                                text = formatTime(duration),
                                color = OnSurfaceSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))

                        // Border 3
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 4.dp)
                                .onPlaced {
                                    border3Y =
                                        it.positionInParent().y + border2Y + with(density) { 10.dp.toPx() }
                                    border3H = it.size.height.toFloat()
                                }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
//                                    CategoryItem(icon = R.drawable.album, label = "Новое")
                                    CategoryItem(
                                        icon = R.drawable.settings,
                                        label = "Настройки",
                                        onClick = { onMoveTo("settings") },
                                    )
                                    CategoryItem(
                                        icon = R.drawable.search,
                                        label = "Поиск",
                                        onClick = { onMoveTo("search") }
                                    )
                                    CategoryItem(
                                        icon = R.drawable.tracks,
                                        label = "Треки",
                                        onClick = { onMoveTo("tracks") },
                                    )
                                    CategoryItem(
                                        icon = R.drawable.album,
                                        label = "Альбомы",
                                        onClick = { onMoveTo("albums") },
                                    )
                                    CategoryItem(
                                        icon = R.drawable.creators,
                                        label = "Артисты",
                                        onClick = { onMoveTo("creators") },
                                    )
                                    CategoryItem(
                                        icon = R.drawable.favorite_outline,
                                        label = "Избранное",
                                        onClick = {
                                            viewModel.currentPlaylist = viewModel.favorites
                                            onMoveTo("playlist")
                                        },
                                    )
                                    CategoryItem(
                                        icon = R.drawable.data_table,
                                        label = "Плейлисты",
                                        onClick = { onMoveTo("playlists") },
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (coverUri.isNotEmpty()) {
                                        AsyncImage(
                                            model = coverUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        AlbumCover(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            label = name,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                                var showListenTime by remember { mutableIntStateOf(0) }
                                val listenText = when (showListenTime) {
                                    1 -> formatListenTime(currentListen)
                                    2 -> {
                                        val duration = viewModel.currentTrack.durationSec
                                        if (duration > 0) {
                                            val times = currentListen.toDouble() / duration
                                            "x%.2f".format(times)
                                        } else {
                                            "$currentListen"
                                        }
                                    }
                                    else -> "$currentListen"
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                SurfaceCard.copy(alpha = 0.8f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { showListenTime = (showListenTime + 1) % 3 }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.timer),
                                            contentDescription = "Listen time",
                                            tint = Blue60,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            listenText,
                                            color = OnSurfacePrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previousTrack() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.skip_previous),
                                    contentDescription = "Previous",
                                    tint = OnSurfacePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(
                                onClick = { if (isPlaying) viewModel.pause() else viewModel.play() },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    painterResource(if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = OnSurfacePrimary,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.nextTrack() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.skip_next),
                                    contentDescription = "Next",
                                    tint = OnSurfacePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                var editTime by remember { mutableStateOf(false) }
                var showPlaylistDialog by remember { mutableStateOf(false) }

                AnimatedContent(
                    targetState = editTime,
                    modifier = Modifier.onPlaced { border1Y = 0f },
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)))
                                .togetherWith(slideOutHorizontally(tween(300)) { it } + fadeOut(
                                    tween(300)
                                ))
                        } else {
                            (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)))
                                .togetherWith(slideOutHorizontally(tween(300)) { -it } + fadeOut(
                                    tween(300)
                                ))
                        }
                    }
                ) { isEdit ->
                    if (isEdit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.size(48.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(-10, -1, 0, 1, 10).forEach { multi ->
                                    val label = when (multi) {
                                        -10 -> "-10"; -1 -> "-1"; 0 -> "0"; 1 -> "+1"; else -> "+10"
                                    }
                                    IconButton(onClick = { viewModel.adjustListenInSec(multi) }) {
                                        Text(
                                            label,
                                            color = OnSurfaceSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { editTime = false }) {
                                Icon(
                                    painterResource(R.drawable.close),
                                    contentDescription = "Back",
                                    tint = OnSurfaceSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (isShuffle) viewModel.disableShuffle() else viewModel.enableShuffle() }) {
                                Icon(
                                    painterResource(R.drawable.shuffle),
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffle) Blue60 else OnSurfaceSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { showPlaylistDialog = true }) {
                                Icon(
                                    painterResource(R.drawable.add_link),
                                    contentDescription = "Add to playlist",
                                    tint = OnSurfaceSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { if (isRepeat) viewModel.disableRepeat() else viewModel.enableRepeat() }) {
                                Icon(
                                    painterResource(R.drawable.repeat_one),
                                    contentDescription = "Repeat",
                                    tint = if (isRepeat) Blue60 else OnSurfaceSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { editTime = !editTime }) {
                                Icon(
                                    painterResource(R.drawable.more_time),
                                    contentDescription = "Edit Time",
                                    tint = if (editTime) Blue60 else OnSurfaceSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.changeTrackFavoriteState(viewModel.currentTrack) }) {
                                Icon(
                                    painterResource(if (isFavorite) R.drawable.favorite_filled else R.drawable.favorite_outline),
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Blue60 else OnSurfaceSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                if (showPlaylistDialog) {
                    AddToPlaylistDialog(
                        track = viewModel.currentTrack,
                        allPlaylists = viewModel.allPlaylists,
                        onDismiss = { showPlaylistDialog = false },
                        onToggle = { playlist, add ->
                            val list = playlist.tracklist.toMutableList()
                            if (add) list.add(viewModel.currentTrack) else list.removeIf { it.id == viewModel.currentTrack.id }
                            viewModel.savePlaylist(playlist.copy(tracklist = list.toImmutableList()))
                        })
                }
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .onPlaced { tabsY = it.positionInParent().y },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomTabItem(
                    icon = R.drawable.queue,
                    label = "Очередь",
                    onClick = { onMoveTo("queue") })
                BottomTabItem(
                    icon = R.drawable.album,
                    label = "Альбом",
                    onClick = {
                        viewModel.currentAlbum = viewModel.allAlbums.find {
                            it.fileName == viewModel.currentTrack.album
                        } ?: AlbumDocument.createEmpty()
                        onMoveTo("album")
                    },
                )
                BottomTabItem(
                    icon = R.drawable.creators,
                    label = "Артист",
                    onClick = { onMoveTo("trackCreators") })
                BottomTabItem(
                    icon = R.drawable.link,
                    label = "Связанное",
                    onClick = { onMoveTo("related") })
            }
        }
    }
}

@Composable
private fun BottomTabItem(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = OnSurfaceSecondary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = OnSurfaceSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun CategoryItem(icon: Int, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = OnSurfaceSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = OnSurfaceSecondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun rememberCoverUri(viewModel: MusicPlayerViewModel): String {
    val trackId = viewModel.currentTrack.id
    val cover = viewModel.currentTrack.cover
    return androidx.compose.runtime.remember(trackId, cover) { viewModel.getCoverUri(coverString = cover) }
}

// --- Queue screen ---
@Composable
fun QueueTracksScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit = {},
    onTrackSelected: (TrackDocument) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val queue = if (viewModel.isShuffle) viewModel.randomQueue else viewModel.currentQueue
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
                "Очередь (${queue.size})",
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(
                    items = queue,
                    key = { it.id }) { track ->
                    val latestListenInSec = viewModel.allTracks.find { it.id == track.id }?.listenInSec ?: track.listenInSec
                    TrackListItem(
                        track = track,
                        isActive = track.id == viewModel.currentTrack.id,
                        coverUri = viewModel.getCoverUri(coverString = track.cover),
                        listenInSec = latestListenInSec,
                        allPlaylists = viewModel.allPlaylists,
                        onClick = { onTrackSelected(track) },
                        onAddToPlaylist = { playlist, add -> viewModel.toggleTrackInPlaylist(track, playlist, add) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onPlayNext = { viewModel.playNext(track) })
                }
            }
            BottomScrollControls(listState, viewModel, queue)
        }
        BottomPlayerMini(viewModel)
    }
}

@Composable
fun TrackCreatorsScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit = {},
    onCreatorSelected: (CreatorDocument) -> Unit = {},
) {
    val currentCreators = viewModel.currentTrack.creators
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
                "Артисты",
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = currentCreators, key = { it.id }) { creator ->
                val creatorName = creator.aliases.getOrElse(0) { CreatorDocument.UNKNOWN }
                val trackCount =
                    viewModel.allTracks.count { track -> track.creators.any { it.id == creator.id } }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCreatorSelected(creator) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumCover(
                        modifier = Modifier.size(48.dp),
                        label = creatorName,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            creatorName,
                            color = OnSurfacePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ); Text("$trackCount треков", color = OnSurfaceSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
        BottomPlayerMini(viewModel)
    }
}

// --- All tracks screen ---
@Composable
fun AllTracksScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit = {},
    onTrackSelected: (TrackDocument) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val sortMode = viewModel.tracksSortMode
    val sortAscending = viewModel.tracksSortAscending
    val sortedTracks = viewModel.sortedAllTracks
    var showSortMenu by remember { mutableStateOf(false) }
    val sortLabels = listOf(
        "По имени",
        "По дате",
        "По прослушиваниям",
        "По годам",
    )

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
                "Треки (${viewModel.allTracks.size})",
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        painterResource(R.drawable.sort),
                        contentDescription = "Sort",
                        tint = OnSurfaceSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    sortLabels.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    label,
                                    color = if (sortMode == index) Blue60 else OnSurfacePrimary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                viewModel.tracksSortMode = index
                            }
                        )
                    }
                    HorizontalDivider(color = DividerColor)
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "По возрастанию",
                                    color = OnSurfacePrimary,
                                    fontSize = 14.sp
                                )
                                Checkbox(
                                    checked = sortAscending,
                                    onCheckedChange = { viewModel.tracksSortAscending = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Blue60)
                                )
                            }
                        },
                        onClick = { viewModel.tracksSortAscending = !sortAscending }
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            val showYearHeaders = sortMode == 3
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                if (showYearHeaders) {
                    var lastYear = ""
                    sortedTracks.forEach { track ->
                        val year = if (track.year > 0) "${track.year}" else "Без года"
                        if (year != lastYear) {
                            stickyHeader(key = "header_$year") {
                                Text(
                                    text = year,
                                    color = Blue60,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceDark)
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                            lastYear = year
                        }
                        item(key = track.id) {
                            TrackListItem(
                                track = track,
                                isActive = track.id == viewModel.currentTrack.id,
                                coverUri = viewModel.getCoverUri(coverString = track.cover),
                                allPlaylists = viewModel.allPlaylists,
                                onClick = { onTrackSelected(track) },
                                onAddToPlaylist = { playlist, add -> viewModel.toggleTrackInPlaylist(track, playlist, add) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onPlayNext = { viewModel.playNext(track) })
                        }
                    }
                } else {
                    items(
                        items = sortedTracks,
                        key = { it.id }
                    ) { track ->
                        TrackListItem(
                            track = track,
                            isActive = track.id == viewModel.currentTrack.id,
                            coverUri = viewModel.getCoverUri(coverString = track.cover),
                            allPlaylists = viewModel.allPlaylists,
                            onClick = { onTrackSelected(track) },
                            onAddToPlaylist = { playlist, add -> viewModel.toggleTrackInPlaylist(track, playlist, add) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onPlayNext = { viewModel.playNext(track) })
                    }
                }
            }
            BottomScrollControls(listState, viewModel, sortedTracks)
        }
        BottomPlayerMini(viewModel)
    }
}

// --- All creators screen ---
@Composable
fun AllCreatorsScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit = {},
    onCreatorSelected: (CreatorDocument) -> Unit = {},
) {
    val listState = rememberLazyListState()
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
                "Артисты (${viewModel.allCreators.size})",
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            items(items = viewModel.allCreators, key = { it.id }) { creator ->
                val creatorName = creator.aliases.getOrElse(0) { CreatorDocument.UNKNOWN }
                val trackCount =
                    viewModel.allTracks.count { track -> track.creators.any { it.id == creator.id } }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCreatorSelected(creator) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumCover(
                        modifier = Modifier.size(48.dp),
                        label = creatorName,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            creatorName,
                            color = OnSurfacePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ); Text("$trackCount треков", color = OnSurfaceSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
        BottomPlayerMini(viewModel)
    }
}

// --- Creator tracks screen ---
@Composable
fun CreatorTracksScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit = {},
    onTrackSelected: (TrackDocument) -> Unit = {}
) {
    val creator = viewModel.currentCreator
    val creatorName = creator.aliases.getOrElse(0) { CreatorDocument.UNKNOWN }
    val tracks = viewModel.getCreatorTracks(creator)
    val listState = rememberLazyListState()

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
                creatorName,
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(items = tracks, key = { it.id }) { track ->
                    TrackListItem(
                        track = track,
                        isActive = track.id == viewModel.currentTrack.id,
                        coverUri = viewModel.getCoverUri(coverString = track.cover),
                        allPlaylists = viewModel.allPlaylists,
                        onClick = {
                            viewModel.setQueueFromSource(tracks, track)
                            viewModel.setMediaSourceWithService(track)
                            onTrackSelected(track)
                        },
                        onAddToPlaylist = { playlist, add -> viewModel.toggleTrackInPlaylist(track, playlist, add) }
                    )
                }
            }
            BottomScrollControls(listState, viewModel, tracks)
        }
        BottomPlayerMini(viewModel)
    }
}

// --- Related tracks screen ---
@Composable
fun RelatedTracksScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit = {},
    onTrackSelected: (TrackDocument) -> Unit = {}
) {
    val track = viewModel.currentTrack
    val relatedTracks = viewModel.getRelatedTracks(track)
    val covers = viewModel.getCoverOfTracks(track)
    val coverOfTrack = if (track.coverOf.isNotEmpty()) {
        val baseName = track.coverOf.removeSurrounding("[[", "]]")
        viewModel.allTracks.find { it.fileName == baseName }
    } else null
    val listState = rememberLazyListState()

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
                "Связанное",
                color = OnSurfacePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                if (relatedTracks.isNotEmpty()) {
                    stickyHeader(key = "header_related") {
                        Text(
                            text = "Похожее (${relatedTracks.size})",
                            color = Blue60,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(items = relatedTracks, key = { "related_${it.id}" }) { relatedTrack ->
                        TrackListItem(
                            track = relatedTrack,
                            isActive = relatedTrack.id == viewModel.currentTrack.id,
                            coverUri = viewModel.getCoverUri(coverString = relatedTrack.cover),
                            allPlaylists = viewModel.allPlaylists,
                            onClick = {
                                viewModel.setQueueFromSource(relatedTracks, relatedTrack)
                                viewModel.setMediaSourceWithService(relatedTrack)
                                onTrackSelected(relatedTrack)
                            },
                            onAddToPlaylist = { playlist, add -> viewModel.toggleTrackInPlaylist(relatedTrack, playlist, add) },
                            onAddToQueue = { viewModel.addToQueue(relatedTrack) },
                            onPlayNext = { viewModel.playNext(relatedTrack) }
                        )
                    }
                }
                if (coverOfTrack != null) {
                    stickyHeader(key = "header_coverof") {
                        Text(
                            text = "Кавер на",
                            color = Blue60,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    item(key = "coverof_${coverOfTrack.id}") {
                        TrackListItem(
                            track = coverOfTrack,
                            isActive = coverOfTrack.id == viewModel.currentTrack.id,
                            coverUri = viewModel.getCoverUri(coverString = coverOfTrack.cover),
                            allPlaylists = viewModel.allPlaylists,
                            onClick = {
                                viewModel.setMediaSourceWithService(coverOfTrack)
                                onTrackSelected(coverOfTrack)
                            },
                            onAddToPlaylist = { playlist, add -> viewModel.toggleTrackInPlaylist(coverOfTrack, playlist, add) },
                            onAddToQueue = { viewModel.addToQueue(coverOfTrack) },
                            onPlayNext = { viewModel.playNext(coverOfTrack) }
                        )
                    }
                }
                if (covers.isNotEmpty()) {
                    stickyHeader(key = "header_covers") {
                        Text(
                            text = "Кавера (${covers.size})",
                            color = Blue60,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(items = covers, key = { "cover_${it.id}" }) { coverOfTrack ->
                        TrackListItem(
                            track = coverOfTrack,
                            isActive = coverOfTrack.id == viewModel.currentTrack.id,
                            coverUri = viewModel.getCoverUri(coverString = coverOfTrack.cover),
                            allPlaylists = viewModel.allPlaylists,
                            onClick = {
                                viewModel.setQueueFromSource(covers, coverOfTrack)
                                viewModel.setMediaSourceWithService(coverOfTrack)
                                onTrackSelected(coverOfTrack)
                            },
                            onAddToPlaylist = { playlist, add -> viewModel.toggleTrackInPlaylist(coverOfTrack, playlist, add) },
                            onAddToQueue = { viewModel.addToQueue(coverOfTrack) },
                            onPlayNext = { viewModel.playNext(coverOfTrack) }
                        )
                    }
                }
                if (relatedTracks.isEmpty() && coverOfTrack == null && covers.isEmpty()) {
                    item {
                        Text(
                            text = "Нет связанных треков",
                            color = OnSurfaceSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            BottomScrollControls(listState, viewModel, relatedTracks + listOfNotNull(coverOfTrack) + covers)
        }
        BottomPlayerMini(viewModel)
    }
}

// --- Mini player bar ---
@Composable
fun BottomPlayerMini(viewModel: MusicPlayerViewModel) {
    val isPlaying = viewModel.isPlaying
    val name = viewModel.currentTrack.aliases.getOrElse(0) { "" }
    val artists =
        viewModel.currentTrack.creators.joinToString(", ") { it.aliases.getOrElse(0) { "" } }
    val coverUri = viewModel.getCoverUri(coverString = viewModel.currentTrack.cover)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(SurfaceCard)
            .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumCover(modifier = Modifier.size(48.dp), label = name, coverUri = coverUri)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name.ifEmpty { "No track" },
                color = OnSurfacePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            ); Text(
            text = artists.ifEmpty { "Unknown" },
            color = OnSurfaceSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
        }
        IconButton(onClick = { viewModel.previousTrack() }) {
            Icon(
                painterResource(R.drawable.skip_previous),
                contentDescription = "Previous",
                tint = OnSurfacePrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(onClick = { if (isPlaying) viewModel.pause() else viewModel.play() }) {
            Icon(
                painterResource(if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = OnSurfacePrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        IconButton(onClick = { viewModel.nextTrack() }) {
            Icon(
                painterResource(R.drawable.skip_next),
                contentDescription = "Next",
                tint = OnSurfacePrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BottomScrollControls(
    listState: androidx.compose.foundation.lazy.LazyListState,
    viewModel: MusicPlayerViewModel,
    trackList: List<TrackDocument>
) {
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Column(modifier = Modifier.padding(bottom = 75.dp, end = 5.dp)) {
            IconButton(
                onClick = { coroutineScope.launch { listState.scrollToItem(0) } },
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .background(color = SurfaceCard, shape = RoundedCornerShape(50))
            ) {
                Icon(
                    painterResource(R.drawable.double_arrow_up),
                    contentDescription = "Scroll to top",
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = {
                    val trackIndex =
                        trackList.indexOfFirst { it.id == viewModel.currentTrack.id }; if (trackIndex >= 0) coroutineScope.launch {
                    listState.scrollToItem(
                        trackIndex
                    )
                }
                },
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .background(color = SurfaceCard, shape = RoundedCornerShape(50))
            ) {
                Icon(
                    painterResource(R.drawable.arrows_input),
                    contentDescription = "Scroll to current",
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = { coroutineScope.launch { listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1) } },
                modifier = Modifier.background(color = SurfaceCard, shape = RoundedCornerShape(50))
            ) {
                Icon(
                    painterResource(R.drawable.double_arrow_down),
                    contentDescription = "Scroll to bottom",
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MusicPlayerScreenPreview() {
    MusicPlayerScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = MusicPlayerViewModel(LocalContext.current)
    )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QueueTracksScreenPreview() {
    QueueTracksScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = MusicPlayerViewModel(LocalContext.current)
    )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TrackCreatorsScreenPreview() {
    TrackCreatorsScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = MusicPlayerViewModel(LocalContext.current)
    )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AllTracksScreenPreview() {
    AllTracksScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = MusicPlayerViewModel(LocalContext.current)
    )
}

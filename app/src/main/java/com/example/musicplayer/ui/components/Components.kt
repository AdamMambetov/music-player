package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.musicplayer.R
import com.example.musicplayer.data.CreatorDocument
import com.example.musicplayer.data.TrackDocument
import com.example.musicplayer.ui.theme.AccentRed
import com.example.musicplayer.ui.theme.Blue60
import com.example.musicplayer.ui.theme.DividerColor
import com.example.musicplayer.ui.theme.OnSurfacePrimary
import com.example.musicplayer.ui.theme.OnSurfaceSecondary
import com.example.musicplayer.ui.theme.SurfaceCard

@Composable
fun AlbumCover(
    modifier: Modifier = Modifier,
    label: String = "",
    coverUri: String = "",
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
) {
    if (coverUri.isNotEmpty()) {
        AsyncImage(
            model = coverUri,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        androidx.compose.foundation.Image(
            painter = painterResource(R.mipmap.no_album_art),
            contentDescription = label,
            modifier = modifier.clip(shape)
        )
    }
}

@Composable
fun TrackListItem(
    track: TrackDocument,
    isActive: Boolean = false,
    coverUri: String = "",
    listenInSec: Int = track.listenInSec,
    onClick: () -> Unit,
    onMenuClick: () -> Unit = {},
) {
    val name =
        track.aliases.getOrElse(0) { TrackDocument.UNKNOWN }.ifEmpty { TrackDocument.UNKNOWN }
    val artists = track.creators
        .map { it.aliases.getOrElse(0) { CreatorDocument.UNKNOWN } }
        .ifEmpty { listOf(CreatorDocument.UNKNOWN) }
        .joinToString(", ")
    val listenText = formatListenTime(listenInSec).ifEmpty { null }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) SurfaceCard else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (coverUri.isNotEmpty()) {
            AsyncImage(
                model = coverUri,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            AlbumCover(
                modifier = Modifier.size(48.dp),
                label = name,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = if (isActive) Blue60 else OnSurfacePrimary,
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artists,
                color = OnSurfaceSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (listenText != null) {
                Text(
                    text = listenText,
                    color = OnSurfaceSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    lineHeight = 0.5.sp,
                )
            }
        }
        IconButton(onClick = onMenuClick) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Menu",
                tint = OnSurfaceSecondary
            )
        }
    }
}

@Composable
fun PlayerProgressBar(
    progressMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    val fraction = if (durationMs > 0) progressMs.toFloat() / durationMs else 0f
    Column {
        Slider(
            value = fraction,
            onValueChange = { onSeek((it * durationMs).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = AccentRed,
                activeTrackColor = AccentRed,
                inactiveTrackColor = DividerColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(progressMs), color = OnSurfaceSecondary, fontSize = 12.sp)
            Text(formatTime(durationMs), color = OnSurfaceSecondary, fontSize = 12.sp)
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatListenTime(seconds: Int): String {
    if (seconds <= 0) return ""
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return buildString {
        if (days > 0) append("${days}д ")
        if (hours > 0) append("${hours}ч ")
        if (minutes > 0) append("${minutes}м ")
        if (secs > 0 || isEmpty()) append("${secs}с")
    }.trim()
}

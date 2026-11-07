package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyAction
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.record.SendToToolboxDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.record.ToolboxTargets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The kinds the library filter offers. zh-CN: 媒体库筛选提供的类型. */
private enum class Filter(@param:StringRes val label: Int, val kind: MediaKind?) {
    ALL(R.string.library_filter_all, null),
    VIDEO(R.string.library_filter_video, MediaKind.VIDEO),
    AUDIO(R.string.library_filter_audio, MediaKind.AUDIO),
    IMAGE(R.string.library_filter_image, MediaKind.IMAGE),
}

/**
 * The library tab (roadmap D37): the media of the library folders, newest first, with a kind filter; without storage
 * access it explains why and asks for it. Each video or audio row can be sent to a toolbox wizard ([onProcess], roadmap
 * P4.5).
 * zh-CN: 媒体库标签 (路线图 D37): 媒体库目录中的媒体, 最新在前, 可按类型筛选; 没有存储权限时说明原因并申请. 每个视频或音频行都可以
 * 送进工具箱向导 ([onProcess], 路线图 P4.5).
 */
@Composable
internal fun LibraryScreen(
    storageGranted: Boolean,
    refreshKey: Int,
    onGrantStorage: () -> Unit,
    onOpen: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onProcess: (op: String, path: String) -> Unit = { _, _ -> },
) {
    if (!storageGranted) {
        EmptyState(
            title = stringResource(R.string.home_storage_title),
            body = stringResource(R.string.library_storage_body),
            icon = R.drawable.ic_storage_24,
            primary = EmptyAction(stringResource(R.string.home_storage_grant), onGrantStorage, "slate.library.grant"),
        )
        return
    }
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<MediaEntry>?>(null) }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var sending by remember { mutableStateOf<MediaEntry?>(null) }
    LaunchedEffect(refreshKey) {
        entries = withContext(Dispatchers.IO) { MediaLibrary.query(context, MediaLibrary.folders(context)) }
    }
    Column(Modifier.fillMaxSize().testTag("slate.library")) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Filter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(stringResource(item.label)) },
                    modifier = Modifier.testTag("slate.library.filter.${item.name.lowercase()}"),
                )
            }
        }
        val all = entries
        when {
            all == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> {
                val shown = all.filter { filter.kind == null || it.kind == filter.kind }
                if (shown.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.library_empty_title),
                        body = stringResource(R.string.library_empty_body, MediaLibrary.folders(context).joinToString("\n")),
                        icon = R.drawable.ic_folder_24,
                        secondary = listOf(EmptyAction(stringResource(R.string.library_manage_folders), onOpenSettings, "slate.library.folders")),
                    )
                } else {
                    sending?.let { entry ->
                        SendToToolboxDialog(entry.path, entry.kind, onDismiss = { sending = null }, onChoose = { op -> sending = null; onProcess(op, entry.path) })
                    }
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(shown, key = { it.id }) { entry ->
                            MediaRow(entry, onClick = { onOpen(entry.path) }, onSend = { sending = entry })
                            Hairline(insetStart = 88.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(entry: MediaEntry, onClick: () -> Unit, onSend: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp).testTag("slate.library.item"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(entry, Modifier.size(56.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            val details = buildList {
                entry.durationMillis?.let { add(Formats.duration(it)) }
                if (entry.width != null && entry.height != null) add(Formats.resolution(entry.width, entry.height))
                add(Formats.size(context, entry.sizeBytes))
            }.joinToString("  ")
            Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                entry.folder,
                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (ToolboxTargets.operations(entry.kind).isNotEmpty()) {
            IconButton(onClick = onSend, modifier = Modifier.testTag("slate.library.send")) {
                Icon(painterResource(R.drawable.ic_build_24), contentDescription = stringResource(R.string.record_send_to_toolbox),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A 56 dp thumbnail, or the kind icon while loading and for audio. zh-CN: 56 dp 缩略图; 加载中与音频显示类型图标. */
@Composable
internal fun Thumbnail(entry: MediaEntry, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(entry.id, entry.modifiedSeconds) { mutableStateOf<Bitmap?>(Thumbnails.cached(entry)) }
    LaunchedEffect(entry.id, entry.modifiedSeconds) {
        if (bitmap == null) Thumbnails.load(context, entry) { bitmap = it }
    }
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(image.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(painterResource(kindIcon(entry.kind)), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@DrawableRes
internal fun kindIcon(kind: MediaKind): Int = when (kind) {
    MediaKind.VIDEO -> R.drawable.ic_movie_24
    MediaKind.AUDIO -> R.drawable.ic_music_note_24
    MediaKind.IMAGE -> R.drawable.ic_image_24
    MediaKind.OTHER -> R.drawable.ic_file_24
}

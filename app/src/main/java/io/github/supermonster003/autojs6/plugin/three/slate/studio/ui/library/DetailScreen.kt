package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.InputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionSchemas
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.MediaActions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.Labels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** What the detail page read about a file. zh-CN: 详情页读取到的文件信息. */
private data class FileDetails(
    val file: File,
    val kind: MediaKind,
    val info: MediaInfo?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val preview: MediaEntry?,
)

/**
 * The media detail page (roadmap P4.1, D37, D18): the probe of the file, open / share, rename / delete inside the library
 * folders, "view with MediaInfo" when MediaInfo can show the file (appendix D Q52), and the toolbox operations that
 * take the file as their input.
 * zh-CN: 媒体详情页 (路线图 P4.1, D37, D18): 文件的探测信息, 打开 / 分享, 媒体库目录内的重命名 / 删除, MediaInfo 能显示该文件时的
 * "用 MediaInfo 查看" (附录 D Q52), 以及以该文件为输入的工具箱操作.
 */
@Composable
internal fun DetailScreen(path: String, onBack: () -> Unit, onProcess: (op: String, path: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val renameFailed = stringResource(R.string.detail_rename_failed)
    val actionFailed = stringResource(R.string.action_failed)
    var current by rememberSaveable { mutableStateOf(path) }
    var revision by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf<FileDetails?>(null) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(current, revision) {
        details = withContext(Dispatchers.IO) { read(context, current) }
    }
    val inLibrary = remember(current) { MediaLibrary.folderOf(context, current) != null }
    val mediaInfo = remember(current) { mediaInfoIntent(context, current) }
    val failed = stringResource(R.string.action_failed)
    SlateScreen(
        title = File(current).name,
        singleLineTitle = true,
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            TopBarAction(R.drawable.ic_open_in_new_24, stringResource(R.string.action_open_with), "slate.detail.open") {
                if (!MediaActions.open(context, current)) scope.launch { snackbar.showSnackbar(failed) }
            }
            TopBarAction(R.drawable.ic_share_24, stringResource(R.string.action_share), "slate.detail.share") {
                if (!MediaActions.share(context, listOf(current))) scope.launch { snackbar.showSnackbar(failed) }
            }
            if (inLibrary) {
                TopBarAction(R.drawable.ic_edit_24, stringResource(R.string.detail_rename), "slate.detail.rename") { dialog = DIALOG_RENAME }
                TopBarAction(R.drawable.ic_delete_24, stringResource(R.string.detail_delete), "slate.detail.delete") { dialog = DIALOG_DELETE }
            }
        },
    ) { padding ->
        val loaded = details
        if (loaded == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@SlateScreen
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("slate.detail")) {
            val entry = loaded.preview
            if (entry != null && loaded.kind != MediaKind.AUDIO) {
                Thumbnail(entry, Modifier.fillMaxWidth().padding(horizontal = 16.dp).aspectRatio(16f / 9f))
            }
            if (!loaded.file.exists()) {
                Text(stringResource(R.string.detail_missing), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }
            SectionHeader(stringResource(R.string.detail_info))
            InfoRows(context, loaded)
            if (mediaInfo != null) {
                Hairline()
                SettingsRow(stringResource(R.string.detail_mediainfo), summary = stringResource(R.string.detail_mediainfo_summary), icon = R.drawable.ic_info_24,
                    onClick = { runCatching { context.startActivity(mediaInfo) } }, modifier = Modifier.testTag("slate.detail.mediainfo"))
            }
            val operations = remember(loaded.kind) {
                OptionSchemas.ALL.filter { schema ->
                    when (schema.inputs.kind) {
                        InputKind.VIDEO -> loaded.kind == MediaKind.VIDEO
                        InputKind.AUDIO -> loaded.kind == MediaKind.AUDIO
                        InputKind.MEDIA -> loaded.kind == MediaKind.VIDEO || loaded.kind == MediaKind.AUDIO
                        InputKind.SUBTITLE -> loaded.kind == MediaKind.OTHER && isSubtitle(current)
                    }
                }
            }
            if (operations.isNotEmpty()) {
                SectionHeader(stringResource(R.string.detail_process))
                operations.forEachIndexed { index, schema ->
                    if (index > 0) Hairline()
                    val labels = Labels.operations.getValue(schema.op)
                    SettingsRow(stringResource(labels.title), summary = stringResource(labels.summary), icon = labels.icon,
                        onClick = { onProcess(schema.op, current) }, modifier = Modifier.testTag("slate.detail.op.${schema.op}"))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    when (dialog) {
        DIALOG_RENAME -> RenameDialog(
            name = File(current).name,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                scope.launch {
                    val renamed = withContext(Dispatchers.IO) { MediaLibrary.rename(context, current, name) }
                    if (renamed == null) snackbar.showSnackbar(renameFailed) else { current = renamed; revision++ }
                }
            },
        )
        DIALOG_DELETE -> MessageDialog(
            title = stringResource(R.string.detail_delete),
            message = stringResource(R.string.detail_delete_message, File(current).name),
            onDismissRequest = { dialog = null },
            onConfirm = {
                dialog = null
                scope.launch {
                    val deleted = withContext(Dispatchers.IO) { MediaLibrary.delete(context, current) }
                    if (deleted) onBack() else snackbar.showSnackbar(actionFailed)
                }
            },
            confirmText = stringResource(R.string.detail_delete),
            tag = "slate.dialog.delete",
        )
    }
}

@Composable
private fun InfoRows(context: Context, details: FileDetails) {
    val rows = buildList {
        add(R.string.detail_path to Formats.isolate(details.file.path))
        add(R.string.detail_size to Formats.size(context, details.file.length()))
        // The plugin's language, not the default locale of the process. zh-CN: 用插件的语言, 不是进程的默认区域设置.
        val locale = context.resources.configuration.locales[0]
        add(R.string.detail_modified to DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(details.file.lastModified())))
        val info = details.info
        if (info != null) {
            info.mime?.let { add(R.string.detail_format to it) }
            if (info.durationMillis > 0) add(R.string.option_duration to Formats.duration(info.durationMillis))
            if (info.width != null && info.height != null) add(R.string.detail_resolution to Formats.resolution(info.width, info.height))
            if (info.rotation != 0) add(R.string.option_rotate to Formats.degrees(info.rotation))
            info.frameRate?.let { add(R.string.option_frame_rate to Formats.frameRate(it)) }
            info.videoCodec?.let { add(R.string.detail_video_codec to it) }
            info.bitRate?.let { add(R.string.option_bit_rate to Formats.bitRate(it)) }
            info.audioCodec?.let { add(R.string.detail_audio_codec to it) }
            info.sampleRate?.let { add(R.string.option_sample_rate to Formats.sampleRate(it)) }
            info.channels?.let { add(R.string.option_channels to it.toString()) }
        } else if (details.imageWidth != null && details.imageHeight != null) {
            add(R.string.detail_resolution to Formats.resolution(details.imageWidth, details.imageHeight))
        }
    }
    rows.forEachIndexed { index, (label, value) ->
        if (index > 0) Hairline(insetStart = 24.dp)
        SettingsRow(stringResource(label), summary = value, showChevron = false, modifier = Modifier.testTag("slate.detail.info.$index"))
    }
}

@Composable
private fun RenameDialog(name: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(name) }
    val valid = text.isNotBlank() && '/' !in text && text != name
    SlateDialog(
        title = stringResource(R.string.detail_rename),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(text.trim()) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = valid,
        tag = "slate.dialog.rename",
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(horizontal = DialogDefaults.contentPadding).testTag("slate.dialog.rename.field"),
        )
        Spacer(Modifier.height(8.dp))
    }
}

private fun read(context: Context, path: String): FileDetails {
    val file = File(path)
    val kind = kindOf(path)
    val info = if (kind == MediaKind.VIDEO || kind == MediaKind.AUDIO) runCatching { MediaProbe.probe(path) }.getOrNull() else null
    var width: Int? = null
    var height: Int? = null
    if (kind == MediaKind.IMAGE) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        width = bounds.outWidth.takeIf { it > 0 }
        height = bounds.outHeight.takeIf { it > 0 }
    }
    val preview = if (kind == MediaKind.VIDEO || kind == MediaKind.IMAGE) previewEntry(context, file, kind) else null
    return FileDetails(file, kind, info, width, height, preview)
}

/** A library entry for the preview thumbnail (MediaStore id), or null outside MediaStore. zh-CN: 用于预览缩略图的条目. */
private fun previewEntry(context: Context, file: File, kind: MediaKind): MediaEntry? {
    if (!file.exists()) return null
    val uri = MediaLibrary.contentUri(context, file.path) ?: return null
    val id = runCatching { uri.lastPathSegment?.toLong() }.getOrNull() ?: return null
    return MediaEntry(id, uri, file.path, file.name, kind, file.length(), null, null, null, file.lastModified() / 1000, file.parent.orEmpty(), null)
}

internal fun kindOf(path: String): MediaKind = MediaActions.mimeOf(path).let { mime ->
    when {
        mime.startsWith("video/") -> MediaKind.VIDEO
        mime.startsWith("audio/") -> MediaKind.AUDIO
        mime.startsWith("image/") -> MediaKind.IMAGE
        else -> MediaKind.OTHER
    }
}

internal fun isSubtitle(path: String): Boolean = path.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("srt", "ass", "ssa")

/**
 * An Intent that MediaInfo answers for this file (roadmap D18: an Intent, no binding), or null. MediaInfo 2.1.4 has no
 * viewer Activity, so the entry stays hidden until it gets one (appendix D Q52).
 * zh-CN: MediaInfo 能响应的该文件的 Intent (路线图 D18: 用 Intent, 不绑定), 否则为 null. MediaInfo 2.1.4 没有查看器 Activity,
 * 在它提供之前入口不显示 (附录 D Q52).
 */
private fun mediaInfoIntent(context: Context, path: String): Intent? {
    val uri = MediaActions.uriOf(context, path) ?: return null
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, MediaActions.mimeOf(path)).setPackage(MEDIAINFO_PACKAGE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return intent.takeIf { context.packageManager.resolveActivity(it, 0) != null }
}

internal const val MEDIAINFO_PACKAGE = "io.github.supermonster003.autojs6.plugin.mediainfo"
private const val DIALOG_RENAME = "rename"
private const val DIALOG_DELETE = "delete"

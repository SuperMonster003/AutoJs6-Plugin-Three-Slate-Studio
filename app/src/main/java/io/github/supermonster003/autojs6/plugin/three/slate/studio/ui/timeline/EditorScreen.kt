package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.preview.TimelinePreview
import io.github.supermonster003.autojs6.plugin.three.slate.studio.preview.TimelinePreviewFrame
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditOps
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaEntry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaLibrary
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.Thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

private enum class Picker { VIDEO, AUDIO, IMAGE, SUBTITLES }

/**
 * The timeline editor (roadmap P4.3, D10): the preview with its transport, the multi-track timeline and the inspector,
 * side by side from [WIDE_DP] wide. Projects are saved as `.slate.json`; the export submits `compose` to the task
 * queue and [onExported] opens its result page.
 * zh-CN: 时间线剪辑器 (路线图 P4.3, D10): 预览及播放控制, 多轨时间线与检查器, 宽度达到 [WIDE_DP] 时左右并排. 工程保存为 `.slate.json`;
 * 导出把 `compose` 提交到任务队列, [onExported] 打开其结果页.
 */
@Composable
internal fun EditorScreen(model: EditorModel, openProjects: Boolean, onFinish: () -> Unit, onExported: (String) -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val project = model.project
    var picker by remember { mutableStateOf<Picker?>(null) }
    var textTarget by remember { mutableStateOf<Long?>(null) }
    var showProjects by rememberSaveable { mutableStateOf(openProjects) }
    var leaving by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    var importPath by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    model.message?.let { message ->
        val text = stringResource(message.text)
        LaunchedEffect(message) {
            model.message = null
            snackbar.showSnackbar(listOfNotNull(text, message.detail).joinToString(": "))
        }
    }

    val systemPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val kind = picker
        picker = null
        if (uri == null || kind == null) return@rememberLauncherForActivityResult
        val path = MediaLibrary.pathOf(context, uri)
        if (path == null) {
            model.message = EditorModel.Message(R.string.timeline_message_unreadable)
            return@rememberLauncherForActivityResult
        }
        when (kind) {
            Picker.VIDEO -> model.add(listOf(path), EditorModel.AddKind.VIDEO)
            Picker.AUDIO -> model.add(listOf(path), EditorModel.AddKind.AUDIO)
            Picker.IMAGE -> model.add(listOf(path), EditorModel.AddKind.IMAGE)
            Picker.SUBTITLES -> if (model.project.subtitles.cues.isEmpty()) model.importSubtitles(path, append = false) else importPath = path
        }
    }
    val projectPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull() }
            if (text == null) model.message = EditorModel.Message(R.string.timeline_message_open_failed) else model.open(null, text)
        }
    }
    val srtWriter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri: Uri? ->
        if (uri != null) model.writeSrt(uri)
    }

    val back = { if (model.dirty && !model.project.isEmpty) leaving = true else onFinish() }
    BackHandler(onBack = back)
    val textDefault = stringResource(R.string.choice_overlay_text)

    val actions = InspectorActions(
        addVideo = { picker = Picker.VIDEO },
        addAudio = { picker = Picker.AUDIO },
        addImage = { picker = Picker.IMAGE },
        addText = {
            var created = -1L
            model.edit { EditOps.addText(it, model.playheadMs, textDefault).also { (_, id) -> created = id }.first }
            model.selected = created
            textTarget = created
        },
        addCue = {
            var created = -1L
            model.edit { EditOps.addCue(it, model.playheadMs, textDefault).also { (_, id) -> created = id }.first }
            model.selected = created
            textTarget = created
        },
        importSubtitles = { picker = Picker.SUBTITLES },
        exportSrt = { srtWriter.launch((model.name ?: "subtitles") + ".srt") },
        editText = { id -> textTarget = id },
        split = {
            val id = model.selected
            val at = model.playheadMs
            var result: Long? = null
            if (id != null) model.edit { p ->
                val split = EditOps.splitVideo(p, id, at) ?: EditOps.splitAudio(p, id, at)
                result = split?.second
                split?.first ?: p
            }
            if (result == null) model.message = EditorModel.Message(R.string.timeline_message_split_failed)
        },
    )

    SlateScreen(
        title = model.name ?: stringResource(R.string.timeline_title),
        singleLineTitle = true,
        onBack = back,
        snackbarHostState = snackbar,
        actions = {
            TopBarAction(R.drawable.ic_undo_24, stringResource(R.string.timeline_undo), "slate.timeline.undo") { model.undo() }
            TopBarAction(R.drawable.ic_redo_24, stringResource(R.string.timeline_redo), "slate.timeline.redo") { model.redo() }
            TopBarAction(R.drawable.ic_save_24, stringResource(R.string.timeline_save), "slate.timeline.save") { model.save() }
            TopBarAction(R.drawable.ic_upload_24, stringResource(R.string.timeline_export), "slate.timeline.export") {
                if (model.project.isEmpty) model.message = EditorModel.Message(R.string.timeline_message_empty) else exporting = true
            }
            Box {
                TopBarAction(R.drawable.ic_more_vert_24, stringResource(R.string.timeline_more), "slate.timeline.more") { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.timeline_new)) }, onClick = { menu = false; model.newProject() },
                        modifier = Modifier.testTag("slate.timeline.menu.new"))
                    DropdownMenuItem(text = { Text(stringResource(R.string.timeline_open)) }, onClick = { menu = false; showProjects = true },
                        modifier = Modifier.testTag("slate.timeline.menu.open"))
                    DropdownMenuItem(text = { Text(stringResource(R.string.timeline_open_file)) }, onClick = { menu = false; projectPicker.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.testTag("slate.timeline.menu.openFile"))
                }
            }
        },
    ) { padding ->
        val wide = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } >= WIDE_DP.dp
        val timelineDescription = stringResource(R.string.timeline_tracks_description)
        val labels = TimelineLabels(
            overlays = stringResource(R.string.timeline_track_overlays),
            subtitles = stringResource(R.string.timeline_track_subtitles),
            video = { "V$it" },
            audio = { "A$it" },
        )
        Column(Modifier.fillMaxSize().padding(padding).testTag("slate.timeline")) {
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp)) else Spacer(Modifier.height(2.dp))
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Preview(model, Modifier.weight(1f).fillMaxWidth())
                        Transport(model, actions)
                        TimelineView(model, height = 260.dp, description = timelineDescription, labels = labels)
                    }
                    Column(Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        Inspector(model, actions)
                    }
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val available = maxHeight
                    val previewHeight = (available * 0.32f).coerceAtLeast(160.dp)
                    Column(Modifier.fillMaxSize()) {
                        Preview(model, Modifier.fillMaxWidth().height(previewHeight))
                        Transport(model, actions)
                        TimelineView(model, height = (available * 0.30f).coerceAtLeast(180.dp), description = timelineDescription, labels = labels)
                        HorizontalDivider()
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                            Inspector(model, actions)
                        }
                    }
                }
            }
        }
    }

    picker?.let { kind ->
        if (kind == Picker.SUBTITLES) {
            LaunchedEffect(kind) { systemPicker.launch(arrayOf("application/x-subrip", "text/x-ssa", "text/plain", "*/*")) }
        } else {
            LibraryPickerDialog(
                kind = kind,
                onDismiss = { picker = null },
                onPick = { path ->
                    picker = null
                    model.add(listOf(path), when (kind) {
                        Picker.AUDIO -> EditorModel.AddKind.AUDIO
                        Picker.IMAGE -> EditorModel.AddKind.IMAGE
                        else -> EditorModel.AddKind.VIDEO
                    })
                },
                onSystem = {
                    systemPicker.launch(when (kind) {
                        Picker.AUDIO -> arrayOf("audio/*", "video/*")
                        Picker.IMAGE -> arrayOf("image/*")
                        else -> arrayOf("video/*")
                    })
                },
            )
        }
    }
    textTarget?.let { id ->
        val initial = project.overlays.firstOrNull { it.id == id }?.text ?: project.subtitles.cues.firstOrNull { it.id == id }?.text
        if (initial == null) {
            textTarget = null
        } else {
            TextDialog(initial, onDismiss = { textTarget = null }) { text ->
                textTarget = null
                if (text.isNotBlank()) model.edit { p ->
                    if (p.overlays.any { it.id == id }) EditOps.updateOverlay(p, id) { it.copy(text = text) }
                    else EditOps.updateCue(p, id, text = text.take(EditOps.MAX_CUE_CHARS))
                }
            }
        }
    }
    importPath?.let { path ->
        MessageDialog(
            title = stringResource(R.string.timeline_import_title),
            message = stringResource(R.string.timeline_import_message, project.subtitles.cues.size),
            onDismissRequest = { importPath = null },
            onConfirm = { importPath = null; model.importSubtitles(path, append = false) },
            confirmText = stringResource(R.string.timeline_import_replace),
            neutralText = stringResource(R.string.timeline_import_append),
            onNeutral = { importPath = null; model.importSubtitles(path, append = true) },
            tag = "slate.timeline.import",
        )
    }
    if (showProjects) {
        ProjectsDialog(onDismiss = { showProjects = false }) { file ->
            showProjects = false
            model.open(file)
        }
    }
    if (leaving) {
        MessageDialog(
            title = stringResource(R.string.timeline_unsaved_title),
            message = stringResource(R.string.timeline_unsaved_message),
            onDismissRequest = { leaving = false },
            onConfirm = { leaving = false; model.save { onFinish() } },
            confirmText = stringResource(R.string.timeline_save),
            neutralText = stringResource(R.string.timeline_discard),
            onNeutral = { leaving = false; onFinish() },
            tag = "slate.timeline.unsaved",
        )
    }
    if (exporting) ExportDialog(model, onDismiss = { exporting = false }) {
        exporting = false
        onExported(model.export())
    }
}

@Composable
private fun Preview(model: EditorModel, modifier: Modifier) {
    TimelinePreviewFrame(
        preview = model.preview,
        project = model.project,
        modifier = modifier.testTag("slate.timeline.preview"),
        overlayModifier = { overlay, pxScale ->
            if (overlay.id == model.selected) {
                val (frameW, frameH) = TimelinePreview.frameSize(model.project)
                Modifier.pointerInput(overlay.id, pxScale) {
                    var total = Offset.Zero
                    detectDragGestures(
                        onDragStart = { total = Offset.Zero; model.beginGesture() },
                        onDragEnd = { model.endGesture() },
                        onDragCancel = { model.endGesture() },
                    ) { change, drag ->
                        change.consume()
                        total += drag
                        val dx = total.x / (frameW * pxScale) * 100.0
                        val dy = total.y / (frameH * pxScale) * 100.0
                        model.gesture { p -> EditOps.updateOverlay(p, overlay.id) { it.copy(x = it.x + dx, y = it.y + dy) } }
                    }
                }
            } else {
                Modifier.pointerInput(overlay.id) { detectTapGestures { model.selected = overlay.id } }
            }
        },
    )
}

@Composable
private fun Transport(model: EditorModel, actions: InspectorActions) {
    val preview = model.preview
    // Playback controls keep their direction in right-to-left languages (Material bidirectionality).
    // zh-CN: 播放控件在从右到左的语言中保持方向 (Material 双向文字规范).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { preview.toggle() }, modifier = Modifier.testTag("slate.timeline.play")) {
                Icon(
                    painterResource(if (preview.playing) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24),
                    contentDescription = stringResource(if (preview.playing) R.string.timeline_pause else R.string.timeline_play),
                )
            }
            Text(
                Formats.ltr("${formatTime(preview.positionMs)} / ${formatTime(model.project.durationUs / 1_000.0)}"),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f).testTag("slate.timeline.time"),
            )
            IconButton(onClick = actions.split, enabled = model.selected != null, modifier = Modifier.testTag("slate.timeline.transport.split")) {
                Icon(painterResource(R.drawable.ic_content_cut_24), contentDescription = stringResource(R.string.timeline_split))
            }
            IconButton(onClick = { model.zoom = (model.zoom / ZOOM_STEP).coerceAtLeast(EditorModel.MIN_ZOOM) }, modifier = Modifier.testTag("slate.timeline.zoomOut")) {
                Icon(painterResource(R.drawable.ic_zoom_out_24), contentDescription = stringResource(R.string.timeline_zoom_out))
            }
            IconButton(onClick = { model.zoom = (model.zoom * ZOOM_STEP).coerceAtMost(EditorModel.MAX_ZOOM) }, modifier = Modifier.testTag("slate.timeline.zoomIn")) {
                Icon(painterResource(R.drawable.ic_zoom_in_24), contentDescription = stringResource(R.string.timeline_zoom_in))
            }
        }
    }
}

@Composable
private fun LibraryPickerDialog(kind: Picker, onDismiss: () -> Unit, onPick: (String) -> Unit, onSystem: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<MediaEntry>?>(null) }
    LaunchedEffect(kind) {
        entries = withContext(Dispatchers.IO) {
            runCatching { MediaLibrary.query(context, MediaLibrary.folders(context)) }.getOrDefault(emptyList()).filter { entry ->
                when (kind) {
                    Picker.VIDEO -> entry.kind == MediaKind.VIDEO
                    Picker.AUDIO -> entry.kind == MediaKind.AUDIO || entry.kind == MediaKind.VIDEO
                    Picker.IMAGE -> entry.kind == MediaKind.IMAGE
                    Picker.SUBTITLES -> false
                }
            }
        }
    }
    SlateDialog(
        title = stringResource(R.string.wizard_from_library),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_close),
        onConfirm = onDismiss,
        dismissText = null,
        neutralText = stringResource(R.string.wizard_pick_file),
        onNeutral = onSystem,
        tag = "slate.timeline.library",
    ) {
        val list = entries
        when {
            list == null -> Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Text(stringResource(R.string.library_empty_title), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            else -> list.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(entry.path) }.padding(horizontal = 24.dp, vertical = 8.dp).testTag("slate.timeline.library.item"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Thumbnail(entry, Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(entry.name, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        entry.durationMillis?.let { Text(Formats.duration(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsDialog(onDismiss: () -> Unit, onOpen: (File) -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<ProjectStore.Entry>?>(null) }
    LaunchedEffect(Unit) { entries = withContext(Dispatchers.IO) { ProjectStore.list(context) } }
    SlateDialog(
        title = stringResource(R.string.timeline_open),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_close),
        onConfirm = onDismiss,
        dismissText = null,
        tag = "slate.timeline.projects",
    ) {
        val list = entries
        when {
            list == null -> Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Text(stringResource(R.string.timeline_projects_empty), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            else -> list.forEach { entry ->
                Column(Modifier.fillMaxWidth().clickable { onOpen(entry.file) }.padding(horizontal = 24.dp, vertical = 8.dp).testTag("slate.timeline.projects.item")) {
                    Text(entry.name, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content), maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                    Text(
                        listOfNotNull(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.modified)),
                            if (entry.shared) null else stringResource(R.string.timeline_project_private),
                        ).joinToString("  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TextDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    SlateDialog(
        title = stringResource(R.string.timeline_edit_text),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { onConfirm(text) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = text.isNotBlank(),
        tag = "slate.timeline.text",
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(EditOps.MAX_CUE_CHARS) },
            minLines = 2,
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("slate.timeline.text.field"),
        )
    }
}

/**
 * Confirms the export after the plugin's own check: a refused timeline shows the plugin's detail, a timeline above the
 * measured decoder count (appendix D Q37) shows the budget, as the export failure would.
 * zh-CN: 经插件自身检查后确认导出: 被拒绝的时间线显示插件的详情, 超过实测解码路数 (附录 D Q37) 时显示与导出失败相同的预算说明.
 */
@Composable
private fun ExportDialog(model: EditorModel, onDismiss: () -> Unit, onExport: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(model.project) {
        problem = model.check()
        checked = true
    }
    val budget = remember(model.project) { model.budget() }
    SlateDialog(
        title = stringResource(R.string.timeline_export_title),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.timeline_export),
        onConfirm = onExport,
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = checked && problem == null,
        tag = "slate.timeline.exportDialog",
    ) {
        val project = model.project
        val (w, h) = TimelinePreview.frameSize(project)
        Text(
            listOf(
                Formats.resolution(w, h),
                Formats.ltr("${project.frameRate} fps"),
                Formats.ltr(formatTime(project.durationUs / 1_000.0)),
                Formats.ltr(project.export.codec.uppercase()),
                project.export.bitRate?.let { Formats.bitRate(it) } ?: stringResource(R.string.timeline_bit_rate_auto),
            ).joinToString("  "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.timeline_export_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp))
        if (!checked) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp))
        problem?.let {
            Text(stringResource(R.string.timeline_export_invalid), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp).testTag("slate.timeline.exportDialog.problem"))
        }
        budget?.let {
            Text(
                stringResource(R.string.timeline_budget, it.count, it.overlappingVideoTracks, it.otherVideoTracks, it.audioTracks,
                    io.github.supermonster003.autojs6.plugin.three.slate.studio.core.CodecResources.MEASURED_PASSING_SOURCES),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp).testTag("slate.timeline.exportDialog.budget"),
            )
        }
    }
}

private const val WIDE_DP = 600
private const val ZOOM_STEP = 1.5


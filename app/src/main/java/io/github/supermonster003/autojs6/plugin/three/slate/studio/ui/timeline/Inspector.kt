package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionType
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.preview.TimelinePreview
import io.github.supermonster003.autojs6.plugin.three.slate.studio.preview.parseColor
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.AudioClipModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.CueModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditOps
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditorProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.OverlayModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.SubtitleLayer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.TransitionModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.VideoClipModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SectionHeader
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** What the inspector asks the screen to do (pickers and dialogs). zh-CN: 检查器请界面执行的操作 (选择器与对话框). */
internal class InspectorActions(
    val addVideo: () -> Unit,
    val addAudio: () -> Unit,
    val addImage: () -> Unit,
    val addText: () -> Unit,
    val addCue: () -> Unit,
    val importSubtitles: () -> Unit,
    val exportSrt: () -> Unit,
    val editText: (Long) -> Unit,
    val split: () -> Unit,
)

/**
 * The panel below (or beside) the timeline: what can be added and the project settings when nothing is selected,
 * otherwise the settings of the selected clip, overlay or cue. Sliders are one undo step per drag.
 * zh-CN: 时间线下方 (或旁边) 的面板: 未选择时为可添加的内容与工程设置, 否则为所选片段, 叠加物或字幕条的设置. 每次拖动滑块为一步撤销.
 */
@Composable
internal fun Inspector(model: EditorModel, actions: InspectorActions, modifier: Modifier = Modifier) {
    val project = model.project
    val id = model.selected
    Column(modifier.testTag("slate.timeline.inspector")) {
        when {
            id == null -> ProjectPanel(model, project, actions)
            project.videoClip(id) != null -> VideoPanel(model, project, project.videoClip(id)!!.first, project.videoClip(id)!!.second, actions)
            project.audioClip(id) != null -> AudioPanel(model, project, project.audioClip(id)!!.second, actions)
            project.overlays.any { it.id == id } -> OverlayPanel(model, project.overlays.first { it.id == id }, actions)
            project.subtitles.cues.any { it.id == id } -> CuePanel(model, project, project.subtitles.cues.first { it.id == id }, actions)
            else -> ProjectPanel(model, project, actions)
        }
    }
}

@Composable
private fun ProjectPanel(model: EditorModel, project: EditorProject, actions: InspectorActions) {
    SectionHeader(stringResource(R.string.timeline_add))
    ButtonRow {
        ActionButton(R.drawable.ic_movie_24, stringResource(R.string.timeline_add_video), "slate.timeline.add.video", onClick = actions.addVideo)
        ActionButton(R.drawable.ic_music_note_24, stringResource(R.string.timeline_add_audio), "slate.timeline.add.audio", onClick = actions.addAudio)
        ActionButton(R.drawable.ic_text_fields_24, stringResource(R.string.choice_overlay_text), "slate.timeline.add.text", onClick = actions.addText)
        ActionButton(R.drawable.ic_image_24, stringResource(R.string.choice_overlay_image), "slate.timeline.add.image", onClick = actions.addImage)
        ActionButton(R.drawable.ic_subtitles_24, stringResource(R.string.timeline_add_cue), "slate.timeline.add.cue", onClick = actions.addCue)
    }
    ButtonRow {
        ActionButton(R.drawable.ic_add_24, stringResource(R.string.timeline_add_video_track), "slate.timeline.add.videoTrack",
            enabled = project.videoTracks.size < EditorProject.MAX_VIDEO_TRACKS) { model.edit(EditOps::addVideoTrack) }
        ActionButton(R.drawable.ic_add_24, stringResource(R.string.timeline_add_audio_track), "slate.timeline.add.audioTrack",
            enabled = project.audioTracks.size < EditorProject.MAX_AUDIO_TRACKS) { model.edit(EditOps::addAudioTrack) }
        val empty = project.videoTracks.drop(1).lastOrNull { it.clips.isEmpty() }?.id
            ?: project.audioTracks.lastOrNull { it.clips.isEmpty() }?.id
        ActionButton(R.drawable.ic_delete_24, stringResource(R.string.timeline_remove_track), "slate.timeline.removeTrack", enabled = empty != null) {
            empty?.let { trackId -> model.edit { EditOps.removeTrack(it, trackId) } }
        }
    }
    SectionHeader(stringResource(R.string.timeline_track_subtitles))
    Text(
        stringResource(R.string.timeline_cues_count, project.subtitles.cues.size),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    ButtonRow {
        ActionButton(R.drawable.ic_file_24, stringResource(R.string.timeline_import_subtitles), "slate.timeline.importSubtitles", onClick = actions.importSubtitles)
        ActionButton(R.drawable.ic_share_24, stringResource(R.string.timeline_export_srt), "slate.timeline.exportSrt",
            enabled = project.subtitles.cues.isNotEmpty(), onClick = actions.exportSrt)
    }
    SubtitleStyle(model, project.subtitles)

    SectionHeader(stringResource(R.string.timeline_project))
    val (autoW, autoH) = TimelinePreview.frameSize(project.copy(width = null, height = null))
    val sizes = listOf<Pair<Int, Int>?>(null, 1920 to 1080, 1280 to 720, 1080 to 1920, 720 to 1280, 1080 to 1080)
    Chips(
        stringResource(R.string.detail_resolution),
        sizes.map { size -> (size?.let { "${it.first}x${it.second}" } ?: "auto") to (size?.let { Formats.resolution(it.first, it.second) }
            ?: stringResource(R.string.timeline_resolution_auto, Formats.resolution(autoW, autoH))) },
        project.width?.let { "${it}x${project.height}" } ?: "auto",
        "slate.timeline.resolution",
    ) { key ->
        val size = sizes.firstOrNull { (it?.let { s -> "${s.first}x${s.second}" } ?: "auto") == key }
        model.edit { it.copy(width = size?.first, height = size?.second) }
    }
    Chips(
        stringResource(R.string.option_frame_rate),
        listOf(24, 25, 30, 50, 60).map { it.toString() to Formats.ltr("$it fps") },
        project.frameRate.toString(),
        "slate.timeline.frameRate",
    ) { key -> model.edit { it.copy(frameRate = key.toInt()) } }
    Chips(
        stringResource(R.string.option_background),
        COLORS.map { it to it },
        project.background.uppercase(Locale.ROOT),
        "slate.timeline.background",
        swatches = true,
    ) { key -> model.edit { it.copy(background = key) } }
    Chips(
        stringResource(R.string.option_codec),
        VideoCodec.entries.map { it.id to it.id.uppercase(Locale.ROOT).replace("H2", "H.2") },
        project.export.codec,
        "slate.timeline.codec",
    ) { key -> model.edit { it.copy(export = it.export.copy(codec = key)) } }
    Chips(
        stringResource(R.string.option_bit_rate),
        BIT_RATES.map { (it?.toString() ?: "auto") to (it?.let { rate -> Formats.bitRate(rate) } ?: stringResource(R.string.timeline_bit_rate_auto)) },
        project.export.bitRate?.toString() ?: "auto",
        "slate.timeline.bitRate",
    ) { key -> model.edit { it.copy(export = it.export.copy(bitRate = key.toLongOrNull())) } }
    Spacer(Modifier.size(24.dp))
}

@Composable
private fun SubtitleStyle(model: EditorModel, layer: SubtitleLayer) {
    Chips(
        stringResource(R.string.option_subtitle_align),
        SubtitleLayer.ALIGNS.map { it to stringResource(alignLabel(it)) },
        layer.align,
        "slate.timeline.subtitleAlign",
    ) { key -> model.edit { EditOps.setSubtitleStyle(it) { s -> s.copy(align = key) } } }
    Chips(
        stringResource(R.string.option_color),
        COLORS.map { it to it },
        layer.color.uppercase(Locale.ROOT),
        "slate.timeline.subtitleColor",
        swatches = true,
    ) { key -> model.edit { EditOps.setSubtitleStyle(it) { s -> s.copy(color = key) } } }
    val defaultSize = (TimelinePreview.frameSize(model.project).second / 18.0)
    ValueSlider(
        model,
        stringResource(R.string.option_font_size),
        (layer.fontSize ?: defaultSize).toFloat(),
        12f..200f,
        Formats.ltr("${(layer.fontSize ?: defaultSize).roundToInt()} px"),
        "slate.timeline.subtitleSize",
    ) { project, value -> EditOps.setSubtitleStyle(project) { s -> s.copy(fontSize = value.roundToInt().toDouble()) } }
    SwitchRow(stringResource(R.string.option_stroke), layer.stroke != null, "slate.timeline.subtitleStroke") { on ->
        model.edit { EditOps.setSubtitleStyle(it) { s -> s.copy(stroke = if (on) "#000000" else null) } }
    }
    SwitchRow(stringResource(R.string.option_background), layer.background != null, "slate.timeline.subtitleBackground") { on ->
        model.edit { EditOps.setSubtitleStyle(it) { s -> s.copy(background = if (on) "#80000000" else null) } }
    }
}

@Composable
private fun VideoPanel(model: EditorModel, project: EditorProject, track: Int, clip: VideoClipModel, actions: InspectorActions) {
    val clips = project.videoTracks[track].clips
    val index = clips.indexOfFirst { it.id == clip.id }
    ClipTitle(R.string.timeline_clip, clip.src, project.videoPlacements[clip.id]?.let { it.atMs to it.endMs })
    ButtonRow {
        ActionButton(R.drawable.ic_content_cut_24, stringResource(R.string.timeline_split), "slate.timeline.split", onClick = actions.split)
        ActionButton(R.drawable.ic_delete_24, stringResource(R.string.detail_delete), "slate.timeline.delete") { model.edit { EditOps.deleteClip(it, clip.id) } }
        ActionButton(R.drawable.ic_arrow_upward_24, stringResource(R.string.timeline_move_earlier), "slate.timeline.earlier", enabled = index > 0) {
            model.edit { EditOps.reorderVideo(it, clip.id, -1) }
        }
        ActionButton(R.drawable.ic_arrow_downward_24, stringResource(R.string.timeline_move_later), "slate.timeline.later", enabled = index < clips.lastIndex) {
            model.edit { EditOps.reorderVideo(it, clip.id, 1) }
        }
    }
    if (project.videoTracks.size > 1) {
        Chips(
            stringResource(R.string.timeline_move_to_track),
            project.videoTracks.indices.map { it.toString() to Formats.ltr("V${it + 1}") },
            track.toString(),
            "slate.timeline.track",
        ) { key -> model.edit { EditOps.moveVideoToTrack(it, clip.id, key.toInt()) } }
    }
    ValueSlider(model, stringResource(R.string.option_factor), clip.speed.toFloat(), MediaStudioLimits.MIN_SPEED_FACTOR.toFloat()..4f,
        Formats.ltr(formatSpeed(clip.speed)), "slate.timeline.speed") { project, value -> EditOps.setSpeed(project, clip.id, (value * 20).roundToInt() / 20.0) }
    ValueSlider(model, stringResource(R.string.option_volume), clip.volume.toFloat(), 0f..2f, Formats.ltr("${(clip.volume * 100).roundToInt()}%"),
        "slate.timeline.volume") { project, value -> EditOps.setVolume(project, clip.id, (value * 20).roundToInt() / 20.0) }

    if (index > 0) {
        SectionHeader(stringResource(R.string.timeline_transition))
        val transition = clip.transition
        val type = transition?.type ?: "cut"
        Chips(
            null,
            TransitionType.IDS.map { it to stringResource(transitionLabel(it)) },
            type,
            "slate.timeline.transition",
        ) { key ->
            model.edit { EditOps.setTransition(it, clip.id, if (key == "cut") null else TransitionModel(key, transition?.durationMs ?: MediaStudioLimits.DEFAULT_TRANSITION_MILLIS, transition?.direction)) }
        }
        if (transition != null) {
            val max = EditOps.maxTransitionMs(clips, index, transition.type).coerceAtLeast(MediaStudioLimits.MIN_TRANSITION_MILLIS)
            ValueSlider(model, stringResource(R.string.option_duration), transition.durationMs.toFloat(),
                MediaStudioLimits.MIN_TRANSITION_MILLIS.toFloat()..max.toFloat().coerceAtLeast(MediaStudioLimits.MIN_TRANSITION_MILLIS + 1f),
                Formats.ltr(seconds(transition.durationMs)), "slate.timeline.transitionDuration") { project, value ->
                EditOps.setTransition(project, clip.id, transition.copy(durationMs = (value / 50f).roundToLong() * 50))
            }
            if (transition.kind.directional) {
                Chips(
                    stringResource(R.string.timeline_direction),
                    DIRECTIONS.map { it to stringResource(directionLabel(it)) },
                    transition.direction ?: "left",
                    "slate.timeline.direction",
                ) { key -> model.edit { EditOps.setTransition(it, clip.id, transition.copy(direction = key)) } }
            }
        }
    }

    SectionHeader(stringResource(R.string.option_scale))
    ValueSlider(model, stringResource(R.string.option_scale), clip.scale.toFloat(), 0.25f..4f, Formats.ltr(String.format(Locale.ROOT, "%.2fx", clip.scale)),
        "slate.timeline.scale") { project, value -> EditOps.setTransform(project, clip.id, scale = (value * 20).roundToInt() / 20.0) }
    Chips(stringResource(R.string.option_rotate), listOf(0, 90, 180, 270).map { it.toString() to Formats.degrees(it) }, clip.rotate.toString(),
        "slate.timeline.rotate") { key -> model.edit { EditOps.setTransform(it, clip.id, rotate = key.toInt()) } }
    Chips(stringResource(R.string.option_flip), VideoClipModel.FLIPS.map { it to stringResource(flipLabel(it)) }, clip.flip, "slate.timeline.flip") { key ->
        model.edit { EditOps.setTransform(it, clip.id, flip = key) }
    }
    Chips(stringResource(R.string.option_fit), VideoClipModel.FITS.map { it to stringResource(fitLabel(it)) }, clip.fit, "slate.timeline.fit") { key ->
        model.edit { EditOps.setTransform(it, clip.id, fit = key) }
    }
    Spacer(Modifier.size(24.dp))
}

@Composable
private fun AudioPanel(model: EditorModel, project: EditorProject, clip: AudioClipModel, actions: InspectorActions) {
    ClipTitle(R.string.timeline_audio_clip, clip.src, project.audioPlacements[clip.id]?.let { it.atMs to it.endMs })
    ButtonRow {
        ActionButton(R.drawable.ic_content_cut_24, stringResource(R.string.timeline_split), "slate.timeline.split", onClick = actions.split)
        ActionButton(R.drawable.ic_delete_24, stringResource(R.string.detail_delete), "slate.timeline.delete") { model.edit { EditOps.deleteClip(it, clip.id) } }
    }
    val length = (clip.endMs - clip.startMs).toFloat()
    ValueSlider(model, stringResource(R.string.option_volume), clip.volume.toFloat(), 0f..2f, Formats.ltr("${(clip.volume * 100).roundToInt()}%"),
        "slate.timeline.volume") { project, value -> EditOps.setVolume(project, clip.id, (value * 20).roundToInt() / 20.0) }
    ValueSlider(model, stringResource(R.string.option_fade_in), clip.fadeInMs.toFloat(), 0f..minOf(length, 10_000f).coerceAtLeast(1f),
        Formats.ltr(seconds(clip.fadeInMs)), "slate.timeline.fadeIn") { project, value ->
        val current = project.audioClip(clip.id)?.second ?: clip
        EditOps.setFades(project, clip.id, (value / 100f).roundToLong() * 100, current.fadeOutMs)
    }
    ValueSlider(model, stringResource(R.string.option_fade_out), clip.fadeOutMs.toFloat(), 0f..minOf(length, 10_000f).coerceAtLeast(1f),
        Formats.ltr(seconds(clip.fadeOutMs)), "slate.timeline.fadeOut") { project, value ->
        val current = project.audioClip(clip.id)?.second ?: clip
        EditOps.setFades(project, clip.id, current.fadeInMs, (value / 100f).roundToLong() * 100)
    }
    Spacer(Modifier.size(24.dp))
}

@Composable
private fun OverlayPanel(model: EditorModel, overlay: OverlayModel, actions: InspectorActions) {
    val text = overlay.type == OverlayModel.TEXT
    SectionHeader(stringResource(if (text) R.string.timeline_text_overlay else R.string.timeline_image_overlay))
    TimeRange(overlay.fromMs.toDouble(), overlay.toMs.toDouble())
    if (text) {
        Text(overlay.text.orEmpty(), style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp))
    } else {
        Text(Formats.isolate(File(overlay.src.orEmpty()).name), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 24.dp))
    }
    ButtonRow {
        if (text) ActionButton(R.drawable.ic_edit_24, stringResource(R.string.timeline_edit_text), "slate.timeline.editText") { actions.editText(overlay.id) }
        ActionButton(R.drawable.ic_delete_24, stringResource(R.string.detail_delete), "slate.timeline.delete") { model.edit { EditOps.deleteClip(it, overlay.id) } }
        ActionButton(R.drawable.ic_schedule_24, stringResource(R.string.timeline_start_here), "slate.timeline.startHere") {
            model.edit { EditOps.updateOverlay(it, overlay.id) { o -> o.copy(fromMs = model.playheadMs.coerceAtMost(o.toMs - EditOps.MIN_CLIP_MS)) } }
        }
        ActionButton(R.drawable.ic_schedule_24, stringResource(R.string.timeline_end_here), "slate.timeline.endHere") {
            model.edit { EditOps.updateOverlay(it, overlay.id) { o -> o.copy(toMs = model.playheadMs) } }
        }
        ActionButton(R.drawable.ic_tune_24, stringResource(R.string.timeline_center), "slate.timeline.center") {
            model.edit { EditOps.updateOverlay(it, overlay.id) { o -> o.copy(x = 50.0, y = 50.0) } }
        }
    }
    Text(stringResource(R.string.timeline_position_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp))
    if (text) {
        ValueSlider(model, stringResource(R.string.option_font_size), overlay.fontSize.toFloat(), 12f..240f, Formats.ltr("${overlay.fontSize.roundToInt()} px"),
            "slate.timeline.fontSize") { project, value -> EditOps.updateOverlay(project, overlay.id) { it.copy(fontSize = value.roundToInt().toDouble()) } }
        Chips(stringResource(R.string.option_color), COLORS.map { it to it }, overlay.color.uppercase(Locale.ROOT), "slate.timeline.color", swatches = true) { key ->
            model.edit { EditOps.updateOverlay(it, overlay.id) { o -> o.copy(color = key) } }
        }
        SwitchRow(stringResource(R.string.option_stroke), overlay.stroke != null, "slate.timeline.stroke") { on ->
            model.edit { EditOps.updateOverlay(it, overlay.id) { o -> o.copy(stroke = if (on) "#000000" else null) } }
        }
    } else {
        ValueSlider(model, stringResource(R.string.option_scale), overlay.scale.toFloat(), 0.1f..4f, Formats.ltr(String.format(Locale.ROOT, "%.2fx", overlay.scale)),
            "slate.timeline.scale") { project, value -> EditOps.updateOverlay(project, overlay.id) { it.copy(scale = (value * 20).roundToInt() / 20.0) } }
    }
    ValueSlider(model, stringResource(R.string.option_alpha), overlay.alpha.toFloat(), 0f..1f, Formats.ltr("${(overlay.alpha * 100).roundToInt()}%"),
        "slate.timeline.alpha") { project, value -> EditOps.updateOverlay(project, overlay.id) { it.copy(alpha = (value * 20).roundToInt() / 20.0) } }
    Spacer(Modifier.size(24.dp))
}

@Composable
private fun CuePanel(model: EditorModel, project: EditorProject, cue: CueModel, actions: InspectorActions) {
    SectionHeader(stringResource(R.string.timeline_cue))
    TimeRange(cue.startMs.toDouble(), cue.endMs.toDouble())
    Text(cue.text, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content), maxLines = 4, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 24.dp))
    ButtonRow {
        ActionButton(R.drawable.ic_edit_24, stringResource(R.string.timeline_edit_text), "slate.timeline.editText") { actions.editText(cue.id) }
        ActionButton(R.drawable.ic_delete_24, stringResource(R.string.detail_delete), "slate.timeline.delete") { model.edit { EditOps.deleteClip(it, cue.id) } }
        ActionButton(R.drawable.ic_schedule_24, stringResource(R.string.timeline_start_here), "slate.timeline.startHere") {
            model.edit { EditOps.updateCue(it, cue.id, startMs = model.playheadMs.coerceAtMost(cue.endMs - EditOps.MIN_CLIP_MS)) }
        }
        ActionButton(R.drawable.ic_schedule_24, stringResource(R.string.timeline_end_here), "slate.timeline.endHere") {
            model.edit { EditOps.updateCue(it, cue.id, endMs = model.playheadMs) }
        }
    }
    SectionHeader(stringResource(R.string.option_style))
    SubtitleStyle(model, project.subtitles)
    Spacer(Modifier.size(24.dp))
}

@Composable
private fun ClipTitle(title: Int, src: String, span: Pair<Double, Double>?) {
    SectionHeader(stringResource(title))
    Text(Formats.isolate(File(src).name), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
        modifier = Modifier.padding(horizontal = 24.dp))
    span?.let { TimeRange(it.first, it.second) }
}

@Composable
private fun TimeRange(from: Double, to: Double) {
    Text(
        Formats.ltr("${formatTime(from)} - ${formatTime(to)}"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp).testTag("slate.timeline.range"),
    )
}

@Composable
private fun ButtonRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ActionButton(@DrawableRes icon: Int, label: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/** A row of single-choice chips; [swatches] shows color values. zh-CN: 单选标签行; [swatches] 用于颜色值. */
@Composable
private fun Chips(label: String?, options: List<Pair<String, String>>, selected: String, tag: String, swatches: Boolean = false, onSelect: (String) -> Unit) {
    if (label != null) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp))
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 4.dp).testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (key, text) ->
            FilterChip(
                selected = key == selected,
                onClick = { if (key != selected) onSelect(key) },
                label = { Text(if (swatches) Formats.ltr(text) else text) },
                leadingIcon = if (swatches) {
                    { Box(Modifier.size(16.dp).background(parseColor(key, Color.Transparent), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) }
                } else null,
                modifier = Modifier.testTag("$tag.$key"),
            )
        }
    }
}

/**
 * A labeled slider whose drag is one undo step: [change] applies the value to the state at the start of the drag.
 * zh-CN: 带标签的滑块, 一次拖动为一步撤销: [change] 把数值作用于拖动开始时的状态.
 */
@Composable
private fun ValueSlider(
    model: EditorModel,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    tag: String,
    change: (EditorProject, Float) -> EditorProject,
) {
    var dragging by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(display, style = MaterialTheme.typography.labelLarge)
    }
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = { next ->
            if (!dragging) {
                dragging = true
                model.beginGesture()
            }
            model.gesture { change(it, next) }
        },
        onValueChangeFinished = {
            dragging = false
            model.endGesture()
        },
        valueRange = range,
        modifier = Modifier.padding(horizontal = 16.dp).testTag(tag),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

/** `0.75 s`: up to two decimals. zh-CN: 至多两位小数的秒数. */
private fun seconds(ms: Long): String = String.format(Locale.ROOT, "%.2f", ms / 1_000.0).trimEnd('0').trimEnd('.') + " s"

internal fun formatTime(ms: Double): String {
    val total = ms.coerceAtLeast(0.0).roundToLong()
    return String.format(Locale.ROOT, "%d:%02d.%d", total / 60_000, (total % 60_000) / 1_000, (total % 1_000) / 100)
}

private val COLORS = listOf("#FFFFFF", "#000000", "#FFEB3B", "#F44336", "#4CAF50", "#2196F3", "#9E9E9E")
private val BIT_RATES = listOf<Long?>(null, 4_000_000L, 8_000_000L, 12_000_000L, 20_000_000L)
private val DIRECTIONS = listOf("left", "right", "up", "down")

private fun transitionLabel(id: String): Int = when (id) {
    "fade" -> R.string.timeline_transition_fade
    "dissolve" -> R.string.timeline_transition_dissolve
    "slide" -> R.string.timeline_transition_slide
    "wipe" -> R.string.timeline_transition_wipe
    "zoom" -> R.string.timeline_transition_zoom
    "blur" -> R.string.timeline_transition_blur
    else -> R.string.timeline_transition_cut
}

private fun directionLabel(id: String): Int = when (id) {
    "right" -> R.string.timeline_direction_right
    "up" -> R.string.timeline_direction_up
    "down" -> R.string.timeline_direction_down
    else -> R.string.timeline_direction_left
}

private fun alignLabel(id: String): Int = when (id) {
    "top" -> R.string.choice_anchor_top
    "center" -> R.string.choice_anchor_center
    else -> R.string.choice_anchor_bottom
}

private fun flipLabel(id: String): Int = when (id) {
    "horizontal" -> R.string.choice_flip_horizontal
    "vertical" -> R.string.choice_flip_vertical
    "both" -> R.string.choice_flip_both
    else -> R.string.choice_flip_none
}

private fun fitLabel(id: String): Int = when (id) {
    "cover" -> R.string.choice_fit_cover
    "stretch" -> R.string.choice_fit_stretch
    else -> R.string.choice_fit_contain
}

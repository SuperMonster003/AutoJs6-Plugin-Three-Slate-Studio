package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditOps
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditorProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.OverlayModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.Snapping
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * The multi-track timeline (roadmap P4.3): a ruler, the overlay and subtitle rows, the video tracks with the upper
 * layer on top and the audio tracks, drawn on one canvas around a fixed playhead in the middle. Dragging empty space or
 * an unselected item scrubs, dragging the selected item moves it (snapping to edges, the playhead and 0; a video clip
 * can be dropped on another video track), its edge handles trim, a tap selects, two fingers zoom and a vertical drag
 * scrolls the tracks. Time runs left to right in every language, like playback controls (Material bidirectionality).
 * zh-CN: 多轨时间线 (路线图 P4.3): 标尺, 叠加物与字幕行, 上层在上的视频轨与音频轨, 绘制在一张画布上, 播放头固定在中间. 拖动空白或未选中
 * 的条目为拖动播放头, 拖动已选条目为移动 (吸附到边缘, 播放头与 0; 视频片段可放到另一条视频轨), 拖动两端手柄为裁剪, 点按为选择, 双指缩放,
 * 竖向拖动滚动轨道. 时间在所有语言中都从左到右, 与播放控件相同 (Material 双向文字规范).
 */
@Composable
internal fun TimelineView(model: EditorModel, modifier: Modifier = Modifier, height: Dp, description: String, labels: TimelineLabels) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        TimelineCanvas(model, modifier.fillMaxWidth().height(height).semantics { contentDescription = description }.testTag("slate.timeline.tracks"), labels)
    }
}

/** Track names, already localized and isolated. zh-CN: 已本地化的轨道名. */
internal data class TimelineLabels(val overlays: String, val subtitles: String, val video: (Int) -> String, val audio: (Int) -> String)

private enum class RowKind { RULER, OVERLAYS, CUES, VIDEO, AUDIO }

private data class TrackRow(val kind: RowKind, val index: Int, val top: Float, val height: Float, val sub: Int = 0)

private enum class Part { BODY, START, END }

private data class Hit(val id: Long, val row: TrackRow, val part: Part, val startMs: Double, val endMs: Double)

@Composable
private fun TimelineCanvas(model: EditorModel, modifier: Modifier, labels: TimelineLabels) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer(64)
    val touchSlop = LocalViewConfiguration.current.touchSlop
    var scrollY by remember { mutableFloatStateOf(0f) }
    val current by rememberUpdatedState(model.project)
    val rulerH = with(density) { 24.dp.toPx() }
    val videoH = with(density) { 52.dp.toPx() }
    val audioH = with(density) { 40.dp.toPx() }
    val barH = with(density) { 26.dp.toPx() }
    val gap = with(density) { 4.dp.toPx() }
    val handleW = with(density) { 14.dp.toPx() }
    val snapPx = with(density) { 10.dp.toPx() }
    val labelStyle = TextStyle(fontSize = 11.sp, color = colors.onSurface)
    val headerStyle = TextStyle(fontSize = 10.sp, color = colors.onSurfaceVariant)

    fun pxPerMs(): Float = with(density) { (model.zoom / 1_000.0).toFloat() * 1.dp.toPx() }

    fun overlayRows(project: EditorProject): Map<Long, Int> {
        val ends = ArrayList<Long>()
        return project.overlays.sortedBy { it.fromMs }.associate { overlay ->
            val sub = ends.indexOfFirst { it <= overlay.fromMs }.let { if (it < 0) ends.size.also { ends += overlay.toMs } else it.also { ends[it] = overlay.toMs } }
            overlay.id to sub
        }
    }

    fun rows(project: EditorProject): List<TrackRow> {
        val list = ArrayList<TrackRow>()
        var y = rulerH + gap - scrollY
        val overlaySubs = max(1, (overlayRows(project).values.maxOrNull() ?: -1) + 1)
        for (sub in 0 until overlaySubs) {
            list += TrackRow(RowKind.OVERLAYS, 0, y, barH, sub)
            y += barH + gap
        }
        list += TrackRow(RowKind.CUES, 0, y, barH)
        y += barH + gap
        for (index in project.videoTracks.indices.reversed()) {
            list += TrackRow(RowKind.VIDEO, index, y, videoH)
            y += videoH + gap
        }
        for (index in project.audioTracks.indices) {
            list += TrackRow(RowKind.AUDIO, index, y, audioH)
            y += audioH + gap
        }
        return list
    }

    fun contentHeight(project: EditorProject): Float = rows(project).lastOrNull()?.let { it.top + it.height + scrollY } ?: rulerH

    fun hitTest(project: EditorProject, position: Offset, width: Float): Hit? {
        if (position.y < rulerH) return null
        val row = rows(project).firstOrNull { position.y >= it.top && position.y < it.top + it.height } ?: return null
        val ppm = pxPerMs()
        val t = model.preview.positionMs + (position.x - width / 2f) / ppm
        val slop = handleW / ppm
        val items: List<Triple<Long, Double, Double>> = when (row.kind) {
            RowKind.VIDEO -> project.videoTracks[row.index].clips.mapNotNull { c -> project.videoPlacements[c.id]?.let { Triple(c.id, it.atMs, it.endMs) } }
            RowKind.AUDIO -> project.audioTracks[row.index].clips.mapNotNull { c -> project.audioPlacements[c.id]?.let { Triple(c.id, it.atMs, it.endMs) } }
            RowKind.OVERLAYS -> {
                val subs = overlayRows(project)
                project.overlays.filter { subs[it.id] == row.sub }.map { Triple(it.id, it.fromMs.toDouble(), it.toMs.toDouble()) }
            }
            RowKind.CUES -> project.subtitles.cues.map { Triple(it.id, it.startMs.toDouble(), it.endMs.toDouble()) }
            RowKind.RULER -> emptyList()
        }
        val selected = model.selected
        items.firstOrNull { it.first == selected }?.let { (id, start, end) ->
            if (abs(t - start) <= slop) return Hit(id, row, Part.START, start, end)
            if (abs(t - end) <= slop) return Hit(id, row, Part.END, start, end)
        }
        // A later video clip lies above the overlap of a transition. zh-CN: 转场重叠处后一段在上.
        return items.lastOrNull { t >= it.second && t < it.third }?.let { (id, start, end) -> Hit(id, row, Part.BODY, start, end) }
    }

    Canvas(
        modifier.pointerInput(model) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val width = size.width.toFloat()
                val project = current
                val hit = hitTest(project, down.position, width)
                val selectedHit = hit?.takeIf { it.id == model.selected }
                val startPlayhead = model.preview.positionMs
                val startScroll = scrollY
                var total = Offset.Zero
                var mode = 0 // 0 undecided, 1 scrub, 2 scroll, 3 move / trim, 4 zoom
                var position = down.position
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } >= 2) {
                        if (mode == 3) model.endGesture()
                        mode = 4
                        val zoom = event.calculateZoom()
                        if (zoom != 1f) model.zoom = (model.zoom * zoom).coerceIn(EditorModel.MIN_ZOOM, EditorModel.MAX_ZOOM)
                        event.changes.forEach { it.consume() }
                        continue
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                    if (!change.pressed) {
                        if (mode == 0) {
                            model.selected = hit?.id
                        } else if (mode == 3) {
                            val target = selectedHit
                            if (target != null && target.row.kind == RowKind.VIDEO && target.part == Part.BODY) {
                                val drop = rows(current).firstOrNull { position.y >= it.top && position.y < it.top + it.height }
                                if (drop != null && drop.kind == RowKind.VIDEO && drop.index != target.row.index) {
                                    model.gesture { EditOps.moveVideoToTrack(it, target.id, drop.index) }
                                }
                            }
                            model.endGesture()
                        } else if (mode == 1) {
                            model.preview.scrubbing = false
                            model.preview.seek(model.preview.positionMs)
                        }
                        break
                    }
                    total += change.positionChange()
                    position = change.position
                    if (mode == 0) {
                        if (abs(total.x) > touchSlop || abs(total.y) > touchSlop) {
                            mode = when {
                                selectedHit != null && abs(total.x) >= abs(total.y) -> 3
                                selectedHit != null && selectedHit.row.kind == RowKind.VIDEO && selectedHit.part == Part.BODY -> 3
                                abs(total.x) >= abs(total.y) -> 1
                                else -> 2
                            }
                            if (mode == 3) model.beginGesture()
                            if (mode == 1) {
                                model.preview.pause()
                                model.preview.scrubbing = true
                            }
                        } else continue
                    }
                    val ppm = pxPerMs()
                    when (mode) {
                        1 -> model.preview.seek(startPlayhead - total.x / ppm)
                        2 -> scrollY = (startScroll - total.y).coerceIn(0f, max(0f, contentHeight(current) - size.height + gap))
                        3 -> {
                            val target = selectedHit!!
                            val deltaMs = total.x / ppm
                            val targets = Snapping.targets(project, model.preview.positionMs, target.id)
                            val threshold = (snapPx / ppm).toDouble()
                            model.gesture { base -> drag(base, target, deltaMs.toDouble(), targets, threshold) }
                        }
                    }
                    change.consume()
                }
            }
        },
    ) {
        model.media.version.intValue
        val project = model.project
        val width = size.width
        val ppm = pxPerMs()
        val playhead = model.preview.positionMs
        fun x(ms: Double) = width / 2f + ((ms - playhead) * ppm).toFloat()
        val visibleFrom = playhead - width / 2f / ppm
        val visibleTo = playhead + width / 2f / ppm
        val rowList = rows(project)
        val overlaySubs = overlayRows(project)
        val radius = CornerRadius(6.dp.toPx())

        clipRect(0f, rulerH, width, size.height) {
            // Track backgrounds and the timeline end. zh-CN: 轨道背景与时间线结尾.
            val endX = x(project.durationUs / 1_000.0)
            rowList.forEach { row ->
                drawRect(colors.surfaceContainerHighest.copy(alpha = 0.35f), Offset(0f, row.top), Size(width, row.height))
            }
            if (endX < width) drawRect(colors.scrim.copy(alpha = 0.08f), Offset(max(endX, 0f), rulerH), Size(width - max(endX, 0f), size.height))

            rowList.forEach { row ->
                when (row.kind) {
                    RowKind.VIDEO -> {
                        val clips = project.videoTracks[row.index].clips
                        clips.forEachIndexed { clipIndex, clip ->
                            val placement = project.videoPlacements[clip.id] ?: return@forEachIndexed
                            if (placement.endMs < visibleFrom || placement.atMs > visibleTo) return@forEachIndexed
                            val left = x(placement.atMs)
                            val right = x(placement.endMs)
                            val rect = Rect(left, row.top, right, row.top + row.height)
                            drawRoundRect(colors.secondaryContainer, rect.topLeft, rect.size, radius)
                            clipRect(max(rect.left, 0f), rect.top, min(rect.right, width), rect.bottom) {
                                val tileW = row.height * 16f / 9f
                                val firstTile = max(0, floor((max(left, 0f) - left) / tileW).toInt())
                                val lastTile = ceil((min(right, width) - left) / tileW).toInt()
                                for (tile in firstTile until lastTile) {
                                    val tileX = left + tile * tileW
                                    val sourceMs = clip.startMs + ((tileX - left) / ppm) * clip.speed
                                    val image = model.media.thumbnail(clip.src, sourceMs.toLong(), row.height.toInt())
                                    if (image != null) {
                                        val scale = max(tileW / image.width, row.height / image.height)
                                        val w = image.width * scale
                                        val h = image.height * scale
                                        drawImage(image, dstOffset = IntOffset((tileX + (tileW - w) / 2f).toInt(), (row.top + (row.height - h) / 2f).toInt()),
                                            dstSize = IntSize(w.toInt(), h.toInt()))
                                    }
                                }
                                val label = buildString {
                                    append(File(clip.src).name)
                                    if (clip.speed != 1.0) append("  ").append(formatSpeed(clip.speed))
                                }
                                drawLabel(measurer, label, labelStyle.copy(color = Color.White), Offset(max(left, 0f) + 4.dp.toPx(), row.top + 2.dp.toPx()),
                                    (min(right, width) - max(left, 0f) - 8.dp.toPx()), Color.Black.copy(alpha = 0.45f))
                            }
                            val transition = clip.transition
                            if (clipIndex > 0 && transition != null) {
                                val side = transition.sideUs / 1_000.0
                                val from = if (transition.overlaps) placement.atMs else placement.atMs - side
                                val tx0 = x(from)
                                val tx1 = x(placement.atMs + side)
                                drawRect(colors.tertiary.copy(alpha = 0.35f), Offset(tx0, row.top), Size(max(tx1 - tx0, 2f), row.height))
                                diamond(Offset((tx0 + tx1) / 2f, row.top + row.height - 8.dp.toPx()), 6.dp.toPx(), colors.tertiary)
                            }
                            if (clip.id == model.selected) selection(rect, colors.primary, handleW, radius)
                        }
                        trackHeader(measurer, labels.video(row.index + 1), row, headerStyle, colors.surface)
                    }
                    RowKind.AUDIO -> {
                        val clips = project.audioTracks[row.index].clips
                        clips.forEach { clip ->
                            val placement = project.audioPlacements[clip.id] ?: return@forEach
                            if (placement.endMs < visibleFrom || placement.atMs > visibleTo) return@forEach
                            val left = x(placement.atMs)
                            val right = x(placement.endMs)
                            val rect = Rect(left, row.top, right, row.top + row.height)
                            drawRoundRect(colors.tertiaryContainer, rect.topLeft, rect.size, radius)
                            val peaks = model.media.waveform(clip.src)
                            if (peaks != null && peaks.isNotEmpty()) {
                                val mid = row.top + row.height / 2f
                                val step = 2.dp.toPx()
                                var px = max(left, 0f)
                                val end = min(right, width)
                                while (px < end) {
                                    val sourceMs = clip.startMs + (px - left) / ppm
                                    val bucket = (sourceMs / TimelineMedia.PEAK_MS).toInt()
                                    val level = (peaks.getOrElse(bucket) { 0f } * clip.volume.toFloat()).coerceIn(0f, 1f)
                                    val h = max(1f, level * (row.height - 6.dp.toPx()))
                                    drawRect(colors.onTertiaryContainer.copy(alpha = 0.7f), Offset(px, mid - h / 2f), Size(step * 0.6f, h))
                                    px += step
                                }
                            }
                            if (clip.fadeInMs > 0) drawLine(colors.onTertiaryContainer, Offset(left, rect.bottom), Offset(x(placement.atMs + clip.fadeInMs), rect.top), 1.5.dp.toPx())
                            if (clip.fadeOutMs > 0) drawLine(colors.onTertiaryContainer, Offset(x(placement.endMs - clip.fadeOutMs), rect.top), Offset(right, rect.bottom), 1.5.dp.toPx())
                            clipRect(max(rect.left, 0f), rect.top, min(rect.right, width), rect.bottom) {
                                drawLabel(measurer, File(clip.src).name, labelStyle.copy(color = colors.onTertiaryContainer), Offset(max(left, 0f) + 4.dp.toPx(), row.top + 2.dp.toPx()),
                                    (min(right, width) - max(left, 0f) - 8.dp.toPx()), null)
                            }
                            if (clip.id == model.selected) selection(rect, colors.primary, handleW, radius)
                        }
                        trackHeader(measurer, labels.audio(row.index + 1), row, headerStyle, colors.surface)
                    }
                    RowKind.OVERLAYS -> {
                        project.overlays.filter { overlaySubs[it.id] == row.sub }.forEach { overlay ->
                            if (overlay.toMs < visibleFrom || overlay.fromMs > visibleTo) return@forEach
                            val rect = Rect(x(overlay.fromMs.toDouble()), row.top, x(overlay.toMs.toDouble()), row.top + row.height)
                            drawRoundRect(colors.primaryContainer, rect.topLeft, rect.size, radius)
                            val text = if (overlay.type == OverlayModel.IMAGE) File(overlay.src.orEmpty()).name else overlay.text.orEmpty().replace('\n', ' ')
                            clipRect(max(rect.left, 0f), rect.top, min(rect.right, width), rect.bottom) {
                                drawLabel(measurer, text, labelStyle.copy(color = colors.onPrimaryContainer), Offset(max(rect.left, 0f) + 4.dp.toPx(), row.top + 4.dp.toPx()),
                                    min(rect.right, width) - max(rect.left, 0f) - 8.dp.toPx(), null)
                            }
                            if (overlay.id == model.selected) selection(rect, colors.primary, handleW, radius)
                        }
                        if (row.sub == 0) trackHeader(measurer, labels.overlays, row, headerStyle, colors.surface)
                    }
                    RowKind.CUES -> {
                        project.subtitles.cues.forEach { cue ->
                            if (cue.endMs < visibleFrom || cue.startMs > visibleTo) return@forEach
                            val rect = Rect(x(cue.startMs.toDouble()), row.top, x(cue.endMs.toDouble()), row.top + row.height)
                            drawRoundRect(colors.surfaceVariant, rect.topLeft, rect.size, radius)
                            drawRoundRect(colors.outline, rect.topLeft, rect.size, radius, style = Stroke(1.dp.toPx()))
                            clipRect(max(rect.left, 0f), rect.top, min(rect.right, width), rect.bottom) {
                                drawLabel(measurer, cue.text.replace('\n', ' '), labelStyle.copy(color = colors.onSurfaceVariant),
                                    Offset(max(rect.left, 0f) + 4.dp.toPx(), row.top + 4.dp.toPx()), min(rect.right, width) - max(rect.left, 0f) - 8.dp.toPx(), null)
                            }
                            if (cue.id == model.selected) selection(rect, colors.primary, handleW, radius)
                        }
                        trackHeader(measurer, labels.subtitles, row, headerStyle, colors.surface)
                    }
                    RowKind.RULER -> Unit
                }
            }
        }

        // Ruler. zh-CN: 标尺.
        drawRect(colors.surface, Offset.Zero, Size(width, rulerH))
        val stepMs = RULER_STEPS_MS.firstOrNull { it * ppm >= 72.dp.toPx() } ?: RULER_STEPS_MS.last()
        var tick = floor(max(0.0, visibleFrom) / stepMs).toLong() * stepMs
        while (tick <= visibleTo) {
            val tx = x(tick.toDouble())
            drawLine(colors.outline, Offset(tx, rulerH - 8.dp.toPx()), Offset(tx, rulerH), 1.dp.toPx())
            drawLabel(measurer, formatRuler(tick, stepMs), headerStyle, Offset(tx + 3.dp.toPx(), 2.dp.toPx()), 64.dp.toPx(), null)
            val minor = stepMs / 4
            for (i in 1..3) {
                val mx = x((tick + minor * i).toDouble())
                drawLine(colors.outlineVariant, Offset(mx, rulerH - 4.dp.toPx()), Offset(mx, rulerH), 1.dp.toPx())
            }
            tick += stepMs
        }
        drawLine(colors.outlineVariant, Offset(0f, rulerH), Offset(width, rulerH), 1.dp.toPx())

        // Playhead. zh-CN: 播放头.
        val cx = width / 2f
        drawLine(colors.primary, Offset(cx, 0f), Offset(cx, size.height), 2.dp.toPx())
        val head = Path().apply {
            moveTo(cx - 6.dp.toPx(), 0f)
            lineTo(cx + 6.dp.toPx(), 0f)
            lineTo(cx, 8.dp.toPx())
            close()
        }
        drawPath(head, colors.primary)
    }
}

/** One drag step of the selected item from the gesture start state [base]. zh-CN: 从手势起始状态 [base] 拖动已选条目的一步. */
private fun drag(base: EditorProject, hit: Hit, deltaMs: Double, targets: List<Double>, threshold: Double): EditorProject {
    val id = hit.id
    return when (hit.row.kind) {
        RowKind.VIDEO -> {
            val clip = base.videoClip(id)?.second ?: return base
            val placement = base.videoPlacements[id] ?: return base
            when (hit.part) {
                Part.BODY -> {
                    val at = Snapping.snap(placement.atMs + deltaMs, targets, threshold)
                    EditOps.moveVideo(base, id, (at - placement.atMs).roundToLong())
                }
                Part.START -> EditOps.trimVideoStart(base, id, clip.startMs + (deltaMs * clip.speed).roundToLong())
                Part.END -> {
                    val end = Snapping.snap(placement.endMs + deltaMs, targets, threshold)
                    EditOps.trimVideoEnd(base, id, clip.startMs + ((end - placement.atMs) * clip.speed).roundToLong())
                }
            }
        }
        RowKind.AUDIO -> {
            val clip = base.audioClip(id)?.second ?: return base
            val placement = base.audioPlacements[id] ?: return base
            when (hit.part) {
                Part.BODY -> {
                    val at = Snapping.snap(placement.atMs + deltaMs, targets, threshold)
                    EditOps.moveAudio(base, id, (at - placement.atMs).roundToLong())
                }
                Part.START -> EditOps.trimAudioStart(base, id, clip.startMs + deltaMs.roundToLong())
                Part.END -> {
                    val end = Snapping.snap(placement.endMs + deltaMs, targets, threshold)
                    EditOps.trimAudioEnd(base, id, clip.startMs + (end - placement.atMs).roundToLong())
                }
            }
        }
        RowKind.OVERLAYS -> {
            val overlay = base.overlays.firstOrNull { it.id == id } ?: return base
            when (hit.part) {
                Part.BODY -> {
                    val from = Snapping.snap(overlay.fromMs + deltaMs, targets, threshold)
                    EditOps.moveOverlay(base, id, (from - overlay.fromMs).roundToLong())
                }
                Part.START -> EditOps.updateOverlay(base, id) { it.copy(fromMs = Snapping.snap(overlay.fromMs + deltaMs, targets, threshold).roundToLong().coerceAtMost(overlay.toMs - EditOps.MIN_CLIP_MS)) }
                Part.END -> EditOps.updateOverlay(base, id) { it.copy(toMs = Snapping.snap(overlay.toMs + deltaMs, targets, threshold).roundToLong()) }
            }
        }
        RowKind.CUES -> {
            val cue = base.subtitles.cues.firstOrNull { it.id == id } ?: return base
            when (hit.part) {
                Part.BODY -> {
                    val start = Snapping.snap(cue.startMs + deltaMs, targets, threshold)
                    EditOps.moveCue(base, id, (start - cue.startMs).roundToLong())
                }
                Part.START -> EditOps.updateCue(base, id, startMs = Snapping.snap(cue.startMs + deltaMs, targets, threshold).roundToLong().coerceAtMost(cue.endMs - EditOps.MIN_CLIP_MS))
                Part.END -> EditOps.updateCue(base, id, endMs = Snapping.snap(cue.endMs + deltaMs, targets, threshold).roundToLong())
            }
        }
        RowKind.RULER -> base
    }
}

private val RULER_STEPS_MS = longArrayOf(100, 200, 500, 1_000, 2_000, 5_000, 10_000, 15_000, 30_000, 60_000, 120_000, 300_000, 600_000)

private fun formatRuler(ms: Long, stepMs: Long): String {
    val minutes = ms / 60_000
    val seconds = (ms % 60_000) / 1_000
    return if (stepMs < 1_000) String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, (ms % 1_000) / 100)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}

internal fun formatSpeed(speed: Double): String = String.format(Locale.ROOT, "%sx", if (speed == Math.rint(speed)) speed.toLong().toString() else "%.2f".format(Locale.ROOT, speed).trimEnd('0'))

private fun DrawScope.selection(rect: Rect, color: Color, handleW: Float, radius: CornerRadius) {
    drawRoundRect(color, rect.topLeft, rect.size, radius, style = Stroke(2.dp.toPx()))
    val w = handleW * 0.6f
    drawRoundRect(color, Offset(rect.left, rect.top), Size(w, rect.height), radius)
    drawRoundRect(color, Offset(rect.right - w, rect.top), Size(w, rect.height), radius)
    val grip = Color.White.copy(alpha = 0.9f)
    drawLine(grip, Offset(rect.left + w / 2f, rect.top + rect.height * 0.3f), Offset(rect.left + w / 2f, rect.top + rect.height * 0.7f), 1.5.dp.toPx())
    drawLine(grip, Offset(rect.right - w / 2f, rect.top + rect.height * 0.3f), Offset(rect.right - w / 2f, rect.top + rect.height * 0.7f), 1.5.dp.toPx())
}

private fun DrawScope.diamond(center: Offset, half: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half, center.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawLabel(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    style: TextStyle,
    topLeft: Offset,
    maxWidth: Float,
    background: Color?,
) {
    if (maxWidth < 12.dp.toPx() || text.isEmpty()) return
    val layout = measurer.measure(text, style, overflow = TextOverflow.Ellipsis, maxLines = 1, constraints = Constraints(maxWidth = maxWidth.toInt()))
    if (background != null) {
        drawRoundRect(background, topLeft - Offset(2.dp.toPx(), 0f), Size(layout.size.width + 4.dp.toPx(), layout.size.height.toFloat()), CornerRadius(3.dp.toPx()))
    }
    drawText(layout, topLeft = topLeft)
}

private fun DrawScope.trackHeader(measurer: androidx.compose.ui.text.TextMeasurer, text: String, row: TrackRow, style: TextStyle, background: Color) {
    val layout = measurer.measure(text, style, maxLines = 1)
    val pad = 4.dp.toPx()
    val topLeft = Offset(pad, row.top + row.height - layout.size.height - pad)
    drawRoundRect(background.copy(alpha = 0.85f), topLeft - Offset(pad / 2f, 1f), Size(layout.size.width + pad, layout.size.height + 2f), CornerRadius(4.dp.toPx()))
    drawText(layout, topLeft = topLeft)
}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.Choreographer
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionType
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.CueModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditorProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.OverlayModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.Placement
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.VideoClipModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The live preview of the timeline editor, the self-written preview (b) of roadmap appendix D Q3: the spike showed that
 * media3 1.10.1 `CompositionPlayer` stops on the export's composition (the clock sequence, two lanes per track with
 * overlapping transitions), so the preview plays the clips with one [ExoPlayer] per lane (even and odd clips of a video
 * track, as the export splits them, plus one per audio track) on a shared clock, and approximates the compositor in
 * Compose: alpha of the upper lane for `dissolve` / `zoom` / `blur`, offsets for `slide`, scale for `zoom`, a black
 * veil for `fade`, a clip rectangle for `wipe` and a blur (API 31+) for `blur`, with the same progress formulas as
 * `ComposeCompositorSettings` and `TransitionEffect`. Overlays and cues are drawn by Compose from the same model. The
 * exported file stays the reference; the preview trades exactness for responsiveness.
 * zh-CN: 时间线编辑器的实时预览, 即路线图附录 D Q3 的自写预览 (b): spike 表明 media3 1.10.1 的 `CompositionPlayer` 在导出所用的合成结构
 * (时钟序列, 带重叠转场的轨道分成两个车道) 上会停住, 因此预览按车道各用一个 [ExoPlayer] (视频轨的奇数段与偶数段, 与导出的拆分相同,
 * 另加每条音频轨一个) 在共用时钟下播放片段, 并在 Compose 中近似合成器: `dissolve` / `zoom` / `blur` 调上层车道的透明度, `slide` 平移,
 * `zoom` 缩放, `fade` 用黑色遮罩, `wipe` 用裁切矩形, `blur` 用模糊 (API 31+), 进度公式与 `ComposeCompositorSettings` 和
 * `TransitionEffect` 相同. 叠加物与字幕由 Compose 按同一模型绘制. 以导出的文件为准; 预览以响应速度换取精确度.
 */
internal class TimelinePreview(context: Context) {

    private val context = context.applicationContext

    var project: EditorProject = EditorProject()

    /** Clock position in milliseconds. zh-CN: 时钟位置 (毫秒). */
    var positionMs by mutableDoubleStateOf(0.0)
        private set

    var playing by mutableStateOf(false)
        private set

    /** True while the user drags the playhead: seeks favor speed. zh-CN: 用户拖动播放头时为真: 跳转以速度优先. */
    var scrubbing = false

    private val lanes = LinkedHashMap<String, Lane>()
    internal val visuals = mutableStateMapOf<String, LaneVisual>()
    private var lastFrameNanos = 0L
    private var stallStartedAt = 0L
    private var released = false

    // Playback runs on display frames of its own, not on the composition's frame clock. zh-CN: 播放使用自己的显示帧回调, 不用组合的帧时钟.
    private val frames = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!playing || released) return
            frame(frameTimeNanos)
            if (playing) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun play() {
        if (project.durationUs <= 0L || released) return
        if (positionMs * 1_000.0 >= project.durationUs - 50_000) positionMs = 0.0
        lastFrameNanos = 0L
        playing = true
        Choreographer.getInstance().removeFrameCallback(frames)
        Choreographer.getInstance().postFrameCallback(frames)
    }

    fun pause() {
        playing = false
        Choreographer.getInstance().removeFrameCallback(frames)
        lanes.values.forEach { it.player.playWhenReady = false }
    }

    fun toggle() = if (playing) pause() else play()

    fun seek(ms: Double) {
        positionMs = ms.coerceIn(0.0, project.durationUs / 1_000.0)
        if (!playing) sync()
    }

    /** One display frame: advances the clock unless a starting clip still buffers, then drives the lanes. zh-CN: 一帧. */
    fun frame(nowNanos: Long) {
        if (released) return
        if (playing) {
            val elapsed = if (lastFrameNanos == 0L) 0.0 else (nowNanos - lastFrameNanos) / 1_000_000.0
            val stalled = lanes.values.any { it.active && it.player.playbackState == Player.STATE_BUFFERING }
            val now = SystemClock.elapsedRealtime()
            if (stalled && stallStartedAt == 0L) stallStartedAt = now
            if (!stalled) stallStartedAt = 0L
            // Hold the clock while a clip that just started buffers (at most a second), so sound and picture stay together.
            // zh-CN: 刚开始的片段在缓冲时暂停时钟 (至多一秒), 使声音与画面保持同步.
            if (!stalled || now - stallStartedAt > MAX_STALL_MS) positionMs += elapsed.coerceAtMost(MAX_FRAME_STEP_MS)
            val end = project.durationUs / 1_000.0
            if (positionMs >= end) {
                positionMs = end
                pause()
            }
        }
        lastFrameNanos = nowNanos
        sync()
    }

    fun release() {
        released = true
        playing = false
        Choreographer.getInstance().removeFrameCallback(frames)
        lanes.values.forEach { it.release() }
        lanes.clear()
        visuals.clear()
    }

    /** Drives every lane to the clock position. zh-CN: 让每个车道对齐时钟位置. */
    fun sync() {
        if (released) return
        val project = project
        val tUs = (positionMs * 1_000.0).toLong()
        val used = HashSet<String>()
        project.videoTracks.forEachIndexed { trackIndex, track ->
            for (laneIndex in 0..1) {
                val laneClips = track.clips.withIndex().filter { it.index % 2 == laneIndex }
                if (laneClips.isEmpty()) continue
                val key = "v${track.id}:$laneIndex"
                used += key
                val lane = lanes.getOrPut(key) { Lane() }
                val active = laneClips.firstOrNull { (_, clip) -> project.videoPlacements[clip.id]?.let { tUs >= it.atUs && tUs < it.endUs } == true }
                if (active == null) {
                    val next = laneClips.firstOrNull { (_, clip) -> project.videoPlacements[clip.id]?.let { it.atUs > tUs && it.atUs - tUs < PRELOAD_US } == true }
                    idle(lane, next?.value?.let { it.src to it.startMs.toDouble() })
                    visuals[key] = LaneVisual(trackIndex, laneIndex, null)
                } else {
                    val (index, clip) = active
                    val placement = project.videoPlacements.getValue(clip.id)
                    val sourceMs = clip.startMs + (tUs - placement.atUs) / 1_000.0 * clip.speed
                    drive(lane, clip.id, clip.src, sourceMs, clip.speed, if (clip.hasAudio) clip.volume else 0.0)
                    visuals[key] = visualOf(track.clips, index, placement, tUs, top = laneIndex == 1).copy(track = trackIndex, lane = laneIndex)
                }
            }
        }
        project.audioTracks.forEach { track ->
            if (track.clips.isEmpty()) return@forEach
            val key = "a${track.id}"
            used += key
            val lane = lanes.getOrPut(key) { Lane() }
            val active = track.clips.firstOrNull { clip -> project.audioPlacements[clip.id]?.let { tUs >= it.atUs && tUs < it.endUs } == true }
            if (active == null) {
                val next = track.clips.firstOrNull { clip -> project.audioPlacements[clip.id]?.let { it.atUs > tUs && it.atUs - tUs < PRELOAD_US } == true }
                idle(lane, next?.let { it.src to it.startMs.toDouble() })
            } else {
                val placement = project.audioPlacements.getValue(active.id)
                val localMs = (tUs - placement.atUs) / 1_000.0
                val lengthMs = placement.lengthUs / 1_000.0
                var gain = active.volume
                if (active.fadeInMs > 0 && localMs < active.fadeInMs) gain *= localMs / active.fadeInMs
                if (active.fadeOutMs > 0 && lengthMs - localMs < active.fadeOutMs) gain *= (lengthMs - localMs) / active.fadeOutMs
                drive(lane, active.id, active.src, active.startMs + localMs, 1.0, gain)
            }
        }
        (lanes.keys - used).toList().forEach { key ->
            lanes.remove(key)?.release()
            visuals.remove(key)
        }
    }

    private fun idle(lane: Lane, next: Pair<String, Double>?) {
        lane.active = false
        lane.clipId = -1L
        lane.player.playWhenReady = false
        lane.player.volume = 0f
        // Prepare the next clip of the lane while the other lane plays. zh-CN: 另一车道播放时准备本车道的下一段.
        if (next != null && (lane.src != next.first || abs(lane.requestedMs - next.second) > SEEK_TOLERANCE_PAUSED_MS)) {
            source(lane, next.first)
            seek(lane, next.second)
        }
    }

    private fun drive(lane: Lane, clipId: Long, src: String, sourceMs: Double, speed: Double, gain: Double) {
        lane.active = true
        val player = lane.player
        val changed = lane.clipId != clipId
        lane.clipId = clipId
        source(lane, src)
        if (player.playbackParameters.speed != speed.toFloat()) player.playbackParameters = PlaybackParameters(speed.toFloat())
        // ExoPlayer cannot amplify: gains above 1 play at full volume. zh-CN: ExoPlayer 不能放大: 大于 1 的增益按满音量播放.
        player.volume = gain.coerceIn(0.0, 1.0).toFloat()
        val now = SystemClock.elapsedRealtime()
        if (playing) {
            val drift = abs(player.currentPosition - sourceMs)
            when {
                changed && abs(lane.requestedMs - sourceMs) > SEEK_TOLERANCE_PAUSED_MS -> seek(lane, sourceMs)
                !player.playWhenReady && drift > SEEK_TOLERANCE_PAUSED_MS -> seek(lane, sourceMs)
                player.playbackState == Player.STATE_READY && drift > DRIFT_TOLERANCE_MS && now - lane.seekedAt > RESEEK_INTERVAL_MS -> seek(lane, sourceMs)
            }
            player.playWhenReady = true
        } else {
            player.playWhenReady = false
            if (abs(lane.requestedMs - sourceMs) > SEEK_TOLERANCE_PAUSED_MS && (!scrubbing || now - lane.seekedAt > SCRUB_INTERVAL_MS)) seek(lane, sourceMs)
        }
    }

    private fun source(lane: Lane, src: String) {
        if (lane.src == src) return
        lane.src = src
        lane.requestedMs = -1.0
        lane.player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(src))))
        lane.player.prepare()
    }

    private fun seek(lane: Lane, sourceMs: Double) {
        lane.requestedMs = sourceMs
        lane.seekedAt = SystemClock.elapsedRealtime()
        lane.player.seekTo(sourceMs.roundToInt().toLong().coerceAtLeast(0L))
    }

    internal fun attach(key: String, view: TextureView) {
        lanes[key]?.let { lane ->
            if (lane.view !== view) {
                lane.view = view
                lane.player.setVideoTextureView(view)
            }
        }
    }

    internal fun detach(key: String, view: TextureView) {
        lanes[key]?.let { lane ->
            if (lane.view === view) {
                lane.player.clearVideoTextureView(view)
                lane.view = null
            }
        }
    }

    private inner class Lane {
        val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
        }
        var src: String? = null
        var clipId = -1L
        var active = false
        var requestedMs = -1.0
        var seekedAt = 0L
        var view: TextureView? = null

        fun release() {
            view?.let { player.clearVideoTextureView(it) }
            player.release()
        }
    }

    internal companion object {
        const val PRELOAD_US = 1_500_000L
        const val SEEK_TOLERANCE_PAUSED_MS = 15.0
        const val DRIFT_TOLERANCE_MS = 150.0
        const val RESEEK_INTERVAL_MS = 600L
        const val SCRUB_INTERVAL_MS = 80L
        const val MAX_STALL_MS = 1_000L
        const val MAX_FRAME_STEP_MS = 100.0
        const val ZOOM_FROM = 0.5f
        const val MAX_BLUR_FRACTION = 0.05f

        /**
         * The look of a clip at [tUs] inside its transitions, as the export computes it: `into` is the transition into
         * the clip, `out` the one into the next clip; [top] says the clip is in the upper lane.
         * zh-CN: 片段在 [tUs] 时处于转场中的外观, 与导出的计算相同: `into` 为进入本段的转场, `out` 为进入下一段的转场; [top] 表示本段位于上层车道.
         */
        fun visualOf(clips: List<VideoClipModel>, index: Int, placement: Placement, tUs: Long, top: Boolean): LaneVisual {
            val clip = clips[index]
            var visual = LaneVisual(0, 0, clip)
            val into = clip.transition?.takeIf { index > 0 && it.kind != TransitionType.CUT }
            val out = clips.getOrNull(index + 1)?.transition?.takeIf { it.kind != TransitionType.CUT }
            if (into != null && tUs - placement.atUs < into.sideUs) {
                visual = apply(visual, into.kind, into.direction, progress(tUs - placement.atUs, into.sideUs), incoming = true, top = top)
            }
            if (out != null) {
                val begin = placement.endUs - out.sideUs
                if (tUs >= begin) visual = apply(visual, out.kind, out.direction, progress(tUs - begin, out.sideUs), incoming = false, top = top)
            }
            return visual
        }

        private fun apply(visual: LaneVisual, type: TransitionType, direction: String?, p: Float, incoming: Boolean, top: Boolean): LaneVisual {
            var v = visual
            if ((type == TransitionType.DISSOLVE || type == TransitionType.ZOOM || type == TransitionType.BLUR) && top) {
                v = v.copy(alpha = v.alpha * if (incoming) p else 1f - p)
            }
            when (type) {
                TransitionType.FADE -> v = v.copy(brightness = v.brightness * if (incoming) p else 1f - p)
                TransitionType.ZOOM -> v = v.copy(scale = if (incoming) ZOOM_FROM + (1f - ZOOM_FROM) * p else 1f + (1f - ZOOM_FROM) * p)
                TransitionType.SLIDE -> {
                    val (vx, vy) = slideVector(direction)
                    val distance = if (incoming) 1f - p else -p
                    v = v.copy(offsetX = vx * distance, offsetY = vy * distance)
                }
                TransitionType.BLUR -> v = v.copy(blur = max(v.blur, if (incoming) 1f - p else p))
                TransitionType.WIPE -> if (top) v = v.copy(wipe = Wipe(direction ?: "left", p, keepIncoming = incoming))
                else -> Unit
            }
            return v
        }

        /** Where the incoming clip starts, in frames, screen y down. zh-CN: 入场片段的起始位置 (以帧为单位, 屏幕 y 向下). */
        fun slideVector(direction: String?): Pair<Float, Float> = when (direction) {
            "right" -> -1f to 0f
            "up" -> 0f to 1f
            "down" -> 0f to -1f
            else -> 1f to 0f
        }

        fun progress(elapsedUs: Long, spanUs: Long): Float = (elapsedUs.toDouble() / spanUs.coerceAtLeast(1L)).coerceIn(0.0, 1.0).toFloat()

        /** The output frame size: the project size, else the first clip after its rotation. zh-CN: 输出画面尺寸. */
        fun frameSize(project: EditorProject): Pair<Int, Int> {
            val first = project.videoTracks.firstNotNullOfOrNull { it.clips.firstOrNull() }
            val (w, h) = if (first == null || first.width <= 0 || first.height <= 0) 1280 to 720
            else if (first.rotate % 180 != 0) first.height to first.width else first.width to first.height
            return when {
                project.width != null && project.height != null -> project.width to project.height
                project.width != null -> project.width to max(2, (project.width.toLong() * h / w).toInt())
                project.height != null -> max(2, (project.height.toLong() * w / h).toInt()) to project.height
                else -> w to h
            }
        }
    }

}

/**
 * The look of one lane at the clock position: [clip] null hides it. Offsets are in frames.
 * zh-CN: 一个车道在时钟位置的外观: [clip] 为 null 时隐藏. 偏移以帧为单位.
 */
internal data class LaneVisual(
    val track: Int,
    val lane: Int,
    val clip: VideoClipModel?,
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val brightness: Float = 1f,
    val blur: Float = 0f,
    val wipe: Wipe? = null,
)

/**
 * A wipe on the upper clip: the incoming clip shows on the side the transition comes from, growing with [progress];
 * [keepIncoming] says this clip is the incoming one.
 * zh-CN: 上层片段上的擦除: 入场片段显示在转场到来的一侧, 随 [progress] 增大; [keepIncoming] 表示本段是入场片段.
 */
internal data class Wipe(val direction: String, val progress: Float, val keepIncoming: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = progress
        val incoming = when (direction) {
            "right" -> Rect(0f, 0f, size.width * p, size.height)
            "up" -> Rect(0f, size.height * (1f - p), size.width, size.height)
            "down" -> Rect(0f, 0f, size.width, size.height * p)
            else -> Rect(size.width * (1f - p), 0f, size.width, size.height)
        }
        if (keepIncoming) return Outline.Rectangle(incoming)
        val outgoing = when (direction) {
            "right" -> Rect(size.width * p, 0f, size.width, size.height)
            "up" -> Rect(0f, 0f, size.width, size.height * (1f - p))
            "down" -> Rect(0f, size.height * p, size.width, size.height)
            else -> Rect(0f, 0f, size.width * (1f - p), size.height)
        }
        return Outline.Rectangle(outgoing)
    }
}

/**
 * The preview frame: tracks from the bottom up, overlays, then the cue of the clock position, letterboxed in the
 * available space; the activity pauses playback when it stops.
 * zh-CN: 预览画面: 轨道自下而上, 然后是叠加物与时钟位置的字幕, 在可用空间中按比例居中; 界面停止时由 Activity 暂停播放.
 */
@Composable
internal fun TimelinePreviewFrame(
    preview: TimelinePreview,
    project: EditorProject,
    modifier: Modifier = Modifier,
    overlayModifier: (OverlayModel, Float) -> Modifier = { _, _ -> Modifier },
) {
    preview.project = project
    val (frameW, frameH) = TimelinePreview.frameSize(project)
    val background = parseColor(project.background, Color.Black)
    // The picture has no reading direction: overlay positions and slides stay as exported in right-to-left languages.
    // zh-CN: 画面没有阅读方向: 在从右到左的语言中, 叠加物位置与推移仍与导出一致.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            val ratio = frameW.toFloat() / frameH
            val boxW = constraints.maxWidth.toFloat()
            val boxH = constraints.maxHeight.toFloat()
            val (w, h) = if (boxW / boxH > ratio) boxH * ratio to boxH else boxW to boxW / ratio
            val density = LocalDensity.current
            val pxScale = w / frameW
            with(density) {
                Box(Modifier.requiredSize(w.toDp(), h.toDp()).clipToBounds().background(background)) {
                    val visuals = preview.visuals
                    project.videoTracks.forEachIndexed { trackIndex, track ->
                        for (laneIndex in 0..1) {
                            if (track.clips.size <= laneIndex) continue
                            val key = "v${track.id}:$laneIndex"
                            LaneView(preview, key, visuals[key], w, h, frameW, frameH)
                        }
                    }
                    val t = preview.positionMs
                    project.overlays.forEach { overlay ->
                        if (t >= overlay.fromMs && t < overlay.toMs) OverlayView(overlay, pxScale, overlayModifier(overlay, pxScale))
                    }
                    project.subtitles.cues.firstOrNull { t >= it.startMs && t < it.endMs }?.let { CueView(it, project, pxScale, frameH) }
                }
            }
        }
    }
}

@Composable
private fun LaneView(preview: TimelinePreview, key: String, visual: LaneVisual?, w: Float, h: Float, frameW: Int, frameH: Int) {
    val clip = visual?.clip
    val density = LocalDensity.current
    // The content box: the clip fitted into the frame after its rotation. zh-CN: 内容框: 片段按旋转后的尺寸适配到画面.
    val sourceW = (clip?.width?.takeIf { it > 0 } ?: frameW).toFloat()
    val sourceH = (clip?.height?.takeIf { it > 0 } ?: frameH).toFloat()
    val rotated = (clip?.rotate ?: 0) % 180 != 0
    val footW = if (rotated) sourceH else sourceW
    val footH = if (rotated) sourceW else sourceH
    val (boxW, boxH) = when (clip?.fit) {
        "stretch" -> if (rotated) h to w else w to h
        "cover" -> max(w / footW, h / footH).let { sourceW * it to sourceH * it }
        else -> min(w / footW, h / footH).let { sourceW * it to sourceH * it }
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            alpha = if (clip == null) 0f else visual.alpha
            val wipe = visual?.wipe
            if (wipe != null) {
                shape = wipe
                this.clip = true
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        with(density) {
            Box(
                Modifier.wrapContentSize(unbounded = true).requiredSize(boxW.toDp(), boxH.toDp()).graphicsLayer {
                    if (clip == null) return@graphicsLayer
                    val s = (clip.scale * visual.scale).toFloat()
                    scaleX = s * if (clip.flip == "horizontal" || clip.flip == "both") -1f else 1f
                    scaleY = s * if (clip.flip == "vertical" || clip.flip == "both") -1f else 1f
                    rotationZ = clip.rotate.toFloat()
                    translationX = visual.offsetX * w
                    translationY = visual.offsetY * h
                    if (visual.blur > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val radius = visual.blur * min(w, h) * TimelinePreview.MAX_BLUR_FRACTION
                        if (radius >= 0.5f) renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    }
                },
            ) {
                AndroidView(
                    factory = { context -> TextureView(context).also { preview.attach(key, it) } },
                    update = { view -> preview.attach(key, view) },
                    onRelease = { view -> preview.detach(key, view) },
                    modifier = Modifier.fillMaxSize(),
                )
                if (clip != null && visual.brightness < 1f) {
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - visual.brightness }.background(Color.Black))
                }
            }
        }
    }
}

@Composable
private fun OverlayView(overlay: OverlayModel, pxScale: Float, modifier: Modifier) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cx = constraints.maxWidth * (overlay.x / 100.0).toFloat()
            val cy = constraints.maxHeight * (overlay.y / 100.0).toFloat()
            var size by remember(overlay.id) { mutableStateOf(IntSize.Zero) }
            Box(
                modifier
                    .offset { IntOffset((cx - size.width / 2f).roundToInt(), (cy - size.height / 2f).roundToInt()) }
                    .onSizeChanged { size = it }
                    .graphicsLayer { alpha = overlay.alpha.toFloat() },
            ) {
                if (overlay.type == OverlayModel.IMAGE) {
                    val src = overlay.src
                    val bitmap = if (src == null) null else rememberImage(src)
                    if (bitmap != null) {
                        val w = bitmap.width * overlay.scale.toFloat() * pxScale * imageScaleOf(src!!, bitmap)
                        val h = bitmap.height * overlay.scale.toFloat() * pxScale * imageScaleOf(src, bitmap)
                        with(density) { Image(bitmap, null, Modifier.requiredSize(w.toDp(), h.toDp()), contentScale = ContentScale.FillBounds) }
                    }
                } else {
                    val fontPx = (overlay.fontSize * overlay.scale).toFloat() * pxScale
                    val fontSp = with(density) { fontPx.toSp() }
                    val color = parseColor(overlay.color, Color.White)
                    val text = overlay.text.orEmpty()
                    Box {
                        overlay.stroke?.let { stroke ->
                            Text(text, style = TextStyle(fontSize = fontSp, color = parseColor(stroke, Color.Black),
                                drawStyle = Stroke(width = fontPx / 16f * 2f), textAlign = TextAlign.Center))
                        }
                        Text(text, style = TextStyle(fontSize = fontSp, color = color, textAlign = TextAlign.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun CueView(cue: CueModel, project: EditorProject, pxScale: Float, frameH: Int) {
    val style = project.subtitles
    val density = LocalDensity.current
    val fontPx = (style.fontSize ?: (frameH / 18.0)).toFloat() * pxScale
    val fontSp = with(density) { fontPx.toSp() }
    val alignment = when (style.align) {
        "top" -> Alignment.TopCenter
        "center" -> Alignment.Center
        else -> Alignment.BottomCenter
    }
    val margin = with(density) { (frameH * 0.05f * pxScale).toDp() }
    val text = plainCueText(cue.text)
    Box(Modifier.fillMaxSize().padding(vertical = margin, horizontal = margin), contentAlignment = alignment) {
        Box(Modifier.then(style.background?.let { Modifier.background(parseColor(it, Color.Transparent)) } ?: Modifier)) {
            style.stroke?.let { stroke ->
                Text(text, style = TextStyle(fontSize = fontSp, color = parseColor(stroke, Color.Black),
                    drawStyle = Stroke(width = fontPx / 8f * 2f), textAlign = TextAlign.Center))
            }
            Text(text, style = TextStyle(fontSize = fontSp, color = parseColor(style.color, Color.White), textAlign = TextAlign.Center))
        }
    }
}

/** Plain text of a cue: SRT tags and ASS overrides removed, `\N` as a line break. zh-CN: cue 的纯文本. */
internal fun plainCueText(text: String): String = text
    .replace(Regex("\\{[^}]*\\}"), "")
    .replace(Regex("</?[a-zA-Z][^>]*>"), "")
    .replace("\\N", "\n").replace("\\n", "\n")

/** `#RGB`, `#RRGGBB` or `#AARRGGBB`; anything else is [fallback]. zh-CN: 颜色解析, 其他写法取 [fallback]. */
internal fun parseColor(value: String?, fallback: Color): Color {
    val text = value?.trim() ?: return fallback
    return runCatching {
        val hex = text.removePrefix("#")
        val expanded = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        Color("#$expanded".toColorInt())
    }.getOrDefault(fallback)
}

private val imageScales = HashMap<String, Float>()

/** Decoded images are at most [MAX_IMAGE_SIDE]; this restores the size in source pixels. zh-CN: 解码图片的缩小比例补偿. */
private fun imageScaleOf(src: String, bitmap: ImageBitmap): Float = imageScales[src] ?: 1f

private const val MAX_IMAGE_SIDE = 1024

@Composable
private fun rememberImage(src: String): ImageBitmap? {
    val state = produceState<ImageBitmap?>(null, src) {
        value = withContext(Dispatchers.IO) { decodeImage(src) }
    }
    return state.value
}

private fun decodeImage(src: String): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(src, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_SIDE) sample *= 2
    val bitmap: Bitmap = BitmapFactory.decodeFile(src, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    synchronized(imageScales) { imageScales[src] = bounds.outWidth.toFloat() / bitmap.width }
    bitmap.asImageBitmap()
}.getOrNull()

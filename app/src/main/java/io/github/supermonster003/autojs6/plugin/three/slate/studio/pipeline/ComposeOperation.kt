@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaCodec
import android.net.Uri
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.createBitmap
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioEncoding
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioTargets
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.BitRates
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.CodecResources
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Colors
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Fit
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Flip
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameGeometry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameSize
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.PlacedVideo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleWarnings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineLayout
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Transition
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionDirection
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionType
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * `compose` (roadmap P3.1 / P3.2, D33 / D30): places the timeline ([TimelineLayout]), mixes its audio into one PCM file
 * with [PcmMixer] (two passes, appendix D Q17 (c)) and exports one media3 [Composition]:
 *
 * - sequence 0 is a transparent "clock" image at the timeline frame rate; the compositor takes the timestamps of its
 *   first input, so the output runs at that rate and lasts the whole timeline;
 * - then the video tracks from the top down; a track with overlapping transitions uses two sequences ("lanes", odd
 *   clips above even clips), every lane fills its gaps with media3 gap frames that [ComposeCompositorSettings] hides;
 * - then the background color (when it is not black) and the mixed audio;
 * - the timeline overlays are a composition effect drawn on the composited frame.
 *
 * Every clip is cropped, rotated, flipped, fitted to the output size and scaled by its own effects; the shader
 * transitions add a [TransitionEffect]. When the export fails in the frame processor and the timeline holds shader
 * transitions, it is exported again with `dissolve` / `cut` instead, and then with every transition as `cut`; each
 * fallback adds a warning (roadmap P3.2).
 * zh-CN: `compose` (路线图 P3.1 / P3.2, D33 / D30): 定位时间线 ([TimelineLayout]), 用 [PcmMixer] 把音频混成一个 PCM 文件 (两遍,
 * 附录 D Q17 (c)), 再导出一个 media3 [Composition]:
 *
 * - 序列 0 是按时间线帧率的透明 "时钟" 图片; 合成器取其第一个输入的时间戳, 因此输出按该帧率运行并覆盖整条时间线;
 * - 其后是自上而下的视频轨; 有重叠转场的轨道用两个序列 ("车道", 奇数段在偶数段之上), 各车道的空档由 media3 空档帧填充, 由
 *   [ComposeCompositorSettings] 隐藏;
 * - 然后是背景色 (非黑色时) 与混好的音频;
 * - 时间线叠加物作为合成级效果画在合成后的帧上.
 *
 * 每段由自身效果完成裁切, 旋转, 翻转, 适配输出尺寸与缩放; 着色器转场另加 [TransitionEffect]. 导出在帧处理器中失败且时间线含
 * 着色器转场时, 改用 `dissolve` / `cut` 再导出一次, 再失败则把全部转场改为 `cut`; 每次回退都加一条警告 (路线图 P3.2).
 */
internal class ComposeOperation(
    private val context: Context,
    private val task: TaskRunner.JobContext,
    private val notes: MutableList<String>,
) {

    private val export = TransformerExport(context, task)

    private val temporary = mutableListOf<File>()

    private val resources = mutableListOf<Closeable>()

    /** The `subtitles` of the timeline, loaded once for every export attempt. zh-CN: 时间线的 `subtitles`, 只加载一次, 供每次导出尝试使用. */
    private var painter: SubtitlePainter? = null

    fun run(spec: OperationSpec.Compose, inputs: List<MediaInfo>, output: String, from: Double, to: Double) {
        val probed = inputs.associateBy { it.path }
        val layout = TimelineLayout.resolve(spec.timeline) { path ->
            probed[path] ?: throw MediaStudioException(MediaStudioErrorCodes.INTERNAL_ERROR, "$path was not probed")
        }
        layout.overlays.forEachIndexed { index, item ->
            if (item.placement.fromMillis >= layout.durationMillis) {
                notes += "timeline.overlays[$index] starts at ${item.placement.fromMillis} ms, after the end of the timeline (${layout.durationMillis} ms); it never shows"
            }
        }
        layout.subtitles?.let { subtitles ->
            val warnings = SubtitleWarnings()
            val track = SubtitleFiles.load(subtitles.source, warnings)
            painter = SubtitlePainter(track, subtitles.style, warnings)
            if (track.cues.none { it.startMillis < layout.durationMillis }) {
                notes += "no cue of timeline.subtitles starts before the end of the timeline (${layout.durationMillis} ms); none shows"
            }
            notes += warnings.list()
        }
        try {
            val audioEnd = from + (to - from) * AUDIO_SHARE
            val audio = mixAudio(layout, from, audioEnd)
            val start = if (audio != null) audioEnd else from
            exportWithFallback(spec, layout, audio, output, start, to)
        } finally {
            notes += export.notes
            resources.forEach { runCatching { it.close() } }
            resources.clear()
            temporary.forEach { it.delete() }
            temporary.clear()
        }
    }

    private fun exportWithFallback(spec: OperationSpec.Compose, layout: TimelineLayout, audio: File?, output: String, from: Double, to: Double) {
        var current = layout
        var step = 0
        while (true) {
            try {
                val composition = composition(current, audio)
                val size = FrameSize(current.width, current.height)
                val settings = TransformerExport.Settings(
                    videoMime = spec.video.codec.mime,
                    audioMime = if (audio != null) MimeTypes.AUDIO_AAC else null,
                    videoBitRate = spec.video.bitRate ?: BitRates.estimateVideo(size.width, size.height, current.frameRate.toDouble(), spec.video.codec),
                )
                export.run(composition, output, settings, from, to, MediaStudioContract.STAGE_ENCODING)
                return
            } catch (e: MediaStudioException) {
                if (e.code == MediaStudioErrorCodes.TASK_CANCELLED || task.isCancelled) throw e
                if (!isFrameProcessingFailure(e)) throw exhausted(e, spec) ?: e
                var next: TimelineLayout? = null
                // First the shader transitions, then every transition; a step that changes nothing is skipped.
                // zh-CN: 先替换着色器转场, 再替换全部转场; 不改变任何内容的步骤跳过.
                while (next == null && step < 2) {
                    val replaced = current.transitionTypes().filter { if (step == 0) it.shader else it != TransitionType.CUT }
                    if (replaced.isNotEmpty()) {
                        next = if (step == 0) current.withTransitions { it.gpuFallback } else current.withTransitions { TransitionType.CUT }
                        val into = if (step == 0) "dissolve (fade as cut)" else "cut"
                        notes += "the GPU of this device failed on the ${replaced.joinToString { it.id }} transitions " +
                            "(${e.detail.take(NOTE_DETAIL_CHARS)}); they were exported as $into"
                    }
                    step++
                }
                current = next ?: throw e
                File(output).delete()
                resources.forEach { runCatching { it.close() } }
                resources.clear()
            }
        }
    }

    /**
     * An actionable CODEC_ERROR when the export failed because no further encoder or decoder could start (appendix D
     * Q37 (a)), or null for any other failure.
     * zh-CN: 导出因无法再启动编码器或解码器而失败时返回可操作的 CODEC_ERROR (附录 D Q37 (a)); 其他失败返回 null.
     */
    private fun exhausted(e: MediaStudioException, spec: OperationSpec.Compose): MediaStudioException? {
        val export = e.cause as? ExportException ?: return null
        val chain = generateSequence<Throwable>(export) { it.cause }.take(MAX_CAUSES).toList()
        val codecErrors = chain.filterIsInstance<MediaCodec.CodecException>().map { it.errorCode }
        val texts = chain.mapNotNull { it.message }
        val budget = CodecResources.budgetOf(spec.timeline)
        if (!CodecResources.isExhausted(export.errorCodeName, codecErrors, texts, budget)) return null
        val detail = CodecResources.message(export.errorCodeName, CodecResources.resourceSign(codecErrors, texts), budget)
        return MediaStudioException(MediaStudioErrorCodes.CODEC_ERROR, detail, export)
    }

    private fun TimelineLayout.transitionTypes(): List<TransitionType> =
        videoTracks.flatten().mapNotNull { it.into?.type }.distinct()

    /**
     * The audio of the timeline as one 16-bit WAV in the cache (null when nothing sounds): video clips with an audio
     * track and a volume above 0 (at their speed, the pitch kept, cross-faded over their transitions) and the audio
     * clips with their fades.
     * zh-CN: 时间线的音频, 混为缓存中的一个 16 位 WAV (无声时为 null): 有音轨且音量大于 0 的视频段 (按其速度, 保持音高, 在转场处
     * 交叉淡化) 与带淡入淡出的音频段.
     */
    private fun mixAudio(layout: TimelineLayout, from: Double, to: Double): File? {
        val clips = layout.videoTracks.flatten().filter { it.info.hasAudio && it.clip.volume > 0.0 }
        if (clips.isEmpty() && layout.audio.isEmpty()) return null
        val infos = clips.map { it.info } + layout.audio.map { it.info }
        val (rate, channels) = AudioTargets.common(infos.map { it.sampleRate }, infos.map { it.channels })
        val target = AudioTargets.of(AudioContainer.M4A, AudioEncoding(), rate, channels)
        fun frames(us: Long) = Math.round(us * target.sampleRate / 1_000_000.0)
        val sources = clips.map { placed ->
            MixSource(
                path = placed.clip.src,
                volume = placed.clip.volume.toFloat(),
                offsetFrames = frames(placed.atUs),
                startUs = placed.sourceStartUs,
                endUs = placed.sourceEndUs,
                speed = placed.clip.speed.toFloat(),
                pitch = 1f,
                lengthFrames = frames(placed.lengthUs),
                fadeInFrames = placed.into?.let { frames(TimelineLayout.sideUs(it)) } ?: 0L,
                fadeOutFrames = placed.out?.let { frames(TimelineLayout.sideUs(it)) } ?: 0L,
            )
        } + layout.audio.map { placed ->
            MixSource(
                path = placed.clip.src,
                volume = placed.clip.volume.toFloat(),
                offsetFrames = frames(placed.atUs),
                startUs = placed.sourceStartUs,
                endUs = placed.sourceEndUs,
                lengthFrames = frames(placed.lengthUs),
                fadeInFrames = frames(placed.clip.fadeInMillis * 1_000L),
                fadeOutFrames = frames(placed.clip.fadeOutMillis * 1_000L),
            )
        }
        val total = frames(layout.durationUs)
        val mixer = PcmMixer(task, target, sources, totalFrames = total)
        val scan = { fraction: Double -> task.progress(from + (to - from) * PcmMixer.ANALYSIS_SHARE * fraction, MediaStudioContract.STAGE_DECODING) }
        val analysis = mixer.analyze(scan)
        val file = File(context.cacheDir, "compose-${task.taskId}.wav").also { temporary += it }
        val write = { fraction: Double ->
            task.progress(from + (to - from) * (PcmMixer.ANALYSIS_SHARE + (1 - PcmMixer.ANALYSIS_SHARE) * fraction), MediaStudioContract.STAGE_DECODING)
        }
        val sink = PcmSinks.open(AudioContainer.WAV, file.path, target.sampleRate, target.channels, null)
        var finished = false
        try {
            var buffer = ByteBuffer.allocateDirect(PcmMixer.BLOCK_FRAMES * target.channels * 2).order(ByteOrder.LITTLE_ENDIAN)
            mixer.write(analysis.scale, write) { samples, count ->
                if (buffer.capacity() < count * 2) buffer = ByteBuffer.allocateDirect(count * 2).order(ByteOrder.LITTLE_ENDIAN)
                buffer.clear()
                buffer.asShortBuffer().put(samples, 0, count)
                buffer.limit(count * 2)
                sink.write(buffer)
            }
            sink.finish()
            finished = true
        } finally {
            if (!finished) sink.close()
        }
        PcmMixer.scaledNote(analysis, "the timeline audio")?.let { notes += it }
        return file
    }

    private fun composition(layout: TimelineLayout, audio: File?): Composition {
        val sequences = mutableListOf<EditedMediaItemSequence>()
        val lanes = mutableMapOf<Int, Lane>()
        sequences += imageSequence(layout, Color.TRANSPARENT, "clock")
        for (track in layout.videoTracks.indices.reversed()) {
            val placed = layout.videoTracks[track]
            val split = placed.zipWithNext().any { (previous, next) -> next.atUs < previous.endUs }
            val groups = if (split) listOf(placed.filter { it.index % 2 == 1 } to true, placed.filter { it.index % 2 == 0 } to false) else listOf(placed to false)
            groups.forEach { (clips, top) ->
                if (clips.isEmpty()) return@forEach
                lanes[sequences.size] = Lane(clips, top)
                sequences += laneSequence(layout, clips, top)
            }
        }
        var background: Int? = null
        if (Color.alpha(layout.background) != 0 && layout.background != Colors.BLACK) {
            background = sequences.size
            sequences += imageSequence(layout, layout.background, "background")
        }
        audio?.let { file ->
            val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(file))).setRemoveVideo(true).build()
            sequences += EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO)).addItem(item).build()
        }
        val effects = if (layout.overlays.isEmpty() && painter == null) Effects.EMPTY else {
            val renderer = OverlayRenderer(layout.overlays, FrameSize(layout.width, layout.height), painter).also { resources += it }
            Effects(emptyList(), listOf<Effect>(OverlayEffect(listOf(renderer.overlay()))))
        }
        return Composition.Builder(sequences)
            .setVideoCompositorSettings(ComposeCompositorSettings(layout.width, layout.height, lanes, background))
            .setEffects(effects)
            .build()
    }

    /**
     * A still image covering the whole timeline at its frame rate (the clock, or the background stretched to the
     * output size).
     * zh-CN: 按时间线帧率覆盖整条时间线的静态图片 (时钟, 或拉伸到输出尺寸的背景).
     */
    private fun imageSequence(layout: TimelineLayout, color: Int, name: String): EditedMediaItemSequence {
        val file = File(context.cacheDir, "compose-${task.taskId}-$name.png").also { temporary += it }
        val bitmap = createBitmap(IMAGE_SIDE, IMAGE_SIDE).apply { eraseColor(color) }
        try {
            FileOutputStream(file).use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        } finally {
            bitmap.recycle()
        }
        val durationMs = layout.durationMillis
        val item = MediaItem.Builder().setUri(Uri.fromFile(file)).setMimeType(MimeTypes.IMAGE_PNG).setImageDurationMs(durationMs).build()
        val edited = EditedMediaItem.Builder(item)
            .setDurationUs(durationMs * 1_000L)
            .setFrameRate(layout.frameRate)
            .setEffects(Effects(emptyList(), listOf<Effect>(Presentation.createForWidthAndHeight(layout.width, layout.height, Presentation.LAYOUT_STRETCH_TO_FIT))))
            .build()
        return EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO)).addItem(edited).build()
    }

    private fun laneSequence(layout: TimelineLayout, clips: List<PlacedVideo>, top: Boolean): EditedMediaItemSequence {
        // The track types make a leading gap a video gap (the force flags conflict with them). zh-CN: 轨道类型让开头的空档成为视频空档 (与 force 标志冲突).
        val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
        var cursor = 0L
        clips.forEach { placed ->
            if (placed.atUs > cursor) sequence.addGap(placed.atUs - cursor)
            sequence.addItem(clipItem(layout, placed, top))
            cursor = maxOf(cursor, placed.endUs)
        }
        // Gap frames keep the lane current until the end, so no stale frame stays visible. zh-CN: 空档帧让车道一直推进到结尾, 不会留下过期画面.
        if (cursor < layout.durationUs) sequence.addGap(layout.durationUs - cursor)
        return sequence.build()
    }

    private fun clipItem(layout: TimelineLayout, placed: PlacedVideo, top: Boolean): EditedMediaItem {
        val transform = placed.clip.transform
        var size = FrameSize(placed.info.width ?: 0, placed.info.height ?: 0)
        val effects = mutableListOf<Effect>()
        transform.crop?.let { crop ->
            FrameGeometry.cropProblem(crop, size)?.let { throw MediaStudioException.invalid("${placed.clip.path}.transform: $it") }
            val ndc = FrameGeometry.cropNdc(crop, size)
            effects += Crop(ndc[0], ndc[1], ndc[2], ndc[3])
            size = FrameSize(crop.width, crop.height)
        }
        if (transform.rotate != 0) {
            // media3 rotates counterclockwise. zh-CN: media3 按逆时针旋转.
            effects += ScaleAndRotateTransformation.Builder().setRotationDegrees(-transform.rotate.toFloat()).build()
        }
        if (transform.flip != Flip.NONE) {
            effects += ScaleAndRotateTransformation.Builder()
                .setScale(if (transform.flip.horizontal) -1f else 1f, if (transform.flip.vertical) -1f else 1f)
                .build()
        }
        effects += Presentation.createForWidthAndHeight(layout.width, layout.height, layoutOf(transform.fit))
        if (transform.scale != 1.0) {
            effects += ScaleAndRotateTransformation.Builder().setScale(transform.scale.toFloat(), transform.scale.toFloat()).build()
        }
        val into = placed.into?.let { part(it, top) }
        val out = placed.out?.let { part(it, top) }
        if (into != null || out != null) {
            effects += TransitionEffect(placed.lengthUs, into, out).also { it.failForTest = failShadersForTest }
            if (into?.type == TransitionType.BLUR || out?.type == TransitionType.BLUR) {
                effects += TransitionEffect(placed.lengthUs, into, out, verticalBlur = true).also { it.failForTest = failShadersForTest }
            }
        }
        val clipping = MediaItem.ClippingConfiguration.Builder().setStartPositionUs(placed.sourceStartUs)
        if (placed.sourceEndUs < placed.info.durationMillis * 1_000L) clipping.setEndPositionUs(placed.sourceEndUs)
        val item = MediaItem.Builder().setUri(Uri.fromFile(File(placed.clip.src))).setClippingConfiguration(clipping.build()).build()
        val edited = EditedMediaItem.Builder(item).setRemoveAudio(true).setEffects(Effects(emptyList(), effects))
        if (placed.clip.speed != 1.0) edited.setSpeed(ConstantSpeed(placed.clip.speed.toFloat()))
        return edited.build()
    }

    private fun part(transition: Transition, top: Boolean): TransitionEffect.Part? = transition.takeIf { it.type.shader }?.let {
        TransitionEffect.Part(it.type, TimelineLayout.sideUs(it), it.direction, masked = top && it.type == TransitionType.WIPE)
    }

    private class ConstantSpeed(private val factor: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long): Float = factor
        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
    }

    /** The clips of one sequence; [top] is the upper lane of a split track. zh-CN: 一个序列中的片段; [top] 表示拆分轨道的上层车道. */
    class Lane(val clips: List<PlacedVideo>, val top: Boolean) {
        fun at(timeUs: Long): PlacedVideo? = clips.firstOrNull { timeUs >= it.atUs && timeUs < it.endUs }
    }

    companion object {

        /** Share of the progress for the audio mix. zh-CN: 混音所占的进度份额. */
        const val AUDIO_SHARE = 0.15

        const val IMAGE_SIDE = 16

        const val NOTE_DETAIL_CHARS = 160

        /** Causes searched for a codec resource sign. zh-CN: 查找编解码器资源迹象时检查的原因层数. */
        const val MAX_CAUSES = 8

        /**
         * Makes every [TransitionEffect] fail to compile, to test the GPU fallback in instrumented tests.
         * zh-CN: 让每个 [TransitionEffect] 编译失败, 用于在仪器测试中检验 GPU 回退.
         */
        @VisibleForTesting
        @Volatile
        var failShadersForTest: Boolean = false

        fun isFrameProcessingFailure(e: MediaStudioException): Boolean =
            e.detail.startsWith("ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED") || e.detail.startsWith("ERROR_CODE_FAILED_RUNTIME_CHECK")

        fun layoutOf(fit: Fit): Int = when (fit) {
            Fit.CONTAIN -> Presentation.LAYOUT_SCALE_TO_FIT
            Fit.COVER -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            Fit.STRETCH -> Presentation.LAYOUT_STRETCH_TO_FIT
        }

    }

}

/**
 * The compositor half of `compose` (roadmap P3.1 / P3.2): every input is [width] x [height] already; the clock (input 0)
 * and lanes in a gap are invisible; inside a transition the upper lane fades (dissolve, zoom, blur), the two clips move
 * (slide) or scale (zoom). Inputs are numbered like the sequences; the compositor draws input 0 on top.
 * zh-CN: `compose` 的合成器部分 (路线图 P3.1 / P3.2): 每个输入已是 [width] x [height]; 时钟 (输入 0) 与处于空档的车道不可见; 转场中
 * 上层车道渐隐渐显 (dissolve, zoom, blur), 两段移动 (slide) 或缩放 (zoom). 输入按序列编号; 合成器把输入 0 画在最上层.
 */
@OptIn(UnstableApi::class)
internal class ComposeCompositorSettings(
    private val width: Int,
    private val height: Int,
    private val lanes: Map<Int, ComposeOperation.Lane>,
    private val background: Int?,
) : VideoCompositorSettings {

    override fun getOutputSize(inputSizes: MutableList<Size>): Size = Size(width, height)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        if (inputId == background) return VISIBLE
        val lane = lanes[inputId] ?: return HIDDEN
        val placed = lane.at(presentationTimeUs) ?: return HIDDEN
        val into = placed.into?.takeIf { it.type.overlaps }
        val out = placed.out?.takeIf { it.type.overlaps }
        return when {
            into != null && presentationTimeUs < placed.atUs + TimelineLayout.sideUs(into) ->
                settings(into, TransitionEffect.progress(presentationTimeUs - placed.atUs, TimelineLayout.sideUs(into)), incoming = true, top = lane.top)
            out != null && presentationTimeUs >= placed.endUs - TimelineLayout.sideUs(out) -> {
                val side = TimelineLayout.sideUs(out)
                settings(out, TransitionEffect.progress(presentationTimeUs - (placed.endUs - side), side), incoming = false, top = lane.top)
            }
            else -> VISIBLE
        }
    }

    companion object {

        /** Scale of the incoming clip at the start of a zoom, and growth of the outgoing one. zh-CN: zoom 开始时入场片段的缩放, 以及离场片段的放大量. */
        const val ZOOM_FROM = 0.5f

        val VISIBLE: OverlaySettings = StaticOverlaySettings.Builder().build()

        val HIDDEN: OverlaySettings = StaticOverlaySettings.Builder().setAlphaScale(0f).build()

        /**
         * The settings of one clip at [progress] (0 - 1) of [transition]; [top] says the clip is in the upper lane.
         * zh-CN: 一段处于 [transition] 的 [progress] (0 - 1) 时的设置; [top] 表示该段位于上层车道.
         */
        fun settings(transition: Transition, progress: Float, incoming: Boolean, top: Boolean): OverlaySettings {
            val builder = StaticOverlaySettings.Builder()
            val fading = transition.type == TransitionType.DISSOLVE || transition.type == TransitionType.ZOOM || transition.type == TransitionType.BLUR
            if (fading && top) builder.setAlphaScale(if (incoming) progress else 1f - progress)
            when (transition.type) {
                TransitionType.ZOOM -> {
                    val scale = if (incoming) ZOOM_FROM + (1f - ZOOM_FROM) * progress else 1f + (1f - ZOOM_FROM) * progress
                    builder.setScale(scale, scale)
                }
                TransitionType.SLIDE -> {
                    val (vx, vy) = slideVector(transition.direction)
                    val distance = if (incoming) 2f * (1f - progress) else -2f * progress
                    // An offset of up to two frame widths, split between both anchors so each stays in [-1, 1].
                    // zh-CN: 偏移至多两个帧宽, 分摊到两个锚点上, 使每个都在 [-1, 1] 内.
                    builder.setBackgroundFrameAnchor(vx * distance / 2f, vy * distance / 2f)
                    builder.setOverlayFrameAnchor(-vx * distance / 2f, -vy * distance / 2f)
                }
                else -> Unit
            }
            return builder.build()
        }

        /**
         * Where the incoming clip starts, in units of the frame (NDC y grows upwards).
         * zh-CN: 入场片段的起始位置, 以帧为单位 (NDC 的 y 向上增大).
         */
        fun slideVector(direction: TransitionDirection): Pair<Float, Float> = when (direction) {
            TransitionDirection.LEFT -> 1f to 0f
            TransitionDirection.RIGHT -> -1f to 0f
            TransitionDirection.UP -> 0f to -1f
            TransitionDirection.DOWN -> 0f to 1f
        }

    }

}

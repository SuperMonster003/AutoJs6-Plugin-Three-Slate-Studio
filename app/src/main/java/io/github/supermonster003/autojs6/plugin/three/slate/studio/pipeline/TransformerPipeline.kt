@file:OptIn(UnstableApi::class, ExperimentalApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioEncoding
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.BitRates
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Fit
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Flip
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameGeometry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameSize
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleWarnings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import java.io.Closeable
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * One media3 Transformer export on its own looper thread (roadmap P2.2). [run] blocks the worker thread until the
 * export ends, polls the progress every 200 ms into the range [from, to] and cancels the export when the task is
 * cancelled. Encoder fallbacks of media3 end up in [notes].
 * zh-CN: 在独立 looper 线程上运行的一次 media3 Transformer 导出 (路线图 P2.2). [run] 阻塞工作线程直到导出结束, 每 200 ms 把进度
 * 换算到 [from, to] 区间, 任务被取消时取消导出. media3 的编码器回退记入 [notes].
 */
internal class TransformerExport(private val context: Context, private val task: TaskRunner.JobContext) {

    /**
     * Output settings. A null MIME keeps the input codec where media3 can pass the track through; a bit rate makes the
     * track re-encode.
     * zh-CN: 输出设置. MIME 为 null 时, media3 能直通的轨道保留输入编码; 给出码率会让该轨道重新编码.
     */
    data class Settings(
        val videoMime: String? = null,
        val audioMime: String? = null,
        val videoBitRate: Int? = null,
        val audioBitRate: Int? = null,
        val trimOptimization: Boolean = false,
    )

    val notes: MutableList<String> = CopyOnWriteArrayList()

    private sealed interface Outcome {
        class Completed(val result: ExportResult) : Outcome
        class Failed(val error: ExportException) : Outcome
        class Crashed(val error: RuntimeException) : Outcome
        data object Cancelled : Outcome
    }

    fun run(composition: Composition, output: String, settings: Settings, from: Double, to: Double, stage: String): ExportResult {
        task.throwIfCancelled()
        val thread = HandlerThread("SlateStudio-export-${task.taskId}").apply { start() }
        val handler = Handler(thread.looper)
        val done = CountDownLatch(1)
        val outcome = AtomicReference<Outcome?>(null)
        val finish = { value: Outcome -> if (outcome.compareAndSet(null, value)) done.countDown() }
        // Only touched on the export thread. zh-CN: 只在导出线程上访问.
        var transformer: Transformer? = null
        val holder = ProgressHolder()
        val poll = object : Runnable {
            override fun run() {
                val current = transformer ?: return
                if (outcome.get() != null) return
                if (current.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    task.progress(from + (to - from) * holder.progress.coerceIn(0, 100) / 100.0, stage)
                }
                handler.postDelayed(this, POLL_INTERVAL_MILLIS)
            }
        }
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) = finish(Outcome.Completed(exportResult))

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) =
                finish(Outcome.Failed(exportException))

            override fun onFallbackApplied(
                composition: Composition,
                originalTransformationRequest: TransformationRequest,
                fallbackTransformationRequest: TransformationRequest,
            ) {
                fallbackNote(originalTransformationRequest, fallbackTransformationRequest)?.let { notes += it }
            }
        }
        handler.post {
            if (task.isCancelled) {
                finish(Outcome.Cancelled)
                return@post
            }
            try {
                val built = build(settings, thread.looper, listener)
                transformer = built
                built.start(composition, output)
                handler.postDelayed(poll, POLL_INTERVAL_MILLIS)
            } catch (e: RuntimeException) {
                transformer?.cancel()
                finish(Outcome.Crashed(e))
            }
        }
        task.setCancelHandler {
            handler.post {
                transformer?.cancel()
                finish(Outcome.Cancelled)
            }
        }
        try {
            done.await()
        } catch (e: InterruptedException) {
            handler.post {
                transformer?.cancel()
                finish(Outcome.Cancelled)
            }
            Thread.currentThread().interrupt()
            throw MediaStudioException(MediaStudioErrorCodes.TASK_CANCELLED, "task ${task.taskId} was interrupted", e)
        } finally {
            task.setCancelHandler(null)
            handler.removeCallbacks(poll)
            thread.quitSafely()
        }
        return when (val result = outcome.get()) {
            is Outcome.Completed -> result.result
            is Outcome.Failed -> throw errorOf(result.error)
            is Outcome.Crashed -> throw MediaStudioException(
                MediaStudioErrorCodes.INTERNAL_ERROR,
                "the export could not start (${result.error.message ?: result.error.javaClass.simpleName})",
                result.error,
            )
            Outcome.Cancelled, null -> throw MediaStudioException(MediaStudioErrorCodes.TASK_CANCELLED, "task ${task.taskId} was cancelled")
        }
    }

    private fun build(settings: Settings, looper: Looper, listener: Transformer.Listener): Transformer {
        // The requested size first, the media3 fallback only when the encoder refuses it (appendix D Q38 (a)).
        // zh-CN: 先用请求尺寸, 编码器拒绝时才用 media3 的回退 (附录 D Q38 (a)).
        val encoders = RequestedSizeEncoderFactory(
            context.applicationContext,
            settings.videoBitRate?.let { VideoEncoderSettings.Builder().setBitrate(it).build() } ?: VideoEncoderSettings.DEFAULT,
            settings.audioBitRate?.let { AudioEncoderSettings.Builder().setBitrate(it).build() } ?: AudioEncoderSettings.DEFAULT,
        ) { note -> notes += note }
        val builder = Transformer.Builder(context.applicationContext)
            .setLooper(looper)
            .setEncoderFactory(encoders)
            .experimentalSetTrimOptimizationEnabled(settings.trimOptimization)
            .addListener(listener)
        settings.videoMime?.let { builder.setVideoMimeType(it) }
        settings.audioMime?.let { builder.setAudioMimeType(it) }
        return builder.build()
    }

    companion object {

        const val POLL_INTERVAL_MILLIS = 200L

        /**
         * Maps an export failure onto a contract code: IO 2xxx to IO_ERROR, unsupported decoder or encoder formats to
         * UNSUPPORTED_FORMAT, other codec, frame, audio and muxer failures (3xxx - 7xxx) to CODEC_ERROR, the rest to
         * INTERNAL_ERROR.
         * zh-CN: 把导出失败映射为契约错误码: IO 类 2xxx 为 IO_ERROR, 解码器或编码器格式不支持为 UNSUPPORTED_FORMAT, 其他编解码,
         * 帧处理, 音频与封装失败 (3xxx - 7xxx) 为 CODEC_ERROR, 其余为 INTERNAL_ERROR.
         */
        fun errorOf(error: ExportException): MediaStudioException {
            val code = when (error.errorCode) {
                ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
                -> MediaStudioErrorCodes.UNSUPPORTED_FORMAT
                in 2000..2999 -> MediaStudioErrorCodes.IO_ERROR
                in 3000..7999 -> MediaStudioErrorCodes.CODEC_ERROR
                else -> MediaStudioErrorCodes.INTERNAL_ERROR
            }
            val detail = buildString {
                append(error.errorCodeName)
                error.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
                error.cause?.message?.takeIf { it.isNotBlank() && it != error.message }?.let { append(" (").append(it).append(')') }
            }
            return MediaStudioException(code, detail, error)
        }

        /**
         * The note of a media3 fallback: MIME type changes only; a size change is reported with both dimensions by
         * [RequestedSizeEncoderFactory] (appendix D Q38 (a)), so a fallback of the size alone adds nothing here.
         * zh-CN: media3 回退的说明: 只列 MIME 类型的变化; 尺寸变化由 [RequestedSizeEncoderFactory] 连同宽高一起报告 (附录 D Q38 (a)),
         * 因此只改尺寸的回退在这里不加说明.
         */
        private fun fallbackNote(original: TransformationRequest, fallback: TransformationRequest): String? {
            val changes = buildList {
                if (original.videoMimeType != fallback.videoMimeType) add("video ${original.videoMimeType} -> ${fallback.videoMimeType}")
                if (original.audioMimeType != fallback.audioMimeType) add("audio ${original.audioMimeType} -> ${fallback.audioMimeType}")
            }
            if (changes.isEmpty()) return null
            return changes.joinToString(prefix = "the encoder fell back on this device: ")
        }

    }

}

/**
 * Builds the media3 compositions of the video operations (roadmap P2.2, D29) and runs them; `extractAudio` to M4A runs
 * here as well, the other audio containers and the `audio*` operations go through [AudioOperations].
 * zh-CN: 构建视频操作的 media3 合成并运行 (路线图 P2.2, D29); `extractAudio` 输出 M4A 也在这里运行, 其他音频容器与 `audio*` 操作走 [AudioOperations].
 */
internal class VideoOperations(
    context: Context,
    private val task: TaskRunner.JobContext,
    private val notes: MutableList<String>,
) {

    private val export = TransformerExport(context, task)

    /** Resources an export holds until it ends. zh-CN: 导出结束前持有的资源. */
    private val resources = mutableListOf<Closeable>()

    private data class Plan(val composition: Composition, val settings: TransformerExport.Settings, val stage: String = MediaStudioContract.STAGE_ENCODING)

    fun run(spec: OperationSpec, inputs: List<MediaInfo>, output: String, from: Double, to: Double) {
        try {
            if (spec is OperationSpec.Trim) {
                trim(spec, inputs.single(), output, from, to)
                return
            }
            val plan = when (spec) {
                is OperationSpec.Concat -> concat(spec, inputs)
                is OperationSpec.Speed -> speed(spec, inputs.single())
                is OperationSpec.Transform -> transform(spec, inputs.single())
                is OperationSpec.Transcode -> transcode(spec, inputs.single())
                OperationSpec.RemoveAudio -> removeAudio(inputs.single())
                is OperationSpec.ExtractAudio -> extractAudio(spec, inputs.single())
                is OperationSpec.Overlay -> overlay(spec, inputs.single())
                is OperationSpec.BurnSubtitles -> burnSubtitles(spec, inputs.single())
                is OperationSpec.Trim -> error("handled above")
                OperationSpec.SubtitlesParse, is OperationSpec.SubtitlesWrite -> error("${spec::class.simpleName} is written by OperationJob")
                is OperationSpec.Compose -> error("compose is written by ComposeOperation")
                is OperationSpec.Frames, is OperationSpec.Frame, is OperationSpec.Thumbnail, is OperationSpec.ToGif ->
                    error("${spec::class.simpleName} is written by StillOperations")
                is OperationSpec.AudioTrim, is OperationSpec.AudioConcat, is OperationSpec.AudioMix, is OperationSpec.AudioVolume,
                is OperationSpec.AudioConvert, is OperationSpec.AudioSpeed ->
                    error("${spec::class.simpleName} is written by AudioOperations")
            }
            export.run(plan.composition, output, plan.settings, from, to, plan.stage)
        } finally {
            notes += export.notes
            resources.forEach { runCatching { it.close() } }
            resources.clear()
        }
    }

    /**
     * Pass-through first (media3 trim optimization keeps the input codec and re-encodes only up to the first key frame);
     * when that export fails, once more with H.264 at the estimated bit rate.
     * zh-CN: 先尝试直通 (media3 剪辑优化保留输入编码, 只重编码到第一个关键帧); 该导出失败时, 用 H.264 与估算码率再导出一次.
     */
    private fun trim(spec: OperationSpec.Trim, input: MediaInfo, output: String, from: Double, to: Double) {
        requireVideo(input, MediaStudioOps.TRIM)
        if (spec.startMillis >= input.durationMillis) {
            throw MediaStudioException.invalid("options.start (${spec.startMillis} ms) is not before the end of the input (${input.durationMillis} ms)")
        }
        val clipping = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(spec.startMillis)
        spec.endMillis?.takeIf { it < input.durationMillis }?.let { clipping.setEndPositionMs(it) }
        val item = MediaItem.Builder().setUri(uriOf(input.path)).setClippingConfiguration(clipping.build()).build()
        val composition = single(EditedMediaItem.Builder(item).build())
        try {
            export.run(composition, output, TransformerExport.Settings(trimOptimization = true), from, to, MediaStudioContract.STAGE_ENCODING)
        } catch (e: MediaStudioException) {
            if (e.code == MediaStudioErrorCodes.TASK_CANCELLED || task.isCancelled) throw e
            File(output).delete()
            notes += "trim re-encoded the video with H.264 because the pass-through export failed (${e.detail.take(NOTE_DETAIL_CHARS)})"
            val size = FrameGeometry.even(displaySize(input))
            val settings = TransformerExport.Settings(
                videoMime = VideoCodec.H264.mime,
                audioMime = if (input.hasAudio) MimeTypes.AUDIO_AAC else null,
                videoBitRate = BitRates.estimateVideo(size.width, size.height, input.frameRate, VideoCodec.H264),
            )
            export.run(composition, output, settings, from, to, MediaStudioContract.STAGE_ENCODING)
        }
    }

    private fun concat(spec: OperationSpec.Concat, inputs: List<MediaInfo>): Plan {
        inputs.forEach { requireVideo(it, MediaStudioOps.CONCAT) }
        val target = FrameGeometry.target(spec.width, spec.height, displaySize(inputs.first()))
        FrameGeometry.sizeProblem(target)?.let { throw MediaStudioException.invalid(it) }
        val presentation = Presentation.createForWidthAndHeight(target.width, target.height, layoutOf(spec.fit))
        val items = inputs.map { info ->
            EditedMediaItem.Builder(mediaItemOf(info.path)).setEffects(Effects(emptyList(), listOf<Effect>(presentation))).build()
        }
        val withAudio = inputs.count { it.hasAudio }
        val sequence = EditedMediaItemSequence.Builder(items)
        if (withAudio in 1 until inputs.size) {
            // Silence for the inputs without an audio track. zh-CN: 为没有音轨的输入补静音.
            sequence.experimentalSetForceAudioTrack(true)
        }
        val frameRate = inputs.mapNotNull { it.frameRate }.maxOrNull()
        return Plan(
            Composition.Builder(listOf(sequence.build())).build(),
            TransformerExport.Settings(
                videoMime = spec.video.codec.mime,
                audioMime = if (withAudio > 0) MimeTypes.AUDIO_AAC else null,
                videoBitRate = spec.video.bitRate ?: BitRates.estimateVideo(target.width, target.height, frameRate, spec.video.codec),
            ),
        )
    }

    /**
     * media3 changes the speed like a playback rate (the pitch follows the factor); `keepPitch` adds a Sonic pitch of
     * 1 / factor to undo that.
     * zh-CN: media3 按播放速率方式变速 (音高随倍数变化); `keepPitch` 追加 Sonic 音高 1 / 倍数 以抵消.
     */
    private fun speed(spec: OperationSpec.Speed, input: MediaInfo): Plan {
        requireVideo(input, MediaStudioOps.SPEED)
        val factor = spec.factor.toFloat()
        val audio = if (spec.keepPitch && input.hasAudio) listOf<AudioProcessor>(SonicAudioProcessor().apply { setPitch(1f / factor) }) else emptyList()
        val item = EditedMediaItem.Builder(mediaItemOf(input.path))
            .setSpeed(ConstantSpeed(factor))
            .setEffects(Effects(audio, emptyList()))
            .build()
        val size = FrameGeometry.even(displaySize(input))
        return Plan(
            single(item),
            TransformerExport.Settings(
                videoMime = spec.video.codec.mime,
                audioMime = if (input.hasAudio) MimeTypes.AUDIO_AAC else null,
                videoBitRate = spec.video.bitRate ?: BitRates.estimateVideo(size.width, size.height, input.frameRate?.times(spec.factor), spec.video.codec),
            ),
        )
    }

    /** Crop, then rotate, then flip, then size the frame. zh-CN: 先裁剪, 再旋转, 再翻转, 最后调整尺寸. */
    private fun transform(spec: OperationSpec.Transform, input: MediaInfo): Plan {
        requireVideo(input, MediaStudioOps.TRANSFORM)
        var size = displaySize(input)
        val effects = mutableListOf<Effect>()
        spec.crop?.let { crop ->
            FrameGeometry.cropProblem(crop, size)?.let { throw MediaStudioException.invalid(it) }
            val ndc = FrameGeometry.cropNdc(crop, size)
            effects += Crop(ndc[0], ndc[1], ndc[2], ndc[3])
            size = FrameSize(crop.width, crop.height)
        }
        if (spec.rotate != 0) {
            // media3 rotates counterclockwise. zh-CN: media3 按逆时针旋转.
            effects += ScaleAndRotateTransformation.Builder().setRotationDegrees(-spec.rotate.toFloat()).build()
            size = FrameGeometry.rotate(size, spec.rotate)
        }
        if (spec.flip != Flip.NONE) {
            effects += ScaleAndRotateTransformation.Builder()
                .setScale(if (spec.flip.horizontal) -1f else 1f, if (spec.flip.vertical) -1f else 1f)
                .build()
        }
        val (target, layout) = when {
            spec.width != null || spec.height != null -> FrameGeometry.target(spec.width, spec.height, size) to layoutOf(spec.fit)
            spec.scale != null -> FrameGeometry.scale(size, spec.scale) to Presentation.LAYOUT_STRETCH_TO_FIT
            else -> FrameGeometry.even(size) to Presentation.LAYOUT_STRETCH_TO_FIT
        }
        FrameGeometry.sizeProblem(target)?.let { throw MediaStudioException.invalid(it) }
        effects += Presentation.createForWidthAndHeight(target.width, target.height, layout)
        val item = EditedMediaItem.Builder(mediaItemOf(input.path)).setEffects(Effects(emptyList(), effects)).build()
        return Plan(
            single(item),
            TransformerExport.Settings(
                videoMime = spec.video.codec.mime,
                audioMime = if (input.hasAudio) MimeTypes.AUDIO_AAC else null,
                videoBitRate = spec.video.bitRate ?: BitRates.estimateVideo(target.width, target.height, input.frameRate, spec.video.codec),
            ),
        )
    }

    private fun transcode(spec: OperationSpec.Transcode, input: MediaInfo): Plan {
        val videoEffects = mutableListOf<Effect>()
        var videoMime: String? = null
        var videoBitRate: Int? = null
        if (input.hasVideo) {
            val source = displaySize(input)
            val resized = spec.width != null || spec.height != null
            val target = if (resized) FrameGeometry.target(spec.width, spec.height, source) else FrameGeometry.even(source)
            FrameGeometry.sizeProblem(target)?.let { throw MediaStudioException.invalid(it) }
            if (resized || target != source) {
                videoEffects += Presentation.createForWidthAndHeight(target.width, target.height, if (resized) layoutOf(spec.fit) else Presentation.LAYOUT_STRETCH_TO_FIT)
            }
            var frameRate = input.frameRate
            spec.video.frameRate?.let { requested ->
                val inputRate = input.frameRate
                if (inputRate == null || requested < inputRate - FRAME_RATE_TOLERANCE) {
                    videoEffects += FrameDropEffect.createDefaultFrameDropEffect(requested.toFloat())
                    frameRate = requested
                } else {
                    notes += "options.frameRate $requested is not below the input frame rate $inputRate; the input rate stays"
                }
            }
            videoMime = spec.video.codec.mime
            videoBitRate = spec.video.bitRate ?: BitRates.estimateVideo(target.width, target.height, frameRate, spec.video.codec)
        } else if (spec.width != null || spec.height != null || spec.video.frameRate != null || spec.video.bitRate != null) {
            notes += "the input has no video track; the video options were ignored"
        }
        val audioProcessors = if (input.hasAudio) audioProcessors(spec.audio) else emptyList()
        val audioBitRate = if (input.hasAudio && spec.audio != AudioEncoding()) {
            spec.audio.bitRate ?: BitRates.aac(spec.audio.channels ?: input.channels ?: 2)
        } else {
            null
        }
        val item = EditedMediaItem.Builder(mediaItemOf(input.path)).setEffects(Effects(audioProcessors, videoEffects)).build()
        return Plan(
            single(item),
            TransformerExport.Settings(
                videoMime = videoMime,
                audioMime = if (input.hasAudio) MimeTypes.AUDIO_AAC else null,
                videoBitRate = videoBitRate,
                audioBitRate = audioBitRate,
            ),
        )
    }

    private fun removeAudio(input: MediaInfo): Plan {
        requireVideo(input, MediaStudioOps.REMOVE_AUDIO)
        val item = EditedMediaItem.Builder(mediaItemOf(input.path)).setRemoveAudio(true).build()
        return Plan(single(item), TransformerExport.Settings(), MediaStudioContract.STAGE_MUXING)
    }

    /** M4A only; AAC input passes through unless audio options are given. zh-CN: 仅 M4A; 未给音频选项时 AAC 输入直通. */
    private fun extractAudio(spec: OperationSpec.ExtractAudio, input: MediaInfo): Plan {
        val explicit = spec.audio != AudioEncoding()
        val item = EditedMediaItem.Builder(mediaItemOf(input.path))
            .setRemoveVideo(true)
            .setEffects(Effects(audioProcessors(spec.audio), emptyList()))
            .build()
        return Plan(
            single(item),
            TransformerExport.Settings(
                audioMime = MimeTypes.AUDIO_AAC,
                audioBitRate = if (explicit) spec.audio.bitRate ?: BitRates.aac(spec.audio.channels ?: input.channels ?: 2) else null,
            ),
            if (explicit) MediaStudioContract.STAGE_ENCODING else MediaStudioContract.STAGE_MUXING,
        )
    }

    /**
     * One full-frame bitmap overlay after a Presentation to the even display size, so pixel positions and percentages
     * refer to the output frame; the audio passes through where media3 can.
     * zh-CN: 先用 Presentation 调到偶数的显示尺寸, 再加一个整帧位图叠加层, 使像素位置与百分比都以输出帧为准; 音频在 media3
     * 能直通时直通.
     */
    private fun overlay(spec: OperationSpec.Overlay, input: MediaInfo): Plan {
        requireVideo(input, MediaStudioOps.OVERLAY)
        val size = FrameGeometry.even(displaySize(input))
        FrameGeometry.sizeProblem(size)?.let { throw MediaStudioException.invalid(it) }
        spec.items.forEachIndexed { index, item ->
            if (item.placement.fromMillis >= input.durationMillis) {
                notes += "overlay $index starts at ${item.placement.fromMillis} ms, after the end of the input (${input.durationMillis} ms); it never shows"
            }
        }
        val renderer = OverlayRenderer(spec.items, size).also { resources += it }
        val effects = listOf<Effect>(
            Presentation.createForWidthAndHeight(size.width, size.height, Presentation.LAYOUT_STRETCH_TO_FIT),
            OverlayEffect(listOf(renderer.overlay())),
        )
        val item = EditedMediaItem.Builder(mediaItemOf(input.path)).setEffects(Effects(emptyList(), effects)).build()
        return Plan(
            single(item),
            TransformerExport.Settings(
                videoMime = spec.video.codec.mime,
                audioMime = if (input.hasAudio) MimeTypes.AUDIO_AAC else null,
                videoBitRate = spec.video.bitRate ?: BitRates.estimateVideo(size.width, size.height, input.frameRate, spec.video.codec),
            ),
        )
    }

    /**
     * `burnSubtitles` (roadmap P3.3, D31): the cues drawn by a [SubtitlePainter] in the same full-frame bitmap overlay as
     * [overlay], after a Presentation to the even display size; the audio passes through where media3 can.
     * zh-CN: `burnSubtitles` (路线图 P3.3, D31): 由 [SubtitlePainter] 把 cue 画进与 [overlay] 相同的整帧位图叠加层, 之前先用
     * Presentation 调到偶数的显示尺寸; 音频在 media3 能直通时直通.
     */
    private fun burnSubtitles(spec: OperationSpec.BurnSubtitles, input: MediaInfo): Plan {
        requireVideo(input, MediaStudioOps.BURN_SUBTITLES)
        val size = FrameGeometry.even(displaySize(input))
        FrameGeometry.sizeProblem(size)?.let { throw MediaStudioException.invalid(it) }
        val warnings = SubtitleWarnings()
        val track = SubtitleFiles.load(spec.subtitles.source, warnings)
        val painter = SubtitlePainter(track, spec.subtitles.style, warnings)
        if (track.cues.none { it.startMillis < input.durationMillis }) {
            notes += "no subtitle cue starts before the end of the input (${input.durationMillis} ms); nothing was burned"
        }
        notes += warnings.list()
        val renderer = OverlayRenderer(emptyList(), size, painter).also { resources += it }
        val effects = listOf<Effect>(
            Presentation.createForWidthAndHeight(size.width, size.height, Presentation.LAYOUT_STRETCH_TO_FIT),
            OverlayEffect(listOf(renderer.overlay())),
        )
        val item = EditedMediaItem.Builder(mediaItemOf(input.path)).setEffects(Effects(emptyList(), effects)).build()
        return Plan(
            single(item),
            TransformerExport.Settings(
                videoMime = spec.video.codec.mime,
                audioMime = if (input.hasAudio) MimeTypes.AUDIO_AAC else null,
                videoBitRate = spec.video.bitRate ?: BitRates.estimateVideo(size.width, size.height, input.frameRate, spec.video.codec),
            ),
        )
    }

    private fun audioProcessors(audio: AudioEncoding): List<AudioProcessor> = buildList {
        audio.channels?.let { add(AudioProcessors.channelMixer(it)) }
        audio.sampleRate?.let { rate -> add(SonicAudioProcessor().apply { setOutputSampleRateHz(rate) }) }
    }

    private class ConstantSpeed(private val factor: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long): Float = factor
        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
    }

    private companion object {

        const val NOTE_DETAIL_CHARS = 200

        const val FRAME_RATE_TOLERANCE = 0.01

        fun requireVideo(info: MediaInfo, op: String) {
            if (!info.hasVideo) throw MediaStudioException.unsupported("$op needs a video track; ${info.path} has none")
        }

        fun displaySize(info: MediaInfo) = FrameSize(info.width ?: 0, info.height ?: 0)

        fun uriOf(path: String): Uri = Uri.fromFile(File(path))

        fun mediaItemOf(path: String): MediaItem = MediaItem.fromUri(uriOf(path))

        fun single(item: EditedMediaItem): Composition =
            Composition.Builder(listOf(EditedMediaItemSequence.Builder(listOf(item)).build())).build()

        fun layoutOf(fit: Fit): Int = when (fit) {
            Fit.CONTAIN -> Presentation.LAYOUT_SCALE_TO_FIT
            Fit.COVER -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            Fit.STRETCH -> Presentation.LAYOUT_STRETCH_TO_FIT
        }

    }

}

/** Audio processors shared by both pipelines. zh-CN: 两条管线共用的音频处理器. */
internal object AudioProcessors {

    private const val MAX_INPUT_CHANNELS = 8

    /**
     * Mixes any input of 1 - 8 channels to [outputChannels]: the media3 constant gain matrix where it has one, otherwise
     * the first front channels.
     * zh-CN: 把 1 - 8 声道的输入混为 [outputChannels] 声道: media3 有恒定增益矩阵时用它, 否则保留前置的前几个声道.
     */
    fun channelMixer(outputChannels: Int): ChannelMixingAudioProcessor = ChannelMixingAudioProcessor().apply {
        for (input in 1..MAX_INPUT_CHANNELS) putChannelMixingMatrix(matrix(input, outputChannels))
    }

    fun matrix(inputChannels: Int, outputChannels: Int): ChannelMixingMatrix = try {
        ChannelMixingMatrix.createForConstantGain(inputChannels, outputChannels)
    } catch (_: UnsupportedOperationException) {
        // Row major, rows are input channels. zh-CN: 行优先, 行对应输入声道.
        val coefficients = FloatArray(inputChannels * outputChannels)
        for (channel in 0 until minOf(inputChannels, outputChannels)) coefficients[channel * outputChannels + channel] = 1f
        ChannelMixingMatrix(inputChannels, outputChannels, coefficients)
    }

}

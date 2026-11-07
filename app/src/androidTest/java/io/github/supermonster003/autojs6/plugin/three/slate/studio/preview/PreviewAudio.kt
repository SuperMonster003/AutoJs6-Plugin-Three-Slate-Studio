package io.github.supermonster003.autojs6.plugin.three.slate.studio.preview

import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioEncoding
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioTargets
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineLayout
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.MixSource
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.PcmMixer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.PcmSinks
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The audio of the timeline preview (roadmap P4.3): the same two-pass mix as `compose` (`ComposeOperation.mixAudio`,
 * appendix D Q17 (c)) into one 16-bit WAV, so the preview sounds like the export: video clips with an audio track and a
 * volume above 0 (at their speed, cross-faded over their transitions) and the audio clips with their fades. [mix]
 * blocks; [cancel] stops it from another thread (the partial file is deleted).
 * zh-CN: 时间线预览的音频 (路线图 P4.3): 与 `compose` 相同的两遍混音 (`ComposeOperation.mixAudio`, 附录 D Q17 (c)), 混成一个 16 位
 * WAV, 预览听起来与导出一致: 有音轨且音量大于 0 的视频段 (按速度, 在转场处交叉淡化) 与带淡入淡出的音频段. [mix] 阻塞; [cancel]
 * 可在其他线程停止它 (删除未完成的文件).
 */
internal class PreviewAudio {

    private val cancelled = AtomicBoolean()

    private val job = object : TaskRunner.JobContext {
        override val taskId: Int = -1
        override val isCancelled: Boolean get() = cancelled.get()
        override fun progress(percent: Double, stage: String) = Unit
        override fun setCancelHandler(handler: (() -> Unit)?) = Unit
    }

    fun cancel() = cancelled.set(true)

    /** The mix of [layout] written to [file], or null when nothing sounds. zh-CN: 写到 [file] 的 [layout] 混音, 无声时为 null. */
    fun mix(layout: TimelineLayout, file: File): File? {
        val sources = sources(layout) ?: return null
        val (target, list) = sources
        val mixer = PcmMixer(job, target, list, totalFrames = frames(layout.durationUs, target))
        val analysis = mixer.analyze { }
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        val sink = PcmSinks.open(AudioContainer.WAV, temporary.path, target.sampleRate, target.channels, null)
        var finished = false
        try {
            var buffer = ByteBuffer.allocateDirect(PcmMixer.BLOCK_FRAMES * target.channels * 2).order(ByteOrder.LITTLE_ENDIAN)
            mixer.write(analysis.scale, { }) { samples, count ->
                if (buffer.capacity() < count * 2) buffer = ByteBuffer.allocateDirect(count * 2).order(ByteOrder.LITTLE_ENDIAN)
                buffer.clear()
                buffer.asShortBuffer().put(samples, 0, count)
                buffer.limit(count * 2)
                sink.write(buffer)
            }
            sink.finish()
            finished = true
        } finally {
            if (!finished) {
                sink.close()
                temporary.delete()
            }
        }
        file.delete()
        if (!temporary.renameTo(file)) {
            temporary.delete()
            return null
        }
        return file
    }

    companion object {

        /**
         * The mix sources of [layout] and their common target, as `compose` builds them, or null when nothing sounds.
         * zh-CN: 与 `compose` 相同方式得到的 [layout] 混音来源与共同目标格式, 无声时为 null.
         */
        fun sources(layout: TimelineLayout): Pair<AudioTargets.Target, List<MixSource>>? {
            val clips = layout.videoTracks.flatten().filter { it.info.hasAudio && it.clip.volume > 0.0 }
            if (clips.isEmpty() && layout.audio.isEmpty()) return null
            val infos = clips.map { it.info } + layout.audio.map { it.info }
            val (rate, channels) = AudioTargets.common(infos.map { it.sampleRate }, infos.map { it.channels })
            val target = AudioTargets.of(AudioContainer.M4A, AudioEncoding(), rate, channels)
            val list = clips.map { placed ->
                MixSource(
                    path = placed.clip.src,
                    volume = placed.clip.volume.toFloat(),
                    offsetFrames = frames(placed.atUs, target),
                    startUs = placed.sourceStartUs,
                    endUs = placed.sourceEndUs,
                    speed = placed.clip.speed.toFloat(),
                    pitch = 1f,
                    lengthFrames = frames(placed.lengthUs, target),
                    fadeInFrames = placed.into?.let { frames(TimelineLayout.sideUs(it), target) } ?: 0L,
                    fadeOutFrames = placed.out?.let { frames(TimelineLayout.sideUs(it), target) } ?: 0L,
                )
            } + layout.audio.map { placed ->
                MixSource(
                    path = placed.clip.src,
                    volume = placed.clip.volume.toFloat(),
                    offsetFrames = frames(placed.atUs, target),
                    startUs = placed.sourceStartUs,
                    endUs = placed.sourceEndUs,
                    lengthFrames = frames(placed.lengthUs, target),
                    fadeInFrames = frames(placed.clip.fadeInMillis * 1_000L, target),
                    fadeOutFrames = frames(placed.clip.fadeOutMillis * 1_000L, target),
                )
            }
            return target to list
        }

        private fun frames(us: Long, target: AudioTargets.Target): Long = Math.round(us * target.sampleRate / 1_000_000.0)

    }

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioTargets
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.PcmMath
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import java.util.Locale

/**
 * One source of [PcmMixer]: the first audio track of [path], cut to source time [startUs, endUs), played at [speed]
 * ([pitch] 1 keeps the pitch), times [volume], starting at frame [offsetFrames] of the mix. [lengthFrames] caps the
 * frames it adds (null: until the decoder ends); [fadeInFrames] / [fadeOutFrames] are linear fades at its start / end,
 * the fade out needs [lengthFrames].
 * zh-CN: [PcmMixer] 的一个来源: [path] 的第一条音轨, 按源时间裁到 [startUs, endUs), 以 [speed] 播放 ([pitch] 为 1 时保持音高),
 * 乘以 [volume], 从混音的第 [offsetFrames] 帧开始. [lengthFrames] 限制其加入的帧数 (null 表示直到解码结束); [fadeInFrames] /
 * [fadeOutFrames] 为开头 / 结尾的线性淡入淡出, 淡出需要 [lengthFrames].
 */
internal data class MixSource(
    val path: String,
    val volume: Float = 1f,
    val offsetFrames: Long = 0L,
    val startUs: Long = 0L,
    val endUs: Long? = null,
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val lengthFrames: Long? = null,
    val fadeInFrames: Long = 0L,
    val fadeOutFrames: Long = 0L,
) {
    /** The gain of local frame [frame] (volume and fades). zh-CN: 本地第 [frame] 帧的增益 (音量与淡入淡出). */
    fun gain(frame: Long): Float {
        var gain = volume.toDouble()
        if (fadeInFrames > 0 && frame < fadeInFrames) gain *= frame.toDouble() / fadeInFrames
        if (fadeOutFrames > 0 && lengthFrames != null) {
            val remaining = lengthFrames - 1 - frame
            if (remaining < fadeOutFrames) gain *= remaining.coerceAtLeast(0).toDouble() / fadeOutFrames
        }
        return gain.toFloat()
    }

    val hasFades: Boolean get() = fadeInFrames > 0 || (fadeOutFrames > 0 && lengthFrames != null)
}

/**
 * Sums [sources] into one [target] stream in blocks of [BLOCK_FRAMES] frames (`audioMix` and the audio of `compose`).
 * A source decoder opens when the mix reaches its offset and closes when it ends, so only the sources that overlap run
 * at once. [mix] runs one pass; a mix takes two (appendix D Q17 (c)): the first pass ([analyze]) finds the peak, the
 * second scales by [PcmMath.mixScale] and writes. The decoded samples are the same in both passes, so the peak of the
 * first pass is the peak of the second. The mix lasts until the last source ends, or exactly [totalFrames] frames.
 * zh-CN: 以 [BLOCK_FRAMES] 帧为一块把 [sources] 相加为一条 [target] 流 (`audioMix` 与 `compose` 的音频). 来源的解码器在混音到达其起点时
 * 打开, 结束时关闭, 因此只有相互重叠的来源同时运行. [mix] 运行一遍; 一次混音分两遍 (附录 D Q17 (c)): 第一遍 ([analyze]) 求峰值,
 * 第二遍按 [PcmMath.mixScale] 缩放并写出. 两遍解码出的样本相同, 因此第一遍的峰值就是第二遍的峰值. 混音持续到最后一个来源结束,
 * 或恰好 [totalFrames] 帧.
 */
internal class PcmMixer(
    private val task: TaskRunner.JobContext,
    private val target: AudioTargets.Target,
    private val sources: List<MixSource>,
    private val totalFrames: Long? = null,
    expected: Long? = null,
) {

    data class Analysis(val frames: Long, val peak: Float) {
        val scale: Double get() = PcmMath.mixScale(peak)
    }

    private val channels = target.channels

    /** The frames the mix is expected to last, for progress. zh-CN: 预计的混音帧数, 用于进度. */
    val expectedFrames: Long = (expected ?: totalFrames ?: sources.maxOfOrNull { source ->
        source.offsetFrames + (source.lengthFrames ?: 0L)
    } ?: 0L).coerceAtLeast(1L)

    fun analyze(progress: (Double) -> Unit): Analysis {
        var peak = 0f
        val frames = mix(progress) { sum, count -> peak = maxOf(peak, PcmMath.peak(sum, count * channels)) }
        return Analysis(frames, peak)
    }

    /**
     * Writes the mix scaled by [scale] through [write] (samples, sample count).
     * zh-CN: 把按 [scale] 缩放的混音经 [write] (样本, 样本数) 写出.
     */
    fun write(scale: Double, progress: (Double) -> Unit, write: (ShortArray, Int) -> Unit): Long {
        val block = ShortArray(BLOCK_FRAMES * channels)
        return mix(progress) { sum, count ->
            PcmMath.clip(sum, count * channels, block, scale)
            write(block, count * channels)
        }
    }

    /**
     * One pass: hands every block to [use] as (sum, frame count) and returns the frames mixed.
     * zh-CN: 一遍: 把每一块以 (和, 帧数) 交给 [use], 返回混合的帧数.
     */
    fun mix(progress: (Double) -> Unit, use: (FloatArray, Int) -> Unit): Long {
        val pending = sources.sortedBy { it.offsetFrames }.toMutableList()
        val active = mutableListOf<Track>()
        val sum = FloatArray(BLOCK_FRAMES * channels)
        var position = 0L
        try {
            while (true) {
                task.throwIfCancelled()
                val limit = totalFrames?.let { (it - position).coerceAtMost(BLOCK_FRAMES.toLong()).toInt() } ?: BLOCK_FRAMES
                if (limit <= 0) break
                while (pending.isNotEmpty() && pending.first().offsetFrames < position + limit) {
                    active += Track(pending.removeAt(0))
                }
                sum.fill(0f)
                var length = if (totalFrames != null || pending.isNotEmpty()) limit else 0
                val iterator = active.iterator()
                while (iterator.hasNext()) {
                    val track = iterator.next()
                    val first = maxOf(track.source.offsetFrames - position, 0L).toInt()
                    var frame = first
                    while (frame < limit) {
                        val taken = track.add(sum, frame * channels, limit - frame)
                        if (taken == 0) break
                        frame += taken
                    }
                    if (track.done) {
                        track.close()
                        iterator.remove()
                        if (frame > first) length = maxOf(length, frame)
                    } else {
                        length = maxOf(length, frame)
                    }
                }
                if (length == 0) break
                use(sum, length)
                position += length
                progress(position.toDouble() / expectedFrames)
            }
        } finally {
            active.forEach { it.close() }
        }
        return position
    }

    /** One open source and its unread samples. zh-CN: 一个已打开的来源及其未读样本. */
    private inner class Track(val source: MixSource) {

        private val decoded = DecodedAudio(task, source.path, source.startUs, source.endUs, source.speed, source.pitch) { target }

        private var chunk: ShortArray? = null

        private var index = 0

        /** Local frames added so far. zh-CN: 已加入的本地帧数. */
        private var added = 0L

        var done = false
            private set

        /**
         * Adds up to [frames] frames into [sum] from sample [at]; 0 when the source is done.
         * zh-CN: 从样本 [at] 起把至多 [frames] 帧加入 [sum]; 来源结束时返回 0.
         */
        fun add(sum: FloatArray, at: Int, frames: Int): Int {
            if (done) return 0
            val cap = source.lengthFrames?.let { it - added }
            if (cap != null && cap <= 0) {
                done = true
                return 0
            }
            var current = chunk
            while (current == null || current.size - index < channels) {
                current = decoded.read()
                if (current == null) {
                    done = true
                    chunk = null
                    return 0
                }
                chunk = current
                index = 0
            }
            var count = minOf(frames, (current.size - index) / channels)
            if (cap != null) count = minOf(count.toLong(), cap).toInt()
            if (source.hasFades) {
                for (frame in 0 until count) {
                    val gain = source.gain(added + frame)
                    val from = index + frame * channels
                    val to = at + frame * channels
                    for (channel in 0 until channels) sum[to + channel] += current[from + channel] * gain
                }
            } else {
                PcmMath.accumulate(sum, at, current, index, count * channels, source.volume)
            }
            index += count * channels
            added += count
            return count
        }

        fun close() = decoded.close()

    }

    companion object {

        /** Frames per mix block. zh-CN: 每个混音块的帧数. */
        const val BLOCK_FRAMES = 4_096

        /** Share of the progress for the first pass. zh-CN: 第一遍所占的进度份额. */
        const val ANALYSIS_SHARE = 0.4

        /**
         * The warning of a mix that was scaled down (appendix D Q17 (c)), or null when it kept its level.
         * zh-CN: 混音被缩放时的警告 (附录 D Q17 (c)); 保持原电平时为 null.
         */
        fun scaledNote(analysis: Analysis, what: String = "the mix"): String? {
            val scale = analysis.scale
            if (scale >= 1.0) return null
            val peakDb = PcmMath.linearToDb(analysis.peak / PcmMath.FULL_SCALE)
            return String.format(
                Locale.ROOT,
                "%s peaked at %+.2f dBFS; it was scaled by %.2f dB to %.0f dBFS to avoid clipping",
                what,
                peakDb,
                PcmMath.linearToDb(scale),
                PcmMath.MIX_PEAK_DBFS,
            )
        }

    }

}

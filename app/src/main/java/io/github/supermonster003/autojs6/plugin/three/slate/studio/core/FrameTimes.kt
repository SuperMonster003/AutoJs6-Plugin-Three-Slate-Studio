package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import kotlin.math.ceil

/** When `frames` samples the input (roadmap P2.4). zh-CN: `frames` 在输入上的取帧时间 (路线图 P2.4). */
internal sealed interface FrameTimes {

    /** Explicit times of `at` or `times` ([key]), in the given order. zh-CN: `at` 或 `times` ([key]) 给出的时间, 按给出顺序. */
    data class Points(val millis: List<Double>, val key: String) : FrameTimes

    /**
     * Every [intervalMillis] from [startMillis] while before [endMillis] (exclusive, null = end of input), at most [count].
     * zh-CN: 从 [startMillis] 起每隔 [intervalMillis] 取一帧, 直到 [endMillis] 之前 (不含, null 为输入结尾), 最多 [count] 帧.
     */
    data class Every(val intervalMillis: Double, val startMillis: Double, val endMillis: Double?, val count: Int?) : FrameTimes

    /**
     * [count] frames spread evenly over [startMillis] - [endMillis], each in the middle of its slot.
     * zh-CN: 在 [startMillis] - [endMillis] 内均匀分布的 [count] 帧, 每帧取所在时段的中点.
     */
    data class Count(val count: Int, val startMillis: Double, val endMillis: Double?) : FrameTimes

}

/**
 * Turns [FrameTimes] and the `toGif` range into decode times once the input duration is known. Times after the end are
 * INVALID_OPTIONS, too many frames LIMIT_EXCEEDED (frames: [OperationOptions.MAX_FRAMES], GIF:
 * [MediaStudioLimits.MAX_GIF_FRAMES]); an end beyond the input is cut to the input.
 * zh-CN: 已知输入时长后, 把 [FrameTimes] 与 `toGif` 的范围换算为解码时间. 超过结尾的时间为 INVALID_OPTIONS, 帧数过多为
 * LIMIT_EXCEEDED (frames: [OperationOptions.MAX_FRAMES], GIF: [MediaStudioLimits.MAX_GIF_FRAMES]); 超出输入的结束时间截到输入结尾.
 */
internal object FrameSampling {

    /** Rounding slack so 3000 / 100 counts 30 frames, not 31. zh-CN: 取整余量, 使 3000 / 100 计为 30 帧而非 31 帧. */
    private const val EPSILON = 1e-9

    fun frameTimes(times: FrameTimes, durationMillis: Long): List<Double> = when (times) {
        is FrameTimes.Points -> times.millis.also { points ->
            points.forEachIndexed { index, time ->
                if (time > durationMillis) {
                    val name = if (times.key == "times") "options.times[$index]" else "options.${times.key}"
                    throw MediaStudioException.invalid("$name (${format(time)} ms) is after the end of the input ($durationMillis ms)")
                }
            }
        }
        is FrameTimes.Every -> {
            val end = rangeEnd(times.startMillis, times.endMillis, durationMillis)
            val all = ceil((end - times.startMillis) / times.intervalMillis - EPSILON).toLong().coerceAtLeast(1)
            val frames = times.count?.let { minOf(it.toLong(), all) } ?: all
            if (frames > OperationOptions.MAX_FRAMES) {
                throw OperationOptions.limit(
                    "frames would extract $frames frames, at most ${OperationOptions.MAX_FRAMES}; raise options.every or set options.count",
                )
            }
            List(frames.toInt()) { index -> times.startMillis + index * times.intervalMillis }
        }
        is FrameTimes.Count -> {
            val end = rangeEnd(times.startMillis, times.endMillis, durationMillis)
            val slot = (end - times.startMillis) / times.count
            List(times.count) { index -> times.startMillis + (index + 0.5) * slot }
        }
    }

    /** Frames of a GIF over [startMillis] - [endMillis] at [fps]. zh-CN: [fps] 下 [startMillis] - [endMillis] 内 GIF 的帧数. */
    fun gifFrameCount(startMillis: Double, endMillis: Double, fps: Int): Int {
        val frames = ceil((endMillis - startMillis) * fps / 1000.0 - EPSILON).toLong().coerceAtLeast(1)
        if (frames > MediaStudioLimits.MAX_GIF_FRAMES) {
            throw OperationOptions.limit(
                "toGif would write $frames frames, at most ${MediaStudioLimits.MAX_GIF_FRAMES}; shorten options.start - options.end or lower options.fps",
            )
        }
        return frames.toInt()
    }

    /** Decode times of the GIF frames; frame i starts at `start + i / fps`. zh-CN: GIF 各帧的解码时间; 第 i 帧始于 `start + i / fps`. */
    fun gifTimes(spec: OperationSpec.ToGif, durationMillis: Long): List<Double> {
        val end = rangeEnd(spec.startMillis, spec.endMillis, durationMillis)
        val frames = gifFrameCount(spec.startMillis, end, spec.fps)
        return List(frames) { index -> spec.startMillis + index * 1000.0 / spec.fps }
    }

    /**
     * The frame shown at [timeMillis]; the end of the input maps to its last frame.
     * zh-CN: [timeMillis] 处显示的帧; 输入结尾映射到最后一帧.
     */
    fun decodeMicros(timeMillis: Double, durationMillis: Long): Long =
        (minOf(timeMillis, (durationMillis - 1).coerceAtLeast(0).toDouble()) * 1000).toLong()

    private fun rangeEnd(startMillis: Double, endMillis: Double?, durationMillis: Long): Double {
        if (startMillis >= durationMillis) {
            throw MediaStudioException.invalid("options.start (${format(startMillis)} ms) is not before the end of the input ($durationMillis ms)")
        }
        return minOf(endMillis ?: durationMillis.toDouble(), durationMillis.toDouble())
    }

    private fun format(value: Double): String =
        if (value == Math.floor(value) && Math.abs(value) <= 9_007_199_254_740_992.0) value.toLong().toString() else value.toString()

}

// Copy of the AutoJs6 host kernel file app/src/main/java/org/autojs/autojs/core/record/Pcm16.kt at host commit
// 3ef5cec633 (roadmap P1.2; unchanged up to host 063b9f0b8a), kept by 3-Slate Studio roadmap D41 / P4.5; see README.md.
// Plugin change: [peak] for the level meter of the Studio recorder.
package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

/**
 * Interleaved PCM16 helpers of the audio capture.
 *
 * zh-CN: 音频采集使用的交错 PCM16 工具.
 */
internal object Pcm16 {

    /** Equal gain for each source avoids clipping when both are full scale. zh-CN: 两路等增益, 两路都满幅时也不削波. */
    fun mix(a: Short, b: Short): Short = ((a.toInt() + b.toInt()) / 2).toShort()

    /** Mixes [count] samples of [other] into [target] in place. zh-CN: 把 [other] 的前 [count] 个采样就地混入 [target]. */
    fun mixInto(target: ShortArray, other: ShortArray, count: Int) {
        for (i in 0 until count) target[i] = mix(target[i], other[i])
    }

    /**
     * Copies [frames] mono samples of [mono] to both channels of the interleaved stereo [stereo].
     *
     * zh-CN: 把 [mono] 的 [frames] 个单声道采样复制到交错立体声 [stereo] 的两个声道.
     */
    fun monoToStereo(mono: ShortArray, stereo: ShortArray, frames: Int) {
        for (i in frames - 1 downTo 0) {
            val sample = mono[i]
            stereo[2 * i] = sample
            stereo[2 * i + 1] = sample
        }
    }

    /** Presentation time (us) of frame [frames] at [sampleRate]. zh-CN: 第 [frames] 帧在 [sampleRate] 下的呈现时间 (微秒). */
    fun framesToMicros(frames: Long, sampleRate: Int): Long = frames * 1_000_000L / sampleRate

    /**
     * Plugin addition: peak of the first [count] samples as a fraction of full scale (0 - 1), for the level meter.
     * zh-CN: 插件新增: 前 [count] 个采样的峰值占满幅的比例 (0 - 1), 用于电平表.
     */
    fun peak(samples: ShortArray, count: Int): Float {
        var peak = 0
        for (i in 0 until count) {
            val value = samples[i].toInt()
            val magnitude = if (value < 0) -value else value
            if (magnitude > peak) peak = magnitude
        }
        return (peak / 32768f).coerceAtMost(1f)
    }
}

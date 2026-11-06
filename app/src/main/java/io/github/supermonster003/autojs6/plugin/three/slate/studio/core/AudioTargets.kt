package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

/**
 * The output format of the audio operations (roadmap P2.2 / P2.5, D15) when the options leave it open. Channels: WAV
 * keeps the source, the others keep at most 2. Sample rate: OGG runs at 48 kHz (the native Opus rate), M4A keeps the
 * source rate when AAC takes it (otherwise the next rate of [OperationOptions.SAMPLE_RATES], at most 48 kHz), WAV and
 * FLAC keep the source rate. Several inputs (`audioConcat`, `audioMix`) count as one source with the highest rate and
 * the most channels among them. Bit rate: AAC 128 kbps mono / 192 kbps stereo, capped at 6 bits per sample and channel
 * (the AAC LC maximum), Opus 64 / 96 kbps.
 * zh-CN: 选项未指定时音频操作 (路线图 P2.2 / P2.5, D15) 的输出格式. 声道: WAV 保留源声道数, 其他最多 2 个. 采样率: OGG 为
 * 48 kHz (Opus 原生采样率), M4A 在 AAC 支持时保留源采样率 (否则取 [OperationOptions.SAMPLE_RATES] 中更高的下一档, 至多 48 kHz),
 * WAV 与 FLAC 保留源采样率. 多个输入 (`audioConcat`, `audioMix`) 视为一个源, 取其中最高的采样率与最多的声道数. 码率: AAC 单声道
 * 128 kbps / 立体声 192 kbps, 且不超过每声道每样本 6 位 (AAC LC 的上限), Opus 64 / 96 kbps.
 */
internal object AudioTargets {

    data class Target(val sampleRate: Int, val channels: Int) {
        override fun toString() = "$sampleRate Hz, $channels ch"
    }

    const val OPUS_SAMPLE_RATE = 48_000

    /** Used when the probe gives no rate or channel count. zh-CN: 探测没有给出采样率或声道数时使用. */
    const val FALLBACK_SAMPLE_RATE = 44_100
    const val FALLBACK_CHANNELS = 2

    /** AAC LC carries at most 6144 bits per 1024-sample frame and channel. zh-CN: AAC LC 每声道每 1024 样本帧至多 6144 位. */
    private const val AAC_MAX_BITS_PER_SAMPLE = 6

    fun of(container: AudioContainer, audio: AudioEncoding, sourceRate: Int, sourceChannels: Int) = Target(
        sampleRate = audio.sampleRate ?: when (container) {
            AudioContainer.OGG -> OPUS_SAMPLE_RATE
            AudioContainer.M4A -> OperationOptions.SAMPLE_RATES.firstOrNull { it >= sourceRate } ?: OperationOptions.SAMPLE_RATES.last()
            AudioContainer.WAV, AudioContainer.FLAC -> sourceRate
        },
        channels = audio.channels ?: if (container == AudioContainer.WAV) sourceChannels else minOf(sourceChannels, 2),
    )

    /** One source format for several inputs. zh-CN: 多个输入合成的一个源格式. */
    fun common(rates: List<Int?>, channels: List<Int?>): Pair<Int, Int> =
        (rates.filterNotNull().filter { it > 0 }.maxOrNull() ?: FALLBACK_SAMPLE_RATE) to
            (channels.filterNotNull().filter { it > 0 }.maxOrNull() ?: FALLBACK_CHANNELS)

    /** Null for the lossless containers. zh-CN: 无损容器为 null. */
    fun bitRate(container: AudioContainer, audio: AudioEncoding, target: Target): Int? = audio.bitRate ?: when (container) {
        AudioContainer.M4A -> minOf(BitRates.aac(target.channels), AAC_MAX_BITS_PER_SAMPLE * target.sampleRate * target.channels)
        AudioContainer.OGG -> BitRates.opus(target.channels)
        AudioContainer.WAV, AudioContainer.FLAC -> null
    }

    fun frames(millis: Long, sampleRate: Int): Long = Math.round(millis * sampleRate / 1000.0)

}

/**
 * Sample arithmetic of the audio operations on interleaved 16-bit PCM: mixing with per-track volume, gain with linear
 * fades, peak normalization. Results are rounded and clamped to 16 bits; the functions return how many samples were
 * clipped (appendix D Q17 (a): the mix is a plain sum, clipping is reported as a warning).
 * zh-CN: 音频操作在交错 16 位 PCM 上的样本运算: 按轨音量混音, 带线性淡入淡出的增益, 峰值归一化. 结果四舍五入并截断到 16 位;
 * 函数返回被削波的样本数 (附录 D Q17 (a): 混音为直接相加, 削波以警告报告).
 */
internal object PcmMath {

    /** 0 dBFS, the largest positive sample. zh-CN: 0 dBFS, 最大正样本. */
    const val FULL_SCALE = 32_767.0

    fun dbfsToLinear(dbfs: Double): Double = Math.pow(10.0, dbfs / 20.0)

    /** The largest absolute sample of [samples]. zh-CN: [samples] 中绝对值最大的样本. */
    fun peak(samples: ShortArray, count: Int = samples.size): Int {
        var peak = 0
        for (index in 0 until count) {
            val value = Math.abs(samples[index].toInt())
            if (value > peak) peak = value
        }
        return peak
    }

    /**
     * The factor that brings [peak] to [targetDbfs]; null for silence (nothing to scale).
     * zh-CN: 把 [peak] 调到 [targetDbfs] 的倍数; 静音时为 null (无可缩放).
     */
    fun normalizeFactor(peak: Int, targetDbfs: Double): Double? =
        if (peak <= 0) null else FULL_SCALE * dbfsToLinear(targetDbfs) / peak

    /**
     * Linear fades over whole frames: the fade in rises from 0 at the first frame, the fade out reaches 0 at the last of
     * [totalFrames] (unknown total: no fade out); overlapping fades multiply.
     * zh-CN: 以整帧计的线性淡入淡出: 淡入从第一帧的 0 起升, 淡出在 [totalFrames] 的最后一帧降到 0 (总帧数未知时不淡出); 两者重叠时相乘.
     */
    data class Fades(val inFrames: Long, val outFrames: Long, val totalFrames: Long?) {

        fun gain(frame: Long): Double {
            var gain = 1.0
            if (inFrames > 0 && frame < inFrames) gain *= frame.toDouble() / inFrames
            if (outFrames > 0 && totalFrames != null) {
                val remaining = totalFrames - 1 - frame
                if (remaining < outFrames) gain *= remaining.coerceAtLeast(0).toDouble() / outFrames
            }
            return gain
        }

        val isEmpty: Boolean get() = inFrames <= 0 && (outFrames <= 0 || totalFrames == null)
    }

    /**
     * Scales [samples] in place by [factor] and [fades], [firstFrame] being the index of their first frame in the
     * stream; returns the number of clipped samples.
     * zh-CN: 按 [factor] 与 [fades] 原地缩放 [samples], [firstFrame] 为其首帧在流中的序号; 返回被削波的样本数.
     */
    fun applyGain(samples: ShortArray, channels: Int, firstFrame: Long, factor: Double, fades: Fades?): Int {
        val frames = samples.size / channels
        var clipped = 0
        for (frame in 0 until frames) {
            val gain = factor * (fades?.gain(firstFrame + frame) ?: 1.0)
            val base = frame * channels
            for (channel in 0 until channels) {
                val value = Math.round(samples[base + channel] * gain)
                samples[base + channel] = when {
                    value > Short.MAX_VALUE -> Short.MAX_VALUE.also { clipped++ }
                    value < Short.MIN_VALUE -> Short.MIN_VALUE.also { clipped++ }
                    else -> value.toShort()
                }
            }
        }
        return clipped
    }

    /**
     * Adds [count] samples of [samples] from [from], times [volume], into [mix] from [mixFrom].
     * zh-CN: 把 [samples] 自 [from] 起的 [count] 个样本乘以 [volume] 后加到 [mix] 自 [mixFrom] 起的位置.
     */
    fun accumulate(mix: FloatArray, mixFrom: Int, samples: ShortArray, from: Int, count: Int, volume: Float) {
        for (index in 0 until count) mix[mixFrom + index] += samples[from + index] * volume
    }

    /**
     * Rounds the first [count] values of [mix] into [out] with clamping; returns the number of clipped samples.
     * zh-CN: 把 [mix] 前 [count] 个值四舍五入写入 [out] 并截断; 返回被削波的样本数.
     */
    fun clip(mix: FloatArray, count: Int, out: ShortArray): Int {
        var clipped = 0
        for (index in 0 until count) {
            val value = Math.round(mix[index])
            out[index] = when {
                value > Short.MAX_VALUE -> Short.MAX_VALUE.also { clipped++ }
                value < Short.MIN_VALUE -> Short.MIN_VALUE.also { clipped++ }
                else -> value.toShort()
            }
        }
        return clipped
    }

}

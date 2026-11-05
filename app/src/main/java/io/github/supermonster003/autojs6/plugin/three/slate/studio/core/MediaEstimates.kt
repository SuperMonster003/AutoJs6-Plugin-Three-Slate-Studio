package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioLimits

/**
 * Default bit rates of roadmap D29: 8 Mbps for 1080p30 H.264, proportional to the pixel count, scaled by the frame rate
 * (0.5x - 2x of 30 fps) and by 0.6 for H.265 (appendix D Q12), clamped to 200 kbps - 100 Mbps.
 * zh-CN: 路线图 D29 的默认码率: 1080p30 H.264 为 8 Mbps, 与像素数成正比, 按帧率缩放 (30 fps 的 0.5 - 2 倍), H.265 再乘 0.6
 * (附录 D Q12), 限制在 200 kbps - 100 Mbps.
 */
internal object BitRates {

    const val REFERENCE_BIT_RATE = 8_000_000
    const val REFERENCE_PIXELS = 1920 * 1080
    const val REFERENCE_FRAME_RATE = 30.0
    const val H265_FACTOR = 0.6
    const val MIN_VIDEO = 200_000
    const val MAX_VIDEO = 100_000_000

    fun estimateVideo(width: Int, height: Int, frameRate: Double?, codec: VideoCodec): Int {
        val pixels = width.toDouble() * height.toDouble()
        val rate = frameRate?.takeIf { it > 0 && !it.isInfinite() } ?: REFERENCE_FRAME_RATE
        val fpsFactor = (rate / REFERENCE_FRAME_RATE).coerceIn(0.5, 2.0)
        val codecFactor = if (codec == VideoCodec.H265) H265_FACTOR else 1.0
        val estimate = REFERENCE_BIT_RATE * pixels / REFERENCE_PIXELS * fpsFactor * codecFactor
        return Math.round(estimate).coerceIn(MIN_VIDEO.toLong(), MAX_VIDEO.toLong()).toInt()
    }

    /** AAC when re-encoding: 128 kbps mono, 192 kbps stereo (the host recording defaults of Q1). zh-CN: 重编码时的 AAC: 单声道 128 kbps, 立体声 192 kbps (宿主录音 Q1 的默认值). */
    fun aac(channels: Int): Int = if (channels >= 2) 192_000 else 128_000

    /** Opus for OGG: 64 kbps mono, 96 kbps stereo. zh-CN: OGG 用的 Opus: 单声道 64 kbps, 立体声 96 kbps. */
    fun opus(channels: Int): Int = if (channels >= 2) 96_000 else 64_000

}

/**
 * The `warnings[]` of a result (roadmap appendix C.3, Q6): the bare code `OUTPUT_LARGE` when the output or one input
 * exceeds 2 GiB, `DURATION_LONG` when the output or one input exceeds 60 minutes, so scripts can test
 * `warnings.includes("OUTPUT_LARGE")`. Free-text notes (an encoder fallback) follow the codes; the task never fails for
 * them.
 * zh-CN: 结果的 `warnings[]` (路线图附录 C.3, Q6): 输出或单个输入超过 2 GiB 时为裸码 `OUTPUT_LARGE`, 输出或单个输入超过 60 分钟
 * 时为 `DURATION_LONG`, 脚本可用 `warnings.includes("OUTPUT_LARGE")` 判断. 自由文本说明 (编码器回退) 排在警告码之后; 任务不会
 * 因此失败.
 */
internal object MediaWarnings {

    data class Media(val sizeBytes: Long, val durationMillis: Long)

    fun of(output: Media, inputs: List<Media>): List<String> {
        val all = listOf(output) + inputs
        return listOfNotNull(
            MediaStudioContract.WARNING_OUTPUT_LARGE.takeIf { all.any { it.sizeBytes > MediaStudioLimits.OUTPUT_SIZE_WARNING_BYTES } },
            MediaStudioContract.WARNING_DURATION_LONG.takeIf { all.any { it.durationMillis > MediaStudioLimits.DURATION_WARNING_MILLIS } },
        )
    }

}

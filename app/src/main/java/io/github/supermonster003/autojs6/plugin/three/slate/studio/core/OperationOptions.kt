package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import org.autojs.plugin.mediastudio.api.MediaStudioOps

/** Video codecs of roadmap D29 / Q7: H.264 by default, H.265 when asked. zh-CN: 路线图 D29 / Q7 的视频编码: 默认 H.264, 可选 H.265. */
internal enum class VideoCodec(val id: String, val mime: String) {
    H264("h264", "video/avc"),
    H265("h265", "video/hevc"),
    ;

    companion object {
        fun of(id: String): VideoCodec = entries.first { it.id == id }
    }
}

/** How a frame fills a target size that has another aspect ratio. zh-CN: 帧填充宽高比不同的目标尺寸的方式. */
internal enum class Fit(val id: String) {
    /** Letterbox, the whole frame stays visible. zh-CN: 加黑边, 整帧可见. */
    CONTAIN("contain"),

    /** Fill and crop the overflow. zh-CN: 填满并裁掉溢出部分. */
    COVER("cover"),

    /** Fill and distort. zh-CN: 填满并拉伸变形. */
    STRETCH("stretch"),
    ;

    companion object {
        fun of(id: String): Fit = entries.first { it.id == id }
    }
}

internal enum class Flip(val id: String, val horizontal: Boolean, val vertical: Boolean) {
    NONE("none", false, false),
    HORIZONTAL("horizontal", true, false),
    VERTICAL("vertical", false, true),
    BOTH("both", true, true),
    ;

    companion object {
        fun of(id: String): Flip = entries.first { it.id == id }
    }
}

/** Audio outputs of `extractAudio` (roadmap P2.2, D15). zh-CN: `extractAudio` 的音频输出 (路线图 P2.2, D15). */
internal enum class AudioContainer(val id: String, val mime: String) {
    M4A("m4a", "audio/mp4"),
    WAV("wav", "audio/wav"),
    FLAC("flac", "audio/flac"),
    OGG("ogg", "audio/ogg"),
    ;

    val extension: String get() = id

    companion object {
        fun ofExtension(extension: String): AudioContainer? = entries.firstOrNull { it.id == extension.lowercase() }
    }
}

internal data class VideoEncoding(val codec: VideoCodec = VideoCodec.H264, val bitRate: Int? = null, val frameRate: Double? = null)

internal data class AudioEncoding(val bitRate: Int? = null, val sampleRate: Int? = null, val channels: Int? = null)

/** A crop rectangle in pixels of the displayed (rotation applied) source frame. zh-CN: 以源显示帧 (已应用旋转) 像素表示的裁切矩形. */
internal data class CropRect(val x: Int, val y: Int, val width: Int, val height: Int)

/** The validated options of one operation. zh-CN: 一个操作经校验的选项. */
internal sealed interface OperationSpec {

    /** [endMillis] is exclusive and null for "until the end". zh-CN: [endMillis] 不含, null 表示到结尾. */
    data class Trim(val startMillis: Long, val endMillis: Long?) : OperationSpec

    data class Concat(val width: Int?, val height: Int?, val fit: Fit, val video: VideoEncoding) : OperationSpec

    data class Speed(val factor: Double, val keepPitch: Boolean, val video: VideoEncoding) : OperationSpec

    /** [rotate] is clockwise, one of 0 / 90 / 180 / 270. zh-CN: [rotate] 为顺时针, 取 0 / 90 / 180 / 270. */
    data class Transform(
        val width: Int?,
        val height: Int?,
        val scale: Double?,
        val rotate: Int,
        val crop: CropRect?,
        val flip: Flip,
        val fit: Fit,
        val video: VideoEncoding,
    ) : OperationSpec

    data class Transcode(val width: Int?, val height: Int?, val fit: Fit, val video: VideoEncoding, val audio: AudioEncoding) : OperationSpec

    data object RemoveAudio : OperationSpec

    data class ExtractAudio(val container: AudioContainer, val audio: AudioEncoding) : OperationSpec

}

/**
 * The option schemas of the operations of roadmap P2.2 (appendix B.1). The host script module (roadmap P1.3) applies the
 * same rules before it submits, the plugin checks again (roadmap 5.2): unknown keys and values outside the ranges are
 * INVALID_OPTIONS, numbers may arrive as Double (JSON.stringify), integer options need an integral value. The rules are
 * copied into the hand-over note `3-slate-p2-notes.md` for the host.
 * zh-CN: 路线图 P2.2 各操作的选项模式 (附录 B.1). 宿主脚本模块 (路线图 P1.3) 提交前按同一规则校验, 插件再校验一次 (路线图
 * 5.2): 未知键与越界值为 INVALID_OPTIONS, 数字可能以 Double 到达 (JSON.stringify), 整数选项要求整数值. 规则抄在交接笔记
 * `3-slate-p2-notes.md` 供宿主照抄.
 */
internal object OperationOptions {

    const val MIN_DIMENSION = 16
    const val MAX_DIMENSION = 8192
    const val MAX_SCALE = 8.0
    const val MIN_VIDEO_BIT_RATE = 100_000
    const val MAX_VIDEO_BIT_RATE = 200_000_000
    const val MIN_AUDIO_BIT_RATE = 8_000
    const val MAX_AUDIO_BIT_RATE = 512_000
    const val MIN_FRAME_RATE = 1.0
    const val MAX_FRAME_RATE = 120.0

    /** 2^53: larger JSON numbers are not exact. zh-CN: 2^53: 更大的 JSON 数字不精确. */
    const val MAX_TIME_MILLIS = 9_007_199_254_740_992L

    val SAMPLE_RATES: List<Int> = listOf(8_000, 11_025, 12_000, 16_000, 22_050, 24_000, 32_000, 44_100, 48_000)

    /** The rates of the platform Opus encoder. zh-CN: 平台 Opus 编码器支持的采样率. */
    val OPUS_SAMPLE_RATES: List<Int> = listOf(8_000, 12_000, 16_000, 24_000, 48_000)

    val CHANNELS: List<Int> = listOf(1, 2)

    /** Single input, single output video operations. zh-CN: 单输入单输出的视频操作. */
    val SINGLE_INPUT_OPS: Set<String> = setOf(
        MediaStudioOps.TRIM,
        MediaStudioOps.SPEED,
        MediaStudioOps.TRANSFORM,
        MediaStudioOps.TRANSCODE,
        MediaStudioOps.REMOVE_AUDIO,
        MediaStudioOps.EXTRACT_AUDIO,
    )

    /** Operations submit runs in this build. zh-CN: 本构建中 submit 可执行的操作. */
    val SUBMIT_OPS: List<String> = listOf(
        MediaStudioOps.TRIM,
        MediaStudioOps.CONCAT,
        MediaStudioOps.SPEED,
        MediaStudioOps.TRANSFORM,
        MediaStudioOps.TRANSCODE,
        MediaStudioOps.REMOVE_AUDIO,
        MediaStudioOps.EXTRACT_AUDIO,
    )

    /**
     * Parses [options] of [op] (already a known operation of [SUBMIT_OPS]) for [inputCount] inputs.
     * zh-CN: 解析 [op] (已知且属于 [SUBMIT_OPS]) 在 [inputCount] 个输入下的 [options].
     */
    fun parse(op: String, inputCount: Int, options: Map<String, Any?>): OperationSpec {
        if (op in SINGLE_INPUT_OPS && inputCount != 1) {
            throw MediaStudioException.invalid("$op takes exactly 1 input, got $inputCount")
        }
        if (op == MediaStudioOps.CONCAT && inputCount < 2) {
            throw MediaStudioException.invalid("concat takes at least 2 inputs, got $inputCount")
        }
        val reader = OptionReader(options, "options.")
        val spec = when (op) {
            MediaStudioOps.TRIM -> trim(reader)
            MediaStudioOps.CONCAT -> OperationSpec.Concat(
                width = reader.dimension("width"),
                height = reader.dimension("height"),
                fit = reader.fit(),
                video = reader.videoEncoding(),
            )
            MediaStudioOps.SPEED -> OperationSpec.Speed(
                factor = reader.double("factor", MediaStudioLimits.MIN_SPEED_FACTOR, MediaStudioLimits.MAX_SPEED_FACTOR)
                    ?: throw MediaStudioException.invalid("options.factor is required"),
                keepPitch = reader.bool("keepPitch") ?: true,
                video = reader.videoEncoding(),
            )
            MediaStudioOps.TRANSFORM -> transform(reader)
            MediaStudioOps.TRANSCODE -> OperationSpec.Transcode(
                width = reader.dimension("width"),
                height = reader.dimension("height"),
                fit = reader.fit(),
                video = reader.videoEncoding(withFrameRate = true),
                audio = reader.child("audio")?.let { audio -> audio.audioEncoding(SAMPLE_RATES).also { audio.finish() } } ?: AudioEncoding(),
            )
            MediaStudioOps.REMOVE_AUDIO -> OperationSpec.RemoveAudio
            MediaStudioOps.EXTRACT_AUDIO -> extractAudio(reader)
            else -> throw IllegalArgumentException("not a submit operation: $op")
        }
        reader.finish()
        return spec
    }

    private fun trim(reader: OptionReader): OperationSpec.Trim {
        val start = reader.long("start", 0, MAX_TIME_MILLIS)
        val end = reader.long("end", 1, MAX_TIME_MILLIS)
        val duration = reader.long("duration", 1, MAX_TIME_MILLIS)
        if (start == null && end == null && duration == null) {
            throw MediaStudioException.invalid("trim needs options.start, options.end or options.duration")
        }
        if (end != null && duration != null) {
            throw MediaStudioException.invalid("options.end and options.duration are exclusive")
        }
        val startMillis = start ?: 0L
        if (end != null && end <= startMillis) {
            throw MediaStudioException.invalid("options.end ($end) must be greater than options.start ($startMillis)")
        }
        val endMillis = end ?: duration?.let { startMillis + it }?.also {
            if (it > MAX_TIME_MILLIS) throw MediaStudioException.invalid("options.start + options.duration is too large")
        }
        return OperationSpec.Trim(startMillis, endMillis)
    }

    private fun transform(reader: OptionReader): OperationSpec.Transform {
        val width = reader.dimension("width")
        val height = reader.dimension("height")
        val scale = reader.double("scale", 0.0, MAX_SCALE, minExclusive = true)
        if (scale != null && (width != null || height != null)) {
            throw MediaStudioException.invalid("options.scale and options.width / options.height are exclusive")
        }
        val rotate = reader.long("rotate", -MAX_TIME_MILLIS, MAX_TIME_MILLIS)?.let { degrees ->
            if (degrees % 90L != 0L) throw MediaStudioException.invalid("options.rotate must be a multiple of 90, got $degrees")
            (((degrees % 360L) + 360L) % 360L).toInt()
        } ?: 0
        val crop = reader.child("crop")?.let { crop ->
            CropRect(
                x = crop.int("x", 0, Int.MAX_VALUE) ?: 0,
                y = crop.int("y", 0, Int.MAX_VALUE) ?: 0,
                width = crop.int("width", MIN_DIMENSION, Int.MAX_VALUE) ?: throw MediaStudioException.invalid("options.crop.width is required"),
                height = crop.int("height", MIN_DIMENSION, Int.MAX_VALUE) ?: throw MediaStudioException.invalid("options.crop.height is required"),
            ).also { crop.finish() }
        }
        val flip = reader.choice("flip", Flip.entries.map { it.id })?.let(Flip::of) ?: Flip.NONE
        if (width == null && height == null && scale == null && rotate == 0 && crop == null && flip == Flip.NONE) {
            throw MediaStudioException.invalid("transform needs one of options.width, height, scale, rotate, crop or flip")
        }
        return OperationSpec.Transform(width, height, scale, rotate, crop, flip, reader.fit(), reader.videoEncoding())
    }

    private fun extractAudio(reader: OptionReader): OperationSpec.ExtractAudio {
        val container = reader.choice("format", AudioContainer.entries.map { it.id })?.let { AudioContainer.ofExtension(it) }
            ?: AudioContainer.M4A
        val rates = if (container == AudioContainer.OGG) OPUS_SAMPLE_RATES else SAMPLE_RATES
        val audio = reader.audioEncoding(rates)
        if (audio.bitRate != null && (container == AudioContainer.WAV || container == AudioContainer.FLAC)) {
            throw MediaStudioException.invalid("options.bitRate does not apply to lossless ${container.id}")
        }
        return OperationSpec.ExtractAudio(container, audio)
    }

    /** True when [options] names an explicit `format`. zh-CN: [options] 显式给出 `format` 时为 true. */
    fun hasExplicitFormat(options: Map<String, Any?>): Boolean = options["format"] != null

    private fun OptionReader.fit(): Fit = choice("fit", Fit.entries.map { it.id })?.let(Fit::of) ?: Fit.CONTAIN

    private fun OptionReader.videoEncoding(withFrameRate: Boolean = false): VideoEncoding = VideoEncoding(
        codec = choice("codec", VideoCodec.entries.map { it.id })?.let(VideoCodec::of) ?: VideoCodec.H264,
        bitRate = int("bitRate", MIN_VIDEO_BIT_RATE, MAX_VIDEO_BIT_RATE),
        frameRate = if (withFrameRate) double("frameRate", MIN_FRAME_RATE, MAX_FRAME_RATE) else null,
    )

    private fun OptionReader.audioEncoding(sampleRates: List<Int>): AudioEncoding = AudioEncoding(
        bitRate = int("bitRate", MIN_AUDIO_BIT_RATE, MAX_AUDIO_BIT_RATE),
        sampleRate = int("sampleRate", 1, Int.MAX_VALUE)?.also { rate ->
            if (rate !in sampleRates) throw MediaStudioException.invalid("${name("sampleRate")} must be one of ${sampleRates.joinToString()}, got $rate")
        },
        channels = int("channels", 1, 2),
    )

    /** Width or height in [MIN_DIMENSION, MAX_DIMENSION], rounded down to even. zh-CN: 宽或高, 取值 [MIN_DIMENSION, MAX_DIMENSION], 向下取偶. */
    private fun OptionReader.dimension(key: String): Int? = int(key, MIN_DIMENSION, MAX_DIMENSION)?.let { it and 1.inv() }

}

/**
 * Reads one options object and remembers which keys it read, so [finish] rejects the unknown ones. JSON null counts as
 * absent, for known and unknown keys alike.
 * zh-CN: 读取一个选项对象并记住读过的键, [finish] 据此拒绝未知键. JSON null 视为未提供, 已知键与未知键都一样.
 */
internal class OptionReader(private val values: Map<String, Any?>, private val prefix: String) {

    private val read = mutableSetOf<String>()

    fun name(key: String) = prefix + key

    private fun raw(key: String): Any? {
        read += key
        return values[key]
    }

    fun bool(key: String): Boolean? = when (val value = raw(key)) {
        null -> null
        is Boolean -> value
        else -> throw MediaStudioException.invalid("${name(key)} must be a boolean")
    }

    fun choice(key: String, choices: Collection<String>): String? = when (val value = raw(key)) {
        null -> null
        is String -> value.takeIf { it in choices }
            ?: throw MediaStudioException.invalid("${name(key)} must be one of ${choices.joinToString()}, got $value")
        else -> throw MediaStudioException.invalid("${name(key)} must be a string")
    }

    fun double(key: String, min: Double, max: Double, minExclusive: Boolean = false): Double? {
        val value = number(key) ?: return null
        val below = if (minExclusive) value <= min else value < min
        if (below || value > max) {
            val lower = if (minExclusive) "greater than ${format(min)}" else "at least ${format(min)}"
            throw MediaStudioException.invalid("${name(key)} must be $lower and at most ${format(max)}, got ${format(value)}")
        }
        return value
    }

    fun long(key: String, min: Long, max: Long): Long? {
        val value = number(key) ?: return null
        if (value != Math.floor(value)) throw MediaStudioException.invalid("${name(key)} must be an integer, got ${format(value)}")
        if (value < min.toDouble() || value > max.toDouble()) {
            throw MediaStudioException.invalid("${name(key)} must be between $min and $max, got ${format(value)}")
        }
        return value.toLong()
    }

    fun int(key: String, min: Int, max: Int): Int? = long(key, min.toLong(), max.toLong())?.toInt()

    fun child(key: String): OptionReader? = when (val value = raw(key)) {
        null -> null
        is Map<*, *> -> OptionReader(value.entries.associate { (k, v) -> k.toString() to v }, "${name(key)}.")
        else -> throw MediaStudioException.invalid("${name(key)} must be an object")
    }

    /** Rejects keys nobody read; a null value is absent, so it is never unknown. zh-CN: 拒绝无人读取的键; 值为 null 视为未提供, 不算未知键. */
    fun finish() {
        val unknown = values.filter { (key, value) -> key !in read && value != null }.keys.sorted()
        if (unknown.isNotEmpty()) {
            throw MediaStudioException.invalid("unknown option ${unknown.joinToString { name(it) }}")
        }
    }

    private fun number(key: String): Double? = when (val value = raw(key)) {
        null -> null
        is Number -> value.toDouble().also {
            if (it.isNaN() || it.isInfinite()) throw MediaStudioException.invalid("${name(key)} must be a finite number")
        }
        else -> throw MediaStudioException.invalid("${name(key)} must be a number")
    }

    private fun format(value: Double): String =
        if (value == Math.floor(value) && Math.abs(value) < 1e15) value.toLong().toString() else value.toString()

}

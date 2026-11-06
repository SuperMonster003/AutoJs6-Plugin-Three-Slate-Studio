package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
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

/**
 * Still image outputs of `frames`, `frame` and `thumbnail` (roadmap P2.4, D32); `jpeg` is accepted as another name of
 * `jpg`, as option value and as extension.
 * zh-CN: `frames`, `frame` 与 `thumbnail` 的静态图片输出 (路线图 P2.4, D32); `jpeg` 作为 `jpg` 的别名, 选项值与扩展名都接受.
 */
internal enum class ImageFormat(val id: String, val mime: String) {
    PNG("png", "image/png"),
    JPG("jpg", "image/jpeg"),
    ;

    val extension: String get() = id

    companion object {
        /** Accepted `format` values and extensions. zh-CN: 接受的 `format` 值与扩展名. */
        val NAMES: List<String> = listOf("png", "jpg", "jpeg")

        fun of(name: String): ImageFormat? = when (name.lowercase()) {
            "png" -> PNG
            "jpg", "jpeg" -> JPG
            else -> null
        }
    }
}

/** [width] null keeps the operation default. zh-CN: [width] 为 null 时用操作的默认值. */
internal data class ImageEncoding(val format: ImageFormat, val quality: Int, val width: Int?)

internal data class VideoEncoding(val codec: VideoCodec = VideoCodec.H264, val bitRate: Int? = null, val frameRate: Double? = null)

internal data class AudioEncoding(val bitRate: Int? = null, val sampleRate: Int? = null, val channels: Int? = null)

/** One input of `audioMix`: its linear [volume] and its start [atMillis] in the mix. zh-CN: `audioMix` 的一个输入: 线性音量 [volume] 与在混音中的起点 [atMillis]. */
internal data class MixTrack(val volume: Double = 1.0, val atMillis: Long = 0L)

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

    /**
     * Operations that write an audio file in [container] with [audio] (`extractAudio` and the `audio*` operations of
     * roadmap P2.5); the defaults of [audio] follow [AudioTargets].
     * zh-CN: 以 [container] 与 [audio] 写出音频文件的操作 (`extractAudio` 与路线图 P2.5 的 `audio*` 操作); [audio] 的默认值见 [AudioTargets].
     */
    sealed interface AudioOutput : OperationSpec {
        val container: AudioContainer
        val audio: AudioEncoding
    }

    data class ExtractAudio(override val container: AudioContainer, override val audio: AudioEncoding) : AudioOutput

    /** [endMillis] is exclusive and null for "until the end". zh-CN: [endMillis] 不含, null 表示到结尾. */
    data class AudioTrim(val startMillis: Long, val endMillis: Long?, override val container: AudioContainer, override val audio: AudioEncoding) : AudioOutput

    data class AudioConcat(override val container: AudioContainer, override val audio: AudioEncoding) : AudioOutput

    /** [tracks] holds one entry per input, in input order. zh-CN: [tracks] 按输入顺序每个输入一项. */
    data class AudioMix(val tracks: List<MixTrack>, override val container: AudioContainer, override val audio: AudioEncoding) : AudioOutput

    /**
     * [gain] is a linear factor; [normalizeDbfs] is the target peak in dBFS (exclusive with [gain]); the fades are in
     * milliseconds.
     * zh-CN: [gain] 为线性倍数; [normalizeDbfs] 为目标峰值 (dBFS, 与 [gain] 互斥); 淡入淡出以毫秒计.
     */
    data class AudioVolume(
        val gain: Double?,
        val normalizeDbfs: Double?,
        val fadeInMillis: Long?,
        val fadeOutMillis: Long?,
        override val container: AudioContainer,
        override val audio: AudioEncoding,
    ) : AudioOutput

    data class AudioConvert(override val container: AudioContainer, override val audio: AudioEncoding) : AudioOutput

    data class AudioSpeed(val factor: Double, val keepPitch: Boolean, override val container: AudioContainer, override val audio: AudioEncoding) : AudioOutput

    /** Text and picture overlays drawn on every frame they cover (roadmap P2.3). zh-CN: 画在所覆盖各帧上的文字与图片叠加 (路线图 P2.3). */
    data class Overlay(val items: List<OverlayItem>, val video: VideoEncoding) : OperationSpec

    /**
     * The multi-track timeline of roadmap P3.1 / P3.2 (D33, D30), encoded with [video] (`codec`, `bitRate`).
     * zh-CN: 路线图 P3.1 / P3.2 的多轨时间线 (D33, D30), 以 [video] (`codec`, `bitRate`) 编码.
     */
    data class Compose(val timeline: TimelineSpec, val video: VideoEncoding) : OperationSpec

    /** Subtitles burned into the frames (roadmap P3.3, D31). zh-CN: 烧录进画面的字幕 (路线图 P3.3, D31). */
    data class BurnSubtitles(val subtitles: SubtitlesSpec, val video: VideoEncoding) : OperationSpec

    /** Reads the cues of the input subtitle file (roadmap P3.3). zh-CN: 读取输入字幕文件的 cue (路线图 P3.3). */
    data object SubtitlesParse : OperationSpec

    /** Writes [cues] as an SRT or ASS file (roadmap P3.3). zh-CN: 把 [cues] 写成 SRT 或 ASS 文件 (路线图 P3.3). */
    data class SubtitlesWrite(val cues: List<SubtitleCue>, val format: SubtitleFormat) : OperationSpec

    /** Frames written as images into one folder (roadmap P2.4). zh-CN: 写成图片放进一个文件夹的多帧 (路线图 P2.4). */
    data class Frames(val times: FrameTimes, val image: ImageEncoding) : OperationSpec

    /** One frame; the host decodes the file into an `Image` (appendix D Q25 (a)). zh-CN: 单帧; 宿主把文件解码为 `Image` (附录 D Q25 (a)). */
    data class Frame(val atMillis: Double, val image: ImageEncoding) : OperationSpec

    /** [atMillis] null takes the frame at a third of the duration. zh-CN: [atMillis] 为 null 时取时长三分之一处的帧. */
    data class Thumbnail(val atMillis: Double?, val image: ImageEncoding) : OperationSpec

    /**
     * [endMillis] is exclusive and null for "until the end"; [loop] is the NETSCAPE2.0 repeat count (0 plays forever),
     * null plays once without the loop extension.
     * zh-CN: [endMillis] 不含, null 表示到结尾; [loop] 为 NETSCAPE2.0 重复次数 (0 为无限循环), null 表示不写循环扩展, 只播放一次.
     */
    data class ToGif(
        val startMillis: Double,
        val endMillis: Double?,
        val fps: Int,
        val width: Int?,
        val loop: Int?,
        val dither: Boolean,
    ) : OperationSpec

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

    /**
     * Frames of one `frames` task (appendix D Q15 (a), the contract fixes no limit); more are LIMIT_EXCEEDED.
     * zh-CN: 一个 `frames` 任务的帧数上限 (附录 D Q15 (a), 契约未规定); 超过时为 LIMIT_EXCEEDED.
     */
    const val MAX_FRAMES = 1000

    /** GIF delays are centiseconds, so 50 fps (2 cs) is the fastest. zh-CN: GIF 延时以百分之一秒计, 50 fps (2 cs) 最快. */
    const val MAX_GIF_FPS = 50
    const val DEFAULT_IMAGE_QUALITY = 90
    const val DEFAULT_THUMBNAIL_QUALITY = 85
    const val DEFAULT_THUMBNAIL_WIDTH = 320
    const val MIN_IMAGE_QUALITY = 1
    const val MAX_IMAGE_QUALITY = 100

    /** Largest `gain` of `audioVolume` and `volume` of an `audioMix` track (+24 dB). zh-CN: `audioVolume` 的 `gain` 与 `audioMix` 各轨 `volume` 的上限 (+24 dB). */
    const val MAX_GAIN = 16.0

    /** `normalize` takes a target peak from -60 to 0 dBFS; `true` means -1 dBFS. zh-CN: `normalize` 的目标峰值为 -60 至 0 dBFS; `true` 表示 -1 dBFS. */
    const val MIN_NORMALIZE_DBFS = -60.0
    const val DEFAULT_NORMALIZE_DBFS = -1.0

    /**
     * Inputs of one `audioMix`: every input keeps a decoder open while the mix runs (appendix D Q18 (a)); more are
     * LIMIT_EXCEEDED.
     * zh-CN: 一个 `audioMix` 的输入上限: 混音期间每个输入各占一个解码器 (附录 D Q18 (a)); 超过时为 LIMIT_EXCEEDED.
     */
    const val MAX_MIX_INPUTS = 16

    /** Single input, single output operations. zh-CN: 单输入单输出的操作. */
    val SINGLE_INPUT_OPS: Set<String> = setOf(
        MediaStudioOps.TRIM,
        MediaStudioOps.SPEED,
        MediaStudioOps.TRANSFORM,
        MediaStudioOps.TRANSCODE,
        MediaStudioOps.REMOVE_AUDIO,
        MediaStudioOps.EXTRACT_AUDIO,
        MediaStudioOps.OVERLAY,
        MediaStudioOps.FRAMES,
        MediaStudioOps.FRAME,
        MediaStudioOps.THUMBNAIL,
        MediaStudioOps.TO_GIF,
        MediaStudioOps.AUDIO_TRIM,
        MediaStudioOps.AUDIO_VOLUME,
        MediaStudioOps.AUDIO_CONVERT,
        MediaStudioOps.AUDIO_SPEED,
        MediaStudioOps.BURN_SUBTITLES,
        MediaStudioOps.SUBTITLES_PARSE,
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
        MediaStudioOps.OVERLAY,
        MediaStudioOps.FRAMES,
        MediaStudioOps.FRAME,
        MediaStudioOps.THUMBNAIL,
        MediaStudioOps.TO_GIF,
        MediaStudioOps.COMPOSE,
        MediaStudioOps.BURN_SUBTITLES,
        MediaStudioOps.SUBTITLES_PARSE,
        MediaStudioOps.SUBTITLES_WRITE,
        MediaStudioOps.AUDIO_TRIM,
        MediaStudioOps.AUDIO_CONCAT,
        MediaStudioOps.AUDIO_MIX,
        MediaStudioOps.AUDIO_VOLUME,
        MediaStudioOps.AUDIO_CONVERT,
        MediaStudioOps.AUDIO_SPEED,
    )

    /**
     * Parses [options] of [op] (already a known operation of [SUBMIT_OPS]) for [inputCount] inputs.
     * zh-CN: 解析 [op] (已知且属于 [SUBMIT_OPS]) 在 [inputCount] 个输入下的 [options].
     */
    fun parse(op: String, inputCount: Int, options: Map<String, Any?>, timeline: Map<String, Any?>? = null): OperationSpec {
        if (op in SINGLE_INPUT_OPS && inputCount != 1) {
            throw MediaStudioException.invalid("$op takes exactly 1 input, got $inputCount")
        }
        if ((op == MediaStudioOps.CONCAT || op == MediaStudioOps.AUDIO_CONCAT || op == MediaStudioOps.AUDIO_MIX) && inputCount < 2) {
            throw MediaStudioException.invalid("$op takes at least 2 inputs, got $inputCount")
        }
        if (op == MediaStudioOps.AUDIO_MIX && inputCount > MAX_MIX_INPUTS) {
            throw limit("audioMix takes at most $MAX_MIX_INPUTS inputs, got $inputCount")
        }
        if (op == MediaStudioOps.SUBTITLES_WRITE && inputCount != 0) {
            throw MediaStudioException.invalid("subtitlesWrite takes no inputs, got $inputCount")
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
            MediaStudioOps.OVERLAY -> OperationSpec.Overlay(OverlayOptions.parse(reader, "overlays"), reader.videoEncoding())
            MediaStudioOps.FRAMES -> frames(reader)
            MediaStudioOps.FRAME -> OperationSpec.Frame(
                atMillis = reader.time("at") ?: throw MediaStudioException.invalid("options.at is required"),
                image = reader.imageEncoding(ImageFormat.PNG, DEFAULT_IMAGE_QUALITY),
            )
            MediaStudioOps.THUMBNAIL -> OperationSpec.Thumbnail(
                atMillis = reader.time("at"),
                image = reader.imageEncoding(ImageFormat.JPG, DEFAULT_THUMBNAIL_QUALITY),
            )
            MediaStudioOps.TO_GIF -> toGif(reader)
            // The timeline first: its field paths are more specific than the two options. zh-CN: 先解析时间线: 其字段路径比两个选项更具体.
            MediaStudioOps.COMPOSE -> TimelineOptions.parse(timeline).let { spec -> OperationSpec.Compose(spec, reader.videoEncoding()) }
            MediaStudioOps.BURN_SUBTITLES -> {
                // The script passes one object after the subtitles; codec and bitRate travel in it. zh-CN: 脚本在字幕之后只传一个对象, codec 与 bitRate 也在其中.
                val (source, style, video) = SubtitleOptions.source(reader, op, "subtitles") { it.videoEncoding() }
                OperationSpec.BurnSubtitles(SubtitlesSpec(source, style), video ?: VideoEncoding())
            }
            MediaStudioOps.SUBTITLES_PARSE -> OperationSpec.SubtitlesParse
            MediaStudioOps.SUBTITLES_WRITE -> OperationSpec.SubtitlesWrite(
                cues = SubtitleOptions.cues(reader, "cues"),
                format = reader.choice("format", SubtitleFormat.IDS)?.let(SubtitleFormat::of)
                    ?: throw MediaStudioException.invalid("subtitlesWrite needs options.format or an output ending with .srt or .ass"),
            )
            MediaStudioOps.AUDIO_TRIM -> trim(reader, op).let { range ->
                reader.audioOutput(op) { container, audio -> OperationSpec.AudioTrim(range.startMillis, range.endMillis, container, audio) }
            }
            MediaStudioOps.AUDIO_CONCAT -> reader.audioOutput(op, build = OperationSpec::AudioConcat)
            MediaStudioOps.AUDIO_MIX -> mix(reader, inputCount)
            MediaStudioOps.AUDIO_VOLUME -> volume(reader)
            MediaStudioOps.AUDIO_CONVERT -> reader.audioOutput(op, formatRequired = true, build = OperationSpec::AudioConvert)
            MediaStudioOps.AUDIO_SPEED -> {
                val factor = reader.double("factor", MediaStudioLimits.MIN_SPEED_FACTOR, MediaStudioLimits.MAX_SPEED_FACTOR)
                    ?: throw MediaStudioException.invalid("options.factor is required")
                val keepPitch = reader.bool("keepPitch") ?: true
                reader.audioOutput(op) { container, audio -> OperationSpec.AudioSpeed(factor, keepPitch, container, audio) }
            }
            else -> throw IllegalArgumentException("not a submit operation: $op")
        }
        reader.finish()
        return spec
    }

    private fun trim(reader: OptionReader, op: String = MediaStudioOps.TRIM): OperationSpec.Trim {
        val start = reader.long("start", 0, MAX_TIME_MILLIS)
        val end = reader.long("end", 1, MAX_TIME_MILLIS)
        val duration = reader.long("duration", 1, MAX_TIME_MILLIS)
        if (start == null && end == null && duration == null) {
            throw MediaStudioException.invalid("$op needs options.start, options.end or options.duration")
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

    private fun extractAudio(reader: OptionReader): OperationSpec.ExtractAudio = reader.audioOutput(MediaStudioOps.EXTRACT_AUDIO, build = OperationSpec::ExtractAudio)

    /**
     * `tracks` (one object per input, `volume` and `at`), left out when no input has settings (the host sends it only
     * then).
     * zh-CN: `tracks` (每个输入一个对象, 含 `volume` 与 `at`), 所有输入都没有设置时省略 (宿主只在有设置时发送).
     */
    private fun mix(reader: OptionReader, inputCount: Int): OperationSpec.AudioMix {
        val tracks = reader.objects("tracks")?.let { entries ->
            if (entries.size != inputCount) {
                throw MediaStudioException.invalid("options.tracks has ${entries.size} entries for $inputCount inputs")
            }
            entries.map { track ->
                MixTrack(
                    volume = track.double("volume", 0.0, MAX_GAIN) ?: 1.0,
                    atMillis = track.long("at", 0, MAX_TIME_MILLIS) ?: 0L,
                ).also { track.finish() }
            }
        } ?: List(inputCount) { MixTrack() }
        return reader.audioOutput(MediaStudioOps.AUDIO_MIX) { container, audio -> OperationSpec.AudioMix(tracks, container, audio) }
    }

    /**
     * `gain` (linear, appendix D Q19 (a)) or `normalize` (`true` or a target peak in dBFS), `fadeIn` / `fadeOut` in
     * milliseconds (0 means none); at least one of them.
     * zh-CN: `gain` (线性倍数, 附录 D Q19 (a)) 或 `normalize` (`true` 或目标峰值 dBFS), `fadeIn` / `fadeOut` 以毫秒计 (0 表示不淡);
     * 至少给出一项.
     */
    private fun volume(reader: OptionReader): OperationSpec.AudioVolume {
        val gain = reader.double("gain", 0.0, MAX_GAIN)
        val normalize = when (val value = reader.boolOrDouble("normalize", MIN_NORMALIZE_DBFS, 0.0)) {
            true -> DEFAULT_NORMALIZE_DBFS
            is Double -> value
            else -> null
        }
        if (gain != null && normalize != null) throw MediaStudioException.invalid("options.gain and options.normalize are exclusive")
        val fadeIn = reader.long("fadeIn", 0, MAX_TIME_MILLIS)?.takeIf { it > 0 }
        val fadeOut = reader.long("fadeOut", 0, MAX_TIME_MILLIS)?.takeIf { it > 0 }
        if (gain == null && normalize == null && fadeIn == null && fadeOut == null) {
            throw MediaStudioException.invalid("audioVolume needs options.gain, normalize, fadeIn or fadeOut")
        }
        return reader.audioOutput(MediaStudioOps.AUDIO_VOLUME) { container, audio ->
            OperationSpec.AudioVolume(gain, normalize, fadeIn, fadeOut, container, audio)
        }
    }

    /**
     * The output keys every audio operation shares: `format` (filled from the output extension by the planner, M4A by
     * default, appendix D Q16 (a)), `bitRate` (not for the lossless WAV and FLAC), `sampleRate` (the Opus rates for OGG)
     * and `channels`.
     * zh-CN: 各音频操作共用的输出键: `format` (由规划器按输出扩展名补上, 默认 M4A, 附录 D Q16 (a)), `bitRate` (无损的 WAV 与
     * FLAC 不适用), `sampleRate` (OGG 只接受 Opus 采样率) 与 `channels`.
     */
    private fun <T : OperationSpec.AudioOutput> OptionReader.audioOutput(
        op: String,
        formatRequired: Boolean = false,
        build: (AudioContainer, AudioEncoding) -> T,
    ): T {
        val format = choice("format", AudioContainer.entries.map { it.id })
        if (format == null && formatRequired) {
            throw MediaStudioException.invalid(
                "$op needs options.format or an output ending with ${AudioContainer.entries.joinToString { "." + it.extension }}",
            )
        }
        val container = format?.let { AudioContainer.ofExtension(it) } ?: AudioContainer.M4A
        val rates = if (container == AudioContainer.OGG) OPUS_SAMPLE_RATES else SAMPLE_RATES
        val audio = audioEncoding(rates)
        if (audio.bitRate != null && (container == AudioContainer.WAV || container == AudioContainer.FLAC)) {
            throw MediaStudioException.invalid("options.bitRate does not apply to lossless ${container.id}")
        }
        return build(container, audio)
    }

    /**
     * `at` (one time) or `times` (several, in the given order), or `every` / `count` within `start` - `end`; `every` and
     * `count` together take the first `count` frames of the interval sequence.
     * zh-CN: `at` (一个时间点) 或 `times` (多个, 按给出顺序), 或在 `start` - `end` 内按 `every` / `count` 取帧; `every` 与
     * `count` 同时给出时取间隔序列的前 `count` 帧.
     */
    private fun frames(reader: OptionReader): OperationSpec.Frames {
        val at = reader.time("at")
        val times = reader.numbers("times")
        val every = reader.double("every", 1.0, MAX_TIME_MILLIS.toDouble())
        val count = reader.int("count", 1, Int.MAX_VALUE)
        val start = reader.time("start")
        val end = reader.double("end", 0.0, MAX_TIME_MILLIS.toDouble(), minExclusive = true)
        val points = if (at != null) "at" else if (times != null) "times" else null
        if (at != null && times != null) throw MediaStudioException.invalid("options.at and options.times are exclusive")
        if (points != null && (every != null || count != null)) {
            throw MediaStudioException.invalid("options.$points and options.every / options.count are exclusive")
        }
        if (points != null && (start != null || end != null)) {
            throw MediaStudioException.invalid("options.start and options.end apply to options.every and options.count only")
        }
        if (points == null && every == null && count == null) {
            throw MediaStudioException.invalid("frames needs options.at, times, every or count")
        }
        val startMillis = start ?: 0.0
        if (end != null && end <= startMillis) {
            throw MediaStudioException.invalid("options.end (${reader.format(end)}) must be greater than options.start (${reader.format(startMillis)})")
        }
        if (count != null && count > MAX_FRAMES) throw limit("options.count is $count, at most $MAX_FRAMES frames")
        val sampling = when {
            at != null -> FrameTimes.Points(listOf(at), "at")
            times != null -> {
                if (times.isEmpty()) throw MediaStudioException.invalid("options.times must hold at least 1 time")
                if (times.size > MAX_FRAMES) throw limit("options.times holds ${times.size} times, at most $MAX_FRAMES")
                times.forEachIndexed { index, time ->
                    if (time < 0 || time > MAX_TIME_MILLIS) {
                        throw MediaStudioException.invalid("options.times[$index] must be at least 0 and at most $MAX_TIME_MILLIS, got ${reader.format(time)}")
                    }
                }
                FrameTimes.Points(times, "times")
            }
            every != null -> FrameTimes.Every(every, startMillis, end, count)
            else -> FrameTimes.Count(count!!, startMillis, end)
        }
        return OperationSpec.Frames(sampling, reader.imageEncoding(ImageFormat.PNG, DEFAULT_IMAGE_QUALITY))
    }

    private fun toGif(reader: OptionReader): OperationSpec.ToGif {
        val start = reader.time("start") ?: 0.0
        val end = reader.double("end", 0.0, MAX_TIME_MILLIS.toDouble(), minExclusive = true)
        if (end != null && end <= start) {
            throw MediaStudioException.invalid("options.end (${reader.format(end)}) must be greater than options.start (${reader.format(start)})")
        }
        val fps = reader.int("fps", 1, MAX_GIF_FPS) ?: MediaStudioLimits.DEFAULT_GIF_FPS
        val width = reader.int("width", MIN_DIMENSION, MAX_DIMENSION)
        val loop = when (val value = reader.boolOrInt("loop", 0, GifEncoder.MAX_LOOP)) {
            null, true -> 0
            false -> null
            else -> value as Int
        }
        val dither = reader.bool("dither") ?: false
        // A known range is checked before any decoding. zh-CN: 已知范围时在解码前检查.
        if (end != null) FrameSampling.gifFrameCount(start, end, fps)
        return OperationSpec.ToGif(start, end, fps, width, loop, dither)
    }

    fun limit(detail: String) = MediaStudioException(MediaStudioErrorCodes.LIMIT_EXCEEDED, detail)

    /**
     * Files besides the inputs that the task reads (overlay pictures, font files and subtitle files); the planner checks
     * them like inputs.
     * zh-CN: 输入之外任务要读取的文件 (叠加图片, 字体文件与字幕文件); 规划器像检查输入一样检查它们.
     */
    fun resources(spec: OperationSpec): List<String> = when (spec) {
        is OperationSpec.Overlay -> overlayFiles(spec.items)
        is OperationSpec.Compose -> (overlayFiles(spec.timeline.overlays) + spec.timeline.subtitles?.files.orEmpty()).distinct()
        is OperationSpec.BurnSubtitles -> spec.subtitles.files
        else -> emptyList()
    }

    private fun overlayFiles(items: List<OverlayItem>): List<String> = items.mapNotNull { item ->
        when (item) {
            is OverlayItem.Image -> item.src
            is OverlayItem.Text -> item.fontFile
        }
    }.distinct()

    /** True when [options] names an explicit `format`. zh-CN: [options] 显式给出 `format` 时为 true. */
    fun hasExplicitFormat(options: Map<String, Any?>): Boolean = options["format"] != null

    /** A time in milliseconds, fractions allowed, 0 to 2^53. zh-CN: 毫秒时间, 可带小数, 0 至 2^53. */
    private fun OptionReader.time(key: String): Double? = double(key, 0.0, MAX_TIME_MILLIS.toDouble())

    /** `format`, `quality` (jpg only) and `width` of still image outputs. zh-CN: 静态图片输出的 `format`, `quality` (仅 jpg) 与 `width`. */
    private fun OptionReader.imageEncoding(defaultFormat: ImageFormat, defaultQuality: Int): ImageEncoding {
        val format = choice("format", ImageFormat.NAMES)?.let { ImageFormat.of(it) } ?: defaultFormat
        val quality = int("quality", MIN_IMAGE_QUALITY, MAX_IMAGE_QUALITY)
        if (quality != null && format != ImageFormat.JPG) throw MediaStudioException.invalid("options.quality applies to jpg only")
        return ImageEncoding(format, quality ?: defaultQuality, int("width", MIN_DIMENSION, MAX_DIMENSION))
    }

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

    /** True when [key] holds a value (null counts as absent); does not mark it read. zh-CN: [key] 有值时为 true (null 视为未提供); 不标记为已读. */
    fun has(key: String): Boolean = values[key] != null

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

    /** A boolean, or an integer in [min, max]. zh-CN: 布尔值, 或 [min, max] 内的整数. */
    fun boolOrInt(key: String, min: Int, max: Int): Any? = when (values[key]) {
        null, is Boolean -> bool(key)
        is Number -> int(key, min, max)
        else -> {
            read += key
            throw MediaStudioException.invalid("${name(key)} must be a boolean or an integer")
        }
    }

    /** A boolean, or a number in [min, max]. zh-CN: 布尔值, 或 [min, max] 内的数字. */
    fun boolOrDouble(key: String, min: Double, max: Double): Any? = when (values[key]) {
        null, is Boolean -> bool(key)
        is Number -> double(key, min, max)
        else -> {
            read += key
            throw MediaStudioException.invalid("${name(key)} must be a boolean or a number")
        }
    }

    fun child(key: String): OptionReader? = when (val value = raw(key)) {
        null -> null
        is Map<*, *> -> OptionReader(value.entries.associate { (k, v) -> k.toString() to v }, "${name(key)}.")
        else -> throw MediaStudioException.invalid("${name(key)} must be an object")
    }

    fun string(key: String): String? = when (val value = raw(key)) {
        null -> null
        is String -> value
        else -> throw MediaStudioException.invalid("${name(key)} must be a string")
    }

    /** A finite number as Double or a string, for options that take both. zh-CN: 有限数字 (Double) 或字符串, 用于两者皆可的选项. */
    fun numberOrString(key: String): Any? = when (val value = values[key]) {
        is String -> value.also { read += key }
        else -> number(key)
    }

    /**
     * An array of objects, one reader per element (`<key>[i].`); the caller finishes each.
     * zh-CN: 对象数组, 每个元素一个读取器 (`<key>[i].`); 由调用方逐个 finish.
     */
    fun objects(key: String): List<OptionReader>? = when (val value = raw(key)) {
        null -> null
        is List<*> -> value.mapIndexed { index, element ->
            val map = element as? Map<*, *> ?: throw MediaStudioException.invalid("${name(key)}[$index] must be an object")
            OptionReader(map.entries.associate { (k, v) -> k.toString() to v }, "${name(key)}[$index].")
        }
        else -> throw MediaStudioException.invalid("${name(key)} must be an array of objects")
    }

    /** An array of finite numbers. zh-CN: 有限数字数组. */
    fun numbers(key: String): List<Double>? = when (val value = raw(key)) {
        null -> null
        is List<*> -> value.mapIndexed { index, element ->
            val number = (element as? Number)?.toDouble()
            if (number == null || number.isNaN() || number.isInfinite()) throw MediaStudioException.invalid("${name(key)}[$index] must be a finite number")
            number
        }
        else -> throw MediaStudioException.invalid("${name(key)} must be an array of numbers")
    }

    /** Rejects keys nobody read; a null value is absent, so it is never unknown. zh-CN: 拒绝无人读取的键; 值为 null 视为未提供, 不算未知键. */
    fun finish() {
        val unknown = values.filter { (key, value) -> key !in read && value != null }.keys.sorted()
        if (unknown.isNotEmpty()) {
            throw MediaStudioException.invalid("unknown option ${unknown.joinToString { name(it) }}")
        }
    }

    fun number(key: String): Double? = when (val value = raw(key)) {
        null -> null
        is Number -> value.toDouble().also {
            if (it.isNaN() || it.isInfinite()) throw MediaStudioException.invalid("${name(key)} must be a finite number")
        }
        else -> throw MediaStudioException.invalid("${name(key)} must be a number")
    }

    /** Integral values up to 2^53 without a fraction, for messages. zh-CN: 不超过 2^53 的整数值不带小数部分, 用于消息. */
    fun format(value: Double): String =
        if (value == Math.floor(value) && Math.abs(value) <= MAX_EXACT_INTEGER) value.toLong().toString() else value.toString()

    private companion object {
        /** 2^53, the largest integer a double holds exactly. zh-CN: 2^53, double 能精确表示的最大整数. */
        const val MAX_EXACT_INTEGER = 9_007_199_254_740_992.0
    }

}

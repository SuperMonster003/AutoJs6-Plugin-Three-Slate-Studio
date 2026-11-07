package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Anchor
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Fit
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Flip
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.GifEncoder
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.ImageFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OverlayOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleStyles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TextAlign
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import org.autojs.plugin.mediastudio.api.MediaStudioOps

/** Units shown next to a field and written into the declarations. zh-CN: 显示在字段旁并写进声明的单位. */
internal enum class OptionUnit(val symbol: String) {
    NONE(""),
    MILLIS("ms"),
    PIXELS("px"),
    BITS_PER_SECOND("bps"),
    HERTZ("Hz"),
    FRAMES_PER_SECOND("fps"),
    DBFS("dBFS"),
    TIMES("x"),
    DEGREES("deg"),
}

/** What a file field holds. zh-CN: 文件字段的内容类型. */
internal enum class FileKind { IMAGE, SUBTITLE }

/** Lower bound of a [FieldType.Length]. zh-CN: [FieldType.Length] 的下限. */
internal enum class LengthLower { SIGNED, NON_NEGATIVE, POSITIVE }

/**
 * The value kinds of option fields; each mirrors one reader of `OperationOptions` / `OverlayOptions` / `SubtitleOptions`.
 * zh-CN: 选项字段的值类型; 每种对应 `OperationOptions` / `OverlayOptions` / `SubtitleOptions` 的一个读取方法.
 */
internal sealed interface FieldType {

    /** An integral number in [min, max]. zh-CN: [min, max] 内的整数. */
    data class Integer(val min: Long, val max: Long) : FieldType

    /** A number in [min, max]; [minExclusive] makes the lower bound open. zh-CN: [min, max] 内的数字; [minExclusive] 时下限不含. */
    data class Decimal(val min: Double, val max: Double, val minExclusive: Boolean = false) : FieldType

    data class Choice(val values: List<String>) : FieldType

    data class IntChoice(val values: List<Int>) : FieldType

    data object Flag : FieldType

    /** `true` / `false` or an integer in [min, max] (`loop`). zh-CN: 布尔值或 [min, max] 内的整数 (`loop`). */
    data class FlagOrInteger(val min: Long, val max: Long) : FieldType

    /** `true` / `false` or a number in [min, max] (`normalize`). zh-CN: 布尔值或 [min, max] 内的数字 (`normalize`). */
    data class FlagOrDecimal(val min: Double, val max: Double) : FieldType

    /** `#RGB`, `#RRGGBB`, `#AARRGGBB`, a color name or an ARGB integer. zh-CN: 颜色. */
    data object Color : FieldType

    /** Pixels as a number or a percentage string `"NN%"`. zh-CN: 像素数字或百分比字符串 `"NN%"`. */
    data class Length(val maxPixels: Double, val maxPercent: Double, val lower: LengthLower) : FieldType

    data class Text(val minChars: Int, val maxChars: Int) : FieldType

    /** A font family of [OverlayOptions.FONT_FAMILIES] or an absolute font file path. zh-CN: 字体族或字体文件绝对路径. */
    data object Font : FieldType

    data class FilePath(val kind: FileKind) : FieldType

    /** An array of times (`times` of `frames`). zh-CN: 时间数组 (`frames` 的 `times`). */
    data class Numbers(val min: Double, val max: Double, val maxCount: Int) : FieldType

    /** A nested object (`crop`, `audio`, `style`). zh-CN: 嵌套对象 (`crop`, `audio`, `style`). */
    data class Group(val fields: List<OptionField>) : FieldType

    /** An array of objects told apart by [discriminator] (`overlays`). zh-CN: 以 [discriminator] 区分的对象数组 (`overlays`). */
    data class Items(val min: Int, val max: Int, val discriminator: String, val variants: Map<String, List<OptionField>>) : FieldType

    /** One object per input, in input order (`tracks` of `audioMix`). zh-CN: 每个输入一个对象, 按输入顺序 (`audioMix` 的 `tracks`). */
    data class PerInput(val fields: List<OptionField>) : FieldType
}

/** [key] equals (or, with [negate], differs from) one of [values]. zh-CN: [key] 等于 (或 [negate] 时不等于) [values] 之一. */
internal data class Condition(val key: String, val values: Set<String>, val negate: Boolean = false) {
    fun holds(value: String?): Boolean = (value in values) != negate
}

/**
 * One option key. [default] is the documented default (null: none, or one that depends on the input); fields with the
 * same [exclusiveGroup] are alternatives ([alternative] names the choice they belong to); [appliesWhen] limits a field
 * to some values of a sibling; [uiChoices] narrows what the form offers without changing what the plugin accepts.
 * zh-CN: 一个选项键. [default] 为文档中的默认值 (null: 没有, 或取决于输入); [exclusiveGroup] 相同的字段互为替代 ([alternative]
 * 为其所属的选择); [appliesWhen] 把字段限定在某个同级字段的部分取值下; [uiChoices] 只收窄表单提供的选项, 不改变插件接受的范围.
 */
internal data class OptionField(
    val key: String,
    val type: FieldType,
    val default: Any? = null,
    val required: Boolean = false,
    val unit: OptionUnit = OptionUnit.NONE,
    val exclusiveGroup: String? = null,
    val alternative: String? = null,
    val appliesWhen: Condition? = null,
    val uiChoices: List<Int>? = null,
    val advanced: Boolean = false,
)

internal enum class OperationCategory { VIDEO, IMAGE, AUDIO, SUBTITLE }

/** Inputs of an operation: VIDEO / AUDIO / MEDIA (either) / SUBTITLE files. zh-CN: 操作的输入: 视频 / 音频 / 媒体 (皆可) / 字幕文件. */
internal enum class InputKind { VIDEO, AUDIO, MEDIA, SUBTITLE }

internal data class InputSpec(val min: Int, val max: Int, val kind: InputKind)

/** What the operation writes. zh-CN: 操作写出的内容. */
internal enum class OutputKind { VIDEO, AUDIO, IMAGE, GIF, FOLDER, SUBTITLE }

/**
 * Argument slots of a script call, in the order of the d.ts (roadmap appendix B.1 / B.2). Empty optional slots are left
 * out, which the host's argument mapping (appendix D Q27 (a): string = output, object = options) allows in any order.
 * zh-CN: 脚本调用的参数位置, 按 d.ts 顺序 (路线图附录 B.1 / B.2). 空的可选位置被省略, 宿主的参数映射 (附录 D Q27 (a): 字符串 =
 * 输出, 对象 = 选项) 允许任意顺序.
 */
internal enum class Slot {
    INPUT, INPUTS, MIX_INPUTS, OPTIONS, OUTPUT,

    /** The `overlays` option as its own argument. zh-CN: `overlays` 选项作为单独参数. */
    OVERLAYS,

    /** The `at` option of `video.frame` as its own argument. zh-CN: `video.frame` 的 `at` 选项作为单独参数. */
    AT,

    /** The `subtitles` file of `burnSubtitles`. zh-CN: `burnSubtitles` 的 `subtitles` 文件. */
    SUBTITLES,

    /** The `style` object of `burnSubtitles` (with `codec` / `bitRate`). zh-CN: `burnSubtitles` 的 `style` 对象 (含 `codec` / `bitRate`). */
    STYLE,
}

/**
 * The schema of one wizard (roadmap D35): the contract [op] (or [SUBTITLES_CONVERT], which runs `subtitlesParse` then
 * `subtitlesWrite`), the script [module] and [method], inputs, fields, output and argument [slots].
 * zh-CN: 一个向导的模式 (路线图 D35): 契约操作 [op] (或 [SUBTITLES_CONVERT], 依次运行 `subtitlesParse` 与 `subtitlesWrite`),
 * 脚本的 [module] 与 [method], 输入, 字段, 输出与参数位置 [slots].
 */
internal data class OperationSchema(
    val op: String,
    val module: String,
    val method: String,
    val category: OperationCategory,
    val inputs: InputSpec,
    val fields: List<OptionField>,
    val output: OutputKind,
    val slots: List<Slot>,
) {
    fun field(key: String): OptionField? = fields.firstOrNull { it.key == key }

    companion object {
        /** The subtitle conversion wizard (P3.3 `subtitlesParse` + `subtitlesWrite`). zh-CN: 字幕格式转换向导. */
        const val SUBTITLES_CONVERT = "subtitlesConvert"
    }
}

/**
 * The option schemas of the toolbox wizards (roadmap D35, P4.2): one per processing API of roadmap P2.2 - P2.5 plus
 * `burnSubtitles` and the subtitle conversion of P3.3. Ranges, defaults and choices come from the constants of the
 * plugin's validators (`OperationOptions`, `OverlayOptions`, `SubtitleOptions`); `OptionSchemasTest` feeds the bounds of
 * every field to the validators, and `MediaStudioDeclarations` renders the same schemas as the TypeScript option types
 * (roadmap D36: the code generator and the d.ts share one source).
 * zh-CN: 工具箱向导的选项模式 (路线图 D35, P4.2): 路线图 P2.2 - P2.5 的每个加工 API 一个, 另有 P3.3 的 `burnSubtitles` 与字幕格式
 * 转换. 范围, 默认值与选项取自插件校验器 (`OperationOptions`, `OverlayOptions`, `SubtitleOptions`) 的常量; `OptionSchemasTest`
 * 把每个字段的边界交给校验器核对, `MediaStudioDeclarations` 把同一模式渲染为 TypeScript 选项类型 (路线图 D36: 代码生成器与
 * d.ts 同源).
 */
internal object OptionSchemas {

    private const val MAX_TIME = OperationOptions.MAX_TIME_MILLIS
    private val MAX_TIME_DOUBLE = OperationOptions.MAX_TIME_MILLIS.toDouble()
    private val AUDIO_FORMATS = AudioContainer.entries.map { it.id }
    private val LOSSLESS = setOf(AudioContainer.WAV.id, AudioContainer.FLAC.id)
    private val JPEG = setOf("jpg", "jpeg")

    private fun start() = OptionField("start", FieldType.Integer(0, MAX_TIME), default = 0L, unit = OptionUnit.MILLIS)
    private fun end() = OptionField("end", FieldType.Integer(1, MAX_TIME), unit = OptionUnit.MILLIS, exclusiveGroup = "length", alternative = "end")
    private fun duration() = OptionField("duration", FieldType.Integer(1, MAX_TIME), unit = OptionUnit.MILLIS, exclusiveGroup = "length", alternative = "duration")
    private fun width(group: String? = null, alternative: String? = null) =
        OptionField("width", FieldType.Integer(OperationOptions.MIN_DIMENSION.toLong(), OperationOptions.MAX_DIMENSION.toLong()), unit = OptionUnit.PIXELS, exclusiveGroup = group, alternative = alternative)
    private fun height(group: String? = null, alternative: String? = null) =
        OptionField("height", FieldType.Integer(OperationOptions.MIN_DIMENSION.toLong(), OperationOptions.MAX_DIMENSION.toLong()), unit = OptionUnit.PIXELS, exclusiveGroup = group, alternative = alternative)
    private fun fit() = OptionField("fit", FieldType.Choice(Fit.entries.map { it.id }), default = Fit.CONTAIN.id)
    private fun codec() = OptionField("codec", FieldType.Choice(VideoCodec.entries.map { it.id }), default = VideoCodec.H264.id, advanced = true)
    private fun videoBitRate() = OptionField(
        "bitRate",
        FieldType.Integer(OperationOptions.MIN_VIDEO_BIT_RATE.toLong(), OperationOptions.MAX_VIDEO_BIT_RATE.toLong()),
        unit = OptionUnit.BITS_PER_SECOND,
        advanced = true,
    )
    private fun factor() = OptionField(
        "factor",
        FieldType.Decimal(MediaStudioLimits.MIN_SPEED_FACTOR, MediaStudioLimits.MAX_SPEED_FACTOR),
        required = true,
        unit = OptionUnit.TIMES,
    )
    private fun keepPitch() = OptionField("keepPitch", FieldType.Flag, default = true)
    private fun audioBitRate(condition: Condition? = null) = OptionField(
        "bitRate",
        FieldType.Integer(OperationOptions.MIN_AUDIO_BIT_RATE.toLong(), OperationOptions.MAX_AUDIO_BIT_RATE.toLong()),
        unit = OptionUnit.BITS_PER_SECOND,
        appliesWhen = condition,
        advanced = true,
    )
    private fun sampleRate(rates: List<Int>, condition: Condition? = null) =
        OptionField("sampleRate", FieldType.IntChoice(rates), unit = OptionUnit.HERTZ, appliesWhen = condition, advanced = true)
    private fun channels() = OptionField("channels", FieldType.IntChoice(OperationOptions.CHANNELS), advanced = true)

    /** `format`, `bitRate` (lossy only), `sampleRate` (Opus rates for ogg) and `channels`. zh-CN: 音频输出键. */
    private fun audioOutput(formatRequired: Boolean = false): List<OptionField> = listOf(
        OptionField(
            "format",
            FieldType.Choice(AUDIO_FORMATS),
            default = if (formatRequired) null else AudioContainer.M4A.id,
            required = formatRequired,
        ),
        audioBitRate(Condition("format", LOSSLESS, negate = true)),
        sampleRate(OperationOptions.SAMPLE_RATES, Condition("format", setOf(AudioContainer.OGG.id), negate = true)),
        sampleRate(OperationOptions.OPUS_SAMPLE_RATES, Condition("format", setOf(AudioContainer.OGG.id))),
        channels(),
    )

    private fun imageEncoding(defaultFormat: ImageFormat, defaultQuality: Int): List<OptionField> = listOf(
        OptionField("format", FieldType.Choice(ImageFormat.NAMES), default = defaultFormat.id),
        OptionField(
            "quality",
            FieldType.Integer(OperationOptions.MIN_IMAGE_QUALITY.toLong(), OperationOptions.MAX_IMAGE_QUALITY.toLong()),
            default = defaultQuality.toLong(),
            appliesWhen = Condition("format", JPEG),
        ),
        width(),
    )

    private fun time(key: String, required: Boolean = false, group: String? = null, alternative: String? = null, minExclusive: Boolean = false) =
        OptionField(key, FieldType.Decimal(0.0, MAX_TIME_DOUBLE, minExclusive), required = required, unit = OptionUnit.MILLIS, exclusiveGroup = group, alternative = alternative)

    /** Text style keys shared by text overlays and subtitles. zh-CN: 文字叠加与字幕共用的文字样式键. */
    private fun textStyle(fontSizeDefault: Any?, colorDefault: Any?, shadowDefaults: Boolean): List<OptionField> = listOf(
        OptionField("font", FieldType.Font, default = "sans-serif"),
        OptionField("fontSize", FieldType.Decimal(0.0, OverlayOptions.MAX_FONT_SIZE, minExclusive = true), default = fontSizeDefault, unit = OptionUnit.PIXELS),
        OptionField("bold", FieldType.Flag, default = false),
        OptionField("italic", FieldType.Flag, default = false),
        OptionField("color", FieldType.Color, default = colorDefault),
        OptionField("stroke", FieldType.Color),
        OptionField("strokeWidth", FieldType.Decimal(0.0, OverlayOptions.MAX_EFFECT_PIXELS), unit = OptionUnit.PIXELS, advanced = true),
        OptionField("shadow", FieldType.Color, advanced = true),
        OptionField("shadowRadius", FieldType.Decimal(0.0, OverlayOptions.MAX_EFFECT_PIXELS), default = if (shadowDefaults) OverlayOptions.DEFAULT_SHADOW_RADIUS else null, unit = OptionUnit.PIXELS, advanced = true),
        OptionField("shadowDx", FieldType.Decimal(-OverlayOptions.MAX_EFFECT_PIXELS, OverlayOptions.MAX_EFFECT_PIXELS), default = if (shadowDefaults) OverlayOptions.DEFAULT_SHADOW_OFFSET else null, unit = OptionUnit.PIXELS, advanced = true),
        OptionField("shadowDy", FieldType.Decimal(-OverlayOptions.MAX_EFFECT_PIXELS, OverlayOptions.MAX_EFFECT_PIXELS), default = if (shadowDefaults) OverlayOptions.DEFAULT_SHADOW_OFFSET else null, unit = OptionUnit.PIXELS, advanced = true),
    )

    /** Placement keys of every overlay. zh-CN: 每个叠加物共有的位置键. */
    private fun placement(): List<OptionField> = listOf(
        OptionField("x", FieldType.Length(OverlayOptions.MAX_PIXELS, OverlayOptions.MAX_PERCENT, LengthLower.SIGNED), default = "50%"),
        OptionField("y", FieldType.Length(OverlayOptions.MAX_PIXELS, OverlayOptions.MAX_PERCENT, LengthLower.SIGNED), default = "50%"),
        OptionField("anchor", FieldType.Choice(Anchor.entries.map { it.id }), default = Anchor.CENTER.id),
        OptionField("from", FieldType.Integer(0, MAX_TIME), default = 0L, unit = OptionUnit.MILLIS),
        OptionField("to", FieldType.Integer(1, MAX_TIME), unit = OptionUnit.MILLIS),
        OptionField("alpha", FieldType.Decimal(0.0, 1.0), default = 1.0),
    )

    private val textOverlay: List<OptionField> = listOf(OptionField("text", FieldType.Text(1, OverlayOptions.MAX_TEXT_CHARS), required = true)) +
        textStyle(OverlayOptions.DEFAULT_FONT_SIZE, "#FFFFFF", shadowDefaults = true) +
        OptionField("align", FieldType.Choice(TextAlign.entries.map { it.id }), default = TextAlign.CENTER.id) +
        placement()

    private val imageOverlay: List<OptionField> = listOf(
        OptionField("src", FieldType.FilePath(FileKind.IMAGE), required = true),
        OptionField("scale", FieldType.Decimal(0.0, OperationOptions.MAX_SCALE, minExclusive = true), exclusiveGroup = "size", alternative = "scale"),
        OptionField("width", FieldType.Length(OverlayOptions.MAX_PIXELS, OverlayOptions.MAX_PERCENT, LengthLower.POSITIVE), exclusiveGroup = "size", alternative = "size"),
        OptionField("height", FieldType.Length(OverlayOptions.MAX_PIXELS, OverlayOptions.MAX_PERCENT, LengthLower.POSITIVE), exclusiveGroup = "size", alternative = "size"),
    ) + placement()

    private fun margin(key: String) = OptionField(
        key,
        FieldType.Length(SubtitleOptions.MAX_MARGIN_PIXELS, SubtitleOptions.MAX_MARGIN_PERCENT, LengthLower.NON_NEGATIVE),
        default = "${SubtitleStyles.DEFAULT_MARGIN_PERCENT.toInt()}%",
        advanced = true,
    )

    private val subtitleStyle: List<OptionField> = textStyle(fontSizeDefault = null, colorDefault = "#FFFFFF", shadowDefaults = false) + listOf(
        OptionField("background", FieldType.Color),
        OptionField("align", FieldType.Choice(Anchor.entries.map { it.id }), default = Anchor.BOTTOM.id),
        margin("marginV"),
        margin("marginH"),
        codec(),
        videoBitRate(),
    )

    private const val SINGLE_VIDEO_MAX = 1

    private fun single(kind: InputKind) = InputSpec(1, SINGLE_VIDEO_MAX, kind)

    val ALL: List<OperationSchema> = listOf(
        OperationSchema(MediaStudioOps.TRIM, "video", "trim", OperationCategory.VIDEO, single(InputKind.VIDEO),
            listOf(start(), end(), duration()), OutputKind.VIDEO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.CONCAT, "video", "concat", OperationCategory.VIDEO, InputSpec(2, MediaStudioLimits.MAX_INPUTS, InputKind.VIDEO),
            listOf(width(), height(), fit(), codec(), videoBitRate()), OutputKind.VIDEO, listOf(Slot.INPUTS, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.SPEED, "video", "speed", OperationCategory.VIDEO, single(InputKind.VIDEO),
            listOf(factor(), keepPitch(), codec(), videoBitRate()), OutputKind.VIDEO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.TRANSFORM, "video", "transform", OperationCategory.VIDEO, single(InputKind.VIDEO),
            listOf(
                width("size", "size"),
                height("size", "size"),
                OptionField("scale", FieldType.Decimal(0.0, OperationOptions.MAX_SCALE, minExclusive = true), unit = OptionUnit.TIMES, exclusiveGroup = "size", alternative = "scale"),
                OptionField("rotate", FieldType.Integer(-MAX_TIME, MAX_TIME), default = 0L, unit = OptionUnit.DEGREES, uiChoices = listOf(0, 90, 180, 270)),
                OptionField("crop", FieldType.Group(listOf(
                    OptionField("x", FieldType.Integer(0, Int.MAX_VALUE.toLong()), default = 0L, unit = OptionUnit.PIXELS),
                    OptionField("y", FieldType.Integer(0, Int.MAX_VALUE.toLong()), default = 0L, unit = OptionUnit.PIXELS),
                    OptionField("width", FieldType.Integer(OperationOptions.MIN_DIMENSION.toLong(), Int.MAX_VALUE.toLong()), required = true, unit = OptionUnit.PIXELS),
                    OptionField("height", FieldType.Integer(OperationOptions.MIN_DIMENSION.toLong(), Int.MAX_VALUE.toLong()), required = true, unit = OptionUnit.PIXELS),
                ))),
                OptionField("flip", FieldType.Choice(Flip.entries.map { it.id }), default = Flip.NONE.id),
                fit(), codec(), videoBitRate(),
            ),
            OutputKind.VIDEO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.TRANSCODE, "video", "transcode", OperationCategory.VIDEO, single(InputKind.VIDEO),
            listOf(
                width(), height(), fit(),
                codec().copy(advanced = false),
                videoBitRate().copy(advanced = false),
                OptionField("frameRate", FieldType.Decimal(OperationOptions.MIN_FRAME_RATE, OperationOptions.MAX_FRAME_RATE), unit = OptionUnit.FRAMES_PER_SECOND),
                OptionField("audio", FieldType.Group(listOf(audioBitRate(), sampleRate(OperationOptions.SAMPLE_RATES), channels())), advanced = true),
            ),
            OutputKind.VIDEO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.REMOVE_AUDIO, "video", "removeAudio", OperationCategory.VIDEO, single(InputKind.VIDEO),
            emptyList(), OutputKind.VIDEO, listOf(Slot.INPUT, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.EXTRACT_AUDIO, "video", "extractAudio", OperationCategory.VIDEO, single(InputKind.VIDEO),
            audioOutput(), OutputKind.AUDIO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.OVERLAY, "video", "overlay", OperationCategory.VIDEO, single(InputKind.VIDEO),
            listOf(
                OptionField(
                    "overlays",
                    FieldType.Items(1, OverlayOptions.MAX_OVERLAYS, "type", linkedMapOf(OverlayOptions.TYPE_TEXT to textOverlay, OverlayOptions.TYPE_IMAGE to imageOverlay)),
                    required = true,
                ),
                codec(), videoBitRate(),
            ),
            OutputKind.VIDEO, listOf(Slot.INPUT, Slot.OVERLAYS, Slot.OUTPUT, Slot.OPTIONS)),
        OperationSchema(MediaStudioOps.FRAMES, "video", "frames", OperationCategory.IMAGE, single(InputKind.VIDEO),
            listOf(
                time("at", group = "sampling", alternative = "at"),
                OptionField("times", FieldType.Numbers(0.0, MAX_TIME_DOUBLE, OperationOptions.MAX_FRAMES), unit = OptionUnit.MILLIS, exclusiveGroup = "sampling", alternative = "times"),
                OptionField("every", FieldType.Decimal(1.0, MAX_TIME_DOUBLE), unit = OptionUnit.MILLIS, exclusiveGroup = "sampling", alternative = "range"),
                OptionField("count", FieldType.Integer(1, OperationOptions.MAX_FRAMES.toLong()), exclusiveGroup = "sampling", alternative = "range"),
                time("start", group = "sampling", alternative = "range").copy(default = 0.0),
                time("end", group = "sampling", alternative = "range", minExclusive = true),
            ) + imageEncoding(ImageFormat.PNG, OperationOptions.DEFAULT_IMAGE_QUALITY),
            OutputKind.FOLDER, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.FRAME, "video", "frame", OperationCategory.IMAGE, single(InputKind.VIDEO),
            listOf(time("at", required = true)) + imageEncoding(ImageFormat.PNG, OperationOptions.DEFAULT_IMAGE_QUALITY),
            OutputKind.IMAGE, listOf(Slot.INPUT, Slot.AT, Slot.OPTIONS)),
        OperationSchema(MediaStudioOps.THUMBNAIL, "video", "thumbnail", OperationCategory.IMAGE, single(InputKind.VIDEO),
            listOf(time("at")) + imageEncoding(ImageFormat.JPG, OperationOptions.DEFAULT_THUMBNAIL_QUALITY),
            OutputKind.IMAGE, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.TO_GIF, "video", "toGif", OperationCategory.IMAGE, single(InputKind.VIDEO),
            listOf(
                time("start").copy(default = 0.0),
                time("end", minExclusive = true),
                OptionField("fps", FieldType.Integer(1, OperationOptions.MAX_GIF_FPS.toLong()), default = MediaStudioLimits.DEFAULT_GIF_FPS.toLong(), unit = OptionUnit.FRAMES_PER_SECOND),
                width(),
                OptionField("loop", FieldType.FlagOrInteger(0, GifEncoder.MAX_LOOP.toLong()), default = true),
                OptionField("dither", FieldType.Flag, default = false),
            ),
            OutputKind.GIF, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.AUDIO_TRIM, "audio", "trim", OperationCategory.AUDIO, single(InputKind.MEDIA),
            listOf(start(), end(), duration()) + audioOutput(), OutputKind.AUDIO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.AUDIO_CONCAT, "audio", "concat", OperationCategory.AUDIO, InputSpec(2, MediaStudioLimits.MAX_INPUTS, InputKind.MEDIA),
            audioOutput(), OutputKind.AUDIO, listOf(Slot.INPUTS, Slot.OUTPUT, Slot.OPTIONS)),
        OperationSchema(MediaStudioOps.AUDIO_MIX, "audio", "mix", OperationCategory.AUDIO, InputSpec(2, OperationOptions.MAX_MIX_INPUTS, InputKind.MEDIA),
            listOf(
                OptionField("tracks", FieldType.PerInput(listOf(
                    OptionField("volume", FieldType.Decimal(0.0, OperationOptions.MAX_GAIN), default = 1.0, unit = OptionUnit.TIMES),
                    OptionField("at", FieldType.Integer(0, MAX_TIME), default = 0L, unit = OptionUnit.MILLIS),
                ))),
            ) + audioOutput(),
            OutputKind.AUDIO, listOf(Slot.MIX_INPUTS, Slot.OUTPUT, Slot.OPTIONS)),
        OperationSchema(MediaStudioOps.AUDIO_VOLUME, "audio", "volume", OperationCategory.AUDIO, single(InputKind.MEDIA),
            listOf(
                OptionField("gain", FieldType.Decimal(0.0, OperationOptions.MAX_GAIN), unit = OptionUnit.TIMES, exclusiveGroup = "level", alternative = "gain"),
                OptionField("normalize", FieldType.FlagOrDecimal(OperationOptions.MIN_NORMALIZE_DBFS, 0.0), unit = OptionUnit.DBFS, exclusiveGroup = "level", alternative = "normalize"),
                OptionField("fadeIn", FieldType.Integer(0, MAX_TIME), default = 0L, unit = OptionUnit.MILLIS),
                OptionField("fadeOut", FieldType.Integer(0, MAX_TIME), default = 0L, unit = OptionUnit.MILLIS),
            ) + audioOutput(),
            OutputKind.AUDIO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.AUDIO_CONVERT, "audio", "convert", OperationCategory.AUDIO, single(InputKind.MEDIA),
            audioOutput(formatRequired = true), OutputKind.AUDIO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.AUDIO_SPEED, "audio", "speed", OperationCategory.AUDIO, single(InputKind.MEDIA),
            listOf(factor(), keepPitch()) + audioOutput(), OutputKind.AUDIO, listOf(Slot.INPUT, Slot.OPTIONS, Slot.OUTPUT)),
        OperationSchema(MediaStudioOps.BURN_SUBTITLES, "video", "burnSubtitles", OperationCategory.SUBTITLE, single(InputKind.VIDEO),
            listOf(
                OptionField("subtitles", FieldType.FilePath(FileKind.SUBTITLE), required = true),
                OptionField("style", FieldType.Group(subtitleStyle)),
            ),
            OutputKind.VIDEO, listOf(Slot.INPUT, Slot.SUBTITLES, Slot.STYLE, Slot.OUTPUT)),
        OperationSchema(OperationSchema.SUBTITLES_CONVERT, "video", "subtitles.write", OperationCategory.SUBTITLE, single(InputKind.SUBTITLE),
            listOf(OptionField("format", FieldType.Choice(SubtitleFormat.IDS), required = true)),
            OutputKind.SUBTITLE, listOf(Slot.INPUT, Slot.OUTPUT, Slot.OPTIONS)),
    )

    private val byOp: Map<String, OperationSchema> = ALL.associateBy { it.op }

    fun of(op: String): OperationSchema? = byOp[op]

    fun inCategory(category: OperationCategory): List<OperationSchema> = ALL.filter { it.category == category }

}

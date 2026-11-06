package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OverlayOptions.color
import kotlin.math.roundToInt

/**
 * The `style` object of `burnSubtitles` and `timeline.subtitles` (roadmap P3.3); null keeps the default of the cue
 * (the ASS style, or the SRT defaults of [SubtitleStyles]). Sizes are output pixels; [align] places the text block
 * like an overlay anchor (`bottom` by default), [marginV] / [marginH] keep it off the frame edges.
 * zh-CN: `burnSubtitles` 与 `timeline.subtitles` 的 `style` 对象 (路线图 P3.3); null 表示沿用 cue 的默认值 (ASS 样式, 或
 * [SubtitleStyles] 的 SRT 默认值). 尺寸为输出像素; [align] 像叠加锚点一样放置文字块 (默认 `bottom`), [marginV] / [marginH]
 * 使其与画面边缘保持距离.
 */
internal data class SubtitleStyleOptions(
    val font: String? = null,
    val fontSize: Double? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val color: Int? = null,
    val stroke: Int? = null,
    val strokeWidth: Double? = null,
    val shadow: Int? = null,
    val shadowRadius: Double? = null,
    val shadowDx: Double? = null,
    val shadowDy: Double? = null,
    val background: Int? = null,
    val align: Anchor? = null,
    val marginV: Length? = null,
    val marginH: Length? = null,
) {
    val fontFile: String? get() = font?.takeIf { it.startsWith("/") }
}

/** Where the cues come from: a subtitle file or a cue array. zh-CN: cue 的来源: 字幕文件或 cue 数组. */
internal sealed interface SubtitleSource {
    data class File(val path: String) : SubtitleSource
    data class Cues(val cues: List<SubtitleCue>) : SubtitleSource
}

/** Validated subtitles of `burnSubtitles` or a timeline. zh-CN: `burnSubtitles` 或时间线中经校验的字幕. */
internal data class SubtitlesSpec(val source: SubtitleSource, val style: SubtitleStyleOptions) {
    /** Files the task reads besides its inputs. zh-CN: 任务在输入之外读取的文件. */
    val files: List<String> get() = listOfNotNull((source as? SubtitleSource.File)?.path, style.fontFile)
}

/**
 * The option schemas of the subtitle operations (roadmap P3.3, appendix B.1): `burnSubtitles` (`subtitles` path or
 * `cues`, `style`), `subtitlesParse` (no options), `subtitlesWrite` (`cues`, `format`) and the `subtitles` key of the
 * `compose` timeline (`src` or `cues`, `style`). A cue is `{start, end, text}` in milliseconds (end exclusive), the text
 * may hold SRT markup. The host copies the rules (appendix D Q24).
 * zh-CN: 字幕操作的选项模式 (路线图 P3.3, 附录 B.1): `burnSubtitles` (`subtitles` 路径或 `cues`, `style`), `subtitlesParse` (无选项),
 * `subtitlesWrite` (`cues`, `format`) 与 `compose` 时间线的 `subtitles` 键 (`src` 或 `cues`, `style`). cue 为毫秒的
 * `{start, end, text}` (end 不含), 文本可含 SRT 标记. 宿主照抄这些规则 (附录 D Q24).
 */
internal object SubtitleOptions {

    /** Cues of one array; a feature film has about 1500. zh-CN: 一个数组的 cue 上限; 一部电影约 1500 条. */
    const val MAX_CUES = 10_000
    const val MAX_TEXT_CHARS = 1000

    fun cues(reader: OptionReader, key: String): List<SubtitleCue> {
        val items = reader.objects(key) ?: throw MediaStudioException.invalid("${reader.name(key)} is required")
        if (items.isEmpty()) throw MediaStudioException.invalid("${reader.name(key)} must hold at least 1 cue")
        if (items.size > MAX_CUES) throw OperationOptions.limit("${reader.name(key)} holds ${items.size} cues, at most $MAX_CUES")
        return items.map { item ->
            val start = item.long("start", 0, OperationOptions.MAX_TIME_MILLIS) ?: throw MediaStudioException.invalid("${item.name("start")} is required")
            val end = item.long("end", 1, OperationOptions.MAX_TIME_MILLIS) ?: throw MediaStudioException.invalid("${item.name("end")} is required")
            if (end <= start) throw MediaStudioException.invalid("${item.name("end")} ($end) must be greater than ${item.name("start")} ($start)")
            val text = item.string("text") ?: throw MediaStudioException.invalid("${item.name("text")} is required")
            if (text.isBlank() || text.length > MAX_TEXT_CHARS) {
                throw MediaStudioException.invalid("${item.name("text")} must have 1 to $MAX_TEXT_CHARS characters and not be blank, got ${text.length}")
            }
            item.finish()
            SubtitleCue(start, end, text, SubtitleMarkup.SRT)
        }
    }

    /**
     * `subtitles` (an absolute subtitle file path, [fileKey]) or `cues`, exactly one, else "[owner] needs ..."; then `style`. [encoding] reads the
     * `codec` / `bitRate` of `burnSubtitles` from the style object (the script passes one object after the subtitles).
     * zh-CN: `subtitles` (字幕文件绝对路径, [fileKey]) 或 `cues`, 二者恰好一个; 然后是 `style`. [encoding] 从样式对象读取
     * `burnSubtitles` 的 `codec` / `bitRate` (脚本在字幕之后只传一个对象).
     */
    fun <T> source(reader: OptionReader, owner: String, fileKey: String, encoding: (OptionReader) -> T): Triple<SubtitleSource, SubtitleStyleOptions, T?> {
        val hasFile = reader.has(fileKey)
        val hasCues = reader.has("cues")
        if (hasFile && hasCues) throw MediaStudioException.invalid("${reader.name(fileKey)} and ${reader.name("cues")} are exclusive")
        val source = when {
            hasFile -> {
                val path = reader.string(fileKey)!!
                if (!path.startsWith("/")) throw MediaStudioException.invalid("${reader.name(fileKey)} must be an absolute path, got $path")
                SubtitleSource.File(path)
            }
            hasCues -> SubtitleSource.Cues(cues(reader, "cues"))
            else -> throw MediaStudioException.invalid("$owner needs ${reader.name(fileKey)} or ${reader.name("cues")}")
        }
        var extra: T? = null
        val style = reader.child("style")?.let { child ->
            style(child).also {
                extra = encoding(child)
                child.finish()
            }
        } ?: SubtitleStyleOptions()
        return Triple(source, style, extra)
    }

    /** `timeline.subtitles`: `src` or `cues`, and `style`. zh-CN: `timeline.subtitles`: `src` 或 `cues`, 以及 `style`. */
    fun timeline(reader: OptionReader): SubtitlesSpec {
        val (source, style) = source(reader, reader.name("").removeSuffix("."), "src") { }
        reader.finish()
        return SubtitlesSpec(source, style)
    }

    fun style(reader: OptionReader): SubtitleStyleOptions {
        val font = reader.string("font")?.also { font ->
            if (font !in OverlayOptions.FONT_FAMILIES && !font.startsWith("/")) {
                throw MediaStudioException.invalid("${reader.name("font")} must be one of ${OverlayOptions.FONT_FAMILIES.joinToString()} or an absolute font file path, got $font")
            }
        }
        return SubtitleStyleOptions(
            font = font,
            fontSize = reader.double("fontSize", 0.0, OverlayOptions.MAX_FONT_SIZE, minExclusive = true),
            bold = reader.bool("bold"),
            italic = reader.bool("italic"),
            color = reader.color("color"),
            stroke = reader.color("stroke"),
            strokeWidth = reader.double("strokeWidth", 0.0, OverlayOptions.MAX_EFFECT_PIXELS),
            shadow = reader.color("shadow"),
            shadowRadius = reader.double("shadowRadius", 0.0, OverlayOptions.MAX_EFFECT_PIXELS),
            shadowDx = reader.double("shadowDx", -OverlayOptions.MAX_EFFECT_PIXELS, OverlayOptions.MAX_EFFECT_PIXELS),
            shadowDy = reader.double("shadowDy", -OverlayOptions.MAX_EFFECT_PIXELS, OverlayOptions.MAX_EFFECT_PIXELS),
            background = reader.color("background"),
            align = reader.choice("align", Anchor.entries.map { it.id })?.let(Anchor::of),
            marginV = reader.margin("marginV"),
            marginH = reader.margin("marginH"),
        )
    }

    /** A margin: pixels 0 - 4096 or a percentage 0 - 50 %. zh-CN: 边距: 像素 0 - 4096 或百分比 0 - 50 %. */
    private fun OptionReader.margin(key: String): Length? {
        val raw = numberOrString(key) ?: return null
        return when (raw) {
            is Double -> {
                if (raw < 0.0 || raw > MAX_MARGIN_PIXELS) throw MediaStudioException.invalid("${name(key)} must be at least 0 px and at most ${MAX_MARGIN_PIXELS.roundToInt()} px, got ${format(raw)}")
                Length.Pixels(raw)
            }
            is String -> {
                val text = raw.trim()
                val number = text.removeSuffix("%").trim().toDoubleOrNull()
                if (!text.endsWith("%") || number == null || number.isNaN() || number.isInfinite()) {
                    throw MediaStudioException.invalid("${name(key)} must be a number of pixels or a percentage like \"5%\", got $raw")
                }
                if (number < 0.0 || number > MAX_MARGIN_PERCENT) throw MediaStudioException.invalid("${name(key)} must be at least 0% and at most ${MAX_MARGIN_PERCENT.roundToInt()}%, got $raw")
                Length.Percent(number)
            }
            else -> error("unreachable")
        }
    }

    const val MAX_MARGIN_PIXELS = 4096.0
    const val MAX_MARGIN_PERCENT = 50.0
}

/**
 * The drawing parameters of one cue in output pixels: [alignment] is the ASS numpad position (1 - 3 bottom, 4 - 6
 * middle, 7 - 9 top), [scaleX] / [scaleY] map script pixels (inline `\fs` and `\pos` of ASS) to output pixels.
 * zh-CN: 一条 cue 以输出像素表示的绘制参数: [alignment] 为 ASS 小键盘位置 (1 - 3 底部, 4 - 6 中部, 7 - 9 顶部), [scaleX] /
 * [scaleY] 把脚本像素 (ASS 的内联 `\fs` 与 `\pos`) 换算为输出像素.
 */
internal data class ResolvedSubtitleStyle(
    val font: String,
    val fontSize: Double,
    val bold: Boolean,
    val italic: Boolean,
    val color: Int,
    val strokeColor: Int?,
    val strokeWidth: Double,
    val shadowColor: Int?,
    val shadowRadius: Double,
    val shadowDx: Double,
    val shadowDy: Double,
    val boxColor: Int?,
    val boxPadding: Double,
    val alignment: Int,
    val marginL: Double,
    val marginR: Double,
    val marginV: Double,
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
)

/**
 * Resolves the style of a cue (roadmap D31): the ASS style scaled from `PlayResX` / `PlayResY` to the frame, or the SRT
 * defaults (font size = frame height / 18, white with a black outline of font size / 8, bottom center, margins 5 %), then
 * the script `style` keys on top. The inline tags of a cue refine it per run in the renderer.
 * zh-CN: 解析一条 cue 的样式 (路线图 D31): ASS 样式按 `PlayResX` / `PlayResY` 缩放到画面, 或 SRT 默认值 (字号 = 画面高 / 18, 白字黑边,
 * 描边宽度为字号 / 8, 底部居中, 边距 5 %), 再叠加脚本 `style` 的键. cue 的内联标签在渲染器中按片段细化.
 */
internal object SubtitleStyles {

    const val DEFAULT_HEIGHT_DIVISOR = 18.0
    const val DEFAULT_STROKE_DIVISOR = 8.0
    const val DEFAULT_MARGIN_PERCENT = 5.0

    /** A shadow without blur (ASS) keeps a tiny radius, since 0 removes the shadow layer. zh-CN: 无模糊的阴影 (ASS) 保留极小半径, 半径为 0 会移除阴影层. */
    const val HARD_SHADOW_RADIUS = 0.01

    fun resolve(track: SubtitleTrack, cue: SubtitleCue, options: SubtitleStyleOptions, width: Int, height: Int): ResolvedSubtitleStyle {
        val ass = track.styleOf(cue)
        val base = if (ass != null) {
            val sx = width.toDouble() / (track.playResX ?: SubtitleFiles.DEFAULT_PLAY_RES_X)
            val sy = height.toDouble() / (track.playResY ?: SubtitleFiles.DEFAULT_PLAY_RES_Y)
            val box = ass.borderStyle == 3
            ResolvedSubtitleStyle(
                font = ass.font,
                fontSize = ass.fontSize * sy,
                bold = ass.bold,
                italic = ass.italic,
                color = ass.primaryColor,
                strokeColor = if (!box && ass.outline > 0) ass.outlineColor else null,
                // ASS outline is the border outside the glyph; a Paint stroke is centered. zh-CN: ASS 描边在字形外侧; Paint 描边居中.
                strokeWidth = if (!box) ass.outline * sy * 2 else 0.0,
                shadowColor = if (ass.shadow > 0) ass.backColor else null,
                shadowRadius = HARD_SHADOW_RADIUS,
                shadowDx = ass.shadow * sy,
                shadowDy = ass.shadow * sy,
                boxColor = if (box) ass.outlineColor else null,
                boxPadding = ass.outline * sy,
                alignment = ass.alignment,
                marginL = (cue.marginL.takeIf { it != 0 }?.toDouble() ?: ass.marginL) * sx,
                marginR = (cue.marginR.takeIf { it != 0 }?.toDouble() ?: ass.marginR) * sx,
                marginV = (cue.marginV.takeIf { it != 0 }?.toDouble() ?: ass.marginV) * sy,
                scaleX = sx,
                scaleY = sy,
            )
        } else {
            val fontSize = options.fontSize ?: (height / DEFAULT_HEIGHT_DIVISOR)
            ResolvedSubtitleStyle(
                font = OverlayOptions.FONT_FAMILIES.first(),
                fontSize = fontSize,
                bold = false,
                italic = false,
                color = Colors.WHITE,
                strokeColor = Colors.BLACK,
                strokeWidth = fontSize / DEFAULT_STROKE_DIVISOR,
                shadowColor = null,
                shadowRadius = OverlayOptions.DEFAULT_SHADOW_RADIUS,
                shadowDx = OverlayOptions.DEFAULT_SHADOW_OFFSET,
                shadowDy = OverlayOptions.DEFAULT_SHADOW_OFFSET,
                boxColor = null,
                boxPadding = fontSize / DEFAULT_STROKE_DIVISOR,
                alignment = 2,
                marginL = width * DEFAULT_MARGIN_PERCENT / 100,
                marginR = width * DEFAULT_MARGIN_PERCENT / 100,
                marginV = height * DEFAULT_MARGIN_PERCENT / 100,
            )
        }
        return apply(base, options, width, height)
    }

    private fun apply(base: ResolvedSubtitleStyle, options: SubtitleStyleOptions, width: Int, height: Int): ResolvedSubtitleStyle {
        val strokeWidth = options.strokeWidth ?: base.strokeWidth
        val stroked = (options.stroke ?: base.strokeColor) != null && strokeWidth > 0.0
        val shadowGiven = options.shadow != null || options.shadowRadius != null || options.shadowDx != null || options.shadowDy != null
        val shadowColor = options.shadow ?: base.shadowColor ?: if (shadowGiven) Colors.BLACK else null
        return base.copy(
            font = options.font ?: base.font,
            fontSize = options.fontSize ?: base.fontSize,
            bold = options.bold ?: base.bold,
            italic = options.italic ?: base.italic,
            color = options.color ?: base.color,
            strokeColor = if (stroked) options.stroke ?: base.strokeColor ?: Colors.BLACK else null,
            strokeWidth = if (stroked) strokeWidth else 0.0,
            shadowColor = shadowColor,
            shadowRadius = options.shadowRadius ?: if (shadowGiven && base.shadowColor == null) OverlayOptions.DEFAULT_SHADOW_RADIUS else base.shadowRadius,
            shadowDx = options.shadowDx ?: base.shadowDx,
            shadowDy = options.shadowDy ?: base.shadowDy,
            boxColor = options.background ?: base.boxColor,
            alignment = options.align?.let(::numpad) ?: base.alignment,
            marginL = options.marginH?.resolve(width) ?: base.marginL,
            marginR = options.marginH?.resolve(width) ?: base.marginR,
            marginV = options.marginV?.resolve(height) ?: base.marginV,
        )
    }

    /** The ASS numpad position of an anchor (`center` is 5). zh-CN: 锚点对应的 ASS 小键盘位置 (`center` 为 5). */
    fun numpad(anchor: Anchor): Int = when (anchor) {
        Anchor.BOTTOM_LEFT -> 1
        Anchor.BOTTOM -> 2
        Anchor.BOTTOM_RIGHT -> 3
        Anchor.LEFT -> 4
        Anchor.CENTER -> 5
        Anchor.RIGHT -> 6
        Anchor.TOP_LEFT -> 7
        Anchor.TOP -> 8
        Anchor.TOP_RIGHT -> 9
    }
}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import kotlin.math.roundToInt

/**
 * A length in pixels of the output frame (a number) or in percent of the frame width or height (a string `"25%"`).
 * zh-CN: 以输出帧像素表示 (数字) 或以帧宽 / 高的百分比表示 (字符串 `"25%"`) 的长度.
 */
internal sealed interface Length {

    fun resolve(frame: Int): Double

    data class Pixels(val value: Double) : Length {
        override fun resolve(frame: Int) = value
    }

    data class Percent(val value: Double) : Length {
        override fun resolve(frame: Int) = frame * value / 100.0
    }

}

/**
 * The point of an overlay that `x` / `y` place; `center` by default. [fx] / [fy] are its fractions of the overlay
 * width and height from the top left corner.
 * zh-CN: 叠加物上由 `x` / `y` 定位的点, 默认 `center`. [fx] / [fy] 为该点距左上角的宽 / 高比例.
 */
internal enum class Anchor(val id: String, val fx: Double, val fy: Double) {
    CENTER("center", 0.5, 0.5),
    TOP_LEFT("top-left", 0.0, 0.0),
    TOP("top", 0.5, 0.0),
    TOP_RIGHT("top-right", 1.0, 0.0),
    LEFT("left", 0.0, 0.5),
    RIGHT("right", 1.0, 0.5),
    BOTTOM_LEFT("bottom-left", 0.0, 1.0),
    BOTTOM("bottom", 0.5, 1.0),
    BOTTOM_RIGHT("bottom-right", 1.0, 1.0),
    ;

    companion object {
        fun of(id: String): Anchor = entries.first { it.id == id }
    }
}

/** Alignment of the lines of a multi-line text. zh-CN: 多行文字各行的对齐方式. */
internal enum class TextAlign(val id: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right"),
    ;

    companion object {
        fun of(id: String): TextAlign = entries.first { it.id == id }
    }
}

/**
 * Where and when one overlay shows: [x] / [y] place its [anchor] point, [fromMillis] is inclusive, [toMillis] exclusive
 * and null for "until the end", [alpha] multiplies its opacity.
 * zh-CN: 叠加物的显示位置与时段: [x] / [y] 定位其 [anchor] 点, [fromMillis] 含, [toMillis] 不含且 null 表示到结尾, [alpha] 乘以
 * 其不透明度.
 */
internal data class OverlayPlacement(
    val x: Length,
    val y: Length,
    val anchor: Anchor,
    val fromMillis: Long,
    val toMillis: Long?,
    val alpha: Double,
) {
    fun isActive(timeMillis: Double): Boolean = timeMillis >= fromMillis && (toMillis == null || timeMillis < toMillis)

    /**
     * The top left corner of an item of [itemWidth] x [itemHeight] in a frame of [frameWidth] x [frameHeight], in pixels.
     * zh-CN: [itemWidth] x [itemHeight] 的叠加物在 [frameWidth] x [frameHeight] 帧中的左上角像素坐标.
     */
    fun topLeft(frameWidth: Int, frameHeight: Int, itemWidth: Double, itemHeight: Double): Pair<Double, Double> =
        x.resolve(frameWidth) - anchor.fx * itemWidth to y.resolve(frameHeight) - anchor.fy * itemHeight
}

/** One validated overlay of `overlay` (roadmap P2.3, D29). zh-CN: `overlay` 的一个经校验的叠加物 (路线图 P2.3, D29). */
internal sealed interface OverlayItem {

    val placement: OverlayPlacement

    /**
     * [font] is a family of [OverlayOptions.FONT_FAMILIES] or an absolute font file path; sizes are output pixels; colors
     * are ARGB; a null [strokeColor] / [shadowColor] turns the stroke / shadow off.
     * zh-CN: [font] 为 [OverlayOptions.FONT_FAMILIES] 之一或字体文件的绝对路径; 尺寸单位为输出像素; 颜色为 ARGB; [strokeColor] /
     * [shadowColor] 为 null 时不描边 / 无阴影.
     */
    data class Text(
        val text: String,
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
        val align: TextAlign,
        override val placement: OverlayPlacement,
    ) : OverlayItem {
        val fontFile: String? get() = font.takeIf { it.startsWith("/") }
    }

    /**
     * [scale] multiplies the pixel size of the picture; [width] / [height] set it instead (one of them keeps the ratio).
     * zh-CN: [scale] 乘以图片的像素尺寸; [width] / [height] 直接指定尺寸 (只给一个时保持宽高比).
     */
    data class Image(
        val src: String,
        val scale: Double?,
        val width: Length?,
        val height: Length?,
        override val placement: OverlayPlacement,
    ) : OverlayItem {

        /**
         * The drawn size of a [pictureWidth] x [pictureHeight] picture in a [frameWidth] x [frameHeight] frame.
         * zh-CN: [pictureWidth] x [pictureHeight] 的图片在 [frameWidth] x [frameHeight] 帧中的绘制尺寸.
         */
        fun drawnSize(pictureWidth: Int, pictureHeight: Int, frameWidth: Int, frameHeight: Int): Pair<Double, Double> {
            val w = width?.resolve(frameWidth)
            val h = height?.resolve(frameHeight)
            return when {
                w != null && h != null -> w to h
                w != null -> w to w * pictureHeight / pictureWidth
                h != null -> h * pictureWidth / pictureHeight to h
                else -> (scale ?: 1.0).let { pictureWidth * it to pictureHeight * it }
            }
        }
    }

}

/**
 * The option schema of `overlays[]` (roadmap P2.3, appendix B.1); [OperationOptions] reads `options.overlays` with it
 * and roadmap P3 reuses it for `timeline.overlays`. The host copies the rules from the hand-over note
 * `3-slate-p2b-notes.md`.
 * zh-CN: `overlays[]` 的选项模式 (路线图 P2.3, 附录 B.1); [OperationOptions] 用它读取 `options.overlays`, 路线图 P3 的
 * `timeline.overlays` 复用它. 宿主按交接笔记 `3-slate-p2b-notes.md` 照抄规则.
 */
internal object OverlayOptions {

    const val MAX_OVERLAYS = 64
    const val MAX_TEXT_CHARS = 1000
    const val MAX_FONT_SIZE = 1000.0
    const val DEFAULT_FONT_SIZE = 48.0
    const val MAX_EFFECT_PIXELS = 100.0

    /** Coordinates beyond twice the largest frame are mistakes. zh-CN: 超过最大帧两倍的坐标视为错误. */
    const val MAX_PIXELS = 16384.0
    const val MAX_PERCENT = 1000.0

    const val DEFAULT_SHADOW_RADIUS = 4.0
    const val DEFAULT_SHADOW_OFFSET = 2.0

    /** Stroke width without `strokeWidth`: font size / 16. zh-CN: 未给 `strokeWidth` 时的描边宽度: 字号 / 16. */
    const val DEFAULT_STROKE_DIVISOR = 16.0

    const val TYPE_TEXT = "text"
    const val TYPE_IMAGE = "image"

    val FONT_FAMILIES: List<String> = listOf("sans-serif", "serif", "monospace")

    private val DEFAULT_POSITION = Length.Percent(50.0)

    fun parse(reader: OptionReader, key: String): List<OverlayItem> {
        val items = reader.objects(key) ?: throw MediaStudioException.invalid("${reader.name(key)} is required")
        if (items.isEmpty() || items.size > MAX_OVERLAYS) {
            throw MediaStudioException.invalid("${reader.name(key)} must hold 1 to $MAX_OVERLAYS overlays, got ${items.size}")
        }
        return items.map { item -> parseItem(item).also { item.finish() } }
    }

    private fun parseItem(reader: OptionReader): OverlayItem {
        val type = reader.choice("type", listOf(TYPE_TEXT, TYPE_IMAGE)) ?: throw MediaStudioException.invalid("${reader.name("type")} is required")
        val item = if (type == TYPE_TEXT) text(reader) else image(reader)
        return item
    }

    private fun text(reader: OptionReader): OverlayItem.Text {
        val text = reader.string("text") ?: throw MediaStudioException.invalid("${reader.name("text")} is required")
        if (text.isEmpty() || text.length > MAX_TEXT_CHARS) {
            throw MediaStudioException.invalid("${reader.name("text")} must have 1 to $MAX_TEXT_CHARS characters, got ${text.length}")
        }
        val font = reader.string("font")?.also { font ->
            if (font !in FONT_FAMILIES && !font.startsWith("/")) {
                throw MediaStudioException.invalid("${reader.name("font")} must be one of ${FONT_FAMILIES.joinToString()} or an absolute font file path, got $font")
            }
        } ?: FONT_FAMILIES.first()
        val fontSize = reader.double("fontSize", 0.0, MAX_FONT_SIZE, minExclusive = true) ?: DEFAULT_FONT_SIZE
        val strokeColor = reader.color("stroke")
        val strokeWidth = reader.double("strokeWidth", 0.0, MAX_EFFECT_PIXELS)
        val shadowColor = reader.color("shadow")
        val shadowRadius = reader.double("shadowRadius", 0.0, MAX_EFFECT_PIXELS)
        val shadowDx = reader.double("shadowDx", -MAX_EFFECT_PIXELS, MAX_EFFECT_PIXELS)
        val shadowDy = reader.double("shadowDy", -MAX_EFFECT_PIXELS, MAX_EFFECT_PIXELS)
        val stroked = strokeColor != null || (strokeWidth != null && strokeWidth > 0.0)
        val shadowed = shadowColor != null || shadowRadius != null || shadowDx != null || shadowDy != null
        return OverlayItem.Text(
            text = text,
            font = font,
            fontSize = fontSize,
            bold = reader.bool("bold") ?: false,
            italic = reader.bool("italic") ?: false,
            color = reader.color("color") ?: Colors.WHITE,
            strokeColor = if (stroked) strokeColor ?: Colors.BLACK else null,
            strokeWidth = if (stroked) strokeWidth ?: (fontSize / DEFAULT_STROKE_DIVISOR) else 0.0,
            shadowColor = if (shadowed) shadowColor ?: Colors.BLACK else null,
            shadowRadius = shadowRadius ?: DEFAULT_SHADOW_RADIUS,
            shadowDx = shadowDx ?: DEFAULT_SHADOW_OFFSET,
            shadowDy = shadowDy ?: DEFAULT_SHADOW_OFFSET,
            align = reader.choice("align", TextAlign.entries.map { it.id })?.let(TextAlign::of) ?: TextAlign.CENTER,
            placement = placement(reader),
        )
    }

    private fun image(reader: OptionReader): OverlayItem.Image {
        val src = reader.string("src") ?: throw MediaStudioException.invalid("${reader.name("src")} is required")
        if (!src.startsWith("/")) throw MediaStudioException.invalid("${reader.name("src")} must be an absolute path, got $src")
        val scale = reader.double("scale", 0.0, OperationOptions.MAX_SCALE, minExclusive = true)
        val width = reader.length("width", positive = true)
        val height = reader.length("height", positive = true)
        if (scale != null && (width != null || height != null)) {
            throw MediaStudioException.invalid("${reader.name("scale")} and ${reader.name("width")} / ${reader.name("height")} are exclusive")
        }
        return OverlayItem.Image(src, scale, width, height, placement(reader))
    }

    private fun placement(reader: OptionReader): OverlayPlacement {
        val from = reader.long("from", 0, OperationOptions.MAX_TIME_MILLIS) ?: 0L
        val to = reader.long("to", 1, OperationOptions.MAX_TIME_MILLIS)
        if (to != null && to <= from) {
            throw MediaStudioException.invalid("${reader.name("to")} ($to) must be greater than ${reader.name("from")} ($from)")
        }
        return OverlayPlacement(
            x = reader.length("x", positive = false) ?: DEFAULT_POSITION,
            y = reader.length("y", positive = false) ?: DEFAULT_POSITION,
            anchor = reader.choice("anchor", Anchor.entries.map { it.id })?.let(Anchor::of) ?: Anchor.CENTER,
            fromMillis = from,
            toMillis = to,
            alpha = reader.double("alpha", 0.0, 1.0) ?: 1.0,
        )
    }

    /** Pixels as a number, percent as `"NN%"`. zh-CN: 像素为数字, 百分比为 `"NN%"`. */
    private fun OptionReader.length(key: String, positive: Boolean): Length? {
        val raw = numberOrString(key) ?: return null
        val (value, percent) = when (raw) {
            is Double -> raw to false
            is String -> {
                val text = raw.trim()
                val number = text.removeSuffix("%").trim().toDoubleOrNull()
                if (!text.endsWith("%") || number == null || number.isNaN() || number.isInfinite()) {
                    throw MediaStudioException.invalid("${name(key)} must be a number of pixels or a percentage like \"50%\", got $raw")
                }
                number to true
            }
            else -> error("unreachable")
        }
        val limit = if (percent) MAX_PERCENT else MAX_PIXELS
        val below = if (positive) value <= 0.0 else value < -limit
        if (below || value > limit) {
            val unit = if (percent) "%" else " px"
            val lower = if (positive) "greater than 0" else "at least -${limit.roundToInt()}$unit"
            throw MediaStudioException.invalid("${name(key)} must be $lower and at most ${limit.roundToInt()}$unit, got ${if (raw is Double) format(raw) else raw}")
        }
        return if (percent) Length.Percent(value) else Length.Pixels(value)
    }

    private fun OptionReader.color(key: String): Int? {
        val raw = numberOrString(key) ?: return null
        return when (raw) {
            is String -> Colors.parse(raw) ?: throw MediaStudioException.invalid(
                "${name(key)} must be #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer, got $raw",
            )
            is Double -> {
                if (raw != Math.floor(raw) || raw < Int.MIN_VALUE.toDouble() || raw > 0xFFFF_FFFFL.toDouble()) {
                    throw MediaStudioException.invalid("${name(key)} as a number must be an ARGB integer, got ${format(raw)}")
                }
                raw.toLong().toInt()
            }
            else -> error("unreachable")
        }
    }

}

/**
 * Color strings of the overlay options: `#RGB`, `#RRGGBB`, `#AARRGGBB` and the color names of `android.graphics.Color`,
 * case-insensitive; pure Kotlin so JVM tests and the host share the rules.
 * zh-CN: 叠加选项的颜色字符串: `#RGB`, `#RRGGBB`, `#AARRGGBB` 与 `android.graphics.Color` 的颜色名, 不区分大小写; 纯 Kotlin
 * 实现, 便于 JVM 测试与宿主共用规则.
 */
internal object Colors {

    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF000000.toInt()

    val NAMES: Map<String, Int> = mapOf(
        "black" to BLACK,
        "darkgray" to 0xFF444444.toInt(),
        "gray" to 0xFF888888.toInt(),
        "lightgray" to 0xFFCCCCCC.toInt(),
        "white" to WHITE,
        "red" to 0xFFFF0000.toInt(),
        "green" to 0xFF00FF00.toInt(),
        "blue" to 0xFF0000FF.toInt(),
        "yellow" to 0xFFFFFF00.toInt(),
        "cyan" to 0xFF00FFFF.toInt(),
        "magenta" to 0xFFFF00FF.toInt(),
        "aqua" to 0xFF00FFFF.toInt(),
        "fuchsia" to 0xFFFF00FF.toInt(),
        "darkgrey" to 0xFF444444.toInt(),
        "grey" to 0xFF888888.toInt(),
        "lightgrey" to 0xFFCCCCCC.toInt(),
        "lime" to 0xFF00FF00.toInt(),
        "maroon" to 0xFF800000.toInt(),
        "navy" to 0xFF000080.toInt(),
        "olive" to 0xFF808000.toInt(),
        "purple" to 0xFF800080.toInt(),
        "silver" to 0xFFC0C0C0.toInt(),
        "teal" to 0xFF008080.toInt(),
        "transparent" to 0,
    )

    fun parse(text: String): Int? {
        val value = text.trim().lowercase()
        if (!value.startsWith("#")) return NAMES[value]
        val hex = value.substring(1)
        if (hex.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
        return when (hex.length) {
            3 -> (0xFF000000L or hex.map { "$it$it" }.joinToString("").toLong(16)).toInt()
            6 -> (0xFF000000L or hex.toLong(16)).toInt()
            8 -> hex.toLong(16).toInt()
            else -> null
        }
    }

}

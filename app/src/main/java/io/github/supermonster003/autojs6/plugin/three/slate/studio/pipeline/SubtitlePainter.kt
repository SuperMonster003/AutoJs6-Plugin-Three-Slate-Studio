package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.UnderlineSpan
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameSize
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OverlayOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.ResolvedSubtitleStyle
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.RichText
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleMarkups
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleStyleOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleStyles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleTrack
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleWarnings
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Draws the cues of a [SubtitleTrack] (roadmap P3.3, D31) with `StaticLayout`: the resolved style of each cue
 * ([SubtitleStyles]), the inline runs of [SubtitleMarkups] as spans, an outline pass under the fill pass, an optional box
 * behind each line (ASS `BorderStyle` 3 or `style.background`), and the shadow. Cues without `\pos` that show at the
 * same time stack instead of overlapping: at the bottom the earlier cue stays lowest and later ones go above it, at the
 * top the earlier stays highest; the order is layer, start, file order. The [OverlayRenderer] calls it when the set of
 * visible cues changes; it draws above the overlays.
 * zh-CN: 用 `StaticLayout` 绘制 [SubtitleTrack] 的 cue (路线图 P3.3, D31): 每条 cue 的解析样式 ([SubtitleStyles]), [SubtitleMarkups]
 * 的内联片段转为 span, 先描边后填充, 可选的每行底框 (ASS `BorderStyle` 3 或 `style.background`), 以及阴影. 没有 `\pos` 且同时显示的
 * cue 依次堆叠而不重叠: 底部时较早的 cue 在最下, 后来的在其上方; 顶部时较早的在最上; 顺序为图层, 开始时间, 文件顺序. 可见 cue 的
 * 集合变化时由 [OverlayRenderer] 调用, 画在叠加物之上.
 */
internal class SubtitlePainter(private val track: SubtitleTrack, private val options: SubtitleStyleOptions, warnings: SubtitleWarnings) {

    private val texts: List<RichText> = track.cues.map { SubtitleMarkups.parse(it.text, it.markup, track.wrapStyle, warnings) }

    private val typefaces = HashMap<String, Typeface>()

    init {
        val fonts = track.styles.values.map { it.font } + listOfNotNull(options.font) + OverlayOptions.FONT_FAMILIES.first()
        fonts.distinct().forEach { typefaces[it] = loadTypeface(it) }
    }

    /** Indices of the cues shown at [timeMillis]. zh-CN: [timeMillis] 时显示的 cue 的索引. */
    fun visible(timeMillis: Double): List<Int> = track.cues.indices.filter { track.cues[it].isActive(timeMillis) && texts[it].plain.isNotBlank() }

    private class Block(val style: ResolvedSubtitleStyle, val layout: StaticLayout, val stroke: StaticLayout?, val width: Int, val height: Int, val text: RichText)

    fun draw(canvas: Canvas, indices: List<Int>, size: FrameSize) {
        val order = indices.sortedWith(compareBy({ track.cues[it].layer }, { track.cues[it].startMillis }, { it }))
        val blocks = order.map { block(it, size) }
        var bottom = 0.0
        var top = 0.0
        blocks.forEach { block ->
            val style = block.style
            val column = (style.alignment - 1) % 3
            val row = (style.alignment - 1) / 3
            val position = block.text.position
            val (left, upper) = if (position != null) {
                val x = position.first * style.scaleX
                val y = position.second * style.scaleY
                x - column * 0.5 * block.width to y - (2 - row) * 0.5 * block.height
            } else {
                val available = size.width - style.marginL - style.marginR
                val x = when (column) {
                    0 -> style.marginL
                    1 -> style.marginL + (available - block.width) / 2
                    else -> size.width - style.marginR - block.width
                }
                val y = when (row) {
                    0 -> (size.height - style.marginV - block.height - bottom).also { bottom += block.height }
                    2 -> (style.marginV + top).also { top += block.height }
                    else -> (size.height - block.height) / 2.0
                }
                x to y
            }
            paint(canvas, block, left.toFloat(), upper.toFloat())
        }
    }

    private fun block(index: Int, size: FrameSize): Block {
        val cue = track.cues[index]
        val text = texts[index]
        val style = SubtitleStyles.resolve(track, cue, options, size.width, size.height).let { resolved ->
            text.alignment?.let { resolved.copy(alignment = it) } ?: resolved
        }
        val base = typefaces[style.font] ?: typefaces.getValue(OverlayOptions.FONT_FAMILIES.first())
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.fontSize.toFloat()
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val fill = spanned(text, style, base, colors = true)
        val maxWidth = max(1, (size.width - style.marginL - style.marginR).roundToInt())
        val desired = fill.split('\n').let { lines ->
            var offset = 0
            lines.maxOf { line ->
                val width = Layout.getDesiredWidth(fill, offset, offset + line.length, paint)
                offset += line.length + 1
                width
            }
        }
        val padding = if (style.strokeColor != null) style.strokeWidth / 2 else 0.0
        val width = minOf(maxWidth, ceil(desired + padding * 2).toInt().coerceAtLeast(1))
        val alignment = when ((style.alignment - 1) % 3) {
            0 -> Layout.Alignment.ALIGN_NORMAL
            1 -> Layout.Alignment.ALIGN_CENTER
            else -> Layout.Alignment.ALIGN_OPPOSITE
        }
        fun layout(content: CharSequence, textPaint: TextPaint) = StaticLayout.Builder.obtain(content, 0, content.length, textPaint, width)
            .setAlignment(alignment)
            .setIncludePad(false)
            .build()
        val layout = layout(fill, paint)
        val stroke = style.strokeColor?.let { color ->
            val strokePaint = TextPaint(paint).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = style.strokeWidth.toFloat()
                this.color = color
            }
            layout(spanned(text, style, base, colors = false), strokePaint)
        }
        return Block(style, layout, stroke, width, layout.height, text)
    }

    /** The runs as spans; the outline pass leaves the colors out so the paint color (outline) applies. zh-CN: 片段转为 span; 描边一遍不带颜色, 使用画笔颜色 (描边色). */
    private fun spanned(text: RichText, style: ResolvedSubtitleStyle, base: Typeface, colors: Boolean): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        text.runs.forEach { run ->
            val start = builder.length
            builder.append(run.text)
            val end = builder.length
            if (start == end) return@forEach
            builder.setSpan(FaceSpan(base, run.bold ?: style.bold, run.italic ?: style.italic), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            run.fontSize?.let { builder.setSpan(AbsoluteSizeSpan((it * style.scaleY).roundToInt().coerceAtLeast(1)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            if (run.underline == true) builder.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (colors) builder.setSpan(ForegroundColorSpan(run.color ?: style.color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
    }

    private fun paint(canvas: Canvas, block: Block, left: Float, top: Float) {
        val style = block.style
        val saved = canvas.save()
        canvas.translate(left, top)
        style.boxColor?.let { color ->
            val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            val pad = style.boxPadding.toFloat()
            for (line in 0 until block.layout.lineCount) {
                canvas.drawRect(
                    block.layout.getLineLeft(line) - pad,
                    block.layout.getLineTop(line) - pad,
                    block.layout.getLineRight(line) + pad,
                    block.layout.getLineBottom(line) + pad,
                    box,
                )
            }
        }
        val shadow = style.shadowColor
        val first = block.stroke ?: block.layout
        if (shadow != null) {
            first.paint.setShadowLayer(style.shadowRadius.toFloat().coerceAtLeast(MIN_SHADOW_RADIUS), style.shadowDx.toFloat(), style.shadowDy.toFloat(), shadow)
        }
        first.draw(canvas)
        first.paint.clearShadowLayer()
        if (block.stroke != null) block.layout.draw(canvas)
        canvas.restoreToCount(saved)
    }

    private fun loadTypeface(font: String): Typeface {
        if (!font.startsWith("/")) return Typeface.create(font, Typeface.NORMAL)
        return try {
            Typeface.createFromFile(File(font))
        } catch (e: RuntimeException) {
            throw MediaStudioException.unsupported("cannot load the font $font (${e.message ?: e.javaClass.simpleName})")
        }
    }

    /** Bold / italic of one run; fonts without the style get a synthetic one. zh-CN: 一个片段的粗体 / 斜体; 字体缺该样式时用合成样式. */
    private class FaceSpan(private val base: Typeface, private val bold: Boolean, private val italic: Boolean) : MetricAffectingSpan() {

        private val typeface: Typeface = Typeface.create(
            base,
            when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            },
        )

        override fun updateDrawState(paint: TextPaint) = apply(paint)

        override fun updateMeasureState(paint: TextPaint) = apply(paint)

        private fun apply(paint: TextPaint) {
            paint.typeface = typeface
            paint.isFakeBoldText = bold && !typeface.isBold
            paint.textSkewX = if (italic && !typeface.isItalic) SYNTHETIC_ITALIC_SKEW else 0f
        }
    }

    private companion object {
        const val SYNTHETIC_ITALIC_SKEW = -0.25f
        const val MIN_SHADOW_RADIUS = 0.01f
    }
}

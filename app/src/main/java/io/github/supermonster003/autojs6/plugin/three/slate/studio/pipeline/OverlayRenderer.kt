@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.OptIn
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameSize
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OverlayItem
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TextAlign
import java.io.Closeable
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Draws the overlays of `overlay` (roadmap P2.3, D29) into one transparent bitmap of the frame size, redrawn only when
 * the set of visible overlays changes, so media3 uploads a texture once per time segment. Fonts and pictures load in
 * the constructor, before the export starts, so a bad font or picture fails the task at once.
 * zh-CN: 把 `overlay` 的叠加物 (路线图 P2.3, D29) 画进一张与帧同尺寸的透明位图, 仅在可见叠加物的集合变化时重画, 因此 media3
 * 每个时间段只上传一次纹理. 字体与图片在构造时加载 (导出开始之前), 字体或图片有问题时任务立即失败.
 */
internal class OverlayRenderer(
    private val items: List<OverlayItem>,
    private val frame: FrameSize,
    /** Subtitles drawn above the overlays (roadmap P3.3). zh-CN: 画在叠加物之上的字幕 (路线图 P3.3). */
    private val subtitles: SubtitlePainter? = null,
) : Closeable {

    private class Picture(val bitmap: Bitmap, val width: Int, val height: Int)

    private val typefaces = HashMap<String, Typeface>()

    private val pictures = HashMap<String, Picture>()

    init {
        try {
            items.forEach { item ->
                when (item) {
                    is OverlayItem.Text -> typefaces.getOrPut(item.font) { loadTypeface(item.font) }
                    is OverlayItem.Image -> if (item.src !in pictures) pictures[item.src] = loadPicture(item)
                }
            }
        } catch (e: Throwable) {
            close()
            throw e
        }
    }

    /** A media3 overlay drawing [items]; one per export. zh-CN: 绘制 [items] 的 media3 叠加层; 每次导出一个. */
    fun overlay(): BitmapOverlay = object : BitmapOverlay() {

        private var size = frame

        /** The canvas target (premultiplied). zh-CN: 画布目标 (预乘). */
        private var bitmap: Bitmap? = null

        /** The same colors unpremultiplied, handed to media3. zh-CN: 同样的颜色 (非预乘), 交给 media3. */
        private var straight: Bitmap? = null

        private var pixels: IntArray? = null

        private var visible: Pair<List<Int>, List<Int>>? = null

        override fun configure(videoSize: Size) {
            super.configure(videoSize)
            if (videoSize.width != size.width || videoSize.height != size.height) {
                size = FrameSize(videoSize.width, videoSize.height)
                recycle()
                visible = null
            }
        }

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            val millis = presentationTimeUs / 1_000.0
            val now = items.indices.filter { items[it].placement.isActive(millis) } to subtitles?.visible(millis).orEmpty()
            val (width, height) = size.width to size.height
            val target = bitmap ?: createBitmap(width, height).also { bitmap = it }
            val upload = straight ?: createBitmap(width, height).apply { isPremultiplied = false }.also { straight = it }
            if (now != visible) {
                target.eraseColor(0)
                val canvas = Canvas(target)
                now.first.forEach { draw(canvas, items[it], size) }
                if (now.second.isNotEmpty()) subtitles?.draw(canvas, now.second, size)
                // The media3 overlay shader blends straight alpha (rgb * a + video * (1 - a)); the texture of a
                // premultiplied bitmap would apply the alpha twice (half transparent blue over black came out at 0x39
                // instead of 0x80 on XQ-AT72). getPixels answers unpremultiplied colors and setPixels keeps them.
                // zh-CN: media3 叠加着色器按非预乘 alpha 混合 (rgb * a + video * (1 - a)); 预乘位图的纹理会让 alpha 乘两次
                // (XQ-AT72 上黑底半透明蓝为 0x39 而非 0x80). getPixels 返回非预乘颜色, setPixels 原样保存.
                val buffer = pixels ?: IntArray(width * height).also { pixels = it }
                target.getPixels(buffer, 0, width, 0, 0, width, height)
                upload.setPixels(buffer, 0, width, 0, 0, width, height)
                visible = now
            }
            return upload
        }

        override fun release() {
            super.release()
            recycle()
        }

        private fun recycle() {
            bitmap?.recycle()
            bitmap = null
            straight?.recycle()
            straight = null
            pixels = null
        }
    }

    /** Draws [item] onto [canvas] of [size]; also used by tests. zh-CN: 把 [item] 画到 [size] 的 [canvas] 上; 测试也会用到. */
    fun draw(canvas: Canvas, item: OverlayItem, size: FrameSize) {
        val alpha = (item.placement.alpha * 255).roundToInt().coerceIn(0, 255)
        if (alpha == 0) return
        when (item) {
            is OverlayItem.Text -> drawText(canvas, item, size, alpha)
            is OverlayItem.Image -> drawImage(canvas, item, size, alpha)
        }
    }

    private fun drawText(canvas: Canvas, item: OverlayItem.Text, size: FrameSize, alpha: Int) {
        val base = typefaces.getValue(item.font)
        val style = when {
            item.bold && item.italic -> Typeface.BOLD_ITALIC
            item.bold -> Typeface.BOLD
            item.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val typeface = Typeface.create(base, style)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = item.fontSize.toFloat()
            // Fonts without the style get a synthetic one. zh-CN: 字体缺少该样式时使用合成样式.
            isFakeBoldText = item.bold && !typeface.isBold
            textSkewX = if (item.italic && !typeface.isItalic) SYNTHETIC_ITALIC_SKEW else 0f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val width = ceil(item.text.split('\n').maxOf { Layout.getDesiredWidth(it, paint) }.toDouble()).toInt().coerceAtLeast(1)
        val alignment = when (item.align) {
            TextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val layout = StaticLayout.Builder.obtain(item.text, 0, item.text.length, paint, width)
            .setAlignment(alignment)
            .setIncludePad(false)
            .build()
        val (left, top) = item.placement.topLeft(size.width, size.height, width.toDouble(), layout.height.toDouble())
        val saved = if (alpha < 255) canvas.saveLayerAlpha(0f, 0f, size.width.toFloat(), size.height.toFloat(), alpha) else canvas.save()
        canvas.translate(left.toFloat(), top.toFloat())
        val shadow = item.shadowColor
        val stroke = item.strokeColor
        if (stroke != null && item.strokeWidth > 0.0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = item.strokeWidth.toFloat()
            paint.color = stroke
            if (shadow != null) paint.setShadowLayer(shadowRadius(item), item.shadowDx.toFloat(), item.shadowDy.toFloat(), shadow)
            layout.draw(canvas)
            paint.clearShadowLayer()
            paint.style = Paint.Style.FILL
            paint.color = item.color
            layout.draw(canvas)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = item.color
            if (shadow != null) paint.setShadowLayer(shadowRadius(item), item.shadowDx.toFloat(), item.shadowDy.toFloat(), shadow)
            layout.draw(canvas)
        }
        canvas.restoreToCount(saved)
    }

    private fun drawImage(canvas: Canvas, item: OverlayItem.Image, size: FrameSize, alpha: Int) {
        val picture = pictures.getValue(item.src)
        val (width, height) = item.drawnSize(picture.width, picture.height, size.width, size.height)
        val (left, top) = item.placement.topLeft(size.width, size.height, width, height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
        canvas.drawBitmap(picture.bitmap, null, RectF(left.toFloat(), top.toFloat(), (left + width).toFloat(), (top + height).toFloat()), paint)
    }

    override fun close() {
        pictures.values.forEach { it.bitmap.recycle() }
        pictures.clear()
    }

    private fun loadTypeface(font: String): Typeface {
        if (!font.startsWith("/")) return Typeface.create(font, Typeface.NORMAL)
        return try {
            Typeface.createFromFile(File(font))
        } catch (e: RuntimeException) {
            throw MediaStudioException.unsupported("cannot load the font $font (${e.message ?: e.javaClass.simpleName})")
        }
    }

    /**
     * Decodes the picture of [item] with a sample size that keeps at least its drawn size in this frame.
     * zh-CN: 解码 [item] 的图片, 采样率保证解码尺寸不小于它在本帧中的绘制尺寸.
     */
    private fun loadPicture(item: OverlayItem.Image): Picture {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(item.src, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw MediaStudioException.unsupported("cannot decode the overlay image ${item.src}")
        }
        val (drawnWidth, drawnHeight) = items.filterIsInstance<OverlayItem.Image>().filter { it.src == item.src }
            .map { it.drawnSize(bounds.outWidth, bounds.outHeight, frame.width, frame.height) }
            .reduce { a, b -> maxOf(a.first, b.first) to maxOf(a.second, b.second) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= drawnWidth && bounds.outHeight / (sample * 2) >= drawnHeight && sample < MAX_SAMPLE_SIZE) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = try {
            BitmapFactory.decodeFile(item.src, options)
        } catch (e: OutOfMemoryError) {
            throw MediaStudioException.unsupported("the overlay image ${item.src} (${bounds.outWidth}x${bounds.outHeight}) is too large to decode")
        } ?: throw MediaStudioException.unsupported("cannot decode the overlay image ${item.src}")
        return Picture(bitmap, bounds.outWidth, bounds.outHeight)
    }

    private fun shadowRadius(item: OverlayItem.Text): Float =
        // A zero radius removes the shadow layer; keep a hard shadow. zh-CN: 半径为 0 会移除阴影层; 保留硬阴影.
        item.shadowRadius.toFloat().coerceAtLeast(MIN_SHADOW_RADIUS)

    private companion object {
        const val SYNTHETIC_ITALIC_SKEW = -0.25f
        const val MIN_SHADOW_RADIUS = 0.01f
        const val MAX_SAMPLE_SIZE = 64
    }

}

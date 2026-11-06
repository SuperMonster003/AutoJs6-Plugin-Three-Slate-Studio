package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import kotlin.math.abs

/**
 * [GifEncoder] of roadmap D32 (`toGif`, appendix D Q2 (a)), decoded back with the JDK GIF reader: exact palettes up to
 * 256 colors, a single color, frames large enough to fill the LZW code table many times, delays and the loop extension,
 * median cut and dithering.
 * zh-CN: 路线图 D32 (`toGif`, 附录 D Q2 (a)) 的 [GifEncoder], 用 JDK 的 GIF 读取器解码回来验证: 不超过 256 色的原色调色板,
 * 单色, 大到多次填满 LZW 码表的帧, 延时与循环扩展, 中位切分与抖动.
 */
class GifEncoderTest {

    @Test
    fun `256 colors survive exactly`() {
        val pixels = IntArray(16 * 16) { i -> (i shl 16) or ((255 - i) shl 8) or (i * 7 and 0xFF) }
        assertEquals(256, pixels.map { it and 0xFFFFFF }.toSet().size)
        val gif = encode(16, 16, loop = 0) { addFrame(pixels, 10) }
        val reader = reader(gif)
        assertEquals(1, reader.getNumImages(true))
        assertArrayEquals(pixels, rgb(reader, 0))
    }

    @Test
    fun `a single color frame uses the smallest table`() {
        val pixels = IntArray(7 * 5) { 0x336699 }
        val gif = encode(7, 5, loop = 0) { addFrame(pixels, 4) }
        assertEquals(1, GifPalette.bitsFor(1))
        val reader = reader(gif)
        assertEquals(7, reader.getWidth(0))
        assertEquals(5, reader.getHeight(0))
        assertArrayEquals(pixels, rgb(reader, 0))
        // Header 13, NETSCAPE 19, control 8, descriptor 10, table 6 (2 entries), data, trailer.
        // zh-CN: 文件头 13, NETSCAPE 19, 控制块 8, 描述符 10, 颜色表 6 (2 项), 数据, 结尾.
        assertEquals(0x80, gif[13 + 19 + 8 + 9].toInt() and 0xFF)
    }

    @Test
    fun `large frames reset the LZW table and still decode`() {
        // 256 random colors over 600 x 400 pixels emit far more than 4096 codes. zh-CN: 600 x 400 像素的 256 种随机颜色远超 4096 个码.
        val random = Random(7)
        val colors = IntArray(256) { random.nextInt(0x1000000) }
        val first = IntArray(600 * 400) { colors[random.nextInt(colors.size)] }
        val second = IntArray(600 * 400) { i -> if (i % 600 < 300) 0xFF0000 else 0x0000FF }
        val gif = encode(600, 400, loop = 0) {
            addFrame(first, 5)
            addFrame(second, GifEncoder.MAX_DELAY)
        }
        val reader = reader(gif)
        assertEquals(2, reader.getNumImages(true))
        assertArrayEquals(first.map { it and 0xFFFFFF }.toIntArray(), rgb(reader, 0))
        assertArrayEquals(second, rgb(reader, 1))
        assertEquals(listOf(5, GifEncoder.MAX_DELAY), (0..1).map { delay(reader, it) })
    }

    @Test
    fun `many frames keep their count and duration`() {
        val delays = GifEncoder.delays(90, 30)
        var encoder: GifEncoder? = null
        val gif = encode(8, 8, loop = 0) {
            encoder = this
            delays.forEachIndexed { index, delay -> addFrame(IntArray(64) { (index * 2) shl 8 }, delay) }
        }
        assertEquals(90, encoder!!.frameCount)
        assertEquals(300L, encoder!!.durationCentis)
        val reader = reader(gif)
        assertEquals(90, reader.getNumImages(true))
        assertEquals(300, (0 until 90).sumOf { delay(reader, it) })
        assertEquals(88 shl 8, rgb(reader, 44)[0] and 0xFFFFFF)
    }

    @Test
    fun `delays round on the running total`() {
        assertEquals(listOf(3, 4, 3, 3, 4, 3), GifEncoder.delays(6, 30).toList())
        assertEquals(100, GifEncoder.delays(30, 30).sum())
        assertEquals(List(10) { 10 }, GifEncoder.delays(10, 10).toList())
        assertEquals(List(5) { 2 }, GifEncoder.delays(5, 50).toList())
        assertEquals(List(3) { 100 }, GifEncoder.delays(3, 1).toList())
        assertEquals(1000, GifEncoder.delays(600, 60).sum())
        assertTrue(GifEncoder.delays(600, 60).all { it in 1..2 })
    }

    @Test
    fun `the loop extension follows the loop option`() {
        val frame = IntArray(4) { 0xFFFFFF }
        val forever = encode(2, 2, loop = 0) { addFrame(frame, 10) }
        val thrice = encode(2, 2, loop = 3) { addFrame(frame, 10) }
        val once = encode(2, 2, loop = null) { addFrame(frame, 10) }
        assertEquals(0, loopCount(forever))
        assertEquals(3, loopCount(thrice))
        assertEquals(null, loopCount(once))
        assertFalse(String(once, Charsets.ISO_8859_1).contains("NETSCAPE2.0"))
        assertEquals(1, reader(once).getNumImages(true))
    }

    @Test
    fun `median cut keeps 256 representative colors`() {
        // A smooth 64 x 64 gradient has 4096 colors. zh-CN: 64 x 64 的平滑渐变有 4096 种颜色.
        val pixels = IntArray(64 * 64) { i -> ((i % 64) * 4 shl 16) or ((i / 64) * 4 shl 8) or 0x80 }
        val palette = GifPalette.medianCut(pixels, 256)
        assertTrue(palette.size in 200..256)
        assertTrue(palette.all { (it and 0xFF) in 0x78..0x88 })
        val gif = encode(64, 64, loop = 0) { addFrame(pixels, 10) }
        val decoded = rgb(reader(gif), 0)
        assertTrue("mean error ${meanError(pixels, decoded)}", meanError(pixels, decoded) < 5.0)

        // Two clusters collapse to their averages. zh-CN: 两簇颜色归并为各自的平均值.
        val clusters = IntArray(100) { i -> if (i < 50) 0x100000 + (i % 2) * 0x080000 else 0x0000F0 + (i % 2) * 8 }
        assertEquals(setOf(0x140000, 0x0000F4), GifPalette.medianCut(clusters, 2).toSet())
    }

    @Test
    fun `dithering spreads the error without changing the size`() {
        // A smooth gradient over all three channels, 6144 colors. zh-CN: 三个通道都平滑渐变, 6144 种颜色.
        val pixels = IntArray(96 * 64) { i -> ((i % 96) * 255 / 95 shl 16) or ((i / 96) * 4 shl 8) or ((i % 96 + i / 96) * 255 / 158) }
        val plain = rgb(reader(encode(96, 64, loop = 0, dither = false) { addFrame(pixels, 10) }), 0)
        val dithered = rgb(reader(encode(96, 64, loop = 0, dither = true) { addFrame(pixels, 10) }), 0)
        assertNotEquals(plain.toList(), dithered.toList())
        assertTrue("plain error ${meanError(pixels, plain)}", meanError(pixels, plain) < 12.0)
        assertTrue("dithered error ${meanError(pixels, dithered)}", meanError(pixels, dithered) < 16.0)
        // Dithering keeps the average color of a region closer. zh-CN: 抖动使区域的平均颜色更接近原图.
        assertTrue(regionBias(pixels, dithered) <= regionBias(pixels, plain) + 0.5)
    }

    @Test
    fun `invalid frames are refused`() {
        val encoder = GifEncoder(ByteArrayOutputStream(), 4, 4, loop = 0, dither = false)
        assertThrows(IllegalArgumentException::class.java) { encoder.addFrame(IntArray(15), 10) }
        assertThrows(IllegalArgumentException::class.java) { encoder.addFrame(IntArray(16), GifEncoder.MAX_DELAY + 1) }
        assertThrows(IllegalArgumentException::class.java) { GifEncoder(ByteArrayOutputStream(), 0, 4, loop = 0, dither = false) }
        assertThrows(IllegalArgumentException::class.java) { GifEncoder(ByteArrayOutputStream(), 4, 4, loop = GifEncoder.MAX_LOOP + 1, dither = false) }
    }

    private fun encode(width: Int, height: Int, loop: Int?, dither: Boolean = false, frames: GifEncoder.() -> Unit): ByteArray {
        val out = ByteArrayOutputStream()
        GifEncoder(out, width, height, loop, dither).apply {
            frames()
            finish()
        }
        return out.toByteArray()
    }

    private fun reader(gif: ByteArray): ImageReader = ImageIO.getImageReadersByFormatName("gif").next().apply {
        input = ImageIO.createImageInputStream(ByteArrayInputStream(gif))
    }

    private fun rgb(reader: ImageReader, index: Int): IntArray {
        val image = reader.read(index)
        return image.getRGB(0, 0, image.width, image.height, null, 0, image.width).map { it and 0xFFFFFF }.toIntArray()
    }

    private fun delay(reader: ImageReader, index: Int): Int {
        val tree = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0") as Element
        val control = tree.getElementsByTagName("GraphicControlExtension").item(0) as Element
        return control.getAttribute("delayTime").toInt()
    }

    /** The NETSCAPE2.0 repeat count, or null without the extension. zh-CN: NETSCAPE2.0 的重复次数, 没有该扩展时为 null. */
    private fun loopCount(gif: ByteArray): Int? {
        val at = String(gif, Charsets.ISO_8859_1).indexOf("NETSCAPE2.0")
        if (at < 0) return null
        assertEquals(3, gif[at + 11].toInt())
        assertEquals(1, gif[at + 12].toInt())
        return (gif[at + 13].toInt() and 0xFF) or ((gif[at + 14].toInt() and 0xFF) shl 8)
    }

    private fun meanError(a: IntArray, b: IntArray): Double {
        var total = 0L
        for (i in a.indices) for (shift in intArrayOf(16, 8, 0)) total += abs(((a[i] shr shift) and 0xFF) - ((b[i] shr shift) and 0xFF))
        return total.toDouble() / (a.size * 3)
    }

    /** Mean absolute difference of 8 x 8 block averages. zh-CN: 8 x 8 块平均值的平均绝对差. */
    private fun regionBias(a: IntArray, b: IntArray): Double {
        var total = 0.0
        var blocks = 0
        for (by in 0 until 8) for (bx in 0 until 12) for (shift in intArrayOf(16, 8, 0)) {
            var sa = 0
            var sb = 0
            for (y in 0 until 8) for (x in 0 until 8) {
                val i = (by * 8 + y) * 96 + bx * 8 + x
                sa += (a[i] shr shift) and 0xFF
                sb += (b[i] shr shift) and 0xFF
            }
            total += abs(sa - sb) / 64.0
            blocks++
        }
        return total / blocks
    }

}

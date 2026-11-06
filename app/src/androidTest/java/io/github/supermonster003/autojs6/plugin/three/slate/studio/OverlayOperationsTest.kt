package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * `overlay` of roadmap P2.3 (D29) on the color bar fixture (320 x 240, eight bars of 40 pixels: white, yellow, cyan,
 * green, magenta, red, blue, black): pictures placed in pixels and percent with anchors, time windows and opacity are
 * checked on decoded frames, text is checked by the pixel difference against the source frame inside and outside its box.
 * zh-CN: 在彩条夹具 (320 x 240, 八条各 40 像素: 白, 黄, 青, 绿, 品红, 红, 蓝, 黑) 上验证路线图 P2.3 (D29) 的 `overlay`:
 * 以像素与百分比加锚点放置的图片, 时间段与不透明度在解码帧上检查, 文字按其区域内外相对源帧的像素差检查.
 */
@RunWith(AndroidJUnit4::class)
class OverlayOperationsTest : MediaStudioTestBase() {

    @Test
    fun pictureOverlaysFollowTheirPlacementTimeWindowAndOpacity() = withMediaStudio { plugin ->
        val red = picture("red.png", 64, 64, Color.RED)
        val blue = picture("blue.png", 32, 32, Color.BLUE)
        val result = run(
            plugin,
            request(
                MediaStudioOps.OVERLAY, listOf(fixtures.colorBarsMp4), output("overlay-pictures.mp4"),
                options(
                    "overlays" to listOf(
                        options("type" to "image", "src" to red.path, "x" to 0, "y" to 0, "anchor" to "top-left", "from" to 0, "to" to 2_000),
                        options("type" to "image", "src" to blue.path, "x" to "100%", "y" to "100%", "anchor" to "bottom-right", "alpha" to 0.5),
                        options("type" to "image", "src" to blue.path, "width" to "25%"),
                    ),
                ),
            ),
        )
        assertEquals(320, result.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(240, result.getInt(MediaStudioResultKeys.HEIGHT))
        assertDuration(3000, result, tolerance = 200)
        val video = File(result.getString(MediaStudioResultKeys.PATH)!!)
        frame(video, atMillis = 1_000).let { frame ->
            assertColor("red picture at the top left", 0xFF0000, frame.getPixel(32, 32))
            assertColor("white bar beside the red picture", 0xFFFFFF, frame.getPixel(32, 80))
            // A tight tolerance: alpha applied twice (premultiplied texture) gives about 0x40.
            // zh-CN: 容差收紧: alpha 被乘两次 (预乘纹理) 时约为 0x40.
            assertColor("half transparent blue over the black bar", 0x000080, frame.getPixel(308, 228), tolerance = 32)
            assertColor("black bar above the blue picture", 0x000000, frame.getPixel(308, 196))
            // 25% of 320 is 80 pixels, centered: x 120 to 200, y 80 to 160. zh-CN: 320 的 25% 为 80 像素, 居中: x 120 至 200, y 80 至 160.
            assertColor("scaled blue picture over the green bar", 0x0000FF, frame.getPixel(126, 120))
            assertColor("scaled blue picture over the magenta bar", 0x0000FF, frame.getPixel(194, 120))
            assertColor("cyan bar left of the scaled picture", 0x00FFFF, frame.getPixel(114, 120))
            assertColor("green bar above the scaled picture", 0x00FF00, frame.getPixel(126, 72))
        }
        frame(video, atMillis = 2_500).let { frame ->
            assertColor("the red picture ends at 2 s", 0xFFFFFF, frame.getPixel(32, 32))
            assertColor("the scaled picture stays", 0x0000FF, frame.getPixel(160, 120))
        }
    }

    @Test
    fun textOverlaysChangeTheirBoxOnly() = withMediaStudio { plugin ->
        val font = systemFont()
        val result = run(
            plugin,
            request(
                MediaStudioOps.OVERLAY, listOf(fixtures.colorBarsMp4), output("overlay-text.mp4"),
                options(
                    "overlays" to listOfNotNull(
                        options("type" to "text", "text" to "AutoJs6", "fontSize" to 48, "color" to "#FF0000", "stroke" to "black", "strokeWidth" to 3, "bold" to true),
                        font?.let {
                            options("type" to "text", "text" to "3-Slate", "font" to it.path, "fontSize" to 24, "x" to 8, "y" to "100%", "anchor" to "bottom-left", "shadow" to "#000000")
                        },
                    ),
                ),
            ),
        )
        val output = frame(File(result.getString(MediaStudioResultKeys.PATH)!!))
        val source = frame(fixtures.colorBarsMp4)
        // 48 px text of 7 characters is about 180 x 50 pixels around the center. zh-CN: 7 个字符的 48 像素文字约 180 x 50 像素, 居中.
        val inside = changedShare(source, output, 90, 230, 100, 140)
        assertTrue("only ${percent(inside)} of the text box changed", inside > 0.15)
        val above = changedShare(source, output, 0, 320, 0, 60)
        assertTrue("${percent(above)} changed above the text", above < 0.02)
        if (font != null) {
            val corner = changedShare(source, output, 8, 100, 212, 240)
            assertTrue("only ${percent(corner)} of the corner text box changed", corner > 0.10)
        }
    }

    @Test
    fun overlayRejectionsAndNotes() = withMediaStudio { plugin ->
        // INVALID_OPTIONS arrives as IllegalArgumentException, IO_ERROR as IllegalStateException.
        // zh-CN: INVALID_OPTIONS 以 IllegalArgumentException 到达, IO_ERROR 以 IllegalStateException 到达.
        fun rejection(options: Bundle): Pair<String, String> {
            val error = assertThrows(RuntimeException::class.java) {
                plugin.submit(request(MediaStudioOps.OVERLAY, listOf(fixtures.colorBarsMp4), output("overlay-rejected.mp4"), options), RecordingCallback())
            }
            return requireNotNull(MediaStudioErrorCodes.decode(error.message!!))
        }
        val missing = File(directory, "missing.png").path
        assertEquals(
            MediaStudioErrorCodes.IO_ERROR to "file not found: $missing",
            rejection(options("overlays" to listOf(options("type" to "image", "src" to missing)))),
        )
        assertEquals(
            MediaStudioErrorCodes.INVALID_OPTIONS to "options.overlays[0].text is required",
            rejection(options("overlays" to listOf(options("type" to "text")))),
        )

        val notPicture = File(directory, "not-a-picture.png").apply { writeText("text") }
        val callback = RecordingCallback()
        plugin.submit(
            request(MediaStudioOps.OVERLAY, listOf(fixtures.colorBarsMp4), output("overlay-bad-picture.mp4"), options("overlays" to listOf(options("type" to "image", "src" to notPicture.path)))),
            callback,
        )
        assertEquals(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, callback.awaitError().getString(MediaStudioContract.KEY_ERROR_CODE))
        assertNoPartFiles()

        val late = run(
            plugin,
            request(
                MediaStudioOps.OVERLAY, listOf(fixtures.colorBarsMp4), output("overlay-late.mp4"),
                options("overlays" to listOf(options("type" to "text", "text" to "late", "from" to 5_000))),
            ),
        )
        val warnings = late.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().toList()
        assertTrue(warnings.toString(), warnings.any { "overlay 0 starts at 5000 ms" in it && "it never shows" in it })
    }

    private fun picture(name: String, width: Int, height: Int, color: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        return File(directory, name).also { file ->
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }

    /** A copy of a readable system font, or null on a device without one. zh-CN: 可读系统字体的副本, 设备上没有时为 null. */
    private fun systemFont(): File? {
        val source = File("/system/fonts").listFiles().orEmpty().filter { it.name.endsWith(".ttf") && it.canRead() }.minByOrNull { it.length() } ?: return null
        return File(directory, "font.ttf").also { source.copyTo(it, overwrite = true) }
    }

    /** The share of pixels in the box whose color moved by more than 64 in a channel. zh-CN: 区域内某通道变化超过 64 的像素比例. */
    private fun changedShare(a: Bitmap, b: Bitmap, left: Int, right: Int, top: Int, bottom: Int): Double {
        var changed = 0
        for (y in top until bottom) for (x in left until right) {
            val p = a.getPixel(x, y)
            val q = b.getPixel(x, y)
            if (abs(Color.red(p) - Color.red(q)) > 64 || abs(Color.green(p) - Color.green(q)) > 64 || abs(Color.blue(p) - Color.blue(q)) > 64) changed++
        }
        return changed.toDouble() / ((right - left) * (bottom - top))
    }

    private fun percent(share: Double) = "%.1f%%".format(share * 100)

}

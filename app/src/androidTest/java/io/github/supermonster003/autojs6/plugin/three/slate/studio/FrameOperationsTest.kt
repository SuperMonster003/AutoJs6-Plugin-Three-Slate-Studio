package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.AnimatedImageDrawable
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * `frames`, `frame`, `thumbnail` and `toGif` of roadmap P2.4 (D32) on the color bar fixture (320 x 240, 3 s, eight bars
 * of 40 pixels: white, yellow, cyan, green, magenta, red, blue, black). Images are decoded by BitmapFactory; GIFs are
 * parsed block by block (frame count, delays, loop) and decoded by the platform (Movie for the duration, ImageDecoder
 * for the animation, BitmapFactory for the first frame). MediaMetadataRetriever reads video and audio only, so it has no
 * GIF check.
 * zh-CN: 在彩条夹具 (320 x 240, 3 秒, 八条各 40 像素: 白, 黄, 青, 绿, 品红, 红, 蓝, 黑) 上验证路线图 P2.4 (D32) 的 `frames`,
 * `frame`, `thumbnail` 与 `toGif`. 图片由 BitmapFactory 解码; GIF 逐块解析 (帧数, 延时, 循环), 再由平台解码 (Movie 取时长,
 * ImageDecoder 取动画, BitmapFactory 取首帧). MediaMetadataRetriever 只读视频与音频, 因此不做 GIF 检查.
 */
@RunWith(AndroidJUnit4::class)
class FrameOperationsTest : MediaStudioTestBase() {

    @Test
    fun framesWritesNumberedImagesIntoAFolder() = withMediaStudio { plugin ->
        val folder = output("frames-png")
        val png = run(plugin, request(MediaStudioOps.FRAMES, listOf(fixtures.colorBarsMp4), folder, options("every" to 1_000)))
        assertEquals(folder.path, png.getString(MediaStudioResultKeys.PATH))
        val paths = png.getStringArray(MediaStudioResultKeys.PATHS)!!.map(::File)
        assertEquals(listOf("color-bars-3s-0001.png", "color-bars-3s-0002.png", "color-bars-3s-0003.png"), paths.map { it.name })
        assertEquals("image/png", png.getString(MediaStudioResultKeys.MIME))
        assertEquals(320, png.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(240, png.getInt(MediaStudioResultKeys.HEIGHT))
        assertEquals(paths.sumOf { it.length() }, png.getLong(MediaStudioResultKeys.SIZE_BYTES))
        assertFalse(png.containsKey(MediaStudioResultKeys.CODEC))
        assertEquals(paths.toSet(), folder.listFiles()!!.toSet())
        paths.forEach { file ->
            val image = BitmapFactory.decodeFile(file.path)
            assertEquals(320, image.width)
            assertEquals(240, image.height)
            assertColor("white bar of ${file.name}", 0xFFFFFF, image.getPixel(20, 120))
            assertColor("cyan bar of ${file.name}", 0x00FFFF, image.getPixel(100, 120))
            assertColor("black bar of ${file.name}", 0x000000, image.getPixel(300, 120))
        }

        // An existing folder is used as it is; its frame files must not exist yet. zh-CN: 已有文件夹按原样使用; 帧文件不能已存在.
        val jpg = run(
            plugin,
            request(MediaStudioOps.FRAMES, listOf(fixtures.colorBarsMp4), File(folder.path + "/"), options("count" to 4, "format" to "jpg", "quality" to 80, "width" to 160)),
        )
        val jpgs = jpg.getStringArray(MediaStudioResultKeys.PATHS)!!.map(::File)
        assertEquals(4, jpgs.size)
        assertEquals("image/jpeg", jpg.getString(MediaStudioResultKeys.MIME))
        jpgs.forEach { file ->
            assertTrue(file.name.endsWith(".jpg"))
            val image = BitmapFactory.decodeFile(file.path)
            assertEquals(160, image.width)
            assertEquals(120, image.height)
            assertColor("white bar of ${file.name}", 0xFFFFFF, image.getPixel(10, 60))
            assertColor("black bar of ${file.name}", 0x000000, image.getPixel(150, 60))
        }
        val again = RecordingCallback()
        plugin.submit(request(MediaStudioOps.FRAMES, listOf(fixtures.colorBarsMp4), folder, options("every" to 1_000)), again)
        val error = again.awaitError()
        assertEquals(MediaStudioErrorCodes.IO_ERROR, error.getString(MediaStudioContract.KEY_ERROR_CODE))
        assertEquals(7, folder.listFiles()!!.size)
    }

    @Test
    fun framesTakesTimesAndKeepsTheirOrder() = withMediaStudio { plugin ->
        val result = run(
            plugin,
            request(MediaStudioOps.FRAMES, listOf(fixtures.colorBarsMp4), output("frames-times"), options("times" to doubleArrayOf(2_500.0, 0.0, 3_000.0))),
        )
        val paths = result.getStringArray(MediaStudioResultKeys.PATHS)!!.map(::File)
        assertEquals(3, paths.size)
        paths.forEach { assertColor("white bar of ${it.name}", 0xFFFFFF, BitmapFactory.decodeFile(it.path).getPixel(20, 120)) }

        val late = RecordingCallback()
        plugin.submit(request(MediaStudioOps.FRAMES, listOf(fixtures.colorBarsMp4), output("frames-late"), options("at" to 5_000)), late)
        val error = late.awaitError()
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, error.getString(MediaStudioContract.KEY_ERROR_CODE))
        assertEquals("options.at (5000 ms) is after the end of the input (3000 ms)", error.getString(MediaStudioContract.KEY_ERROR_MESSAGE))

        val many = RecordingCallback()
        plugin.submit(request(MediaStudioOps.FRAMES, listOf(fixtures.colorBarsMp4), output("frames-many"), options("every" to 1)), many)
        assertEquals(MediaStudioErrorCodes.LIMIT_EXCEEDED, many.awaitError().getString(MediaStudioContract.KEY_ERROR_CODE))
        assertFalse("a rejected task leaves no folder it created", output("frames-many").exists())
    }

    @Test
    fun frameAndThumbnailWriteOneImage() = withMediaStudio { plugin ->
        val png = run(plugin, request(MediaStudioOps.FRAME, listOf(fixtures.colorBarsMp4), output(".frame-test.png"), options("at" to 1_500.5)))
        assertEquals("image/png", png.getString(MediaStudioResultKeys.MIME))
        assertFalse(png.containsKey(MediaStudioResultKeys.DURATION_MILLIS))
        BitmapFactory.decodeFile(png.getString(MediaStudioResultKeys.PATH)).let { image ->
            assertEquals(320, image.width)
            assertEquals(240, image.height)
            assertColor("red bar", 0xFF0000, image.getPixel(220, 120))
        }
        val jpeg = run(plugin, request(MediaStudioOps.FRAME, listOf(fixtures.colorBarsMp4), output("frame.JPEG"), options("at" to 0, "width" to 64)))
        assertEquals("image/jpeg", jpeg.getString(MediaStudioResultKeys.MIME))
        assertEquals(64, jpeg.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(48, jpeg.getInt(MediaStudioResultKeys.HEIGHT))

        val thumbnail = run(plugin, request(MediaStudioOps.THUMBNAIL, listOf(fixtures.colorBarsMp4), output("thumbnail.jpg")))
        assertEquals("image/jpeg", thumbnail.getString(MediaStudioResultKeys.MIME))
        BitmapFactory.decodeFile(thumbnail.getString(MediaStudioResultKeys.PATH)).let { image ->
            assertEquals(320, image.width)
            assertEquals(240, image.height)
        }

        val audioOnly = RecordingCallback()
        plugin.submit(request(MediaStudioOps.FRAME, listOf(fixtures.sineM4a), output("no-video.png"), options("at" to 0)), audioOnly)
        val error = audioOnly.awaitError()
        assertEquals(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, error.getString(MediaStudioContract.KEY_ERROR_CODE))
        assertFalse(output("no-video.png").exists())
        assertNoPartFiles()
    }

    @Test
    fun framesOfARotatedVideoAreUpright() = withMediaStudio { plugin ->
        val rotated = rotate(fixtures.colorBarsMp4, output("rotated-90.mp4"), 90)
        val result = run(plugin, request(MediaStudioOps.FRAME, listOf(rotated), output("rotated.png"), options("at" to 1_000)))
        assertEquals(240, result.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(320, result.getInt(MediaStudioResultKeys.HEIGHT))
        // Turned clockwise, the white bar on the left becomes the top rows. zh-CN: 顺时针转后, 左侧白条成为顶部各行.
        BitmapFactory.decodeFile(result.getString(MediaStudioResultKeys.PATH)).let { image ->
            assertEquals(240, image.width)
            assertEquals(320, image.height)
            assertColor("white bar on top", 0xFFFFFF, image.getPixel(120, 20))
            assertColor("black bar at the bottom", 0x000000, image.getPixel(120, 300))
        }
    }

    @Test
    fun toGifWritesTheFramesDelaysAndLoopOfTheRange() = withMediaStudio { plugin ->
        val result = run(
            plugin,
            request(MediaStudioOps.TO_GIF, listOf(fixtures.colorBarsMp4), output("bars.gif"), options("start" to 500, "end" to 2_500, "fps" to 10, "width" to 160)),
        )
        val gif = File(result.getString(MediaStudioResultKeys.PATH)!!)
        assertEquals("image/gif", result.getString(MediaStudioResultKeys.MIME))
        assertEquals(160, result.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(120, result.getInt(MediaStudioResultKeys.HEIGHT))
        assertEquals(2_000L, result.getLong(MediaStudioResultKeys.DURATION_MILLIS))
        assertEquals(gif.length(), result.getLong(MediaStudioResultKeys.SIZE_BYTES))
        assertFalse(result.containsKey(MediaStudioResultKeys.CODEC))
        parseGif(gif).let { info ->
            assertEquals(160, info.width)
            assertEquals(120, info.height)
            assertEquals(List(20) { 10 }, info.delays)
            assertEquals(0, info.loop)
        }
        assertPlatformDecodes(gif, 160, 120, 2_000)

        // 30 fps has no whole centisecond delay; the delays alternate so the total stays exact.
        // zh-CN: 30 fps 没有整厘秒延时; 延时交替使总时长保持准确.
        val smooth = run(
            plugin,
            request(MediaStudioOps.TO_GIF, listOf(fixtures.colorBarsMp4), output("bars-30fps.gif"), options("fps" to 30, "loop" to false, "dither" to true)),
        )
        assertEquals(240, smooth.getInt(MediaStudioResultKeys.HEIGHT))
        assertEquals(320, smooth.getInt(MediaStudioResultKeys.WIDTH))
        parseGif(File(smooth.getString(MediaStudioResultKeys.PATH)!!)).let { info ->
            assertEquals(90, info.delays.size)
            assertEquals(300, info.delays.sum())
            assertTrue(info.delays.all { it == 3 || it == 4 })
            assertNull("loop false writes no NETSCAPE block", info.loop)
        }
        assertEquals(3_000L, smooth.getLong(MediaStudioResultKeys.DURATION_MILLIS))
        assertPlatformDecodes(File(smooth.getString(MediaStudioResultKeys.PATH)!!), 320, 240, 3_000)
    }

    @Test
    fun toGifLimitsAreCheckedAtSubmitWhenTheRangeIsKnown() = withMediaStudio { plugin ->
        val tooLong = request(MediaStudioOps.TO_GIF, listOf(fixtures.colorBarsMp4), output("too-long.gif"), options("end" to 60_001))
        val error = assertThrows(IllegalArgumentException::class.java) { plugin.submit(tooLong, RecordingCallback()) }
        assertEquals(MediaStudioErrorCodes.LIMIT_EXCEEDED, MediaStudioErrorCodes.decode(error.message!!)?.first)
        val wrongExtension = request(MediaStudioOps.TO_GIF, listOf(fixtures.colorBarsMp4), output("bars.png"))
        val format = assertThrows(IllegalArgumentException::class.java) { plugin.submit(wrongExtension, RecordingCallback()) }
        assertEquals(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, MediaStudioErrorCodes.decode(format.message!!)?.first)
    }

    @Test
    fun cancelledFramesRemoveTheirFilesAndFolder() = withMediaStudio { plugin ->
        val folder = output("frames-cancelled")
        val callback = RecordingCallback()
        val taskId = plugin.submit(request(MediaStudioOps.FRAMES, listOf(fixtures.colorBarsMp4), folder, options("every" to 3)), callback)
        waitUntil("the first frame") { folder.listFiles()?.any { !it.name.startsWith(".") } == true }
        assertTrue("the task finished before it was cancelled", plugin.cancel(taskId))
        assertEquals(MediaStudioErrorCodes.TASK_CANCELLED, callback.awaitError().getString(MediaStudioContract.KEY_ERROR_CODE))
        waitUntil("the folder to be removed") { !folder.exists() }
    }

    // region Helpers

    private class GifInfo(val width: Int, val height: Int, val delays: List<Int>, val loop: Int?)

    /** Walks the blocks of a GIF89a file. zh-CN: 逐块遍历 GIF89a 文件. */
    private fun parseGif(file: File): GifInfo {
        val bytes = file.readBytes()
        assertEquals("GIF89a", String(bytes, 0, 6, Charsets.US_ASCII))
        fun u8(at: Int) = bytes[at].toInt() and 0xFF
        fun u16(at: Int) = u8(at) or (u8(at + 1) shl 8)
        fun skipBlocks(from: Int): Int {
            var at = from
            while (true) {
                val size = u8(at++)
                if (size == 0) return at
                at += size
            }
        }

        var at = 13
        if (u8(10) and 0x80 != 0) at += 3 * (1 shl ((u8(10) and 7) + 1))
        val delays = mutableListOf<Int>()
        var loop: Int? = null
        var delay = 0
        while (true) {
            when (u8(at)) {
                0x21 -> {
                    val label = u8(at + 1)
                    at += 2
                    if (label == 0xF9) delay = u16(at + 2)
                    if (label == 0xFF && String(bytes, at + 1, 11, Charsets.US_ASCII) == "NETSCAPE2.0") loop = u16(at + 14)
                    at = skipBlocks(at)
                }
                0x2C -> {
                    delays += delay
                    val packed = u8(at + 9)
                    at += 10
                    if (packed and 0x80 != 0) at += 3 * (1 shl ((packed and 7) + 1))
                    at = skipBlocks(at + 1)
                }
                0x3B -> return GifInfo(u16(6), u16(8), delays, loop)
                else -> fail("unexpected block 0x${Integer.toHexString(u8(at))} at $at of ${file.name}")
            }
        }
    }

    private fun assertPlatformDecodes(gif: File, width: Int, height: Int, durationMillis: Int) {
        @Suppress("DEPRECATION")
        val movie = requireNotNull(Movie.decodeFile(gif.path)) { "Movie cannot decode ${gif.name}" }
        assertEquals(width, movie.width())
        assertEquals(height, movie.height())
        assertEquals(durationMillis, movie.duration())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertTrue(ImageDecoder.decodeDrawable(ImageDecoder.createSource(gif)) is AnimatedImageDrawable)
        }
        BitmapFactory.decodeFile(gif.path).let { first ->
            assertEquals(width, first.width)
            assertColor("white bar of the first GIF frame", 0xFFFFFF, first.getPixel(width / 16, height / 2))
            assertColor("black bar of the first GIF frame", 0x000000, first.getPixel(width - width / 16, height / 2))
        }
    }

    /** The video track of [source] with an orientation hint, not re-encoded. zh-CN: 带方向提示的 [source] 视频轨, 不重新编码. */
    private fun rotate(source: File, output: File, degrees: Int): File {
        val extractor = MediaExtractor().apply { setDataSource(source.path) }
        val muxer = MediaMuxer(output.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val track = (0 until extractor.trackCount).first { extractor.getTrackFormat(it).getString("mime")!!.startsWith("video/") }
            extractor.selectTrack(track)
            val target = muxer.addTrack(extractor.getTrackFormat(track))
            muxer.setOrientationHint(degrees)
            muxer.start()
            val buffer = ByteBuffer.allocate(4 shl 20)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                info.set(0, size, extractor.sampleTime, flags)
                muxer.writeSampleData(target, buffer, info)
                extractor.advance()
            }
            muxer.stop()
        } finally {
            muxer.release()
            extractor.release()
        }
        return output
    }

    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            if (System.nanoTime() > deadline) fail("timed out waiting for $what")
            Thread.sleep(20)
        }
    }

    // endregion

}

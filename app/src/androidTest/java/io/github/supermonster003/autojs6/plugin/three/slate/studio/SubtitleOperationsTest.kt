package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * The subtitle operations of roadmap P3.3 (D31) on the color bar fixture (320 x 240, 3 s): burned SRT and ASS cues are
 * checked by the pixel difference against the source frame in horizontal bands (time windows, bottom stacking of
 * overlapping cues, `\an8` / `\pos`, the opaque box of ASS `BorderStyle` 3), the timeline `subtitles`, and
 * `subtitlesParse` / `subtitlesWrite` through Binder.
 * zh-CN: 在彩条夹具 (320 x 240, 3 秒) 上验证路线图 P3.3 (D31) 的字幕操作: 烧录的 SRT 与 ASS cue 按水平带内相对源帧的像素差检查
 * (时间段, 重叠 cue 在底部的堆叠, `\an8` / `\pos`, ASS `BorderStyle` 3 的不透明底框), 时间线的 `subtitles`, 以及经 Binder 的
 * `subtitlesParse` / `subtitlesWrite`.
 */
@RunWith(AndroidJUnit4::class)
class SubtitleOperationsTest : MediaStudioTestBase() {

    private fun file(name: String, bytes: ByteArray): File = File(directory, name).also { it.writeBytes(bytes) }

    private fun bom(text: String) = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray()

    @Test
    fun burnedSrtCuesFollowTheirTimesAndStack() = withMediaStudio { plugin ->
        // BOM, CRLF, an extra empty line, overlapping cues; the third goes to the top with {\an8}.
        // zh-CN: BOM, CRLF, 多余的空行, 重叠的 cue; 第三条用 {\an8} 放到顶部.
        val srt = file(
            "burn.srt",
            bom("1\r\n00:00:00,000 --> 00:00:02,000\r\nHELLO\r\n\r\n\r\n2\r\n00:00:01,000 --> 00:00:02,000\r\nWORLD\r\n\r\n3\r\n00:00:02,200 --> 00:00:02,900\r\n{\\an8}TOP\r\n"),
        )
        val result = run(
            plugin,
            request(
                MediaStudioOps.BURN_SUBTITLES, listOf(fixtures.colorBarsMp4), output("burn-srt.mp4"),
                options("subtitles" to srt.path, "style" to options("fontSize" to 32, "color" to "#FF0000")),
            ),
        )
        assertEquals(320 to 240, result.getInt(MediaStudioResultKeys.WIDTH) to result.getInt(MediaStudioResultKeys.HEIGHT))
        assertDuration(3000, result, tolerance = 200)
        assertTrue(result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().toList().toString(), result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().none { "ignored" in it })
        val video = File(result.getString(MediaStudioResultKeys.PATH)!!)
        val source = frame(fixtures.colorBarsMp4, atMillis = 500)
        // 32 px lines are about 38 px high; the bottom margin is 5 % (12 px). zh-CN: 32 像素的行高约 38 像素; 底边距 5 % (12 像素).
        frame(video, atMillis = 500).let { frame ->
            assertTrue("bottom line", changedShare(source, frame, 80, 240, 192, 226) > 0.10)
            assertTrue("nothing above the bottom line", changedShare(source, frame, 0, 320, 0, 180) < 0.01)
        }
        frame(video, atMillis = 1_500).let { frame ->
            assertTrue("the earlier cue stays at the bottom", changedShare(source, frame, 80, 240, 192, 226) > 0.10)
            assertTrue("the later cue stacks above it", changedShare(source, frame, 80, 240, 154, 188) > 0.10)
            assertTrue("nothing at the top", changedShare(source, frame, 0, 320, 0, 120) < 0.01)
        }
        frame(video, atMillis = 2_500).let { frame ->
            assertTrue("the {\\an8} cue is at the top", changedShare(source, frame, 100, 220, 12, 48) > 0.10)
            assertTrue("the bottom cues ended", changedShare(source, frame, 0, 320, 130, 240) < 0.01)
        }
    }

    @Test
    fun burnedAssUsesItsStylesTagsAndBox() = withMediaStudio { plugin ->
        val ass = file(
            "burn.ass",
            """
            [Script Info]
            PlayResX: 320
            PlayResY: 240

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Box,sans-serif,30,&H00FFFFFF,&H000000FF,&H00FF0000,&H00000000,-1,0,0,0,100,100,0,0,3,4,0,5,10,10,10,1

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:00.00,0:00:02.00,Box,,0,0,0,,{\k20}BOX{\k20}ED
            Dialogue: 0,0:00:00.00,0:00:02.00,Box,,0,0,0,,{\an7\pos(8,8)\c&H00FF00&}G
            """.trimIndent().toByteArray(),
        )
        val result = run(plugin, request(MediaStudioOps.BURN_SUBTITLES, listOf(fixtures.colorBarsMp4), output("burn-ass.mp4"), options("subtitles" to ass.path)))
        val warnings = result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().toList()
        assertTrue(warnings.toString(), warnings.any { "\\k (2)" in it })
        val frame = frame(File(result.getString(MediaStudioResultKeys.PATH)!!), atMillis = 1_000)
        // The middle cue sits on a blue box (OutlineColour of BorderStyle 3). zh-CN: 中部 cue 下有蓝色底框 (BorderStyle 3 的 OutlineColour).
        assertTrue("blue box in the middle", share(frame, 120, 200, 104, 136) { isBlue(it) } > 0.3)
        assertTrue("white text on the box", share(frame, 120, 200, 104, 136) { isWhite(it) } > 0.05)
        // \pos(8,8) with \an7 places the green G at the top left. zh-CN: \pos(8,8) 加 \an7 把绿色的 G 放在左上角.
        assertTrue("green G at the top left", share(frame, 8, 40, 8, 44) { isGreen(it) } > 0.05)
        assertFalse("the white bar keeps its color away from the cues", share(frame, 8, 40, 180, 230) { !isWhite(it) } > 0.05)
    }

    @Test
    fun cueArraysAndTimelineSubtitles() = withMediaStudio { plugin ->
        val cues = listOf(options("start" to 0L, "end" to 3_000L, "text" to "<b>CUE</b>"))
        val burned = run(
            plugin,
            request(MediaStudioOps.BURN_SUBTITLES, listOf(fixtures.colorBarsMp4), output("burn-cues.mp4"), options("cues" to cues, "style" to options("fontSize" to 40, "align" to "top", "background" to "#000000"))),
        )
        val source = frame(fixtures.colorBarsMp4, atMillis = 1_000)
        frame(File(burned.getString(MediaStudioResultKeys.PATH)!!), atMillis = 1_000).let { frame ->
            assertTrue("top cue", changedShare(source, frame, 100, 220, 14, 56) > 0.2)
            assertTrue("nothing at the bottom", changedShare(source, frame, 0, 320, 120, 240) < 0.01)
        }
        val timeline = options(
            "video" to listOf(options("clips" to listOf(options("src" to fixtures.colorBarsMp4.path)))),
            "subtitles" to options("cues" to cues, "style" to options("fontSize" to 40)),
        )
        val composed = run(
            plugin,
            Bundle().apply {
                putString(MediaStudioRequestKeys.OP, MediaStudioOps.COMPOSE)
                putStringArray(MediaStudioRequestKeys.INPUTS, emptyArray())
                putString(MediaStudioRequestKeys.OUTPUT, output("compose-subtitles.mp4").path)
                putBundle(MediaStudioRequestKeys.TIMELINE, timeline)
                putString(MediaStudioRequestKeys.HOST_TOKEN, HOST_TOKEN)
            },
        )
        frame(File(composed.getString(MediaStudioResultKeys.PATH)!!), atMillis = 1_000).let { frame ->
            assertTrue("timeline cue at the bottom", changedShare(source, frame, 100, 220, 170, 228) > 0.10)
            assertTrue("nothing at the top", changedShare(source, frame, 0, 320, 0, 120) < 0.01)
        }
    }

    @Test
    fun writeAndParseThroughBinder() = withMediaStudio { plugin ->
        val cues = listOf(
            options("start" to 0L, "end" to 1_500L, "text" to "One"),
            options("start" to 1_000L, "end" to 2_000L, "text" to "<i>Two</i>\nlines"),
        )
        val srt = output("written.srt")
        val written = run(plugin, Bundle().apply {
            putString(MediaStudioRequestKeys.OP, MediaStudioOps.SUBTITLES_WRITE)
            putStringArray(MediaStudioRequestKeys.INPUTS, emptyArray())
            putString(MediaStudioRequestKeys.OUTPUT, srt.path)
            putBundle(MediaStudioRequestKeys.OPTIONS, options("cues" to cues))
            putString(MediaStudioRequestKeys.HOST_TOKEN, HOST_TOKEN)
            putBoolean(MediaStudioRequestKeys.REGISTER_MEDIA, false)
        })
        assertEquals("application/x-subrip", written.getString(MediaStudioResultKeys.MIME))
        assertTrue(srt.readText().startsWith("1\r\n00:00:00,000 --> 00:00:01,500\r\nOne\r\n\r\n2\r\n"))
        fun parse(path: String) = Bundle().apply {
            putString(MediaStudioRequestKeys.OP, MediaStudioOps.SUBTITLES_PARSE)
            putStringArray(MediaStudioRequestKeys.INPUTS, arrayOf(path))
            putString(MediaStudioRequestKeys.HOST_TOKEN, HOST_TOKEN)
            putBoolean(MediaStudioRequestKeys.REGISTER_MEDIA, false)
        }
        val parsed = run(plugin, parse(srt.path))
        @Suppress("DEPRECATION")
        val read = parsed.getParcelableArrayList<Bundle>(MediaStudioResultKeys.CUES).orEmpty()
        assertEquals(listOf(0L to 1_500L, 1_000L to 2_000L), read.map { it.getLong("start") to it.getLong("end") })
        assertEquals(listOf("One", "Two\nlines"), read.map { it.getString("text") })
        assertTrue("parse lists no task output", listTasks(plugin).none { it.getString("output") == "" })

        val error = assertThrows(RuntimeException::class.java) {
            plugin.submit(
                Bundle().apply {
                    putString(MediaStudioRequestKeys.OP, MediaStudioOps.SUBTITLES_WRITE)
                    putString(MediaStudioRequestKeys.OUTPUT, output("written.vtt").path)
                    putBundle(MediaStudioRequestKeys.OPTIONS, options("cues" to cues))
                    putString(MediaStudioRequestKeys.HOST_TOKEN, HOST_TOKEN)
                },
                RecordingCallback(),
            )
        }
        assertEquals(
            MediaStudioErrorCodes.UNSUPPORTED_FORMAT to "subtitle output must end with .srt or .ass, got written.vtt",
            MediaStudioErrorCodes.decode(error.message!!),
        )
        val broken = file("broken.srt", "not subtitles".toByteArray())
        val callback = RecordingCallback()
        plugin.submit(parse(broken.path), callback)
        assertEquals(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, callback.awaitError().getString(MediaStudioContract.KEY_ERROR_CODE))
    }

    private fun isBlue(color: Int) = Color.blue(color) > 160 && Color.red(color) < 90 && Color.green(color) < 90

    private fun isGreen(color: Int) = Color.green(color) > 160 && Color.red(color) < 90 && Color.blue(color) < 90

    private fun isWhite(color: Int) = Color.red(color) > 190 && Color.green(color) > 190 && Color.blue(color) > 190

    private fun share(frame: Bitmap, left: Int, right: Int, top: Int, bottom: Int, test: (Int) -> Boolean): Double {
        var hits = 0
        for (y in top until bottom) for (x in left until right) if (test(frame.getPixel(x, y))) hits++
        return hits.toDouble() / ((right - left) * (bottom - top))
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
}

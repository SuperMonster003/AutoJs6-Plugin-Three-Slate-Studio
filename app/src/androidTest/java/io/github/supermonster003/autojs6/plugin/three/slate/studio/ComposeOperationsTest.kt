package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures.MediaFixtures
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.ComposeOperation
import org.autojs.plugin.mediastudio.api.IMediaStudioPlugin
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * `compose` of roadmap P3.1 (D33) and the transitions of P3.2 (D30). P3.1: two video clips, one audio clip and a text
 * overlay come out with the right duration, tracks, frames and overlay position; a second track sits above the first
 * and the background shows around a letterboxed clip; timeline errors name their field path. P3.2: every transition
 * joins a red and a blue clip into a 2 s sample whose middle frame holds both sources (or black for `fade`); a GPU that
 * fails on the shaders falls back to `dissolve` with a warning.
 * zh-CN: 路线图 P3.1 (D33) 的 `compose` 与 P3.2 (D30) 的转场. P3.1: 两段视频, 一段音频与一个文字叠加的导出在时长, 轨道, 画面与叠加
 * 位置上正确; 第二条轨道位于第一条之上, 加黑边的片段四周显示背景色; 时间线错误给出字段路径. P3.2: 每种转场把一段红色与一段蓝色
 * 连接为 2 秒样本, 其中间帧同时含两个源 (`fade` 为黑); 着色器在 GPU 上失败时回退为 `dissolve` 并给出警告.
 */
@RunWith(AndroidJUnit4::class)
class ComposeOperationsTest : MediaStudioTestBase() {

    @After
    fun restoreShaders() {
        ComposeOperation.failShadersForTest = false
    }

    @Test
    fun twoClipsAnAudioClipAndATextOverlay() = withMediaStudio { plugin ->
        val blue = solid("blue-3s.mp4", BLUE, 3_000)
        val timeline = options(
            "frameRate" to 30,
            "video" to listOf(
                options(
                    "clips" to listOf(
                        options("src" to fixtures.colorBarsMp4.path, "end" to 2_000),
                        options("src" to blue.path, "start" to 500, "end" to 2_500),
                    ),
                ),
            ),
            "audio" to listOf(options("clips" to listOf(options("src" to fixtures.sineM4a.path, "at" to 500)))),
            "overlays" to listOf(
                options("type" to "text", "text" to "Slate", "fontSize" to 40, "color" to "#FFFFFF", "stroke" to "#000000", "x" to "50%", "y" to "20%", "from" to 1_000, "to" to 3_000),
            ),
        )
        val result = run(plugin, compose(timeline, output("compose-basic.mp4")))
        assertEquals(320, result.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(240, result.getInt(MediaStudioResultKeys.HEIGHT))
        assertDuration(4_000, result, tolerance = 100)
        val video = File(result.getString(MediaStudioResultKeys.PATH)!!)
        assertEquals("one video and one audio track", listOf("audio", "video"), trackKinds(video))
        frame(video, 500).let { frame ->
            assertColor("first clip: white bar", 0xFFFFFF, frame.getPixel(20, 200))
            assertColor("first clip: blue bar", 0x0000FF, frame.getPixel(260, 200))
        }
        frame(video, 3_000).let { frame -> assertColor("second clip", BLUE, frame.getPixel(160, 200)) }
        // The overlay box: y 20 % of 240 = 48, 40 px text centered there. zh-CN: 叠加框: y 为 240 的 20 % = 48, 40 像素文字以此居中.
        val withText = frame(video, 1_500)
        val withoutText = frame(video, 900)
        assertTrue("the text is drawn around y 48", changed(withText, withoutText, 100..220, 30..66) > 100)
        assertTrue("nothing else changes", changed(withText, withoutText, 0..320, 100..240) < 10)
        assertTrue("the text ends at 3 s", changed(frame(video, 3_500), frame(video, 2_500), 100..220, 30..66) > 100)
        // The audio clip starts at 500 ms and lasts 3 s. zh-CN: 音频段在 500 ms 开始, 持续 3 秒.
        run(plugin, request(MediaStudioOps.EXTRACT_AUDIO, listOf(video), output("compose-basic.wav")))
        val wav = Wav(output("compose-basic.wav"))
        assertTrue("silence before the audio clip", wav.peak(0, 400) < 300)
        assertTrue("the tone plays", wav.peak(1_000, 3_000) > 10_000)
        assertTrue("silence after the audio clip", wav.peak(3_650, 3_950) < 300)
    }

    @Test
    fun upperTracksCoverLowerTracksAndTheBackgroundFillsTheLetterbox() = withMediaStudio { plugin ->
        val blue = solid("blue-2s.mp4", BLUE, 2_000)
        val red = solid("red-2s.mp4", RED, 2_000)
        val timeline = options(
            "width" to 640,
            "height" to 240,
            "background" to "#00FF00",
            "video" to listOf(
                options("clips" to listOf(options("src" to blue.path))),
                options("clips" to listOf(options("src" to red.path, "at" to 500, "transform" to options("scale" to 0.25)))),
            ),
        )
        val result = run(plugin, compose(timeline, output("compose-tracks.mp4")))
        assertEquals(640, result.getInt(MediaStudioResultKeys.WIDTH))
        assertDuration(2_500, result, tolerance = 100)
        val video = File(result.getString(MediaStudioResultKeys.PATH)!!)
        frame(video, 300).let { frame ->
            assertColor("the lower track, letterboxed", BLUE, frame.getPixel(320, 120))
            assertColor("the background beside it", 0x00FF00, frame.getPixel(40, 120))
        }
        frame(video, 1_000).let { frame ->
            assertColor("the upper track, scaled to a quarter", RED, frame.getPixel(320, 120))
            assertColor("the lower track around it", BLUE, frame.getPixel(230, 120))
        }
        frame(video, 2_300).let { frame ->
            assertColor("only the upper track after the lower one ends", RED, frame.getPixel(320, 120))
            assertColor("the background where no track shows", 0x00FF00, frame.getPixel(230, 120))
        }
    }

    @Test
    fun timelineErrorsNameTheFieldPath() = withMediaStudio { plugin ->
        val clip = options("src" to fixtures.colorBarsMp4.path)
        val short = options("src" to fixtures.colorBarsMp4.path, "transition" to options("type" to "dissolve", "duration" to 50))
        val error = assertThrows(IllegalArgumentException::class.java) {
            plugin.submit(compose(options("video" to listOf(options("clips" to listOf(clip, short)))), output("bad-1.mp4")), RecordingCallback())
        }
        assertEquals(
            "${MediaStudioErrorCodes.INVALID_OPTIONS}: timeline.video[0].clips[1].transition.duration must be between 100 and 3000, got 50",
            error.message,
        )
        val unknown = assertThrows(IllegalArgumentException::class.java) {
            plugin.submit(compose(options("video" to listOf(options("clips" to listOf(options("src" to fixtures.colorBarsMp4.path, "volumes" to 1))))), output("bad-2.mp4")), RecordingCallback())
        }
        assertTrue(unknown.message, unknown.message!!.endsWith("unknown option timeline.video[0].clips[0].volumes"))
        // Rules that need the media fail in the task. zh-CN: 需要媒体的规则在任务中失败.
        val late = options("src" to fixtures.colorBarsMp4.path, "at" to 1_000, "transition" to options("type" to "dissolve", "duration" to 500))
        val callback = RecordingCallback()
        plugin.submit(compose(options("video" to listOf(options("clips" to listOf(clip, late)))), output("bad-3.mp4")), callback)
        val failure = callback.awaitError()
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, failure.getString(MediaStudioContract.KEY_ERROR_CODE))
        assertTrue(failure.toString(), failure.getString(MediaStudioContract.KEY_ERROR_MESSAGE)!!.startsWith("timeline.video[0].clips[1].at (1000) must be 2500"))
        assertNoPartFiles()
    }

    // region P3.2 transitions

    @Test
    fun cut() = transition("cut") { video ->
        assertColor("before the cut", RED, frame(video, 950).getPixel(160, 120))
        assertColor("after the cut", BLUE, frame(video, 1_050).getPixel(160, 120))
    }

    @Test
    fun fade() = transition("fade") { video ->
        assertColor("through black", 0x000000, frame(video, 1_000).getPixel(160, 120), tolerance = 24)
        assertBetween("the first clip at half brightness", Color.red(frame(video, 750).getPixel(160, 120)), 60..200)
        assertBetween("the second clip at half brightness", Color.blue(frame(video, 1_250).getPixel(160, 120)), 60..200)
    }

    @Test
    fun dissolve() = transition("dissolve") { video -> assertMixed("both clips", frame(video, 1_000).getPixel(160, 120)) }

    @Test
    fun slide() = transition("slide", "left") { video ->
        frame(video, 1_000).let { frame ->
            assertColor("the outgoing clip moved to the left half", RED, frame.getPixel(60, 120))
            assertColor("the incoming clip on the right half", BLUE, frame.getPixel(260, 120))
        }
    }

    @Test
    fun wipe() = transition("wipe", "up") { video ->
        frame(video, 1_000).let { frame ->
            assertColor("the outgoing clip stays on the top half", RED, frame.getPixel(160, 40))
            assertColor("the incoming clip is revealed from the bottom", BLUE, frame.getPixel(160, 200))
        }
    }

    @Test
    fun zoom() = transition("zoom") { video ->
        frame(video, 1_000).let { frame ->
            assertMixed("both clips in the middle", frame.getPixel(160, 120))
            assertColor("only the outgoing clip in the corner", RED, frame.getPixel(6, 6))
        }
    }

    @Test
    fun blur() = transition("blur") { video -> assertMixed("both clips", frame(video, 1_000).getPixel(160, 120)) }

    @Test
    fun shaderTransitionsFallBackToDissolveWhenTheGpuFails() = withMediaStudio { plugin ->
        ComposeOperation.failShadersForTest = true
        val result = run(plugin, compose(transitionTimeline("wipe", "left"), output("compose-fallback.mp4")))
        val warnings = result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().toList()
        assertTrue(warnings.toString(), warnings.any { "failed on the wipe transitions" in it && "exported as dissolve" in it })
        assertDuration(2_000, result, tolerance = 100)
        assertMixed("dissolved instead", frame(File(result.getString(MediaStudioResultKeys.PATH)!!), 1_000).getPixel(160, 120))
    }

    /**
     * A 2 s sample: 1.5 s red, then 1.5 s blue with a 1 s transition (the non-overlapping `cut` and `fade` take 1 s
     * each); the middle of the transition is at 1 s.
     * zh-CN: 2 秒样本: 1.5 秒红色, 接 1.5 秒蓝色, 转场 1 秒 (不重叠的 `cut` 与 `fade` 各取 1 秒); 转场中点在 1 秒处.
     */
    private fun transition(type: String, direction: String? = null, check: (File) -> Unit) = withMediaStudio { plugin ->
        val result = run(plugin, compose(transitionTimeline(type, direction), output("compose-$type.mp4")))
        assertDuration(2_000, result, tolerance = 100)
        check(File(result.getString(MediaStudioResultKeys.PATH)!!))
    }

    private fun transitionTimeline(type: String, direction: String?): Bundle {
        val overlaps = type != "cut" && type != "fade"
        val length = if (overlaps) 1_500 else 1_000
        val red = solid("red-$length.mp4", RED, length.toLong())
        val blue = solid("blue-$length.mp4", BLUE, length.toLong())
        val transition = options("type" to type, "duration" to 1_000).apply { direction?.let { putString("direction", it) } }
        return options(
            "video" to listOf(options("clips" to listOf(options("src" to red.path), options("src" to blue.path, "transition" to transition)))),
        )
    }

    // endregion

    private fun compose(timeline: Bundle, output: File, options: Bundle? = null): Bundle =
        request(MediaStudioOps.COMPOSE, emptyList(), output, options).apply { putBundle(MediaStudioRequestKeys.TIMELINE, timeline) }

    private fun solid(name: String, rgb: Int, durationMillis: Long): File {
        val file = File(directory, "inputs/$name")
        if (!file.isFile) MediaFixtures.colorBars(file, intArrayOf(rgb), durationMillis * 1_000L)
        return file
    }

    private fun trackKinds(file: File): List<String> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.path)
            return (0 until extractor.trackCount).map { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.substringBefore('/') }.sorted()
        } finally {
            extractor.release()
        }
    }

    /** Pixels in a box whose color differs by more than 96 in some channel. zh-CN: 区域内某通道相差超过 96 的像素数. */
    private fun changed(a: Bitmap, b: Bitmap, xs: IntRange, ys: IntRange): Int {
        var count = 0
        for (y in ys) {
            if (y >= a.height) break
            for (x in xs) {
                if (x >= a.width) break
                val p = a.getPixel(x, y)
                val q = b.getPixel(x, y)
                if (abs(Color.red(p) - Color.red(q)) > 96 || abs(Color.green(p) - Color.green(q)) > 96 || abs(Color.blue(p) - Color.blue(q)) > 96) count++
            }
        }
        return count
    }

    /** Red and blue both clearly present. zh-CN: 红与蓝都明显存在. */
    private fun assertMixed(message: String, pixel: Int) {
        assertBetween("$message: red of ${Integer.toHexString(pixel)}", Color.red(pixel), 50..215)
        assertBetween("$message: blue of ${Integer.toHexString(pixel)}", Color.blue(pixel), 50..215)
        assertTrue("$message: green of ${Integer.toHexString(pixel)}", Color.green(pixel) < 60)
    }

    private fun assertBetween(message: String, value: Int, range: IntRange) = assertTrue("$message: $value not in $range", value in range)

    private class Wav(file: File) {
        private val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        private val channels = bytes.getShort(22).toInt()
        private val sampleRate = bytes.getInt(24)
        private val frames = (bytes.capacity() - WAV_HEADER) / (2 * channels)

        fun peak(fromMillis: Long, toMillis: Long): Int {
            val from = (fromMillis * sampleRate / 1_000).toInt().coerceIn(0, frames)
            val to = (toMillis * sampleRate / 1_000).toInt().coerceIn(from, frames)
            return (from until to).maxOfOrNull { abs(bytes.getShort(WAV_HEADER + it * channels * 2).toInt()) } ?: 0
        }
    }

    private companion object {
        const val RED = 0xFF0000
        const val BLUE = 0x0000FF
    }

}

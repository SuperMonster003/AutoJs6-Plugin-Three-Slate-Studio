package io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures

import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * Generates the media fixtures with MediaCodec and checks them with MediaExtractor and MediaMetadataRetriever (roadmap
 * P0.3). The files are removed afterwards unless the instrumentation argument `keepMediaFixtures` is `true`, which
 * `.python/generate_media_fixtures.py` passes before it pulls them.
 * zh-CN: 用 MediaCodec 生成媒体夹具, 再用 MediaExtractor 与 MediaMetadataRetriever 检查 (路线图 P0.3). 除非仪器参数
 * `keepMediaFixtures` 为 `true` (`.python/generate_media_fixtures.py` 拉取前会传入), 测试结束后删除文件.
 */
@RunWith(AndroidJUnit4::class)
class MediaFixturesTest {

    @Test
    fun fixturesAreThreeSecondsOfColorBarsAndSine() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.cacheDir, MediaFixtures.DIRECTORY)
        val keep = InstrumentationRegistry.getArguments().getString("keepMediaFixtures") == "true"
        val fixtures = MediaFixtures.generate(directory)
        try {
            single(fixtures.colorBarsMp4).let { format ->
                assertEquals(MediaFormat.MIMETYPE_VIDEO_AVC, format.getString(MediaFormat.KEY_MIME))
                assertEquals(MediaFixtures.WIDTH, format.getInteger(MediaFormat.KEY_WIDTH))
                assertEquals(MediaFixtures.HEIGHT, format.getInteger(MediaFormat.KEY_HEIGHT))
                assertThreeSeconds(format)
            }
            assertColorBars(fixtures.colorBarsMp4)
            single(fixtures.sineM4a).let { format ->
                assertEquals(MediaFormat.MIMETYPE_AUDIO_AAC, format.getString(MediaFormat.KEY_MIME))
                assertEquals(MediaFixtures.SAMPLE_RATE, format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                assertEquals(1, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                assertThreeSeconds(format)
            }
            single(fixtures.sineWav).let { format ->
                assertEquals(MediaFormat.MIMETYPE_AUDIO_RAW, format.getString(MediaFormat.KEY_MIME))
                assertEquals(MediaFixtures.SAMPLE_RATE, format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                assertThreeSeconds(format)
            }
            tracks(fixtures.barsToneMp4).let { formats ->
                assertEquals(listOf(MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_AUDIO_AAC), formats.map { it.getString(MediaFormat.KEY_MIME) })
                formats.forEach(::assertThreeSeconds)
            }
            val wav = fixtures.sineWav.readBytes()
            assertEquals(44 + MediaFixtures.TOTAL_SAMPLES * 2, wav.size)
            assertArrayEquals("RIFF".toByteArray(Charsets.US_ASCII), wav.copyOfRange(0, 4))
            assertArrayEquals("WAVE".toByteArray(Charsets.US_ASCII), wav.copyOfRange(8, 12))
            // A quarter period (25 samples at 440 Hz) after zero the half scale sine peaks near 16384.
            assertTrue(abs(abs(MediaFixtures.sample(25).toInt()) - 16384) <= 120)
        } finally {
            if (!keep) directory.deleteRecursively()
        }
    }

    private fun single(file: File): MediaFormat = tracks(file).also { assertEquals("${file.name} has one track", 1, it.size) }.single()

    private fun tracks(file: File): List<MediaFormat> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.path)
            return (0 until extractor.trackCount).map(extractor::getTrackFormat)
        } finally {
            extractor.release()
        }
    }

    private fun assertThreeSeconds(format: MediaFormat) {
        val duration = format.getLong(MediaFormat.KEY_DURATION)
        assertTrue("duration $duration us", abs(duration - MediaFixtures.DURATION_US) <= 100_000L)
    }

    /** Decodes the first frame and samples the center of each bar. zh-CN: 解码首帧并取每条彩条中心的颜色. */
    private fun assertColorBars(file: File) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.path)
            val frame = requireNotNull(retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)) { "No frame" }
            assertEquals(MediaFixtures.WIDTH, frame.width)
            val barWidth = frame.width / MediaFixtures.BARS.size
            MediaFixtures.BARS.forEachIndexed { bar, expected ->
                val actual = frame.getPixel(bar * barWidth + barWidth / 2, frame.height / 2)
                listOf(
                    Color.red(actual) to (expected shr 16 and 0xFF),
                    Color.green(actual) to (expected shr 8 and 0xFF),
                    Color.blue(actual) to (expected and 0xFF),
                ).forEach { (channel, wanted) ->
                    assertTrue("bar $bar: ${Integer.toHexString(actual)} vs ${Integer.toHexString(expected)}", abs(channel - wanted) <= 48)
                }
            }
        } finally {
            retriever.release()
        }
    }

}

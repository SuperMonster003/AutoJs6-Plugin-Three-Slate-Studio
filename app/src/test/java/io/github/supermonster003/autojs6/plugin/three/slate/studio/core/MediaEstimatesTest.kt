package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Default bit rates of roadmap D29 (appendix D Q12) and the `warnings[]` codes of appendix C.3 (Q6).
 * zh-CN: 路线图 D29 的默认码率 (附录 D Q12) 与附录 C.3 的 `warnings[]` 警告码 (Q6).
 */
class MediaEstimatesTest {

    @Test
    fun `1080p30 H264 is 8 Mbps and scales with pixels, frame rate and codec`() {
        assertEquals(8_000_000, BitRates.estimateVideo(1920, 1080, 30.0, VideoCodec.H264))
        assertEquals("unknown frame rate counts as 30", 8_000_000, BitRates.estimateVideo(1920, 1080, null, VideoCodec.H264))
        assertEquals(16_000_000, BitRates.estimateVideo(1920, 1080, 60.0, VideoCodec.H264))
        assertEquals("the frame rate factor stops at 2x", 16_000_000, BitRates.estimateVideo(1920, 1080, 120.0, VideoCodec.H264))
        assertEquals("and at 0.5x", 4_000_000, BitRates.estimateVideo(1920, 1080, 5.0, VideoCodec.H264))
        assertEquals(4_800_000, BitRates.estimateVideo(1920, 1080, 30.0, VideoCodec.H265))
        assertEquals(2_000_000, BitRates.estimateVideo(960, 540, 30.0, VideoCodec.H264))
        assertEquals(32_000_000, BitRates.estimateVideo(3840, 2160, 30.0, VideoCodec.H264))
    }

    @Test
    fun `estimates stay between 200 kbps and 100 Mbps`() {
        assertEquals(200_000, BitRates.estimateVideo(16, 16, 30.0, VideoCodec.H264))
        assertEquals(100_000_000, BitRates.estimateVideo(8192, 8192, 120.0, VideoCodec.H264))
        assertEquals("a non-positive frame rate counts as 30", 8_000_000, BitRates.estimateVideo(1920, 1080, -1.0, VideoCodec.H264))
    }

    @Test
    fun `audio bit rates follow the channel count`() {
        assertEquals(128_000, BitRates.aac(1))
        assertEquals(192_000, BitRates.aac(2))
        assertEquals(64_000, BitRates.opus(1))
        assertEquals(96_000, BitRates.opus(2))
    }

    @Test
    fun `warnings are bare codes for a large or long output or input`() {
        val small = MediaWarnings.Media(sizeBytes = 1_000, durationMillis = 1_000)
        val twoGiB = 2L * 1024 * 1024 * 1024
        val hour = 60L * 60 * 1000
        assertEquals(emptyList<String>(), MediaWarnings.of(small, listOf(small)))
        assertEquals("exactly 2 GiB and 60 minutes are fine", emptyList<String>(), MediaWarnings.of(MediaWarnings.Media(twoGiB, hour), emptyList()))
        assertEquals(listOf("OUTPUT_LARGE"), MediaWarnings.of(MediaWarnings.Media(twoGiB + 1, 1_000), emptyList()))
        assertEquals(listOf("DURATION_LONG"), MediaWarnings.of(small, listOf(MediaWarnings.Media(1_000, hour + 1))))
        assertEquals(
            listOf("OUTPUT_LARGE", "DURATION_LONG"),
            MediaWarnings.of(small, listOf(MediaWarnings.Media(twoGiB + 1, 1_000), MediaWarnings.Media(1_000, hour + 1))),
        )
    }

}

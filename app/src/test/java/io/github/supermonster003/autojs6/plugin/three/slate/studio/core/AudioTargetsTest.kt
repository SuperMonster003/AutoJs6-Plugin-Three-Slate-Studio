package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Output formats of the audio operations and the sample arithmetic of mixing, gain and fades (roadmap P2.5, D15).
 * zh-CN: 音频操作的输出格式, 以及混音, 增益与淡入淡出的样本运算 (路线图 P2.5, D15).
 */
class AudioTargetsTest {

    private val none = AudioEncoding()

    @Test
    fun `the target keeps what the container takes`() {
        assertEquals(AudioTargets.Target(44_100, 2), AudioTargets.of(AudioContainer.M4A, none, 44_100, 2))
        assertEquals("M4A rounds up to an AAC rate", AudioTargets.Target(24_000, 1), AudioTargets.of(AudioContainer.M4A, none, 23_000, 1))
        assertEquals("M4A caps at 48 kHz", AudioTargets.Target(48_000, 2), AudioTargets.of(AudioContainer.M4A, none, 96_000, 2))
        assertEquals("OGG runs at 48 kHz", AudioTargets.Target(48_000, 1), AudioTargets.of(AudioContainer.OGG, none, 44_100, 1))
        assertEquals("FLAC keeps the source", AudioTargets.Target(96_000, 2), AudioTargets.of(AudioContainer.FLAC, none, 96_000, 6))
        assertEquals("WAV keeps every channel", AudioTargets.Target(48_000, 6), AudioTargets.of(AudioContainer.WAV, none, 48_000, 6))
        assertEquals(AudioTargets.Target(16_000, 1), AudioTargets.of(AudioContainer.WAV, AudioEncoding(null, 16_000, 1), 48_000, 6))
    }

    @Test
    fun `several inputs count as one source with the highest rate and the most channels`() {
        assertEquals(48_000 to 2, AudioTargets.common(listOf(44_100, 48_000, null), listOf(1, 2, null)))
        assertEquals(44_100 to 2, AudioTargets.common(listOf(null, 0), listOf(null)))
    }

    @Test
    fun `bit rates stay within AAC LC and skip the lossless containers`() {
        assertEquals(192_000, AudioTargets.bitRate(AudioContainer.M4A, none, AudioTargets.Target(44_100, 2)))
        assertEquals("6 bits per sample at 8 kHz mono", 48_000, AudioTargets.bitRate(AudioContainer.M4A, none, AudioTargets.Target(8_000, 1)))
        assertEquals(96_000, AudioTargets.bitRate(AudioContainer.OGG, none, AudioTargets.Target(48_000, 2)))
        assertEquals(64_000, AudioTargets.bitRate(AudioContainer.M4A, AudioEncoding(64_000, null, null), AudioTargets.Target(44_100, 2)))
        assertNull(AudioTargets.bitRate(AudioContainer.FLAC, none, AudioTargets.Target(44_100, 2)))
        assertNull(AudioTargets.bitRate(AudioContainer.WAV, none, AudioTargets.Target(44_100, 2)))
        assertEquals(44_100L, AudioTargets.frames(1_000, 44_100))
        assertEquals(22L, AudioTargets.frames(1, 22_050))
    }

    @Test
    fun `normalize scales the peak to the target`() {
        assertNull("silence stays", PcmMath.normalizeFactor(0, -1.0))
        assertEquals(2.0, PcmMath.normalizeFactor(16_383, 0.0)!!, 1e-3)
        assertEquals(32_767 * 0.891 / 1_000, PcmMath.normalizeFactor(1_000, -1.0)!!, 1e-2)
        assertEquals(32_768, PcmMath.peak(shortArrayOf(5, Short.MIN_VALUE, 100)))
        assertEquals(100, PcmMath.peak(shortArrayOf(5, -3, 100, Short.MIN_VALUE), count = 3))
    }

    @Test
    fun `fades are linear over whole frames and multiply where they overlap`() {
        val fades = PcmMath.Fades(inFrames = 4, outFrames = 4, totalFrames = 10)
        assertEquals(0.0, fades.gain(0), 0.0)
        assertEquals(0.5, fades.gain(2), 0.0)
        assertEquals(1.0, fades.gain(5), 0.0)
        assertEquals(0.75, fades.gain(6), 0.0)
        assertEquals(0.0, fades.gain(9), 0.0)
        assertEquals(0.5 * 0.75, PcmMath.Fades(4, 4, 6).gain(2), 1e-9)
        assertTrue(PcmMath.Fades(0, 4, null).isEmpty)
        assertEquals("no total, no fade out", 1.0, PcmMath.Fades(0, 4, null).gain(100), 0.0)
    }

    @Test
    fun `gain rounds, clamps and counts clipped samples`() {
        val samples = shortArrayOf(100, -100, 20_000, -20_000)
        assertEquals(2, PcmMath.applyGain(samples, channels = 2, firstFrame = 0, factor = 2.0, fades = null))
        assertArrayEquals(shortArrayOf(200, -200, Short.MAX_VALUE, Short.MIN_VALUE), samples)
        val faded = shortArrayOf(1_000, 1_000, 1_000, 1_000)
        assertEquals(0, PcmMath.applyGain(faded, channels = 1, firstFrame = 1, factor = 1.0, fades = PcmMath.Fades(2, 0, null)))
        assertArrayEquals(shortArrayOf(500, 1_000, 1_000, 1_000), faded)
    }

    @Test
    fun `mixing sums with volume and clips the result (Q17 (a))`() {
        val mix = FloatArray(4)
        PcmMath.accumulate(mix, 0, shortArrayOf(10_000, 10_000, 30_000), 0, 3, 1f)
        PcmMath.accumulate(mix, 1, shortArrayOf(9, 20_000, 20_000, -4), 1, 3, 0.5f)
        val out = ShortArray(4)
        assertEquals(1, PcmMath.clip(mix, 3, out))
        assertArrayEquals(shortArrayOf(10_000, 20_000, Short.MAX_VALUE, 0), out)
    }

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures.MediaFixtures
import org.autojs.plugin.mediastudio.api.IMediaStudioPlugin
import org.autojs.plugin.mediastudio.api.MediaStudioCapabilityKeys
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * The audio operations of roadmap P2.5 (D15) on the 440 Hz sine fixtures (3 s, mono, 44.1 kHz, half scale). Every output
 * format is decoded again by the platform (frame count, rate, channels); WAV outputs are measured sample by sample
 * (peaks per segment, tone frequency).
 * zh-CN: 在 440 Hz 正弦波夹具 (3 秒, 单声道, 44.1 kHz, 半幅) 上验证路线图 P2.5 (D15) 的音频操作. 每种输出格式都由平台重新解码
 * (帧数, 采样率, 声道); WAV 输出逐样本测量 (分段峰值, 音调频率).
 */
@RunWith(AndroidJUnit4::class)
class AudioOperationsTest : MediaStudioTestBase() {

    /** What the platform decodes from a file. zh-CN: 平台从文件解码出的内容. */
    private data class Decoded(val sampleRate: Int, val channels: Int, val frames: Long) {
        val durationMillis: Long get() = frames * 1_000L / sampleRate
    }

    private fun decode(file: File): Decoded {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.path)
        val track = (0 until extractor.trackCount).first { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/") }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var bytes = 0L
            var inputDone = false
            var output = codec.outputFormat
            while (true) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val index = codec.dequeueOutputBuffer(info, 10_000)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) output = codec.outputFormat
                if (index >= 0) {
                    bytes += info.size
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            val channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) output.getInteger(MediaFormat.KEY_PCM_ENCODING) else 2
            val bytesPerSample = when (encoding) {
                3 -> 1
                4, 22 -> 4
                21 -> 3
                else -> 2
            }
            return Decoded(output.getInteger(MediaFormat.KEY_SAMPLE_RATE), channels, bytes / (bytesPerSample * channels))
        } finally {
            codec.release()
            extractor.release()
        }
    }

    /** The samples of a canonical 16 bit WAV of the plugin. zh-CN: 插件写出的标准 16 位 WAV 的样本. */
    private class Wav(file: File) {
        private val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        val channels = bytes.getShort(22).toInt()
        val sampleRate = bytes.getInt(24)
        val frames = (bytes.capacity() - MediaStudioTestBase.WAV_HEADER) / (2 * channels)

        fun sample(frame: Int, channel: Int = 0): Int = bytes.getShort(MediaStudioTestBase.WAV_HEADER + (frame * channels + channel) * 2).toInt()

        /** The peak of the first channel between two times. zh-CN: 第一声道在两个时间之间的峰值. */
        fun peak(fromMillis: Long, toMillis: Long): Int {
            val from = (fromMillis * sampleRate / 1_000).toInt().coerceIn(0, frames)
            val to = (toMillis * sampleRate / 1_000).toInt().coerceIn(from, frames)
            return (from until to).maxOfOrNull { abs(sample(it)) } ?: 0
        }
    }

    private val half = (0.5 * Short.MAX_VALUE).toInt()

    private fun assertNear(message: String, expected: Double, actual: Double, relative: Double) =
        assertTrue("$message: $actual, expected $expected +- ${relative * 100} %", abs(actual - expected) <= abs(expected) * relative)

    private fun warnings(result: Bundle) = result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().toList()

    private fun error(plugin: IMediaStudioPlugin, request: Bundle): Pair<String?, String?> {
        val callback = RecordingCallback()
        assertTrue(plugin.submit(request, callback) > 0)
        val error = callback.awaitError()
        return error.getString(MediaStudioContract.KEY_ERROR_CODE) to error.getString(MediaStudioContract.KEY_ERROR_MESSAGE)
    }

    @Test
    fun audioConvertWritesEveryFormat() = withMediaStudio { plugin ->
        val formats = plugin.capabilities.getStringArray(MediaStudioCapabilityKeys.AUDIO_OUTPUT_FORMATS).orEmpty().toList()
        val expected = mapOf("m4a" to "aac", "wav" to "pcm", "flac" to "flac", "ogg" to "opus").filterKeys { it in formats }
        assertTrue("m4a and wav are always available: $formats", "m4a" in expected && "wav" in expected)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) assertTrue("ogg needs API 29: $formats", "ogg" in expected)
        expected.forEach { (extension, codec) ->
            listOf(1, 2).forEach { channels ->
                val file = output("convert-$channels.$extension")
                val result = run(plugin, request(MediaStudioOps.AUDIO_CONVERT, listOf(fixtures.sineM4a), file, options("channels" to channels)))
                assertEquals(extension, codec, result.getString(MediaStudioResultKeys.CODEC))
                assertEquals(extension, channels, result.getInt(MediaStudioResultKeys.CHANNELS))
                assertEquals(extension, if (extension == "ogg") 48_000 else MediaFixtures.SAMPLE_RATE, result.getInt(MediaStudioResultKeys.SAMPLE_RATE))
                assertDuration(3_000, result, tolerance = 150)
                val decoded = decode(file)
                assertEquals("$file decodes to $channels channels", channels, decoded.channels)
                assertTrue("$file decodes to ${decoded.durationMillis} ms", abs(decoded.durationMillis - 3_000) <= 150)
            }
        }
        val low = run(plugin, request(MediaStudioOps.AUDIO_CONVERT, listOf(fixtures.sineM4a), output("convert-16k.wav"), options("sampleRate" to 16_000)))
        assertEquals(16_000, low.getInt(MediaStudioResultKeys.SAMPLE_RATE))
        assertNear("tone after resampling", MediaFixtures.TONE_HZ, frequency(output("convert-16k.wav")), 0.03)
        val (code, _) = error(plugin, request(MediaStudioOps.AUDIO_CONVERT, listOf(fixtures.colorBarsMp4), output("convert-silent.wav")))
        assertEquals("a video without audio", MediaStudioErrorCodes.UNSUPPORTED_FORMAT, code)
    }

    @Test
    fun audioTrimCutsInSourceTime() = withMediaStudio { plugin ->
        val wav = run(plugin, request(MediaStudioOps.AUDIO_TRIM, listOf(fixtures.sineWav), output("trim.wav"), options("start" to 500, "end" to 2_000)))
        assertDuration(1_500, wav, tolerance = 5)
        val trimmed = Wav(output("trim.wav"))
        assertEquals(MediaFixtures.SAMPLE_RATE * 3 / 2, trimmed.frames)
        listOf(0, 1, 25, 1_000, trimmed.frames - 1).forEach { frame ->
            assertEquals("the cut starts at sample 22050: frame $frame", MediaFixtures.sample(MediaFixtures.SAMPLE_RATE / 2 + frame).toInt(), trimmed.sample(frame))
        }
        val m4a = run(plugin, request(MediaStudioOps.AUDIO_TRIM, listOf(fixtures.barsToneMp4), output("trim.m4a"), options("start" to 1_000)))
        assertDuration(2_000, m4a, tolerance = 150)
        assertEquals("aac", m4a.getString(MediaStudioResultKeys.CODEC))
        val tail = run(plugin, request(MediaStudioOps.AUDIO_TRIM, listOf(fixtures.sineWav), output("trim-tail.wav"), options("start" to 2_500, "duration" to 10_000)))
        assertDuration(500, tail, tolerance = 5)
        val (code, message) = error(plugin, request(MediaStudioOps.AUDIO_TRIM, listOf(fixtures.sineWav), output("trim-late.wav"), options("start" to 3_000)))
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, code)
        assertTrue(message, "is not before the end of the input (3000 ms)" in message.orEmpty())
    }

    @Test
    fun audioConcatAppendsInOneFormat() = withMediaStudio { plugin ->
        val stereo = output("concat-stereo-48k.wav")
        run(plugin, request(MediaStudioOps.AUDIO_CONVERT, listOf(fixtures.sineWav), stereo, options("channels" to 2, "sampleRate" to 48_000)))
        val wav = run(plugin, request(MediaStudioOps.AUDIO_CONCAT, listOf(fixtures.sineWav, stereo), output("concat.wav")))
        assertEquals("the most channels", 2, wav.getInt(MediaStudioResultKeys.CHANNELS))
        assertEquals("the highest rate", 48_000, wav.getInt(MediaStudioResultKeys.SAMPLE_RATE))
        assertDuration(6_000, wav, tolerance = 20)
        val samples = Wav(output("concat.wav"))
        assertNear("the mono part is upmixed", half.toDouble(), samples.peak(500, 2_500).toDouble(), 0.03)
        assertNear("the stereo part", half.toDouble(), samples.peak(3_500, 5_500).toDouble(), 0.03)
        assertNear("tone", MediaFixtures.TONE_HZ, frequency(output("concat.wav")), 0.03)
        val m4a = run(plugin, request(MediaStudioOps.AUDIO_CONCAT, listOf(fixtures.sineM4a, fixtures.barsToneMp4, fixtures.sineWav), output("concat.m4a")))
        assertDuration(9_000, m4a, tolerance = 200)
        assertTrue(abs(decode(output("concat.m4a")).durationMillis - 9_000) <= 200)
    }

    @Test
    fun audioMixSumsTheTracksAtTheirOffsets() = withMediaStudio { plugin ->
        val tracks = listOf(options("volume" to 0.5), options("volume" to 0.5, "at" to 1_000))
        val mix = run(plugin, request(MediaStudioOps.AUDIO_MIX, listOf(fixtures.sineWav, fixtures.sineWav), output("mix.wav"), options("tracks" to tracks)))
        assertDuration(4_000, mix, tolerance = 30)
        assertEquals(warnings(mix).toString(), emptyList<String>(), warnings(mix).filter { "clipped" in it })
        val samples = Wav(output("mix.wav"))
        // 1 s is 440 whole periods, so the two tones add in phase. zh-CN: 1 秒恰为 440 个整周期, 两个音调同相相加.
        assertNear("first track alone", half * 0.5, samples.peak(100, 900).toDouble(), 0.05)
        assertNear("both tracks", half.toDouble(), samples.peak(1_200, 2_800).toDouble(), 0.01)
        val m4a = run(plugin, request(MediaStudioOps.AUDIO_MIX, listOf(fixtures.sineM4a, fixtures.barsToneMp4), output("mix.m4a"), options("format" to "m4a")))
        assertDuration(3_000, m4a, tolerance = 150)
        assertTrue(abs(decode(output("mix.m4a")).durationMillis - 3_000) <= 150)
        assertNear("second track alone", half * 0.5, samples.peak(3_100, 3_900).toDouble(), 0.05)
        val loud = run(
            plugin,
            request(MediaStudioOps.AUDIO_MIX, listOf(fixtures.sineWav, fixtures.sineWav), output("mix-loud.m4a"), options("tracks" to listOf(options("volume" to 2.0), options("volume" to 2.0)))),
        )
        // Two passes (appendix D Q17 (c)): the clipping sum is scaled down to -1 dBFS. zh-CN: 两遍: 会削波的和缩放到 -1 dBFS.
        assertTrue(warnings(loud).toString(), warnings(loud).any { "it was scaled by" in it && "to -1 dBFS" in it })
        assertTrue(warnings(loud).toString(), warnings(loud).none { "clipped" in it })
        assertDuration(3_000, loud, tolerance = 150)
        assertEquals(1, decode(output("mix-loud.m4a")).channels)
        val loudWav = run(
            plugin,
            request(MediaStudioOps.AUDIO_MIX, listOf(fixtures.sineWav, fixtures.sineWav), output("mix-loud.wav"), options("tracks" to listOf(options("volume" to 2.0), options("volume" to 2.0)))),
        )
        assertTrue(warnings(loudWav).toString(), warnings(loudWav).any { "peaked at +" in it })
        assertNear("scaled to -1 dBFS", Short.MAX_VALUE * 0.8913, Wav(output("mix-loud.wav")).peak(0, 3_000).toDouble(), 0.002)
    }

    @Test
    fun audioVolumeAppliesGainNormalizationAndFades() = withMediaStudio { plugin ->
        run(plugin, request(MediaStudioOps.AUDIO_VOLUME, listOf(fixtures.sineWav), output("volume-half.wav"), options("gain" to 0.5)))
        assertNear("gain 0.5", half * 0.5, Wav(output("volume-half.wav")).peak(0, 3_000).toDouble(), 0.01)
        run(plugin, request(MediaStudioOps.AUDIO_VOLUME, listOf(fixtures.sineM4a), output("volume-normalized.wav"), options("normalize" to -6.0)))
        assertNear("normalized to -6 dBFS", Short.MAX_VALUE * 0.5012, Wav(output("volume-normalized.wav")).peak(0, 3_000).toDouble(), 0.01)
        val faded = run(plugin, request(MediaStudioOps.AUDIO_VOLUME, listOf(fixtures.sineWav), output("volume-fades.wav"), options("fadeIn" to 1_000, "fadeOut" to 1_000)))
        assertDuration(3_000, faded, tolerance = 5)
        val samples = Wav(output("volume-fades.wav"))
        assertTrue("silent start", samples.peak(0, 20) <= half / 40)
        assertNear("half way into the fade in", half * 0.5, samples.peak(495, 505).toDouble(), 0.05)
        assertNear("full level", half.toDouble(), samples.peak(1_100, 1_900).toDouble(), 0.01)
        assertNear("half way out", half * 0.5, samples.peak(2_495, 2_505).toDouble(), 0.05)
        assertTrue("silent end", samples.peak(2_980, 3_000) <= half / 40)
        val loud = run(plugin, request(MediaStudioOps.AUDIO_VOLUME, listOf(fixtures.sineWav), output("volume-loud.flac"), options("gain" to 4.0)))
        assertTrue(warnings(loud).toString(), warnings(loud).any { "were clipped; lower options.gain" in it })
        assertEquals("flac", loud.getString(MediaStudioResultKeys.CODEC))
    }

    @Test
    fun audioSpeedKeepsOrShiftsThePitch() = withMediaStudio { plugin ->
        val fast = run(plugin, request(MediaStudioOps.AUDIO_SPEED, listOf(fixtures.sineWav), output("speed-2.wav"), options("factor" to 2.0)))
        assertDuration(1_500, fast, tolerance = 60)
        assertNear("pitch kept", MediaFixtures.TONE_HZ, frequency(output("speed-2.wav")), 0.04)
        run(plugin, request(MediaStudioOps.AUDIO_SPEED, listOf(fixtures.sineWav), output("speed-2-chipmunk.wav"), options("factor" to 2.0, "keepPitch" to false)))
        assertNear("pitch doubled", MediaFixtures.TONE_HZ * 2, frequency(output("speed-2-chipmunk.wav")), 0.03)
        val slow = run(plugin, request(MediaStudioOps.AUDIO_SPEED, listOf(fixtures.sineM4a), output("speed-half.m4a"), options("factor" to 0.5)))
        assertDuration(6_000, slow, tolerance = 200)
        assertTrue(abs(decode(output("speed-half.m4a")).durationMillis - 6_000) <= 200)
    }

}

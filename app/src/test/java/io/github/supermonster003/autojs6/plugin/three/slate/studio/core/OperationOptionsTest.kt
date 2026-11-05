package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The option rules of the P2.2 operations (roadmap appendix B.1, 5.2): the host script module copies them from the
 * hand-over note, so every message here is part of the contract the host mirrors.
 * zh-CN: P2.2 各操作的选项规则 (路线图附录 B.1, 5.2): 宿主脚本模块照交接笔记照抄, 因此这里的每条消息都是宿主要对齐的契约.
 */
class OperationOptionsTest {

    private fun parse(op: String, options: Map<String, Any?> = emptyMap(), inputs: Int = if (op == MediaStudioOps.CONCAT) 2 else 1) =
        OperationOptions.parse(op, inputs, options)

    private fun invalid(op: String, options: Map<String, Any?>, inputs: Int = if (op == MediaStudioOps.CONCAT) 2 else 1): String {
        val error = assertThrows(MediaStudioException::class.java) { OperationOptions.parse(op, inputs, options) }
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, error.code)
        assertTrue(error.message!!.startsWith("INVALID_OPTIONS: "))
        return error.detail
    }

    @Test
    fun `input counts follow the operation`() {
        assertEquals("trim takes exactly 1 input, got 2", invalid(MediaStudioOps.TRIM, mapOf("start" to 0), inputs = 2))
        assertEquals("extractAudio takes exactly 1 input, got 0", invalid(MediaStudioOps.EXTRACT_AUDIO, emptyMap(), inputs = 0))
        assertEquals("concat takes at least 2 inputs, got 1", invalid(MediaStudioOps.CONCAT, emptyMap(), inputs = 1))
        assertEquals(OperationSpec.Concat(null, null, Fit.CONTAIN, VideoEncoding()), parse(MediaStudioOps.CONCAT, inputs = 256))
    }

    @Test
    fun `unknown keys are rejected with their full name, nested ones too`() {
        assertEquals("unknown option options.endd", invalid(MediaStudioOps.TRIM, mapOf("start" to 1, "endd" to 5)))
        assertEquals("unknown option options.a, options.b", invalid(MediaStudioOps.REMOVE_AUDIO, mapOf("b" to 1, "a" to 2)))
        assertEquals(
            "unknown option options.crop.w",
            invalid(MediaStudioOps.TRANSFORM, mapOf("crop" to mapOf("width" to 100, "height" to 100, "w" to 1))),
        )
        assertEquals("unknown option options.audio.codec", invalid(MediaStudioOps.TRANSCODE, mapOf("audio" to mapOf("codec" to "aac"))))
    }

    @Test
    fun `null counts as absent`() {
        assertEquals(OperationSpec.Trim(1_000, null), parse(MediaStudioOps.TRIM, mapOf("start" to 1_000, "end" to null)))
        assertEquals(OperationSpec.RemoveAudio, parse(MediaStudioOps.REMOVE_AUDIO, mapOf("anything" to null)))
    }

    @Test
    fun `trim needs a bound, end and duration are exclusive and end follows start`() {
        assertEquals(OperationSpec.Trim(0, 5_000), parse(MediaStudioOps.TRIM, mapOf("end" to 5_000)))
        assertEquals(OperationSpec.Trim(2_000, 5_000), parse(MediaStudioOps.TRIM, mapOf("start" to 2_000.0, "duration" to 3_000)))
        assertEquals(OperationSpec.Trim(2_000, null), parse(MediaStudioOps.TRIM, mapOf("start" to 2_000L)))
        assertEquals("trim needs options.start, options.end or options.duration", invalid(MediaStudioOps.TRIM, emptyMap()))
        assertEquals("options.end and options.duration are exclusive", invalid(MediaStudioOps.TRIM, mapOf("end" to 3, "duration" to 3)))
        assertEquals("options.end (1000) must be greater than options.start (1000)", invalid(MediaStudioOps.TRIM, mapOf("start" to 1_000, "end" to 1_000)))
        assertEquals("options.start must be between 0 and 9007199254740992, got -1", invalid(MediaStudioOps.TRIM, mapOf("start" to -1)))
        assertEquals("options.duration must be between 1 and 9007199254740992, got 0", invalid(MediaStudioOps.TRIM, mapOf("duration" to 0)))
        assertEquals("options.start must be an integer, got 1.5", invalid(MediaStudioOps.TRIM, mapOf("start" to 1.5)))
        assertEquals("options.start must be a number", invalid(MediaStudioOps.TRIM, mapOf("start" to "0")))
        assertEquals("options.start must be a finite number", invalid(MediaStudioOps.TRIM, mapOf("start" to Double.POSITIVE_INFINITY)))
        assertEquals(
            "options.start + options.duration is too large",
            invalid(MediaStudioOps.TRIM, mapOf("start" to OperationOptions.MAX_TIME_MILLIS, "duration" to 1)),
        )
    }

    @Test
    fun `speed needs a factor between 0_25 and 8 and keeps the pitch by default`() {
        assertEquals(OperationSpec.Speed(2.0, true, VideoEncoding()), parse(MediaStudioOps.SPEED, mapOf("factor" to 2)))
        assertEquals(
            OperationSpec.Speed(0.25, false, VideoEncoding(VideoCodec.H265, 4_000_000)),
            parse(MediaStudioOps.SPEED, mapOf("factor" to 0.25, "keepPitch" to false, "codec" to "h265", "bitRate" to 4_000_000)),
        )
        assertEquals(OperationSpec.Speed(8.0, true, VideoEncoding()), parse(MediaStudioOps.SPEED, mapOf("factor" to 8)))
        assertEquals("options.factor is required", invalid(MediaStudioOps.SPEED, emptyMap()))
        assertEquals("options.factor must be at least 0.25 and at most 8, got 8.5", invalid(MediaStudioOps.SPEED, mapOf("factor" to 8.5)))
        assertEquals("options.factor must be at least 0.25 and at most 8, got 0.2", invalid(MediaStudioOps.SPEED, mapOf("factor" to 0.2)))
        assertEquals("options.keepPitch must be a boolean", invalid(MediaStudioOps.SPEED, mapOf("factor" to 2, "keepPitch" to "yes")))
    }

    @Test
    fun `video encoding options are checked for every video operation`() {
        assertEquals("options.codec must be one of h264, h265, got vp9", invalid(MediaStudioOps.CONCAT, mapOf("codec" to "vp9")))
        assertEquals("options.codec must be a string", invalid(MediaStudioOps.CONCAT, mapOf("codec" to 264)))
        assertEquals("options.bitRate must be between 100000 and 200000000, got 99999", invalid(MediaStudioOps.CONCAT, mapOf("bitRate" to 99_999)))
        assertEquals("frameRate is a transcode option", "unknown option options.frameRate", invalid(MediaStudioOps.CONCAT, mapOf("frameRate" to 30)))
        assertEquals(
            OperationSpec.Transcode(1280, 720, Fit.COVER, VideoEncoding(VideoCodec.H264, 2_000_000, 29.97), AudioEncoding()),
            parse(MediaStudioOps.TRANSCODE, mapOf("width" to 1281, "height" to 720, "fit" to "cover", "bitRate" to 2_000_000, "frameRate" to 29.97)),
        )
        assertEquals("options.frameRate must be at least 1 and at most 120, got 121", invalid(MediaStudioOps.TRANSCODE, mapOf("frameRate" to 121)))
        assertEquals("options.fit must be one of contain, cover, stretch, got fill", invalid(MediaStudioOps.TRANSCODE, mapOf("fit" to "fill")))
        assertEquals(
            "removeAudio copies the video and takes no options",
            "unknown option options.codec",
            invalid(MediaStudioOps.REMOVE_AUDIO, mapOf("codec" to "h264")),
        )
    }

    @Test
    fun `dimensions are 16 - 8192 and rounded down to even`() {
        assertEquals(OperationSpec.Concat(16, 8192, Fit.CONTAIN, VideoEncoding()), parse(MediaStudioOps.CONCAT, mapOf("width" to 17, "height" to 8192)))
        assertEquals("options.width must be between 16 and 8192, got 15", invalid(MediaStudioOps.CONCAT, mapOf("width" to 15)))
        assertEquals("options.height must be between 16 and 8192, got 8193", invalid(MediaStudioOps.CONCAT, mapOf("height" to 8193)))
    }

    @Test
    fun `transform needs an effect and normalizes the rotation`() {
        assertEquals(
            "transform needs one of options.width, height, scale, rotate, crop or flip",
            invalid(MediaStudioOps.TRANSFORM, mapOf("rotate" to 360)),
        )
        assertEquals(270, (parse(MediaStudioOps.TRANSFORM, mapOf("rotate" to -90)) as OperationSpec.Transform).rotate)
        assertEquals(90, (parse(MediaStudioOps.TRANSFORM, mapOf("rotate" to 450.0)) as OperationSpec.Transform).rotate)
        assertEquals("options.rotate must be a multiple of 90, got 45", invalid(MediaStudioOps.TRANSFORM, mapOf("rotate" to 45)))
        assertEquals(
            OperationSpec.Transform(null, null, 0.5, 0, null, Flip.HORIZONTAL, Fit.CONTAIN, VideoEncoding()),
            parse(MediaStudioOps.TRANSFORM, mapOf("scale" to 0.5, "flip" to "horizontal")),
        )
        assertEquals("options.scale must be greater than 0 and at most 8, got 0", invalid(MediaStudioOps.TRANSFORM, mapOf("scale" to 0)))
        assertEquals("options.scale and options.width / options.height are exclusive", invalid(MediaStudioOps.TRANSFORM, mapOf("scale" to 2, "width" to 640)))
        assertEquals("options.flip must be one of none, horizontal, vertical, both, got x", invalid(MediaStudioOps.TRANSFORM, mapOf("flip" to "x")))
    }

    @Test
    fun `crop needs a size of at least 16 and defaults its origin to 0`() {
        assertEquals(
            CropRect(0, 0, 640, 360),
            (parse(MediaStudioOps.TRANSFORM, mapOf("crop" to mapOf("width" to 640, "height" to 360))) as OperationSpec.Transform).crop,
        )
        assertEquals(
            CropRect(10, 20, 16, 16),
            (parse(MediaStudioOps.TRANSFORM, mapOf("crop" to mapOf("x" to 10, "y" to 20, "width" to 16, "height" to 16))) as OperationSpec.Transform).crop,
        )
        assertEquals("options.crop.width is required", invalid(MediaStudioOps.TRANSFORM, mapOf("crop" to mapOf("height" to 100))))
        assertEquals("options.crop.height must be between 16 and 2147483647, got 8", invalid(MediaStudioOps.TRANSFORM, mapOf("crop" to mapOf("width" to 100, "height" to 8))))
        assertEquals("options.crop.x must be between 0 and 2147483647, got -1", invalid(MediaStudioOps.TRANSFORM, mapOf("crop" to mapOf("x" to -1, "width" to 100, "height" to 100))))
        assertEquals("options.crop must be an object", invalid(MediaStudioOps.TRANSFORM, mapOf("crop" to "0,0,100,100")))
    }

    @Test
    fun `transcode audio options are a nested object`() {
        assertEquals(
            AudioEncoding(96_000, 44_100, 1),
            (parse(MediaStudioOps.TRANSCODE, mapOf("audio" to mapOf("bitRate" to 96_000, "sampleRate" to 44_100, "channels" to 1))) as OperationSpec.Transcode).audio,
        )
        assertEquals("options.audio.channels must be between 1 and 2, got 6", invalid(MediaStudioOps.TRANSCODE, mapOf("audio" to mapOf("channels" to 6))))
        assertEquals(
            "options.audio.sampleRate must be one of 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000, got 96000",
            invalid(MediaStudioOps.TRANSCODE, mapOf("audio" to mapOf("sampleRate" to 96_000))),
        )
        assertEquals("options.audio must be an object", invalid(MediaStudioOps.TRANSCODE, mapOf("audio" to false)))
    }

    @Test
    fun `extractAudio defaults to m4a and checks the format specific options`() {
        assertEquals(OperationSpec.ExtractAudio(AudioContainer.M4A, AudioEncoding()), parse(MediaStudioOps.EXTRACT_AUDIO))
        assertEquals(
            OperationSpec.ExtractAudio(AudioContainer.OGG, AudioEncoding(64_000, 48_000, 2)),
            parse(MediaStudioOps.EXTRACT_AUDIO, mapOf("format" to "ogg", "bitRate" to 64_000, "sampleRate" to 48_000, "channels" to 2)),
        )
        assertEquals(
            "options.sampleRate must be one of 8000, 12000, 16000, 24000, 48000, got 44100",
            invalid(MediaStudioOps.EXTRACT_AUDIO, mapOf("format" to "ogg", "sampleRate" to 44_100)),
        )
        assertEquals(OperationSpec.ExtractAudio(AudioContainer.WAV, AudioEncoding(null, 16_000, 1)), parse(MediaStudioOps.EXTRACT_AUDIO, mapOf("format" to "wav", "sampleRate" to 16_000, "channels" to 1)))
        assertEquals("options.bitRate does not apply to lossless flac", invalid(MediaStudioOps.EXTRACT_AUDIO, mapOf("format" to "flac", "bitRate" to 128_000)))
        assertEquals("options.format must be one of m4a, wav, flac, ogg, got mp3", invalid(MediaStudioOps.EXTRACT_AUDIO, mapOf("format" to "mp3")))
        assertEquals("options.bitRate must be between 8000 and 512000, got 1000", invalid(MediaStudioOps.EXTRACT_AUDIO, mapOf("bitRate" to 1_000)))
    }

    @Test
    fun `every submit operation parses its minimal options`() {
        val minimal = mapOf(
            MediaStudioOps.TRIM to mapOf("start" to 0),
            MediaStudioOps.SPEED to mapOf("factor" to 1),
            MediaStudioOps.TRANSFORM to mapOf("rotate" to 90),
        )
        OperationOptions.SUBMIT_OPS.forEach { op -> parse(op, minimal[op] ?: emptyMap()) }
        assertEquals(7, OperationOptions.SUBMIT_OPS.size)
        assertTrue(MediaStudioOps.ALL.containsAll(OperationOptions.SUBMIT_OPS))
    }

}

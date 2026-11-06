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

    private fun inputsOf(op: String) = if (op in setOf(MediaStudioOps.CONCAT, MediaStudioOps.AUDIO_CONCAT, MediaStudioOps.AUDIO_MIX)) 2 else 1

    private fun parse(op: String, options: Map<String, Any?> = emptyMap(), inputs: Int = inputsOf(op)) =
        OperationOptions.parse(op, inputs, options)

    private fun invalid(op: String, options: Map<String, Any?>, inputs: Int = inputsOf(op)): String {
        val error = assertThrows(MediaStudioException::class.java) { OperationOptions.parse(op, inputs, options) }
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, error.code)
        assertTrue(error.message!!.startsWith("INVALID_OPTIONS: "))
        return error.detail
    }

    private fun limited(op: String, options: Map<String, Any?>, inputs: Int = inputsOf(op)): String {
        val error = assertThrows(MediaStudioException::class.java) { OperationOptions.parse(op, inputs, options) }
        assertEquals(MediaStudioErrorCodes.LIMIT_EXCEEDED, error.code)
        return error.detail
    }

    @Test
    fun `input counts follow the operation`() {
        assertEquals("trim takes exactly 1 input, got 2", invalid(MediaStudioOps.TRIM, mapOf("start" to 0), inputs = 2))
        assertEquals("extractAudio takes exactly 1 input, got 0", invalid(MediaStudioOps.EXTRACT_AUDIO, emptyMap(), inputs = 0))
        assertEquals("concat takes at least 2 inputs, got 1", invalid(MediaStudioOps.CONCAT, emptyMap(), inputs = 1))
        assertEquals("audioConcat takes at least 2 inputs, got 1", invalid(MediaStudioOps.AUDIO_CONCAT, emptyMap(), inputs = 1))
        assertEquals("audioMix takes at least 2 inputs, got 1", invalid(MediaStudioOps.AUDIO_MIX, emptyMap(), inputs = 1))
        assertEquals("audioVolume takes exactly 1 input, got 2", invalid(MediaStudioOps.AUDIO_VOLUME, mapOf("gain" to 2), inputs = 2))
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
            MediaStudioOps.OVERLAY to mapOf("overlays" to listOf(mapOf("type" to "text", "text" to "a"))),
            MediaStudioOps.FRAMES to mapOf("at" to 0),
            MediaStudioOps.FRAME to mapOf("at" to 0),
            MediaStudioOps.AUDIO_TRIM to mapOf("start" to 0),
            MediaStudioOps.AUDIO_VOLUME to mapOf("gain" to 1),
            MediaStudioOps.AUDIO_CONVERT to mapOf("format" to "wav"),
            MediaStudioOps.AUDIO_SPEED to mapOf("factor" to 1),
        )
        OperationOptions.SUBMIT_OPS.forEach { op -> parse(op, minimal[op] ?: emptyMap()) }
        assertEquals(18, OperationOptions.SUBMIT_OPS.size)
        assertTrue(MediaStudioOps.ALL.containsAll(OperationOptions.SUBMIT_OPS))
    }

    @Test
    fun `frames takes at, times, every or count (P2_4)`() {
        val png = ImageEncoding(ImageFormat.PNG, 90, null)
        val frames = MediaStudioOps.FRAMES
        assertEquals(OperationSpec.Frames(FrameTimes.Points(listOf(1500.5), "at"), png), parse(frames, mapOf("at" to 1500.5)))
        assertEquals(OperationSpec.Frames(FrameTimes.Points(listOf(3000.0, 0.0), "times"), png), parse(frames, mapOf("times" to listOf(3000, 0))))
        assertEquals(
            OperationSpec.Frames(FrameTimes.Every(500.0, 1000.0, 4000.0, 3), ImageEncoding(ImageFormat.JPG, 70, 320)),
            parse(frames, mapOf("every" to 500, "start" to 1000, "end" to 4000, "count" to 3, "format" to "jpeg", "quality" to 70, "width" to 320)),
        )
        assertEquals(OperationSpec.Frames(FrameTimes.Count(10, 0.0, null), png), parse(frames, mapOf("count" to 10)))
        assertEquals("frames needs options.at, times, every or count", invalid(frames, emptyMap()))
        assertEquals("options.at and options.times are exclusive", invalid(frames, mapOf("at" to 0, "times" to listOf(1))))
        assertEquals("options.times and options.every / options.count are exclusive", invalid(frames, mapOf("times" to listOf(1), "count" to 2)))
        assertEquals("options.start and options.end apply to options.every and options.count only", invalid(frames, mapOf("at" to 0, "end" to 5)))
        assertEquals("options.end (1000) must be greater than options.start (1000)", invalid(frames, mapOf("count" to 2, "start" to 1000, "end" to 1000)))
        assertEquals("options.times must hold at least 1 time", invalid(frames, mapOf("times" to emptyList<Int>())))
        assertEquals("options.times[1] must be at least 0 and at most 9007199254740992, got -1", invalid(frames, mapOf("times" to listOf(0, -1))))
        assertEquals("options.times[0] must be a finite number", invalid(frames, mapOf("times" to listOf("a"))))
        assertEquals("options.every must be at least 1 and at most 9007199254740992, got 0.5", invalid(frames, mapOf("every" to 0.5)))
        assertEquals("options.quality applies to jpg only", invalid(frames, mapOf("at" to 0, "quality" to 80)))
        assertEquals("options.format must be one of png, jpg, jpeg, got webp", invalid(frames, mapOf("at" to 0, "format" to "webp")))
        assertEquals("options.width must be between 16 and 8192, got 8", invalid(frames, mapOf("at" to 0, "width" to 8)))
    }

    @Test
    fun `more than 1000 frames is LIMIT_EXCEEDED (Q15)`() {
        assertEquals(1000, OperationOptions.MAX_FRAMES)
        assertEquals("options.count is 1001, at most 1000 frames", limited(MediaStudioOps.FRAMES, mapOf("count" to 1001)))
        assertEquals("options.times holds 1001 times, at most 1000", limited(MediaStudioOps.FRAMES, mapOf("times" to List(1001) { it })))
        parse(MediaStudioOps.FRAMES, mapOf("count" to 1000))
    }

    @Test
    fun `frame needs at, thumbnail defaults to a jpg at quality 85`() {
        assertEquals(OperationSpec.Frame(0.0, ImageEncoding(ImageFormat.PNG, 90, null)), parse(MediaStudioOps.FRAME, mapOf("at" to 0)))
        assertEquals("options.at is required", invalid(MediaStudioOps.FRAME, emptyMap()))
        assertEquals("unknown option options.times", invalid(MediaStudioOps.FRAME, mapOf("at" to 0, "times" to listOf(1))))
        assertEquals(OperationSpec.Thumbnail(null, ImageEncoding(ImageFormat.JPG, 85, null)), parse(MediaStudioOps.THUMBNAIL))
        assertEquals(
            OperationSpec.Thumbnail(2500.0, ImageEncoding(ImageFormat.JPG, 60, 640)),
            parse(MediaStudioOps.THUMBNAIL, mapOf("at" to 2500, "quality" to 60, "width" to 640)),
        )
        assertEquals(ImageFormat.PNG, (parse(MediaStudioOps.THUMBNAIL, mapOf("format" to "png")) as OperationSpec.Thumbnail).image.format)
    }

    @Test
    fun `toGif defaults to 10 fps, loops forever without dithering and checks a known range early`() {
        assertEquals(OperationSpec.ToGif(0.0, null, 10, null, 0, false), parse(MediaStudioOps.TO_GIF))
        assertEquals(
            OperationSpec.ToGif(1000.0, 3000.0, 25, 240, null, true),
            parse(MediaStudioOps.TO_GIF, mapOf("start" to 1000, "end" to 3000, "fps" to 25, "width" to 240, "loop" to false, "dither" to true)),
        )
        assertEquals(3, (parse(MediaStudioOps.TO_GIF, mapOf("loop" to 3)) as OperationSpec.ToGif).loop)
        assertEquals(0, (parse(MediaStudioOps.TO_GIF, mapOf("loop" to true)) as OperationSpec.ToGif).loop)
        assertEquals("options.fps must be between 1 and 50, got 60", invalid(MediaStudioOps.TO_GIF, mapOf("fps" to 60)))
        assertEquals("options.loop must be a boolean or an integer", invalid(MediaStudioOps.TO_GIF, mapOf("loop" to "x")))
        assertEquals("options.loop must be between 0 and 65535, got -1", invalid(MediaStudioOps.TO_GIF, mapOf("loop" to -1)))
        assertEquals("options.end (500) must be greater than options.start (500)", invalid(MediaStudioOps.TO_GIF, mapOf("start" to 500, "end" to 500)))
        parse(MediaStudioOps.TO_GIF, mapOf("end" to 60_000))
        assertEquals(
            "toGif would write 601 frames, at most 600; shorten options.start - options.end or lower options.fps",
            limited(MediaStudioOps.TO_GIF, mapOf("end" to 60_001)),
        )
    }


    @Test
    fun `audio operations share the output keys and default to m4a (P2_5, Q16)`() {
        val m4a = AudioEncoding()
        assertEquals(OperationSpec.AudioConcat(AudioContainer.M4A, m4a), parse(MediaStudioOps.AUDIO_CONCAT))
        assertEquals(OperationSpec.AudioConcat(AudioContainer.M4A, m4a), parse(MediaStudioOps.AUDIO_CONCAT, inputs = 256))
        assertEquals(
            OperationSpec.AudioConcat(AudioContainer.OGG, AudioEncoding(96_000, 48_000, 2)),
            parse(MediaStudioOps.AUDIO_CONCAT, mapOf("format" to "ogg", "bitRate" to 96_000, "sampleRate" to 48_000, "channels" to 2)),
        )
        assertEquals("options.bitRate does not apply to lossless wav", invalid(MediaStudioOps.AUDIO_SPEED, mapOf("factor" to 2, "format" to "wav", "bitRate" to 64_000)))
        assertEquals(
            "options.sampleRate must be one of 8000, 12000, 16000, 24000, 48000, got 44100",
            invalid(MediaStudioOps.AUDIO_TRIM, mapOf("start" to 0, "format" to "ogg", "sampleRate" to 44_100)),
        )
        assertEquals("unknown option options.width", invalid(MediaStudioOps.AUDIO_CONCAT, mapOf("width" to 640)))
        assertEquals(
            "audioConvert needs options.format or an output ending with .m4a, .wav, .flac, .ogg",
            invalid(MediaStudioOps.AUDIO_CONVERT, emptyMap()),
        )
        assertEquals(
            OperationSpec.AudioConvert(AudioContainer.FLAC, AudioEncoding(null, 16_000, 1)),
            parse(MediaStudioOps.AUDIO_CONVERT, mapOf("format" to "flac", "sampleRate" to 16_000, "channels" to 1)),
        )
    }

    @Test
    fun `audioTrim follows the trim rules and audioSpeed the speed rules`() {
        assertEquals(
            OperationSpec.AudioTrim(1_000, 4_000, AudioContainer.M4A, AudioEncoding()),
            parse(MediaStudioOps.AUDIO_TRIM, mapOf("start" to 1_000, "duration" to 3_000)),
        )
        assertEquals(OperationSpec.AudioTrim(0, 2_000, AudioContainer.WAV, AudioEncoding()), parse(MediaStudioOps.AUDIO_TRIM, mapOf("end" to 2_000, "format" to "wav")))
        assertEquals("audioTrim needs options.start, options.end or options.duration", invalid(MediaStudioOps.AUDIO_TRIM, emptyMap()))
        assertEquals("options.end (500) must be greater than options.start (500)", invalid(MediaStudioOps.AUDIO_TRIM, mapOf("start" to 500, "end" to 500)))
        assertEquals(OperationSpec.AudioSpeed(1.5, true, AudioContainer.M4A, AudioEncoding()), parse(MediaStudioOps.AUDIO_SPEED, mapOf("factor" to 1.5)))
        assertEquals(
            OperationSpec.AudioSpeed(0.5, false, AudioContainer.OGG, AudioEncoding()),
            parse(MediaStudioOps.AUDIO_SPEED, mapOf("factor" to 0.5, "keepPitch" to false, "format" to "ogg")),
        )
        assertEquals("options.factor is required", invalid(MediaStudioOps.AUDIO_SPEED, emptyMap()))
        assertEquals("options.factor must be at least 0.25 and at most 8, got 9", invalid(MediaStudioOps.AUDIO_SPEED, mapOf("factor" to 9)))
    }

    @Test
    fun `audioMix takes one track entry per input, at most 16 inputs (Q17, Q18)`() {
        val mix = MediaStudioOps.AUDIO_MIX
        val none = emptyMap<String, Any?>()
        assertEquals(OperationSpec.AudioMix(listOf(MixTrack(), MixTrack()), AudioContainer.M4A, AudioEncoding()), parse(mix))
        assertEquals(
            OperationSpec.AudioMix(listOf(MixTrack(1.0, 0), MixTrack(0.5, 1_000), MixTrack(2.0, 0)), AudioContainer.WAV, AudioEncoding()),
            parse(mix, mapOf("format" to "wav", "tracks" to listOf(none, mapOf("volume" to 0.5, "at" to 1_000), mapOf("volume" to 2))), inputs = 3),
        )
        assertEquals("options.tracks has 1 entries for 2 inputs", invalid(mix, mapOf("tracks" to listOf(mapOf("volume" to 1)))))
        assertEquals("options.tracks[1].volume must be at least 0 and at most 16, got 17", invalid(mix, mapOf("tracks" to listOf(none, mapOf("volume" to 17)))))
        assertEquals("options.tracks[0].at must be between 0 and 9007199254740992, got -5", invalid(mix, mapOf("tracks" to listOf(mapOf("at" to -5), none))))
        assertEquals("unknown option options.tracks[0].gain", invalid(mix, mapOf("tracks" to listOf(mapOf("gain" to 1), none))))
        assertEquals("options.tracks[1] must be an object", invalid(mix, mapOf("tracks" to listOf(none, 1))))
        assertEquals("options.tracks must be an array of objects", invalid(mix, mapOf("tracks" to "all")))
        parse(mix, inputs = 16)
        assertEquals("audioMix takes at most 16 inputs, got 17", limited(mix, none, inputs = 17))
    }

    @Test
    fun `audioVolume takes a linear gain or a normalize target and fades (Q19)`() {
        val volume = MediaStudioOps.AUDIO_VOLUME
        val m4a = AudioContainer.M4A
        assertEquals(OperationSpec.AudioVolume(2.0, null, null, null, m4a, AudioEncoding()), parse(volume, mapOf("gain" to 2)))
        assertEquals(OperationSpec.AudioVolume(null, -1.0, null, null, m4a, AudioEncoding()), parse(volume, mapOf("normalize" to true)))
        assertEquals(
            OperationSpec.AudioVolume(null, -3.0, 500, 1_000, AudioContainer.FLAC, AudioEncoding()),
            parse(volume, mapOf("normalize" to -3, "fadeIn" to 500, "fadeOut" to 1_000, "format" to "flac")),
        )
        assertEquals(OperationSpec.AudioVolume(null, null, null, 800, m4a, AudioEncoding()), parse(volume, mapOf("normalize" to false, "fadeIn" to 0, "fadeOut" to 800)))
        assertEquals("audioVolume needs options.gain, normalize, fadeIn or fadeOut", invalid(volume, mapOf("normalize" to false, "fadeIn" to 0)))
        assertEquals("options.gain and options.normalize are exclusive", invalid(volume, mapOf("gain" to 2, "normalize" to true)))
        assertEquals("options.gain must be at least 0 and at most 16, got 20", invalid(volume, mapOf("gain" to 20)))
        assertEquals("options.normalize must be at least -60 and at most 0, got 1", invalid(volume, mapOf("normalize" to 1)))
        assertEquals("options.normalize must be a boolean or a number", invalid(volume, mapOf("normalize" to "peak")))
        assertEquals("options.fadeOut must be an integer, got 0.5", invalid(volume, mapOf("fadeOut" to 0.5)))
    }

}

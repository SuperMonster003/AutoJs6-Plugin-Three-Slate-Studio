package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * The submit checks after the envelope, in contract order (roadmap D9, D14, D28, D29, Q7), on an in-memory file system.
 * zh-CN: 外层字段之后的 submit 检查, 按契约顺序 (路线图 D9, D14, D28, D29, Q7), 在内存文件系统上进行.
 */
class RequestPlannerTest {

    private class MemoryFiles(vararg files: String) : PlannerFiles {
        val files = files.toMutableSet()
        val unreadable = mutableSetOf<String>()
        val directories = mutableSetOf("/sdcard")
        val unwritable = mutableSetOf<String>()
        override fun exists(path: String) = path in files || path in directories
        override fun isReadableFile(path: String) = path in files && path !in unreadable
        override fun isFile(path: String) = path in files
        override fun ensureDirectory(path: String): Boolean {
            if (path in unwritable) return false
            directories += path
            return true
        }
    }

    private val clip = "/sdcard/DCIM/clip.mp4"
    private val other = "/sdcard/DCIM/other.mp4"
    private val files = MemoryFiles(clip, other)
    private val claimed = mutableSetOf<String>()
    private val denied = mutableSetOf<String>()
    private var encoders = setOf(EncoderIds.H264, EncoderIds.H265, EncoderIds.AAC, EncoderIds.FLAC, EncoderIds.OPUS, EncoderIds.PCM)
    private var oggMuxer = true

    /** 2026-11-05 01:02:03 UTC. zh-CN: 2026-11-05 01:02:03 UTC. */
    private val instant = 1_793_840_523_000L

    private fun planner() = RequestPlanner(
        storageRoot = "/sdcard",
        encoders = encoders,
        oggMuxer = oggMuxer,
        lacksStorageAccess = { it in denied },
        isClaimed = { it in claimed },
        files = files,
        clock = { instant },
        zone = { TimeZone.getTimeZone("UTC") },
    )

    private fun envelope(
        op: String? = MediaStudioOps.TRIM,
        inputs: List<String?> = listOf(clip),
        output: String? = null,
        options: Map<String, Any?> = mapOf("start" to 0, "end" to 1_000),
        hostToken: String? = "host",
        origin: String? = null,
    ) = SubmitEnvelope(op, inputs, output, options, hostToken, origin, registerMedia = true)

    private fun plan(envelope: SubmitEnvelope) = planner().plan(envelope)

    private fun rejected(envelope: SubmitEnvelope): MediaStudioException =
        assertThrows(MediaStudioException::class.java) { plan(envelope) }

    private fun assertRejected(code: String, detail: String, envelope: SubmitEnvelope) {
        val error = rejected(envelope)
        assertEquals(code, error.code)
        assertEquals(detail, error.detail)
        assertEquals("$code: $detail", error.message)
    }

    @Test
    fun `envelope rejections keep their code`() {
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "op is required", envelope(op = null))
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "NOT_IMPLEMENTED: compose", envelope(op = MediaStudioOps.COMPOSE))
        assertRejected(MediaStudioErrorCodes.LIMIT_EXCEEDED, "257 inputs, at most 256", envelope(op = MediaStudioOps.CONCAT, inputs = List(257) { clip }))
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "probe is answered by probe(), not submit", envelope(op = MediaStudioOps.PROBE))
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "audioProbe is answered by probe(), not submit", envelope(op = MediaStudioOps.AUDIO_PROBE))
    }

    @Test
    fun `origin defaults to script and must be known`() {
        assertEquals(MediaStudioContract.ORIGIN_SCRIPT, plan(envelope()).origin)
        assertEquals(MediaStudioContract.ORIGIN_STUDIO, plan(envelope(origin = "studio")).origin)
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "origin must be one of script, studio, got web", envelope(origin = "web"))
    }

    @Test
    fun `inputs and the output must be absolute`() {
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "inputs must be absolute paths, got clip.mp4", envelope(inputs = listOf("clip.mp4")))
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "output must be an absolute file path, got out.mp4", envelope(output = "out.mp4"))
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "output must be an absolute file path, got /sdcard/out/", envelope(output = "/sdcard/out/"))
    }

    @Test
    fun `a blank output means the default of D14 with a collision suffix`() {
        val expected = "/sdcard/Movies/3-Slate Studio/clip-trim-20261105-010203.mp4"
        assertEquals(expected, plan(envelope(output = " ")).output)
        assertTrue("the default folder is created", "/sdcard/Movies/3-Slate Studio" in files.directories)
        files.files += expected
        claimed += "/sdcard/Movies/3-Slate Studio/clip-trim-20261105-010203-1.mp4"
        assertEquals("/sdcard/Movies/3-Slate Studio/clip-trim-20261105-010203-2.mp4", plan(envelope()).output)
    }

    @Test
    fun `concat has no single source and audio goes to Music`() {
        assertEquals(
            "/sdcard/Movies/3-Slate Studio/slate-concat-20261105-010203.mp4",
            plan(envelope(op = MediaStudioOps.CONCAT, inputs = listOf(clip, other), options = emptyMap())).output,
        )
        val audio = plan(envelope(op = MediaStudioOps.EXTRACT_AUDIO, options = mapOf("format" to "ogg")))
        assertEquals("/sdcard/Music/3-Slate Studio/clip-extractAudio-20261105-010203.ogg", audio.output)
        assertTrue(audio.audioOutput)
        assertFalse(plan(envelope()).audioOutput)
    }

    @Test
    fun `video outputs are mp4`() {
        assertEquals("/sdcard/out.mp4", plan(envelope(output = "/sdcard/out.mp4")).output)
        assertEquals("/sdcard/OUT.MP4", plan(envelope(output = "/sdcard/OUT.MP4")).output)
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "video output must end with .mp4, got out.mov", envelope(output = "/sdcard/out.mov"))
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "video output must end with .mp4, got .mp4", envelope(output = "/sdcard/.mp4"))
    }

    @Test
    fun `the audio extension chooses the format and must agree with options_format`() {
        val wav = plan(envelope(op = MediaStudioOps.EXTRACT_AUDIO, output = "/sdcard/a.wav", options = emptyMap()))
        assertEquals(OperationSpec.ExtractAudio(AudioContainer.WAV, AudioEncoding()), wav.spec)
        assertEquals(
            OperationSpec.ExtractAudio(AudioContainer.FLAC, AudioEncoding()),
            plan(envelope(op = MediaStudioOps.EXTRACT_AUDIO, output = "/sdcard/a.flac", options = mapOf("format" to "flac"))).spec,
        )
        assertRejected(
            MediaStudioErrorCodes.INVALID_OPTIONS,
            "options.format flac does not match the output extension .wav",
            envelope(op = MediaStudioOps.EXTRACT_AUDIO, output = "/sdcard/a.wav", options = mapOf("format" to "flac")),
        )
        assertRejected(
            MediaStudioErrorCodes.UNSUPPORTED_FORMAT,
            "audio output must end with .m4a, .wav, .flac, .ogg, got a.mp3",
            envelope(op = MediaStudioOps.EXTRACT_AUDIO, output = "/sdcard/a.mp3", options = emptyMap()),
        )
        assertRejected(
            MediaStudioErrorCodes.INVALID_OPTIONS,
            "options.format must be one of m4a, wav, flac, ogg, got mp3",
            envelope(op = MediaStudioOps.EXTRACT_AUDIO, output = "/sdcard/a.wav", options = mapOf("format" to "mp3")),
        )
    }

    @Test
    fun `options are checked before files`() {
        files.files.clear()
        assertRejected(MediaStudioErrorCodes.INVALID_OPTIONS, "unknown option options.speed", envelope(options = mapOf("start" to 0, "speed" to 2)))
    }

    @Test
    fun `storage access is checked before the files`() {
        denied += clip
        files.files.clear()
        assertRejected(
            MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED,
            "the plugin needs all files access for $clip; grant it in the 3-Slate Studio storage screen",
            envelope(),
        )
        denied.clear()
        denied += "/sdcard/x/out.mp4"
        assertEquals(MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED, rejected(envelope(output = "/sdcard/x/out.mp4")).code)
    }

    @Test
    fun `inputs must exist and be readable`() {
        assertRejected(MediaStudioErrorCodes.IO_ERROR, "input not found: /sdcard/none.mp4", envelope(inputs = listOf("/sdcard/none.mp4")))
        files.unreadable += clip
        assertRejected(MediaStudioErrorCodes.IO_ERROR, "input is not readable: $clip", envelope())
    }

    @Test
    fun `an explicit output must be free and its folder creatable`() {
        assertRejected(MediaStudioErrorCodes.IO_ERROR, "output already exists: $other", envelope(output = other))
        claimed += "/sdcard/busy.mp4"
        assertRejected(MediaStudioErrorCodes.IO_ERROR, "output /sdcard/busy.mp4 is in use by another task", envelope(output = "/sdcard/busy.mp4"))
        files.unwritable += "/sdcard/ro"
        assertRejected(MediaStudioErrorCodes.IO_ERROR, "cannot create the output folder /sdcard/ro", envelope(output = "/sdcard/ro/out.mp4"))
        assertEquals("/sdcard/new/deep/out.mp4", plan(envelope(output = "/sdcard/new/deep/out.mp4")).output)
        assertTrue("/sdcard/new/deep" in files.directories)
    }

    @Test
    fun `H265 needs an encoder, H264 stays the default (Q7)`() {
        val transcode = envelope(op = MediaStudioOps.TRANSCODE, options = mapOf("codec" to "h265"))
        assertEquals(VideoCodec.H265, (plan(transcode).spec as OperationSpec.Transcode).video.codec)
        assertEquals(VideoCodec.H264, (plan(envelope(op = MediaStudioOps.TRANSCODE, options = emptyMap())).spec as OperationSpec.Transcode).video.codec)
        encoders = encoders - EncoderIds.H265
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "this device has no H.265 encoder; use codec h264", transcode)
        assertRejected(
            MediaStudioErrorCodes.UNSUPPORTED_FORMAT,
            "this device has no H.265 encoder; use codec h264",
            envelope(op = MediaStudioOps.SPEED, options = mapOf("factor" to 2, "codec" to "h265")),
        )
        encoders = encoders - EncoderIds.H264
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "this device has no H.264 encoder", envelope(op = MediaStudioOps.CONCAT, inputs = listOf(clip, other), options = emptyMap()))
        assertEquals("trim and removeAudio copy samples when they can", clip, plan(envelope()).inputs.single())
        plan(envelope(op = MediaStudioOps.REMOVE_AUDIO, options = emptyMap()))
    }

    @Test
    fun `audio formats need their encoder or muxer`() {
        fun extract(format: String) = envelope(op = MediaStudioOps.EXTRACT_AUDIO, options = mapOf("format" to format))
        encoders = setOf(EncoderIds.H264)
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "this device has no AAC encoder", extract("m4a"))
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "this device has no FLAC encoder", extract("flac"))
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "this device has no Opus encoder", extract("ogg"))
        assertEquals("wav is written by the plugin", AudioContainer.WAV, (plan(extract("wav")).spec as OperationSpec.ExtractAudio).container)
        oggMuxer = false
        encoders = setOf(EncoderIds.OPUS)
        assertRejected(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, "ogg output needs Android 10 (API 29) or later", extract("ogg"))
    }

    @Test
    fun `the planned task keeps the request`() {
        val task = plan(envelope(op = MediaStudioOps.SPEED, output = "/sdcard/fast.mp4", options = mapOf("factor" to 2), origin = "studio"))
        assertEquals(
            PlannedTask(MediaStudioOps.SPEED, listOf(clip), "/sdcard/fast.mp4", OperationSpec.Speed(2.0, true, VideoEncoding()), "studio", true),
            task,
        )
    }

}

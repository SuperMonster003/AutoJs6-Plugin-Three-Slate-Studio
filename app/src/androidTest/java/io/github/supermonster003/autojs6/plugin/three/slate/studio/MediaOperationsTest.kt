package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures.MediaFixtures
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.ThreeSlateStudioService
import org.autojs.plugin.mediastudio.api.IMediaStudioCallback
import org.autojs.plugin.mediastudio.api.IMediaStudioPlugin
import org.autojs.plugin.mediastudio.api.MediaStudioCapabilityKeys
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioProgressKeys
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeFalse
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * Every operation of roadmap P2.1 / P2.2 end to end over the Binder of `IMediaStudioPlugin` on the 3 second fixtures:
 * `probe`, `trim`, `concat`, `speed` (both pitch modes, measured on the decoded tone), `transform` (checked on the
 * decoded colors), `transcode`, `removeAudio`, `extractAudio` in the four containers, the error codes of `submit`,
 * cancellation and `listTasks`. Inputs and outputs live in the cache folder of the plugin, so no storage grant is needed
 * and nothing reaches the media store. Result Bundles go through a Parcel before they are checked.
 * zh-CN: 在 3 秒夹具上经 `IMediaStudioPlugin` 的 Binder 端到端验证路线图 P2.1 / P2.2 的每个操作: `probe`, `trim`, `concat`,
 * `speed` (两种音高模式, 在解码后的音调上测量), `transform` (在解码后的颜色上检查), `transcode`, `removeAudio`, 四种容器的
 * `extractAudio`, `submit` 的错误码, 取消与 `listTasks`. 输入输出都在插件缓存目录, 无需存储授权, 也不会进入媒体库. 结果 Bundle
 * 先经 Parcel 往返再检查.
 */
@RunWith(AndroidJUnit4::class)
class MediaOperationsTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun probeAnswersTheMuxedFixture() = withMediaStudio { plugin ->
        val info = plugin.probe(Bundle().apply { putString(MediaStudioRequestKeys.PATH, fixtures.barsToneMp4.path) })
        assertNull(info.getString(MediaStudioContract.KEY_ERROR_CODE))
        assertEquals(ThreeSlateStudioContract.CONTRACT_VERSION, info.getInt(MediaStudioContract.KEY_CONTRACT_VERSION))
        assertEquals(fixtures.barsToneMp4.path, info.getString(MediaStudioResultKeys.PATH))
        assertDuration(3000, info)
        assertEquals(fixtures.barsToneMp4.length(), info.getLong(MediaStudioResultKeys.SIZE_BYTES))
        assertTrue(info.getBoolean(MediaStudioResultKeys.HAS_VIDEO))
        assertTrue(info.getBoolean(MediaStudioResultKeys.HAS_AUDIO))
        assertEquals(MediaFixtures.WIDTH, info.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(MediaFixtures.HEIGHT, info.getInt(MediaStudioResultKeys.HEIGHT))
        assertEquals(0, info.getInt(MediaStudioResultKeys.ROTATION))
        assertEquals(MediaFixtures.FRAME_RATE.toDouble(), info.getDouble(MediaStudioResultKeys.FRAME_RATE), 1.0)
        assertEquals("h264", info.getString(MediaStudioResultKeys.VIDEO_CODEC))
        assertEquals("aac", info.getString(MediaStudioResultKeys.AUDIO_CODEC))
        assertEquals(MediaFixtures.SAMPLE_RATE, info.getInt(MediaStudioResultKeys.SAMPLE_RATE))
        assertEquals(1, info.getInt(MediaStudioResultKeys.CHANNELS))
        info.classLoader = Bundle::class.java.classLoader
        @Suppress("DEPRECATION")
        val tracks = requireNotNull(info.getParcelableArray(MediaStudioResultKeys.TRACKS)).map { it as Bundle }
        assertEquals(listOf("h264", "aac"), tracks.map { it.getString(MediaStudioResultKeys.CODEC) })

        val audio = plugin.probe(Bundle().apply {
            putString(MediaStudioRequestKeys.OP, MediaStudioOps.AUDIO_PROBE)
            putString(MediaStudioRequestKeys.PATH, fixtures.sineWav.path)
        })
        assertEquals("pcm", audio.getString(MediaStudioResultKeys.AUDIO_CODEC))
        assertEquals(MediaFixtures.SAMPLE_RATE, audio.getInt(MediaStudioResultKeys.SAMPLE_RATE))
        assertFalse(audio.getBoolean(MediaStudioResultKeys.HAS_VIDEO))
        assertDuration(3000, audio)

        val missing = plugin.probe(Bundle().apply { putString(MediaStudioRequestKeys.PATH, File(directory, "missing.mp4").path) })
        assertEquals(MediaStudioErrorCodes.IO_ERROR, missing.getString(MediaStudioContract.KEY_ERROR_CODE))
        val relative = plugin.probe(Bundle().apply { putString(MediaStudioRequestKeys.PATH, "clip.mp4") })
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, relative.getString(MediaStudioContract.KEY_ERROR_CODE))
    }

    @Test
    fun trimKeepsTheRequestedRangeAndReportsProgress() = withMediaStudio { plugin ->
        val callback = RecordingCallback()
        val output = output("trim.mp4")
        val taskId = plugin.submit(request(MediaStudioOps.TRIM, listOf(fixtures.barsToneMp4), output, options("start" to 500, "end" to 2000)), callback)
        assertTrue(taskId > 0)
        val result = callback.awaitResult()
        assertEquals(output.path, result.getString(MediaStudioResultKeys.PATH))
        assertTrue(output.isFile)
        assertDuration(1500, result, tolerance = 200)
        assertEquals(output.length(), result.getLong(MediaStudioResultKeys.SIZE_BYTES))
        assertEquals(MediaFixtures.WIDTH, result.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals("h264", result.getString(MediaStudioResultKeys.CODEC))
        assertNotNull(result.getStringArray(MediaStudioResultKeys.WARNINGS))
        assertTrue(probe(plugin, output).getBoolean(MediaStudioResultKeys.HAS_AUDIO))

        val percents = callback.progress.map { it.getDouble(MediaStudioProgressKeys.PERCENT) }
        assertTrue("progress $percents", percents.isNotEmpty() && percents.zipWithNext().all { (a, b) -> b >= a } && percents.all { it in 0.0..100.0 })
        assertTrue(callback.progress.all { it.getInt(MediaStudioProgressKeys.TASK_ID) == taskId })
        assertEquals(MediaStudioContract.STAGE_PROBING, callback.progress.first().getString(MediaStudioProgressKeys.STAGE))
        assertNoPartFiles()

        val tasks = listTasks(plugin)
        val task = tasks.single { it.getInt(MediaStudioResultKeys.TASK_ID) == taskId }
        assertEquals(MediaStudioContract.TASK_COMPLETED, task.getString(MediaStudioResultKeys.TASK_STATE))
        assertEquals(MediaStudioOps.TRIM, task.getString(MediaStudioResultKeys.TASK_OP))

        val tail = run(plugin, request(MediaStudioOps.TRIM, listOf(fixtures.colorBarsMp4), output("trim-tail.mp4"), options("start" to 2000)))
        assertDuration(1000, tail, tolerance = 200)
    }

    @Test
    fun concatJoinsTheInputsAndKeepsAnAudioTrack() = withMediaStudio { plugin ->
        val output = output("concat.mp4")
        val result = run(plugin, request(MediaStudioOps.CONCAT, listOf(fixtures.barsToneMp4, fixtures.colorBarsMp4), output))
        assertDuration(6000, result, tolerance = 300)
        assertEquals(MediaFixtures.WIDTH, result.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(MediaFixtures.HEIGHT, result.getInt(MediaStudioResultKeys.HEIGHT))
        assertTrue("the silent second input gets a silent audio track", probe(plugin, output).getBoolean(MediaStudioResultKeys.HAS_AUDIO))
    }

    @Test
    fun speedChangesTheDurationAndKeepsThePitchOnRequest() = withMediaStudio { plugin ->
        val keep = run(plugin, request(MediaStudioOps.SPEED, listOf(fixtures.barsToneMp4), output("speed-keep.mp4"), options("factor" to 2.0)))
        assertDuration(1500, keep, tolerance = 200)
        val shifted = run(
            plugin,
            request(MediaStudioOps.SPEED, listOf(fixtures.barsToneMp4), output("speed-shift.mp4"), options("factor" to 2.0, "keepPitch" to false)),
        )
        assertDuration(1500, shifted, tolerance = 200)
        val keepTone = toneOf(plugin, File(keep.getString(MediaStudioResultKeys.PATH)!!), "speed-keep.wav")
        val shiftedTone = toneOf(plugin, File(shifted.getString(MediaStudioResultKeys.PATH)!!), "speed-shift.wav")
        assertEquals("keepPitch true: $keepTone Hz", MediaFixtures.TONE_HZ, keepTone, MediaFixtures.TONE_HZ * 0.06)
        assertEquals("keepPitch false: $shiftedTone Hz", MediaFixtures.TONE_HZ * 2, shiftedTone, MediaFixtures.TONE_HZ * 2 * 0.06)

        val slow = run(plugin, request(MediaStudioOps.SPEED, listOf(fixtures.colorBarsMp4), output("speed-slow.mp4"), options("factor" to 0.5)))
        assertDuration(6000, slow, tolerance = 300)
    }

    @Test
    fun transformCropsRotatesClockwiseAndFlips() = withMediaStudio { plugin ->
        // The top left quarter holds the bars white, yellow, cyan and green (40 pixels each); after a clockwise quarter
        // turn they run from top to bottom. zh-CN: 左上四分之一含白, 黄, 青, 绿四条 (各 40 像素); 顺时针转 90 度后自上而下排列.
        val rotated = run(
            plugin,
            request(
                MediaStudioOps.TRANSFORM, listOf(fixtures.colorBarsMp4), output("transform-rotate.mp4"),
                options("crop" to options("x" to 0, "y" to 0, "width" to 160, "height" to 120), "rotate" to 90),
            ),
        )
        assertEquals(120, rotated.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(160, rotated.getInt(MediaStudioResultKeys.HEIGHT))
        frame(File(rotated.getString(MediaStudioResultKeys.PATH)!!)).let { frame ->
            listOf(0xFFFFFF, 0xFFFF00, 0x00FFFF, 0x00FF00).forEachIndexed { bar, color -> assertColor("rotated bar $bar", color, frame.getPixel(60, bar * 40 + 20)) }
        }

        val flipped = run(
            plugin,
            request(MediaStudioOps.TRANSFORM, listOf(fixtures.colorBarsMp4), output("transform-flip.mp4"), options("flip" to "horizontal", "scale" to 0.5)),
        )
        assertEquals(160, flipped.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(120, flipped.getInt(MediaStudioResultKeys.HEIGHT))
        frame(File(flipped.getString(MediaStudioResultKeys.PATH)!!)).let { frame ->
            assertColor("flipped left", 0x000000, frame.getPixel(10, 60))
            assertColor("flipped right", 0xFFFFFF, frame.getPixel(150, 60))
        }
    }

    @Test
    fun transcodeResizesAndEncodesTheRequestedCodecs() = withMediaStudio { plugin ->
        val small = run(
            plugin,
            request(
                MediaStudioOps.TRANSCODE, listOf(fixtures.barsToneMp4), output("transcode-small.mp4"),
                options("width" to 160, "bitRate" to 400_000, "frameRate" to 15, "audio" to options("sampleRate" to 22_050, "channels" to 2)),
            ),
        )
        assertEquals(160, small.getInt(MediaStudioResultKeys.WIDTH))
        assertEquals(120, small.getInt(MediaStudioResultKeys.HEIGHT))
        assertEquals("h264", small.getString(MediaStudioResultKeys.CODEC))
        assertDuration(3000, small, tolerance = 200)
        probe(plugin, File(small.getString(MediaStudioResultKeys.PATH)!!)).let { info ->
            assertEquals(15.0, info.getDouble(MediaStudioResultKeys.FRAME_RATE), 1.5)
            assertEquals(22_050, info.getInt(MediaStudioResultKeys.SAMPLE_RATE))
            assertEquals(2, info.getInt(MediaStudioResultKeys.CHANNELS))
        }

        val encoders = plugin.capabilities.getStringArray(MediaStudioCapabilityKeys.VIDEO_ENCODERS).orEmpty().toList()
        val hevc = request(MediaStudioOps.TRANSCODE, listOf(fixtures.colorBarsMp4), output("transcode-h265.mp4"), options("codec" to "h265"))
        if ("h265" in encoders) {
            assertEquals("h265", run(plugin, hevc).getString(MediaStudioResultKeys.CODEC))
        } else {
            val error = assertThrows(IllegalArgumentException::class.java) { plugin.submit(hevc, RecordingCallback()) }
            assertEquals(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, MediaStudioErrorCodes.decode(error.message!!)?.first)
        }
    }

    @Test
    fun removeAudioKeepsTheVideoOnly() = withMediaStudio { plugin ->
        val output = output("remove-audio.mp4")
        val result = run(plugin, request(MediaStudioOps.REMOVE_AUDIO, listOf(fixtures.barsToneMp4), output))
        assertDuration(3000, result, tolerance = 200)
        probe(plugin, output).let { info ->
            assertTrue(info.getBoolean(MediaStudioResultKeys.HAS_VIDEO))
            assertFalse(info.getBoolean(MediaStudioResultKeys.HAS_AUDIO))
        }
    }

    @Test
    fun extractAudioWritesEveryContainer() = withMediaStudio { plugin ->
        val formats = plugin.capabilities.getStringArray(MediaStudioCapabilityKeys.AUDIO_OUTPUT_FORMATS).orEmpty().toList()
        val expected = mapOf("m4a" to "aac", "wav" to "pcm", "flac" to "flac", "ogg" to "opus")
        val written = expected.filterKeys { it in formats }
        assertTrue("m4a and wav are always available: $formats", "m4a" in written && "wav" in written)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) assertTrue("ogg needs API 29: $formats", "ogg" in written)
        written.forEach { (extension, codec) ->
            val result = run(plugin, request(MediaStudioOps.EXTRACT_AUDIO, listOf(fixtures.barsToneMp4), output("extract.$extension")))
            assertEquals(extension, codec, result.getString(MediaStudioResultKeys.CODEC))
            assertDuration(3000, result, tolerance = 200)
            val rate = if (extension == "ogg") 48_000 else MediaFixtures.SAMPLE_RATE
            assertEquals(extension, rate, result.getInt(MediaStudioResultKeys.SAMPLE_RATE))
            assertEquals(extension, 1, result.getInt(MediaStudioResultKeys.CHANNELS))
        }
        assertEquals(MediaFixtures.TONE_HZ, frequency(output("extract.wav")), MediaFixtures.TONE_HZ * 0.03)

        val stereo = run(
            plugin,
            request(MediaStudioOps.EXTRACT_AUDIO, listOf(fixtures.sineWav), output("extract-stereo.wav"), options("channels" to 2, "sampleRate" to 48_000)),
        )
        assertEquals(2, stereo.getInt(MediaStudioResultKeys.CHANNELS))
        assertEquals(48_000, stereo.getInt(MediaStudioResultKeys.SAMPLE_RATE))
        assertEquals(MediaFixtures.TONE_HZ, frequency(output("extract-stereo.wav")), MediaFixtures.TONE_HZ * 0.03)
    }

    @Test
    fun submitRejectionsUseTheContractCodes() = withMediaStudio { plugin ->
        fun rejection(request: Bundle): Pair<String, String> {
            val error = assertThrows(RuntimeException::class.java) { plugin.submit(request, RecordingCallback()) }
            return requireNotNull(MediaStudioErrorCodes.decode(error.message!!)) { "undecodable ${error.message}" }
        }
        val input = listOf(fixtures.barsToneMp4)
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, rejection(request(MediaStudioOps.TRIM, input, output("x.mp4"), options("begin" to 1))).first)
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, rejection(request(MediaStudioOps.SPEED, input, output("x.mp4"), options("factor" to 100.0))).first)
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, rejection(request(MediaStudioOps.CONCAT, input, output("x.mp4"))).first)
        assertEquals(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, rejection(request(MediaStudioOps.TRANSCODE, input, output("x.mov"))).first)
        assertEquals(MediaStudioErrorCodes.IO_ERROR, rejection(request(MediaStudioOps.REMOVE_AUDIO, listOf(File(directory, "missing.mp4")), output("x.mp4"))).first)
        val existing = output("existing.mp4").apply { writeText("taken") }
        rejection(request(MediaStudioOps.REMOVE_AUDIO, input, existing)).let { (code, detail) ->
            assertEquals(MediaStudioErrorCodes.IO_ERROR, code)
            assertTrue(detail, "already exists" in detail)
        }
        assertEquals("taken", existing.readText())
    }

    @Test
    fun sharedStorageNeedsAllFilesAccess() = withMediaStudio { plugin ->
        assumeFalse("all files access is granted on this device", Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager())
        assumeFalse("API 29 and older use runtime storage permissions", Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        @Suppress("DEPRECATION")
        val shared = File(Environment.getExternalStorageDirectory(), "Movies/3-slate-studio-test-${System.nanoTime()}.mp4")
        val error = assertThrows(IllegalStateException::class.java) {
            plugin.submit(request(MediaStudioOps.REMOVE_AUDIO, listOf(fixtures.barsToneMp4), shared), RecordingCallback())
        }
        assertEquals(MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED, MediaStudioErrorCodes.decode(error.message!!)?.first)
        assertFalse(shared.exists())
        val probe = plugin.probe(Bundle().apply { putString(MediaStudioRequestKeys.PATH, shared.path) })
        assertEquals(MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED, probe.getString(MediaStudioContract.KEY_ERROR_CODE))
    }

    @Test
    fun cancelledTasksLeaveNoFiles() = withMediaStudio { plugin ->
        val callback = RecordingCallback()
        val output = output("cancelled.mp4")
        val taskId = plugin.submit(request(MediaStudioOps.SPEED, listOf(fixtures.barsToneMp4), output, options("factor" to 0.25)), callback)
        assertTrue(plugin.cancel(taskId))
        val error = callback.awaitError()
        assertEquals(MediaStudioErrorCodes.TASK_CANCELLED, error.getString(MediaStudioContract.KEY_ERROR_CODE))
        assertFalse(plugin.cancel(taskId))
        assertFalse(plugin.cancel(Int.MAX_VALUE))
        assertFalse(output.exists())
        assertNoPartFiles()
        val task = listTasks(plugin).single { it.getInt(MediaStudioResultKeys.TASK_ID) == taskId }
        assertEquals(MediaStudioContract.TASK_CANCELLED, task.getString(MediaStudioResultKeys.TASK_STATE))
    }

    @Test
    fun aHostTokenHoldsAtMostFourUnfinishedTasks() = withMediaStudio { plugin ->
        val token = "limit-${System.nanoTime()}"
        val callbacks = (1..4).map { RecordingCallback() }
        val ids = callbacks.mapIndexed { index, callback ->
            plugin.submit(request(MediaStudioOps.SPEED, listOf(fixtures.barsToneMp4), output("limit-$index.mp4"), options("factor" to 0.25), token), callback)
        }
        try {
            val error = assertThrows(IllegalStateException::class.java) {
                plugin.submit(request(MediaStudioOps.SPEED, listOf(fixtures.barsToneMp4), output("limit-5.mp4"), options("factor" to 0.25), token), RecordingCallback())
            }
            assertEquals(MediaStudioErrorCodes.TASK_LIMIT, MediaStudioErrorCodes.decode(error.message!!)?.first)
            val states = listTasks(plugin, token).map { it.getString(MediaStudioResultKeys.TASK_STATE) }
            assertEquals(4, states.size)
            assertTrue("at most two run at once: $states", states.count { it == MediaStudioContract.TASK_RUNNING } <= 2)
        } finally {
            ids.forEach(plugin::cancel)
            callbacks.forEach { it.awaitAny() }
        }
        assertNoPartFiles()
    }

    // region Helpers

    /** Collects the callbacks of one task; Bundles take a Parcel round trip first. zh-CN: 收集一个任务的回调; Bundle 先经 Parcel 往返. */
    private class RecordingCallback : IMediaStudioCallback.Stub() {

        val progress = CopyOnWriteArrayList<Bundle>()
        private val outcome = AtomicReference<Pair<Bundle?, Bundle?>>()
        private val done = CountDownLatch(1)

        override fun onProgress(taskId: Int, progress: Bundle?) {
            progress?.let { this.progress += roundTrip(it) }
        }

        override fun onComplete(taskId: Int, result: Bundle?) = finish(result?.let(::roundTrip) to null)

        override fun onError(taskId: Int, error: Bundle?) = finish(null to error?.let(::roundTrip))

        private fun finish(value: Pair<Bundle?, Bundle?>) {
            check(outcome.compareAndSet(null, value)) { "a second terminal callback" }
            done.countDown()
        }

        fun awaitAny(): Pair<Bundle?, Bundle?> {
            assertTrue("the task did not finish within $TASK_TIMEOUT_SECONDS s", done.await(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            return outcome.get()
        }

        fun awaitResult(): Bundle {
            val (result, error) = awaitAny()
            error?.let { fail("${it.getString(MediaStudioContract.KEY_ERROR_CODE)}: ${it.getString(MediaStudioContract.KEY_ERROR_MESSAGE)}") }
            return requireNotNull(result).also {
                assertEquals(ThreeSlateStudioContract.CONTRACT_VERSION, it.getInt(MediaStudioContract.KEY_CONTRACT_VERSION))
            }
        }

        fun awaitError(): Bundle = requireNotNull(awaitAny().second) { "the task completed" }

        private fun roundTrip(bundle: Bundle): Bundle {
            val parcel = Parcel.obtain()
            try {
                parcel.writeBundle(bundle)
                parcel.setDataPosition(0)
                return requireNotNull(parcel.readBundle(Bundle::class.java.classLoader))
            } finally {
                parcel.recycle()
            }
        }
    }

    private fun run(plugin: IMediaStudioPlugin, request: Bundle): Bundle {
        val callback = RecordingCallback()
        assertTrue(plugin.submit(request, callback) > 0)
        return callback.awaitResult()
    }

    private fun request(op: String, inputs: List<File>, output: File, options: Bundle? = null, token: String = HOST_TOKEN) = Bundle().apply {
        putString(MediaStudioRequestKeys.OP, op)
        putStringArray(MediaStudioRequestKeys.INPUTS, inputs.map { it.path }.toTypedArray())
        putString(MediaStudioRequestKeys.OUTPUT, output.path)
        options?.let { putBundle(MediaStudioRequestKeys.OPTIONS, it) }
        putString(MediaStudioRequestKeys.HOST_TOKEN, token)
    }

    private fun options(vararg entries: Pair<String, Any>): Bundle = Bundle().apply {
        entries.forEach { (key, value) ->
            when (value) {
                is Int -> putInt(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                is Bundle -> putBundle(key, value)
                else -> error("unsupported option $key = $value")
            }
        }
    }

    private fun output(name: String): File = File(outputs, name)

    private fun probe(plugin: IMediaStudioPlugin, file: File): Bundle =
        plugin.probe(Bundle().apply { putString(MediaStudioRequestKeys.PATH, file.path) }).also {
            assertNull(it.getString(MediaStudioContract.KEY_ERROR_MESSAGE), it.getString(MediaStudioContract.KEY_ERROR_CODE))
        }

    private fun listTasks(plugin: IMediaStudioPlugin, token: String = HOST_TOKEN): List<Bundle> {
        val answer = plugin.listTasks(Bundle().apply { putString(MediaStudioRequestKeys.HOST_TOKEN, token) })
        answer.classLoader = Bundle::class.java.classLoader
        @Suppress("DEPRECATION")
        return requireNotNull(answer.getParcelableArray(MediaStudioResultKeys.TASKS)).map { it as Bundle }
    }

    /** The tone of a video: its audio extracted to WAV by the plugin, then measured. zh-CN: 视频的音调: 由插件提取为 WAV 后测量. */
    private fun toneOf(plugin: IMediaStudioPlugin, video: File, name: String): Double {
        run(plugin, request(MediaStudioOps.EXTRACT_AUDIO, listOf(video), output(name)))
        return frequency(output(name))
    }

    /**
     * The frequency of a 16 bit WAV tone from the upward zero crossings of its first channel, skipping the first and last
     * fifth.
     * zh-CN: 由第一声道的上行过零点计算 16 位 WAV 音调的频率, 跳过首尾各五分之一.
     */
    private fun frequency(wav: File): Double {
        val bytes = ByteBuffer.wrap(wav.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1, bytes.getShort(20).toInt())
        val channels = bytes.getShort(22).toInt()
        val rate = bytes.getInt(24)
        assertEquals(16, bytes.getShort(34).toInt())
        val frames = (bytes.capacity() - WAV_HEADER) / (2 * channels)
        val from = frames / 5
        val to = frames * 4 / 5
        var crossings = 0
        var previous = bytes.getShort(WAV_HEADER + from * 2 * channels)
        for (frame in from + 1 until to) {
            val sample = bytes.getShort(WAV_HEADER + frame * 2 * channels)
            if (previous < 0 && sample >= 0) crossings++
            previous = sample
        }
        return crossings * rate.toDouble() / (to - from)
    }

    private fun frame(video: File): Bitmap {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.path)
            return requireNotNull(retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)) { "no frame in $video" }
        } finally {
            retriever.release()
        }
    }

    private fun assertColor(message: String, expected: Int, actual: Int) {
        val channels = listOf(
            Color.red(actual) to (expected shr 16 and 0xFF),
            Color.green(actual) to (expected shr 8 and 0xFF),
            Color.blue(actual) to (expected and 0xFF),
        )
        assertTrue("$message: ${Integer.toHexString(actual)} vs ${Integer.toHexString(expected)}", channels.all { (a, e) -> abs(a - e) <= 64 })
    }

    private fun assertDuration(expectedMillis: Long, bundle: Bundle, tolerance: Long = 100) {
        val actual = bundle.getLong(MediaStudioResultKeys.DURATION_MILLIS)
        assertTrue("duration $actual ms, expected $expectedMillis +- $tolerance", abs(actual - expectedMillis) <= tolerance)
    }

    /**
     * A cancelled job that is still running removes its part file once the export stops, shortly after the callback.
     * zh-CN: 仍在运行的已取消作业在导出停止后才删除临时文件, 略晚于回调.
     */
    private fun assertNoPartFiles() {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        var parts: List<File>
        do {
            parts = outputs.listFiles().orEmpty().filter { ".part-" in it.name }
            if (parts.isEmpty()) return
            Thread.sleep(100)
        } while (System.nanoTime() < deadline)
        assertEquals("unfinished part files: $parts", emptyList<File>(), parts)
    }

    private fun <T> withMediaStudio(block: (IMediaStudioPlugin) -> T): T {
        val reference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                reference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val intent = Intent().setComponent(ComponentName(context, ThreeSlateStudioService::class.java))
        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out binding the media studio", connected.await(10, TimeUnit.SECONDS))
            return block(IMediaStudioPlugin.Stub.asInterface(MarshallingBinder(reference.get())))
        } finally {
            context.unbindService(connection)
        }
    }

    // endregion

    companion object {

        private const val HOST_TOKEN = "operations-test"
        private const val TASK_TIMEOUT_SECONDS = 120L
        private const val WAV_HEADER = 44

        private lateinit var directory: File
        private lateinit var outputs: File
        private lateinit var fixtures: MediaFixtures.Fixtures

        @BeforeClass
        @JvmStatic
        fun generateFixtures() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            directory = File(context.cacheDir, "media-operations").apply { deleteRecursively() }
            outputs = File(directory, "outputs").apply { check(mkdirs()) { "cannot create $this" } }
            fixtures = MediaFixtures.generate(File(directory, "inputs"))
        }

        @AfterClass
        @JvmStatic
        fun removeFixtures() {
            if (InstrumentationRegistry.getArguments().getString("keepMediaFixtures") != "true") directory.deleteRecursively()
        }
    }

}

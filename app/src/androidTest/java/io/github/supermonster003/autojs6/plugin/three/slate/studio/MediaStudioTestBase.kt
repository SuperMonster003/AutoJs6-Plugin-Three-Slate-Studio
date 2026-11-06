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
 * The shared part of the operation tests: the Binder connection through [MarshallingBinder], the generated fixtures
 * (roadmap P0.3) in the cache folder of the plugin, request and option Bundles, the callback recorder and the checks on
 * decoded frames and durations. Every subclass gets fresh fixtures and outputs.
 * zh-CN: 各操作测试的共用部分: 经 [MarshallingBinder] 的 Binder 连接, 插件缓存目录中生成的夹具 (路线图 P0.3), 请求与选项
 * Bundle, 回调记录器, 以及对解码帧与时长的检查. 每个子类都使用新生成的夹具与输出目录.
 */
abstract class MediaStudioTestBase {

    protected val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    // region Helpers

    /** Collects the callbacks of one task; Bundles take a Parcel round trip first. zh-CN: 收集一个任务的回调; Bundle 先经 Parcel 往返. */
    protected class RecordingCallback : IMediaStudioCallback.Stub() {

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

    protected fun run(plugin: IMediaStudioPlugin, request: Bundle): Bundle {
        val callback = RecordingCallback()
        assertTrue(plugin.submit(request, callback) > 0)
        return callback.awaitResult()
    }

    protected fun request(op: String, inputs: List<File>, output: File, options: Bundle? = null, token: String = HOST_TOKEN) = Bundle().apply {
        putString(MediaStudioRequestKeys.OP, op)
        putStringArray(MediaStudioRequestKeys.INPUTS, inputs.map { it.path }.toTypedArray())
        putString(MediaStudioRequestKeys.OUTPUT, output.path)
        options?.let { putBundle(MediaStudioRequestKeys.OPTIONS, it) }
        putString(MediaStudioRequestKeys.HOST_TOKEN, token)
    }

    protected fun options(vararg entries: Pair<String, Any>): Bundle = Bundle().apply {
        entries.forEach { (key, value) ->
            when (value) {
                is Int -> putInt(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                is Long -> putLong(key, value)
                is Bundle -> putBundle(key, value)
                is DoubleArray -> putDoubleArray(key, value)
                is List<*> -> putParcelableArrayList(key, ArrayList(value.map { it as Bundle }))
                else -> error("unsupported option $key = $value")
            }
        }
    }

    protected fun output(name: String): File = File(outputs, name)

    protected fun probe(plugin: IMediaStudioPlugin, file: File): Bundle =
        plugin.probe(Bundle().apply { putString(MediaStudioRequestKeys.PATH, file.path) }).also {
            assertNull(it.getString(MediaStudioContract.KEY_ERROR_MESSAGE), it.getString(MediaStudioContract.KEY_ERROR_CODE))
        }

    protected fun listTasks(plugin: IMediaStudioPlugin, token: String = HOST_TOKEN): List<Bundle> {
        val answer = plugin.listTasks(Bundle().apply { putString(MediaStudioRequestKeys.HOST_TOKEN, token) })
        answer.classLoader = Bundle::class.java.classLoader
        @Suppress("DEPRECATION")
        return requireNotNull(answer.getParcelableArray(MediaStudioResultKeys.TASKS)).map { it as Bundle }
    }

    /** The tone of a video: its audio extracted to WAV by the plugin, then measured. zh-CN: 视频的音调: 由插件提取为 WAV 后测量. */
    protected fun toneOf(plugin: IMediaStudioPlugin, video: File, name: String): Double {
        run(plugin, request(MediaStudioOps.EXTRACT_AUDIO, listOf(video), output(name)))
        return frequency(output(name))
    }

    /**
     * The frequency of a 16 bit WAV tone from the upward zero crossings of its first channel, skipping the first and last
     * fifth.
     * zh-CN: 由第一声道的上行过零点计算 16 位 WAV 音调的频率, 跳过首尾各五分之一.
     */
    protected fun frequency(wav: File): Double {
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

    protected fun frame(video: File, atMillis: Long = 1_000): Bitmap {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.path)
            return requireNotNull(retriever.getFrameAtTime(atMillis * 1_000, MediaMetadataRetriever.OPTION_CLOSEST)) { "no frame in $video at $atMillis ms" }
        } finally {
            retriever.release()
        }
    }

    protected fun assertColor(message: String, expected: Int, actual: Int, tolerance: Int = 64) {
        val channels = listOf(
            Color.red(actual) to (expected shr 16 and 0xFF),
            Color.green(actual) to (expected shr 8 and 0xFF),
            Color.blue(actual) to (expected and 0xFF),
        )
        assertTrue("$message: ${Integer.toHexString(actual)} vs ${Integer.toHexString(expected)}", channels.all { (a, e) -> abs(a - e) <= tolerance })
    }

    protected fun assertDuration(expectedMillis: Long, bundle: Bundle, tolerance: Long = 100) {
        val actual = bundle.getLong(MediaStudioResultKeys.DURATION_MILLIS)
        assertTrue("duration $actual ms, expected $expectedMillis +- $tolerance", abs(actual - expectedMillis) <= tolerance)
    }

    /**
     * A cancelled job that is still running removes its part file once the export stops, shortly after the callback.
     * zh-CN: 仍在运行的已取消作业在导出停止后才删除临时文件, 略晚于回调.
     */
    protected fun assertNoPartFiles() {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        var parts: List<File>
        do {
            parts = outputs.listFiles().orEmpty().filter { ".part-" in it.name }
            if (parts.isEmpty()) return
            Thread.sleep(100)
        } while (System.nanoTime() < deadline)
        assertEquals("unfinished part files: $parts", emptyList<File>(), parts)
    }

    protected fun <T> withMediaStudio(block: (IMediaStudioPlugin) -> T): T {
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

        internal const val HOST_TOKEN = "operations-test"
        internal const val TASK_TIMEOUT_SECONDS = 120L
        internal const val WAV_HEADER = 44

        internal lateinit var directory: File
        internal lateinit var outputs: File
        internal lateinit var fixtures: MediaFixtures.Fixtures

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

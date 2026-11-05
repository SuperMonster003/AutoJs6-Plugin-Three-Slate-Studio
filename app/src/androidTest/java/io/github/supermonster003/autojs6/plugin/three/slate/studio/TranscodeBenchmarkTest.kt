package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures.BenchmarkSource
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.ProcessingForegroundService
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.ThreeSlateStudioService
import org.autojs.plugin.mediastudio.api.IMediaStudioCallback
import org.autojs.plugin.mediastudio.api.IMediaStudioPlugin
import org.autojs.plugin.mediastudio.api.MediaStudioCapabilityKeys
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P2.2 appendix F: transcodes a 1080p 60 second clip to H.264 and, when the device has an encoder, H.265 with
 * the default bit rates, and reports the wall time, the video bit rate of the output and whether the processing
 * foreground service ran; a concat of three copies then checks the foreground service of a task longer than 10 s and
 * that the service stops after the cancel. Runs only with the instrumentation argument `benchmark=true`
 * (`.python/benchmark_transcode.py`); `connectedDebugAndroidTest` skips it. Each result is sent as an instrumentation
 * status with the key `benchmark`.
 * zh-CN: 路线图 P2.2 附录 F: 以默认码率把 1080p 60 秒片段转码为 H.264, 设备有编码器时再转 H.265, 报告耗时, 输出的视频码率,
 * 以及处理前台服务是否运行; 然后用三份拼接检查超过 10 秒的任务的前台服务, 以及取消后服务是否停止. 仅在仪器参数
 * `benchmark=true` 时运行 (`.python/benchmark_transcode.py`), `connectedDebugAndroidTest` 会跳过. 每项结果以键 `benchmark`
 * 的仪器状态发送.
 */
@RunWith(AndroidJUnit4::class)
class TranscodeBenchmarkTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun transcode1080pSixtySeconds() {
        assumeTrue("set the instrumentation argument benchmark=true", InstrumentationRegistry.getArguments().getString("benchmark") == "true")
        val directory = File(context.cacheDir, "benchmark").apply { deleteRecursively() }
        try {
            val generated = SystemClock.elapsedRealtime()
            val source = BenchmarkSource.generate(File(directory, "inputs"))
            report("source", "generatedMillis" to SystemClock.elapsedRealtime() - generated, "videoBitRate" to videoBitRate(source), "sizeBytes" to source.length())
            withMediaStudio { plugin ->
                val capabilities = plugin.capabilities
                val encoders = capabilities.getStringArray(MediaStudioCapabilityKeys.VIDEO_ENCODERS).orEmpty().toList()
                report("encoders", "video" to encoders.joinToString("|"), "warnings" to capabilities.getStringArray(MediaStudioCapabilityKeys.WARNINGS).orEmpty().joinToString(" | "))
                listOf("h264", "h265").filter { it in encoders }.forEach { codec ->
                    val output = File(directory, "transcode-$codec.mp4")
                    val callback = Callback()
                    val started = SystemClock.elapsedRealtime()
                    submit(plugin, MediaStudioOps.TRANSCODE, arrayOf(source.path), output, Bundle().apply { putString("codec", codec) }, callback)
                    // The task moves to the foreground service after 10 s. zh-CN: 任务运行 10 秒后转入前台服务.
                    val foreground = callback.foregroundAfter(FOREGROUND_CHECK_MILLIS)
                    val (result, error) = callback.await()
                    val elapsed = SystemClock.elapsedRealtime() - started
                    if (error != null) {
                        report(codec, "pass" to false, "error" to "${error.getString(MediaStudioContract.KEY_ERROR_CODE)}: ${error.getString(MediaStudioContract.KEY_ERROR_MESSAGE)}")
                        return@forEach
                    }
                    requireNotNull(result)
                    report(
                        codec,
                        "pass" to (result.getString(MediaStudioResultKeys.CODEC) == codec),
                        "millis" to elapsed,
                        "videoBitRate" to videoBitRate(output),
                        "sizeBytes" to output.length(),
                        "durationMillis" to result.getLong(MediaStudioResultKeys.DURATION_MILLIS),
                        "size" to "${result.getInt(MediaStudioResultKeys.WIDTH)}x${result.getInt(MediaStudioResultKeys.HEIGHT)}",
                        "foregroundService" to foreground,
                        "warnings" to result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().joinToString(" | "),
                    )
                    output.delete()
                }
                // A fast device transcodes the clip in less than 10 s, so a task three times as long checks the
                // foreground service, is cancelled after the check and the service has to stop with the empty queue.
                // zh-CN: 快的设备不到 10 秒就转完, 所以另用三倍长的任务检查前台服务, 检查后取消, 队列空后服务须自行停止.
                val callback = Callback()
                val taskId = submit(plugin, MediaStudioOps.CONCAT, Array(3) { source.path }, File(directory, "foreground.mp4"), Bundle(), callback)
                val state = callback.foregroundAfter(FOREGROUND_CHECK_MILLIS)
                plugin.cancel(taskId)
                val (_, error) = callback.await()
                val cancelled = SystemClock.elapsedRealtime()
                while (serviceState() != null && SystemClock.elapsedRealtime() - cancelled < STOP_TIMEOUT_MILLIS) SystemClock.sleep(100)
                val stopped = serviceState()?.let { "still $it" } ?: "${SystemClock.elapsedRealtime() - cancelled} ms"
                report("foreground", "state" to state, "cancelled" to error?.getString(MediaStudioContract.KEY_ERROR_CODE).toString(), "stoppedAfterCancel" to stopped, "restricted" to backgroundRestricted())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun submit(plugin: IMediaStudioPlugin, op: String, inputs: Array<String>, output: File, options: Bundle, callback: Callback): Int = plugin.submit(
        Bundle().apply {
            putString(MediaStudioRequestKeys.OP, op)
            putStringArray(MediaStudioRequestKeys.INPUTS, inputs)
            putString(MediaStudioRequestKeys.OUTPUT, output.path)
            putBundle(MediaStudioRequestKeys.OPTIONS, options)
            putString(MediaStudioRequestKeys.HOST_TOKEN, "benchmark")
        },
        callback,
    )

    /**
     * Whether the system restricts the plugin in the background; it then ignores startForeground() (HyperOS default).
     * zh-CN: 系统是否限制插件后台; 受限时系统忽略 startForeground() (HyperOS 默认如此).
     */
    private fun backgroundRestricted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted

    /** `foreground`, `running` or `null` when the processing service does not run. zh-CN: 处理服务未运行时为 `null`. */
    private fun serviceState(): String? {
        @Suppress("DEPRECATION")
        val service = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningServices(Int.MAX_VALUE)
            .firstOrNull { it.service.className == ProcessingForegroundService::class.java.name && it.service.packageName == context.packageName }
        return service?.let { if (it.foreground) "foreground" else "running" }
    }

    private inner class Callback : IMediaStudioCallback.Stub() {

        private val outcome = AtomicReference<Pair<Bundle?, Bundle?>>()
        private val done = CountDownLatch(1)

        override fun onProgress(taskId: Int, progress: Bundle?) = Unit

        override fun onComplete(taskId: Int, result: Bundle?) = finish(result to null)

        override fun onError(taskId: Int, error: Bundle?) = finish(null to error)

        private fun finish(value: Pair<Bundle?, Bundle?>) {
            outcome.compareAndSet(null, value)
            done.countDown()
        }

        /**
         * `running`, `foreground`, `not started` or `finished early` [millis] after the submit.
         * zh-CN: 提交 [millis] 毫秒后的状态: `running`, `foreground`, `not started` 或 `finished early`.
         */
        fun foregroundAfter(millis: Long): String {
            if (done.await(millis, TimeUnit.MILLISECONDS)) return "finished early"
            return serviceState() ?: "not started"
        }

        fun await(): Pair<Bundle?, Bundle?> {
            assertTrue("the transcode did not finish within $TASK_TIMEOUT_MINUTES min", done.await(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES))
            return outcome.get()
        }
    }

    /** The bit rate of the video track from its sample sizes. zh-CN: 由样本大小算出的视频轨码率. */
    private fun videoBitRate(file: File): Long {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.path)
            val track = (0 until extractor.trackCount).first { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("video/") }
            extractor.selectTrack(track)
            val buffer = ByteBuffer.allocate(8 shl 20)
            var bytes = 0L
            var first = -1L
            var last = 0L
            while (true) {
                val time = extractor.sampleTime
                if (time < 0) break
                if (first < 0) first = time
                last = maxOf(last, time)
                bytes += if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) extractor.sampleSize else extractor.readSampleData(buffer, 0).toLong()
                extractor.advance()
            }
            val format = extractor.getTrackFormat(track)
            val frameUs = if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) 1_000_000L / format.getInteger(MediaFormat.KEY_FRAME_RATE) else 0L
            return bytes * 8 * 1_000_000L / (last - first + frameUs).coerceAtLeast(1)
        } finally {
            extractor.release()
        }
    }

    private fun report(name: String, vararg values: Pair<String, Any>) {
        // `;` separates the fields, so values (free-text warnings) carry `,` instead. zh-CN: `;` 分隔字段, 因此值 (自由文本警告) 中的 `;` 换成 `,`.
        val line = (listOf("name" to name) + values).joinToString(";") { (key, value) -> "$key=${value.toString().replace(';', ',').replace('\n', ' ')}" }
        InstrumentationRegistry.getInstrumentation().sendStatus(STATUS_CODE, Bundle().apply { putString("benchmark", line) })
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
        assertTrue(context.bindService(Intent().setComponent(ComponentName(context, ThreeSlateStudioService::class.java)), connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue(connected.await(10, TimeUnit.SECONDS))
            val plugin = IMediaStudioPlugin.Stub.asInterface(MarshallingBinder(reference.get()))
            assertEquals(ThreeSlateStudioContract.CONTRACT_VERSION, plugin.capabilities.getInt(MediaStudioContract.KEY_CONTRACT_VERSION))
            return block(plugin)
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val STATUS_CODE = 2
        const val FOREGROUND_CHECK_MILLIS = 13_000L
        const val TASK_TIMEOUT_MINUTES = 15L
        const val STOP_TIMEOUT_MILLIS = 10_000L
    }

}

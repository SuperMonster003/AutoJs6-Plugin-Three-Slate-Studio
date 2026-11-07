package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.record.RecordPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Roadmap P4.5 on an emulator and on devices. [page] and [microphone] run anywhere (the AVD of the gate grants the
 * microphone and all files access through the shell; without the argument `grant=false` they do the same on a device);
 * [deviceRecording] is the device acceptance, driven by `.python/device_record_check.py` and skipped without its
 * arguments: it starts through the page, so the maintainer answers the permission and screen casting dialogs, plays a
 * tone for the playback capture, pauses, turns the screen off when asked, and reports the probe of the saved file.
 * zh-CN: 路线图 P4.5 在模拟器与真机上的测试. [page] 与 [microphone] 在任何设备上运行 (门禁 AVD 经 shell 授予麦克风与所有文件访问;
 * 没有参数 `grant=false` 时在真机上也这样做); [deviceRecording] 是真机验收, 由 `.python/device_record_check.py` 驱动, 没有其参数时
 * 跳过: 它经页面开始录音, 由维护者回答权限与投屏对话框, 为内录播放测试音, 暂停, 按要求熄屏, 并报告保存文件的探测结果.
 */
@RunWith(AndroidJUnit4::class)
class RecordingDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private val arguments: Bundle get() = InstrumentationRegistry.getArguments()
    private val saved = ArrayList<String>()

    @After
    fun cleanUp() {
        runCatching { if (StudioRecorder.isBusy) StudioRecorder.stop() }
        await(10_000) { !StudioRecorder.isBusy }
        StudioRecorder.dismiss()
        saved.forEach(::delete)
    }

    @Test
    fun page() {
        grantIfAllowed()
        openRecordPage().use {
            compose.onNodeWithTag("slate.record.source.microphone").assertExists()
            val playback = Build.VERSION.SDK_INT >= RecordSettings.PLAYBACK_MIN_SDK
            if (playback) {
                compose.onNodeWithTag("slate.record.source.playback").assertExists()
                compose.onNodeWithTag("slate.record.source.mixed").assertExists()
                compose.onNodeWithTag("slate.record.playbackUnavailable").assertDoesNotExist()
            } else {
                compose.onNodeWithTag("slate.record.source.playback").assertDoesNotExist()
                compose.onNodeWithTag("slate.record.source.mixed").assertDoesNotExist()
                compose.onNodeWithTag("slate.record.playbackUnavailable").assertExists()
            }
            compose.onNodeWithTag("slate.record.format.wav").performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals(AudioOutputFormat.WAV, RecordPreferences.of(context).settings.format)
            compose.onNodeWithTag("slate.record.format.m4a").performScrollTo().performClick()
            compose.onNodeWithTag("slate.record.start").assertExists()
        }
    }

    /**
     * Microphone through the page: the foreground service starts, pause / resume leave the pause out of the duration,
     * the file lands in `Music/3-Slate Studio/` and probes as mono 48 kHz AAC.
     * zh-CN: 经页面录制麦克风: 前台服务启动, 暂停 / 继续后时长不含暂停, 文件落在 `Music/3-Slate Studio/`, 探测为单声道 48 kHz AAC.
     */
    @Test
    fun microphone() {
        assumeTrue("run by the device driver instead", arguments.getString(ARG_SOURCE) == null)
        grantIfAllowed()
        RecordPreferences.of(context).apply {
            settings = RecordSettings()
            notificationsAsked = true
        }
        openRecordPage().use {
            compose.onNodeWithTag("slate.record.start").performClick()
            assertTrue("recording started", await(START_TIMEOUT_MILLIS) { StudioRecorder.phase.value is StudioRecorder.Phase.Active })
            assertTrue("the recording service is in the foreground", serviceInForeground())
            SystemClock.sleep(2_000)
            compose.onNodeWithTag("slate.record.pause").performClick()
            compose.waitForIdle()
            assertEquals(RecordingStates.PAUSED, StudioRecorder.status()?.state)
            SystemClock.sleep(1_000)
            compose.onNodeWithTag("slate.record.resume").performClick()
            SystemClock.sleep(1_000)
            compose.onNodeWithTag("slate.record.stop").performClick()
            assertTrue("recording saved", await(SAVE_TIMEOUT_MILLIS) { StudioRecorder.phase.value is StudioRecorder.Phase.Finished })
            val result = (StudioRecorder.phase.value as StudioRecorder.Phase.Finished).result
            saved += result.path
            assertTrue(result.path, result.path.startsWith(File(Environment.getExternalStorageDirectory(), "Music/3-Slate Studio/slate-record-").path))
            assertTrue(result.path, result.path.endsWith(".m4a"))
            assertTrue("recorded ${result.durationMillis} ms", result.durationMillis in 2_600L..3_700L)
            assertTrue("elapsed ${result.elapsedMillis} ms includes the pause", result.elapsedMillis - result.durationMillis >= 700)
            val info = MediaProbe.probe(result.path)
            assertTrue("probe ${info.durationMillis} ms", abs(info.durationMillis - result.durationMillis) <= 300)
            assertEquals(48_000, info.sampleRate)
            assertEquals(1, info.channels)
            assertEquals("aac", info.audioCodec)
            compose.onNodeWithTag("slate.record.code").performScrollTo().assertExists()
            assertTrue("the service ended", await(5_000) { !serviceRunning() })
        }
    }

    /**
     * Device acceptance (arguments of `.python/device_record_check.py`): `recordSource` (microphone / playback / mixed),
     * `recordFormat`, `recordMillis` before the pause, `pauseMillis`, `screenOffMillis` (0: keep the screen on) and
     * `afterMillis` after it; the numbers go to the instrumentation status for the driver.
     * zh-CN: 真机验收 (`.python/device_record_check.py` 的参数): `recordSource` (microphone / playback / mixed), `recordFormat`,
     * 暂停前的 `recordMillis`, `pauseMillis`, `screenOffMillis` (0: 不熄屏) 与之后的 `afterMillis`; 数值写入 instrumentation 状态供驱动读取.
     */
    @Test
    fun deviceRecording() {
        val source = RecordingAudioSource.of(arguments.getString(ARG_SOURCE))
        assumeTrue("run by .python/device_record_check.py", source != null)
        grantIfAllowed()
        val format = AudioOutputFormat.of(arguments.getString(ARG_FORMAT)) ?: AudioOutputFormat.M4A
        val recordMillis = arguments.getString(ARG_RECORD_MILLIS)?.toLong() ?: 3_000L
        val pauseMillis = arguments.getString(ARG_PAUSE_MILLIS)?.toLong() ?: 1_000L
        val screenOffMillis = arguments.getString(ARG_SCREEN_OFF_MILLIS)?.toLong() ?: 0L
        val afterMillis = arguments.getString(ARG_AFTER_MILLIS)?.toLong() ?: 2_000L
        RecordPreferences.of(context).settings = RecordSettings(source = source!!, format = format)
        val tone = if (source.usesPlayback) Tone().also { it.start() } else null
        try {
            openRecordPage().use {
                compose.onNodeWithTag("slate.record.source.${source.key}").performScrollTo().performClick()
                compose.onNodeWithTag("slate.record.start").performScrollTo().performClick()
                // The maintainer answers the microphone, notification and screen casting dialogs meanwhile.
                // zh-CN: 维护者在此期间回答麦克风, 通知与投屏对话框.
                assertTrue("recording started", await(DIALOG_TIMEOUT_MILLIS) { StudioRecorder.phase.value is StudioRecorder.Phase.Active })
                var peak = 0f
                // The page ticks on the compose test clock, which moves only when the test advances it, so the hold advances
                // it along with the real time: the page then shows the live time and level to the maintainer and in the
                // screenshots.
                // zh-CN: 页面按 compose 测试时钟刷新, 而该时钟只在测试推进时前进, 所以等待期间随真实时间推进它: 维护者与截图看到的是
                // 实时时长与电平.
                fun hold(millis: Long) {
                    val end = SystemClock.elapsedRealtime() + millis
                    while (SystemClock.elapsedRealtime() < end) {
                        peak = maxOf(peak, StudioRecorder.level())
                        SystemClock.sleep(100)
                        runCatching { compose.mainClock.advanceTimeBy(100) }
                    }
                }
                val foreground = serviceInForeground()
                hold(LEAD_MILLIS)
                event("active")
                hold(recordMillis)
                compose.onNodeWithTag("slate.record.pause").performClick()
                compose.waitForIdle()
                event("paused")
                hold(pauseMillis)
                compose.onNodeWithTag("slate.record.resume").performClick()
                if (screenOffMillis > 0) {
                    shell("input keyevent KEYCODE_SLEEP")
                    hold(screenOffMillis)
                    shell("input keyevent KEYCODE_WAKEUP")
                }
                hold(afterMillis)
                val status = StudioRecorder.status()
                StudioRecorder.stop()
                assertTrue("recording saved", await(SAVE_TIMEOUT_MILLIS) { StudioRecorder.phase.value is StudioRecorder.Phase.Finished })
                val result = (StudioRecorder.phase.value as StudioRecorder.Phase.Finished).result
                saved += result.path
                val info = MediaProbe.probe(result.path)
                val report = Bundle().apply {
                    putString("record.source", source.key)
                    putString("record.path", result.path)
                    putString("record.recordedMillis", result.durationMillis.toString())
                    putString("record.elapsedMillis", result.elapsedMillis.toString())
                    putString("record.expectedMillis", (LEAD_MILLIS + recordMillis + screenOffMillis + afterMillis).toString())
                    putString("record.probeMillis", info.durationMillis.toString())
                    putString("record.sampleRate", info.sampleRate.toString())
                    putString("record.channels", info.channels.toString())
                    putString("record.codec", info.audioCodec.toString())
                    putString("record.sizeBytes", info.sizeBytes.toString())
                    putString("record.peak", String.format(java.util.Locale.ROOT, "%.3f", peak))
                    putString("record.foreground", foreground.toString())
                    putString("record.stateBeforeStop", status?.state.toString())
                }
                instrumentation.sendStatus(0, report)
                runCatching { compose.waitForIdle() }
                event("saved")
                // Time for the driver's screenshot of the saved recording. zh-CN: 留给驱动截取已保存录音的画面.
                SystemClock.sleep(2_000)
                assertTrue("foreground service", foreground)
                assertNotNull(info.sampleRate)
                val expected = LEAD_MILLIS + recordMillis + screenOffMillis + afterMillis
                assertTrue("recorded ${result.durationMillis} ms, expected about $expected ms", abs(result.durationMillis - expected) <= 900)
                assertTrue("probe ${info.durationMillis} ms", abs(info.durationMillis - result.durationMillis) <= 300)
            }
        } finally {
            tone?.stop()
        }
    }

    /** A progress event for the driver (screenshots). zh-CN: 给驱动的进度事件 (截图). */
    private fun event(name: String) = instrumentation.sendStatus(STATUS_EVENT, Bundle().apply { putString("record.event", name) })

    private fun openRecordPage(): ActivityScenario<HomeActivity> {
        val scenario = ActivityScenario.launch(HomeActivity::class.java)
        compose.onNodeWithTag("slate.home.record").performClick()
        compose.onNodeWithTag("slate.record").assertExists()
        return scenario
    }

    /** Shell grants on the AVD (and on a device unless `grant=false`). zh-CN: 在 AVD 上 (真机除非 `grant=false`) 经 shell 授权. */
    private fun grantIfAllowed() {
        if (arguments.getString(ARG_GRANT) == "false") return
        val pkg = context.packageName
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) shell("pm grant $pkg ${Manifest.permission.RECORD_AUDIO}")
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        if (Build.VERSION.SDK_INT >= 30) shell("appops set $pkg MANAGE_EXTERNAL_STORAGE allow")
        else {
            shell("pm grant $pkg ${Manifest.permission.READ_EXTERNAL_STORAGE}")
            shell("pm grant $pkg ${Manifest.permission.WRITE_EXTERNAL_STORAGE}")
        }
        RecordPreferences.of(context).notificationsAsked = true
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).close()
        SystemClock.sleep(200)
    }

    @Suppress("DEPRECATION")
    private fun serviceRecord(): ActivityManager.RunningServiceInfo? = context.getSystemService(ActivityManager::class.java)
        ?.getRunningServices(Int.MAX_VALUE)?.firstOrNull { it.service.className == RecordingService::class.java.name }

    private fun serviceRunning(): Boolean = serviceRecord() != null

    private fun serviceInForeground(): Boolean = await(3_000) { serviceRecord()?.foreground == true }

    private fun await(timeoutMillis: Long, predicate: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (predicate()) return true
            SystemClock.sleep(100)
        }
        return predicate()
    }

    /** Deletes a test recording and its MediaStore row. zh-CN: 删除测试录音及其 MediaStore 记录. */
    private fun delete(path: String) {
        @Suppress("DEPRECATION")
        runCatching { context.contentResolver.delete(MediaStore.Files.getContentUri("external"), "${MediaStore.MediaColumns.DATA}=?", arrayOf(path)) }
        File(path).delete()
    }

    /** A 440 Hz tone of the test app for the playback capture. zh-CN: 测试应用播放的 440 Hz 测试音, 供内录捕获. */
    private class Tone {
        @Volatile private var running = true
        private val thread = Thread {
            val rate = 48_000
            val size = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(size)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            val block = ShortArray(1024)
            var phase = 0.0
            try {
                track.play()
                while (running) {
                    for (i in block.indices) {
                        block[i] = (sin(phase) * 0.3 * Short.MAX_VALUE).toInt().toShort()
                        phase += 2 * PI * 440 / rate
                    }
                    track.write(block, 0, block.size)
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }

        fun start() = thread.start()

        fun stop() {
            running = false
            thread.join(2_000)
        }
    }

    private companion object {
        const val ARG_GRANT = "grant"
        const val ARG_SOURCE = "recordSource"
        const val ARG_FORMAT = "recordFormat"
        const val ARG_RECORD_MILLIS = "recordMillis"
        const val ARG_PAUSE_MILLIS = "pauseMillis"
        const val ARG_SCREEN_OFF_MILLIS = "screenOffMillis"
        const val ARG_AFTER_MILLIS = "afterMillis"
        const val START_TIMEOUT_MILLIS = 15_000L
        const val SAVE_TIMEOUT_MILLIS = 15_000L
        const val DIALOG_TIMEOUT_MILLIS = 180_000L
        const val STATUS_EVENT = 100

        /** Recording before the first screenshot. zh-CN: 第一张截图之前的录音时长. */
        const val LEAD_MILLIS = 500L
    }
}

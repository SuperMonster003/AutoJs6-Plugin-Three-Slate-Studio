package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStoreRegistrar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executors

/**
 * The one Studio recording of the plugin process (roadmap P4.5, D41). The page calls [start] with the consent of the
 * media projection when the source needs it; [RecordingService] enters the foreground and calls [begin], which starts the
 * kernel on the worker thread. The kernel's stop listener registers the saved file in MediaStore and ends the service.
 * [phase] drives the page; [status] and [level] are read while recording.
 * zh-CN: 插件进程中唯一的 Studio 录音 (路线图 P4.5, D41). 页面调用 [start], 音源需要时附带媒体投影授权; [RecordingService]
 * 进入前台后调用 [begin], 在工作线程上启动内核. 内核的停止回调把保存的文件登记到 MediaStore 并结束服务. [phase] 驱动页面;
 * 录音期间读取 [status] 与 [level].
 */
internal object StudioRecorder {

    /** Consent of the media projection (the activity result). zh-CN: 媒体投影授权 (Activity 结果). */
    class ProjectionGrant(val resultCode: Int, val data: Intent)

    sealed interface Phase {
        data object Idle : Phase
        data class Starting(val settings: RecordSettings, val output: String) : Phase
        data class Active(val settings: RecordSettings, val output: String) : Phase
        data class Saving(val settings: RecordSettings, val output: String) : Phase
        data class Finished(val settings: RecordSettings, val result: AudioRecordingResult) : Phase
        data class Failed(val settings: RecordSettings?, val message: String) : Phase
    }

    private const val TAG = "SlateStudioRecorder"

    private val worker = Executors.newSingleThreadExecutor { Thread(it, "SlateStudioRecorder") }
    private val state = MutableStateFlow<Phase>(Phase.Idle)
    private val lock = Any()
    // The recorder holds the application context only (see begin). zh-CN: 录音器只持有应用上下文 (见 begin).
    @SuppressLint("StaticFieldLeak")
    private var recorder: AudioRecorder? = null
    private var pending: ProjectionGrant? = null

    val phase: StateFlow<Phase> = state.asStateFlow()

    val isBusy: Boolean get() = state.value.let { it is Phase.Starting || it is Phase.Active || it is Phase.Saving }

    /**
     * Starts the foreground service for [settings] into [output]; false when a recording is already in progress.
     * zh-CN: 为 [settings] 启动前台服务, 输出到 [output]; 已有录音进行中时返回 false.
     */
    fun start(context: Context, settings: RecordSettings, output: String, grant: ProjectionGrant?): Boolean {
        synchronized(lock) {
            if (isBusy) return false
            pending = grant
            state.value = Phase.Starting(settings, output)
        }
        if (!RecordingService.start(context, settings.source.usesPlayback)) {
            fail(settings, IllegalStateException("Android did not start the recording service."))
        }
        return true
    }

    /** The projection consent of the starting recording, handed out once. zh-CN: 正在启动的录音的投影授权, 只交出一次. */
    fun takeGrant(): ProjectionGrant? = synchronized(lock) { pending.also { pending = null } }

    /**
     * Called by [RecordingService] in the foreground: starts the kernel with [projection] (null for the microphone).
     * zh-CN: 由已在前台的 [RecordingService] 调用: 以 [projection] (麦克风时为 null) 启动内核.
     */
    fun begin(context: Context, projection: MediaProjection?) {
        val starting = state.value as? Phase.Starting ?: return
        val application = context.applicationContext
        worker.execute {
            try {
                val created = AudioRecorder(application, File(starting.output), starting.settings.options(), projection, null) { _, result, error ->
                    finished(application, starting.settings, result, error)
                }
                synchronized(lock) { recorder = created }
                created.start()
                state.compareAndSet(starting, Phase.Active(starting.settings, starting.output))
            } catch (error: Throwable) {
                synchronized(lock) { recorder = null }
                fail(starting.settings, error)
                RecordingService.finishRunning()
            }
        }
    }

    /** Saves the recording (also from the notification). zh-CN: 保存录音 (通知中的停止也调用它). */
    fun stop() {
        val active = state.value as? Phase.Active ?: return
        val current = synchronized(lock) { recorder } ?: return
        state.compareAndSet(active, Phase.Saving(active.settings, active.output))
        worker.execute { runCatching { current.stop() }.onFailure { Log.w(TAG, "stop failed", it) } }
    }

    fun pause(): Boolean = synchronized(lock) { recorder }?.pause() == true

    fun resume(): Boolean = synchronized(lock) { recorder }?.resume() == true

    fun status(): AudioRecordingStatus? = synchronized(lock) { recorder }?.status()

    fun level(): Float = synchronized(lock) { recorder }?.level() ?: 0f

    /** Back to idle after a finished or failed recording. zh-CN: 录音结束或失败后回到空闲. */
    fun dismiss() {
        val current = state.value
        if (current is Phase.Finished || current is Phase.Failed) state.compareAndSet(current, Phase.Idle)
    }

    /**
     * Ends a recording the system stopped (the projection was revoked, the service destroyed): saves what was recorded.
     * zh-CN: 结束被系统停止的录音 (投影被撤销, 服务被销毁): 保存已录制的内容.
     */
    fun stopFromSystem() {
        val current = synchronized(lock) { recorder } ?: return
        val active = state.value
        if (active is Phase.Active) state.compareAndSet(active, Phase.Saving(active.settings, active.output))
        worker.execute { runCatching { current.stop() } }
    }

    /** A start that failed before the kernel ran (service refused, projection missing). zh-CN: 内核运行之前失败的启动 (服务被拒, 缺少投影). */
    fun fail(settings: RecordSettings?, error: Throwable) {
        Log.w(TAG, "recording failed", error)
        synchronized(lock) { pending = null }
        state.value = Phase.Failed(settings, error.message ?: error.javaClass.simpleName)
    }

    private fun finished(context: Context, settings: RecordSettings, result: AudioRecordingResult?, error: Throwable?) {
        synchronized(lock) { recorder = null }
        when {
            result != null -> {
                runCatching { MediaStoreRegistrar.register(context, result.path, result.mimeType) }
                state.value = Phase.Finished(settings, result)
            }
            error != null -> state.value = Phase.Failed(settings, error.message ?: error.javaClass.simpleName)
            else -> state.value = Phase.Idle
        }
        RecordingService.finishRunning()
    }

}

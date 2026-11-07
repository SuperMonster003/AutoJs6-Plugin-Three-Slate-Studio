package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.BundleValues
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.CodecSupport
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.RequestPlanner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleCueFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubmitEnvelope
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.toBundle
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.OperationJob
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.storage.StorageAccessActivity
import org.autojs.plugin.mediastudio.api.IMediaStudioCallback
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioProgressKeys
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * The process-wide media studio (roadmap P2.1, D28): one [TaskRunner] shared by every host connection, the submit
 * checks of [RequestPlanner], `probe()` and `listTasks()`, the callback bundles of the contract, cancellation when a
 * callback Binder dies, the data sync foreground service for tasks running longer than [FOREGROUND_AFTER_MILLIS], and
 * the storage guide on STORAGE_PERMISSION_DENIED.
 * zh-CN: 进程级的媒体工坊 (路线图 P2.1, D28): 全部宿主连接共用一个 [TaskRunner], 负责 [RequestPlanner] 的 submit 检查,
 * `probe()` 与 `listTasks()`, 契约的回调 Bundle, 回调 Binder 死亡时取消任务, 运行超过 [FOREGROUND_AFTER_MILLIS] 的任务所用的
 * dataSync 前台服务, 以及 STORAGE_PERMISSION_DENIED 时的存储引导.
 */
internal class MediaStudioEngine private constructor(private val context: Context) {

    private val threadCount = AtomicInteger()

    private val executor: ExecutorService = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "SlateStudio-task-${threadCount.incrementAndGet()}").apply { isDaemon = true }
    }

    val runner = TaskRunner<Bundle>(executor, SystemClock::elapsedRealtime)

    private val main = Handler(Looper.getMainLooper())

    private val foregroundCheck = Runnable { checkForeground() }

    @Volatile
    private var lastGuideAt: Long? = null

    init {
        runner.addObserver { main.post(foregroundCheck) }
        // No task runs yet: cue files a dead process left behind can go (contract version 2, Q36 (b)).
        // zh-CN: 尚无任务运行: 可以删除已死亡进程留下的 cue 文件 (契约版本 2, Q36 (b)).
        executor.execute { SubtitleCueFiles.clearStale(context.cacheDir) }
    }

    /**
     * Plans and queues one request; throws the Binder exception of the contract (`CODE: detail`) when a check fails.
     * zh-CN: 规划并排入一个请求; 检查失败时抛出契约规定的 Binder 异常 (`CODE: detail`).
     */
    fun submit(request: Bundle, callback: IMediaStudioCallback, callingUid: Int): Int {
        request.classLoader = javaClass.classLoader
        val envelope = SubmitEnvelope(
            op = request.getString(MediaStudioRequestKeys.OP),
            inputs = request.getStringArray(MediaStudioRequestKeys.INPUTS)?.toList().orEmpty(),
            output = request.getString(MediaStudioRequestKeys.OUTPUT),
            options = BundleValues.toMap(request.getBundle(MediaStudioRequestKeys.OPTIONS)),
            hostToken = request.getString(MediaStudioRequestKeys.HOST_TOKEN),
            origin = request.getString(MediaStudioRequestKeys.ORIGIN),
            registerMedia = request.getBoolean(MediaStudioRequestKeys.REGISTER_MEDIA, true),
            timeline = request.getBundle(MediaStudioRequestKeys.TIMELINE)?.let { BundleValues.toMap(it) },
        )
        val planned = try {
            planner().plan(envelope)
        } catch (e: MediaStudioException) {
            if (e.code == MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED) openStorageGuide()
            throw e.toBinderException()
        }
        val tokenKey = tokenKey(envelope.hostToken, callingUid)
        val binder = callback.asBinder()
        // The host died: cancel its tasks and delete their unfinished outputs (contract submit()).
        // zh-CN: 宿主已死亡: 取消其任务并删除未完成的输出 (契约 submit()).
        val recipient = IBinder.DeathRecipient { runner.cancelToken(tokenKey) }
        try {
            binder.linkToDeath(recipient, 0)
        } catch (e: RemoteException) {
            throw IllegalStateException(MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "the callback is no longer alive"), e)
        }
        val listener = CallbackListener(callback) { runCatching { binder.unlinkToDeath(recipient, 0) } }
        return try {
            runner.submit(
                tokenKey = tokenKey,
                op = planned.op,
                output = planned.output.ifEmpty { null },
                origin = planned.origin,
                // subtitlesParse writes nothing and claims no path. zh-CN: subtitlesParse 不写文件, 不占用路径.
                claims = listOfNotNull(planned.output.ifEmpty { null }),
                job = OperationJob(context, planned),
                listener = listener,
            )
        } catch (e: MediaStudioException) {
            runCatching { binder.unlinkToDeath(recipient, 0) }
            throw e.toBinderException()
        }
    }

    fun cancel(taskId: Int): Boolean = runner.cancel(taskId)

    /**
     * `probe` and `audioProbe` (request key OP, absent means `probe`); failures are error bundles, never exceptions.
     * zh-CN: `probe` 与 `audioProbe` (请求键 OP, 缺省为 `probe`); 失败以错误 Bundle 返回, 从不抛异常.
     */
    fun probe(request: Bundle?): Bundle {
        val op = request?.getString(MediaStudioRequestKeys.OP) ?: MediaStudioOps.PROBE
        if (op != MediaStudioOps.PROBE && op != MediaStudioOps.AUDIO_PROBE) {
            return errorBundle(MediaStudioErrorCodes.INVALID_OPTIONS, "probe() answers ${MediaStudioOps.PROBE} and ${MediaStudioOps.AUDIO_PROBE}, got $op")
        }
        val path = request?.getString(MediaStudioRequestKeys.PATH)
        if (path.isNullOrBlank()) return errorBundle(MediaStudioErrorCodes.INVALID_OPTIONS, "path is required")
        if (!path.startsWith("/")) return errorBundle(MediaStudioErrorCodes.INVALID_OPTIONS, "path must be an absolute path, got $path")
        if (StorageAccess.lacksAccess(context, path)) {
            openStorageGuide()
            return errorBundle(MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED, storageDetail(path))
        }
        return try {
            val info = if (op == MediaStudioOps.AUDIO_PROBE) MediaProbe.probeAudio(path) else MediaProbe.probe(path)
            info.toBundle().withContractVersion()
        } catch (e: MediaStudioException) {
            errorBundle(e.code, e.detail)
        } catch (e: RuntimeException) {
            Log.w(TAG, "probe failed", e)
            errorBundle(MediaStudioErrorCodes.INTERNAL_ERROR, listOfNotNull(e.javaClass.simpleName, e.message).joinToString(": "))
        }
    }

    /** Tasks of one host token in submit order. zh-CN: 一个宿主 token 的任务, 按提交顺序. */
    fun listTasks(query: Bundle?, callingUid: Int): Bundle {
        val hostToken = query?.getString(MediaStudioRequestKeys.HOST_TOKEN)
        if (hostToken != null && hostToken.length > MediaStudioLimits.MAX_HOST_TOKEN_CHARS) {
            return errorBundle(MediaStudioErrorCodes.INVALID_OPTIONS, "hostToken longer than ${MediaStudioLimits.MAX_HOST_TOKEN_CHARS} characters")
        }
        val tasks = runner.list(tokenKey(hostToken, callingUid)).map { task ->
            Bundle().apply {
                putInt(MediaStudioResultKeys.TASK_ID, task.taskId)
                putString(MediaStudioResultKeys.TASK_OP, task.op)
                putString(MediaStudioResultKeys.TASK_STATE, task.state)
                putDouble(MediaStudioResultKeys.TASK_PERCENT, task.percent)
                putString(MediaStudioResultKeys.TASK_STAGE, task.stage)
                task.output?.let { putString(MediaStudioResultKeys.TASK_OUTPUT, it) }
                putString(MediaStudioResultKeys.TASK_ORIGIN, task.origin)
                putLong(MediaStudioResultKeys.TASK_SUBMITTED_AT, task.submittedAt)
            }
        }
        return Bundle().apply { putParcelableArray(MediaStudioResultKeys.TASKS, tasks.toTypedArray()) }.withContractVersion()
    }

    /** The running and queued counts of the notification. zh-CN: 通知显示的运行中与排队中个数. */
    data class QueueState(val running: Int, val queued: Int, val percent: Double?) {
        val isIdle: Boolean get() = running == 0 && queued == 0
    }

    fun queueState(): QueueState {
        val tasks = runner.unfinished()
        val running = tasks.filter { it.state == MediaStudioContract.TASK_RUNNING }
        return QueueState(running.size, tasks.size - running.size, running.singleOrNull()?.percent)
    }

    private fun planner() = RequestPlanner(
        storageRoot = sharedStorageRoot(),
        encoders = CodecSupport.encoders(),
        oggMuxer = CodecSupport.oggMuxer,
        lacksStorageAccess = { StorageAccess.lacksAccess(context, it) },
        isClaimed = runner::isClaimed,
    )

    /**
     * Starts the foreground service once a task has run for [FOREGROUND_AFTER_MILLIS], otherwise checks again then; the
     * service stops itself when the queue is empty.
     * zh-CN: 有任务运行满 [FOREGROUND_AFTER_MILLIS] 时启动前台服务, 否则到时再检查; 队列为空时服务自行停止.
     */
    private fun checkForeground() {
        main.removeCallbacks(foregroundCheck)
        val started = runner.unfinished().mapNotNull { it.startedAt }.minOrNull() ?: return
        val delay = started + FOREGROUND_AFTER_MILLIS - SystemClock.elapsedRealtime()
        if (delay > 0) {
            main.postDelayed(foregroundCheck, delay)
        } else if (!ProcessingForegroundService.isRunning) {
            ProcessingForegroundService.start(context)
        }
    }

    /** Opens the storage guide, at most once per [GUIDE_INTERVAL_MILLIS]. zh-CN: 打开存储引导, 每 [GUIDE_INTERVAL_MILLIS] 最多一次. */
    private fun openStorageGuide() {
        val now = SystemClock.elapsedRealtime()
        val last = lastGuideAt
        if (last != null && now - last < GUIDE_INTERVAL_MILLIS) return
        lastGuideAt = now
        // Android may block activity starts from the background; the error detail names the screen anyway.
        // zh-CN: Android 可能拦截后台启动活动; 错误详情仍会指明该界面.
        try {
            context.startActivity(Intent(context, StorageAccessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: RuntimeException) {
            Log.w(TAG, "cannot open the storage guide", e)
        }
    }

    private class CallbackListener(
        private val callback: IMediaStudioCallback,
        private val onFinished: () -> Unit,
    ) : TaskRunner.Listener<Bundle> {

        override fun onProgress(taskId: Int, progress: TaskRunner.Progress) {
            callback.onProgress(taskId, Bundle().apply {
                putDouble(MediaStudioProgressKeys.PERCENT, progress.percent)
                putString(MediaStudioProgressKeys.STAGE, progress.stage)
                putLong(MediaStudioProgressKeys.ETA_MILLIS, progress.etaMillis)
                putInt(MediaStudioProgressKeys.TASK_ID, taskId)
            }.withContractVersion())
        }

        override fun onComplete(taskId: Int, result: Bundle) {
            try {
                callback.onComplete(taskId, result.withContractVersion())
            } finally {
                // The oneway call has handed the descriptor to the kernel; this copy is ours to close (contract version 2).
                // zh-CN: oneway 调用已把描述符交给内核; 本进程的副本由我们关闭 (契约版本 2).
                closeDescriptors(result)
                onFinished()
            }
        }

        override fun onError(taskId: Int, code: String, message: String) {
            try {
                callback.onError(taskId, errorBundle(code, detailOf(message)))
            } finally {
                onFinished()
            }
        }

    }

    companion object {

        private const val TAG = "SlateStudioEngine"

        /** Tasks running longer than this move into the foreground service (roadmap P2.1). zh-CN: 运行超过此时长的任务转入前台服务 (路线图 P2.1). */
        const val FOREGROUND_AFTER_MILLIS = 10_000L

        const val GUIDE_INTERVAL_MILLIS = 60_000L

        /** Callers without a host token share one token per calling uid. zh-CN: 没有宿主 token 的调用方按调用 uid 共用一个 token. */
        const val UID_TOKEN_PREFIX = "uid:"

        // The engine keeps the application context only.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MediaStudioEngine? = null

        fun get(context: Context): MediaStudioEngine = instance ?: synchronized(this) {
            instance ?: MediaStudioEngine(context.applicationContext).also { instance = it }
        }

        /** The engine when it exists, without creating it. zh-CN: 引擎已存在时返回它, 不创建. */
        fun peek(): MediaStudioEngine? = instance

        fun tokenKey(hostToken: String?, callingUid: Int): String =
            hostToken?.takeIf { it.isNotEmpty() } ?: "$UID_TOKEN_PREFIX$callingUid"

        fun errorBundle(code: String, detail: String): Bundle = Bundle().apply {
            putString(MediaStudioContract.KEY_ERROR_CODE, code)
            putString(MediaStudioContract.KEY_ERROR_MESSAGE, detail.take(MediaStudioLimits.MAX_ERROR_MESSAGE_CHARS))
        }.withContractVersion()

        /** Closes the descriptors of [result] (`cuesFile`). zh-CN: 关闭 [result] 中的描述符 (`cuesFile`). */
        fun closeDescriptors(result: Bundle) {
            @Suppress("DEPRECATION")
            (result.get(MediaStudioResultKeys.CUES_FILE) as? ParcelFileDescriptor)?.let { runCatching { it.close() } }
        }

        fun Bundle.withContractVersion(): Bundle = apply {
            putInt(MediaStudioContract.KEY_CONTRACT_VERSION, ThreeSlateStudioContract.CONTRACT_VERSION)
        }

        /** The detail of an encoded message: errorMessage carries no code prefix. zh-CN: 编码消息的详情部分: errorMessage 不带错误码前缀. */
        fun detailOf(message: String): String = MediaStudioErrorCodes.decode(message)?.second ?: message

        fun storageDetail(path: String) =
            "the plugin needs all files access for $path; grant it in the 3-Slate Studio storage screen"

        /**
         * Argument errors (options, formats, limits) are IllegalArgumentException, state errors (task limit, storage,
         * files, libraries) IllegalStateException; both cross the Binder with their message.
         * zh-CN: 参数错误 (选项, 格式, 上限) 为 IllegalArgumentException, 状态错误 (任务上限, 存储, 文件, 库) 为
         * IllegalStateException; 两者都会连同消息跨 Binder 传递.
         */
        fun MediaStudioException.toBinderException(): RuntimeException {
            val message = message ?: MediaStudioException.encodeMessage(code, detail)
            return when (code) {
                MediaStudioErrorCodes.INVALID_OPTIONS,
                MediaStudioErrorCodes.UNSUPPORTED_FORMAT,
                MediaStudioErrorCodes.LIMIT_EXCEEDED,
                -> IllegalArgumentException(message)
                else -> IllegalStateException(message)
            }
        }

        @Suppress("DEPRECATION")
        private fun sharedStorageRoot(): String = Environment.getExternalStorageDirectory().path

    }

}

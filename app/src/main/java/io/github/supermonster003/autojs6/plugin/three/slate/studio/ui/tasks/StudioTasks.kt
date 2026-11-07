package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.MediaStudioEngine
import org.autojs.plugin.mediastudio.api.IMediaStudioCallback
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.json.JSONArray
import java.io.FileInputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** A wizard request in the contract shape. zh-CN: 契约形态的向导请求. */
internal data class StudioRequest(
    val schema: OperationSchema,
    val inputs: List<String>,
    val options: Map<String, Any?>,
    val output: String?,
    val code: String,
)

/**
 * One row of the task queue (roadmap D35: script and Studio tasks in one list). Studio rows carry their request,
 * result and failure; script rows come from the shared [TaskRunner] and carry the state only.
 * zh-CN: 任务队列中的一行 (路线图 D35: 脚本与 Studio 任务同列). Studio 行带有请求, 结果与失败原因; 脚本行取自共用的 [TaskRunner],
 * 只有状态.
 */
internal data class TaskRow(
    val key: String,
    val taskId: Int?,
    val op: String,
    val origin: String,
    val state: String,
    val percent: Double,
    val stage: String,
    val output: String?,
    val submittedAt: Long,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val request: StudioRequest? = null,
    val result: Bundle? = null,
) {
    val isFinal: Boolean get() = state in MediaStudioContract.FINAL_TASK_STATES
}

/**
 * The Studio side of the shared executor (roadmap D35): wizard requests go through [MediaStudioEngine.submit] in the
 * plugin process (origin `studio`, host token [HOST_TOKEN]) with a local callback, so they share the queue, the limits
 * and the foreground service with script tasks; [rows] merges them with the script tasks of the runner. Compose state,
 * updated on the main thread.
 * zh-CN: 共用执行器的 Studio 一侧 (路线图 D35): 向导请求在插件进程内经 [MediaStudioEngine.submit] 提交 (origin `studio`, 宿主
 * token [HOST_TOKEN]), 使用本地回调, 因而与脚本任务共用队列, 上限与前台服务; [rows] 把它们与执行器中的脚本任务合并. Compose
 * 状态, 在主线程更新.
 */
internal object StudioTasks {

    const val HOST_TOKEN = "studio"
    private const val TAG = "SlateStudioTasks"

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "SlateStudioSubmit").apply { isDaemon = true } }
    private val keys = AtomicInteger()

    /** Studio rows by key, newest last; touched on the main thread only. zh-CN: 按键保存的 Studio 行; 只在主线程访问. */
    private val studio = LinkedHashMap<String, TaskRow>()
    private val scriptTokens = LinkedHashSet<String>()
    private var observing = false

    var rows by mutableStateOf<List<TaskRow>>(emptyList())
        private set

    fun row(key: String): TaskRow? = studio[key]

    /**
     * Submits [request] off the main thread and answers the row key at once; a refused request ends as a failed row
     * with the plugin's code and detail.
     * zh-CN: 在后台线程提交 [request] 并立即返回行键; 被拒绝的请求以带插件错误码与详情的失败行结束.
     */
    fun submit(context: Context, request: StudioRequest, prepare: (StudioRequest) -> StudioRequest = { it }): String {
        val engine = MediaStudioEngine.get(context)
        observe(engine)
        val key = "s" + keys.incrementAndGet()
        put(TaskRow(key, null, request.schema.op, MediaStudioContract.ORIGIN_STUDIO, MediaStudioContract.TASK_QUEUED, 0.0,
            MediaStudioContract.STAGE_QUEUED, request.output, SystemClock.elapsedRealtime(), request = request))
        worker.execute {
            val prepared = runCatching { prepare(request) }.getOrElse { request }
            main.post { studio[key]?.let { put(it.copy(request = prepared)) } }
            if (prepared.schema.op == OperationSchema.SUBTITLES_CONVERT) {
                submitStep(engine, key, MediaStudioOps.SUBTITLES_PARSE, prepared.inputs, emptyMap(), null) { result ->
                    val cues = readCues(result)
                    if (cues == null) {
                        fail(key, MediaStudioErrorCodes.INTERNAL_ERROR, "subtitlesParse answered no cues")
                    } else {
                        worker.execute {
                            submitStep(engine, key, MediaStudioOps.SUBTITLES_WRITE, emptyList(), prepared.options + ("cues" to cues), prepared.output, null)
                        }
                    }
                }
            } else {
                submitStep(engine, key, prepared.schema.op, prepared.inputs, prepared.options, prepared.output, null)
            }
        }
        return key
    }

    private fun submitStep(
        engine: MediaStudioEngine,
        key: String,
        op: String,
        inputs: List<String>,
        options: Map<String, Any?>,
        output: String?,
        onComplete: ((Bundle) -> Unit)?,
    ) {
        val request = Bundle().apply {
            putString(MediaStudioRequestKeys.OP, op)
            putStringArray(MediaStudioRequestKeys.INPUTS, inputs.toTypedArray())
            output?.let { putString(MediaStudioRequestKeys.OUTPUT, it) }
            putBundle(MediaStudioRequestKeys.OPTIONS, RequestBundles.of(options))
            putString(MediaStudioRequestKeys.HOST_TOKEN, HOST_TOKEN)
            putString(MediaStudioRequestKeys.ORIGIN, MediaStudioContract.ORIGIN_STUDIO)
        }
        val callback = object : IMediaStudioCallback.Stub() {
            override fun onProgress(taskId: Int, progress: Bundle) {
                val percent = progress.getDouble("percent", 0.0)
                val stage = progress.getString("stage") ?: MediaStudioContract.STAGE_QUEUED
                main.post { studio[key]?.takeIf { !it.isFinal }?.let { put(it.copy(state = MediaStudioContract.TASK_RUNNING, percent = percent, stage = stage)) } }
            }

            override fun onComplete(taskId: Int, result: Bundle) {
                // A cues descriptor is closed by the engine after this call: read it now (contract version 2).
                // zh-CN: cue 描述符在本调用之后由引擎关闭: 现在读取 (契约版本 2).
                if (onComplete != null) {
                    val copy = Bundle(result)
                    readCuesNow(copy)
                    main.post { onComplete(copy) }
                    return
                }
                val copy = Bundle(result).apply { remove(CUES_FILE) }
                main.post {
                    studio[key]?.let { put(it.copy(state = MediaStudioContract.TASK_COMPLETED, percent = 100.0, stage = MediaStudioContract.STAGE_WRITING, result = copy, output = copy.getString("path") ?: it.output)) }
                }
            }

            override fun onError(taskId: Int, error: Bundle) {
                val code = error.getString(MediaStudioContract.KEY_ERROR_CODE) ?: MediaStudioErrorCodes.INTERNAL_ERROR
                val message = error.getString(MediaStudioContract.KEY_ERROR_MESSAGE).orEmpty()
                val state = if (code == MediaStudioErrorCodes.TASK_CANCELLED) MediaStudioContract.TASK_CANCELLED else MediaStudioContract.TASK_FAILED
                main.post { studio[key]?.let { put(it.copy(state = state, errorCode = code, errorMessage = message)) } }
            }
        }
        try {
            val taskId = engine.submit(request, callback, Process.myUid())
            main.post { studio[key]?.let { put(it.copy(taskId = taskId)) } }
        } catch (e: RuntimeException) {
            val message = e.message.orEmpty()
            val decoded = MediaStudioErrorCodes.decode(message)
            fail(key, decoded?.first ?: MediaStudioErrorCodes.INTERNAL_ERROR, decoded?.second ?: message)
        }
    }

    private fun fail(key: String, code: String, message: String) {
        main.post { studio[key]?.let { put(it.copy(state = MediaStudioContract.TASK_FAILED, errorCode = code, errorMessage = message)) } }
    }

    /** Cues of a `subtitlesParse` result: `cuesFile` (contract version 2) or `cues` (version 1). zh-CN: `subtitlesParse` 结果中的 cue. */
    private fun readCues(result: Bundle): List<Map<String, Any?>>? {
        result.classLoader = javaClass.classLoader
        @Suppress("DEPRECATION")
        (result.get(PARSED_CUES) as? ArrayList<*>)?.let { list -> return list.filterIsInstance<Map<String, Any?>>() }
        @Suppress("DEPRECATION")
        val inline = result.get("cues")
        val bundles = when (inline) {
            is Array<*> -> inline.filterIsInstance<Bundle>()
            is List<*> -> inline.filterIsInstance<Bundle>()
            else -> return null
        }
        return bundles.map { mapOf("start" to it.getLong("start"), "end" to it.getLong("end"), "text" to it.getString("text")) }
    }

    /** Reads `cuesFile` while the descriptor is open and keeps the cues in the copy. zh-CN: 在描述符仍打开时读取 `cuesFile`. */
    private fun readCuesNow(result: Bundle) {
        @Suppress("DEPRECATION")
        val descriptor = result.get(CUES_FILE) as? ParcelFileDescriptor ?: return
        result.remove(CUES_FILE)
        val text = runCatching { FileInputStream(descriptor.fileDescriptor).bufferedReader(Charsets.UTF_8).readText() }
            .onFailure { Log.w(TAG, "cannot read the cues", it) }.getOrNull() ?: return
        val array = JSONArray(text)
        val cues = ArrayList<Map<String, Any?>>(array.length())
        for (i in 0 until array.length()) {
            val cue = array.getJSONObject(i)
            cues += mapOf("start" to cue.getLong("start"), "end" to cue.getLong("end"), "text" to cue.getString("text"))
        }
        result.putSerializable(PARSED_CUES, cues)
    }

    fun cancel(context: Context, row: TaskRow): Boolean {
        val taskId = row.taskId ?: return false
        return MediaStudioEngine.get(context).cancel(taskId)
    }

    /** Removes the finished Studio rows. zh-CN: 移除已结束的 Studio 行. */
    fun clearFinished() {
        studio.values.filter { it.isFinal }.forEach { studio.remove(it.key) }
        publish()
    }

    /** Merges the runner's script tasks into [rows]; call on the main thread. zh-CN: 把执行器中的脚本任务并入 [rows]; 在主线程调用. */
    fun refresh(context: Context) {
        val engine = MediaStudioEngine.get(context)
        observe(engine)
        publish(engine.runner)
    }

    private fun observe(engine: MediaStudioEngine) {
        if (observing) return
        observing = true
        engine.runner.addObserver { runner -> main.post { publish(runner) } }
    }

    private fun put(row: TaskRow) {
        studio[row.key] = row
        publish()
    }

    private fun publish(runner: TaskRunner<*>? = MediaStudioEngine.peek()?.runner) {
        val scripts = if (runner == null) emptyList() else {
            runner.unfinished().forEach { if (it.origin != MediaStudioContract.ORIGIN_STUDIO) scriptTokens += it.tokenKey }
            scriptTokens.flatMap { token -> runner.list(token) }.filter { it.origin != MediaStudioContract.ORIGIN_STUDIO }.map { task ->
                TaskRow("t${task.taskId}", task.taskId, task.op, task.origin, task.state, task.percent, task.stage, task.output, task.submittedAt)
            }
        }
        rows = (studio.values + scripts).sortedByDescending { it.submittedAt }
    }

    private const val CUES_FILE = "cuesFile"
    private const val PARSED_CUES = "slate.parsedCues"

}

/**
 * The plain values of a wizard request as the contract Bundle the host would send (shared notes of roadmap P1.3):
 * integers as Long, other numbers as Double, objects as Bundle, arrays of objects as Bundle[], arrays of numbers as
 * double[], strings as String[].
 * zh-CN: 把向导请求的纯值转成宿主会发送的契约 Bundle (路线图 P1.3 共享笔记): 整数为 Long, 其他数为 Double, 对象为 Bundle,
 * 对象数组为 Bundle[], 数字数组为 double[], 字符串数组为 String[].
 */
internal object RequestBundles {
    fun of(values: Map<String, Any?>): Bundle = Bundle().apply {
        for ((key, value) in values) put(this, key, value)
    }

    private fun put(bundle: Bundle, key: String, value: Any?) {
        when (value) {
            null -> Unit
            is String -> bundle.putString(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is Int -> bundle.putLong(key, value.toLong())
            is Long -> bundle.putLong(key, value)
            is Float -> bundle.putDouble(key, value.toDouble())
            is Double -> bundle.putDouble(key, value)
            is Map<*, *> -> bundle.putBundle(key, of(value.entries.associate { (k, v) -> k.toString() to v }))
            is List<*> -> when {
                value.all { it is Map<*, *> } -> bundle.putParcelableArray(key, value.map { of((it as Map<*, *>).entries.associate { (k, v) -> k.toString() to v }) }.toTypedArray())
                value.all { it is Number } -> bundle.putDoubleArray(key, value.map { (it as Number).toDouble() }.toDoubleArray())
                else -> bundle.putStringArray(key, value.map { it.toString() }.toTypedArray())
            }
            else -> bundle.putString(key, value.toString())
        }
    }
}

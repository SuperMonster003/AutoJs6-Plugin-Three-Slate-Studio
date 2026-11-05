package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import java.util.concurrent.Executor

/**
 * The task queue of roadmap D28 / P2.1: at most [maxConcurrent] jobs run at once (one encode each), a host token has at
 * most [maxPerToken] unfinished tasks, and every task ends with exactly one [Listener.onComplete] or [Listener.onError].
 * Listeners are called outside the runner lock; progress is monotonic and at least [minProgressIntervalMillis] apart.
 * The runner is plain Kotlin so the JVM tests drive it with a manual executor and a fake clock.
 * zh-CN: 路线图 D28 / P2.1 的任务队列: 同时最多运行 [maxConcurrent] 个作业 (每个占一路编码), 每个宿主 token 最多
 * [maxPerToken] 个未完成任务, 每个任务恰好以一次 [Listener.onComplete] 或 [Listener.onError] 结束. 监听器在执行器锁外调用;
 * 进度不递减, 两次至少间隔 [minProgressIntervalMillis]. 执行器是纯 Kotlin, JVM 测试用手动执行器与假时钟驱动.
 */
internal class TaskRunner<R>(
    private val executor: Executor,
    private val monotonicClock: () -> Long,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val maxConcurrent: Int = MediaStudioLimits.MAX_CONCURRENT_ENCODES,
    private val maxPerToken: Int = MediaStudioLimits.MAX_TASKS_PER_TOKEN,
    private val minProgressIntervalMillis: Long = MediaStudioLimits.MIN_PROGRESS_INTERVAL_MILLIS,
    private val keepFinishedPerToken: Int = KEEP_FINISHED_PER_TOKEN,
) {

    /** One operation. zh-CN: 一个操作. */
    interface Job<R> {

        /** Runs on a worker thread; throws [MediaStudioException] to fail. zh-CN: 在工作线程运行; 抛 [MediaStudioException] 表示失败. */
        fun run(context: JobContext): R

        /**
         * Deletes what an unfinished run left (temporary and final outputs); called once after a failed or cancelled run,
         * and for a task cancelled while queued.
         * zh-CN: 删除未完成的运行留下的文件 (临时与最终输出); 运行失败或被取消后调用一次, 排队中被取消的任务也调用.
         */
        fun discard() {}

    }

    /** What a running job sees. zh-CN: 运行中的作业可用的接口. */
    interface JobContext {

        val taskId: Int

        val isCancelled: Boolean

        /** Percent 0-100 (clamped, never decreasing) and a contract stage. zh-CN: 百分比 0-100 (截断, 不递减) 与契约阶段. */
        fun progress(percent: Double, stage: String)

        /**
         * Called (once, on the cancelling thread) when the task is cancelled; replaces the previous handler and runs at
         * once when the task is already cancelled.
         * zh-CN: 任务被取消时 (在发起取消的线程上) 调用一次; 替换之前的处理器, 任务已取消时立即调用.
         */
        fun setCancelHandler(handler: (() -> Unit)?)

        fun throwIfCancelled() {
            if (isCancelled) throw MediaStudioException(MediaStudioErrorCodes.TASK_CANCELLED, "task $taskId was cancelled")
        }

    }

    interface Listener<R> {
        fun onProgress(taskId: Int, progress: Progress)
        fun onComplete(taskId: Int, result: R)
        fun onError(taskId: Int, code: String, message: String)
    }

    /** Observes state transitions (submitted, started, finished). zh-CN: 观察状态变化 (提交, 开始, 结束). */
    fun interface Observer {
        fun onTasksChanged(runner: TaskRunner<*>)
    }

    data class Progress(val percent: Double, val stage: String, val etaMillis: Long)

    data class TaskSnapshot(
        val taskId: Int,
        val tokenKey: String,
        val op: String,
        val state: String,
        val percent: Double,
        val stage: String,
        val output: String?,
        val origin: String,
        val submittedAt: Long,
        /** Monotonic start time, null while queued. zh-CN: 单调时钟的开始时间, 排队中为 null. */
        val startedAt: Long?,
    )

    private inner class Task(
        val id: Int,
        val tokenKey: String,
        val op: String,
        val output: String?,
        val origin: String,
        val claims: Set<String>,
        val job: Job<R>,
        val listener: Listener<R>,
        val submittedAt: Long,
    ) : JobContext {

        var state: String = MediaStudioContract.TASK_QUEUED
        var percent: Double = 0.0
        var stage: String = MediaStudioContract.STAGE_QUEUED
        var startedAt: Long? = null
        var lastEmitAt: Long? = null
        var onCancel: (() -> Unit)? = null
        var cancelled: Boolean = false
        var finishedAt: Long = 0L

        override val taskId: Int get() = id

        override val isCancelled: Boolean get() = synchronized(lock) { cancelled }

        val isFinal: Boolean get() = state in MediaStudioContract.FINAL_TASK_STATES

        override fun progress(percent: Double, stage: String) {
            val event: Progress
            synchronized(lock) {
                if (isFinal || cancelled) return
                val clamped = if (percent.isNaN()) this.percent else percent.coerceIn(0.0, 100.0)
                this.percent = maxOf(this.percent, clamped)
                this.stage = stage
                val now = monotonicClock()
                val last = lastEmitAt
                if (last != null && now - last < minProgressIntervalMillis) return
                lastEmitAt = now
                event = Progress(this.percent, stage, eta(now))
            }
            safely { listener.onProgress(id, event) }
        }

        override fun setCancelHandler(handler: (() -> Unit)?) {
            val runNow = synchronized(lock) {
                if (cancelled) true else {
                    onCancel = handler
                    false
                }
            }
            if (runNow) handler?.let { safely(it) }
        }

        private fun eta(now: Long): Long {
            val start = startedAt ?: return -1L
            if (percent < 1.0 || percent >= 100.0) return -1L
            val elapsed = now - start
            return (elapsed * (100.0 - percent) / percent).toLong()
        }

        fun snapshot() = TaskSnapshot(id, tokenKey, op, state, percent, stage, output, origin, submittedAt, startedAt)

    }

    private val lock = Any()
    private val tasks = LinkedHashMap<Int, Task>()
    private val queue = ArrayDeque<Task>()
    private val observers = mutableListOf<Observer>()
    private var nextId = 1
    private var running = 0
    private var finishSequence = 0L

    /**
     * Queues a job and returns its task id (> 0). Throws [MediaStudioException] with TASK_LIMIT when [tokenKey] already
     * has [maxPerToken] unfinished tasks, and with IO_ERROR when one of [claims] (output paths) belongs to an unfinished
     * task.
     * zh-CN: 排入一个作业并返回任务 id (> 0). [tokenKey] 已有 [maxPerToken] 个未完成任务时抛 TASK_LIMIT, [claims] (输出路径)
     * 之一属于未完成任务时抛 IO_ERROR.
     */
    fun submit(
        tokenKey: String,
        op: String,
        output: String?,
        origin: String,
        claims: Collection<String>,
        job: Job<R>,
        listener: Listener<R>,
    ): Int {
        val task: Task
        synchronized(lock) {
            val unfinished = tasks.values.count { it.tokenKey == tokenKey && !it.isFinal }
            if (unfinished >= maxPerToken) {
                throw MediaStudioException(
                    MediaStudioErrorCodes.TASK_LIMIT,
                    "$unfinished unfinished tasks for this host token, at most $maxPerToken",
                )
            }
            claims.firstNotNullOfOrNull { path -> claimantOf(path)?.let { path to it } }?.let { (path, owner) ->
                throw MediaStudioException.io("output $path is in use by task ${owner.id}")
            }
            task = Task(nextId++, tokenKey, op, output, origin, claims.toSet(), job, listener, wallClock())
            tasks[task.id] = task
            queue.addLast(task)
        }
        notifyObservers()
        pump()
        return task.id
    }

    /** True when an unfinished task claims [path]. zh-CN: 有未完成任务占用 [path] 时为 true. */
    fun isClaimed(path: String): Boolean = synchronized(lock) { claimantOf(path) != null }

    private fun claimantOf(path: String): Task? = tasks.values.firstOrNull { !it.isFinal && path in it.claims }

    /**
     * Cancels a queued or running task; false when [taskId] is unknown or already finished. The listener receives
     * TASK_CANCELLED at once; a running job learns it through its cancel handler and [JobContext.isCancelled].
     * zh-CN: 取消排队中或运行中的任务; [taskId] 未知或已结束时返回 false. 监听器立即收到 TASK_CANCELLED; 运行中的作业经取消
     * 处理器与 [JobContext.isCancelled] 得知.
     */
    fun cancel(taskId: Int): Boolean {
        val task: Task
        val handler: (() -> Unit)?
        val wasQueued: Boolean
        synchronized(lock) {
            task = tasks[taskId] ?: return false
            if (task.isFinal) return false
            wasQueued = task.state == MediaStudioContract.TASK_QUEUED
            if (wasQueued) queue.remove(task)
            task.cancelled = true
            handler = task.onCancel
            task.onCancel = null
            finish(task, MediaStudioContract.TASK_CANCELLED)
        }
        handler?.let { safely(it) }
        if (wasQueued) safely { task.job.discard() }
        safely { task.listener.onError(taskId, MediaStudioErrorCodes.TASK_CANCELLED, cancelledMessage(taskId)) }
        notifyObservers()
        return true
    }

    /** Cancels every unfinished task of [tokenKey] (its callback Binder died); returns how many. zh-CN: 取消 [tokenKey] 的全部未完成任务 (其回调 Binder 已死亡), 返回个数. */
    fun cancelToken(tokenKey: String): Int {
        val ids = synchronized(lock) { tasks.values.filter { it.tokenKey == tokenKey && !it.isFinal }.map { it.id } }
        return ids.count { cancel(it) }
    }

    /** Tasks of [tokenKey] in submit order. zh-CN: [tokenKey] 的任务, 按提交顺序. */
    fun list(tokenKey: String): List<TaskSnapshot> = synchronized(lock) {
        tasks.values.filter { it.tokenKey == tokenKey }.map { it.snapshot() }
    }

    /** Unfinished tasks of every token in submit order. zh-CN: 全部 token 的未完成任务, 按提交顺序. */
    fun unfinished(): List<TaskSnapshot> = synchronized(lock) {
        tasks.values.filter { !it.isFinal }.map { it.snapshot() }
    }

    fun addObserver(observer: Observer) {
        synchronized(lock) { observers += observer }
    }

    fun removeObserver(observer: Observer) {
        synchronized(lock) { observers -= observer }
    }

    private fun pump() {
        val starting = mutableListOf<Task>()
        synchronized(lock) {
            while (running < maxConcurrent && queue.isNotEmpty()) {
                val task = queue.removeFirst()
                task.state = MediaStudioContract.TASK_RUNNING
                task.startedAt = monotonicClock()
                running++
                starting += task
            }
        }
        if (starting.isEmpty()) return
        notifyObservers()
        starting.forEach { task -> executor.execute { execute(task) } }
    }

    private fun execute(task: Task) {
        val result: Result<R> = runCatching {
            task.throwIfCancelled()
            task.job.run(task)
        }
        var completed = false
        var failure: Pair<String, String>? = null
        synchronized(lock) {
            running--
            if (!task.isFinal) {
                val error = result.exceptionOrNull()
                if (error == null) {
                    task.percent = 100.0
                    finish(task, MediaStudioContract.TASK_COMPLETED)
                    completed = true
                } else if (task.cancelled || (error as? MediaStudioException)?.code == MediaStudioErrorCodes.TASK_CANCELLED) {
                    task.cancelled = true
                    finish(task, MediaStudioContract.TASK_CANCELLED)
                    failure = MediaStudioErrorCodes.TASK_CANCELLED to cancelledMessage(task.id)
                } else {
                    finish(task, MediaStudioContract.TASK_FAILED)
                    failure = describe(error)
                }
            }
        }
        if (!completed) safely { task.job.discard() }
        if (completed) {
            @Suppress("UNCHECKED_CAST")
            safely { task.listener.onComplete(task.id, result.getOrNull() as R) }
        }
        failure?.let { (code, message) -> safely { task.listener.onError(task.id, code, message) } }
        notifyObservers()
        pump()
    }

    /** Marks [task] final under the lock and trims the finished history of its token. zh-CN: 在锁内把 [task] 标为终态并裁剪其 token 的已结束记录. */
    private fun finish(task: Task, state: String) {
        task.state = state
        task.finishedAt = ++finishSequence
        val finished = tasks.values.filter { it.tokenKey == task.tokenKey && it.isFinal }
        if (finished.size > keepFinishedPerToken) {
            finished.sortedBy { it.finishedAt }.take(finished.size - keepFinishedPerToken).forEach { tasks.remove(it.id) }
        }
    }

    private fun describe(error: Throwable): Pair<String, String> = when (error) {
        is MediaStudioException -> error.code to (error.message ?: error.code)
        else -> MediaStudioErrorCodes.INTERNAL_ERROR to MediaStudioException.encodeMessage(
            MediaStudioErrorCodes.INTERNAL_ERROR,
            listOfNotNull(error.javaClass.simpleName, error.message).joinToString(": "),
        )
    }

    private fun cancelledMessage(taskId: Int) =
        MediaStudioException.encodeMessage(MediaStudioErrorCodes.TASK_CANCELLED, "task $taskId was cancelled")

    private fun notifyObservers() {
        val current = synchronized(lock) { observers.toList() }
        current.forEach { observer -> safely { observer.onTasksChanged(this) } }
    }

    /** Listener and handler failures (a dead Binder) never break the runner. zh-CN: 监听器与处理器的失败 (Binder 已死亡) 不影响执行器. */
    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
        }
    }

    companion object {

        /** Finished tasks kept per token for `listTasks`. zh-CN: 每个 token 为 `listTasks` 保留的已结束任务数. */
        const val KEEP_FINISHED_PER_TOKEN = 16

    }

}

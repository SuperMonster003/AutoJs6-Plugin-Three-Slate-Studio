package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

/**
 * Queue, concurrency, per token limit, cancellation and progress rules of the task runner (roadmap D28, P2.1).
 * zh-CN: 任务执行器的队列, 并发, 每 token 上限, 取消与进度规则 (路线图 D28, P2.1).
 */
class TaskRunnerTest {

    /** Runs submitted work only when the test says so. zh-CN: 只在测试要求时运行提交的工作. */
    private class ManualExecutor : Executor {
        val pending = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) {
            pending.addLast(command)
        }

        fun runNext() = pending.removeFirst().run()
    }

    private class Recorder : TaskRunner.Listener<String> {
        val events = mutableListOf<String>()
        val progress = mutableListOf<TaskRunner.Progress>()
        override fun onProgress(taskId: Int, progress: TaskRunner.Progress) {
            this.progress += progress
        }

        override fun onComplete(taskId: Int, result: String) {
            events += "complete $taskId $result"
        }

        override fun onError(taskId: Int, code: String, message: String) {
            events += "error $taskId $code"
        }
    }

    private class ScriptedJob(val body: (TaskRunner.JobContext) -> String = { "ok" }) : TaskRunner.Job<String> {
        var discards = 0
        override fun run(context: TaskRunner.JobContext): String = body(context)
        override fun discard() {
            discards++
        }
    }

    private var now = 1_000L
    private val executor = ManualExecutor()
    private val runner = TaskRunner<String>(executor, monotonicClock = { now }, wallClock = { 42L })

    private fun submit(
        token: String = "host-a",
        job: TaskRunner.Job<String> = ScriptedJob(),
        listener: Recorder = Recorder(),
        claims: List<String> = emptyList(),
    ): Int = runner.submit(token, "trim", claims.firstOrNull(), MediaStudioContract.ORIGIN_SCRIPT, claims, job, listener)

    private fun states(token: String = "host-a") = runner.list(token).map { it.state }

    @Test
    fun `task ids start at 1 and grow`() {
        assertEquals(1, submit())
        assertEquals(2, submit(token = "host-b"))
        assertEquals(3, submit())
    }

    @Test
    fun `at most two jobs run at once and the rest wait in submit order`() {
        val ids = (1..3).map { submit(token = "host-$it") }
        assertEquals(2, executor.pending.size)
        assertEquals(listOf(MediaStudioContract.TASK_RUNNING, MediaStudioContract.TASK_RUNNING, MediaStudioContract.TASK_QUEUED), runner.unfinished().map { it.state })
        executor.runNext()
        assertEquals("the freed slot starts the queued task", 2, executor.pending.size)
        assertEquals(ids.drop(1), runner.unfinished().map { it.taskId })
        executor.runNext()
        executor.runNext()
        assertTrue(runner.unfinished().isEmpty())
    }

    @Test
    fun `a host token has at most four unfinished tasks`() {
        repeat(4) { submit() }
        val error = assertThrows(MediaStudioException::class.java) { submit() }
        assertEquals(MediaStudioErrorCodes.TASK_LIMIT, error.code)
        assertTrue(error.message!!.startsWith("TASK_LIMIT: "))
        assertEquals("another token is not limited", 5, submit(token = "host-b"))
        executor.runNext()
        assertEquals("a finished task frees a place", 6, submit())
    }

    @Test
    fun `completion reports the result exactly once`() {
        val listener = Recorder()
        val id = submit(listener = listener, job = ScriptedJob { "done" })
        executor.runNext()
        assertEquals(listOf("complete $id done"), listener.events)
        val snapshot = runner.list("host-a").single()
        assertEquals(MediaStudioContract.TASK_COMPLETED, snapshot.state)
        assertEquals(100.0, snapshot.percent, 0.0)
        assertEquals(42L, snapshot.submittedAt)
        assertFalse(runner.cancel(id))
    }

    @Test
    fun `failures keep their code and other exceptions become INTERNAL_ERROR`() {
        val listener = Recorder()
        val codec = ScriptedJob { throw MediaStudioException(MediaStudioErrorCodes.CODEC_ERROR, "boom") }
        val crash = ScriptedJob { throw IllegalStateException("bug") }
        val first = submit(listener = listener, job = codec)
        val second = submit(listener = listener, job = crash)
        executor.runNext()
        executor.runNext()
        assertEquals(listOf("error $first CODEC_ERROR", "error $second INTERNAL_ERROR"), listener.events)
        assertEquals("failed runs discard their outputs", 1, codec.discards)
        assertEquals(1, crash.discards)
        assertEquals(listOf(MediaStudioContract.TASK_FAILED, MediaStudioContract.TASK_FAILED), states())
    }

    @Test
    fun `cancelling a queued task ends it at once and discards its outputs`() {
        submit(token = "host-b")
        submit(token = "host-b")
        val listener = Recorder()
        val job = ScriptedJob()
        val id = submit(listener = listener, job = job)
        assertTrue(runner.cancel(id))
        assertEquals(listOf("error $id TASK_CANCELLED"), listener.events)
        assertEquals(1, job.discards)
        assertEquals(listOf(MediaStudioContract.TASK_CANCELLED), states())
        assertFalse("a second cancel is refused", runner.cancel(id))
        assertFalse("unknown ids are refused", runner.cancel(999))
        executor.runNext()
        executor.runNext()
        assertTrue("the cancelled task never ran", executor.pending.isEmpty())
    }

    @Test
    fun `cancelling a running task calls its handler and ignores its late result`() {
        val listener = Recorder()
        var handled = 0
        lateinit var context: TaskRunner.JobContext
        val job = ScriptedJob { ctx ->
            context = ctx
            ctx.setCancelHandler { handled++ }
            assertTrue(runner.cancel(ctx.taskId))
            assertTrue(ctx.isCancelled)
            "late"
        }
        val id = submit(listener = listener, job = job)
        executor.runNext()
        assertEquals(1, handled)
        assertEquals(listOf("error $id TASK_CANCELLED"), listener.events)
        assertEquals("a cancelled run discards even a finished output", 1, job.discards)
        var lateHandler = 0
        context.setCancelHandler { lateHandler++ }
        assertEquals("a handler set after the cancel runs at once", 1, lateHandler)
    }

    @Test
    fun `a job that throws TASK_CANCELLED itself ends as cancelled`() {
        val listener = Recorder()
        val id = submit(listener = listener, job = ScriptedJob { ctx ->
            runner.cancel(ctx.taskId)
            ctx.throwIfCancelled()
            "unreachable"
        })
        executor.runNext()
        assertEquals(listOf("error $id TASK_CANCELLED"), listener.events)
    }

    @Test
    fun `cancelToken cancels every unfinished task of one token`() {
        val a = Recorder()
        val b = Recorder()
        submit(token = "dead", listener = a)
        submit(token = "alive", listener = b)
        submit(token = "dead", listener = a)
        assertEquals(2, runner.cancelToken("dead"))
        assertEquals(2, a.events.count { it.endsWith("TASK_CANCELLED") })
        assertTrue(b.events.isEmpty())
        assertEquals(0, runner.cancelToken("dead"))
    }

    @Test
    fun `an output claimed by an unfinished task is refused`() {
        submit(claims = listOf("/sdcard/out.mp4"))
        assertTrue(runner.isClaimed("/sdcard/out.mp4"))
        val error = assertThrows(MediaStudioException::class.java) { submit(token = "host-b", claims = listOf("/sdcard/out.mp4")) }
        assertEquals(MediaStudioErrorCodes.IO_ERROR, error.code)
        executor.runNext()
        assertFalse(runner.isClaimed("/sdcard/out.mp4"))
    }

    @Test
    fun `progress is clamped, never decreases and is at least 200 ms apart`() {
        val listener = Recorder()
        submit(listener = listener, job = ScriptedJob { ctx ->
            ctx.progress(10.0, MediaStudioContract.STAGE_ENCODING)
            now += 100
            ctx.progress(20.0, MediaStudioContract.STAGE_ENCODING)
            now += 100
            ctx.progress(5.0, MediaStudioContract.STAGE_ENCODING)
            now += 300
            ctx.progress(250.0, MediaStudioContract.STAGE_WRITING)
            "ok"
        })
        executor.runNext()
        assertEquals(listOf(10.0, 20.0, 100.0), listener.progress.map { it.percent })
        assertEquals(MediaStudioContract.STAGE_WRITING, listener.progress.last().stage)
    }

    @Test
    fun `eta follows the elapsed time and is -1 while unknown`() {
        val listener = Recorder()
        submit(listener = listener, job = ScriptedJob { ctx ->
            ctx.progress(0.5, MediaStudioContract.STAGE_ENCODING)
            now += 4_000
            ctx.progress(25.0, MediaStudioContract.STAGE_ENCODING)
            "ok"
        })
        executor.runNext()
        assertEquals(listOf(-1L, 12_000L), listener.progress.map { it.etaMillis })
    }

    @Test
    fun `listTasks keeps submit order and the last 16 finished tasks per token`() {
        repeat(20) {
            submit()
            executor.runNext()
        }
        val ids = runner.list("host-a").map { it.taskId }
        assertEquals((5..20).toList(), ids)
        assertTrue(runner.list("nobody").isEmpty())
    }

    @Test
    fun `listener failures do not break the runner`() {
        val broken = object : TaskRunner.Listener<String> {
            override fun onProgress(taskId: Int, progress: TaskRunner.Progress) = throw IllegalStateException("dead binder")
            override fun onComplete(taskId: Int, result: String) = throw IllegalStateException("dead binder")
            override fun onError(taskId: Int, code: String, message: String) = throw IllegalStateException("dead binder")
        }
        runner.submit("host-a", "trim", null, MediaStudioContract.ORIGIN_SCRIPT, emptyList(), ScriptedJob { ctx ->
            ctx.progress(50.0, MediaStudioContract.STAGE_ENCODING)
            "ok"
        }, broken)
        executor.runNext()
        assertEquals(listOf(MediaStudioContract.TASK_COMPLETED), states())
    }

    @Test
    fun `observers see submits, starts and finishes`() {
        val seen = mutableListOf<Int>()
        runner.addObserver { seen += it.unfinished().size }
        submit()
        executor.runNext()
        assertEquals(listOf(1, 1, 0), seen)
    }

}

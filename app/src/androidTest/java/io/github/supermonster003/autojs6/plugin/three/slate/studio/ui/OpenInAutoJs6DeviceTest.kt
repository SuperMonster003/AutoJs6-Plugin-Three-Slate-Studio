package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.AutoJs6Scripts
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.CodeGenerator
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.GeneratedScript
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioTasks
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.TaskRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.OpenInAutoJs6Button
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/**
 * Roadmap P4.4 acceptance on a device, driven by `.python/device_wizard_check.py --open-in-autojs6` (skipped without its
 * `openInAutoJs6` argument; needs AutoJs6 6641 or later, a debug build so that its private copy can be read through
 * run-as): "Open in AutoJs6" from a wizard result sends the code inline, and a script over the inline limits goes
 * through the plugin's FileProvider as a content URI. Each case waits for the host's `CREATE_SCRIPT accepted` and for
 * its editor on top; the URI case also finds the MD5 of the sent code in the host's private copy. Nothing on the phone
 * has to be tapped: the clicks are on the plugin's own screens.
 * zh-CN: 路线图 P4.4 的真机验收, 由 `.python/device_wizard_check.py --open-in-autojs6` 驱动 (没有其 `openInAutoJs6` 参数时跳过;
 * 需要 AutoJs6 6641 或更新, 且为 debug 构建, 以便经 run-as 读取其私有副本): 从向导结果页 "在 AutoJs6 中打开" 内联发送代码, 超过
 * 内联上限的脚本经插件的 FileProvider 以 content URI 发送. 每种情形都等待宿主的 `CREATE_SCRIPT accepted` 与其编辑器到前台; URI
 * 情形还在宿主私有副本中找到所发送代码的 MD5. 手机上无需点按: 点击都在插件自己的页面上.
 */
@RunWith(AndroidJUnit4::class)
class OpenInAutoJs6DeviceTest {

    // The v1 rule like WizardDeviceTest: its unconfined dispatcher runs the button's coroutine at once, while the v2
    // rule queues it until the test advances the clock, which the polling below never does.
    // zh-CN: 与 WizardDeviceTest 相同的 v1 规则: 其非受限调度器立即运行按钮的协程, v2 规则则排队到测试推进时钟为止, 而下面的轮询不推进.
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val arguments get() = InstrumentationRegistry.getArguments()

    private fun requireHost() {
        assumeTrue("run by .python/device_wizard_check.py --open-in-autojs6", arguments.getString(ARG_OPEN) != null)
        assertTrue("AutoJs6 ${AutoJs6Scripts.hostVersionCode(context)} has no CREATE_SCRIPT entry", AutoJs6Scripts.isAvailable(context))
    }

    @Test
    fun wizardResultOpensInTheEditor() {
        requireHost()
        val library = File(arguments.getString(ARG_LIBRARY)!!)
        val target = File(File(arguments.getString(ARG_OUT)!!).apply { mkdirs() }, "open-trim.mp4")
        ActivityScenario.launch(HomeActivity::class.java).use {
            compose.onNodeWithTag("slate.home.tab.toolbox").performClick()
            compose.onNodeWithTag("slate.toolbox.${MediaStudioOps.TRIM}").performClick()
            compose.onNodeWithTag("slate.wizard.library").performScrollTo().performClick()
            val item = hasTestTag("slate.dialog.library.item") and hasText(library.name, substring = true)
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(item).performClick()
            compose.onNodeWithTag("slate.wizard.field.start").performScrollTo().performTextInput("500")
            compose.onNodeWithTag("slate.wizard.alt.length.duration").performScrollTo().performClick()
            compose.onNodeWithTag("slate.wizard.field.duration").performScrollTo().performTextInput("1500")
            compose.onNodeWithTag("slate.wizard.output").performScrollTo().performTextInput(target.path)
            compose.onNodeWithTag("slate.wizard.submit").performClick()
            val row = await({ StudioTasks.rows.lastOrNull { it.origin == MediaStudioContract.ORIGIN_STUDIO && it.output == target.path } }, TASK_TIMEOUT_MILLIS)
            assertEquals(row?.errorMessage, MediaStudioContract.TASK_COMPLETED, row?.state)
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(hasTestTag("slate.result.output.0") and hasText(target.path, substring = true)).fetchSemanticsNodes().isNotEmpty() }
            val request = row!!.request!!
            val script = CodeGenerator.wizardScript(request.schema, request.inputs, request.options, request.output)
            // The result page shows the complete script it sends. zh-CN: 结果页显示的就是它发送的完整脚本.
            compose.onNode(hasTestTag("slate.result.code") and hasText(script.code)).assertExists()
            assertEquals(AutoJs6Scripts.Transport.Inline, AutoJs6Scripts.transportOf(script.code))
            // performScrollTo only scrolls the closest scrollable parent, the horizontal button row; the page itself is
            // scrolled to its end, where the buttons are. zh-CN: performScrollTo 只滚动最近的可滚动父节点 (横向按钮行); 页面本身
            // 滚到末尾, 按钮在那里.
            compose.onNodeWithTag("slate.result").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, PAGE_END_PX) }
            val since = System.currentTimeMillis()
            compose.onNodeWithTag("slate.code.openInAutoJs6").assertIsDisplayed().performClick()
            evidence("inline") {
                awaitAccepted(since)
                awaitEditor()
            }
            record("inline", script, copy = null)
        }
    }

    @Test
    fun largeCodeOpensThroughAContentUri() {
        requireHost()
        val script = CodeGenerator.composeScript(timeline(CLIPS), "/sdcard/Movies/3-Slate Studio/open-in-autojs6.mp4")
        assertEquals(AutoJs6Scripts.Transport.ContentUri, AutoJs6Scripts.transportOf(script.code))
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            // The same button the result pages use. zh-CN: 与结果页相同的按钮.
            scenario.onActivity { it.setContent { OpenInAutoJs6Button(script, remember { SnackbarHostState() }) } }
            val since = System.currentTimeMillis()
            compose.onNodeWithTag("slate.code.openInAutoJs6").performClick()
            evidence("contentUri") {
                awaitAccepted(since)
                awaitEditor()
            }
            // The host read the URI into a private copy, which the editor keeps until the document is saved or closed.
            // zh-CN: 宿主把 URI 读成私有副本, 编辑器在文档保存或关闭前一直保留.
            val expected = md5(script.code)
            val host = AutoJs6Scripts.HOST_PACKAGE_NAME
            val copies = shell("run-as $host ls cache/create-script").lines().map { it.trim() }.filter { it.isNotEmpty() }
            assertTrue("no private copy in the host (a debug host is needed): $copies", copies.isNotEmpty() && copies.none { "not debuggable" in it })
            val copy = copies.firstOrNull { name -> shell("run-as $host md5sum cache/create-script/$name").startsWith(expected) }
            assertTrue("no host copy has MD5 $expected among $copies", copy != null)
            record("contentUri", script, copy)
        }
    }

    // region Helpers

    private fun <T> onMain(block: () -> T): T {
        var value: Result<T>? = null
        instrumentation.runOnMainSync { value = runCatching(block) }
        return value!!.getOrThrow()
    }

    private fun await(predicate: () -> TaskRow?, timeoutMillis: Long): TaskRow? {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            onMain(predicate)?.takeIf { it.isFinal }?.let { return it }
            SystemClock.sleep(POLL_MILLIS)
        }
        return null
    }

    /** Runs a shell command (no shell parsing: arguments are split at spaces). zh-CN: 运行 shell 命令 (不经 shell 解析, 按空格分割参数). */
    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** Waits for the host's answer logged after [since]; a refusal fails at once. zh-CN: 等待 [since] 之后记录的宿主应答; 拒绝则立即失败. */
    private fun awaitAccepted(since: Long) {
        val deadline = SystemClock.uptimeMillis() + HOST_TIMEOUT_MILLIS
        var answers = emptyList<String>()
        while (SystemClock.uptimeMillis() < deadline) {
            answers = shell("logcat -d -v epoch -s $HOST_TAG:I").lines().filter { line ->
                val time = line.trim().substringBefore(' ').toDoubleOrNull()
                time != null && time * 1000 >= since - CLOCK_SLACK_MILLIS && "CREATE_SCRIPT " in line
            }
            answers.firstOrNull { "CREATE_SCRIPT refused" in it }?.let { fail(it) }
            if (answers.any { "CREATE_SCRIPT accepted" in it }) return
            SystemClock.sleep(POLL_MILLIS)
        }
        fail("no CREATE_SCRIPT answer from the host within $HOST_TIMEOUT_MILLIS ms: $answers")
    }

    private fun awaitEditor() {
        val deadline = SystemClock.uptimeMillis() + HOST_TIMEOUT_MILLIS
        var resumed = emptyList<String>()
        while (SystemClock.uptimeMillis() < deadline) {
            resumed = shell("dumpsys activity activities").lines().filter { "ResumedActivity" in it }.map { it.trim() }
            if (resumed.any { EDITOR in it }) return
            SystemClock.sleep(POLL_MILLIS)
        }
        fail("the host editor is not on top: $resumed")
    }

    /**
     * Runs [checks]; when they fail, leaves a screenshot and the button's semantics for the driver first.
     * zh-CN: 运行 [checks]; 失败时先给驱动留下截图与按钮的语义信息.
     */
    private fun evidence(case: String, checks: () -> Unit) {
        try {
            checks()
        } catch (e: AssertionError) {
            runCatching { compose.onNodeWithTag("slate.code.openInAutoJs6").printToLog("SlateOpenInAutoJs6") }
            runCatching {
                val screen = instrumentation.uiAutomation.takeScreenshot()
                File(arguments.getString(ARG_OUT)!!, "open-in-autojs6-$case.png").outputStream().use { screen.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            throw e
        }
    }

    private fun md5(text: String): String =
        MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    /** Leaves a summary for the driver next to the wizard outputs. zh-CN: 在向导输出旁给驱动留一份摘要. */
    private fun record(case: String, script: GeneratedScript, copy: String?) {
        val summary = JSONObject()
            .put("case", case)
            .put("transport", AutoJs6Scripts.transportOf(script.code).javaClass.simpleName)
            .put("chars", script.code.length)
            .put("bytes", script.code.toByteArray(Charsets.UTF_8).size)
            .put("md5", md5(script.code))
            .put("suggestedName", script.suggestedName)
            .put("hostCopy", copy ?: JSONObject.NULL)
        File(arguments.getString(ARG_OUT)!!, "open-in-autojs6-$case.json").writeText(summary.toString())
    }

    private fun timeline(clips: Int): Map<String, Any?> = linkedMapOf(
        "width" to 1280L, "height" to 720L, "frameRate" to 30L,
        "video" to listOf(mapOf("clips" to List(clips) { index ->
            linkedMapOf<String, Any?>("src" to "/sdcard/Movies/3-Slate Studio/clip $index.mp4", "start" to 0L, "end" to 2000L, "volume" to 0.5,
                "transition" to if (index == 0) null else mapOf("type" to "dissolve", "duration" to 300L))
        })),
        "subtitles" to mapOf("cues" to List(clips) { index -> mapOf("start" to index * 1700L, "end" to index * 1700L + 1500L, "text" to "字幕 $index \"quoted\"") }),
    )

    // endregion

    private companion object {
        const val ARG_OPEN = "openInAutoJs6"
        const val ARG_OUT = "wizardOut"
        const val ARG_LIBRARY = "wizardLibrary"
        const val HOST_TAG = "CreateScriptActivity"
        const val EDITOR = "org.autojs.autojs.ui.edit.EditActivity"
        const val CLIPS = 1200
        const val POLL_MILLIS = 200L
        const val TASK_TIMEOUT_MILLIS = 180_000L
        const val UI_TIMEOUT_MILLIS = 15_000L
        const val HOST_TIMEOUT_MILLIS = 20_000L
        const val CLOCK_SLACK_MILLIS = 1_000L
        const val PAGE_END_PX = 100_000f
    }
}

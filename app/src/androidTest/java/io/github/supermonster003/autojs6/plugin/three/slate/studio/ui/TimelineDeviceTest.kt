package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.CodeGenerator
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Json
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditOps
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditorProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.OverlayModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.ProjectFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.TimelineJson
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.TransitionModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioTasks
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.TaskRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline.EditorModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline.TimelineActivity
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Roadmap P4.3 acceptance on a device, driven by `.python/device_timeline_check.py` and skipped without its arguments
 * (`timelineIn`: the fixtures, `timelineOut`: the output folder, `timelineShots`: screenshots). [timelineProject] builds
 * the 30 s project of the acceptance (3 clips with a dissolve and a slide, music with fades, imported subtitles, a text
 * and an image overlay) partly through the editor's controls, checks 12 undo / redo steps and the `.slate.json` round
 * trip, plays the preview, then exports through the export dialog and reports the result with its equivalent script,
 * which the driver runs in AutoJs6 to compare the outputs. [timelineBasics] (`timelineMode=basic`, for the API 24
 * emulator) only edits, plays and opens the dialogs.
 * zh-CN: 路线图 P4.3 真机验收, 由 `.python/device_timeline_check.py` 驱动, 缺少参数时跳过. [timelineProject] 部分经剪辑器控件
 * 搭建验收用的 30 秒工程 (3 段片段, 一个叠化一个推移, 带淡入淡出的音乐, 导入的字幕, 文字与图片叠加), 检查 12 步撤销 / 重做与
 * `.slate.json` 往返, 播放预览, 再经导出对话框导出并报告结果及等价脚本, 由驱动在 AutoJs6 中运行并比对输出. [timelineBasics]
 * (`timelineMode=basic`, 供 API 24 模拟器) 只做编辑, 播放与打开对话框.
 */
@RunWith(AndroidJUnit4::class)
class TimelineDeviceTest {

    // The v1 rule runs the screen's coroutines at once, as WizardDeviceTest does (the v2 rule queues them).
    // zh-CN: v1 规则立即执行界面协程, 与 WizardDeviceTest 相同 (v2 规则会排队).
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val arguments get() = InstrumentationRegistry.getArguments()

    private fun <T> onMain(block: () -> T): T {
        var value: Result<T>? = null
        instrumentation.runOnMainSync { value = runCatching(block) }
        return value!!.getOrThrow()
    }

    private fun waitFor(timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (onMain(condition)) return
            SystemClock.sleep(POLL_MILLIS)
        }
        throw AssertionError("timed out after $timeoutMillis ms")
    }

    private fun shot(folder: File?, name: String) {
        folder ?: return
        SystemClock.sleep(SETTLE_MILLIS)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        folder.mkdirs()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    /**
     * Clicks a control by its click action, so controls scrolled out of the inspector work too.
     * zh-CN: 经点击动作点按控件, 检查器中滚出视野的控件也可以.
     */
    private fun click(tag: String) {
        try {
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("no enabled $tag; tags: ${tags().joinToString(" ")}", e)
        }
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun tags(): List<String> = compose.onAllNodes(SemanticsMatcher("tagged") { SemanticsProperties.TestTag in it.config }, useUnmergedTree = true)
        .fetchSemanticsNodes().map { it.config[SemanticsProperties.TestTag] }.filter { it.startsWith("slate.timeline") }

    private fun clickInDialog(tag: String) = click(tag)

    private fun launch(): Pair<ActivityScenario<TimelineActivity>, EditorModel> {
        val scenario = ActivityScenario.launch(TimelineActivity::class.java)
        var model: EditorModel? = null
        scenario.onActivity { model = ViewModelProvider(it)[EditorModel::class.java] }
        compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(hasTestTag("slate.timeline")).fetchSemanticsNodes().isNotEmpty() }
        return scenario to model!!
    }

    @Test
    fun timelineProject() {
        val input = arguments.getString(ARG_IN)
        val output = arguments.getString(ARG_OUT)
        assumeTrue("run by .python/device_timeline_check.py", input != null && output != null && arguments.getString(ARG_MODE) != MODE_BASIC)
        val inputs = File(input!!)
        val outputs = File(output!!).apply { mkdirs() }
        val shots = arguments.getString(ARG_SHOTS)?.let(::File)
        val tag = arguments.getString(ARG_NAME) ?: "timeline"
        val export = arguments.getString(ARG_EXPORT) != "false"
        val report = JSONObject()
        val (scenario, model) = launch()
        var savedProject: File? = null
        try {
            shot(shots, "$tag-empty")
            // Three clips of 10.5 s: a dissolve and a slide of 750 ms give 30 s. zh-CN: 三段 10.5 秒, 叠化与推移各 750 ms, 共 30 秒.
            onMain { model.add(CLIPS.map { File(inputs, it).path }, EditorModel.AddKind.VIDEO) }
            waitFor(UI_TIMEOUT_MILLIS) { !model.busy && model.project.videoTracks[0].clips.size == CLIPS.size }
            val ids = onMain { model.project.videoTracks[0].clips.map { it.id } }
            onMain { ids.forEach { id -> model.edit { EditOps.trimVideoEnd(it, id, CLIP_MILLIS) } } }
            onMain { model.selected = ids[1] }
            compose.waitForIdle()
            click("slate.timeline.transition.dissolve")
            onMain { model.selected = ids[2] }
            compose.waitForIdle()
            click("slate.timeline.transition.slide")
            onMain {
                model.edit { EditOps.setTransition(it, ids[1], TransitionModel("dissolve", TRANSITION_MILLIS)) }
                model.edit { EditOps.setTransition(it, ids[2], TransitionModel("slide", TRANSITION_MILLIS, "left")) }
                model.selected = null
            }
            val videoEnd = onMain { model.project.videoPlacements.values.maxOf { it.endUs } / 1_000 }
            assertEquals(30_000L, videoEnd)

            onMain { model.add(listOf(File(inputs, MUSIC).path), EditorModel.AddKind.AUDIO) }
            waitFor(UI_TIMEOUT_MILLIS) { !model.busy && model.project.audioTracks[0].clips.isNotEmpty() }
            onMain {
                val music = model.project.audioTracks[0].clips[0].id
                model.edit { EditOps.trimAudioEnd(it, music, videoEnd) }
                model.edit { EditOps.setFades(it, music, 1_000, 1_500) }
                model.edit { EditOps.setVolume(it, music, 0.6) }
            }
            onMain { model.importSubtitles(File(inputs, SUBTITLES).path, append = false) }
            waitFor(UI_TIMEOUT_MILLIS) { !model.busy && model.project.subtitles.cues.isNotEmpty() }
            report.put("cues", onMain { model.project.subtitles.cues.size })
            // Adding selects the new clip; the add buttons are on the project panel. zh-CN: 添加会选中新片段; 添加按钮在工程面板上.
            onMain { model.selected = null }

            // A text overlay through the add button and the text dialog. zh-CN: 经添加按钮与文字对话框添加文字叠加.
            compose.waitForIdle()
            click("slate.timeline.add.text")
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(hasTestTag("slate.timeline.text.field")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("slate.timeline.text.field").performTextReplacement(TEXT)
            clickInDialog("slate.timeline.text.confirm")
            onMain {
                val text = model.project.overlays.single { it.type == OverlayModel.TEXT }
                assertEquals(TEXT, text.text)
                model.edit { EditOps.updateOverlay(it, text.id) { o -> o.copy(fromMs = 500, toMs = 6_000, y = 15.0, fontSize = 64.0) } }
                model.selected = null
            }
            onMain { model.add(listOf(File(inputs, LOGO).path), EditorModel.AddKind.IMAGE) }
            waitFor(UI_TIMEOUT_MILLIS) { model.project.overlays.any { it.type == OverlayModel.IMAGE } }
            onMain {
                val logo = model.project.overlays.single { it.type == OverlayModel.IMAGE }
                model.edit { EditOps.updateOverlay(it, logo.id) { o -> o.copy(fromMs = 0, toMs = videoEnd, x = 90.0, y = 12.0, scale = 0.5, alpha = 0.8) } }
                model.selected = null
            }

            // Undo / redo: 12 steps back and forth return the same project. zh-CN: 撤销 / 重做 12 步后回到同一工程.
            val before = onMain { model.project }
            val undone = onMain { (1..UNDO_STEPS).count { model.canUndo.also { can -> if (can) model.undo() } } }
            onMain { (1..UNDO_STEPS).forEach { _ -> model.redo() } }
            assertEquals(UNDO_STEPS, undone)
            assertEquals(before, onMain { model.project })
            report.put("undoSteps", undone)

            // The `.slate.json` round trip and the project store. zh-CN: `.slate.json` 往返与工程目录.
            val project: EditorProject = onMain { model.project }
            val file = File(outputs, "p43.slate.json")
            val infos = project.sources.associateWith { MediaProbe.probe(it) }
            ProjectFiles.save(file, project, ProjectFiles.EditorState(80.0, 12_000, ids[1]), infos)
            val (opened, state, _) = ProjectFiles.open(file.readText(), { MediaProbe.probe(it) })
            assertEquals(TimelineJson.write(project), TimelineJson.write(opened))
            assertEquals(12_000L, state.playheadMs)
            report.put("roundTrip", true)
            onMain { model.save() }
            waitFor(UI_TIMEOUT_MILLIS) { !model.busy && model.file != null }
            savedProject = onMain { model.file }
            report.put("savedProject", savedProject?.path)
            assertTrue(savedProject!!.isFile)

            // The preview: paused in the dissolve, then playing for 3 s. zh-CN: 预览: 暂停在叠化中, 然后播放 3 秒.
            val dissolveAt = onMain { model.project.videoPlacements.getValue(ids[1]).atMs + TRANSITION_MILLIS / 2.0 }
            onMain {
                model.selected = ids[1]
                model.preview.seek(dissolveAt)
            }
            compose.waitForIdle()
            SystemClock.sleep(PREVIEW_SETTLE_MILLIS)
            shot(shots, "$tag-editor")
            val playFrom = onMain { model.preview.positionMs }
            onMain { model.preview.play() }
            SystemClock.sleep(PLAY_MILLIS)
            val played = onMain { model.preview.positionMs - playFrom }
            onMain { model.preview.pause() }
            report.put("previewPlayedMs", played)
            assertTrue("preview clock advanced $played ms", played >= PLAY_MILLIS * 0.5)
            onMain {
                model.selected = null
                model.preview.seek(20_000.0)
            }
            compose.waitForIdle()
            SystemClock.sleep(PREVIEW_SETTLE_MILLIS)
            shot(shots, "$tag-project")

            val timeline = onMain { TimelineJson.write(model.project) }
            report.put("timeline", JSONObject(Json.write(timeline)))
            if (!export) return
            val target = File(outputs, "wizard-timeline.mp4")
            target.delete()
            onMain { model.setExport(model.project.export.copy(output = target.path)) }
            compose.waitForIdle()
            click("slate.timeline.export")
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(hasTestTag("slate.timeline.exportDialog.confirm") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            shot(shots, "$tag-export")
            val started = SystemClock.elapsedRealtime()
            clickInDialog("slate.timeline.exportDialog.confirm")
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(hasTestTag("slate.result")).fetchSemanticsNodes().isNotEmpty() }
            var row: TaskRow? = null
            waitFor(EXPORT_TIMEOUT_MILLIS) {
                row = StudioTasks.rows.firstOrNull { it.op == MediaStudioOps.COMPOSE && it.output == target.path }
                row?.isFinal == true
            }
            val finished = row!!
            report.put("exportMs", SystemClock.elapsedRealtime() - started)
            compose.waitForIdle()
            shot(shots, "$tag-result")
            val request = finished.request!!
            val code = CodeGenerator.wizardScript(request.schema, request.inputs, request.options, request.output).code
            report.put("state", finished.state)
            report.put("errorCode", finished.errorCode ?: JSONObject.NULL)
            report.put("errorMessage", finished.errorMessage ?: JSONObject.NULL)
            report.put("output", finished.output)
            report.put("code", code)
            report.put("requestCode", request.code)
            assertEquals("the result page shows the code of the request", request.code, code)
        } finally {
            File(outputs, "timeline-report.json").writeText(report.toString(2))
            savedProject?.delete()
            onMain { model.preview.pause() }
            scenario.close()
        }
    }

    @Test
    fun timelineBasics() {
        val input = arguments.getString(ARG_IN)
        assumeTrue("run by .python/device_timeline_check.py --mode basic", input != null && arguments.getString(ARG_MODE) == MODE_BASIC)
        val shots = arguments.getString(ARG_SHOTS)?.let(::File)
        val tag = arguments.getString(ARG_NAME) ?: "basic"
        val (scenario, model) = launch()
        try {
            onMain { model.add(listOf(File(input!!, CLIPS[0]).path, File(input, CLIPS[1]).path), EditorModel.AddKind.VIDEO) }
            waitFor(UI_TIMEOUT_MILLIS) { !model.busy && model.project.videoTracks[0].clips.size == 2 }
            val second = onMain { model.project.videoTracks[0].clips[1].id }
            onMain { model.selected = second }
            compose.waitForIdle()
            click("slate.timeline.transition.fade")
            onMain { model.preview.seek(model.project.videoPlacements.getValue(second).atMs + 2_000.0) }
            compose.waitForIdle()
            click("slate.timeline.transport.split")
            assertEquals(3, onMain { model.project.videoTracks[0].clips.size })
            click("slate.timeline.undo")
            assertEquals(2, onMain { model.project.videoTracks[0].clips.size })
            click("slate.timeline.redo")
            assertEquals(3, onMain { model.project.videoTracks[0].clips.size })
            onMain { model.preview.play() }
            SystemClock.sleep(PLAY_MILLIS)
            onMain { model.preview.pause() }
            onMain { model.selected = null }
            compose.waitForIdle()
            click("slate.timeline.zoomIn")
            click("slate.timeline.add.cue")
            clickInDialog("slate.timeline.text.confirm")
            assertEquals(1, onMain { model.project.subtitles.cues.size })
            click("slate.timeline.more")
            click("slate.timeline.menu.open")
            clickInDialog("slate.timeline.projects.confirm")
            shot(shots, "$tag-editor")
        } finally {
            onMain { model.preview.pause() }
            scenario.close()
        }
    }

    private companion object {
        const val ARG_IN = "timelineIn"
        const val ARG_OUT = "timelineOut"
        const val ARG_SHOTS = "timelineShots"
        const val ARG_NAME = "timelineName"
        const val ARG_MODE = "timelineMode"
        const val ARG_EXPORT = "timelineExport"
        const val MODE_BASIC = "basic"
        val CLIPS = listOf("clip1.mp4", "clip2.mp4", "clip3.mp4")
        const val MUSIC = "music.m4a"
        const val SUBTITLES = "subtitles.srt"
        const val LOGO = "logo.png"
        const val TEXT = "3-Slate P4.3"
        const val CLIP_MILLIS = 10_500L
        const val TRANSITION_MILLIS = 750L
        const val UNDO_STEPS = 12
        const val UI_TIMEOUT_MILLIS = 30_000L
        const val EXPORT_TIMEOUT_MILLIS = 600_000L
        const val POLL_MILLIS = 200L
        const val SETTLE_MILLIS = 400L
        const val PREVIEW_SETTLE_MILLIS = 1_500L
        const val PLAY_MILLIS = 3_000L
    }

}

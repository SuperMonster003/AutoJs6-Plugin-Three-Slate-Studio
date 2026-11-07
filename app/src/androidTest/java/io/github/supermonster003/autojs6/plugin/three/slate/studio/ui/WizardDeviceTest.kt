package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui

import android.media.MediaMetadataRetriever
import android.os.SystemClock
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.InputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionSchemas
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OutputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioTasks
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.TaskRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.WizardForm
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * Roadmap P4.2 acceptance on a device, driven by `.python/device_wizard_check.py` and skipped without its arguments
 * (`wizardIn`: the folder of the fixtures, `wizardOut`): every wizard runs once through the Studio queue the way its
 * screen submits it ([WizardForm] -> [StudioTasks.submit]), and the report lists each wizard's equivalent script call,
 * which the driver then runs in AutoJs6 and compares output by output; one wizard also runs through the screens.
 * zh-CN: 路线图 P4.2 的真机验收, 由 `.python/device_wizard_check.py` 驱动, 没有其参数时跳过 (`wizardIn`: 夹具目录,
 * `wizardOut`): 每个向导按其页面的提交方式 ([WizardForm] -> [StudioTasks.submit]) 经 Studio 队列运行一次, 报告列出每个向导的
 * 等价脚本调用, 驱动随后在 AutoJs6 中运行这些调用并逐个输出比对; 另有一个向导经页面操作完整走通.
 */
@RunWith(AndroidJUnit4::class)
class WizardDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private class Folders(val input: File, val output: File) {
        val video get() = File(input, VIDEO).path
        val video2 get() = File(input, VIDEO_2).path
        val audio get() = File(input, AUDIO).path
        val logo get() = File(input, LOGO).path
        val subtitles get() = File(input, SUBTITLES).path
    }

    private fun folders(): Folders {
        val arguments = InstrumentationRegistry.getArguments()
        val input = arguments.getString(ARG_IN)
        val output = arguments.getString(ARG_OUT)
        assumeTrue("run by .python/device_wizard_check.py", input != null && output != null)
        return Folders(File(input!!), File(output!!).apply { mkdirs() })
    }

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

    /** The output of one wizard in the report folder. zh-CN: 一个向导在报告目录中的输出. */
    private fun target(schema: OperationSchema, folder: File): String {
        val name = "wizard-" + schema.op
        return when (schema.output) {
            OutputKind.VIDEO -> "$folder/$name.mp4"
            OutputKind.AUDIO -> "$folder/$name." + if (schema.op == MediaStudioOps.AUDIO_CONVERT) "wav" else "m4a"
            OutputKind.IMAGE -> "$folder/$name." + if (schema.op == MediaStudioOps.THUMBNAIL) "jpg" else "png"
            OutputKind.GIF -> "$folder/$name.gif"
            OutputKind.SUBTITLE -> "$folder/$name.ass"
            OutputKind.FOLDER -> "$folder/$name"
        }
    }

    /** One filled form per wizard, as a user would fill it. zh-CN: 每个向导一份填好的表单, 如同用户填写. */
    private fun fill(form: WizardForm, files: Folders) {
        when (form.schema.op) {
            MediaStudioOps.CONCAT -> { form.addInput(files.video); form.addInput(files.video2) }
            MediaStudioOps.AUDIO_CONCAT -> { form.addInput(files.audio); form.addInput(files.audio) }
            MediaStudioOps.AUDIO_MIX -> { form.addInput(files.audio); form.addInput(files.video) }
            OperationSchema.SUBTITLES_CONVERT -> form.addInput(files.subtitles)
            else -> form.addInput(if (form.schema.inputs.kind == InputKind.VIDEO) files.video else files.audio)
        }
        when (form.schema.op) {
            MediaStudioOps.TRIM -> { form.set("start", "500"); form.alternatives["length"] = "duration"; form.set("duration", "1500") }
            MediaStudioOps.CONCAT -> { form.set("width", "320"); form.set("height", "180") }
            MediaStudioOps.SPEED -> form.set("factor", "2")
            MediaStudioOps.TRANSFORM -> { form.alternatives["size"] = "size"; form.set("width", "320"); form.set("height", "180"); form.set("flip", "horizontal") }
            MediaStudioOps.TRANSCODE -> { form.set("width", "480"); form.set("height", "270"); form.set("frameRate", "24") }
            MediaStudioOps.OVERLAY -> {
                form.addItem("overlays", "text", OVERLAY_LIMIT)
                form.addItem("overlays", "image", OVERLAY_LIMIT)
                val (text, image) = form.items.getValue("overlays")
                form.set("overlays#${text.id}/text", "3-Slate P4")
                form.set("overlays#${text.id}/color", "#FFCC00")
                form.set("overlays#${image.id}/src", files.logo)
                form.set("overlays#${image.id}/x", "75%")
                form.set("overlays#${image.id}/y", "10%")
            }
            MediaStudioOps.FRAMES -> { form.alternatives["sampling"] = "times"; form.set("times", "500, 1500, 2500") }
            MediaStudioOps.FRAME -> form.set("at", "1000")
            MediaStudioOps.THUMBNAIL -> form.set("at", "2000")
            MediaStudioOps.TO_GIF -> { form.set("end", "1000"); form.set("fps", "8"); form.set("width", "160") }
            MediaStudioOps.AUDIO_TRIM -> { form.set("start", "1000"); form.alternatives["length"] = "duration"; form.set("duration", "2000") }
            MediaStudioOps.AUDIO_MIX -> {
                val second = form.inputs[1].id
                form.set("tracks@$second/volume", "0.5")
                form.set("tracks@$second/at", "500")
            }
            MediaStudioOps.AUDIO_VOLUME -> { form.alternatives["level"] = "gain"; form.set("gain", "0.5"); form.set("fadeIn", "300") }
            MediaStudioOps.AUDIO_CONVERT -> form.set("format", "wav")
            MediaStudioOps.AUDIO_SPEED -> form.set("factor", "1.5")
            MediaStudioOps.BURN_SUBTITLES -> form.set("subtitles", files.subtitles)
            OperationSchema.SUBTITLES_CONVERT -> form.set("format", "ass")
        }
    }

    @Test
    fun everyWizardRunsThroughTheStudioQueue() {
        val files = folders()
        val report = JSONArray()
        val failures = mutableListOf<String>()
        for (schema in OptionSchemas.ALL) {
            val form = WizardForm(schema)
            fill(form, files)
            form.output = target(schema, files.output)
            val built = form.build()
            assertTrue("${schema.op}: ${built.issues}", built.valid)
            val request = form.request(built)
            val started = SystemClock.elapsedRealtime()
            val key = onMain { StudioTasks.submit(context, request) }
            val row = await({ StudioTasks.row(key) }, TASK_TIMEOUT_MILLIS)
            if (row?.state != MediaStudioContract.TASK_COMPLETED) failures += "${schema.op}: ${row?.state} ${row?.errorCode} ${row?.errorMessage}"
            report.put(JSONObject().apply {
                put("op", schema.op)
                put("kind", schema.output.name.lowercase())
                put("code", request.code)
                put("output", request.output)
                put("state", row?.state ?: "timeout")
                put("errorCode", row?.errorCode ?: JSONObject.NULL)
                put("errorMessage", row?.errorMessage ?: JSONObject.NULL)
                put("millis", SystemClock.elapsedRealtime() - started)
            })
        }
        File(files.output, REPORT).writeText(report.toString(1))
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun trimWizardRunsThroughTheScreens() {
        val files = folders()
        // The library dialog lists the media of the library folders; the driver puts a clip into one and scans it.
        // zh-CN: 媒体库对话框列出媒体库目录中的媒体; 驱动把一段片段放进其中一个目录并扫描.
        val library = InstrumentationRegistry.getArguments().getString(ARG_LIBRARY)
        assumeTrue("run by .python/device_wizard_check.py", library != null)
        val source = File(library!!)
        val target = File(files.output, "screens-trim.mp4")
        ActivityScenario.launch(HomeActivity::class.java).use {
            compose.onNodeWithTag("slate.home.tab.toolbox").performClick()
            compose.onNodeWithTag("slate.toolbox.${MediaStudioOps.TRIM}").performClick()
            compose.onNodeWithTag("slate.wizard.library").performScrollTo().performClick()
            val item = hasTestTag("slate.dialog.library.item") and hasText(source.name, substring = true)
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(item).performClick()
            compose.onNodeWithTag("slate.wizard.field.start").performScrollTo().performTextInput("500")
            compose.onNodeWithTag("slate.wizard.alt.length.duration").performScrollTo().performClick()
            compose.onNodeWithTag("slate.wizard.field.duration").performScrollTo().performTextInput("1500")
            compose.onNodeWithTag("slate.wizard.output").performScrollTo().performTextInput(target.path)
            compose.onNodeWithTag("slate.wizard.submit").performClick()
            val row = await({ StudioTasks.rows.lastOrNull { it.origin == MediaStudioContract.ORIGIN_STUDIO && it.output == target.path } }, TASK_TIMEOUT_MILLIS)
            assertEquals(row?.errorMessage, MediaStudioContract.TASK_COMPLETED, row?.state)
            // The library answers the storage path (/storage/emulated/0/...), the driver names it through /sdcard.
            // zh-CN: 媒体库给出存储路径 (/storage/emulated/0/...), 驱动经 /sdcard 指定.
            val input = row!!.request!!.inputs.single()
            assertTrue(input, input.endsWith("/" + source.name))
            assertEquals("video.trim(\"$input\", { start: 500, duration: 1500 }, \"${target.path}\");", row.request!!.code)
            compose.waitUntil(UI_TIMEOUT_MILLIS) { compose.onAllNodes(hasTestTag("slate.result.output.0") and hasText(target.path, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        }
        val duration = MediaMetadataRetriever().run {
            try {
                setDataSource(target.path)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            } finally {
                release()
            }
        }
        assertTrue("duration $duration", abs(duration - 1500) <= DURATION_TOLERANCE_MILLIS)
    }

    private companion object {
        const val ARG_IN = "wizardIn"
        const val ARG_OUT = "wizardOut"
        const val ARG_LIBRARY = "wizardLibrary"
        const val REPORT = "wizard-report.json"
        const val VIDEO = "video.mp4"
        const val VIDEO_2 = "video2.mp4"
        const val AUDIO = "audio.m4a"
        const val LOGO = "logo.png"
        const val SUBTITLES = "cues.srt"
        const val OVERLAY_LIMIT = 64
        const val POLL_MILLIS = 200L
        const val TASK_TIMEOUT_MILLIS = 180_000L
        const val UI_TIMEOUT_MILLIS = 15_000L
        const val DURATION_TOLERANCE_MILLIS = 120L
    }
}

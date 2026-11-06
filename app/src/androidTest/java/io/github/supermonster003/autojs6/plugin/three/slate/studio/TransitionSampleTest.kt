package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.os.Bundle
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures.MediaFixtures
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Roadmap P3.2: transition samples to look at on a device. Joins the color bars (2 s) and the reversed color bars (2 s)
 * with each transition of D30 (1 s; `slide` and `wipe` once to the left and once upwards), so every sample lasts 3 s
 * (`cut` and `fade` 4 s), and copies them to the app specific Movies folder. Runs only with the instrumentation argument
 * `transitionSamples=true` (`.python/transition_samples.py`); each sample is reported as an instrumentation status with
 * the key `transition` (duration, size, warnings such as a GPU fallback).
 * zh-CN: 路线图 P3.2: 供真机目视的转场样本. 用 D30 的每种转场 (1 秒; `slide` 与 `wipe` 各做向左与向上两次) 连接彩条 (2 秒)
 * 与反序彩条 (2 秒), 每个样本 3 秒 (`cut` 与 `fade` 为 4 秒), 复制到应用专属 Movies 目录. 仅在仪器参数
 * `transitionSamples=true` 时运行 (`.python/transition_samples.py`); 每个样本以键 `transition` 的仪器状态报告 (时长, 尺寸,
 * GPU 回退等警告).
 */
@RunWith(AndroidJUnit4::class)
class TransitionSampleTest : MediaStudioTestBase() {

    @Test
    fun everyTransition() {
        assumeTrue("set the instrumentation argument transitionSamples=true", InstrumentationRegistry.getArguments().getString("transitionSamples") == "true")
        val first = MediaFixtures.colorBars(File(directory, "inputs/bars-2s.mp4"), MediaFixtures.BARS, 2_000_000L)
        val second = MediaFixtures.colorBars(File(directory, "inputs/bars-reversed-2s.mp4"), MediaFixtures.BARS.reversedArray(), 2_000_000L)
        val target = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!.apply { mkdirs() }
        withMediaStudio { plugin ->
            SAMPLES.forEach { (type, direction) ->
                val name = "3-slate-${type}${direction?.let { "-$it" } ?: ""}.mp4"
                val transition = options("type" to type, "duration" to 1_000).apply { direction?.let { putString("direction", it) } }
                val timeline = options(
                    "video" to listOf(options("clips" to listOf(options("src" to first.path), options("src" to second.path, "transition" to transition)))),
                )
                val request = request(MediaStudioOps.COMPOSE, emptyList(), output(name)).apply { putBundle(MediaStudioRequestKeys.TIMELINE, timeline) }
                val result = run(plugin, request)
                output(name).copyTo(File(target, name), overwrite = true)
                report(name, result)
            }
        }
        InstrumentationRegistry.getInstrumentation().sendStatus(STATUS_CODE, Bundle().apply { putString("transition", "name=folder;path=${target.path}") })
    }

    private fun report(name: String, result: Bundle) {
        val values = listOf(
            "name" to name,
            "durationMillis" to result.getLong(MediaStudioResultKeys.DURATION_MILLIS),
            "size" to "${result.getInt(MediaStudioResultKeys.WIDTH)}x${result.getInt(MediaStudioResultKeys.HEIGHT)}",
            "warnings" to result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().joinToString(" | "),
        )
        val line = values.joinToString(";") { (key, value) -> "$key=${value.toString().replace(';', ',').replace('\n', ' ')}" }
        InstrumentationRegistry.getInstrumentation().sendStatus(STATUS_CODE, Bundle().apply { putString("transition", line) })
    }

    private companion object {
        const val STATUS_CODE = 2

        val SAMPLES: List<Pair<String, String?>> = listOf(
            "cut" to null,
            "fade" to null,
            "dissolve" to null,
            "slide" to "left",
            "slide" to "up",
            "wipe" to "left",
            "wipe" to "up",
            "zoom" to null,
            "blur" to null,
        )
    }

}

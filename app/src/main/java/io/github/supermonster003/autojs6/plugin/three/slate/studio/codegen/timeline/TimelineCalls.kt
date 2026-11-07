package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.timeline

import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.CodeGenerator
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.InputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.InputSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationCategory
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OutputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.Slot
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditorProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.TimelineJson
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioRequest
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import org.autojs.plugin.mediastudio.api.MediaStudioOps

/**
 * The timeline export of the editor as a Studio request and as the equivalent script (roadmap P4.3, D11): the request
 * carries the timeline object under the option key [TIMELINE] (the task queue sends it as the contract's `timeline`)
 * plus `codec` / `bitRate`, and its code is `video.compose(timeline, output, options)` from the code generator.
 * zh-CN: 编辑器的时间线导出, 作为 Studio 请求与等价脚本 (路线图 P4.3, D11): 请求在选项键 [TIMELINE] 下带上时间线对象 (任务队列把它作为
 * 契约的 `timeline` 发送), 另有 `codec` / `bitRate`; 代码为代码生成器给出的 `video.compose(timeline, output, options)`.
 */
internal object TimelineCalls {

    const val TIMELINE = "timeline"

    /** `compose` for the task queue and the result page; not a toolbox wizard. zh-CN: 供任务队列与结果页使用的 `compose`; 不是工具箱向导. */
    val SCHEMA = OperationSchema(
        op = MediaStudioOps.COMPOSE,
        module = "video",
        method = "compose",
        category = OperationCategory.VIDEO,
        inputs = InputSpec(0, MediaStudioLimits.MAX_INPUTS, InputKind.MEDIA),
        fields = emptyList(),
        output = OutputKind.VIDEO,
        slots = listOf(Slot.OPTIONS, Slot.OUTPUT),
    )

    /** The `options` argument of `video.compose`. zh-CN: `video.compose` 的 `options` 参数. */
    fun options(project: EditorProject): LinkedHashMap<String, Any?> = linkedMapOf<String, Any?>().apply {
        if (project.export.codec != "h264") put("codec", project.export.codec)
        project.export.bitRate?.let { put("bitRate", it) }
    }

    fun code(timeline: Map<String, Any?>, output: String?, options: Map<String, Any?>): String =
        CodeGenerator.composeScript(timeline, output, options).code

    fun request(project: EditorProject): StudioRequest {
        val timeline = TimelineJson.write(project)
        val options = options(project)
        val output = project.export.output?.takeIf { it.isNotBlank() }
        return StudioRequest(
            schema = SCHEMA,
            inputs = emptyList(),
            options = linkedMapOf<String, Any?>(TIMELINE to timeline).apply { putAll(options) },
            output = output,
            code = code(timeline, output, options),
        )
    }

}

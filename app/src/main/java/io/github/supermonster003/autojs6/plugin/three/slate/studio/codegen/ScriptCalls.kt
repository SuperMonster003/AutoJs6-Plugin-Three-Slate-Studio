package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import org.autojs.plugin.mediastudio.api.MediaStudioOps

/**
 * The script call equivalent to a wizard request (roadmap D11, D35; the complete script for the result page and "open
 * in AutoJs6" is [CodeGenerator.wizardScript], roadmap P4.4). [options] are in the contract shape the wizard submits (`overlays`, `at`, `subtitles`, `style`,
 * `tracks` included); the call moves them into the argument slots of the script API (roadmap appendix B, the host
 * mapping of appendix D Q27 (a)). An absent [output] means the default output (D14), which the call leaves out.
 * zh-CN: 与向导请求等价的脚本调用 (路线图 D11, D35; 结果页与 "在 AutoJs6 中打开" 使用的完整脚本为 [CodeGenerator.wizardScript],
 * 路线图 P4.4). [options] 为向导
 * 提交的契约形态 (含 `overlays`, `at`, `subtitles`, `style`, `tracks`); 调用把它们放进脚本 API 的参数位置 (路线图附录 B, 附录 D
 * Q27 (a) 的宿主映射). [output] 为空表示默认输出 (D14), 调用中省略.
 */
internal object ScriptCalls {

    fun code(schema: OperationSchema, inputs: List<String>, options: Map<String, Any?>, output: String?): String {
        val present = options.filterValues { it != null }
        return when (schema.op) {
            MediaStudioOps.FRAME -> frame(schema, inputs, present, output)
            OperationSchema.SUBTITLES_CONVERT -> subtitlesConvert(inputs, present, output)
            else -> CodeGenerator.call(schema.module, schema.method, *arguments(schema, inputs, present, output).toTypedArray())
        }
    }

    /**
     * The arguments of the script call in slot order; [options] without null values.
     * zh-CN: 按参数位置排列的脚本调用参数; [options] 不含 null 值.
     */
    internal fun arguments(schema: OperationSchema, inputs: List<String>, options: Map<String, Any?>, output: String?): List<Any?> {
        val pulled = setOf("overlays", "at", "subtitles", "style", "tracks").filter { key ->
            when (key) {
                "overlays" -> Slot.OVERLAYS in schema.slots
                "at" -> Slot.AT in schema.slots
                "subtitles" -> Slot.SUBTITLES in schema.slots
                "style" -> Slot.STYLE in schema.slots
                else -> Slot.MIX_INPUTS in schema.slots
            }
        }.toSet()
        val rest = options.filterKeys { it !in pulled }
        val arguments = ArrayList<Any?>()
        for (slot in schema.slots) {
            when (slot) {
                Slot.INPUT -> arguments += inputs.single()
                Slot.INPUTS -> arguments.add(inputs)
                Slot.MIX_INPUTS -> arguments.add(mixInputs(inputs, options["tracks"]))
                Slot.OVERLAYS -> arguments += options["overlays"]
                Slot.AT -> arguments += options["at"]
                Slot.SUBTITLES -> arguments += options["subtitles"]
                Slot.STYLE -> (options["style"] as? Map<*, *>)?.takeIf { it.isNotEmpty() }?.let { arguments += it }
                Slot.OPTIONS -> if (rest.isNotEmpty()) arguments += rest
                Slot.OUTPUT -> if (output != null) arguments += output
            }
        }
        return arguments
    }

    /** A plain path for an input without settings, `{src, volume, at}` otherwise. zh-CN: 无设置的输入为路径, 否则为 `{src, volume, at}`. */
    private fun mixInputs(inputs: List<String>, tracks: Any?): List<Any> {
        val settings = (tracks as? List<*>).orEmpty()
        return inputs.mapIndexed { index, path ->
            val track = (settings.getOrNull(index) as? Map<*, *>)?.filterValues { it != null }.orEmpty()
            if (track.isEmpty()) path else linkedMapOf<String, Any?>("src" to path).apply { track.forEach { (k, v) -> put(k.toString(), v) } }
        }
    }

    /**
     * `video.frame` answers an `Image`; the wizard writes it to [output], so the call saves and recycles it.
     * zh-CN: `video.frame` 返回 `Image`; 向导把它写到 [output], 所以调用保存并回收它.
     */
    private fun frame(schema: OperationSchema, inputs: List<String>, options: Map<String, Any?>, output: String?): String {
        val call = CodeGenerator.call(schema.module, schema.method, *arguments(schema, inputs, options, null).toTypedArray())
        val target = output ?: return call
        return listOf("let image = $call", saveImage(options, target), "image.recycle();").joinToString("\n")
    }

    /** `images.save` of the `video.frame` image in the format of [options]. zh-CN: 按 [options] 的格式保存 `video.frame` 的图片. */
    internal fun saveImage(options: Map<String, Any?>, target: String): String {
        val format = (options["format"] as? String)?.lowercase() ?: "png"
        if (format == "png") return "images.save(image, ${CodeGenerator.literal(target)});"
        val quality = (options["quality"] as? Number)?.toInt() ?: io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions.DEFAULT_IMAGE_QUALITY
        return "images.save(image, ${CodeGenerator.literal(target)}, \"jpg\", $quality);"
    }

    private fun subtitlesConvert(inputs: List<String>, options: Map<String, Any?>, output: String?): String {
        val format = options["format"] as? String
        val write = listOfNotNull(CodeGenerator.literal(output), "cues", format?.let { CodeGenerator.literal(it) }).joinToString(", ")
        return listOf(
            "let cues = video.subtitles.parse(${CodeGenerator.literal(inputs.single())});",
            "video.subtitles.write($write);",
        ).joinToString("\n")
    }

}

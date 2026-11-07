package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import org.autojs.plugin.mediastudio.api.MediaStudioOps

/**
 * A complete script (roadmap D11, D36): [code] runs as is in AutoJs6, [suggestedName] is the file name offered to the
 * host's "new script" entry (roadmap D19) and [api] names the call it makes, for example `video.trim`.
 * zh-CN: 一个完整的脚本 (路线图 D11, D36): [code] 可在 AutoJs6 中直接运行, [suggestedName] 是交给宿主 "新建脚本" 入口 (路线图 D19)
 * 的建议文件名, [api] 为其调用的方法, 例如 `video.trim`.
 */
internal data class GeneratedScript(val code: String, val suggestedName: String, val api: String)

/**
 * Turns API calls into the equivalent JavaScript source (roadmap D11, D36, P4.4). Arguments are serialized like
 * `JSON.stringify`: strings are escaped, paths keep their original value, maps become object literals in insertion
 * order and `null` values of maps are dropped; a statement longer than [LINE_WIDTH] breaks its arguments across
 * indented lines. The option shapes are those of `OptionSchemas`, the source of the d.ts declarations
 * (`MediaStudioDeclarations`), and [ScriptCalls] places them into the argument slots of the script API.
 *
 * Complete scripts: [wizardScript] for the toolbox wizards (roadmap P4.2), [composeScript] / [composeCall] for timelines
 * (roadmap P3.1, used by the timeline editor of P4.3) and [recordScript] for the Studio recorder (roadmap P4.5). Each
 * starts with a comment naming the call and logs what the call returns.
 * zh-CN: 把 API 调用转为等价的 JavaScript 源码 (路线图 D11, D36, P4.4). 参数像 `JSON.stringify` 一样序列化: 字符串转义, 路径保留
 * 原值, Map 按插入顺序转为对象字面量, Map 中的 `null` 值被省略; 超过 [LINE_WIDTH] 的语句把参数分行缩进. 选项形态取自 d.ts 声明
 * (`MediaStudioDeclarations`) 的同一来源 `OptionSchemas`, 由 [ScriptCalls] 放进脚本 API 的参数位置.
 *
 * 完整脚本: 工具箱向导用 [wizardScript] (路线图 P4.2), 时间线用 [composeScript] / [composeCall] (路线图 P3.1, 供 P4.3 的时间线剪辑器使用),
 * Studio 录音用 [recordScript] (路线图 P4.5). 每个脚本以注明调用的注释开头, 并输出调用的返回值.
 */
internal object CodeGenerator {

    val MODULES: Set<String> = linkedSetOf("video", "audio")

    /** Statements and literals longer than this break across lines. zh-CN: 超过此宽度的语句与字面量分行书写. */
    const val LINE_WIDTH = 100

    /** The recording length of [recordScript] when the caller gives none. zh-CN: 调用方未给出时 [recordScript] 的录音时长. */
    /** The option key of the timeline in a compose request of the editor (P4.3). zh-CN: 剪辑器 compose 请求中时间线的选项键. */
    const val TIMELINE = "timeline"

    const val DEFAULT_RECORD_MILLIS = 10_000L

    private const val INDENT = "  "
    private const val NL = "\n"

    private val LINE_SEPARATOR = Char(0x2028)
    private val PARAGRAPH_SEPARATOR = Char(0x2029)

    private val IDENTIFIER = Regex("[A-Za-z_$][A-Za-z0-9_$]*")

    // region Calls and literals

    /**
     * `module.method(arguments);` on one line. [method] may be a dotted path (`subtitles.write`).
     * zh-CN: 单行的 `module.method(arguments);`. [method] 可以是带点的路径 (`subtitles.write`).
     */
    fun call(module: String, method: String, vararg arguments: Any?): String {
        val callee = callee(module, method)
        return arguments.joinToString(", ", prefix = "$callee(", postfix = ");") { literal(it) }
    }

    /**
     * `prefix module.method(arguments);`, with one argument per indented line when the single line would be longer than
     * [LINE_WIDTH]; object and array arguments then break the same way ([pretty]).
     * zh-CN: `prefix module.method(arguments);`, 单行超过 [LINE_WIDTH] 时每个参数占一行并缩进; 对象与数组参数按同样方式分行 ([pretty]).
     */
    fun statement(prefix: String, module: String, method: String, arguments: List<Any?>): String {
        val callee = callee(module, method)
        val single = arguments.joinToString(", ", prefix = "$prefix$callee(", postfix = ");") { literal(it) }
        if (single.length <= LINE_WIDTH || arguments.isEmpty()) return single
        return arguments.joinToString(",$NL", prefix = "$prefix$callee($NL", postfix = "$NL);") { INDENT + pretty(it, INDENT.length) }
    }

    fun literal(value: Any?): String = when (value) {
        null -> "null"
        is Raw -> value.source
        is String -> string(value)
        is Boolean -> value.toString()
        is Int, is Long, is Short, is Byte -> value.toString()
        is Float -> number(value.toDouble())
        is Double -> number(value)
        is Map<*, *> -> entries(value)
            .joinToString(", ", prefix = "{ ", postfix = " }") { (key, item) -> key(key) + ": " + literal(item) }
            .let { if (it == "{  }") "{}" else it }
        is Iterable<*> -> value.joinToString(", ", prefix = "[", postfix = "]") { literal(it) }
        is Array<*> -> literal(value.asList())
        is LongArray -> literal(value.asList())
        is IntArray -> literal(value.asList())
        is DoubleArray -> literal(value.asList())
        is FloatArray -> literal(value.asList())
        is BooleanArray -> literal(value.asList())
        else -> throw IllegalArgumentException("unsupported value type: ${value::class.java.name}")
    }

    /**
     * [literal], or for objects and arrays that do not fit in [LINE_WIDTH] at column [column], one entry per line
     * indented by two spaces more than [column]; no trailing commas.
     * zh-CN: [literal]; 在第 [column] 列放不下 [LINE_WIDTH] 的对象与数组改为每项一行, 比 [column] 多缩进两格; 不带末尾逗号.
     */
    fun pretty(value: Any?, column: Int = 0): String {
        val single = literal(value)
        if (column + single.length <= LINE_WIDTH) return single
        val inner = " ".repeat(column) + INDENT
        val close = " ".repeat(column)
        return when (value) {
            is Map<*, *> -> {
                val items = entries(value)
                if (items.isEmpty()) single
                else items.joinToString(",$NL", prefix = "{$NL", postfix = "$NL$close}") { (key, item) ->
                    val name = key(key) + ": "
                    inner + name + pretty(item, inner.length + name.length)
                }
            }
            is Iterable<*>, is Array<*>, is LongArray, is IntArray, is DoubleArray, is FloatArray, is BooleanArray -> {
                val items = asList(value)
                if (items.isEmpty()) single
                else items.joinToString(",$NL", prefix = "[$NL", postfix = "$NL$close]") { inner + pretty(it, inner.length) }
            }
            else -> single
        }
    }

    private fun callee(module: String, method: String): String {
        require(module in MODULES) { "unknown module: $module" }
        require(method.split('.').all { IDENTIFIER.matches(it) }) { "invalid method name: $method" }
        return "$module.$method"
    }

    private fun entries(map: Map<*, *>): List<Pair<Any?, Any?>> = map.entries.filter { it.value != null }.map { it.key to it.value }

    private fun asList(value: Any): List<Any?> = when (value) {
        is Iterable<*> -> value.toList()
        is Array<*> -> value.asList()
        is LongArray -> value.asList()
        is IntArray -> value.asList()
        is DoubleArray -> value.asList()
        is FloatArray -> value.asList()
        is BooleanArray -> value.asList()
        else -> listOf(value)
    }

    private fun key(key: Any?): String {
        val text = key as? String ?: throw IllegalArgumentException("object keys must be strings: $key")
        return if (IDENTIFIER.matches(text)) text else string(text)
    }

    private fun number(value: Double): String {
        require(value.isFinite()) { "numbers must be finite: $value" }
        return if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) value.toLong().toString() else value.toString()
    }

    private fun string(value: String): String = buildString(value.length + 2) {
        append('"')
        for (char in value) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                Char(0x0C) -> append("\\f")
                // Line and paragraph separators end a line in older engines. zh-CN: 旧引擎把行分隔符与段分隔符视为换行.
                LINE_SEPARATOR, PARAGRAPH_SEPARATOR -> append(unicodeEscape(char))
                else -> if (char < ' ') append(unicodeEscape(char)) else append(char)
            }
        }
        append('"')
    }

    private fun unicodeEscape(char: Char): String = "\\u" + char.code.toString(16).padStart(4, '0')

    // endregion

    // region Complete scripts

    /**
     * The script of a wizard request (roadmap P4.2 / P4.4): the call of [ScriptCalls] with its result logged; `frame`
     * saves and recycles its image, the subtitle conversion parses and writes, `frames` logs the paths. A timeline export
     * of the editor (roadmap P4.3: op `compose`, the timeline in `options.timeline`, `codec` / `bitRate` beside it) is
     * [composeScript].
     * zh-CN: 向导请求的脚本 (路线图 P4.2 / P4.4): [ScriptCalls] 的调用并输出结果; `frame` 保存并回收图片, 字幕转换先解析再写出,
     * `frames` 输出路径. 剪辑器的时间线导出 (路线图 P4.3: op 为 `compose`, 时间线在 `options.timeline`, 旁边是 `codec` /
     * `bitRate`) 即 [composeScript].
     */
    fun wizardScript(schema: OperationSchema, inputs: List<String>, options: Map<String, Any?>, output: String?): GeneratedScript {
        if (schema.op == MediaStudioOps.COMPOSE) {
            @Suppress("UNCHECKED_CAST")
            val timeline = requireNotNull(options[TIMELINE] as? Map<String, Any?>) { "a compose request carries options.$TIMELINE" }
            return composeScript(timeline, output, options - TIMELINE)
        }
        val present = options.filterValues { it != null }
        val body = when (schema.op) {
            MediaStudioOps.FRAME -> buildList {
                add(statement("let image = ", schema.module, schema.method, ScriptCalls.arguments(schema, inputs, present, null)))
                if (output != null) add(ScriptCalls.saveImage(present, output)) else add("console.log(image.width + \"x\" + image.height);")
                add("image.recycle();")
            }
            OperationSchema.SUBTITLES_CONVERT -> listOf(
                statement("let cues = ", "video", "subtitles.parse", listOf(inputs.single())),
                statement("let result = ", "video", "subtitles.write", listOf(output, Raw("cues")) + listOfNotNull(present["format"])),
                "console.log(result.path);",
            )
            MediaStudioOps.FRAMES -> listOf(
                statement("let paths = ", schema.module, schema.method, ScriptCalls.arguments(schema, inputs, present, output)),
                "console.log(paths.join(\", \"));",
            )
            else -> listOf(
                statement("let result = ", schema.module, schema.method, ScriptCalls.arguments(schema, inputs, present, output)),
                "console.log(result.path);",
            )
        }
        val api = if (schema.op == OperationSchema.SUBTITLES_CONVERT) "video.subtitles.parse / video.subtitles.write" else "${schema.module}.${schema.method}"
        val name = if (schema.op == OperationSchema.SUBTITLES_CONVERT) "subtitles-convert" else "${schema.module}-${schema.method.replace('.', '-')}"
        return GeneratedScript(script(api, body), "$name.js", api)
    }

    /**
     * The statement `video.compose(timeline, output, options);` of a timeline (roadmap appendix B.4): [output] null is
     * the default output (written as `null` only when [options] follow), [options] holds `codec` / `bitRate`.
     * zh-CN: 时间线 (路线图附录 B.4) 的语句 `video.compose(timeline, output, options);`: [output] 为 null 表示默认输出 (只在后面有
     * [options] 时写成 `null`), [options] 为 `codec` / `bitRate`.
     */
    fun composeCall(timeline: Map<String, Any?>, output: String? = null, options: Map<String, Any?> = emptyMap(), prefix: String = ""): String =
        statement(prefix, "video", "compose", composeArguments(timeline, output, options))

    /** The complete script of a timeline export. zh-CN: 时间线导出的完整脚本. */
    fun composeScript(timeline: Map<String, Any?>, output: String? = null, options: Map<String, Any?> = emptyMap()): GeneratedScript =
        GeneratedScript(
            script("video.compose", listOf(composeCall(timeline, output, options, prefix = "let result = "), "console.log(result.path);")),
            "video-compose.js",
            "video.compose",
        )

    /**
     * The script of a Studio recording (roadmap P4.5, appendix B.2): `audio.record(path, options)`, then it waits
     * [durationMillis] and stops; [path] null is the default output.
     * zh-CN: Studio 录音的脚本 (路线图 P4.5, 附录 B.2): `audio.record(path, options)`, 等待 [durationMillis] 后停止; [path] 为 null
     * 表示默认输出.
     */
    fun recordScript(path: String?, options: Map<String, Any?> = emptyMap(), durationMillis: Long = DEFAULT_RECORD_MILLIS): GeneratedScript {
        require(durationMillis > 0) { "durationMillis must be positive: $durationMillis" }
        val present = options.filterValues { it != null }
        val arguments = when {
            present.isNotEmpty() -> listOf(path, present)
            path != null -> listOf(path)
            else -> emptyList()
        }
        val body = listOf(
            statement("let recording = ", "audio", "record", arguments),
            "sleep($durationMillis);",
            "let result = recording.stop();",
            "console.log(result.path);",
        )
        return GeneratedScript(script("audio.record", body), "audio-record.js", "audio.record")
    }

    private fun composeArguments(timeline: Map<String, Any?>, output: String?, options: Map<String, Any?>): List<Any?> {
        val present = options.filterValues { it != null }
        return when {
            present.isNotEmpty() -> listOf(timeline, output, present)
            output != null -> listOf(timeline, output)
            else -> listOf(timeline)
        }
    }

    private fun script(api: String, body: List<String>): String = buildString {
        append("// Generated by 3-Slate Studio: ").append(api).append(NL)
        append("// Runs in AutoJs6 with the 3-Slate Studio plugin enabled in the Plugin Center.").append(NL)
        append(NL)
        body.forEach { append(it).append(NL) }
    }

    // endregion

}

/** Source text written as is (a variable name). zh-CN: 原样写出的源码 (变量名). */
internal data class Raw(val source: String)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import java.io.File

/**
 * A Studio project file `.slate.json` (roadmap D33): the `compose` timeline object itself plus `version` (the file
 * format, [VERSION]) and `ui` (editor state such as zoom and selection, kept as it is). Reading checks the timeline
 * with [TimelineOptions] (messages name the field path from `timeline.`), so a project that opens also composes.
 * Numbers follow [BundleValues]: integers as Long, other numbers as Double.
 * zh-CN: Studio 工程文件 `.slate.json` (路线图 D33): 即 `compose` 的时间线对象本身, 另加 `version` (文件格式, [VERSION]) 与 `ui`
 * (缩放, 选中等编辑器状态, 原样保存). 读取时用 [TimelineOptions] 校验时间线 (消息给出自 `timeline.` 起的字段路径), 因此能打开的
 * 工程也能合成. 数字与 [BundleValues] 一致: 整数为 Long, 其他数字为 Double.
 */
internal data class SlateProject(val timeline: Map<String, Any?>, val ui: Map<String, Any?> = emptyMap()) {

    /** The checked timeline. zh-CN: 经校验的时间线. */
    val spec: TimelineSpec get() = TimelineOptions.parse(timeline)

    fun toJson(): String = Json.write(LinkedHashMap<String, Any?>().apply {
        put(KEY_VERSION, VERSION.toLong())
        putAll(timeline)
        if (ui.isNotEmpty()) put(KEY_UI, ui)
    })

    fun write(file: File) {
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        temporary.writeText(toJson(), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            file.delete()
            if (!temporary.renameTo(file)) throw MediaStudioException.io("cannot write ${file.path}")
        }
    }

    companion object {

        const val EXTENSION = ".slate.json"
        const val VERSION = 1
        const val KEY_VERSION = "version"
        const val KEY_UI = "ui"

        fun read(file: File): SlateProject = parse(file.readText(Charsets.UTF_8))

        fun parse(text: String): SlateProject {
            val root = try {
                Json.parse(text)
            } catch (e: IllegalArgumentException) {
                throw MediaStudioException.invalid("not a 3-Slate Studio project: ${e.message}")
            }
            @Suppress("UNCHECKED_CAST")
            val map = root as? Map<String, Any?> ?: throw MediaStudioException.invalid("not a 3-Slate Studio project: the JSON is not an object")
            when (val version = map[KEY_VERSION]) {
                null -> Unit
                is Long -> if (version > VERSION) {
                    throw MediaStudioException.invalid("the project needs a newer 3-Slate Studio (format version $version, this build reads $VERSION)")
                }
                else -> throw MediaStudioException.invalid("version must be an integer, got $version")
            }
            @Suppress("UNCHECKED_CAST")
            val ui = when (val value = map[KEY_UI]) {
                null -> emptyMap()
                is Map<*, *> -> value as Map<String, Any?>
                else -> throw MediaStudioException.invalid("ui must be an object")
            }
            val timeline = map.filterKeys { it != KEY_VERSION && it != KEY_UI }
            return SlateProject(timeline, ui).also { it.spec }
        }

    }

}

/**
 * A small JSON reader and writer for project files (no Android or third-party dependency, so JVM tests run it): objects
 * keep their key order, integers within 2^53 read as Long, other numbers as Double.
 * zh-CN: 工程文件用的小型 JSON 读写 (不依赖 Android 与第三方库, JVM 测试可直接运行): 对象保持键顺序, 2^53 以内的整数读为 Long,
 * 其他数字读为 Double.
 */
internal object Json {

    fun parse(text: String): Any? = Parser(text).run {
        val value = value()
        skipSpace()
        if (index != text.length) fail("unexpected text after the value")
        value
    }

    fun write(value: Any?): String = StringBuilder().also { write(it, value, 0) }.append('\n').toString()

    private fun write(out: StringBuilder, value: Any?, depth: Int) {
        when (value) {
            null -> out.append("null")
            is Boolean -> out.append(value)
            is Long, is Int, is Short, is Byte -> out.append(value)
            is Number -> {
                val double = value.toDouble()
                require(!double.isNaN() && !double.isInfinite()) { "JSON has no $double" }
                if (double == Math.floor(double) && Math.abs(double) < 1e15) out.append(double.toLong()) else out.append(double)
            }
            is String -> string(out, value)
            is Map<*, *> -> {
                if (value.isEmpty()) {
                    out.append("{}")
                    return
                }
                out.append("{\n")
                value.entries.forEachIndexed { index, (key, item) ->
                    indent(out, depth + 1)
                    string(out, key.toString())
                    out.append(": ")
                    write(out, item, depth + 1)
                    if (index < value.size - 1) out.append(',')
                    out.append('\n')
                }
                indent(out, depth)
                out.append('}')
            }
            is Iterable<*> -> {
                val items = value.toList()
                if (items.isEmpty()) {
                    out.append("[]")
                    return
                }
                out.append("[\n")
                items.forEachIndexed { index, item ->
                    indent(out, depth + 1)
                    write(out, item, depth + 1)
                    if (index < items.size - 1) out.append(',')
                    out.append('\n')
                }
                indent(out, depth)
                out.append(']')
            }
            is Array<*> -> write(out, value.toList(), depth)
            else -> throw IllegalArgumentException("cannot write ${value.javaClass.simpleName} as JSON")
        }
    }

    private fun indent(out: StringBuilder, depth: Int) {
        repeat(depth) { out.append("  ") }
    }

    private fun string(out: StringBuilder, text: String) {
        out.append('"')
        text.forEach { char ->
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char < ' ' -> out.append(String.format("\\u%04x", char.code))
                else -> out.append(char)
            }
        }
        out.append('"')
    }

    private class Parser(private val text: String) {

        var index = 0

        fun fail(message: String): Nothing = throw IllegalArgumentException("$message at offset $index")

        fun skipSpace() {
            while (index < text.length && text[index] in " \t\r\n") index++
        }

        fun value(): Any? {
            skipSpace()
            if (index >= text.length) fail("unexpected end of the text")
            return when (val char = text[index]) {
                '{' -> obj()
                '[' -> array()
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (char == '-' || char in '0'..'9') number() else fail("unexpected character '$char'")
            }
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!text.startsWith(word, index)) fail("expected $word")
            index += word.length
            return value
        }

        private fun obj(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            index++
            skipSpace()
            if (peek() == '}') {
                index++
                return map
            }
            while (true) {
                skipSpace()
                if (peek() != '"') fail("expected a key")
                val key = string()
                skipSpace()
                if (peek() != ':') fail("expected ':'")
                index++
                if (map.containsKey(key)) fail("duplicate key $key")
                map[key] = value()
                skipSpace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return map
                    }
                    else -> fail("expected ',' or '}'")
                }
            }
        }

        private fun array(): List<Any?> {
            val list = ArrayList<Any?>()
            index++
            skipSpace()
            if (peek() == ']') {
                index++
                return list
            }
            while (true) {
                list += value()
                skipSpace()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return list
                    }
                    else -> fail("expected ',' or ']'")
                }
            }
        }

        private fun string(): String {
            val out = StringBuilder()
            index++
            while (true) {
                if (index >= text.length) fail("unterminated string")
                when (val char = text[index++]) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (index >= text.length) fail("unterminated escape")
                        when (val escape = text[index++]) {
                            '"', '\\', '/' -> out.append(escape)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000c')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (index + 4 > text.length) fail("short unicode escape")
                                val code = text.substring(index, index + 4).toIntOrNull(16) ?: fail("bad unicode escape")
                                out.append(code.toChar())
                                index += 4
                            }
                            else -> fail("bad escape \\$escape")
                        }
                    }
                    else -> if (char < ' ') fail("control character in a string") else out.append(char)
                }
            }
        }

        private fun number(): Any {
            val start = index
            if (peek() == '-') index++
            while (index < text.length && (text[index].isDigit() || text[index] in ".eE+-")) index++
            val raw = text.substring(start, index)
            val double = raw.toDoubleOrNull() ?: fail("bad number $raw")
            val integral = raw.none { it == '.' || it == 'e' || it == 'E' }
            return if (integral && Math.abs(double) <= MAX_EXACT) raw.toLong() else double
        }

        private fun peek(): Char? = text.getOrNull(index)

    }

    private const val MAX_EXACT = 9_007_199_254_740_992.0

}

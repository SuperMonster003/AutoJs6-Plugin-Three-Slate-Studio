package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

/**
 * Turns an API call (module, method, arguments) into the equivalent JavaScript source (roadmap D11, D36). Arguments
 * are serialized like `JSON.stringify`: strings are escaped, paths keep their original value, maps become object
 * literals in insertion order and `null` values of maps are dropped. Roadmap P4.4 adds the option schemas shared with
 * the d.ts declarations.
 * zh-CN: 把一次 API 调用 (模块, 方法, 参数) 转为等价的 JavaScript 源码 (路线图 D11, D36). 参数像 `JSON.stringify` 一样
 * 序列化: 字符串转义, 路径保留原值, Map 按插入顺序转为对象字面量, Map 中的 `null` 值被省略. 路线图 P4.4 再加与 d.ts 声明
 * 同源的选项模式.
 */
internal object CodeGenerator {

    val MODULES: Set<String> = linkedSetOf("video", "audio")

    private val LINE_SEPARATOR = Char(0x2028)
    private val PARAGRAPH_SEPARATOR = Char(0x2029)

    private val IDENTIFIER = Regex("[A-Za-z_$][A-Za-z0-9_$]*")

    fun call(module: String, method: String, vararg arguments: Any?): String {
        require(module in MODULES) { "unknown module: $module" }
        require(IDENTIFIER.matches(method)) { "invalid method name: $method" }
        return arguments.joinToString(", ", prefix = "$module.$method(", postfix = ");") { literal(it) }
    }

    fun literal(value: Any?): String = when (value) {
        null -> "null"
        is String -> string(value)
        is Boolean -> value.toString()
        is Int, is Long, is Short, is Byte -> value.toString()
        is Float -> number(value.toDouble())
        is Double -> number(value)
        is Map<*, *> -> value.entries
            .filter { it.value != null }
            .joinToString(", ", prefix = "{ ", postfix = " }") { (key, item) -> key(key) + ": " + literal(item) }
            .let { if (it == "{  }") "{}" else it }
        is Iterable<*> -> value.joinToString(", ", prefix = "[", postfix = "]") { literal(it) }
        is Array<*> -> literal(value.asList())
        else -> throw IllegalArgumentException("unsupported value type: ${value::class.java.name}")
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
                '\u000C' -> append("\\f")
                // Line and paragraph separators end a line in older engines. zh-CN: 旧引擎把行分隔符与段分隔符视为换行.
                LINE_SEPARATOR, PARAGRAPH_SEPARATOR -> append(unicodeEscape(char))
                else -> if (char < ' ') append(unicodeEscape(char)) else append(char)
            }
        }
        append('"')
    }

    private fun unicodeEscape(char: Char): String = "\\u" + char.code.toString(16).padStart(4, '0')

}

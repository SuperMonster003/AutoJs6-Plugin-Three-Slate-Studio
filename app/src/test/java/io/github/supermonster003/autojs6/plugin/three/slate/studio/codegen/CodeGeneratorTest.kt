package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * JavaScript source generation for the "generated code" panels (roadmap D11, D36, P0.3). The expected strings write a
 * backslash as `~` (see [js]) so the test source stays free of escape sequences.
 * zh-CN: "生成的代码" 面板的 JavaScript 源码生成 (路线图 D11, D36, P0.3). 期望字符串用 `~` 表示反斜杠 (见 [js]), 使测试源码
 * 不含转义序列.
 */
class CodeGeneratorTest {

    @Test
    fun `calls serialize arguments like JSON stringify and drop null map values`() {
        assertEquals(
            """video.trim("/sdcard/a b.mp4", { start: 1000, end: 2500, output: "/sdcard/out.mp4" });""",
            CodeGenerator.call(
                "video", "trim", "/sdcard/a b.mp4",
                linkedMapOf("start" to 1000, "end" to 2500L, "codec" to null, "output" to "/sdcard/out.mp4"),
            ),
        )
        assertEquals("""audio.audioProbe("x.m4a");""", CodeGenerator.call("audio", "audioProbe", "x.m4a"))
        assertEquals("video.listTasks();", CodeGenerator.call("video", "listTasks"))
    }

    @Test
    fun `literals cover the JSON value types`() {
        assertEquals("null", CodeGenerator.literal(null))
        assertEquals("true", CodeGenerator.literal(true))
        assertEquals("-3", CodeGenerator.literal(-3))
        assertEquals("1", CodeGenerator.literal(1.0))
        assertEquals("0", CodeGenerator.literal(-0.0))
        assertEquals("0.25", CodeGenerator.literal(0.25f))
        assertEquals("1.5", CodeGenerator.literal(1.5))
        assertEquals("{}", CodeGenerator.literal(emptyMap<String, Any>()))
        assertEquals("{}", CodeGenerator.literal(mapOf("a" to null)))
        assertEquals("""[1, "x", null, [true]]""", CodeGenerator.literal(listOf(1, "x", null, arrayOf(true))))
        assertEquals("""{ "a-b": 1, ${'$'}c: { d_1: [] } }""", CodeGenerator.literal(linkedMapOf("a-b" to 1, "\$c" to mapOf("d_1" to emptyList<Int>()))))
    }

    @Test
    fun `strings escape quotes, backslashes, control characters and line separators`() {
        val backslash = Char(92)
        val input = "a" + '"' + "b" + backslash + "c" + Char(10) + Char(13) + Char(9) + Char(8) + Char(12) + Char(1) + Char(0x2028) + Char(0x2029) + "z"
        assertEquals(js(""""a~"b~~c~n~r~t~b~f~u0001~u2028~u2029z""""), CodeGenerator.literal(input))
        val cjk = String(Character.toChars(0x89C6)) + String(Character.toChars(0x9891))
        assertEquals("non-ASCII text stays as is", "\"" + cjk + "\"", CodeGenerator.literal(cjk))
    }

    @Test
    fun `unknown modules, invalid names and unsupported values are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.call("images", "trim") }
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.call("video", "tr im") }
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.call("video", "1trim") }
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.literal(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.literal(Float.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.literal(Any()) }
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.literal(mapOf(1 to "x")) }
        assertEquals(setOf("video", "audio"), CodeGenerator.MODULES)
    }

    /** Writes `~` as a backslash. zh-CN: 把 `~` 写成反斜杠. */
    private fun js(text: String): String = text.replace('~', Char(92))

}

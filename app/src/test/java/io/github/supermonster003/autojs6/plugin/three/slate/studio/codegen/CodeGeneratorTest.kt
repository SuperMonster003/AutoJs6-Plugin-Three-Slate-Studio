package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import org.autojs.plugin.mediastudio.api.MediaStudioOps
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

    @Test
    fun `long statements put one argument per line and break wide objects`() {
        val short = CodeGenerator.statement("let result = ", "video", "trim", listOf("/a.mp4", mapOf("start" to 0, "end" to 5000)))
        assertEquals("""let result = video.trim("/a.mp4", { start: 0, end: 5000 });""", short)
        val overlays = List(3) { mapOf("type" to "text", "text" to "a fairly long caption number $it", "x" to "50%", "y" to "10%") }
        val long = CodeGenerator.statement("let result = ", "video", "overlay", listOf("/a.mp4", overlays, "/b.mp4"))
        val expected = listOf(
            "let result = video.overlay(",
            "  \"/a.mp4\",",
            "  [",
            "    { type: \"text\", text: \"a fairly long caption number 0\", x: \"50%\", y: \"10%\" },",
            "    { type: \"text\", text: \"a fairly long caption number 1\", x: \"50%\", y: \"10%\" },",
            "    { type: \"text\", text: \"a fairly long caption number 2\", x: \"50%\", y: \"10%\" }",
            "  ],",
            "  \"/b.mp4\"",
            ");",
        )
        assertEquals(expected, long.lines())
        assertEquals("[1, 2]", CodeGenerator.literal(longArrayOf(1, 2)))
        assertEquals("[0.5]", CodeGenerator.literal(doubleArrayOf(0.5)))
        assertEquals("cues", CodeGenerator.literal(Raw("cues")))
        assertEquals("""video.subtitles.write("/a.srt", cues);""", CodeGenerator.call("video", "subtitles.write", "/a.srt", Raw("cues")))
    }

    @Test
    fun `complete scripts name the call and log the result`() {
        val trim = OptionSchemas.of("trim")!!
        val script = CodeGenerator.wizardScript(trim, listOf("/sdcard/a.mp4"), mapOf("start" to 0L, "end" to 5000L, "codec" to null), "/sdcard/b.mp4")
        assertEquals(
            listOf(
                "// Generated by 3-Slate Studio: video.trim",
                "// Runs in AutoJs6 with the 3-Slate Studio plugin enabled in the Plugin Center.",
                "",
                """let result = video.trim("/sdcard/a.mp4", { start: 0, end: 5000 }, "/sdcard/b.mp4");""",
                "console.log(result.path);",
                "",
            ),
            script.code.lines(),
        )
        assertEquals("video-trim.js", script.suggestedName)
        assertEquals("video.trim", script.api)
        val convert = CodeGenerator.wizardScript(OptionSchemas.of(OperationSchema.SUBTITLES_CONVERT)!!, listOf("/sdcard/a.ass"), mapOf("format" to "srt"), "/sdcard/a.srt")
        assertEquals("subtitles-convert.js", convert.suggestedName)
        assertEquals(
            listOf("""let cues = video.subtitles.parse("/sdcard/a.ass");""", """let result = video.subtitles.write("/sdcard/a.srt", cues, "srt");""", "console.log(result.path);"),
            convert.code.lines().drop(3).dropLast(1),
        )
        assertEquals("audio-mix.js", CodeGenerator.wizardScript(OptionSchemas.of("audioMix")!!, listOf("/a.m4a", "/b.m4a"), emptyMap(), null).suggestedName)
    }

    @Test
    fun `timeline exports of the editor are compose scripts`() {
        // The request shape of roadmap P4.3: no inputs, the timeline in options.timeline. zh-CN: 路线图 P4.3 的请求形状.
        val schema = OptionSchemas.of("trim")!!.copy(op = MediaStudioOps.COMPOSE, method = "compose")
        val timeline = mapOf("width" to 1280L, "height" to 720L, "video" to listOf(mapOf("clips" to listOf(mapOf("src" to "/a.mp4")))))
        val script = CodeGenerator.wizardScript(schema, emptyList(), linkedMapOf("timeline" to timeline, "codec" to "h265", "bitRate" to null), "/b.mp4")
        assertEquals(CodeGenerator.composeScript(timeline, "/b.mp4", mapOf("codec" to "h265")), script)
        assertEquals("video-compose.js", script.suggestedName)
        assertEquals(
            """let result = video.compose({ width: 1280, height: 720, video: [{ clips: [{ src: "/a.mp4" }] }] });""",
            CodeGenerator.wizardScript(schema, emptyList(), mapOf("timeline" to timeline), null).code.lines()[3],
        )
        assertThrows(IllegalArgumentException::class.java) { CodeGenerator.wizardScript(schema, emptyList(), emptyMap(), null) }
    }

    /** Writes `~` as a backslash. zh-CN: 把 `~` 写成反斜杠. */
    private fun js(text: String): String = text.replace('~', Char(92))

}

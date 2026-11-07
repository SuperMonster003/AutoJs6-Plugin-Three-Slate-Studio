package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter

/**
 * The JSON array of `subtitlesParse` (media studio contract version 2, appendix D Q36 (b)): what the host reads from the
 * descriptor `cuesFile`. The descriptor itself is covered by the instrumented `SubtitleOperationsTest`.
 * zh-CN: `subtitlesParse` 的 JSON 数组 (媒体工坊契约版本 2, 附录 D Q36 (b)): 宿主从描述符 `cuesFile` 读取的内容. 描述符本身由仪器测试
 * `SubtitleOperationsTest` 覆盖.
 */
class SubtitleCueFilesTest {

    private fun json(cues: List<CueText>): String = StringWriter().also { SubtitleCueFiles.write(cues, it) }.toString()

    @Test
    fun cuesRoundTripInFileOrder() {
        val cues = listOf(
            CueText(0, 1_500, "One"),
            CueText(1_000, 2_000, "Two\nlines with \"quotes\", a \\ and a tab\t"),
            CueText(9_007_199_254_740_000, 9_007_199_254_740_991, "中文字幕 😀 control \u0001 end"),
        )
        val text = json(cues)
        assertEquals(
            "[{\"start\":0,\"end\":1500,\"text\":\"One\"},\n" +
                "{\"start\":1000,\"end\":2000,\"text\":\"Two\\nlines with \\\"quotes\\\", a \\\\ and a tab\\t\"},\n",
            text.substring(0, text.indexOf("{\"start\":9007")),
        )
        assertTrue(text, text.contains("\\u0001"))
        assertTrue(text.endsWith("]\n"))
        val parsed = Json.parse(text) as List<*>
        assertEquals(
            cues.map { mapOf("start" to it.startMillis, "end" to it.endMillis, "text" to it.text) },
            parsed,
        )
    }

    @Test
    fun anEmptyTrackIsAnEmptyArray() {
        assertEquals("[]\n", json(emptyList()))
        assertEquals(emptyList<Any?>(), Json.parse(json(emptyList())))
    }

    @Test
    fun unpairedSurrogatesAreEscapedPairsAreKept() {
        // UTF-8 cannot carry a lone surrogate; the escape keeps it for the host's JSON reader.
        // zh-CN: UTF-8 无法承载单独的代理; 转义后宿主的 JSON 读取器仍能得到它.
        val text = json(listOf(CueText(0, 1, "a\uD800b\uDC00c😀")))
        assertTrue(text, text.contains("a\\ud800b\\udc00c😀"))
        assertEquals("a\uD800b\uDC00c😀", ((Json.parse(text) as List<*>).single() as Map<*, *>)["text"])
    }

    @Test
    fun manyCuesStayOneLinePerCue() {
        val cues = (0 until 6_000).map { CueText(it * 1_000L, it * 1_000L + 900, "line $it") }
        val text = json(cues)
        assertEquals(6_000, text.trimEnd().lines().size)
        assertTrue("far above the 256 KiB event limit of contract version 1", text.length > 200_000)
        assertEquals(6_000, (Json.parse(text) as List<*>).size)
    }
}

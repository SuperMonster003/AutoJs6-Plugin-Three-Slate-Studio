package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

/**
 * Default output locations and names of roadmap D14 (P0.3).
 * zh-CN: 路线图 D14 的默认输出位置与文件名 (P0.3).
 */
class OutputPathsTest {

    private val instant: Long = Instant.parse("2026-11-05T01:02:03Z").toEpochMilli()
    private val shanghai: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

    @Test
    fun `videos go to Movies and audio to Music under the plugin folder`() {
        assertEquals("/storage/emulated/0/Movies/3-Slate Studio", OutputPaths.defaultDirectory("/storage/emulated/0", audio = false))
        assertEquals("/storage/emulated/0/Music/3-Slate Studio", OutputPaths.defaultDirectory("/storage/emulated/0/", audio = true))
        assertEquals("3-Slate Studio", OutputPaths.FOLDER_NAME)
    }

    @Test
    fun `file names are source, op and local timestamp with a lowercase extension`() {
        assertEquals(
            "clip.final-trim-20261105-090203.mp4",
            OutputPaths.defaultFileName("/sdcard/DCIM/clip.final.mp4", "trim", "MP4", instant, shanghai),
        )
        assertEquals("recording-toGif-20261105-010203.gif", OutputPaths.defaultFileName("/a/recording", "toGif", ".gif", instant, TimeZone.getTimeZone("UTC")))
        assertEquals(".hidden-speed-20261105-090203.mp4", OutputPaths.defaultFileName("/a/.hidden", "speed", "mp4", instant, shanghai))
    }

    @Test
    fun `operations without a single source use the fallback name`() {
        assertEquals("slate-compose-20261105-090203.mp4", OutputPaths.defaultFileName(null, "compose", "mp4", instant, shanghai))
        assertEquals("slate-audioMix-20261105-090203.m4a", OutputPaths.defaultFileName("/a/...", "audioMix", "m4a", instant, shanghai))
        assertEquals("slate-trim-20261105-090203.mp4", OutputPaths.defaultFileName("/a/", "trim", "mp4", instant, shanghai))
    }

    @Test
    fun `characters FAT volumes reject are replaced`() {
        assertEquals("a_b_c_d_e_-trim-20261105-090203.mp4", OutputPaths.defaultFileName("/x/a:b*c?d|e\".mp4", "trim", "mp4", instant, shanghai))
        assertEquals("tab_name", OutputPaths.sanitize("tab" + Char(9) + "name"))
        assertEquals("name", OutputPaths.sanitize(" name.. "))
    }

    @Test
    fun `invalid extensions are rejected`() {
        listOf("", ".", "m/p4", "mp*").forEach { extension ->
            assertThrows(IllegalArgumentException::class.java) {
                OutputPaths.defaultFileName("/a/b.mp4", "trim", extension, instant, shanghai)
            }
        }
    }

    @Test
    fun `the default output joins the directory and the file name`() {
        assertEquals(
            "/storage/emulated/0/Music/3-Slate Studio/voice-audioTrim-20261105-090203.m4a",
            OutputPaths.defaultOutput("/storage/emulated/0", "/sdcard/voice.m4a", "audioTrim", "m4a", audio = true, timestampMillis = instant, zone = shanghai),
        )
    }

    @Test
    fun `a taken default name gets the first free numeric suffix`() {
        val taken = setOf("/m/a.mp4", "/m/a-1.mp4", "/m/a-2.mp4")
        assertEquals("/m/b.mp4", OutputPaths.uniquePath("/m/b.mp4") { it in taken })
        assertEquals("/m/a-3.mp4", OutputPaths.uniquePath("/m/a.mp4") { it in taken })
        assertEquals("/m/noext-1", OutputPaths.uniquePath("/m/noext") { it == "/m/noext" })
        assertEquals("/m/.hidden-1", OutputPaths.uniquePath("/m/.hidden") { it == "/m/.hidden" })
    }

    @Test
    fun `uniquePath gives up after 9999 suffixes`() {
        val error = assertThrows(MediaStudioException::class.java) { OutputPaths.uniquePath("/m/a.mp4") { true } }
        assertEquals("IO_ERROR", error.code)
        assertEquals(9999, OutputPaths.MAX_COLLISION_SUFFIX)
    }

}

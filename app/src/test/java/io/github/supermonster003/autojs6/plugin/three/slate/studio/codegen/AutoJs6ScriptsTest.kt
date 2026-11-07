package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The transport and host checks of "Open in AutoJs6" (roadmap P4.4, P1.4 / P1.7, appendix D Q28 (c), Q31 (a)): inline up
 * to 512 KiB of UTF-8 and 200000 chars, a content URI up to 4 MiB, nothing beyond; the host build of
 * `REQUIRED_HOST_VERSION` or later.
 * zh-CN: "在 AutoJs6 中打开" 的传输方式与宿主检查 (路线图 P4.4, P1.4 / P1.7, 附录 D Q28 (c), Q31 (a)): 不超过 512 KiB UTF-8 且
 * 200000 字符时内联, 不超过 4 MiB 时用 content URI, 更大则不发送; 需要 `REQUIRED_HOST_VERSION` 或更新的宿主构建.
 */
class AutoJs6ScriptsTest {

    @Test
    fun theLiteralsMatchTheHostContract() {
        assertEquals("org.autojs.autojs6", AutoJs6Scripts.HOST_PACKAGE_NAME)
        assertEquals("org.autojs.autojs.action.CREATE_SCRIPT", AutoJs6Scripts.ACTION_CREATE_SCRIPT)
        assertEquals(listOf("code", "codeUri", "suggestedName", "source", "error"), listOf(
            AutoJs6Scripts.EXTRA_CODE, AutoJs6Scripts.EXTRA_CODE_URI, AutoJs6Scripts.EXTRA_SUGGESTED_NAME, AutoJs6Scripts.EXTRA_SOURCE, AutoJs6Scripts.EXTRA_ERROR,
        ))
        assertEquals(524_288, AutoJs6Scripts.MAX_CODE_BYTES)
        assertEquals(200_000, AutoJs6Scripts.MAX_CODE_CHARS)
        assertEquals(4_194_304, AutoJs6Scripts.MAX_CODE_URI_BYTES)
    }

    @Test
    fun smallCodeTravelsInline() {
        assertEquals(AutoJs6Scripts.Transport.Inline, AutoJs6Scripts.transportOf(""))
        assertEquals(AutoJs6Scripts.Transport.Inline, AutoJs6Scripts.transportOf("a".repeat(200_000)))
        // 3 UTF-8 bytes per CJK char: 174762 chars are 524286 bytes. zh-CN: 每个中文字符 3 个 UTF-8 字节.
        assertEquals(AutoJs6Scripts.Transport.Inline, AutoJs6Scripts.transportOf("视".repeat(174_762)))
    }

    @Test
    fun overEitherInlineLimitTheCodeGoesThroughAContentUri() {
        assertEquals("the char limit", AutoJs6Scripts.Transport.ContentUri, AutoJs6Scripts.transportOf("a".repeat(200_001)))
        assertEquals("the byte limit", AutoJs6Scripts.Transport.ContentUri, AutoJs6Scripts.transportOf("视".repeat(174_763)))
        assertEquals(AutoJs6Scripts.Transport.ContentUri, AutoJs6Scripts.transportOf("a".repeat(4 * 1024 * 1024)))
        assertEquals(AutoJs6Scripts.Transport.TooLarge(4 * 1024 * 1024 + 1), AutoJs6Scripts.transportOf("a".repeat(4 * 1024 * 1024 + 1)))
    }

    @Test
    fun onlyHostsWithTheEntryAreCompatible() {
        assertEquals(6641L, ThreeSlateStudioContract.REQUIRED_HOST_VERSION)
        assertFalse("not installed", AutoJs6Scripts.isCompatible(null))
        assertFalse("public 6.8.0", AutoJs6Scripts.isCompatible(6640))
        assertTrue(AutoJs6Scripts.isCompatible(6641))
        assertTrue(AutoJs6Scripts.isCompatible(7000))
    }

}

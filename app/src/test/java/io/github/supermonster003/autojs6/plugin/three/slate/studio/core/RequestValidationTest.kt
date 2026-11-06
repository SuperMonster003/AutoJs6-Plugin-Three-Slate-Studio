package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The envelope checks of a submit request and the `NOT_IMPLEMENTED` answer of operations later stages add (roadmap D28,
 * P0.3, P2.1; host contract P1.1). [RequestPlannerTest] covers the per operation checks.
 * zh-CN: submit 请求外层字段的检查与后续阶段才加入的操作的 `NOT_IMPLEMENTED` 回答 (路线图 D28, P0.3, P2.1; 宿主契约 P1.1).
 * 按操作的检查见 [RequestPlannerTest].
 */
class RequestValidationTest {

    private val path = "/storage/emulated/0/Movies/clip.mp4"

    @Test
    fun `a missing or blank op is invalid`() {
        listOf(null, "", "  ").forEach { op ->
            assertEquals(invalid("op is required"), RequestValidation.rejection(op, listOf(path), null))
        }
    }

    @Test
    fun `an op outside the contract is invalid`() {
        assertEquals(invalid("unknown op explode"), RequestValidation.rejection("explode", listOf(path), null))
        assertEquals("op names are case sensitive", invalid("unknown op Trim"), RequestValidation.rejection("Trim", listOf(path), null))
    }

    @Test
    fun `the input count is limited before the paths are checked`() {
        val max = MediaStudioLimits.MAX_INPUTS
        assertEquals(256, max)
        assertEquals(
            MediaStudioErrorCodes.encode(MediaStudioErrorCodes.LIMIT_EXCEEDED, "${max + 1} inputs, at most $max"),
            RequestValidation.rejection(MediaStudioOps.CONCAT, List(max + 1) { "" }, null),
        )
        assertEquals(null, RequestValidation.rejection(MediaStudioOps.CONCAT, List(max) { path }, null))
    }

    @Test
    fun `inputs must be non-empty paths`() {
        listOf(listOf(path, ""), listOf(null), listOf(" ")).forEach { inputs ->
            assertEquals(invalid("inputs must be non-empty paths"), RequestValidation.rejection(MediaStudioOps.TRIM, inputs, null))
        }
        assertEquals(
            "operations without inputs reach the operation check",
            RequestValidation.notImplemented(MediaStudioOps.COMPOSE),
            RequestValidation.rejection(MediaStudioOps.COMPOSE, emptyList(), null),
        )
    }

    @Test
    fun `the host token is limited to 128 characters`() {
        val max = MediaStudioLimits.MAX_HOST_TOKEN_CHARS
        assertEquals(128, max)
        assertEquals(
            invalid("hostToken longer than $max characters"),
            RequestValidation.rejection(MediaStudioOps.TRIM, listOf(path), "t".repeat(max + 1)),
        )
        assertEquals(null, RequestValidation.rejection(MediaStudioOps.TRIM, listOf(path), "t".repeat(max)))
    }

    @Test
    fun `P2 implements probe and the basic operations, later operations answer NOT_IMPLEMENTED`() {
        val implemented = listOf(
            MediaStudioOps.PROBE,
            MediaStudioOps.TRIM,
            MediaStudioOps.CONCAT,
            MediaStudioOps.SPEED,
            MediaStudioOps.TRANSFORM,
            MediaStudioOps.EXTRACT_AUDIO,
            MediaStudioOps.REMOVE_AUDIO,
            MediaStudioOps.TRANSCODE,
            MediaStudioOps.OVERLAY,
            MediaStudioOps.FRAMES,
            MediaStudioOps.FRAME,
            MediaStudioOps.THUMBNAIL,
            MediaStudioOps.TO_GIF,
            MediaStudioOps.AUDIO_PROBE,
            MediaStudioOps.AUDIO_TRIM,
            MediaStudioOps.AUDIO_CONCAT,
            MediaStudioOps.AUDIO_MIX,
            MediaStudioOps.AUDIO_VOLUME,
            MediaStudioOps.AUDIO_CONVERT,
            MediaStudioOps.AUDIO_SPEED,
        )
        assertEquals(implemented, ThreeSlateStudioContract.IMPLEMENTED_OPS)
        assertTrue(MediaStudioOps.ALL.containsAll(implemented))
        MediaStudioOps.ALL.forEach { op ->
            assertTrue(MediaStudioOps.isKnown(op))
            val expected = if (op in implemented) null else RequestValidation.notImplemented(op)
            assertEquals(op, expected, RequestValidation.rejection(op, listOf(path), "host"))
        }
    }

    @Test
    fun `NOT_IMPLEMENTED is an INVALID_OPTIONS detail`() {
        assertEquals(
            MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "NOT_IMPLEMENTED: trim"),
            RequestValidation.notImplemented("trim"),
        )
        assertTrue(RequestValidation.notImplemented("trim").startsWith(MediaStudioErrorCodes.INVALID_OPTIONS))
    }

    private fun invalid(detail: String): String = MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, detail)

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Decode times of `frames` and `toGif` once the input duration is known (roadmap P2.4).
 * zh-CN: 已知输入时长后 `frames` 与 `toGif` 的解码时间 (路线图 P2.4).
 */
class FrameSamplingTest {

    private fun rejected(code: String, block: () -> Unit): String {
        val error = assertThrows(MediaStudioException::class.java) { block() }
        assertEquals(code, error.code)
        return error.detail
    }

    @Test
    fun `explicit times must not pass the end of the input`() {
        assertEquals(listOf(0.0, 10_000.0), FrameSampling.frameTimes(FrameTimes.Points(listOf(0.0, 10_000.0), "times"), 10_000))
        assertEquals(
            "options.times[1] (10000.5 ms) is after the end of the input (10000 ms)",
            rejected(MediaStudioErrorCodes.INVALID_OPTIONS) { FrameSampling.frameTimes(FrameTimes.Points(listOf(0.0, 10_000.5), "times"), 10_000) },
        )
        assertEquals(
            "options.at (12000 ms) is after the end of the input (10000 ms)",
            rejected(MediaStudioErrorCodes.INVALID_OPTIONS) { FrameSampling.frameTimes(FrameTimes.Points(listOf(12_000.0), "at"), 10_000) },
        )
    }

    @Test
    fun `every steps from the start and stops before the end`() {
        assertEquals(listOf(0.0, 1000.0, 2000.0), FrameSampling.frameTimes(FrameTimes.Every(1000.0, 0.0, null, null), 3_000))
        assertEquals(listOf(500.0, 1500.0, 2500.0), FrameSampling.frameTimes(FrameTimes.Every(1000.0, 500.0, null, null), 3_000))
        assertEquals(30, FrameSampling.frameTimes(FrameTimes.Every(100.0, 0.0, 3000.0, null), 10_000).size)
        assertEquals(listOf(0.0, 1000.0), FrameSampling.frameTimes(FrameTimes.Every(1000.0, 0.0, 99_999.0, 2), 10_000))
        assertEquals(1000, FrameSampling.frameTimes(FrameTimes.Every(1.0, 0.0, null, 1000), 10_000).size)
        assertEquals(
            "frames would extract 10000 frames, at most 1000; raise options.every or set options.count",
            rejected(MediaStudioErrorCodes.LIMIT_EXCEEDED) { FrameSampling.frameTimes(FrameTimes.Every(1.0, 0.0, null, null), 10_000) },
        )
        assertEquals(
            "options.start (3000 ms) is not before the end of the input (3000 ms)",
            rejected(MediaStudioErrorCodes.INVALID_OPTIONS) { FrameSampling.frameTimes(FrameTimes.Every(100.0, 3000.0, null, null), 3_000) },
        )
    }

    @Test
    fun `count puts each frame in the middle of an equal slot`() {
        assertEquals(listOf(500.0, 1500.0, 2500.0, 3500.0), FrameSampling.frameTimes(FrameTimes.Count(4, 0.0, null), 4_000))
        assertEquals(listOf(1500.0, 2500.0), FrameSampling.frameTimes(FrameTimes.Count(2, 1000.0, 3000.0), 10_000))
    }

    @Test
    fun `GIF frames start every 1 over fps seconds and stay within 600`() {
        assertEquals(10, FrameSampling.gifFrameCount(0.0, 1000.0, 10))
        assertEquals(11, FrameSampling.gifFrameCount(0.0, 1001.0, 10))
        assertEquals(1, FrameSampling.gifFrameCount(0.0, 1.0, 10))
        assertEquals(600, FrameSampling.gifFrameCount(0.0, 60_000.0, 10))
        assertEquals(
            "toGif would write 601 frames, at most 600; shorten options.start - options.end or lower options.fps",
            rejected(MediaStudioErrorCodes.LIMIT_EXCEEDED) { FrameSampling.gifFrameCount(0.0, 60_001.0, 10) },
        )
        assertEquals(listOf(0.0, 250.0, 500.0, 750.0), FrameSampling.gifTimes(OperationSpec.ToGif(0.0, null, 4, null, 0, false), 1_000))
        // An end beyond the input is cut to the input. zh-CN: 超出输入的结束时间截到输入结尾.
        assertEquals(listOf(500.0, 1000.0, 1500.0), FrameSampling.gifTimes(OperationSpec.ToGif(500.0, 99_999.0, 2, null, 0, false), 2_000))
        assertEquals(
            "toGif would write 3000 frames, at most 600; shorten options.start - options.end or lower options.fps",
            rejected(MediaStudioErrorCodes.LIMIT_EXCEEDED) { FrameSampling.gifTimes(OperationSpec.ToGif(0.0, null, 50, null, 0, false), 60_000) },
        )
    }

    @Test
    fun `the end of the input decodes its last frame`() {
        assertEquals(1_500_500L, FrameSampling.decodeMicros(1500.5, 10_000))
        assertEquals(9_999_000L, FrameSampling.decodeMicros(10_000.0, 10_000))
        assertEquals(0L, FrameSampling.decodeMicros(0.0, 0))
    }

}

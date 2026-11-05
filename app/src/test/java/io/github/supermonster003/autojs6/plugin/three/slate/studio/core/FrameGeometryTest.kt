package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Freezes the frame sizes of the video operations (roadmap P2.2): even sizes, aspect ratio for one given side, the
 * rotation swap, crop checks and the media3 normalized device coordinates of a crop.
 * zh-CN: 冻结视频操作的帧尺寸 (路线图 P2.2): 偶数尺寸, 只给一边时的宽高比, 旋转后的宽高交换, 裁切检查, 以及裁切换算出的 media3
 * 归一化设备坐标.
 */
class FrameGeometryTest {

    @Test
    fun `even rounds down and keeps at least 2`() {
        assertEquals(1920, FrameGeometry.even(1920))
        assertEquals(1078, FrameGeometry.even(1079))
        assertEquals(2, FrameGeometry.even(1))
        assertEquals(2, FrameGeometry.even(0))
        assertEquals(FrameSize(320, 240), FrameGeometry.even(FrameSize(321, 241)))
    }

    @Test
    fun `target keeps the aspect ratio when one side is given`() {
        val source = FrameSize(1920, 1080)
        assertEquals(FrameSize(1280, 720), FrameGeometry.target(1280, null, source))
        assertEquals(FrameSize(1280, 720), FrameGeometry.target(null, 720, source))
        // 640 * 1080 / 1920 = 360; 480 * 1920 / 1080 = 853.3 -> 853 -> 852.
        assertEquals(FrameSize(640, 360), FrameGeometry.target(640, null, source))
        assertEquals(FrameSize(852, 480), FrameGeometry.target(null, 480, source))
    }

    @Test
    fun `target uses both sides as given and the source when none is given`() {
        val source = FrameSize(1920, 1080)
        assertEquals(FrameSize(500, 500), FrameGeometry.target(500, 500, source))
        assertEquals(FrameSize(500, 500), FrameGeometry.target(501, 501, source))
        assertEquals(source, FrameGeometry.target(null, null, source))
        assertEquals(FrameSize(1918, 1078), FrameGeometry.target(null, null, FrameSize(1919, 1079)))
    }

    @Test
    fun `target follows the displayed size of a portrait source`() {
        // A 1920x1080 recording with rotation 90 is displayed as 1080x1920.
        val displayed = FrameSize(1080, 1920)
        assertEquals(FrameSize(720, 1280), FrameGeometry.target(720, null, displayed))
        assertEquals(FrameSize(360, 640), FrameGeometry.target(null, 640, displayed))
    }

    @Test
    fun `scale multiplies both sides and stays even`() {
        assertEquals(FrameSize(960, 540), FrameGeometry.scale(FrameSize(1920, 1080), 0.5))
        assertEquals(FrameSize(106, 80), FrameGeometry.scale(FrameSize(320, 240), 1.0 / 3))
        assertEquals(FrameSize(640, 480), FrameGeometry.scale(FrameSize(320, 240), 2.0))
    }

    @Test
    fun `quarter turns swap the sides`() {
        val size = FrameSize(320, 240)
        assertEquals(size, FrameGeometry.rotate(size, 0))
        assertEquals(FrameSize(240, 320), FrameGeometry.rotate(size, 90))
        assertEquals(size, FrameGeometry.rotate(size, 180))
        assertEquals(FrameSize(240, 320), FrameGeometry.rotate(size, 270))
    }

    @Test
    fun `crops must lie inside the frame`() {
        val frame = FrameSize(320, 240)
        assertNull(FrameGeometry.cropProblem(CropRect(0, 0, 320, 240), frame))
        assertNull(FrameGeometry.cropProblem(CropRect(160, 120, 160, 120), frame))
        listOf(
            CropRect(161, 0, 160, 240),
            CropRect(0, 1, 320, 240),
            CropRect(-1, 0, 16, 16),
            CropRect(0, -1, 16, 16),
            CropRect(Int.MAX_VALUE, 0, Int.MAX_VALUE, 16),
        ).forEach { crop ->
            val problem = FrameGeometry.cropProblem(crop, frame)
            assertTrue("$crop -> $problem", problem != null && "outside the 320x240 frame" in problem)
        }
    }

    @Test
    fun `crops become left, right, bottom, top in normalized device coordinates`() {
        val frame = FrameSize(320, 240)
        assertArrayEquals(floatArrayOf(-1f, 1f, -1f, 1f), FrameGeometry.cropNdc(CropRect(0, 0, 320, 240), frame), 1e-6f)
        // The top left quarter: x 0 - 160, y 0 - 120 (pixels, y down) -> left -1, right 0, bottom 0, top 1.
        assertArrayEquals(floatArrayOf(-1f, 0f, 0f, 1f), FrameGeometry.cropNdc(CropRect(0, 0, 160, 120), frame), 1e-6f)
        // The bottom right quarter.
        assertArrayEquals(floatArrayOf(0f, 1f, -1f, 0f), FrameGeometry.cropNdc(CropRect(160, 120, 160, 120), frame), 1e-6f)
        assertArrayEquals(floatArrayOf(-0.5f, 0.5f, -0.5f, 0.5f), FrameGeometry.cropNdc(CropRect(80, 60, 160, 120), frame), 1e-6f)
    }

    @Test
    fun `output sizes stay within 16 - 8192 pixels per side`() {
        assertNull(FrameGeometry.sizeProblem(FrameSize(16, 16)))
        assertNull(FrameGeometry.sizeProblem(FrameSize(8192, 8192)))
        assertEquals("the output size 14x240 is outside 16 - 8192 pixels per side", FrameGeometry.sizeProblem(FrameSize(14, 240)))
        assertTrue(FrameGeometry.sizeProblem(FrameSize(320, 8194)) != null)
    }

}

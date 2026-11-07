package io.github.supermonster003.autojs6.plugin.three.slate.studio.preview

import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditorProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.TransitionModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.VideoClipModel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.VideoTrackModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The self-written preview (roadmap appendix D Q3 (b)) follows the export's transition formulas
 * (`ComposeCompositorSettings`, `TransitionEffect`): alpha only on the upper lane, slide offsets per direction, zoom
 * scales, fade brightness, wipe on the upper lane; the frame size follows the first clip.
 * zh-CN: 自写预览 (路线图附录 D Q3 (b)) 沿用导出的转场公式: 只有上层车道调透明度, slide 按方向平移, zoom 缩放, fade 亮度, wipe 在上层车道;
 * 画面尺寸跟随第一段.
 */
class TimelinePreviewTest {

    private fun clip(id: Long, transition: TransitionModel? = null) =
        VideoClipModel(id, "/sdcard/$id.mp4", 4_000, 0, 4_000, transition = transition, width = 1920, height = 1080)

    private fun project(vararg clips: VideoClipModel) = EditorProject(videoTracks = listOf(VideoTrackModel(1, clips.toList())))

    private fun visual(project: EditorProject, index: Int, tMs: Long): LaneVisual {
        val clips = project.videoTracks[0].clips
        return TimelinePreview.visualOf(clips, index, project.videoPlacements.getValue(clips[index].id), tMs * 1_000, top = index % 2 == 1)
    }

    @Test
    fun dissolveFadesTheUpperLaneOnly() {
        val p = project(clip(1), clip(2, TransitionModel("dissolve", 1_000)))
        // Clip 2 starts at 3 s; at 3.5 s the transition is half done. zh-CN: 第二段 3 s 开始, 3.5 s 时转场过半.
        assertEquals(0.5f, visual(p, 1, 3_500).alpha, 1e-4f)
        assertEquals(1f, visual(p, 0, 3_500).alpha, 1e-4f)
        assertEquals(1f, visual(p, 1, 4_200).alpha, 1e-4f)
    }

    @Test
    fun slideMovesBothClipsAlongTheDirection() {
        val p = project(clip(1), clip(2, TransitionModel("slide", 1_000, "left")))
        val incoming = visual(p, 1, 3_250)
        val outgoing = visual(p, 0, 3_250)
        assertEquals(0.75f, incoming.offsetX, 1e-4f)
        assertEquals(-0.25f, outgoing.offsetX, 1e-4f)
        val up = project(clip(1), clip(2, TransitionModel("slide", 1_000, "up")))
        assertEquals(0.75f, visual(up, 1, 3_250).offsetY, 1e-4f)
    }

    @Test
    fun zoomFadeAndWipeFollowTheExport() {
        val zoom = project(clip(1), clip(2, TransitionModel("zoom", 1_000)))
        assertEquals(0.75f, visual(zoom, 1, 3_500).scale, 1e-4f)
        assertEquals(1.25f, visual(zoom, 0, 3_500).scale, 1e-4f)

        // A fade is half on each side and does not overlap: clip 2 starts at 4 s. zh-CN: fade 两侧各一半且不重叠.
        val fade = project(clip(1), clip(2, TransitionModel("fade", 1_000)))
        assertEquals(4_000_000L, fade.videoPlacements.getValue(2).atUs)
        assertEquals(0.5f, visual(fade, 0, 3_750).brightness, 1e-4f)
        assertEquals(0.5f, visual(fade, 1, 4_250).brightness, 1e-4f)

        val wipe = project(clip(1), clip(2, TransitionModel("wipe", 1_000, "right")))
        val mask = visual(wipe, 1, 3_400).wipe
        assertNotNull(mask)
        assertEquals(0.4f, mask!!.progress, 1e-4f)
        assertEquals(true, mask.keepIncoming)
        assertNull(visual(wipe, 0, 3_400).wipe)
    }

    @Test
    fun frameSizeFollowsTheFirstClip() {
        assertEquals(1920 to 1080, TimelinePreview.frameSize(project(clip(1))))
        assertEquals(1080 to 1920, TimelinePreview.frameSize(project(clip(1).copy(rotate = 90))))
        assertEquals(1280 to 720, TimelinePreview.frameSize(project(clip(1)).copy(width = 1280)))
        assertEquals(720 to 1280, TimelinePreview.frameSize(project(clip(1)).copy(width = 720, height = 1280)))
    }

}

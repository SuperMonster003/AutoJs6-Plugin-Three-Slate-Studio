package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.RequestedSizeEncoderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Appendix D Q37 (a): the actionable message of a `compose` export that ran out of codec resources, and Q38 (a): the
 * warning when an encoder refuses the requested size.
 * zh-CN: 附录 D Q37 (a): `compose` 导出因编解码器资源耗尽失败时的可操作提示; Q38 (a): 编码器拒绝请求尺寸时的警告.
 */
class CodecResourcesTest {

    private fun track(vararg transitions: String?): Map<String, Any?> = mapOf(
        "clips" to transitions.mapIndexed { index, type ->
            mapOf<String, Any?>("src" to "/sdcard/Movies/$index.mp4") + (type?.let { mapOf("transition" to mapOf("type" to it)) } ?: emptyMap())
        },
    )

    private fun audio(): Map<String, Any?> = mapOf("clips" to listOf(mapOf("src" to "/sdcard/Music/a.m4a")))

    private fun budget(video: List<Map<String, Any?>>, audioTracks: Int) =
        CodecResources.budgetOf(TimelineOptions.parse(mapOf("video" to video, "audio" to List(audioTracks) { audio() }.ifEmpty { null })))

    @Test
    fun budgetCountsTwoPerTrackWithAnOverlappingTransition() {
        // The extreme case of appendix F: 8 tracks with a dissolve each plus 16 audio tracks.
        // zh-CN: 附录 F 的极限用例: 8 条各带溶解转场的视频轨加 16 条音频轨.
        val extreme = budget(List(8) { track(null, "dissolve") }, 16)
        assertEquals(CodecResources.Budget(8, 0, 16), extreme)
        assertEquals(32, extreme.count)
        assertEquals(28, budget(List(6) { track(null, "dissolve") }, 16).count)
        // cut and fade do not overlap; a track counts 2 once, however many transitions it has.
        // zh-CN: cut 与 fade 不重叠; 一条轨道无论有几个转场都只计 2.
        assertEquals(CodecResources.Budget(1, 2, 0), budget(listOf(track(null, "wipe", "blur", "slide"), track(null, "cut"), track(null, "fade")), 0))
        assertEquals(CodecResources.Budget(0, 1, 2), budget(listOf(track(null)), 2))
    }

    @Test
    fun onlyInitFailuresWithAResourceSignOrAnOverBudgetCount() {
        val small = CodecResources.Budget(1, 0, 1)
        val large = CodecResources.Budget(8, 0, 16)
        val encoder = CodecResources.ENCODER_INIT_FAILED
        val decoder = CodecResources.DECODER_INIT_FAILED
        assertTrue(CodecResources.isExhausted(encoder, listOf(1100), emptyList(), small))
        assertTrue(CodecResources.isExhausted(decoder, listOf(1101), emptyList(), small))
        assertTrue(CodecResources.isExhausted(encoder, emptyList(), listOf("Codec exception: ...", "Error 0xfffffff4 (InsufficientResources)"), small))
        assertTrue(CodecResources.isExhausted(decoder, emptyList(), listOf("Failed to allocate component instance: NO_MEMORY"), small))
        // Above the measured 28 an init failure counts even without a sign. zh-CN: 超过实测的 28 时, 即使没有迹象也计入.
        assertTrue(CodecResources.isExhausted(decoder, emptyList(), listOf("Codec exception"), large))
        assertFalse(CodecResources.isExhausted(decoder, emptyList(), listOf("Codec exception"), small))
        assertFalse(CodecResources.isExhausted(encoder, listOf(1), listOf("configure failed"), small))
        // Other failures never become the resource message. zh-CN: 其他失败从不变成资源提示.
        assertFalse(CodecResources.isExhausted("ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED", listOf(1100), listOf("InsufficientResources"), large))
        assertFalse(CodecResources.isExhausted("ERROR_CODE_DECODING_FORMAT_UNSUPPORTED", emptyList(), emptyList(), large))
    }

    @Test
    fun theMessageSaysWhatToChange() {
        assertEquals(
            "the device could not start another video encoder (ERROR_CODE_ENCODER_INIT_FAILED, InsufficientResources): this timeline decodes about " +
                "32 sources at once (8 video tracks with overlapping transitions x 2 + 0 other video tracks + 16 audio tracks); " +
                "use fewer video tracks or fewer overlapping transitions",
            CodecResources.message(CodecResources.ENCODER_INIT_FAILED, insufficient = true, CodecResources.Budget(8, 0, 16)),
        )
        assertEquals(
            "the device could not start another video decoder (ERROR_CODE_DECODER_INIT_FAILED): this timeline decodes about " +
                "31 sources at once (7 video tracks with overlapping transitions x 2 + 1 other video tracks + 16 audio tracks); " +
                "use fewer video tracks or fewer overlapping transitions",
            CodecResources.message(CodecResources.DECODER_INIT_FAILED, insufficient = false, CodecResources.Budget(7, 1, 16)),
        )
    }

    @Test
    fun aRefusedSizeNamesBothDimensions() {
        assertEquals(
            "the encoder c2.mtk.avc.encoder did not run 1920x1080; the video was encoded at 1920x1088, the frame fitted inside",
            RequestedSizeEncoderFactory.sizeNote("c2.mtk.avc.encoder", 1920, 1080, 1920, 1088),
        )
        // Only the width changed: media3's own report (height only) would say nothing. zh-CN: 只改宽度: media3 自己的报告 (只有高度) 不会提.
        assertEquals(
            "the encoder c2.android.avc.encoder did not run 360x640; the video was encoded at 368x640, the frame fitted inside",
            RequestedSizeEncoderFactory.sizeNote("c2.android.avc.encoder", 360, 640, 368, 640),
        )
    }
}

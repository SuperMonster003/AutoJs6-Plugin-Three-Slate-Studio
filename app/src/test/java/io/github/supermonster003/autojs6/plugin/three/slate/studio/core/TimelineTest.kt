package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.MixSource
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.TransitionEffect
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P3.1 / P3.2: the `compose` timeline rules (field paths in every message, placement, transitions, limits), the
 * project file, the mix fades and the shader state of the transitions.
 * zh-CN: 路线图 P3.1 / P3.2: `compose` 时间线规则 (每条消息都有字段路径, 定位, 转场, 上限), 工程文件, 混音淡入淡出与转场着色器状态.
 */
class TimelineTest {

    private val a = "/sdcard/Movies/a.mp4"
    private val b = "/sdcard/Movies/b.mp4"
    private val music = "/sdcard/Music/bgm.m4a"

    private val media = mapOf(
        a to info(a, 8_000, 1920, 1080, audio = true),
        b to info(b, 10_000, 1080, 1920, audio = false),
        music to info(music, 20_000, null, null, audio = true),
    )

    private fun info(path: String, duration: Long, width: Int?, height: Int?, audio: Boolean) = MediaInfo(
        path = path, sizeBytes = 1, durationMillis = duration, mime = "video/mp4", width = width, height = height, rotation = 0,
        frameRate = 30.0, bitRate = null, videoCodec = width?.let { "h264" }, audioCodec = if (audio) "aac" else null,
        sampleRate = if (audio) 44_100 else null, channels = if (audio) 2 else null, language = null, tracks = emptyList(),
    )

    private fun track(vararg clips: Map<String, Any?>) = mapOf("clips" to clips.toList())

    private fun clip(src: String, vararg extra: Pair<String, Any?>) = mapOf("src" to src) + extra

    private fun timeline(vararg entries: Pair<String, Any?>): Map<String, Any?> = mapOf(*entries)

    private fun layout(timeline: Map<String, Any?>) = TimelineLayout.resolve(TimelineOptions.parse(timeline)) { media.getValue(it) }

    private fun rejected(expected: String, code: String = MediaStudioErrorCodes.INVALID_OPTIONS, block: () -> Unit) {
        val error = assertThrows(MediaStudioException::class.java) { block() }
        assertEquals(code, error.code)
        assertEquals(expected, error.detail)
    }

    @Test
    fun `defaults and sources`() {
        val spec = TimelineOptions.parse(timeline("video" to listOf(track(clip(a), clip(b), clip(a)))))
        assertEquals(30, spec.frameRate)
        assertEquals(Colors.BLACK, spec.background)
        assertEquals(listOf(a, b), spec.sources)
        val first = spec.videoTracks[0][0]
        assertEquals("timeline.video[0].clips[0]", first.path)
        assertEquals(1.0, first.speed, 0.0)
        assertEquals(ClipTransform(), first.transform)
        assertNull(first.transition)
        val parsed = OperationOptions.parse(MediaStudioOps.COMPOSE, 0, mapOf("codec" to "h265"), timeline("video" to listOf(track(clip(a)))))
        assertTrue(parsed is OperationSpec.Compose)
        assertEquals(VideoCodec.H265, (parsed as OperationSpec.Compose).video.codec)
    }

    @Test
    fun `messages name the field path`() {
        rejected("timeline is required") { OperationOptions.parse(MediaStudioOps.COMPOSE, 0, emptyMap(), null) }
        rejected("timeline.video must hold at least one clip") { TimelineOptions.parse(timeline("audio" to listOf(track(clip(music))))) }
        rejected("timeline.video[0].clips is required") { TimelineOptions.parse(timeline("video" to listOf(mapOf<String, Any?>()))) }
        rejected("timeline.video[0].clips[1].src must be an absolute path, got b.mp4") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a), clip("b.mp4")))))
        }
        rejected("timeline.video[0].clips[1].transition.duration must be between 100 and 3000, got 50") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a), clip(b, "transition" to mapOf("type" to "dissolve", "duration" to 50L))))))
        }
        rejected("timeline.video[0].clips[1].transition.type must be one of cut, fade, dissolve, slide, wipe, zoom, blur, got spin") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a), clip(b, "transition" to mapOf("type" to "spin"))))))
        }
        rejected("timeline.video[0].clips[1].transition.direction applies to slide and wipe only") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a), clip(b, "transition" to mapOf("type" to "zoom", "direction" to "up"))))))
        }
        rejected("unknown option timeline.video[0].clips[0].transform.zoom") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a, "transform" to mapOf("zoom" to 2.0))))))
        }
        rejected("timeline.audio[0].clips[0].end (1000) must be greater than timeline.audio[0].clips[0].start (1000)") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a))), "audio" to listOf(track(clip(music, "start" to 1_000L, "end" to 1_000L)))))
        }
        rejected("timeline.overlays[0].type is required") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a))), "overlays" to listOf(mapOf("text" to "x"))))
        }
        rejected("NOT_IMPLEMENTED: timeline.subtitles (roadmap P3.3)") {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a))), "subtitles" to mapOf("src" to "/a.srt")))
        }
        rejected("unknown option timeline.tracks") { TimelineOptions.parse(timeline("video" to listOf(track(clip(a))), "tracks" to 1L)) }
        rejected("timeline.video holds 5 tracks, at most 4", MediaStudioErrorCodes.LIMIT_EXCEEDED) {
            TimelineOptions.parse(timeline("video" to List(5) { track(clip(a)) }))
        }
        // An empty overlay array is accepted (the editor may save one). zh-CN: 接受空叠加数组 (编辑器可能保存空数组).
        assertEquals(emptyList<OverlayItem>(), TimelineOptions.parse(timeline("video" to listOf(track(clip(a))), "overlays" to emptyList<Any>())).overlays)
    }

    @Test
    fun `clips follow each other and transitions overlap the previous clip`() {
        val placed = layout(
            timeline(
                "video" to listOf(
                    track(
                        clip(a, "end" to 4_000L),
                        clip(b, "start" to 2_000L, "end" to 6_000L, "transition" to mapOf("type" to "dissolve")),
                        clip(a, "speed" to 2.0, "transition" to mapOf("type" to "fade", "duration" to 1_000L)),
                        clip(a, "at" to 20_000L, "end" to 1_000L),
                    ),
                ),
            ),
        ).videoTracks[0]
        assertEquals(listOf(0L, 3_500_000L, 7_500_000L, 20_000_000L), placed.map { it.atUs })
        assertEquals(listOf(4_000_000L, 4_000_000L, 4_000_000L, 1_000_000L), placed.map { it.lengthUs })
        assertEquals(TransitionType.DISSOLVE, placed[0].out!!.type)
        assertEquals(TransitionType.DISSOLVE, placed[1].into!!.type)
        assertEquals(500L, placed[1].into!!.durationMillis)
        assertEquals(TransitionType.FADE, placed[1].out!!.type)
        assertEquals(500_000L, TimelineLayout.sideUs(placed[2].into!!))
        assertNull(placed[3].into)
        assertEquals("cut is no transition", null, layout(timeline("video" to listOf(track(clip(a), clip(b, "transition" to mapOf("type" to "cut")))))).videoTracks[0][1].into)
    }

    @Test
    fun `placement errors need the media`() {
        rejected("timeline.video[0].clips[1].at (7000) must be 7500: the dissolve of 500 ms overlaps the end of the previous clip (8000 ms); leave out at to place it there") {
            layout(timeline("video" to listOf(track(clip(a), clip(b, "at" to 7_000L, "transition" to mapOf("type" to "dissolve"))))))
        }
        assertEquals(7_500_000L, layout(timeline("video" to listOf(track(clip(a), clip(b, "at" to 7_500L, "transition" to mapOf("type" to "dissolve")))))).videoTracks[0][1].atUs)
        rejected("timeline.video[0].clips[1].at (7000) is before the end of the previous clip (8000 ms); clips of one track must not overlap except in a transition") {
            layout(timeline("video" to listOf(track(clip(a), clip(b, "at" to 7_000L)))))
        }
        rejected("timeline.video[0].clips[0].transition needs a previous clip in the track") {
            layout(timeline("video" to listOf(track(clip(a, "transition" to mapOf("type" to "wipe"))))))
        }
        rejected("timeline.video[0].clips[1].transition.duration (3000 ms) is longer than the previous clip (1000 ms)") {
            layout(timeline("video" to listOf(track(clip(a, "end" to 1_000L), clip(b, "transition" to mapOf("type" to "slide", "duration" to 3_000L))))))
        }
        rejected("timeline.video[0].clips[2].transition (800 ms) overlaps the transition into the previous clip (800 ms); the previous clip lasts only 1500 ms") {
            layout(
                timeline(
                    "video" to listOf(
                        track(clip(a), clip(b, "end" to 1_500L, "transition" to mapOf("type" to "wipe", "duration" to 800L)), clip(a, "transition" to mapOf("type" to "blur", "duration" to 800L))),
                    ),
                ),
            )
        }
        rejected("timeline.video[0].clips[0].start (9000 ms) is not before the end of /sdcard/Movies/a.mp4 (8000 ms)") {
            layout(timeline("video" to listOf(track(clip(a, "start" to 9_000L)))))
        }
        rejected("timeline.video[0].clips[0].src /sdcard/Music/bgm.m4a has no video track", MediaStudioErrorCodes.UNSUPPORTED_FORMAT) {
            layout(timeline("video" to listOf(track(clip(music)))))
        }
        rejected("timeline.audio[0].clips[0].src /sdcard/Movies/b.mp4 has no audio track", MediaStudioErrorCodes.UNSUPPORTED_FORMAT) {
            layout(timeline("video" to listOf(track(clip(a))), "audio" to listOf(track(clip(b)))))
        }
        rejected("timeline.audio[0].clips[1].fadeIn + fadeOut (3000 ms) is longer than the clip (2000 ms)") {
            layout(timeline("video" to listOf(track(clip(a))), "audio" to listOf(track(clip(music, "end" to 1_000L), clip(music, "end" to 2_000L, "fadeIn" to 1_500L, "fadeOut" to 1_500L)))))
        }
        rejected("timeline.audio[0].clips[1].at (500) is before the end of the previous clip (1000 ms); clips of one track must not overlap") {
            layout(timeline("video" to listOf(track(clip(a))), "audio" to listOf(track(clip(music, "end" to 1_000L), clip(music, "at" to 500L)))))
        }
    }

    @Test
    fun `size, duration and limits`() {
        val default = layout(timeline("video" to listOf(track(clip(a))), "audio" to listOf(track(clip(music)))))
        assertEquals(1920 to 1080, default.width to default.height)
        assertEquals("the audio clip is longer", 20_000L, default.durationMillis)
        val rotated = layout(timeline("video" to listOf(track(clip(a, "transform" to mapOf("rotate" to 90L, "crop" to mapOf("width" to 1000L, "height" to 500L)))))))
        assertEquals(500 to 1000, rotated.width to rotated.height)
        val widthOnly = layout(timeline("width" to 961L, "video" to listOf(track(clip(a)))))
        assertEquals("even, aspect of the first clip", 960 to 540, widthOnly.width to widthOnly.height)
        // 4 video tracks (two clips at once in a transition) and 8 audio tracks keep the decoders of the mix at 16.
        // zh-CN: 4 条视频轨 (转场时同时两段) 与 8 条音频轨使混音解码器不超过 16 个.
        rejected("timeline.audio holds 9 tracks, at most 8", MediaStudioErrorCodes.LIMIT_EXCEEDED) {
            TimelineOptions.parse(timeline("video" to listOf(track(clip(a))), "audio" to List(9) { track(clip(music)) }))
        }
    }

    @Test
    fun `gpu fallback replaces shader transitions`() {
        assertEquals(TransitionType.DISSOLVE, TransitionType.WIPE.gpuFallback)
        assertEquals(TransitionType.DISSOLVE, TransitionType.BLUR.gpuFallback)
        assertEquals(TransitionType.CUT, TransitionType.FADE.gpuFallback)
        assertEquals(TransitionType.SLIDE, TransitionType.SLIDE.gpuFallback)
        val layout = layout(timeline("video" to listOf(track(clip(a), clip(b, "transition" to mapOf("type" to "wipe"))))))
        val replaced = layout.withTransitions { it.gpuFallback }
        assertEquals(TransitionType.DISSOLVE, replaced.videoTracks[0][0].out!!.type)
        assertEquals(TransitionType.DISSOLVE, replaced.videoTracks[0][1].into!!.type)
        assertEquals("positions stay", layout.videoTracks[0].map { it.atUs }, replaced.videoTracks[0].map { it.atUs })
    }

    @Test
    fun `mix sources fade at both ends`() {
        val source = MixSource(path = a, volume = 0.5f, lengthFrames = 10, fadeInFrames = 4, fadeOutFrames = 2)
        assertEquals(0f, source.gain(0), 0f)
        assertEquals(0.25f, source.gain(2), 1e-6f)
        assertEquals(0.5f, source.gain(5), 0f)
        assertEquals(0.25f, source.gain(8), 1e-6f)
        assertEquals(0f, source.gain(9), 0f)
        assertFalse(MixSource(path = a).hasFades)
        assertFalse("no fade out without a length", MixSource(path = a, fadeOutFrames = 3).hasFades)
    }

    @Test
    fun `shader state of a clip`() {
        val wipe = TransitionEffect.Part(TransitionType.WIPE, 1_000, TransitionDirection.LEFT, masked = true)
        val fade = TransitionEffect.Part(TransitionType.FADE, 500, TransitionDirection.LEFT, masked = false)
        val effect = TransitionEffect(lengthUs = 4_000, into = wipe, out = fade)
        effect.stateAt(250).let { state ->
            assertTrue(state.wipe)
            assertTrue("left reveals from the right edge", state.wipeFlip)
            assertFalse(state.wipeAxisY)
            assertEquals(0.25f, state.wipeThreshold, 1e-6f)
            assertTrue(state.wipeKeepBelow)
        }
        assertEquals(TransitionEffect.State(), effect.stateAt(2_000))
        assertEquals(0.5f, effect.stateAt(3_750).brightness, 1e-6f)
        val outgoingWipe = TransitionEffect(4_000, null, wipe.copy(direction = TransitionDirection.UP)).stateAt(3_500)
        assertFalse("the outgoing clip keeps the part not yet wiped", outgoingWipe.wipeKeepBelow)
        assertTrue(outgoingWipe.wipeAxisY)
        assertFalse(outgoingWipe.wipeFlip)
        assertFalse("the lower lane carries no mask", TransitionEffect(4_000, wipe.copy(masked = false), null).stateAt(100).wipe)
        val blur = TransitionEffect.Part(TransitionType.BLUR, 1_000, TransitionDirection.LEFT, masked = false)
        assertEquals(0.75f, TransitionEffect(4_000, blur, null).stateAt(250).blur, 1e-6f)
        assertEquals(0.25f, TransitionEffect(4_000, null, blur).stateAt(3_250).blur, 1e-6f)
    }

    @Test
    fun `project files keep the timeline and the editor state`() {
        val project = SlateProject(
            timeline("width" to 1080L, "video" to listOf(track(clip(a, "speed" to 1.5), clip(b, "transition" to mapOf("type" to "slide", "direction" to "up")))), "overlays" to listOf(mapOf("type" to "text", "text" to "A \"quote\"\n中"))),
            ui = mapOf("zoom" to 2.5, "selected" to listOf(0L, 1L)),
        )
        val json = project.toJson()
        assertTrue(json.startsWith("{\n  \"version\": 1,\n  \"width\": 1080,"))
        val read = SlateProject.parse(json)
        assertEquals(project, read)
        assertEquals(TransitionDirection.UP, read.spec.videoTracks[0][1].transition!!.direction)
        rejected("not a 3-Slate Studio project: the JSON is not an object") { SlateProject.parse("[1]") }
        rejected("the project needs a newer 3-Slate Studio (format version 2, this build reads 1)") { SlateProject.parse("{\"version\": 2}") }
        rejected("timeline.video must hold at least one clip") { SlateProject.parse("{\"version\": 1, \"ui\": {}}") }
        assertTrue(assertThrows(MediaStudioException::class.java) { SlateProject.parse("{\"a\": 1,}") }.detail.startsWith("not a 3-Slate Studio project: expected a key"))
        assertEquals(mapOf("x" to -12L, "y" to 0.5, "z" to 1e300, "s" to "é/"), Json.parse("{\"x\": -12, \"y\": 5e-1, \"z\": 1e300, \"s\": \"\\u00e9\\/\"}"))
    }

}

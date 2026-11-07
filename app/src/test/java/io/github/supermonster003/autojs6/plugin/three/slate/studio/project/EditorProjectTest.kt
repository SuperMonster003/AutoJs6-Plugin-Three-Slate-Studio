package io.github.supermonster003.autojs6.plugin.three.slate.studio.project

import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.timeline.TimelineCalls
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Json
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SlateProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineLayout
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineOptions
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Roadmap P4.3: the model of the timeline editor (edits, undo / redo, the timeline JSON and its check against the
 * plugin's validator and layout, `.slate.json` projects, the `compose` request and code).
 * zh-CN: 路线图 P4.3: 时间线编辑器的模型 (编辑操作, 撤销 / 重做, 时间线 JSON 及其与插件校验器和定位的一致性, `.slate.json` 工程,
 * `compose` 请求与代码).
 */
class EditorProjectTest {

    private val infos = mapOf(
        A to info(A, 10_000, 1920, 1080, audio = true),
        B to info(B, 8_000, 1920, 1080, audio = true),
        C to info(C, 6_000, 1080, 1920, audio = false),
        MUSIC to info(MUSIC, 40_000, null, null, audio = true),
    )
    private val probe: (String) -> MediaInfo = { path -> infos[path] ?: throw MediaStudioException.io("input not found: $path") }

    private fun source(path: String) = infos.getValue(path).let { EditOps.SourceInfo(path, it.durationMillis, it.width ?: 0, it.height ?: 0, it.hasVideo, it.hasAudio) }

    /** 3 clips + music + text / image overlays + cues: the shape of the 30 second acceptance project. zh-CN: 验收工程的形状. */
    private fun sample(): EditorProject {
        var project = EditorProject()
        val (p1, a) = EditOps.addVideo(project, 0, source(A))
        val (p2, b) = EditOps.addVideo(p1, 0, source(B))
        val (p3, c) = EditOps.addVideo(p2, 0, source(C))
        project = EditOps.trimVideoStart(p3, a, 1_000)
        project = EditOps.setSpeed(project, b, 1.5)
        project = EditOps.setTransition(project, b, TransitionModel("dissolve", 800))
        project = EditOps.setTransition(project, c, TransitionModel("wipe", 600, "up"))
        project = EditOps.setVolume(project, a, 0.5)
        val (p4, music) = EditOps.addAudio(project, 0, source(MUSIC))
        project = EditOps.trimAudioEnd(p4, music, 20_000)
        project = EditOps.setFades(project, music, 1_000, 2_000)
        val (p5, text) = EditOps.addText(project, 500, "Hello")
        project = EditOps.updateOverlay(p5, text) { it.copy(x = 50.0, y = 10.0, fontSize = 60.0) }
        val (p6, _) = EditOps.addImage(project, 0, "/sdcard/Pictures/logo.png")
        project = p6
        val (p7, _) = EditOps.addCue(project, 1_000, "First line")
        val (p8, _) = EditOps.addCue(p7, 4_000, "Second <i>line</i>")
        return p8
    }

    @Test
    fun placementsMatchThePluginLayout() {
        val project = sample()
        assertNull(TimelineJson.check(project, probe))
        val layout = TimelineLayout.resolve(TimelineJson.spec(project), probe)
        val model = project.videoTracks[0].clips.map { project.videoPlacements.getValue(it.id) }
        val placed = layout.videoTracks[0]
        assertEquals(3, placed.size)
        model.zip(placed).forEach { (m, p) ->
            assertTrue("start ${m.atUs} vs ${p.atUs}", kotlin.math.abs(m.atUs - p.atUs) <= TimelineLayout.AT_TOLERANCE_US)
            assertEquals(m.lengthUs, p.lengthUs)
        }
        // a: 9 s; b: 8 s at 1.5x = 5333 ms, 800 ms dissolve; c: 6 s, 600 ms wipe.
        assertEquals(9_000_000L + 5_333_333L - 800_000L + 6_000_000L - 600_000L, model.last().endUs)
        assertEquals(layout.durationUs, project.durationUs)
        val timeline = TimelineJson.write(project)
        @Suppress("UNCHECKED_CAST")
        val clips = ((timeline["video"] as List<Map<String, Any?>>)[0]["clips"] as List<Map<String, Any?>>)
        assertFalse("an overlapping transition leaves out at", clips[1].containsKey("at"))
        assertEquals(mapOf("type" to "wipe", "duration" to 600L, "direction" to "up"), clips[2]["transition"])
        assertEquals(0.5, clips[0]["volume"])
        assertEquals(1_000L, clips[0]["start"])
        assertFalse("the full source end is left out", clips[0].containsKey("end"))
    }

    @Test
    fun editsKeepTheTimelineValid() {
        var project = sample()
        val (a, b, c) = project.videoTracks[0].clips.map { it.id }
        // A long transition is shortened to what the clips hold. zh-CN: 过长的转场缩短到两段能容纳的长度.
        project = EditOps.setTransition(project, c, TransitionModel("dissolve", 3_000))
        assertTrue(project.videoTracks[0].clips[2].transition!!.durationMs <= 3_000)
        project = EditOps.trimVideoEnd(project, c, 300)
        assertNull(TimelineJson.check(project, probe))
        assertTrue(project.videoTracks[0].clips[2].transition == null || project.videoTracks[0].clips[2].transition!!.durationMs <= 300)
        // Split, delete, reorder, move: still valid; the first clip never has a transition.
        project = EditOps.splitVideo(project, a, 4_000)!!.first
        assertEquals(4, project.videoTracks[0].clips.size)
        assertEquals(4_000L, project.videoTracks[0].clips[0].endMs - project.videoTracks[0].clips[0].startMs)
        project = EditOps.reorderVideo(project, b, -1)
        project = EditOps.reorderVideo(project, project.videoTracks[0].clips[1].id, -1)
        assertNull(project.videoTracks[0].clips[0].transition)
        assertNull(TimelineJson.check(project, probe))
        project = EditOps.moveVideo(project, project.videoTracks[0].clips[0].id, 700)
        assertEquals(700_000L, project.videoPlacements.getValue(project.videoTracks[0].clips[0].id).atUs)
        project = EditOps.addVideoTrack(project)
        project = EditOps.moveVideoToTrack(project, b, 1)
        assertEquals(listOf(b), project.videoTracks[1].clips.map { it.id })
        assertNull(project.videoTracks[1].clips[0].transition)
        project = EditOps.deleteClip(project, project.videoTracks[0].clips[0].id)
        assertNull(project.videoTracks[0].clips[0].transition)
        assertNull(TimelineJson.check(project, probe))
        // Ranges: speed and volume are clamped like the validator. zh-CN: 速度与音量按校验器的范围截断.
        project = EditOps.setSpeed(project, b, 100.0)
        assertEquals(8.0, project.videoClip(b)!!.second.speed, 0.0)
        project = EditOps.setVolume(project, b, -1.0)
        assertEquals(0.0, project.videoClip(b)!!.second.volume, 0.0)
        assertNull(TimelineJson.check(project, probe))
        // Too short to split. zh-CN: 太短无法分割.
        val first = project.videoTracks[0].clips[0]
        assertNull(EditOps.splitVideo(project, first.id, project.videoPlacements.getValue(first.id).atUs / 1_000 + 50))
    }

    @Test
    fun undoAndRedoTwelveSteps() {
        val history = EditHistory(EditorProject())
        val states = mutableListOf(history.current)
        val (p, id) = EditOps.addVideo(history.current, 0, source(A))
        history.apply(p)
        states += history.current
        repeat(11) { step ->
            history.apply(EditOps.trimVideoEnd(history.current, id, 9_500L - step * 300L))
            states += history.current
        }
        assertEquals(12, history.undoSteps)
        for (index in states.indices.reversed().drop(1)) {
            assertTrue(history.undo())
            assertEquals(states[index], history.current)
        }
        assertFalse(history.undo())
        repeat(12) { assertTrue(history.redo()) }
        assertEquals(states.last(), history.current)
        // A drag is one step; a new edit clears redo. zh-CN: 一次拖动算一步; 新编辑清空重做.
        history.begin()
        repeat(20) { history.update(EditOps.moveVideo(history.current, id, 10)) }
        history.commit()
        assertEquals(200_000L, history.current.videoPlacements.getValue(id).atUs)
        assertTrue(history.undo())
        assertEquals(states.last(), history.current)
        assertTrue(history.canRedo)
        history.apply(EditOps.setVolume(history.current, id, 2.0))
        assertFalse(history.canRedo)
        // Capacity. zh-CN: 容量.
        val small = EditHistory(0, capacity = 10)
        repeat(15) { small.apply(it + 1) }
        repeat(10) { assertTrue(small.undo()) }
        assertFalse(small.undo())
        assertEquals(5, small.current)
    }

    @Test
    fun projectFilesRoundTrip() {
        val project = sample().let { EditOps.addAudioTrack(it) }.copy(export = ExportSettings("h265", 6_000_000L, "/sdcard/Movies/out.mp4"))
        val directory = Files.createTempDirectory("slate-project").toFile()
        try {
            val file = File(directory, "demo" + ProjectFiles.EXTENSION)
            ProjectFiles.save(file, project, ProjectFiles.EditorState(zoom = 80.0, playheadMs = 1_234), infos)
            val text = file.readText()
            // The file is a valid `compose` timeline plus version and ui. zh-CN: 文件是有效的时间线, 外加 version 与 ui.
            assertNotNull(SlateProject.parse(text).spec)
            @Suppress("UNCHECKED_CAST")
            val root = Json.parse(text) as Map<String, Any?>
            assertEquals(1L, root["version"])
            val probed = mutableListOf<String>()
            val (opened, state, used) = ProjectFiles.open(text, { path -> probed += path; probe(path) })
            assertEquals("stored sources need no probe", emptyList<String>(), probed)
            assertEquals(TimelineJson.write(project), TimelineJson.write(opened))
            assertEquals(project.export, opened.export)
            assertEquals(project.audioTracks.size, opened.audioTracks.size)
            assertEquals(80.0, state.zoom, 0.0)
            assertEquals(1_234L, state.playheadMs)
            assertEquals(project.sources.filter { it in infos }.toSet(), used.keys)
            assertEquals(project.durationUs, opened.durationUs)
        } finally {
            directory.deleteRecursively()
        }
        // A project without video cannot be saved; a script timeline opens. zh-CN: 没有视频的工程不能保存; 脚本的时间线能打开.
        assertThrows(MediaStudioException::class.java) { ProjectFiles.save(File("unused.slate.json"), EditorProject(), ProjectFiles.EditorState(), infos) }
        val script = """{"video": [{"clips": [{"src": "$A", "end": 4000}, {"src": "$B", "transition": {"type": "fade", "duration": 1000}, "at": 5000}]}],
            "overlays": [{"type": "text", "text": "Hi", "x": 960, "y": "10%", "font": "serif"}]}"""
        val (opened, _, _) = ProjectFiles.open(script, probe)
        val clips = opened.videoTracks[0].clips
        assertEquals(1_000L, clips[1].gapMs)
        assertEquals("fade", clips[1].transition!!.type)
        assertEquals(50.0, opened.overlays[0].x, 0.001)
        assertEquals("serif", opened.overlays[0].extra["font"])
        assertNull(TimelineJson.check(opened, probe))
    }

    @Test
    fun composeRequestAndCode() {
        val project = sample().copy(export = ExportSettings("h265", 4_000_000L, "/sdcard/Movies/3-Slate Studio/demo.mp4"))
        val request = TimelineCalls.request(project)
        assertEquals(MediaStudioOps.COMPOSE, request.schema.op)
        assertEquals(emptyList<String>(), request.inputs)
        @Suppress("UNCHECKED_CAST")
        val timeline = request.options[TimelineCalls.TIMELINE] as Map<String, Any?>
        assertEquals(TimelineJson.write(project), timeline)
        assertNotNull(TimelineOptions.parse(timeline))
        assertEquals("h265", request.options["codec"])
        assertEquals(4_000_000L, request.options["bitRate"])
        assertTrue(request.code, request.code.contains("video.compose("))
        assertTrue(request.code, request.code.contains("\"/sdcard/Movies/3-Slate Studio/demo.mp4\""))
        assertTrue(request.code, request.code.contains("codec: \"h265\""))
        // Defaults stay out of the options. zh-CN: 默认值不写进选项.
        assertEquals(emptyMap<String, Any?>(), TimelineCalls.options(EditorProject()))
    }

    @Test
    fun snappingPicksTheNearestTarget() {
        val project = sample()
        val targets = Snapping.targets(project, 2_500.0, exceptId = null)
        assertEquals(9_000.0, Snapping.snap(9_040.0, targets, 50.0), 0.0)
        assertEquals(9_200.0, Snapping.snap(9_200.0, targets, 50.0), 0.0)
        assertEquals(2_500.0, Snapping.snap(2_480.0, targets, 50.0), 0.0)
    }

    private fun info(path: String, duration: Long, width: Int?, height: Int?, audio: Boolean) = MediaInfo(
        path = path, sizeBytes = 1, durationMillis = duration, mime = "video/mp4", width = width, height = height, rotation = 0,
        frameRate = 30.0, bitRate = null, videoCodec = width?.let { "h264" }, audioCodec = if (audio) "aac" else null,
        sampleRate = if (audio) 44_100 else null, channels = if (audio) 2 else null, language = null, tracks = emptyList(),
    )

    private companion object {
        const val A = "/sdcard/Movies/a.mp4"
        const val B = "/sdcard/Movies/b.mp4"
        const val C = "/sdcard/Movies/c.mp4"
        const val MUSIC = "/sdcard/Music/bgm.m4a"
    }

}

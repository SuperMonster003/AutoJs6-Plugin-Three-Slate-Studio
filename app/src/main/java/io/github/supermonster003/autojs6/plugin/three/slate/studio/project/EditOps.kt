package io.github.supermonster003.autojs6.plugin.three.slate.studio.project

import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import kotlin.math.roundToLong

/**
 * The edits of the timeline editor (roadmap P4.3): add, trim, split, delete, move and reorder clips, speed, volume,
 * transform, transitions, audio fades, overlays and cues. Every edit returns a new [EditorProject] and keeps it valid
 * for `compose`: tracks ripple (a longer or later clip pushes the following clips of its track, a shorter or removed
 * one pulls them), clips last at least [MIN_CLIP_MS], a transition fits in both clips (D30, appendix B.4) and values
 * stay inside the ranges of the plugin's validators. Unknown ids leave the project as it is.
 * zh-CN: 时间线编辑器的编辑操作 (路线图 P4.3): 添加, 修剪, 分割, 删除, 移动与重排片段, 变速, 音量, 变换, 转场, 音频淡入淡出, 叠加物与
 * 字幕. 每个操作返回新的 [EditorProject] 并保持它对 `compose` 有效: 轨道按涟漪方式联动 (片段变长或后移时推后同轨后续片段, 变短或
 * 删除时拉前), 片段至少 [MIN_CLIP_MS], 转场能容纳于前后两段 (D30, 附录 B.4), 数值不超出插件校验器的范围. 未知 id 不改变工程.
 */
internal object EditOps {

    const val MIN_CLIP_MS = 100L
    const val DEFAULT_OVERLAY_MS = 3_000L
    const val DEFAULT_CUE_MS = 2_000L
    const val MIN_SCALE = 0.05
    const val MAX_SCALE = 8.0

    /** What the editor knows of a source after probing it. zh-CN: 探测后编辑器所知的源信息. */
    data class SourceInfo(val path: String, val durationMs: Long, val width: Int, val height: Int, val hasVideo: Boolean, val hasAudio: Boolean)

    // Tracks. zh-CN: 轨道.

    fun addVideoTrack(project: EditorProject): EditorProject {
        if (project.videoTracks.size >= EditorProject.MAX_VIDEO_TRACKS) return project
        val (id, next) = project.id()
        return next.copy(videoTracks = next.videoTracks + VideoTrackModel(id))
    }

    fun addAudioTrack(project: EditorProject): EditorProject {
        if (project.audioTracks.size >= EditorProject.MAX_AUDIO_TRACKS) return project
        val (id, next) = project.id()
        return next.copy(audioTracks = next.audioTracks + AudioTrackModel(id))
    }

    /** Removes an empty track; the last video track stays. zh-CN: 移除空轨道; 最后一条视频轨保留. */
    fun removeTrack(project: EditorProject, trackId: Long): EditorProject = when {
        project.videoTracks.any { it.id == trackId && it.clips.isEmpty() } && project.videoTracks.size > 1 ->
            project.copy(videoTracks = project.videoTracks.filterNot { it.id == trackId })
        project.audioTracks.any { it.id == trackId && it.clips.isEmpty() } ->
            project.copy(audioTracks = project.audioTracks.filterNot { it.id == trackId })
        else -> project
    }

    // Adding clips. zh-CN: 添加片段.

    /**
     * Adds [source] at the end of video track [track] (a file without video goes to an audio track instead).
     * zh-CN: 把 [source] 加到视频轨 [track] 的末尾 (没有视频的文件改为加到音频轨).
     */
    fun addVideo(project: EditorProject, track: Int, source: SourceInfo): Pair<EditorProject, Long> {
        if (!source.hasVideo) return addAudio(project, 0, source)
        if (source.durationMs < MIN_CLIP_MS) return project to -1
        val (id, next) = project.id()
        val clip = VideoClipModel(id, source.path, source.durationMs, 0L, source.durationMs, width = source.width, height = source.height, hasAudio = source.hasAudio)
        val index = track.coerceIn(0, next.videoTracks.lastIndex)
        return next.copy(videoTracks = next.videoTracks.mapIndexed { i, t -> if (i == index) t.copy(clips = t.clips + clip) else t }) to id
    }

    fun addAudio(project: EditorProject, track: Int, source: SourceInfo): Pair<EditorProject, Long> {
        if (!source.hasAudio || source.durationMs < MIN_CLIP_MS) return project to -1
        var current = project
        if (current.audioTracks.isEmpty()) current = addAudioTrack(current)
        val (id, next) = current.id()
        val clip = AudioClipModel(id, source.path, source.durationMs, 0L, source.durationMs)
        val index = track.coerceIn(0, next.audioTracks.lastIndex)
        return next.copy(audioTracks = next.audioTracks.mapIndexed { i, t -> if (i == index) t.copy(clips = t.clips + clip) else t }) to id
    }

    // Video clip edits. zh-CN: 视频片段编辑.

    /**
     * Moves a clip by [deltaMs] on its track: the gap before it changes, the following clips follow (ripple). A clip
     * entering with an overlapping transition is tied to the previous one and does not move.
     * zh-CN: 把片段在轨道上移动 [deltaMs]: 改变它之前的间隔, 后续片段随之移动 (涟漪). 以重叠转场进入的片段与上一段相连, 不移动.
     */
    fun moveVideo(project: EditorProject, clipId: Long, deltaMs: Long): EditorProject = updateVideo(project, clipId) { clips, index ->
        val clip = clips[index]
        if (index > 0 && clip.transition?.overlaps == true) return@updateVideo clips
        clips.replace(index, clip.copy(gapMs = (clip.gapMs + deltaMs).coerceAtLeast(0L)))
    }

    /** Swaps a clip with its neighbor ([offset] -1 or +1). zh-CN: 与相邻片段交换位置 ([offset] 为 -1 或 +1). */
    fun reorderVideo(project: EditorProject, clipId: Long, offset: Int): EditorProject = updateVideo(project, clipId) { clips, index ->
        val other = index + offset
        if (other !in clips.indices) return@updateVideo clips
        val list = clips.toMutableList()
        list[index] = clips[other].copy(gapMs = clips[index].gapMs)
        list[other] = clips[index].copy(gapMs = clips[other].gapMs)
        fixTransitions(list)
    }

    /** Moves a clip to the end of another video track. zh-CN: 把片段移到另一条视频轨的末尾. */
    fun moveVideoToTrack(project: EditorProject, clipId: Long, targetTrack: Int): EditorProject {
        val (track, clip) = project.videoClip(clipId) ?: return project
        if (targetTrack !in project.videoTracks.indices || targetTrack == track) return project
        val tracks = project.videoTracks.mapIndexed { i, t ->
            when (i) {
                track -> t.copy(clips = fixTransitions(t.clips.filterNot { it.id == clipId }))
                targetTrack -> t.copy(clips = fixTransitions(t.clips + clip.copy(gapMs = 0L, transition = null)))
                else -> t
            }
        }
        return project.copy(videoTracks = tracks)
    }

    /**
     * Sets the source in point; the clip keeps its timeline start (the content after it shifts) and the following
     * clips ripple. zh-CN: 设置源入点; 片段在时间线上的起点不变 (其后内容平移), 后续片段涟漪移动.
     */
    fun trimVideoStart(project: EditorProject, clipId: Long, startMs: Long): EditorProject = updateVideo(project, clipId) { clips, index ->
        val clip = clips[index]
        val start = startMs.coerceIn(0L, clip.endMs - MIN_CLIP_MS)
        fixTransitions(clips.replace(index, clip.copy(startMs = start)))
    }

    fun trimVideoEnd(project: EditorProject, clipId: Long, endMs: Long): EditorProject = updateVideo(project, clipId) { clips, index ->
        val clip = clips[index]
        val end = endMs.coerceIn(clip.startMs + MIN_CLIP_MS, clip.sourceMs)
        fixTransitions(clips.replace(index, clip.copy(endMs = end)))
    }

    /**
     * Splits a clip at timeline time [atMs] into two clips that play on without a gap; null when [atMs] is not at
     * least [MIN_CLIP_MS] inside the clip. zh-CN: 在时间线时刻 [atMs] 把片段分成无缝衔接的两段; [atMs] 距两端不足 [MIN_CLIP_MS] 时返回 null.
     */
    fun splitVideo(project: EditorProject, clipId: Long, atMs: Long): Pair<EditorProject, Long>? {
        val (track, clip) = project.videoClip(clipId) ?: return null
        val placement = project.videoPlacements[clipId] ?: return null
        val offsetMs = atMs - placement.atMs
        val sourceCut = clip.startMs + (offsetMs * clip.speed).roundToLong()
        if (sourceCut - clip.startMs < MIN_CLIP_MS || clip.endMs - sourceCut < MIN_CLIP_MS) return null
        val (id, next) = project.id()
        val first = clip.copy(endMs = sourceCut)
        val second = clip.copy(id = id, startMs = sourceCut, gapMs = 0L, transition = null)
        val tracks = next.videoTracks.mapIndexed { i, t ->
            if (i != track) t else {
                val index = t.clips.indexOfFirst { it.id == clipId }
                t.copy(clips = fixTransitions(t.clips.take(index) + first + second + t.clips.drop(index + 1)))
            }
        }
        return next.copy(videoTracks = tracks) to id
    }

    fun deleteClip(project: EditorProject, clipId: Long): EditorProject = when {
        project.videoClip(clipId) != null -> project.copy(videoTracks = project.videoTracks.map { t ->
            t.copy(clips = fixTransitions(t.clips.filterNot { it.id == clipId }))
        })
        project.audioClip(clipId) != null -> project.copy(audioTracks = project.audioTracks.map { t -> t.copy(clips = t.clips.filterNot { it.id == clipId }) })
        project.overlays.any { it.id == clipId } -> project.copy(overlays = project.overlays.filterNot { it.id == clipId })
        project.subtitles.cues.any { it.id == clipId } -> project.copy(subtitles = project.subtitles.copy(cues = project.subtitles.cues.filterNot { it.id == clipId }))
        else -> project
    }

    fun setSpeed(project: EditorProject, clipId: Long, speed: Double): EditorProject = updateVideo(project, clipId) { clips, index ->
        val value = speed.coerceIn(MediaStudioLimits.MIN_SPEED_FACTOR, MediaStudioLimits.MAX_SPEED_FACTOR)
        fixTransitions(clips.replace(index, clips[index].copy(speed = value)))
    }

    fun setVolume(project: EditorProject, clipId: Long, volume: Double): EditorProject {
        val value = volume.coerceIn(0.0, OperationOptions.MAX_GAIN)
        project.audioClip(clipId)?.let { return updateAudio(project, clipId) { clips, index -> clips.replace(index, clips[index].copy(volume = value)) } }
        return updateVideo(project, clipId) { clips, index -> clips.replace(index, clips[index].copy(volume = value)) }
    }

    fun setTransform(project: EditorProject, clipId: Long, scale: Double? = null, rotate: Int? = null, flip: String? = null, fit: String? = null): EditorProject =
        updateVideo(project, clipId) { clips, index ->
            val clip = clips[index]
            clips.replace(
                index,
                clip.copy(
                    scale = scale?.coerceIn(MIN_SCALE, MAX_SCALE) ?: clip.scale,
                    rotate = rotate?.let { ((it % 360) + 360) % 360 / 90 * 90 } ?: clip.rotate,
                    flip = flip?.takeIf { it in VideoClipModel.FLIPS } ?: clip.flip,
                    fit = fit?.takeIf { it in VideoClipModel.FITS } ?: clip.fit,
                ),
            )
        }

    /**
     * Sets the transition into a clip (null or `cut` removes it); the first clip of a track has none, the duration
     * is clamped to 100 - 3000 ms and to what both clips can hold.
     * zh-CN: 设置进入片段的转场 (null 或 `cut` 表示去掉); 轨道首段没有转场, 时长截断到 100 - 3000 ms 且不超过两段所能容纳的长度.
     */
    fun setTransition(project: EditorProject, clipId: Long, transition: TransitionModel?): EditorProject = updateVideo(project, clipId) { clips, index ->
        if (index == 0) return@updateVideo clips
        val value = transition?.takeIf { it.type != "cut" }?.let {
            it.copy(
                durationMs = it.durationMs.coerceIn(MediaStudioLimits.MIN_TRANSITION_MILLIS, MediaStudioLimits.MAX_TRANSITION_MILLIS),
                direction = if (it.kind.directional) it.direction ?: "left" else null,
            )
        }
        fixTransitions(clips.replace(index, clips[index].copy(transition = value, gapMs = if (value?.overlaps == true) 0L else clips[index].gapMs)))
    }

    /** The longest transition a clip can take now. zh-CN: 片段当前能容纳的最长转场. */
    fun maxTransitionMs(clips: List<VideoClipModel>, index: Int, type: String): Long {
        if (index <= 0 || index > clips.lastIndex) return 0L
        val previous = clips[index - 1]
        val before = if (index >= 2) previous.transition?.sideUs ?: 0L else 0L
        val roomUs = minOf(previous.lengthUs - before, clips[index].lengthUs - (clips.getOrNull(index + 1)?.transition?.sideUs ?: 0L))
        val factor = if (type == "fade") 2L else 1L
        return (roomUs * factor / 1_000L).coerceAtMost(MediaStudioLimits.MAX_TRANSITION_MILLIS)
    }

    /**
     * Keeps the transitions of a track valid after an edit: none on the first clip, each one shortened to fit, or
     * removed when even 100 ms does not fit.
     * zh-CN: 编辑之后保持轨道转场有效: 首段无转场, 其余缩短到能容纳的长度, 连 100 ms 都容纳不下时去掉.
     */
    fun fixTransitions(clips: List<VideoClipModel>): List<VideoClipModel> {
        val list = clips.toMutableList()
        list.indices.forEach { index ->
            val transition = list[index].transition ?: return@forEach
            if (index == 0) {
                list[index] = list[index].copy(transition = null)
                return@forEach
            }
            val max = maxTransitionMs(list, index, transition.type)
            list[index] = when {
                max < MediaStudioLimits.MIN_TRANSITION_MILLIS -> list[index].copy(transition = null)
                transition.durationMs > max -> list[index].copy(transition = transition.copy(durationMs = max))
                else -> list[index]
            }
        }
        return list
    }

    // Audio clip edits. zh-CN: 音频片段编辑.

    fun moveAudio(project: EditorProject, clipId: Long, deltaMs: Long): EditorProject = updateAudio(project, clipId) { clips, index ->
        clips.replace(index, clips[index].copy(gapMs = (clips[index].gapMs + deltaMs).coerceAtLeast(0L)))
    }

    fun trimAudioStart(project: EditorProject, clipId: Long, startMs: Long): EditorProject = updateAudio(project, clipId) { clips, index ->
        val clip = clips[index]
        clips.replace(index, fitFades(clip.copy(startMs = startMs.coerceIn(0L, clip.endMs - MIN_CLIP_MS))))
    }

    fun trimAudioEnd(project: EditorProject, clipId: Long, endMs: Long): EditorProject = updateAudio(project, clipId) { clips, index ->
        val clip = clips[index]
        clips.replace(index, fitFades(clip.copy(endMs = endMs.coerceIn(clip.startMs + MIN_CLIP_MS, clip.sourceMs))))
    }

    fun splitAudio(project: EditorProject, clipId: Long, atMs: Long): Pair<EditorProject, Long>? {
        val (track, clip) = project.audioClip(clipId) ?: return null
        val placement = project.audioPlacements[clipId] ?: return null
        val sourceCut = clip.startMs + (atMs - placement.atMs).roundToLong()
        if (sourceCut - clip.startMs < MIN_CLIP_MS || clip.endMs - sourceCut < MIN_CLIP_MS) return null
        val (id, next) = project.id()
        val first = fitFades(clip.copy(endMs = sourceCut, fadeOutMs = 0L))
        val second = fitFades(clip.copy(id = id, startMs = sourceCut, gapMs = 0L, fadeInMs = 0L))
        val tracks = next.audioTracks.mapIndexed { i, t ->
            if (i != track) t else {
                val index = t.clips.indexOfFirst { it.id == clipId }
                t.copy(clips = t.clips.take(index) + first + second + t.clips.drop(index + 1))
            }
        }
        return next.copy(audioTracks = tracks) to id
    }

    fun setFades(project: EditorProject, clipId: Long, fadeInMs: Long, fadeOutMs: Long): EditorProject = updateAudio(project, clipId) { clips, index ->
        clips.replace(index, fitFades(clips[index].copy(fadeInMs = fadeInMs.coerceAtLeast(0L), fadeOutMs = fadeOutMs.coerceAtLeast(0L))))
    }

    /** Fades together fit in the clip (appendix B.4). zh-CN: 淡入淡出之和不超过片段长度 (附录 B.4). */
    private fun fitFades(clip: AudioClipModel): AudioClipModel {
        val length = clip.endMs - clip.startMs
        val fadeIn = clip.fadeInMs.coerceAtMost(length)
        return clip.copy(fadeInMs = fadeIn, fadeOutMs = clip.fadeOutMs.coerceAtMost(length - fadeIn))
    }

    // Overlays. zh-CN: 叠加物.

    fun addText(project: EditorProject, atMs: Long, text: String): Pair<EditorProject, Long> {
        val (id, next) = project.id()
        val overlay = OverlayModel(id, OverlayModel.TEXT, atMs.coerceAtLeast(0L), atMs.coerceAtLeast(0L) + DEFAULT_OVERLAY_MS, text = text)
        return next.copy(overlays = next.overlays + overlay) to id
    }

    fun addImage(project: EditorProject, atMs: Long, src: String): Pair<EditorProject, Long> {
        val (id, next) = project.id()
        val overlay = OverlayModel(id, OverlayModel.IMAGE, atMs.coerceAtLeast(0L), atMs.coerceAtLeast(0L) + DEFAULT_OVERLAY_MS, src = src, stroke = null)
        return next.copy(overlays = next.overlays + overlay) to id
    }

    fun updateOverlay(project: EditorProject, id: Long, change: (OverlayModel) -> OverlayModel): EditorProject =
        project.copy(overlays = project.overlays.map { overlay ->
            if (overlay.id != id) overlay else {
                val next = change(overlay)
                val from = next.fromMs.coerceAtLeast(0L)
                next.copy(
                    fromMs = from,
                    toMs = next.toMs.coerceAtLeast(from + MIN_CLIP_MS),
                    x = next.x.coerceIn(0.0, 100.0),
                    y = next.y.coerceIn(0.0, 100.0),
                    fontSize = next.fontSize.coerceIn(1.0, MAX_FONT_SIZE),
                    scale = next.scale.coerceIn(MIN_SCALE, MAX_SCALE),
                    alpha = next.alpha.coerceIn(0.0, 1.0),
                )
            }
        })

    /** Moves an overlay in time by [deltaMs], keeping its length. zh-CN: 把叠加物在时间上移动 [deltaMs], 长度不变. */
    fun moveOverlay(project: EditorProject, id: Long, deltaMs: Long): EditorProject = updateOverlay(project, id) { overlay ->
        val delta = deltaMs.coerceAtLeast(-overlay.fromMs)
        overlay.copy(fromMs = overlay.fromMs + delta, toMs = overlay.toMs + delta)
    }

    // Subtitles. zh-CN: 字幕.

    fun addCue(project: EditorProject, atMs: Long, text: String): Pair<EditorProject, Long> {
        val (id, next) = project.id()
        val start = atMs.coerceAtLeast(0L)
        val cue = CueModel(id, start, start + DEFAULT_CUE_MS, text)
        return next.copy(subtitles = next.subtitles.copy(cues = (next.subtitles.cues + cue).sortedBy { it.startMs })) to id
    }

    fun updateCue(project: EditorProject, id: Long, startMs: Long? = null, endMs: Long? = null, text: String? = null): EditorProject =
        project.copy(subtitles = project.subtitles.copy(cues = project.subtitles.cues.map { cue ->
            if (cue.id != id) cue else {
                val start = (startMs ?: cue.startMs).coerceAtLeast(0L)
                cue.copy(startMs = start, endMs = (endMs ?: cue.endMs).coerceAtLeast(start + MIN_CLIP_MS), text = text ?: cue.text)
            }
        }.sortedBy { it.startMs }))

    fun moveCue(project: EditorProject, id: Long, deltaMs: Long): EditorProject {
        val cue = project.subtitles.cues.firstOrNull { it.id == id } ?: return project
        val delta = deltaMs.coerceAtLeast(-cue.startMs)
        return updateCue(project, id, cue.startMs + delta, cue.endMs + delta)
    }

    /**
     * Replaces (or, with [append], adds to) the cues with imported ones (start, end, plain or SRT-marked text).
     * zh-CN: 用导入的 cue (起止与纯文本或带 SRT 标记的文本) 替换现有 cue ([append] 时追加).
     */
    fun importCues(project: EditorProject, cues: List<Triple<Long, Long, String>>, append: Boolean): EditorProject {
        var next = if (append) project else project.copy(subtitles = project.subtitles.copy(cues = emptyList()))
        val list = next.subtitles.cues.toMutableList()
        cues.forEach { (start, end, text) ->
            if (text.isBlank() || end <= start) return@forEach
            val (id, advanced) = next.id()
            next = advanced
            list += CueModel(id, start, end, text.take(MAX_CUE_CHARS))
        }
        return next.copy(subtitles = next.subtitles.copy(cues = list.sortedBy { it.startMs }.take(MAX_CUES)))
    }

    fun setSubtitleStyle(project: EditorProject, change: (SubtitleLayer) -> SubtitleLayer): EditorProject {
        val next = change(project.subtitles)
        return project.copy(subtitles = next.copy(cues = project.subtitles.cues, fontSize = next.fontSize?.coerceIn(1.0, MAX_FONT_SIZE)))
    }

    const val MAX_FONT_SIZE = 1_000.0
    const val MAX_CUES = 10_000
    const val MAX_CUE_CHARS = 1_000

    // Helpers. zh-CN: 辅助函数.

    private fun updateVideo(project: EditorProject, clipId: Long, change: (List<VideoClipModel>, Int) -> List<VideoClipModel>): EditorProject {
        val tracks = project.videoTracks.map { track ->
            val index = track.clips.indexOfFirst { it.id == clipId }
            if (index < 0) track else track.copy(clips = change(track.clips, index))
        }
        return if (tracks == project.videoTracks) project else project.copy(videoTracks = tracks)
    }

    private fun updateAudio(project: EditorProject, clipId: Long, change: (List<AudioClipModel>, Int) -> List<AudioClipModel>): EditorProject {
        val tracks = project.audioTracks.map { track ->
            val index = track.clips.indexOfFirst { it.id == clipId }
            if (index < 0) track else track.copy(clips = change(track.clips, index))
        }
        return if (tracks == project.audioTracks) project else project.copy(audioTracks = tracks)
    }

    private fun <T> List<T>.replace(index: Int, value: T): List<T> = toMutableList().also { it[index] = value }

}

/**
 * Snapping of the timeline editor: a time within [thresholdMs] of a target (0, the playhead, clip edges) jumps to the
 * nearest target. zh-CN: 时间线编辑器的吸附: 距目标 (0, 播放头, 片段边缘) 不超过 [thresholdMs] 的时刻吸附到最近的目标.
 */
internal object Snapping {

    fun snap(valueMs: Double, targetsMs: Collection<Double>, thresholdMs: Double): Double {
        val nearest = targetsMs.minByOrNull { kotlin.math.abs(it - valueMs) } ?: return valueMs
        return if (kotlin.math.abs(nearest - valueMs) <= thresholdMs) nearest else valueMs
    }

    /** Edges of every clip, overlay and cue except those of [exceptId]. zh-CN: 除 [exceptId] 外所有片段, 叠加物与字幕的边缘. */
    fun targets(project: EditorProject, playheadMs: Double, exceptId: Long?): List<Double> = buildList {
        add(0.0)
        add(playheadMs)
        (project.videoPlacements + project.audioPlacements).forEach { (id, placement) ->
            if (id != exceptId) {
                add(placement.atMs)
                add(placement.endMs)
            }
        }
        project.overlays.forEach { if (it.id != exceptId) { add(it.fromMs.toDouble()); add(it.toMs.toDouble()) } }
        project.subtitles.cues.forEach { if (it.id != exceptId) { add(it.startMs.toDouble()); add(it.endMs.toDouble()) } }
    }

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.project

import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SlateProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineLayout
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineSpec
import java.io.File
import java.util.Locale

/**
 * The editor model as the `compose` timeline object of appendix B.4 and back (roadmap P4.3, D33). Writing leaves out
 * defaults and empty parts; a clip that follows the previous one without a gap, or enters with an overlapping
 * transition, has no `at` (the plugin places it, so rounding of speed changes never breaks the overlap rule). Numbers
 * are Long when integral, Double otherwise, like `BundleValues`. [check] runs the plugin's own validator and layout
 * with the probe of the editor, so what the editor accepts is what `compose` accepts.
 * zh-CN: 编辑器模型与附录 B.4 的 `compose` 时间线对象的互转 (路线图 P4.3, D33). 写出时省略默认值与空的部分; 紧接上一段 (无间隔) 或以
 * 重叠转场进入的片段不写 `at` (由插件定位, 变速的取整不会破坏重叠规则). 数字为整数时用 Long, 否则用 Double, 与 `BundleValues` 一致.
 * [check] 用编辑器的探测结果运行插件自己的校验器与定位, 编辑器接受的即 `compose` 接受的.
 */
internal object TimelineJson {

    fun write(project: EditorProject): LinkedHashMap<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        project.width?.let { map["width"] = it.toLong() }
        project.height?.let { map["height"] = it.toLong() }
        if (project.frameRate != EditorProject.DEFAULT_FRAME_RATE) map["frameRate"] = project.frameRate.toLong()
        if (!project.background.equals(EditorProject.DEFAULT_BACKGROUND, ignoreCase = true)) map["background"] = project.background
        map["video"] = project.videoTracks.filter { it.clips.isNotEmpty() }.map { track ->
            linkedMapOf<String, Any?>("clips" to videoClips(track.clips))
        }
        val audio = project.audioTracks.filter { it.clips.isNotEmpty() }
        if (audio.isNotEmpty()) map["audio"] = audio.map { track -> linkedMapOf<String, Any?>("clips" to audioClips(track.clips)) }
        if (project.overlays.isNotEmpty()) map["overlays"] = project.overlays.map(::overlay)
        if (project.subtitles.cues.isNotEmpty()) map["subtitles"] = subtitles(project.subtitles)
        return map
    }

    private fun videoClips(clips: List<VideoClipModel>): List<Map<String, Any?>> {
        var previousEndUs = 0L
        return clips.mapIndexed { index, clip ->
            val map = linkedMapOf<String, Any?>("src" to clip.src)
            if (clip.startMs > 0) map["start"] = clip.startMs
            if (clip.endMs < clip.sourceMs) map["end"] = clip.endMs
            val overlapping = index > 0 && clip.transition?.overlaps == true
            when {
                index == 0 -> if (clip.gapMs > 0) map["at"] = clip.gapMs
                !overlapping && clip.gapMs > 0 -> map["at"] = (previousEndUs + 999) / 1_000 + clip.gapMs
            }
            if (clip.speed != 1.0) map["speed"] = number(clip.speed)
            if (clip.volume != 1.0) map["volume"] = number(clip.volume)
            val transform = linkedMapOf<String, Any?>()
            if (clip.scale != 1.0) transform["scale"] = number(clip.scale)
            if (clip.rotate != 0) transform["rotate"] = clip.rotate.toLong()
            clip.crop?.let { transform["crop"] = it }
            if (clip.flip != VideoClipModel.FLIP_NONE) transform["flip"] = clip.flip
            if (clip.fit != VideoClipModel.FIT_CONTAIN) transform["fit"] = clip.fit
            if (transform.isNotEmpty()) map["transform"] = transform
            clip.transition?.let { transition ->
                map["transition"] = linkedMapOf<String, Any?>("type" to transition.type, "duration" to transition.durationMs).apply {
                    transition.direction?.let { put("direction", it) }
                }
            }
            // A written `at` is rounded up to whole milliseconds; the plugin then places the clip there.
            // zh-CN: 写出的 `at` 向上取整到毫秒; 插件随后把片段放在那里.
            val atUs = when {
                map["at"] is Long -> (map["at"] as Long) * 1_000L
                index == 0 -> 0L
                overlapping -> previousEndUs - clip.transition.durationMs * 1_000L
                else -> previousEndUs
            }
            previousEndUs = atUs + clip.lengthUs
            map
        }
    }

    private fun audioClips(clips: List<AudioClipModel>): List<Map<String, Any?>> {
        var previousEndMs = 0L
        return clips.mapIndexed { index, clip ->
            val map = linkedMapOf<String, Any?>("src" to clip.src)
            if (clip.startMs > 0) map["start"] = clip.startMs
            if (clip.endMs < clip.sourceMs) map["end"] = clip.endMs
            if (clip.gapMs > 0) map["at"] = (if (index == 0) 0L else previousEndMs) + clip.gapMs
            if (clip.volume != 1.0) map["volume"] = number(clip.volume)
            if (clip.fadeInMs > 0) map["fadeIn"] = clip.fadeInMs
            if (clip.fadeOutMs > 0) map["fadeOut"] = clip.fadeOutMs
            previousEndMs = (map["at"] as? Long ?: previousEndMs) + (clip.endMs - clip.startMs)
            map
        }
    }

    private fun overlay(overlay: OverlayModel): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>("type" to overlay.type)
        if (overlay.type == OverlayModel.TEXT) map["text"] = overlay.text.orEmpty() else map["src"] = overlay.src
        map["from"] = overlay.fromMs
        map["to"] = overlay.toMs
        map["x"] = percent(overlay.x)
        map["y"] = percent(overlay.y)
        if (overlay.type == OverlayModel.TEXT) {
            map["fontSize"] = number(overlay.fontSize)
            map["color"] = overlay.color
            overlay.stroke?.let { map["stroke"] = it }
        } else if (overlay.scale != 1.0) {
            map["scale"] = number(overlay.scale)
        }
        if (overlay.alpha != 1.0) map["alpha"] = number(overlay.alpha)
        overlay.extra.forEach { (key, value) -> if (key !in map) map[key] = value }
        return map
    }

    private fun subtitles(layer: SubtitleLayer): Map<String, Any?> {
        val style = linkedMapOf<String, Any?>()
        layer.fontSize?.let { style["fontSize"] = number(it) }
        style["color"] = layer.color
        layer.stroke?.let { style["stroke"] = it }
        layer.background?.let { style["background"] = it }
        style["align"] = layer.align
        layer.extra.forEach { (key, value) -> if (key !in style) style[key] = value }
        return linkedMapOf(
            "cues" to layer.cues.map { linkedMapOf<String, Any?>("start" to it.startMs, "end" to it.endMs, "text" to it.text) },
            "style" to style,
        )
    }

    /** The parsed spec of [project] (field path messages from `timeline.`). zh-CN: [project] 解析后的规格 (消息带 `timeline.` 字段路径). */
    fun spec(project: EditorProject): TimelineSpec = TimelineOptions.parse(write(project))

    /**
     * Checks [project] with the plugin's validator and layout; null when `compose` accepts it, otherwise the message.
     * zh-CN: 用插件的校验器与定位检查 [project]; `compose` 接受时返回 null, 否则返回消息.
     */
    fun check(project: EditorProject, probe: (String) -> MediaInfo): String? = try {
        TimelineLayout.resolve(spec(project), probe)
        null
    } catch (e: MediaStudioException) {
        e.detail
    }

    /**
     * Builds the editor model from a timeline object (a project file or a script's JSON), placing it with the plugin
     * layout; [probe] answers the sources. Keys the editor does not edit (crop, fonts, anchors, shadows, ...) are kept.
     * zh-CN: 由时间线对象 (工程文件或脚本的 JSON) 构建编辑器模型, 用插件的定位方式放置; [probe] 提供源信息. 编辑器不编辑的键 (裁切,
     * 字体, 锚点, 阴影等) 原样保留.
     */
    fun read(timeline: Map<String, Any?>, probe: (String) -> MediaInfo, cuesOf: (String) -> List<Triple<Long, Long, String>> = { emptyList() }): EditorProject {
        val spec = TimelineOptions.parse(timeline)
        val layout = TimelineLayout.resolve(spec, probe)
        var nextId = 1L
        fun id() = nextId++
        val videoTracks = layout.videoTracks.mapIndexed { track, placed ->
            val raw = rawClips(timeline, "video", track)
            var previousEndUs = 0L
            VideoTrackModel(id(), placed.mapIndexed { index, clip ->
                val gapUs = if (index == 0) clip.atUs else if (clip.into?.type?.overlaps == true) 0L else clip.atUs - previousEndUs
                previousEndUs = clip.endUs
                @Suppress("UNCHECKED_CAST")
                val crop = ((raw.getOrNull(index)?.get("transform") as? Map<String, Any?>)?.get("crop")) as? Map<String, Any?>
                VideoClipModel(
                    id = id(),
                    src = clip.clip.src,
                    sourceMs = clip.info.durationMillis,
                    startMs = clip.sourceStartUs / 1_000L,
                    endMs = clip.sourceEndUs / 1_000L,
                    gapMs = (gapUs / 1_000L).coerceAtLeast(0L),
                    speed = clip.clip.speed,
                    volume = clip.clip.volume,
                    scale = clip.clip.transform.scale,
                    rotate = clip.clip.transform.rotate,
                    flip = clip.clip.transform.flip.id,
                    fit = clip.clip.transform.fit.id,
                    transition = clip.into?.let { TransitionModel(it.type.id, it.durationMillis, it.direction.id.takeIf { _ -> it.type.directional }) },
                    width = clip.info.width ?: 0,
                    height = clip.info.height ?: 0,
                    hasAudio = clip.info.hasAudio,
                    crop = crop,
                )
            })
        }
        var audioIndex = 0
        val audioTracks = spec.audioTracks.map { clips ->
            var previousEndUs = 0L
            AudioTrackModel(id(), clips.mapIndexed { index, _ ->
                val placed = layout.audio[audioIndex++]
                val gapUs = placed.atUs - (if (index == 0) 0L else previousEndUs)
                previousEndUs = placed.endUs
                AudioClipModel(
                    id = id(),
                    src = placed.clip.src,
                    sourceMs = placed.info.durationMillis,
                    startMs = placed.sourceStartUs / 1_000L,
                    endMs = placed.sourceEndUs / 1_000L,
                    gapMs = (gapUs / 1_000L).coerceAtLeast(0L),
                    volume = placed.clip.volume,
                    fadeInMs = placed.clip.fadeInMillis,
                    fadeOutMs = placed.clip.fadeOutMillis,
                )
            })
        }
        val overlays = (timeline["overlays"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().map { raw ->
            @Suppress("UNCHECKED_CAST")
            overlayOf(id(), raw as Map<String, Any?>, layout.width, layout.height, layout.durationMillis)
        }
        @Suppress("UNCHECKED_CAST")
        val subtitles = (timeline["subtitles"] as? Map<String, Any?>)?.let { raw ->
            val cues = (raw["cues"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.map { cue ->
                Triple((cue["start"] as Number).toLong(), (cue["end"] as Number).toLong(), cue["text"].toString())
            } ?: (raw["src"] as? String)?.let(cuesOf).orEmpty()
            val style = raw["style"] as? Map<String, Any?> ?: emptyMap()
            SubtitleLayer(
                cues = cues.map { (start, end, text) -> CueModel(id(), start, end, text) },
                fontSize = (style["fontSize"] as? Number)?.toDouble(),
                color = style["color"] as? String ?: "#FFFFFF",
                stroke = style["stroke"] as? String ?: if (style.containsKey("stroke")) null else "#000000",
                background = style["background"] as? String,
                align = style["align"] as? String ?: SubtitleLayer.ALIGN_BOTTOM,
                extra = style.filterKeys { it !in SUBTITLE_KEYS },
            )
        } ?: SubtitleLayer()
        return EditorProject(
            width = spec.width,
            height = spec.height,
            frameRate = spec.frameRate,
            background = (timeline["background"] as? String) ?: EditorProject.DEFAULT_BACKGROUND,
            videoTracks = videoTracks,
            audioTracks = audioTracks.ifEmpty { listOf(AudioTrackModel(id())) },
            overlays = overlays,
            subtitles = subtitles,
            nextId = nextId,
        )
    }

    private fun rawClips(timeline: Map<String, Any?>, key: String, track: Int): List<Map<String, Any?>> {
        @Suppress("UNCHECKED_CAST")
        val tracks = (timeline[key] as? List<*>)?.filterIsInstance<Map<String, Any?>>()?.filter { (it["clips"] as? List<*>)?.isNotEmpty() == true }
        @Suppress("UNCHECKED_CAST")
        return (tracks?.getOrNull(track)?.get("clips") as? List<*>)?.filterIsInstance<Map<String, Any?>>().orEmpty()
    }

    private fun overlayOf(id: Long, raw: Map<String, Any?>, width: Int, height: Int, durationMs: Long): OverlayModel {
        val type = raw["type"] as? String ?: OverlayModel.TEXT
        return OverlayModel(
            id = id,
            type = type,
            fromMs = (raw["from"] as? Number)?.toLong() ?: 0L,
            toMs = (raw["to"] as? Number)?.toLong() ?: durationMs,
            x = percentOf(raw["x"], width),
            y = percentOf(raw["y"], height),
            text = raw["text"] as? String,
            fontSize = (raw["fontSize"] as? Number)?.toDouble() ?: OverlayModel.DEFAULT_FONT_SIZE,
            color = raw["color"] as? String ?: "#FFFFFF",
            stroke = raw["stroke"] as? String,
            src = raw["src"] as? String,
            scale = (raw["scale"] as? Number)?.toDouble() ?: 1.0,
            alpha = (raw["alpha"] as? Number)?.toDouble() ?: 1.0,
            extra = raw.filterKeys { it !in OVERLAY_KEYS },
        )
    }

    /** "50%", "12.5%" or pixels (as a share of [size]). zh-CN: "50%", "12.5%" 或像素 (按 [size] 换算). */
    private fun percentOf(value: Any?, size: Int): Double = when (value) {
        is String -> value.trim().removeSuffix("%").toDoubleOrNull() ?: 50.0
        is Number -> if (size > 0) value.toDouble() * 100.0 / size else 50.0
        else -> 50.0
    }.coerceIn(0.0, 100.0)

    fun percent(value: Double): String =
        if (value == Math.rint(value)) "${value.toLong()}%" else String.format(Locale.ROOT, "%.2f%%", value).replace(Regex("0+%$"), "%")

    /** Long when integral (as the JSON reader), Double otherwise. zh-CN: 整数为 Long (与 JSON 读取一致), 否则为 Double. */
    fun number(value: Double): Any = if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) value.toLong() else value

    private val OVERLAY_KEYS = setOf("type", "text", "src", "from", "to", "x", "y", "fontSize", "color", "stroke", "scale", "alpha")
    private val SUBTITLE_KEYS = setOf("fontSize", "color", "stroke", "background", "align")

}

/**
 * `.slate.json` projects of the editor (roadmap P4.3, D33): the timeline object of [TimelineJson] plus `ui`, the editor
 * state (zoom, playhead, selection, the export settings of `compose` and the probed sources, so a project opens without
 * probing again). Written through [SlateProject], so a saved project is checked like `compose` checks it; a project
 * without a video clip cannot be saved (`compose` needs one).
 * zh-CN: 编辑器的 `.slate.json` 工程 (路线图 P4.3, D33): [TimelineJson] 的时间线对象加 `ui` (编辑器状态: 缩放, 播放头, 选中项, `compose`
 * 的导出设置与已探测的源信息, 打开工程时不必重新探测). 经 [SlateProject] 写出, 保存的工程与 `compose` 一样经过校验; 没有视频片段的
 * 工程无法保存 (`compose` 需要至少一段).
 */
internal object ProjectFiles {

    const val EXTENSION = SlateProject.EXTENSION

    /** Editor state stored in `ui`. zh-CN: 保存在 `ui` 中的编辑器状态. */
    data class EditorState(val zoom: Double = DEFAULT_ZOOM, val playheadMs: Long = 0L, val selected: Long? = null) {
        companion object {
            /** Pixels (dp) per second. zh-CN: 每秒的像素数 (dp). */
            const val DEFAULT_ZOOM = 60.0
        }
    }

    fun toSlate(project: EditorProject, state: EditorState, sources: Map<String, MediaInfo>): SlateProject {
        val ui = linkedMapOf<String, Any?>(
            "editor" to EDITOR_FORMAT,
            "zoom" to TimelineJson.number(state.zoom),
            "playhead" to state.playheadMs,
            "videoTracks" to project.videoTracks.size.toLong(),
            "audioTracks" to project.audioTracks.size.toLong(),
        )
        val export = linkedMapOf<String, Any?>("codec" to project.export.codec)
        project.export.bitRate?.let { export["bitRate"] = it }
        project.export.output?.let { export["output"] = it }
        ui["export"] = export
        ui["sources"] = project.sources.mapNotNull { path -> sources[path]?.let { path to sourceMap(it) } }.toMap(LinkedHashMap())
        return SlateProject(TimelineJson.write(project), ui)
    }

    /** Writes [project] to [file] (checked by [SlateProject.write]). zh-CN: 把 [project] 写到 [file] (经 [SlateProject.write] 校验). */
    fun save(file: File, project: EditorProject, state: EditorState, sources: Map<String, MediaInfo>) {
        if (project.isEmpty) throw MediaStudioException.invalid("the project holds no video clip")
        val slate = toSlate(project, state, sources)
        slate.spec
        file.parentFile?.let { check(it.isDirectory || it.mkdirs()) { "cannot create $it" } }
        slate.write(file)
    }

    /**
     * Opens a project; [probe] answers sources the file does not describe (or whose size or date changed).
     * zh-CN: 打开工程; [probe] 提供文件中未记录的源信息 (或大小, 日期已变化的源).
     */
    fun open(
        text: String,
        probe: (String) -> MediaInfo,
        cuesOf: (String) -> List<Triple<Long, Long, String>> = { emptyList() },
    ): Triple<EditorProject, EditorState, Map<String, MediaInfo>> {
        val slate = SlateProject.parse(text)
        val known = HashMap<String, MediaInfo>()
        @Suppress("UNCHECKED_CAST")
        (slate.ui["sources"] as? Map<String, Any?>)?.forEach { (path, value) ->
            (value as? Map<String, Any?>)?.let { infoOf(path, it) }?.let { known[path] = it }
        }
        val used = HashMap<String, MediaInfo>()
        val project = TimelineJson.read(slate.timeline, { path ->
            used.getOrPut(path) { known[path]?.takeIf { current(it) } ?: probe(path) }
        }, cuesOf)
        val ui = slate.ui
        @Suppress("UNCHECKED_CAST")
        val export = ui["export"] as? Map<String, Any?>
        var result = project.copy(
            export = ExportSettings(
                codec = export?.get("codec") as? String ?: "h264",
                bitRate = (export?.get("bitRate") as? Number)?.toLong(),
                output = export?.get("output") as? String,
            ),
        )
        // Empty tracks of the editor are not in the timeline; the ui keeps their count.
        // zh-CN: 编辑器的空轨道不在时间线里; ui 记录轨道数.
        val videoCount = ((ui["videoTracks"] as? Number)?.toInt() ?: 0).coerceAtMost(EditorProject.MAX_VIDEO_TRACKS)
        while (result.videoTracks.size < videoCount) result = EditOps.addVideoTrack(result)
        val audioCount = ((ui["audioTracks"] as? Number)?.toInt() ?: 0).coerceAtMost(EditorProject.MAX_AUDIO_TRACKS)
        while (result.audioTracks.size < audioCount) result = EditOps.addAudioTrack(result)
        val state = EditorState(
            zoom = (ui["zoom"] as? Number)?.toDouble() ?: EditorState.DEFAULT_ZOOM,
            playheadMs = (ui["playhead"] as? Number)?.toLong() ?: 0L,
        )
        return Triple(result, state, used)
    }

    private fun sourceMap(info: MediaInfo): Map<String, Any?> = linkedMapOf(
        "durationMs" to info.durationMillis,
        "width" to info.width?.toLong(),
        "height" to info.height?.toLong(),
        "video" to info.hasVideo,
        "audio" to info.hasAudio,
        "sampleRate" to info.sampleRate?.toLong(),
        "channels" to info.channels?.toLong(),
        "sizeBytes" to info.sizeBytes,
    ).filterValues { it != null }.toMap(LinkedHashMap())

    private fun infoOf(path: String, map: Map<String, Any?>): MediaInfo? {
        val duration = (map["durationMs"] as? Number)?.toLong() ?: return null
        return MediaInfo(
            path = path,
            sizeBytes = (map["sizeBytes"] as? Number)?.toLong() ?: -1L,
            durationMillis = duration,
            mime = null,
            width = (map["width"] as? Number)?.toInt(),
            height = (map["height"] as? Number)?.toInt(),
            rotation = 0,
            frameRate = null,
            bitRate = null,
            videoCodec = if (map["video"] == true) "stored" else null,
            audioCodec = if (map["audio"] == true) "stored" else null,
            sampleRate = (map["sampleRate"] as? Number)?.toInt(),
            channels = (map["channels"] as? Number)?.toInt(),
            language = null,
            tracks = emptyList(),
        )
    }

    /** A stored source still describes the file when its size did not change. zh-CN: 文件大小未变时, 记录的源信息仍然有效. */
    private fun current(info: MediaInfo): Boolean {
        val file = File(info.path)
        return !file.exists() || info.sizeBytes < 0 || file.length() == info.sizeBytes
    }

    const val EDITOR_FORMAT = 1L

}

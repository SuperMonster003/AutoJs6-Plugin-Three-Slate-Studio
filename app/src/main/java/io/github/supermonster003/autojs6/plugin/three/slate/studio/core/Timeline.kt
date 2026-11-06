package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits

/**
 * The transitions of roadmap D30. [overlaps]: the clip starts [Transition.durationMillis] before the end of the previous
 * one and both play together (two sequences); `cut` and `fade` (through black, half the duration on each side) keep
 * the clips apart. [shader]: the transition needs the GLSL effect of the plugin ([gpuFallback] replaces it when the GPU
 * cannot run it); `dissolve`, `slide` and `zoom` only use the alpha, position and scale of the media3 compositor.
 * zh-CN: 路线图 D30 的转场. [overlaps]: 本段在上一段结束前 [Transition.durationMillis] 开始, 两段同时播放 (双序列); `cut` 与
 * `fade` (经黑场, 两侧各占一半时长) 不重叠. [shader]: 需要插件的 GLSL 效果 (GPU 无法运行时以 [gpuFallback] 代替);
 * `dissolve`, `slide` 与 `zoom` 只用 media3 合成器的透明度, 位置与缩放.
 */
internal enum class TransitionType(val id: String, val overlaps: Boolean, val directional: Boolean, val shader: Boolean) {
    CUT("cut", overlaps = false, directional = false, shader = false),
    FADE("fade", overlaps = false, directional = false, shader = true),
    DISSOLVE("dissolve", overlaps = true, directional = false, shader = false),
    SLIDE("slide", overlaps = true, directional = true, shader = false),
    WIPE("wipe", overlaps = true, directional = true, shader = true),
    ZOOM("zoom", overlaps = true, directional = false, shader = false),
    BLUR("blur", overlaps = true, directional = false, shader = true),
    ;

    /**
     * What replaces this transition when the GPU cannot run the plugin shaders (roadmap P3.2): overlapping ones become
     * `dissolve`, `fade` becomes `cut`.
     * zh-CN: GPU 无法运行插件着色器时的替代 (路线图 P3.2): 重叠的转场改为 `dissolve`, `fade` 改为 `cut`.
     */
    val gpuFallback: TransitionType get() = if (!shader) this else if (overlaps) DISSOLVE else CUT

    companion object {
        val IDS: List<String> = entries.map { it.id }

        fun of(id: String): TransitionType = entries.first { it.id == id }
    }
}

/**
 * Where a directional transition moves: `left` brings the next clip in from the right edge, moving left.
 * zh-CN: 有方向的转场的运动方向: `left` 让下一段从右边缘进入并向左移动.
 */
internal enum class TransitionDirection(val id: String) {
    LEFT("left"),
    RIGHT("right"),
    UP("up"),
    DOWN("down"),
    ;

    companion object {
        val IDS: List<String> = entries.map { it.id }

        fun of(id: String): TransitionDirection = entries.first { it.id == id }
    }
}

/** The transition into a clip from the previous one (appendix D Q29 (a)). zh-CN: 从上一段进入本段的转场 (附录 D Q29 (a)). */
internal data class Transition(val type: TransitionType, val durationMillis: Long, val direction: TransitionDirection = TransitionDirection.LEFT)

/**
 * The frame changes of one clip: crop (source display pixels), clockwise rotation, flip, then the fit into the output
 * frame and a final [scale] around the center.
 * zh-CN: 一段的画面变换: 裁切 (源显示像素), 顺时针旋转, 翻转, 然后按 [fit] 放进输出帧, 最后以中心为基准缩放 [scale].
 */
internal data class ClipTransform(
    val scale: Double = 1.0,
    val rotate: Int = 0,
    val crop: CropRect? = null,
    val flip: Flip = Flip.NONE,
    val fit: Fit = Fit.CONTAIN,
)

/**
 * A clip as written; [startMillis] / [endMillis] are source time (end exclusive, null: the end of the source), [atMillis]
 * timeline time (null: right after the previous clip, see [TimelineLayout]). [path] is the field path for messages.
 * zh-CN: 按原样记录的片段; [startMillis] / [endMillis] 为源时间 (end 不含, null 表示源结尾), [atMillis] 为时间线时间 (null 表示紧接上一段,
 * 见 [TimelineLayout]). [path] 为消息中的字段路径.
 */
internal data class VideoClip(
    val path: String,
    val src: String,
    val startMillis: Long,
    val endMillis: Long?,
    val atMillis: Long?,
    val speed: Double,
    val volume: Double,
    val transform: ClipTransform,
    val transition: Transition?,
)

internal data class AudioClip(
    val path: String,
    val src: String,
    val startMillis: Long,
    val endMillis: Long?,
    val atMillis: Long?,
    val volume: Double,
    val fadeInMillis: Long,
    val fadeOutMillis: Long,
)

/**
 * The checked `compose` timeline of roadmap D33 (appendix B.4). [videoTracks] go from the bottom up (track order is the
 * z order), [overlays] are drawn above every track; a null [width] / [height] / [frameRate] takes the default of
 * [TimelineLayout].
 * zh-CN: 路线图 D33 (附录 B.4) 经校验的 `compose` 时间线. [videoTracks] 自下而上 (轨道顺序即 z 顺序), [overlays] 画在所有轨道之上;
 * [width] / [height] / [frameRate] 为 null 时用 [TimelineLayout] 的默认值.
 */
internal data class TimelineSpec(
    val width: Int?,
    val height: Int?,
    val frameRate: Int,
    val background: Int,
    val videoTracks: List<List<VideoClip>>,
    val audioTracks: List<List<AudioClip>>,
    val overlays: List<OverlayItem>,
    /** Drawn above the overlays (roadmap P3.3). zh-CN: 画在叠加物之上 (路线图 P3.3). */
    val subtitles: SubtitlesSpec? = null,
) {
    /** The media files the clips read, in first use order. zh-CN: 片段读取的媒体文件, 按首次使用的顺序. */
    val sources: List<String> get() = (videoTracks.flatten().map { it.src } + audioTracks.flatten().map { it.src }).distinct()
}

/**
 * Parses the `timeline` object of `compose` (roadmap P3.1, D33). Every message names the field path from `timeline.`
 * (for example `timeline.video[0].clips[1].transition.duration`); unknown keys are rejected like options. Rules that
 * need the media (source durations, overlaps, transition lengths) are checked by [TimelineLayout] once the sources are
 * probed. The host copies these rules (appendix D Q24).
 * zh-CN: 解析 `compose` 的 `timeline` 对象 (路线图 P3.1, D33). 每条消息都给出自 `timeline.` 起的字段路径 (例如
 * `timeline.video[0].clips[1].transition.duration`); 未知键与选项一样被拒绝. 需要媒体的规则 (源时长, 重叠, 转场长度) 在探测
 * 之后由 [TimelineLayout] 检查. 宿主照抄这些规则 (附录 D Q24).
 */
internal object TimelineOptions {

    const val PREFIX = "timeline."
    const val DEFAULT_FRAME_RATE = 30
    const val MAX_FRAME_RATE = 120

    /**
     * Track limits (appendix D Q34 (c), the maintainer's choice of 2026-11-06): 8 video tracks and 16 audio tracks. A
     * video track decodes two clips at once in an overlapping transition, so up to 8 x 2 + 16 = 32 sources may decode
     * together, twice the 16 inputs of `audioMix` (Q18 (a)); low-end devices may then fail at random (codec resources),
     * see the G8441 result in roadmap appendix F.
     * zh-CN: 轨道上限 (附录 D Q34 (c), 维护者 2026-11-06 选): 视频轨 8 条, 音频轨 16 条. 视频轨在重叠转场中同时解码两段, 因此至多
     * 8 x 2 + 16 = 32 个来源同时解码, 是 `audioMix` 16 路 (Q18 (a)) 的两倍; 低端设备可能因编解码器资源随机失败, 见路线图附录 F 的
     * G8441 结果.
     */
    const val MAX_VIDEO_TRACKS = 8
    const val MAX_AUDIO_TRACKS = 16

    fun parse(timeline: Map<String, Any?>?): TimelineSpec {
        if (timeline == null) throw MediaStudioException.invalid("timeline is required")
        val reader = OptionReader(timeline, PREFIX)
        val width = reader.int("width", OperationOptions.MIN_DIMENSION, OperationOptions.MAX_DIMENSION)?.let { it and 1.inv() }
        val height = reader.int("height", OperationOptions.MIN_DIMENSION, OperationOptions.MAX_DIMENSION)?.let { it and 1.inv() }
        val frameRate = reader.int("frameRate", 1, MAX_FRAME_RATE) ?: DEFAULT_FRAME_RATE
        val background = with(OverlayOptions) { reader.color("background") } ?: Colors.BLACK
        val subtitles = reader.child("subtitles")?.let(SubtitleOptions::timeline)
        val videoTracks = reader.objects("video")?.let { tracks ->
            if (tracks.size > MAX_VIDEO_TRACKS) throw limit("${reader.name("video")} holds ${tracks.size} tracks, at most $MAX_VIDEO_TRACKS")
            tracks.map { track -> track.clips { clip -> videoClip(clip) }.also { track.finish() } }
        }.orEmpty()
        val audioTracks = reader.objects("audio")?.let { tracks ->
            if (tracks.size > MAX_AUDIO_TRACKS) throw limit("${reader.name("audio")} holds ${tracks.size} tracks, at most $MAX_AUDIO_TRACKS")
            tracks.map { track -> track.clips { clip -> audioClip(clip) }.also { track.finish() } }
        }.orEmpty()
        // An empty array is no overlays (the Studio editor may save one). zh-CN: 空数组表示没有叠加 (Studio 编辑器可能保存空数组).
        val overlays = if ((timeline["overlays"] as? List<*>)?.isEmpty() != false) {
            reader.objects("overlays")
            emptyList()
        } else {
            OverlayOptions.parse(reader, "overlays")
        }
        reader.finish()
        if (videoTracks.all { it.isEmpty() }) throw MediaStudioException.invalid("${reader.name("video")} must hold at least one clip")
        val clips = videoTracks.sumOf { it.size } + audioTracks.sumOf { it.size }
        if (clips > MediaStudioLimits.MAX_INPUTS) throw limit("the timeline holds $clips clips, at most ${MediaStudioLimits.MAX_INPUTS}")
        return TimelineSpec(width, height, frameRate, background, videoTracks, audioTracks, overlays, subtitles)
    }

    private fun <T> OptionReader.clips(parse: (OptionReader) -> T): List<T> {
        val clips = objects("clips") ?: throw MediaStudioException.invalid("${name("clips")} is required")
        return clips.map { clip -> parse(clip).also { clip.finish() } }
    }

    private fun videoClip(reader: OptionReader): VideoClip {
        val src = source(reader)
        val (start, end) = range(reader)
        val transform = reader.child("transform")?.let { transform(it).also { _ -> it.finish() } } ?: ClipTransform()
        val transition = reader.child("transition")?.let { transition(it).also { _ -> it.finish() } }
        return VideoClip(
            path = reader.name("").removeSuffix("."),
            src = src,
            startMillis = start,
            endMillis = end,
            atMillis = reader.long("at", 0, OperationOptions.MAX_TIME_MILLIS),
            speed = reader.double("speed", MediaStudioLimits.MIN_SPEED_FACTOR, MediaStudioLimits.MAX_SPEED_FACTOR) ?: 1.0,
            volume = reader.double("volume", 0.0, OperationOptions.MAX_GAIN) ?: 1.0,
            transform = transform,
            transition = transition,
        )
    }

    private fun audioClip(reader: OptionReader): AudioClip {
        val src = source(reader)
        val (start, end) = range(reader)
        return AudioClip(
            path = reader.name("").removeSuffix("."),
            src = src,
            startMillis = start,
            endMillis = end,
            atMillis = reader.long("at", 0, OperationOptions.MAX_TIME_MILLIS),
            volume = reader.double("volume", 0.0, OperationOptions.MAX_GAIN) ?: 1.0,
            fadeInMillis = reader.long("fadeIn", 0, OperationOptions.MAX_TIME_MILLIS) ?: 0L,
            fadeOutMillis = reader.long("fadeOut", 0, OperationOptions.MAX_TIME_MILLIS) ?: 0L,
        )
    }

    private fun source(reader: OptionReader): String {
        val src = reader.string("src") ?: throw MediaStudioException.invalid("${reader.name("src")} is required")
        if (!src.startsWith("/")) throw MediaStudioException.invalid("${reader.name("src")} must be an absolute path, got $src")
        return src
    }

    private fun range(reader: OptionReader): Pair<Long, Long?> {
        val start = reader.long("start", 0, OperationOptions.MAX_TIME_MILLIS) ?: 0L
        val end = reader.long("end", 1, OperationOptions.MAX_TIME_MILLIS)
        if (end != null && end <= start) {
            throw MediaStudioException.invalid("${reader.name("end")} ($end) must be greater than ${reader.name("start")} ($start)")
        }
        return start to end
    }

    private fun transform(reader: OptionReader): ClipTransform {
        val scale = reader.double("scale", 0.0, OperationOptions.MAX_SCALE, minExclusive = true) ?: 1.0
        val rotate = reader.long("rotate", -OperationOptions.MAX_TIME_MILLIS, OperationOptions.MAX_TIME_MILLIS)?.let { degrees ->
            if (degrees % 90L != 0L) throw MediaStudioException.invalid("${reader.name("rotate")} must be a multiple of 90, got $degrees")
            (((degrees % 360L) + 360L) % 360L).toInt()
        } ?: 0
        val crop = reader.child("crop")?.let { crop ->
            CropRect(
                x = crop.int("x", 0, Int.MAX_VALUE) ?: 0,
                y = crop.int("y", 0, Int.MAX_VALUE) ?: 0,
                width = crop.int("width", OperationOptions.MIN_DIMENSION, Int.MAX_VALUE)
                    ?: throw MediaStudioException.invalid("${crop.name("width")} is required"),
                height = crop.int("height", OperationOptions.MIN_DIMENSION, Int.MAX_VALUE)
                    ?: throw MediaStudioException.invalid("${crop.name("height")} is required"),
            ).also { crop.finish() }
        }
        val flip = reader.choice("flip", Flip.entries.map { it.id })?.let(Flip::of) ?: Flip.NONE
        val fit = reader.choice("fit", Fit.entries.map { it.id })?.let(Fit::of) ?: Fit.CONTAIN
        return ClipTransform(scale, rotate, crop, flip, fit)
    }

    private fun transition(reader: OptionReader): Transition {
        val type = reader.choice("type", TransitionType.IDS)?.let(TransitionType::of)
            ?: throw MediaStudioException.invalid("${reader.name("type")} is required")
        val duration = reader.long("duration", MediaStudioLimits.MIN_TRANSITION_MILLIS, MediaStudioLimits.MAX_TRANSITION_MILLIS)
            ?: MediaStudioLimits.DEFAULT_TRANSITION_MILLIS
        val direction = reader.choice("direction", TransitionDirection.IDS)?.let(TransitionDirection::of)
        if (direction != null && !type.directional) {
            throw MediaStudioException.invalid("${reader.name("direction")} applies to slide and wipe only")
        }
        return Transition(type, if (type == TransitionType.CUT) 0L else duration, direction ?: TransitionDirection.LEFT)
    }

    private fun limit(detail: String) = MediaStudioException(MediaStudioErrorCodes.LIMIT_EXCEEDED, detail)

}

/**
 * A clip placed on the timeline, all times in microseconds: it shows from [atUs] for [lengthUs] (source span / speed).
 * [into] / [out] are the transitions at its start and end (the [out] of a clip is the [into] of the next one).
 * zh-CN: 已放到时间线上的片段, 时间均以微秒计: 自 [atUs] 起显示 [lengthUs] (源区间 / 速度). [into] / [out] 为开头与结尾的转场
 * (上一段的 [out] 即下一段的 [into]).
 */
internal data class PlacedVideo(
    val clip: VideoClip,
    val info: MediaInfo,
    val track: Int,
    val index: Int,
    val sourceStartUs: Long,
    val sourceEndUs: Long,
    val atUs: Long,
    val lengthUs: Long,
    val into: Transition?,
    val out: Transition?,
) {
    val endUs: Long get() = atUs + lengthUs
}

internal data class PlacedAudio(
    val clip: AudioClip,
    val info: MediaInfo,
    val sourceStartUs: Long,
    val sourceEndUs: Long,
    val atUs: Long,
    val lengthUs: Long,
) {
    val endUs: Long get() = atUs + lengthUs
}

/**
 * The timeline with every clip placed (roadmap P3.1), after probing the sources. Rules: in a track, a clip without
 * `at` follows the previous one, `at` never goes before the end of the previous clip, and an overlapping transition
 * (D30) starts the clip exactly its duration before that end (an explicit `at` must match it, within 1 ms). A
 * transition fits in both clips, and the two transitions of one clip do not overlap each other. The output is
 * [width] x [height] (default: the first video clip after its crop and rotation, both even; with only one of them the
 * other follows the aspect ratio of that clip) and lasts until the last clip ends.
 * zh-CN: 探测源之后各片段都已定位的时间线 (路线图 P3.1). 规则: 同一轨道中没有 `at` 的片段紧接上一段, `at` 不得早于上一段的结尾,
 * 重叠转场 (D30) 让本段恰好在该结尾之前一个转场时长开始 (显式 `at` 必须与之相符, 允许 1 ms 误差). 转场时长不超过两段各自的长度,
 * 一段两端的转场互不重叠. 输出为 [width] x [height] (默认取第一段视频裁切与旋转后的尺寸并取偶数; 只给一项时另一项按该段宽高比),
 * 时长到最后一段结束.
 */
internal data class TimelineLayout(
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val background: Int,
    val durationUs: Long,
    val videoTracks: List<List<PlacedVideo>>,
    val audio: List<PlacedAudio>,
    val overlays: List<OverlayItem>,
    val subtitles: SubtitlesSpec? = null,
) {

    val durationMillis: Long get() = (durationUs + 999) / 1_000

    /** Replaces the transitions [replace] maps (GPU fallback of roadmap P3.2). zh-CN: 替换 [replace] 映射的转场 (路线图 P3.2 的 GPU 回退). */
    fun withTransitions(replace: (TransitionType) -> TransitionType): TimelineLayout = copy(
        videoTracks = videoTracks.map { track ->
            track.map { placed ->
                placed.copy(
                    into = placed.into?.let { it.copy(type = replace(it.type)) },
                    out = placed.out?.let { it.copy(type = replace(it.type)) },
                )
            }
        },
    )

    companion object {

        /** Tolerance of an explicit `at` against the computed start. zh-CN: 显式 `at` 与计算起点的容差. */
        const val AT_TOLERANCE_US = 1_000L

        fun resolve(spec: TimelineSpec, probe: (String) -> MediaInfo): TimelineLayout {
            val videoTracks = spec.videoTracks.mapIndexed { track, clips -> placeVideo(track, clips, probe) }
            val audio = spec.audioTracks.flatMap { clips -> placeAudio(clips, probe) }
            val first = videoTracks.flatten().first()
            val (width, height) = frameSize(spec, first)
            val duration = maxOf(videoTracks.flatten().maxOf { it.endUs }, audio.maxOfOrNull { it.endUs } ?: 0L)
            return TimelineLayout(width, height, spec.frameRate, spec.background, duration, videoTracks, audio, spec.overlays, spec.subtitles)
        }

        private fun placeVideo(track: Int, clips: List<VideoClip>, probe: (String) -> MediaInfo): List<PlacedVideo> {
            val placed = ArrayList<PlacedVideo>(clips.size)
            clips.forEachIndexed { index, clip ->
                val info = probe(clip.src)
                if (!info.hasVideo) throw MediaStudioException.unsupported("${clip.path}.src ${clip.src} has no video track")
                val (sourceStart, sourceEnd) = sourceRange(clip.path, clip.src, clip.startMillis, clip.endMillis, info)
                val length = Math.round((sourceEnd - sourceStart) / clip.speed)
                val previous = placed.lastOrNull()
                val transition = clip.transition?.takeIf { it.type != TransitionType.CUT }
                if (clip.transition != null && previous == null) {
                    throw MediaStudioException.invalid("${clip.path}.transition needs a previous clip in the track")
                }
                val at = when {
                    previous == null -> (clip.atMillis ?: 0L) * 1_000L
                    transition != null && transition.type.overlaps -> {
                        val durationUs = transition.durationMillis * 1_000L
                        if (durationUs > previous.lengthUs) {
                            throw MediaStudioException.invalid(
                                "${clip.path}.transition.duration (${transition.durationMillis} ms) is longer than the previous clip (${millis(previous.lengthUs)} ms)",
                            )
                        }
                        val expected = previous.endUs - durationUs
                        clip.atMillis?.let { at ->
                            if (Math.abs(at * 1_000L - expected) > AT_TOLERANCE_US) {
                                throw MediaStudioException.invalid(
                                    "${clip.path}.at ($at) must be ${millis(expected)}: the ${transition.type.id} of ${transition.durationMillis} ms " +
                                        "overlaps the end of the previous clip (${millis(previous.endUs)} ms); leave out at to place it there",
                                )
                            }
                        }
                        expected
                    }
                    else -> {
                        val at = clip.atMillis?.let { it * 1_000L } ?: previous.endUs
                        if (at < previous.endUs - AT_TOLERANCE_US) {
                            throw MediaStudioException.invalid(
                                "${clip.path}.at (${clip.atMillis}) is before the end of the previous clip (${millis(previous.endUs)} ms); " +
                                    "clips of one track must not overlap except in a transition",
                            )
                        }
                        maxOf(at, previous.endUs)
                    }
                }
                transition?.let { checkTransitionLength(clip, it, previous!!, length) }
                if (previous != null) placed[placed.lastIndex] = previous.copy(out = transition)
                placed += PlacedVideo(clip, info, track, index, sourceStart, sourceEnd, at, length, transition, null)
            }
            return placed
        }

        /**
         * A transition fits in both clips; the incoming one of the previous clip and this outgoing one must not
         * overlap: `fade` takes half its duration on each side.
         * zh-CN: 转场需容纳于两段之内; 上一段的进入转场与本转场不得重叠: `fade` 在两侧各占一半时长.
         */
        private fun checkTransitionLength(clip: VideoClip, transition: Transition, previous: PlacedVideo, length: Long) {
            val side = sideUs(transition)
            if (side > length) {
                throw MediaStudioException.invalid(
                    "${clip.path}.transition.duration (${transition.durationMillis} ms) is longer than the clip (${millis(length)} ms)",
                )
            }
            if (side > previous.lengthUs) {
                throw MediaStudioException.invalid(
                    "${clip.path}.transition.duration (${transition.durationMillis} ms) is longer than the previous clip (${millis(previous.lengthUs)} ms)",
                )
            }
            val before = previous.into?.let(::sideUs) ?: 0L
            if (before + side > previous.lengthUs) {
                throw MediaStudioException.invalid(
                    "${clip.path}.transition (${transition.durationMillis} ms) overlaps the transition into the previous clip " +
                        "(${previous.into!!.durationMillis} ms); the previous clip lasts only ${millis(previous.lengthUs)} ms",
                )
            }
        }

        /** The part of a transition inside each of the two clips. zh-CN: 转场在两段各自之内所占的部分. */
        fun sideUs(transition: Transition): Long =
            if (transition.type == TransitionType.FADE) transition.durationMillis * 500L else transition.durationMillis * 1_000L

        private fun placeAudio(clips: List<AudioClip>, probe: (String) -> MediaInfo): List<PlacedAudio> {
            val placed = ArrayList<PlacedAudio>(clips.size)
            clips.forEach { clip ->
                val info = probe(clip.src)
                if (!info.hasAudio) throw MediaStudioException.unsupported("${clip.path}.src ${clip.src} has no audio track")
                val (sourceStart, sourceEnd) = sourceRange(clip.path, clip.src, clip.startMillis, clip.endMillis, info)
                val length = sourceEnd - sourceStart
                val previous = placed.lastOrNull()
                val at = clip.atMillis?.let { it * 1_000L } ?: previous?.endUs ?: 0L
                if (previous != null && at < previous.endUs - AT_TOLERANCE_US) {
                    throw MediaStudioException.invalid(
                        "${clip.path}.at (${clip.atMillis}) is before the end of the previous clip (${millis(previous.endUs)} ms); clips of one track must not overlap",
                    )
                }
                if ((clip.fadeInMillis + clip.fadeOutMillis) * 1_000L > length) {
                    throw MediaStudioException.invalid(
                        "${clip.path}.fadeIn + fadeOut (${clip.fadeInMillis + clip.fadeOutMillis} ms) is longer than the clip (${millis(length)} ms)",
                    )
                }
                placed += PlacedAudio(clip, info, sourceStart, sourceEnd, maxOf(at, previous?.endUs ?: 0L), length)
            }
            return placed
        }

        private fun sourceRange(path: String, src: String, startMillis: Long, endMillis: Long?, info: MediaInfo): Pair<Long, Long> {
            val durationUs = info.durationMillis * 1_000L
            val start = startMillis * 1_000L
            if (start >= durationUs) {
                throw MediaStudioException.invalid("$path.start ($startMillis ms) is not before the end of $src (${info.durationMillis} ms)")
            }
            val end = endMillis?.let { minOf(it * 1_000L, durationUs) } ?: durationUs
            return start to end
        }

        private fun frameSize(spec: TimelineSpec, first: PlacedVideo): Pair<Int, Int> {
            var size = FrameSize(first.info.width ?: 0, first.info.height ?: 0)
            first.clip.transform.crop?.let { size = FrameSize(it.width, it.height) }
            size = FrameGeometry.rotate(size, first.clip.transform.rotate)
            val target = when {
                spec.width != null && spec.height != null -> FrameSize(spec.width, spec.height)
                spec.width != null || spec.height != null -> FrameGeometry.target(spec.width, spec.height, size)
                else -> FrameGeometry.even(size)
            }
            FrameGeometry.sizeProblem(target)?.let { throw MediaStudioException.invalid("timeline: $it") }
            return target.width to target.height
        }

        private fun millis(us: Long): String = if (us % 1_000L == 0L) (us / 1_000L).toString() else String.format(java.util.Locale.ROOT, "%.1f", us / 1_000.0)

    }

}

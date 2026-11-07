package io.github.supermonster003.autojs6.plugin.three.slate.studio.project

import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionType
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import kotlin.math.roundToLong

/**
 * The state of the timeline editor (roadmap P4.3, D10, D33): immutable, so every edit is a new value that the undo
 * history ([EditHistory]) keeps. Tracks hold their clips in order; a clip sits [VideoClipModel.gapMs] after the end of
 * the previous one, or, with an overlapping transition (D30), the transition duration before it, which is how the
 * timeline JSON places clips without `at` (appendix B.4), so clips of one track never overlap except in a transition.
 * Times are milliseconds; [placeVideo] / [placeAudio] give the microsecond positions with the same rounding as
 * `TimelineLayout`. The model turns into the `compose` timeline object with [TimelineJson] and into a `.slate.json`
 * project with [ProjectFiles].
 * zh-CN: 时间线编辑器的状态 (路线图 P4.3, D10, D33): 不可变, 每次编辑产生新值, 由撤销历史 ([EditHistory]) 保存. 轨道按顺序保存片段;
 * 片段位于上一段结尾之后 [VideoClipModel.gapMs] 处, 有重叠转场 (D30) 时则在结尾之前一个转场时长处, 这与时间线 JSON 省略 `at`
 * 时的定位方式相同 (附录 B.4), 因此同一轨道的片段除转场外不会重叠. 时间以毫秒计; [placeVideo] / [placeAudio] 按与
 * `TimelineLayout` 相同的取整给出微秒位置. 模型经 [TimelineJson] 转为 `compose` 的时间线对象, 经 [ProjectFiles] 转为 `.slate.json` 工程.
 */
internal data class EditorProject(
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Int = DEFAULT_FRAME_RATE,
    val background: String = DEFAULT_BACKGROUND,
    val videoTracks: List<VideoTrackModel> = listOf(VideoTrackModel(1)),
    val audioTracks: List<AudioTrackModel> = listOf(AudioTrackModel(2)),
    val overlays: List<OverlayModel> = emptyList(),
    val subtitles: SubtitleLayer = SubtitleLayer(),
    val export: ExportSettings = ExportSettings(),
    val nextId: Long = 3,
) {

    /** Where every video clip shows, by clip id. zh-CN: 每个视频片段的显示位置, 以片段 id 为键. */
    val videoPlacements: Map<Long, Placement> by lazy { videoTracks.flatMap { placeVideo(it.clips).entries }.associate { it.key to it.value } }

    val audioPlacements: Map<Long, Placement> by lazy { audioTracks.flatMap { placeAudio(it.clips).entries }.associate { it.key to it.value } }

    /** The timeline length in microseconds: the end of the last clip, overlay or cue. zh-CN: 时间线长度 (微秒): 最后一个片段, 叠加物或字幕的结尾. */
    val durationUs: Long by lazy {
        maxOf(
            videoPlacements.values.maxOfOrNull { it.endUs } ?: 0L,
            audioPlacements.values.maxOfOrNull { it.endUs } ?: 0L,
        )
    }

    val durationMs: Long get() = (durationUs + 999) / 1_000

    val isEmpty: Boolean get() = videoTracks.all { it.clips.isEmpty() }

    /**
     * The video and audio files of the clips, which are probed and described in a project file; overlay images are
     * not media and are read only when drawn.
     * zh-CN: 片段的视频与音频文件, 会被探测并记录在工程文件中; 叠加图片不是媒体, 只在绘制时读取.
     */
    val sources: List<String>
        get() = (videoTracks.flatMap { t -> t.clips.map { it.src } } + audioTracks.flatMap { t -> t.clips.map { it.src } }).distinct()

    fun id(): Pair<Long, EditorProject> = nextId to copy(nextId = nextId + 1)

    fun videoClip(id: Long): Pair<Int, VideoClipModel>? {
        videoTracks.forEachIndexed { track, t -> t.clips.firstOrNull { it.id == id }?.let { return track to it } }
        return null
    }

    fun audioClip(id: Long): Pair<Int, AudioClipModel>? {
        audioTracks.forEachIndexed { track, t -> t.clips.firstOrNull { it.id == id }?.let { return track to it } }
        return null
    }

    companion object {
        const val DEFAULT_FRAME_RATE = 30
        const val DEFAULT_BACKGROUND = "#000000"

        /** Tracks of the editor: the limits of `compose` (appendix D Q34 (c)). zh-CN: 编辑器的轨道数: `compose` 的上限 (附录 D Q34 (c)). */
        const val MAX_VIDEO_TRACKS = 8
        const val MAX_AUDIO_TRACKS = 16

        /**
         * Places the clips of one video track like `TimelineLayout`: a clip follows the previous one after its gap,
         * an overlapping transition starts it the transition duration before that end.
         * zh-CN: 像 `TimelineLayout` 一样定位一条视频轨的片段: 片段在上一段结尾的间隔之后开始, 重叠转场让它在该结尾之前一个转场时长开始.
         */
        fun placeVideo(clips: List<VideoClipModel>): Map<Long, Placement> {
            val placed = LinkedHashMap<Long, Placement>()
            var previous: Placement? = null
            clips.forEach { clip ->
                val length = clip.lengthUs
                val overlap = clip.transition?.takeIf { previous != null && it.overlaps }?.durationMs
                val at = when {
                    previous == null -> clip.gapMs * 1_000L
                    overlap != null -> previous.endUs - overlap * 1_000L
                    else -> previous.endUs + clip.gapMs * 1_000L
                }
                val placement = Placement(at, length)
                placed[clip.id] = placement
                previous = placement
            }
            return placed
        }

        fun placeAudio(clips: List<AudioClipModel>): Map<Long, Placement> {
            val placed = LinkedHashMap<Long, Placement>()
            var end = 0L
            clips.forEachIndexed { index, clip ->
                val at = (if (index == 0) 0L else end) + clip.gapMs * 1_000L
                val placement = Placement(at, (clip.endMs - clip.startMs) * 1_000L)
                placed[clip.id] = placement
                end = placement.endUs
            }
            return placed
        }
    }

}

/** A position on the timeline in microseconds. zh-CN: 时间线上的位置 (微秒). */
internal data class Placement(val atUs: Long, val lengthUs: Long) {
    val endUs: Long get() = atUs + lengthUs
    val atMs: Double get() = atUs / 1_000.0
    val endMs: Double get() = endUs / 1_000.0
}

internal data class VideoTrackModel(val id: Long, val clips: List<VideoClipModel> = emptyList())

internal data class AudioTrackModel(val id: Long, val clips: List<AudioClipModel> = emptyList())

/**
 * A video clip: source span [startMs] - [endMs] of a [sourceMs] long file, [gapMs] after the previous clip (from 0 for
 * the first one), and the clip settings of appendix B.4.
 * zh-CN: 视频片段: [sourceMs] 长的文件中的源区间 [startMs] - [endMs], 位于上一段之后 [gapMs] 处 (首段自 0 起), 以及附录 B.4 的片段设置.
 */
internal data class VideoClipModel(
    val id: Long,
    val src: String,
    val sourceMs: Long,
    val startMs: Long,
    val endMs: Long,
    val gapMs: Long = 0L,
    val speed: Double = 1.0,
    val volume: Double = 1.0,
    val scale: Double = 1.0,
    val rotate: Int = 0,
    val flip: String = FLIP_NONE,
    val fit: String = FIT_CONTAIN,
    val transition: TransitionModel? = null,
    /** Display size of the source (after its rotation), for the preview and thumbnails. zh-CN: 源的显示尺寸 (已含旋转). */
    val width: Int = 0,
    val height: Int = 0,
    val hasAudio: Boolean = true,
    /** `transform.crop` of a timeline written elsewhere, kept as it is (the editor does not crop). zh-CN: 外部写入的 `transform.crop`, 原样保留 (编辑器不裁切). */
    val crop: Map<String, Any?>? = null,
) {
    /** Same rounding as `TimelineLayout`. zh-CN: 与 `TimelineLayout` 相同的取整. */
    val lengthUs: Long get() = ((endMs - startMs) * 1_000L / speed).roundToLong()

    companion object {
        const val FLIP_NONE = "none"
        const val FIT_CONTAIN = "contain"
        val FLIPS = listOf("none", "horizontal", "vertical", "both")
        val FITS = listOf("contain", "cover", "stretch")
    }
}

internal data class AudioClipModel(
    val id: Long,
    val src: String,
    val sourceMs: Long,
    val startMs: Long,
    val endMs: Long,
    val gapMs: Long = 0L,
    val volume: Double = 1.0,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
)

/** The transition into a clip (appendix D Q29 (a)); `cut` is no transition. zh-CN: 进入片段的转场 (附录 D Q29 (a)); `cut` 即无转场. */
internal data class TransitionModel(
    val type: String,
    val durationMs: Long = MediaStudioLimits.DEFAULT_TRANSITION_MILLIS,
    val direction: String? = null,
) {
    val kind: TransitionType get() = TransitionType.of(type)
    val overlaps: Boolean get() = kind.overlaps

    /** The part of the transition inside each of the two clips, as `TimelineLayout.sideUs`. zh-CN: 转场在两段各自之内的部分. */
    val sideUs: Long get() = if (kind == TransitionType.FADE) durationMs * 500L else durationMs * 1_000L
}

/**
 * A text or image overlay of the timeline (`video.overlay` keys of appendix B.4): [x] / [y] place its center in percent
 * of the frame.
 * zh-CN: 时间线的文字或图片叠加物 (附录 B.4 的 `video.overlay` 键): [x] / [y] 以画面百分比定位其中心.
 */
internal data class OverlayModel(
    val id: Long,
    val type: String,
    val fromMs: Long,
    val toMs: Long,
    val x: Double = 50.0,
    val y: Double = 50.0,
    val text: String? = null,
    val fontSize: Double = DEFAULT_FONT_SIZE,
    val color: String = "#FFFFFF",
    val stroke: String? = "#000000",
    val src: String? = null,
    val scale: Double = 1.0,
    val alpha: Double = 1.0,
    /** Keys the editor does not edit (font, anchor, shadow, ...), kept as they are. zh-CN: 编辑器不编辑的键 (字体, 锚点, 阴影等), 原样保留. */
    val extra: Map<String, Any?> = emptyMap(),
) {
    companion object {
        const val TEXT = "text"
        const val IMAGE = "image"
        const val DEFAULT_FONT_SIZE = 48.0
    }
}

internal data class CueModel(val id: Long, val startMs: Long, val endMs: Long, val text: String)

/**
 * The subtitle layer: cues and one style (the `subtitles` of appendix B.4 with `cues`). Imported ASS styles become
 * this one style.
 * zh-CN: 字幕层: cue 与一套样式 (附录 B.4 中带 `cues` 的 `subtitles`). 导入的 ASS 样式统一为这一套样式.
 */
internal data class SubtitleLayer(
    val cues: List<CueModel> = emptyList(),
    val fontSize: Double? = null,
    val color: String = "#FFFFFF",
    val stroke: String? = "#000000",
    val background: String? = null,
    val align: String = ALIGN_BOTTOM,
    val extra: Map<String, Any?> = emptyMap(),
) {
    companion object {
        const val ALIGN_BOTTOM = "bottom"
        val ALIGNS = listOf("bottom", "top", "center")
    }
}

/** What `compose` gets besides the timeline. zh-CN: 时间线之外交给 `compose` 的内容. */
internal data class ExportSettings(
    val codec: String = "h264",
    val bitRate: Long? = null,
    val output: String? = null,
)

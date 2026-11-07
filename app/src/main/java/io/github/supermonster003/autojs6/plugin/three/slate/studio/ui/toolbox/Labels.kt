package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationCategory
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import org.autojs.plugin.mediastudio.api.MediaStudioOps

/**
 * Localized names of the wizard operations, option keys and choice values. Keys come from [OptionSchemas]; a key
 * without a label falls back to the key itself, which `LabelsTest` forbids.
 * zh-CN: 向导操作, 选项键与选项值的本地化名称. 键来自 [OptionSchemas]; 没有名称的键回退为键本身, `LabelsTest` 禁止这种情况.
 */
internal object Labels {

    data class Operation(@param:StringRes val title: Int, @param:StringRes val summary: Int, @param:DrawableRes val icon: Int)

    val operations: Map<String, Operation> = mapOf(
        MediaStudioOps.TRIM to Operation(R.string.op_trim_title, R.string.op_trim_summary, R.drawable.ic_content_cut_24),
        MediaStudioOps.CONCAT to Operation(R.string.op_concat_title, R.string.op_concat_summary, R.drawable.ic_movie_24),
        MediaStudioOps.SPEED to Operation(R.string.op_speed_title, R.string.op_speed_summary, R.drawable.ic_play_arrow_24),
        MediaStudioOps.TRANSFORM to Operation(R.string.op_transform_title, R.string.op_transform_summary, R.drawable.ic_tune_24),
        MediaStudioOps.TRANSCODE to Operation(R.string.op_transcode_title, R.string.op_transcode_summary, R.drawable.ic_refresh_24),
        MediaStudioOps.REMOVE_AUDIO to Operation(R.string.op_remove_audio_title, R.string.op_remove_audio_summary, R.drawable.ic_movie_24),
        MediaStudioOps.EXTRACT_AUDIO to Operation(R.string.op_extract_audio_title, R.string.op_extract_audio_summary, R.drawable.ic_music_note_24),
        MediaStudioOps.OVERLAY to Operation(R.string.op_overlay_title, R.string.op_overlay_summary, R.drawable.ic_edit_24),
        MediaStudioOps.FRAMES to Operation(R.string.op_frames_title, R.string.op_frames_summary, R.drawable.ic_image_24),
        MediaStudioOps.FRAME to Operation(R.string.op_frame_title, R.string.op_frame_summary, R.drawable.ic_image_24),
        MediaStudioOps.THUMBNAIL to Operation(R.string.op_thumbnail_title, R.string.op_thumbnail_summary, R.drawable.ic_image_24),
        MediaStudioOps.TO_GIF to Operation(R.string.op_to_gif_title, R.string.op_to_gif_summary, R.drawable.ic_gif_24),
        MediaStudioOps.AUDIO_TRIM to Operation(R.string.op_audio_trim_title, R.string.op_audio_trim_summary, R.drawable.ic_content_cut_24),
        MediaStudioOps.AUDIO_CONCAT to Operation(R.string.op_audio_concat_title, R.string.op_audio_concat_summary, R.drawable.ic_music_note_24),
        MediaStudioOps.AUDIO_MIX to Operation(R.string.op_audio_mix_title, R.string.op_audio_mix_summary, R.drawable.ic_tune_24),
        MediaStudioOps.AUDIO_VOLUME to Operation(R.string.op_audio_volume_title, R.string.op_audio_volume_summary, R.drawable.ic_tune_24),
        MediaStudioOps.AUDIO_CONVERT to Operation(R.string.op_audio_convert_title, R.string.op_audio_convert_summary, R.drawable.ic_refresh_24),
        MediaStudioOps.AUDIO_SPEED to Operation(R.string.op_audio_speed_title, R.string.op_audio_speed_summary, R.drawable.ic_play_arrow_24),
        MediaStudioOps.BURN_SUBTITLES to Operation(R.string.op_burn_subtitles_title, R.string.op_burn_subtitles_summary, R.drawable.ic_subtitles_24),
        OperationSchema.SUBTITLES_CONVERT to Operation(R.string.op_subtitles_convert_title, R.string.op_subtitles_convert_summary, R.drawable.ic_subtitles_24),
    )

    @StringRes
    fun category(category: OperationCategory): Int = when (category) {
        OperationCategory.VIDEO -> R.string.category_video
        OperationCategory.IMAGE -> R.string.category_image
        OperationCategory.AUDIO -> R.string.category_audio
        OperationCategory.SUBTITLE -> R.string.category_subtitle
    }

    /** Field labels by key; [contextual] holds labels whose meaning depends on the parent. zh-CN: 按键的字段名称. */
    val fields: Map<String, Int> = mapOf(
        "start" to R.string.option_start,
        "end" to R.string.option_end,
        "duration" to R.string.option_duration,
        "width" to R.string.option_width,
        "height" to R.string.option_height,
        "fit" to R.string.option_fit,
        "codec" to R.string.option_codec,
        "bitRate" to R.string.option_bit_rate,
        "factor" to R.string.option_factor,
        "keepPitch" to R.string.option_keep_pitch,
        "scale" to R.string.option_scale,
        "rotate" to R.string.option_rotate,
        "crop" to R.string.option_crop,
        "x" to R.string.option_x,
        "y" to R.string.option_y,
        "flip" to R.string.option_flip,
        "frameRate" to R.string.option_frame_rate,
        "audio" to R.string.option_audio,
        "format" to R.string.option_format,
        "sampleRate" to R.string.option_sample_rate,
        "channels" to R.string.option_channels,
        "at" to R.string.option_at,
        "times" to R.string.option_times,
        "every" to R.string.option_every,
        "count" to R.string.option_count,
        "quality" to R.string.option_quality,
        "fps" to R.string.option_fps,
        "loop" to R.string.option_loop,
        "dither" to R.string.option_dither,
        "tracks" to R.string.option_tracks,
        "volume" to R.string.option_volume,
        "gain" to R.string.option_gain,
        "normalize" to R.string.option_normalize,
        "fadeIn" to R.string.option_fade_in,
        "fadeOut" to R.string.option_fade_out,
        "overlays" to R.string.option_overlays,
        "text" to R.string.option_text,
        "font" to R.string.option_font,
        "fontSize" to R.string.option_font_size,
        "bold" to R.string.option_bold,
        "italic" to R.string.option_italic,
        "color" to R.string.option_color,
        "stroke" to R.string.option_stroke,
        "strokeWidth" to R.string.option_stroke_width,
        "shadow" to R.string.option_shadow,
        "shadowRadius" to R.string.option_shadow_radius,
        "shadowDx" to R.string.option_shadow_dx,
        "shadowDy" to R.string.option_shadow_dy,
        "align" to R.string.option_align,
        "src" to R.string.option_src,
        "anchor" to R.string.option_anchor,
        "from" to R.string.option_from,
        "to" to R.string.option_to,
        "alpha" to R.string.option_alpha,
        "subtitles" to R.string.option_subtitles,
        "style" to R.string.option_style,
        "background" to R.string.option_background,
        "marginV" to R.string.option_margin_v,
        "marginH" to R.string.option_margin_h,
    )

    /** Labels for a key under a parent (`tracks.at`, `style.align`). zh-CN: 父级之下另有含义的键. */
    val contextual: Map<String, Int> = mapOf(
        "tracks.at" to R.string.option_track_at,
        "style.align" to R.string.option_subtitle_align,
    )

    @StringRes
    fun field(parent: String?, key: String): Int = contextual["$parent.$key"] ?: fields[key] ?: 0

    /** Explanations of flag-or-number fields. zh-CN: 布尔或数值字段的说明. */
    val hints: Map<String, Int> = mapOf(
        "loop" to R.string.option_loop_hint,
        "normalize" to R.string.option_normalize_hint,
    )

    /** Alternatives of exclusive groups. zh-CN: 互斥组的各个选择. */
    val alternatives: Map<String, Int> = mapOf(
        "length.end" to R.string.alt_length_end,
        "length.duration" to R.string.alt_length_duration,
        "size.size" to R.string.alt_size_size,
        "size.scale" to R.string.alt_size_scale,
        "sampling.at" to R.string.alt_sampling_at,
        "sampling.times" to R.string.alt_sampling_times,
        "sampling.range" to R.string.alt_sampling_range,
        "level.gain" to R.string.alt_level_gain,
        "level.normalize" to R.string.alt_level_normalize,
    )

    /** Choice values that need words; the others (codecs, formats, numbers) show as they are. zh-CN: 需要文字的选项值. */
    val choices: Map<String, Int> = mapOf(
        "fit.contain" to R.string.choice_fit_contain,
        "fit.cover" to R.string.choice_fit_cover,
        "fit.stretch" to R.string.choice_fit_stretch,
        "flip.none" to R.string.choice_flip_none,
        "flip.horizontal" to R.string.choice_flip_horizontal,
        "flip.vertical" to R.string.choice_flip_vertical,
        "flip.both" to R.string.choice_flip_both,
        "align.left" to R.string.choice_align_left,
        "align.center" to R.string.choice_align_center,
        "align.right" to R.string.choice_align_right,
        "anchor.center" to R.string.choice_anchor_center,
        "anchor.top-left" to R.string.choice_anchor_top_left,
        "anchor.top" to R.string.choice_anchor_top,
        "anchor.top-right" to R.string.choice_anchor_top_right,
        "anchor.left" to R.string.choice_anchor_left,
        "anchor.right" to R.string.choice_anchor_right,
        "anchor.bottom-left" to R.string.choice_anchor_bottom_left,
        "anchor.bottom" to R.string.choice_anchor_bottom,
        "anchor.bottom-right" to R.string.choice_anchor_bottom_right,
        "type.text" to R.string.choice_overlay_text,
        "type.image" to R.string.choice_overlay_image,
        "channels.1" to R.string.choice_channels_mono,
        "channels.2" to R.string.choice_channels_stereo,
    )

    /** A choice label key: subtitle `align` takes the anchor values. zh-CN: 选项值名称的键: 字幕 `align` 取锚点值. */
    fun choiceKey(key: String, value: String): String = if (key == "align" && value !in setOf("left", "center", "right")) "anchor.$value" else "$key.$value"

    /** Display text of values shown as they are. zh-CN: 原样显示的选项值. */
    fun plainChoice(value: String): String = when (value) {
        "h264" -> "H.264"
        "h265" -> "H.265"
        else -> value.uppercase()
    }
}

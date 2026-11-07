@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.preview

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.graphics.createBitmap
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Colors
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Flip
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameGeometry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameSize
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.PlacedVideo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleWarnings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineLayout
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Transition
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionType
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.ComposeCompositorSettings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.ComposeOperation
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.OverlayRenderer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.SubtitlePainter
import io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline.TransitionEffect
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

/**
 * The media3 [Composition] of a timeline for the live preview of the timeline editor (roadmap P4.3, D34): the same
 * sequences, lanes, compositor settings, transitions, overlays and subtitles that `compose` exports
 * (`pipeline/ComposeOperation`, rebuilt here because that class belongs to the export), so the preview shows what the
 * export writes. [audio] is the mix of [PreviewAudio] (the same two-pass mix as the export), or null while it is not
 * ready or when nothing sounds. Close it after the player let it go: it holds the overlay bitmaps and the image files.
 * zh-CN: 时间线编辑器实时预览用的 media3 [Composition] (路线图 P4.3, D34): 与 `compose` 导出相同的序列, 车道, 合成器设置, 转场,
 * 叠加物与字幕 (`pipeline/ComposeOperation` 属于导出, 这里重新组装), 因此预览所见即导出所得. [audio] 为 [PreviewAudio] 的混音
 * (与导出相同的两遍混音), 尚未就绪或无声时为 null. 播放器释放它之后关闭: 它持有叠加位图与图片文件.
 */
internal class PreviewComposition private constructor(
    val composition: Composition,
    val layout: TimelineLayout,
    /** Warnings of the subtitles (ignored tags and the like). zh-CN: 字幕的警告 (被忽略的标签等). */
    val warnings: List<String>,
    private val resources: List<Closeable>,
) : Closeable {

    override fun close() {
        resources.forEach { runCatching { it.close() } }
    }

    companion object {

        /**
         * Builds the composition of [layout]; [directory] receives the clock and background images (reused by later
         * builds). Throws [MediaStudioException] like the export (a bad crop, font or picture).
         * zh-CN: 构建 [layout] 的合成; [directory] 存放时钟与背景图片 (后续构建复用). 与导出一样抛出 [MediaStudioException]
         * (裁切, 字体或图片有误).
         */
        fun build(layout: TimelineLayout, audio: File?, directory: File, clock: Boolean = true, bottomLaneFirst: Boolean = false): PreviewComposition {
            val resources = mutableListOf<Closeable>()
            val warnings = SubtitleWarnings()
            try {
                val painter = layout.subtitles?.let { subtitles ->
                    SubtitlePainter(SubtitleFiles.load(subtitles.source, warnings), subtitles.style, warnings)
                }
                val sequences = mutableListOf<EditedMediaItemSequence>()
                val lanes = mutableMapOf<Int, ComposeOperation.Lane>()
                if (clock) sequences += imageSequence(layout, image(directory, Color.TRANSPARENT))
                for (track in layout.videoTracks.indices.reversed()) {
                    val placed = layout.videoTracks[track]
                    val split = placed.zipWithNext().any { (previous, next) -> next.atUs < previous.endUs }
                    val groups = if (split) listOf(placed.filter { it.index % 2 == 1 } to true, placed.filter { it.index % 2 == 0 } to false) else listOf(placed to false)
                    (if (bottomLaneFirst) groups.reversed() else groups).forEach { (clips, top) ->
                        if (clips.isEmpty()) return@forEach
                        lanes[sequences.size] = ComposeOperation.Lane(clips, top)
                        sequences += laneSequence(layout, clips, top)
                    }
                }
                var background: Int? = null
                if (Color.alpha(layout.background) != 0 && layout.background != Colors.BLACK) {
                    background = sequences.size
                    sequences += imageSequence(layout, image(directory, layout.background))
                }
                audio?.let { file ->
                    val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(file))).setRemoveVideo(true)
                        .setDurationUs(layout.durationUs).build()
                    sequences += EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO)).addItem(item).build()
                }
                val effects = if (layout.overlays.isEmpty() && painter == null) Effects.EMPTY else {
                    val renderer = OverlayRenderer(layout.overlays, FrameSize(layout.width, layout.height), painter).also { resources += it }
                    Effects(emptyList(), listOf<Effect>(OverlayEffect(listOf(renderer.overlay()))))
                }
                val composition = Composition.Builder(sequences)
                    .setVideoCompositorSettings(ComposeCompositorSettings(layout.width, layout.height, lanes, background))
                    .setEffects(effects)
                    .build()
                return PreviewComposition(composition, layout, warnings.list(), resources)
            } catch (e: Throwable) {
                resources.forEach { runCatching { it.close() } }
                throw e
            }
        }

        /** A small solid PNG of [color], written once per color. zh-CN: [color] 的纯色小 PNG, 每种颜色只写一次. */
        private fun image(directory: File, color: Int): File {
            val file = File(directory, "preview-%08x.png".format(color))
            if (file.isFile) return file
            check(directory.isDirectory || directory.mkdirs()) { "cannot create $directory" }
            val temporary = File(directory, ".${file.name}.tmp")
            val bitmap = createBitmap(ComposeOperation.IMAGE_SIDE, ComposeOperation.IMAGE_SIDE).apply { eraseColor(color) }
            try {
                FileOutputStream(temporary).use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            } finally {
                bitmap.recycle()
            }
            if (!temporary.renameTo(file)) temporary.delete()
            return file
        }

        private fun imageSequence(layout: TimelineLayout, file: File): EditedMediaItemSequence {
            val durationMs = layout.durationMillis
            val item = MediaItem.Builder().setUri(Uri.fromFile(file)).setMimeType(MimeTypes.IMAGE_PNG).setImageDurationMs(durationMs).build()
            val edited = EditedMediaItem.Builder(item)
                .setDurationUs(durationMs * 1_000L)
                .setFrameRate(layout.frameRate)
                .setEffects(Effects(emptyList(), listOf<Effect>(Presentation.createForWidthAndHeight(layout.width, layout.height, Presentation.LAYOUT_STRETCH_TO_FIT))))
                .build()
            return EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO)).addItem(edited).build()
        }

        private fun laneSequence(layout: TimelineLayout, clips: List<PlacedVideo>, top: Boolean): EditedMediaItemSequence {
            val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
            var cursor = 0L
            clips.forEach { placed ->
                if (placed.atUs > cursor) sequence.addGap(placed.atUs - cursor)
                sequence.addItem(clipItem(layout, placed, top))
                cursor = maxOf(cursor, placed.endUs)
            }
            if (cursor < layout.durationUs) sequence.addGap(layout.durationUs - cursor)
            return sequence.build()
        }

        private fun clipItem(layout: TimelineLayout, placed: PlacedVideo, top: Boolean): EditedMediaItem {
            val transform = placed.clip.transform
            var size = FrameSize(placed.info.width ?: 0, placed.info.height ?: 0)
            val effects = mutableListOf<Effect>()
            transform.crop?.let { crop ->
                FrameGeometry.cropProblem(crop, size)?.let { throw MediaStudioException.invalid("${placed.clip.path}.transform: $it") }
                val ndc = FrameGeometry.cropNdc(crop, size)
                effects += Crop(ndc[0], ndc[1], ndc[2], ndc[3])
                size = FrameSize(crop.width, crop.height)
            }
            if (transform.rotate != 0) {
                effects += ScaleAndRotateTransformation.Builder().setRotationDegrees(-transform.rotate.toFloat()).build()
            }
            if (transform.flip != Flip.NONE) {
                effects += ScaleAndRotateTransformation.Builder()
                    .setScale(if (transform.flip.horizontal) -1f else 1f, if (transform.flip.vertical) -1f else 1f)
                    .build()
            }
            effects += Presentation.createForWidthAndHeight(layout.width, layout.height, ComposeOperation.layoutOf(transform.fit))
            if (transform.scale != 1.0) {
                effects += ScaleAndRotateTransformation.Builder().setScale(transform.scale.toFloat(), transform.scale.toFloat()).build()
            }
            val into = placed.into?.let { part(it, top) }
            val out = placed.out?.let { part(it, top) }
            if (into != null || out != null) {
                effects += TransitionEffect(placed.lengthUs, into, out)
                if (into?.type == TransitionType.BLUR || out?.type == TransitionType.BLUR) {
                    effects += TransitionEffect(placed.lengthUs, into, out, verticalBlur = true)
                }
            }
            val clipping = MediaItem.ClippingConfiguration.Builder().setStartPositionUs(placed.sourceStartUs)
            if (placed.sourceEndUs < placed.info.durationMillis * 1_000L) clipping.setEndPositionUs(placed.sourceEndUs)
            val item = MediaItem.Builder().setUri(Uri.fromFile(File(placed.clip.src))).setClippingConfiguration(clipping.build()).build()
            // CompositionPlayer needs the source duration up front (the export finds it while reading).
            // zh-CN: CompositionPlayer 需要预先给出源时长 (导出在读取时才得知).
            val edited = EditedMediaItem.Builder(item).setRemoveAudio(true).setEffects(Effects(emptyList(), effects))
                .setDurationUs(placed.info.durationMillis * 1_000L)
            if (placed.clip.speed != 1.0) edited.setSpeed(ConstantSpeed(placed.clip.speed.toFloat()))
            return edited.build()
        }

        private fun part(transition: Transition, top: Boolean): TransitionEffect.Part? = transition.takeIf { it.type.shader }?.let {
            TransitionEffect.Part(it.type, TimelineLayout.sideUs(it), it.direction, masked = top && it.type == TransitionType.WIPE)
        }

    }

    private class ConstantSpeed(private val factor: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long): Float = factor
        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
    }

}

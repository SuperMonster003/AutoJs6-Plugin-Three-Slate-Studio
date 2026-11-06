package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameSampling
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.FrameTimes
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.GifEncoder
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.ImageEncoding
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.ImageFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputPaths
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

/**
 * `frames`, `frame`, `thumbnail` and `toGif` of roadmap P2.4 (D32). Frames come from MediaMetadataRetriever at the
 * closest frame (`OPTION_CLOSEST`), upright (the displayed orientation) and scaled to the requested width with the
 * aspect ratio kept; PNG / JPG are written by Bitmap.compress, GIF by [GifEncoder]. Each decode seeks again from the
 * previous key frame, so the cost grows with the frame count and the key frame distance of the input.
 * zh-CN: 路线图 P2.4 (D32) 的 `frames`, `frame`, `thumbnail` 与 `toGif`. 帧由 MediaMetadataRetriever 取最接近的帧
 * (`OPTION_CLOSEST`), 转为显示方向, 按请求宽度保持宽高比缩放; PNG / JPG 由 Bitmap.compress 写出, GIF 由 [GifEncoder] 写出.
 * 每次解码都从前一个关键帧重新定位, 所以耗时随帧数与输入的关键帧间距增长.
 */
internal class StillOperations(private val task: TaskRunner.JobContext) {

    /** One written image or GIF. zh-CN: 写出的一张图片或一个 GIF. */
    class Still(val width: Int, val height: Int, val mime: String, val durationMillis: Long?)

    /** The images of `frames`, in frame order. zh-CN: `frames` 写出的图片, 按帧顺序. */
    class Frames(val files: List<File>, val width: Int, val height: Int, val mime: String)

    /** `frame` and `thumbnail`: one image at [output]. zh-CN: `frame` 与 `thumbnail`: 在 [output] 写一张图片. */
    fun image(spec: OperationSpec, input: MediaInfo, output: String, from: Double, to: Double): Still {
        val (at, image, width) = when (spec) {
            is OperationSpec.Frame -> Triple(spec.atMillis, spec.image, spec.image.width ?: displayWidth(input, "frame"))
            is OperationSpec.Thumbnail -> Triple(
                spec.atMillis ?: (input.durationMillis / 3.0),
                spec.image,
                spec.image.width ?: minOf(OperationOptions.DEFAULT_THUMBNAIL_WIDTH, displayWidth(input, "thumbnail")),
            )
            else -> throw IllegalArgumentException("not an image operation: $spec")
        }
        FrameSampling.frameTimes(FrameTimes.Points(listOf(at), "at"), input.durationMillis)
        task.progress(from, MediaStudioContract.STAGE_DECODING)
        val bitmap = FrameGrabber(input).use { it.grab(FrameSampling.decodeMicros(at, input.durationMillis), width) }
        try {
            task.throwIfCancelled()
            task.progress(from + (to - from) / 2, MediaStudioContract.STAGE_ENCODING)
            write(bitmap, image, File(output))
            return Still(bitmap.width, bitmap.height, image.format.mime, null)
        } finally {
            bitmap.recycle()
        }
    }

    /** `toGif`: the GIF at [output]. zh-CN: `toGif`: 在 [output] 写出 GIF. */
    fun gif(spec: OperationSpec.ToGif, input: MediaInfo, output: String, from: Double, to: Double): Still {
        val sourceWidth = displayWidth(input, "toGif")
        val times = FrameSampling.gifTimes(spec, input.durationMillis)
        val width = spec.width ?: minOf(MediaStudioLimits.DEFAULT_GIF_WIDTH, sourceWidth)
        val delays = GifEncoder.delays(times.size, spec.fps)
        return FrameGrabber(input).use { grabber ->
            val height = grabber.heightFor(width)
            val pixels = IntArray(width * height)
            BufferedOutputStream(FileOutputStream(output)).use { out ->
                val encoder = GifEncoder(out, width, height, spec.loop, spec.dither)
                times.forEachIndexed { index, time ->
                    task.throwIfCancelled()
                    val bitmap = grabber.grab(FrameSampling.decodeMicros(time, input.durationMillis), width)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    bitmap.recycle()
                    encoder.addFrame(pixels, delays[index])
                    task.progress(from + (to - from) * (index + 1) / times.size, MediaStudioContract.STAGE_ENCODING)
                }
                encoder.finish()
                Still(width, height, GIF_MIME, encoder.durationCentis * 10)
            }
        }
    }

    /**
     * `frames`: images named by [OutputPaths.frameFileName] in [directory], each written to a hidden part file first;
     * [written] learns every finished file so a failed run can remove them. An existing frame file is IO_ERROR.
     * zh-CN: `frames`: 在 [directory] 里按 [OutputPaths.frameFileName] 命名的图片, 每张先写到隐藏的临时文件; 每写完一张都告知
     * [written], 以便失败时删除. 帧文件已存在时为 IO_ERROR.
     */
    fun frames(
        spec: OperationSpec.Frames,
        input: MediaInfo,
        directory: File,
        written: (File) -> Unit,
        from: Double,
        to: Double,
    ): Frames {
        val sourceWidth = displayWidth(input, "frames")
        val times = FrameSampling.frameTimes(spec.times, input.durationMillis)
        val extension = spec.image.format.extension
        val files = times.indices.map { File(directory, OutputPaths.frameFileName(input.path, it, times.size, extension)) }
        files.firstOrNull { it.exists() }?.let { throw MediaStudioException.io("${it.path} already exists") }
        val width = spec.image.width ?: sourceWidth
        return FrameGrabber(input).use { grabber ->
            files.forEachIndexed { index, file ->
                task.throwIfCancelled()
                val bitmap = grabber.grab(FrameSampling.decodeMicros(times[index], input.durationMillis), width)
                val part = File(directory, ".${file.nameWithoutExtension}.part-${task.taskId}.$extension")
                try {
                    write(bitmap, spec.image, part)
                } finally {
                    bitmap.recycle()
                }
                if (file.exists()) {
                    part.delete()
                    throw MediaStudioException.io("${file.path} appeared while the task ran")
                }
                if (!part.renameTo(file)) {
                    part.delete()
                    throw MediaStudioException.io("cannot move the frame to ${file.path}")
                }
                written(file)
                task.progress(from + (to - from) * (index + 1) / files.size, MediaStudioContract.STAGE_ENCODING)
            }
            Frames(files, width, grabber.heightFor(width), spec.image.format.mime)
        }
    }

    private fun write(bitmap: Bitmap, image: ImageEncoding, file: File) {
        val format = if (image.format == ImageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        try {
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(format, image.quality, out)) throw MediaStudioException.io("cannot encode the image ${file.path}")
            }
        } catch (e: IOException) {
            file.delete()
            throw MediaStudioException.io("cannot write ${file.path} (${e.message})", e)
        }
    }

    private fun displayWidth(input: MediaInfo, op: String): Int {
        if (!input.hasVideo || (input.width ?: 0) <= 0 || (input.height ?: 0) <= 0) {
            throw MediaStudioException.unsupported("$op needs a video track; ${input.path} has none")
        }
        return input.width!!
    }

    private companion object {
        const val GIF_MIME = "image/gif"
    }

}

/**
 * Upright frames of one input from MediaMetadataRetriever, scaled to a width with the displayed aspect ratio.
 * zh-CN: 由 MediaMetadataRetriever 取一个输入的显示方向帧, 按显示宽高比缩放到指定宽度.
 */
internal class FrameGrabber(private val info: MediaInfo) : AutoCloseable {

    private val displayWidth = info.width ?: 0
    private val displayHeight = info.height ?: 0

    private val retriever = MediaMetadataRetriever().also { retriever ->
        try {
            retriever.setDataSource(info.path)
        } catch (e: RuntimeException) {
            retriever.release()
            throw MediaStudioException.unsupported("cannot read frames of ${info.path} (${e.message})")
        }
    }

    /** The output height for [width], at least 1. zh-CN: [width] 对应的输出高度, 至少为 1. */
    fun heightFor(width: Int): Int = (width.toDouble() * displayHeight / displayWidth).roundToInt().coerceAtLeast(1)

    /** An ARGB_8888 frame of [width] x [heightFor] at [micros]. zh-CN: [micros] 处 [width] x [heightFor] 的 ARGB_8888 帧. */
    fun grab(micros: Long, width: Int): Bitmap {
        val frame = retriever.getFrameAtTime(micros, MediaMetadataRetriever.OPTION_CLOSEST)
            ?: retriever.getFrameAtTime(micros, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC)
            ?: throw MediaStudioException(MediaStudioErrorCodes.CODEC_ERROR, "cannot decode the frame at ${micros / 1000} ms of ${info.path}")
        return argb(scale(upright(frame), width, heightFor(width)))
    }

    /**
     * MediaMetadataRetriever already applies the rotation on the platform versions seen so far; this turns a frame that
     * still has the coded orientation.
     * zh-CN: 目前所见的平台版本上, MediaMetadataRetriever 已应用旋转; 这里处理仍为编码方向的帧.
     */
    private fun upright(frame: Bitmap): Bitmap {
        val quarter = info.rotation == 90 || info.rotation == 270
        if (!quarter || displayWidth == displayHeight || (frame.width > frame.height) == (displayWidth > displayHeight)) return frame
        val turned = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, Matrix().apply { postRotate(info.rotation.toFloat()) }, true)
        if (turned !== frame) frame.recycle()
        return turned
    }

    /** Halves first so a large reduction stays smooth, then scales to the exact size. zh-CN: 先逐次减半使大幅缩小保持平滑, 再缩放到精确尺寸. */
    private fun scale(frame: Bitmap, width: Int, height: Int): Bitmap {
        var current = frame
        while (current.width / 2 >= width && current.height / 2 >= height) {
            val half = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (half !== current) current.recycle()
            current = half
        }
        if (current.width == width && current.height == height) return current
        val exact = Bitmap.createScaledBitmap(current, width, height, true)
        if (exact !== current) current.recycle()
        return exact
    }

    private fun argb(frame: Bitmap): Bitmap {
        if (frame.config == Bitmap.Config.ARGB_8888) return frame
        val copy = frame.copy(Bitmap.Config.ARGB_8888, false) ?: throw MediaStudioException.io("out of memory for a ${frame.width}x${frame.height} frame")
        frame.recycle()
        return copy
    }

    override fun close() = retriever.release()

}

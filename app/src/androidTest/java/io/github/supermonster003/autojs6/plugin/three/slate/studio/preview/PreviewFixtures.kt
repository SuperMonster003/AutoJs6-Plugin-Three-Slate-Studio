package io.github.supermonster003.autojs6.plugin.three.slate.studio.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.Image
import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures.MediaFixtures
import java.io.File
import java.io.FileOutputStream
import java.nio.Buffer

/**
 * Sources of the timeline preview spike (roadmap P4.3, appendix D Q3) and of the timeline editor tests, generated on the
 * device: [WIDTH] x [HEIGHT] H.264 at [FRAME_RATE] fps whose vertical bars scroll every frame (so every frame decodes
 * and moves), muxed with the 440 Hz tone; a long tone as the music; a PNG logo for the image overlay.
 * zh-CN: 时间线预览 spike (路线图 P4.3, 附录 D Q3) 与时间线编辑器测试用的素材, 在设备上生成: [WIDTH] x [HEIGHT] 的 H.264, [FRAME_RATE]
 * fps, 竖条每帧滚动 (每帧都要解码且有运动), 与 440 Hz 音调封装在一起; 一段长音调作为音乐; 一张 PNG 标志用于图片叠加.
 */
object PreviewFixtures {

    const val WIDTH = 1920
    const val HEIGHT = 1080
    const val FRAME_RATE = 30
    const val BIT_RATE = 8_000_000

    /** Three palettes, one per source clip. zh-CN: 三套配色, 每段源素材一套. */
    val PALETTES: List<IntArray> = listOf(
        intArrayOf(0xFF0000, 0xFFFFFF, 0x0000FF, 0x000000),
        intArrayOf(0x00FF00, 0xFFFF00, 0x00FFFF, 0x202020),
        intArrayOf(0xFF00FF, 0xFFFFFF, 0xFF8000, 0x404040),
    )

    /** A scrolling bar clip with the tone, [durationUs] long. zh-CN: 带音调的滚动竖条片段, 时长 [durationUs]. */
    fun clip(directory: File, index: Int, durationUs: Long, width: Int = WIDTH, height: Int = HEIGHT): File {
        val output = File(directory, "preview-clip-$index-${width}x$height-${durationUs / 1000}ms.mp4")
        if (output.isFile && output.length() > 0) return output
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
        val video = File(directory, "preview-video-$index.mp4")
        val audio = File(directory, "preview-tone-$index.m4a")
        scrollingBars(video, PALETTES[index % PALETTES.size], durationUs, width, height)
        MediaFixtures.sineAac(audio, durationUs)
        MediaFixtures.remux(listOf(video, audio), output)
        video.delete()
        audio.delete()
        return output
    }

    fun music(directory: File, durationUs: Long): File {
        val output = File(directory, "preview-music-${durationUs / 1000}ms.m4a")
        if (output.isFile && output.length() > 0) return output
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
        return MediaFixtures.sineAac(output, durationUs)
    }

    fun logo(directory: File): File {
        val output = File(directory, "preview-logo.png")
        if (output.isFile && output.length() > 0) return output
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.TRANSPARENT)
            drawCircle(128f, 128f, 120f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() })
            drawCircle(128f, 128f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2060FF.toInt() })
        }
        FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return output
    }

    private fun scrollingBars(output: File, palette: IntArray, durationUs: Long, width: Int, height: Int) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val frames = (durationUs * FRAME_RATE / 1_000_000L).toInt()
        val planes = palette.map(MediaFixtures::yuv)
        var frame = 0
        MediaFixtures.encode(format, output, durationUs) { codec, index ->
            if (frame == frames) return@encode false
            val image = requireNotNull(codec.getInputImage(index)) { "The encoder offers no input image" }
            paint(image, planes, frame * SCROLL_PIXELS)
            codec.queueInputBuffer(index, 0, width * height * 3 / 2, frame * 1_000_000L / FRAME_RATE, 0)
            frame++
            true
        }
    }

    /** Bars of [colors] shifted by [offset] pixels. zh-CN: 平移 [offset] 像素的 [colors] 竖条. */
    private fun paint(image: Image, colors: List<IntArray>, offset: Int) {
        image.planes.forEachIndexed { plane, target ->
            val subsampling = if (plane == 0) 1 else 2
            val width = image.width / subsampling
            val height = image.height / subsampling
            val bar = (image.width / colors.size / 2).coerceAtLeast(1)
            val row = ByteArray(width) { x ->
                val source = (x * subsampling + offset) % image.width
                colors[(source / bar) % colors.size][plane].toByte()
            }
            val buffer = target.buffer
            for (y in 0 until height) {
                val start = y * target.rowStride
                if (target.pixelStride == 1) {
                    (buffer as Buffer).position(start)
                    buffer.put(row)
                } else {
                    for (x in 0 until width) buffer.put(start + x * target.pixelStride, row[x])
                }
            }
        }
    }

    private const val SCROLL_PIXELS = 12

}

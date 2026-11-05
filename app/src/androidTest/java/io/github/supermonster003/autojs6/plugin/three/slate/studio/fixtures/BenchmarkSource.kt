package io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures

import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.io.File
import java.nio.Buffer
import kotlin.random.Random

/**
 * The 1080p benchmark source of roadmap P2.2 appendix F, generated on the device: 60 seconds at 30 fps of a detailed
 * texture (random 8 x 8 blocks over a gradient) whose visible window jumps to a random place every frame, H.264 at
 * 16 Mbps, muxed with a 440 Hz AAC tone. Every frame is a scene cut that motion search cannot predict, so the encoders
 * under test run at their target bit rate: the worst case, and the output bit rate shows the rate control. A smoothly
 * panning texture was tried first (2026-11-06) and compressed to 1.5 - 3 Mbps, far below every target.
 * zh-CN: 路线图 P2.2 附录 F 的 1080p 基准源, 在设备上生成: 60 秒 30 fps 的高细节纹理 (渐变上叠加随机 8 x 8 块), 可见窗口每帧跳到
 * 随机位置, H.264 16 Mbps, 与 440 Hz AAC 音调封装在一起. 每帧都是运动搜索预测不了的场景切换, 被测编码器按目标码率工作: 这是最坏
 * 情况, 输出码率反映码率控制. 先试过平滑平移的纹理 (2026-11-06), 被压到 1.5 - 3 Mbps, 远低于各目标码率.
 */
object BenchmarkSource {

    const val WIDTH = 1920
    const val HEIGHT = 1080
    const val FRAME_RATE = 30
    const val DURATION_US = 60_000_000L
    const val BIT_RATE = 16_000_000

    private const val BLOCK = 8

    fun generate(directory: File): File {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
        val video = video(File(directory, "bench-video.mp4"))
        val audio = MediaFixtures.sineAac(File(directory, "bench-tone.m4a"), DURATION_US)
        return MediaFixtures.remux(listOf(video, audio), File(directory, "bench-1080p-60s.mp4")).also {
            video.delete()
            audio.delete()
        }
    }

    private fun video(output: File): File {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val frames = (DURATION_US * FRAME_RATE / 1_000_000L).toInt()
        // Twice the frame size, so every window of one frame size lies inside. zh-CN: 两倍帧尺寸, 每个帧大小的窗口都落在其中.
        val luma = texture(WIDTH * 2, HEIGHT * 2, seed = 1)
        val chroma = texture(WIDTH * 2, HEIGHT, seed = 2)
        val jumps = Random(3)
        var frame = 0
        MediaFixtures.encode(format, output, DURATION_US) { codec, index ->
            if (frame == frames) return@encode false
            val image = requireNotNull(codec.getInputImage(index)) { "The encoder offers no input image" }
            val left = jumps.nextInt(WIDTH / 2) * 2
            val top = jumps.nextInt(HEIGHT / 2) * 2
            image.planes.forEachIndexed { plane, target ->
                val buffer = target.buffer
                if (plane == 0) {
                    for (y in 0 until HEIGHT) {
                        (buffer as Buffer).position(y * target.rowStride)
                        buffer.put(luma, (top + y) * WIDTH * 2 + left, WIDTH)
                    }
                } else if (target.pixelStride == 1) {
                    for (y in 0 until HEIGHT / 2) {
                        (buffer as Buffer).position(y * target.rowStride)
                        buffer.put(chroma, (top / 2 + y) * WIDTH * 2 + left / 2 + (plane - 1) * WIDTH / 2, WIDTH / 2)
                    }
                } else if (plane == 1) {
                    // Interleaved chroma: one write per row fills both planes; the colors carry no meaning here.
                    // zh-CN: 交错色度: 每行写一次即填满两个平面; 这里颜色没有意义.
                    for (y in 0 until HEIGHT / 2) {
                        val start = y * target.rowStride
                        (buffer as Buffer).position(start)
                        buffer.put(chroma, (top / 2 + y) * WIDTH * 2 + left, minOf(WIDTH, buffer.limit() - start))
                    }
                }
            }
            codec.queueInputBuffer(index, 0, WIDTH * HEIGHT * 3 / 2, frame * 1_000_000L / FRAME_RATE, 0)
            frame++
            true
        }
        return output
    }

    /** Random blocks over a diagonal gradient. zh-CN: 对角渐变上的随机块. */
    private fun texture(width: Int, height: Int, seed: Int): ByteArray {
        val random = Random(seed)
        val blocks = IntArray((width / BLOCK + 1) * (height / BLOCK + 1)) { random.nextInt(-48, 49) }
        val columns = width / BLOCK + 1
        return ByteArray(width * height) { index ->
            val x = index % width
            val y = index / width
            val gradient = 40 + (x + y) * 160 / (width + height)
            (gradient + blocks[y / BLOCK * columns + x / BLOCK]).coerceIn(16, 235).toByte()
        }
    }

}

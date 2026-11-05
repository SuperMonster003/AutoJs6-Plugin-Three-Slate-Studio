package io.github.supermonster003.autojs6.plugin.three.slate.studio.fixtures

import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Media fixtures generated on the device at test time (roadmap P0.3), so the repository carries no binary media:
 * 3 seconds of eight vertical color bars as H.264 in MP4, and 3 seconds of a 440 Hz sine as AAC in M4A and as 16 bit
 * PCM in WAV. `.python/generate_media_fixtures.py` runs [MediaFixturesTest] and pulls the files for manual checks.
 * zh-CN: 在测试运行时于设备上生成的媒体夹具 (路线图 P0.3), 仓库因此不含二进制媒体: 3 秒八条竖向彩条的 H.264 MP4, 以及 3 秒
 * 440 Hz 正弦波的 AAC M4A 与 16 位 PCM WAV. `.python/generate_media_fixtures.py` 运行 [MediaFixturesTest] 并拉取文件供人工检查.
 */
object MediaFixtures {

    /** Fixture directory under the cache of the plugin. zh-CN: 插件缓存目录下的夹具目录. */
    const val DIRECTORY = "media-fixtures"

    const val COLOR_BARS_MP4 = "color-bars-3s.mp4"
    const val SINE_M4A = "sine-440hz-3s.m4a"
    const val SINE_WAV = "sine-440hz-3s.wav"

    const val DURATION_US = 3_000_000L
    const val WIDTH = 320
    const val HEIGHT = 240
    const val FRAME_RATE = 30
    const val SAMPLE_RATE = 44_100
    const val TONE_HZ = 440.0

    /** White, yellow, cyan, green, magenta, red, blue, black. zh-CN: 白, 黄, 青, 绿, 品红, 红, 蓝, 黑. */
    val BARS: IntArray = intArrayOf(0xFFFFFF, 0xFFFF00, 0x00FFFF, 0x00FF00, 0xFF00FF, 0xFF0000, 0x0000FF, 0x000000)

    val TOTAL_SAMPLES: Int = (SAMPLE_RATE * DURATION_US / 1_000_000L).toInt()

    private const val TIMEOUT_US = 10_000L
    private const val CHUNK_SAMPLES = 1024

    data class Fixtures(val colorBarsMp4: File, val sineM4a: File, val sineWav: File)

    fun generate(directory: File): Fixtures {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
        return Fixtures(
            colorBarsMp4 = colorBars(File(directory, COLOR_BARS_MP4)),
            sineM4a = sineAac(File(directory, SINE_M4A)),
            sineWav = sineWav(File(directory, SINE_WAV)),
        )
    }

    fun colorBars(output: File): File {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 1_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val frames = (DURATION_US * FRAME_RATE / 1_000_000L).toInt()
        val planes = BARS.map(::yuv)
        var frame = 0
        encode(format, output, DURATION_US) { codec, index ->
            if (frame == frames) return@encode false
            val image = requireNotNull(codec.getInputImage(index)) { "The encoder offers no input image" }
            paint(image, planes)
            codec.queueInputBuffer(index, 0, WIDTH * HEIGHT * 3 / 2, frame * 1_000_000L / FRAME_RATE, 0)
            frame++
            true
        }
        return output
    }

    fun sineAac(output: File): File {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 96_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, CHUNK_SAMPLES * 2)
        }
        var cursor = 0
        encode(format, output, DURATION_US) { codec, index ->
            if (cursor == TOTAL_SAMPLES) return@encode false
            val buffer = requireNotNull(codec.getInputBuffer(index)).also { (it as Buffer).clear() }.order(ByteOrder.LITTLE_ENDIAN)
            val count = minOf(CHUNK_SAMPLES, TOTAL_SAMPLES - cursor, buffer.remaining() / 2)
            repeat(count) { buffer.putShort(sample(cursor + it)) }
            codec.queueInputBuffer(index, 0, count * 2, cursor * 1_000_000L / SAMPLE_RATE, 0)
            cursor += count
            true
        }
        return output
    }

    fun sineWav(output: File): File {
        val dataBytes = TOTAL_SAMPLES * 2
        val bytes = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + dataBytes).put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII)).putInt(16).putShort(1).putShort(1)
            putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
            put("data".toByteArray(Charsets.US_ASCII)).putInt(dataBytes)
            repeat(TOTAL_SAMPLES) { putShort(sample(it)) }
        }
        output.writeBytes(bytes.array())
        return output
    }

    /** Half scale, so the encoders never clip. zh-CN: 半幅, 编码器不会削波. */
    fun sample(index: Int): Short = (sin(2 * PI * TONE_HZ * index / SAMPLE_RATE) * 0.5 * Short.MAX_VALUE).roundToInt().toShort()

    /** BT.601 limited range Y, U, V of an RGB color. zh-CN: RGB 颜色的 BT.601 有限范围 Y, U, V. */
    fun yuv(rgb: Int): IntArray {
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        return intArrayOf(
            (16 + (65.738 * r + 129.057 * g + 25.064 * b) / 256).roundToInt(),
            (128 + (-37.945 * r - 74.494 * g + 112.439 * b) / 256).roundToInt(),
            (128 + (112.439 * r - 94.154 * g - 18.285 * b) / 256).roundToInt(),
        )
    }

    private fun paint(image: Image, colors: List<IntArray>) {
        image.planes.forEachIndexed { plane, target ->
            val subsampling = if (plane == 0) 1 else 2
            val width = image.width / subsampling
            val height = image.height / subsampling
            val row = ByteArray(width) { x -> colors[x * subsampling * colors.size / image.width][plane].toByte() }
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

    /**
     * Runs one encoder into one MP4 track: [queue] fills and queues an input buffer and answers false once the input
     * is exhausted, then the end of stream follows.
     * zh-CN: 把一个编码器写成 MP4 的一条轨道: [queue] 填充并提交一个输入缓冲区, 输入耗尽时返回 false, 随后提交流结束.
     */
    private fun encode(format: MediaFormat, output: File, endUs: Long, queue: (MediaCodec, Int) -> Boolean) {
        val name = requireNotNull(MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format)) {
            "No encoder for $format"
        }
        val codec = MediaCodec.createByCodecName(name)
        val muxer = MediaMuxer(output.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxing = false
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var track = -1
            var inputDone = false
            while (true) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0 && !queue(codec, index)) {
                        codec.queueInputBuffer(index, 0, 0, endUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    }
                }
                val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    check(!muxing) { "The output format changed twice" }
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxing = true
                } else if (index >= 0) {
                    val buffer = requireNotNull(codec.getOutputBuffer(index))
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0) {
                        check(muxing) { "Encoded data before the output format" }
                        // Buffer casts keep the Java 8 descriptors, which every supported API level has.
                        (buffer as Buffer).position(info.offset)
                        (buffer as Buffer).limit(info.offset + info.size)
                        muxer.writeSampleData(track, buffer, info)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            if (muxing) muxer.stop()
            muxer.release()
        }
    }

}

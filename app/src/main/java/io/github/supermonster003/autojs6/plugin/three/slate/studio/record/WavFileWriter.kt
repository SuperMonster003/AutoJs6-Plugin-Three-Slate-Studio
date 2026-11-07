// Copy of the AutoJs6 host kernel file app/src/main/java/org/autojs/autojs/core/record/WavFileWriter.kt at host commit
// 3ef5cec633 (roadmap P1.2; unchanged up to host 063b9f0b8a), kept by 3-Slate Studio roadmap D41 / P4.5; see README.md.
// Plugin change: none besides the package.
package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import java.io.File
import java.io.RandomAccessFile

/**
 * Streams interleaved PCM16 little-endian samples into a canonical 44-byte-header WAV file; [finish] writes the final
 * RIFF and data sizes. The data stays below [MAX_DATA_BYTES] (the 32-bit RIFF size field).
 *
 * zh-CN: 把交错的 PCM16 小端采样流式写入标准 44 字节文件头的 WAV 文件; [finish] 写入最终的 RIFF 与 data 大小.
 * 数据不超过 [MAX_DATA_BYTES] (32 位 RIFF 大小字段).
 */
internal class WavFileWriter(file: File, private val sampleRate: Int, private val channels: Int) : AutoCloseable {

    private val output = RandomAccessFile(file, "rw")
    private var bytes = ByteArray(0)
    private var closed = false

    var dataBytes = 0L
        private set

    init {
        try {
            output.setLength(0)
            output.write(header(sampleRate, channels, 0))
        } catch (error: Throwable) {
            runCatching { output.close() }
            throw error
        }
    }

    /** Whether [sampleCount] more samples still fit. zh-CN: 是否还能写入 [sampleCount] 个采样. */
    fun hasRoomFor(sampleCount: Int): Boolean = dataBytes + sampleCount * 2L <= MAX_DATA_BYTES

    fun write(samples: ShortArray, count: Int) {
        check(!closed) { "The WAV file is closed." }
        check(hasRoomFor(count)) { "The WAV file is full." }
        val size = count * 2
        if (bytes.size < size) bytes = ByteArray(size)
        for (i in 0 until count) {
            val value = samples[i].toInt()
            bytes[2 * i] = value.toByte()
            bytes[2 * i + 1] = (value shr 8).toByte()
        }
        output.write(bytes, 0, size)
        dataBytes += size
    }

    fun finish() {
        check(!closed) { "The WAV file is closed." }
        try {
            output.seek(0)
            output.write(header(sampleRate, channels, dataBytes))
        } finally {
            close()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { output.close() }
    }

    companion object {

        const val HEADER_BYTES = 44

        /** Largest data chunk: the RIFF size (36 + data) must fit in 32 bits, rounded down to whole stereo frames. */
        const val MAX_DATA_BYTES = (0xFFFFFFFFL - 36) / 4 * 4

        fun header(sampleRate: Int, channels: Int, dataBytes: Long): ByteArray {
            val blockAlign = channels * 2
            val header = ByteArray(HEADER_BYTES)
            fun ascii(offset: Int, text: String) = text.forEachIndexed { i, c -> header[offset + i] = c.code.toByte() }
            fun int32(offset: Int, value: Long) {
                for (i in 0 until 4) header[offset + i] = (value shr (8 * i)).toByte()
            }
            fun int16(offset: Int, value: Int) {
                header[offset] = value.toByte()
                header[offset + 1] = (value shr 8).toByte()
            }
            ascii(0, "RIFF")
            int32(4, 36 + dataBytes)
            ascii(8, "WAVE")
            ascii(12, "fmt ")
            int32(16, 16)
            int16(20, 1)
            int16(22, channels)
            int32(24, sampleRate.toLong())
            int32(28, sampleRate.toLong() * blockAlign)
            int16(32, blockAlign)
            int16(34, 16)
            ascii(36, "data")
            int32(40, dataBytes)
            return header
        }
    }
}

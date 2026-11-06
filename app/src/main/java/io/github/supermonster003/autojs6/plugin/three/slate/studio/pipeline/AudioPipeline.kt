@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessingPipeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.google.common.collect.ImmutableList
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
 * Building blocks of the audio operations (roadmap P2.2 / P2.5, D15), driven by [AudioOperations]: decoder PCM in any
 * platform encoding becomes 16-bit little endian ([Pcm16]), [PcmConverter] mixes the channels and runs Sonic (resampling,
 * speed, pitch), [PcmSinks] writes WAV itself, FLAC through the platform encoder, M4A (AAC LC) and OGG Opus (Android 10+)
 * through the platform encoders and MediaMuxer.
 * zh-CN: 音频操作 (路线图 P2.2 / P2.5, D15) 的构件, 由 [AudioOperations] 驱动: 解码器输出的各种平台 PCM 编码先转为 16 位小端
 * ([Pcm16]), [PcmConverter] 混合声道并运行 Sonic (重采样, 变速, 变调), [PcmSinks] 由插件自己写 WAV, 用平台编码器写 FLAC,
 * 用平台编码器与 MediaMuxer 写 M4A (AAC LC) 与 OGG Opus (Android 10+).
 */

/** Decoded PCM as the decoder reports it. zh-CN: 解码器报告的 PCM 格式. */
internal data class PcmFormat(val sampleRate: Int, val channels: Int, val encoding: Int) {

    override fun toString() = "$sampleRate Hz, $channels ch, encoding $encoding"

    companion object {

        fun of(format: MediaFormat): PcmFormat {
            val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING) else Pcm16.ENCODING_16BIT
            if (rate <= 0 || channels <= 0) throw MediaStudioException.unsupported("the decoder reported $rate Hz and $channels channels")
            if (!Pcm16.supports(encoding)) throw MediaStudioException.unsupported("the decoder gave PCM encoding $encoding")
            return PcmFormat(rate, channels, encoding)
        }

    }

}

/** Converts decoder PCM to 16-bit little endian. zh-CN: 把解码器的 PCM 转为 16 位小端. */
internal object Pcm16 {

    // android.media.AudioFormat encodings, as plain values for API 24. zh-CN: android.media.AudioFormat 的编码值, 以常量写出以兼容 API 24.
    const val ENCODING_16BIT = 2
    const val ENCODING_8BIT = 3
    const val ENCODING_FLOAT = 4
    const val ENCODING_24BIT_PACKED = 21
    const val ENCODING_32BIT = 22

    fun supports(encoding: Int): Boolean = encoding in setOf(ENCODING_16BIT, ENCODING_8BIT, ENCODING_FLOAT, ENCODING_24BIT_PACKED, ENCODING_32BIT)

    fun bytesFor(inputBytes: Int, encoding: Int): Int = when (encoding) {
        ENCODING_8BIT -> inputBytes * 2
        ENCODING_24BIT_PACKED -> inputBytes / 3 * 2
        ENCODING_FLOAT, ENCODING_32BIT -> inputBytes / 2
        else -> inputBytes
    }

    /** Reads [source] from position to limit and appends 16-bit samples to [target]. zh-CN: 读取 [source] 从 position 到 limit 的数据, 把 16 位样本追加到 [target]. */
    fun convert(source: ByteBuffer, encoding: Int, target: ByteBuffer) {
        val input = source.order(ByteOrder.LITTLE_ENDIAN)
        val output = target.order(ByteOrder.LITTLE_ENDIAN)
        when (encoding) {
            ENCODING_16BIT -> output.put(input)
            ENCODING_8BIT -> while (input.hasRemaining()) output.putShort((((input.get().toInt() and 0xFF) - 128) shl 8).toShort())
            ENCODING_24BIT_PACKED -> while (input.remaining() >= 3) {
                input.get()
                val low = input.get().toInt() and 0xFF
                val high = input.get().toInt()
                output.putShort(((high shl 8) or low).toShort())
            }
            ENCODING_32BIT -> while (input.remaining() >= 4) output.putShort((input.getInt() shr 16).toShort())
            ENCODING_FLOAT -> while (input.remaining() >= 4) {
                val sample = (input.getFloat().coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
                output.putShort(sample.toShort())
            }
            else -> throw MediaStudioException.unsupported("PCM encoding $encoding")
        }
    }

}

/** Where 16-bit PCM ends up. [write] consumes the whole buffer. zh-CN: 16 位 PCM 的去处. [write] 会消费整个缓冲区. */
internal interface PcmSink : Closeable {
    fun write(pcm: ByteBuffer)

    /** Flushes and closes the output. zh-CN: 刷新并关闭输出. */
    fun finish()

    /** Closes without finishing (failed or cancelled runs). zh-CN: 不收尾直接关闭 (运行失败或被取消时). */
    override fun close()
}

/**
 * 16-bit PCM through the channel mixer and Sonic (resampling, [speed], [pitch]) where the target differs from the source;
 * the output goes to the callback of [write] and [finish], which consumes each buffer.
 * zh-CN: 目标与源不同时, 让 16 位 PCM 经过声道混合器与 Sonic (重采样, [speed], [pitch]); 输出交给 [write] 与 [finish] 的回调,
 * 回调须消费每个缓冲区.
 */
internal class PcmConverter(source: PcmFormat, targetRate: Int, targetChannels: Int, speed: Float = 1f, pitch: Float = 1f) {

    private val pipeline: AudioProcessingPipeline?

    init {
        val processors = buildList<AudioProcessor> {
            if (targetChannels != source.channels) add(AudioProcessors.channelMixer(targetChannels))
            if (targetRate != source.sampleRate || speed != 1f || pitch != 1f) {
                add(
                    SonicAudioProcessor().apply {
                        if (targetRate != source.sampleRate) setOutputSampleRateHz(targetRate)
                        setSpeed(speed)
                        setPitch(pitch)
                    },
                )
            }
        }
        pipeline = if (processors.isEmpty()) {
            null
        } else {
            AudioProcessingPipeline(ImmutableList.copyOf(processors)).also { chain ->
                val configured = try {
                    chain.configure(AudioProcessor.AudioFormat(source.sampleRate, source.channels, C.ENCODING_PCM_16BIT))
                } catch (e: AudioProcessor.UnhandledAudioFormatException) {
                    throw MediaStudioException.unsupported("cannot convert $source to $targetRate Hz, $targetChannels ch (${e.message})")
                }
                if (configured.sampleRate != targetRate || configured.channelCount != targetChannels) {
                    throw MediaStudioException(
                        MediaStudioErrorCodes.INTERNAL_ERROR,
                        "the audio processors produce ${configured.sampleRate} Hz, ${configured.channelCount} ch instead of $targetRate Hz, $targetChannels ch",
                    )
                }
                chain.flush(AudioProcessor.StreamMetadata.DEFAULT)
            }
        }
    }

    fun write(pcm: ByteBuffer, out: (ByteBuffer) -> Unit) {
        val chain = pipeline ?: return out(pcm)
        var stalls = 0
        while (pcm.hasRemaining()) {
            val before = pcm.remaining()
            chain.queueInput(pcm)
            val drained = drain(chain, out)
            stalls = if (pcm.remaining() == before && !drained) stalls + 1 else 0
            if (stalls > MAX_STALLS) throw MediaStudioException(MediaStudioErrorCodes.INTERNAL_ERROR, "the audio processors stopped taking input")
        }
    }

    fun finish(out: (ByteBuffer) -> Unit) {
        val chain = pipeline ?: return
        chain.queueEndOfStream()
        var stalls = 0
        while (!chain.isEnded) {
            stalls = if (drain(chain, out)) 0 else stalls + 1
            if (stalls > MAX_STALLS) throw MediaStudioException(MediaStudioErrorCodes.INTERNAL_ERROR, "the audio processors did not end")
        }
        drain(chain, out)
    }

    private fun drain(chain: AudioProcessingPipeline, out: (ByteBuffer) -> Unit): Boolean {
        var any = false
        while (true) {
            val output = chain.getOutput()
            if (!output.hasRemaining()) return any
            out(output)
            any = true
        }
    }

    private companion object {
        const val MAX_STALLS = 1_000
    }

}

/** Canonical 44-byte header WAV, header written on [finish]. zh-CN: 44 字节标准头的 WAV, 头部在 [finish] 时写入. */
internal class WavSink(path: String, private val sampleRate: Int, private val channels: Int) : PcmSink {

    private val file = RandomAccessFile(path, "rw").apply {
        setLength(0)
        write(ByteArray(HEADER_BYTES))
    }

    private val channel = file.channel

    private var dataBytes = 0L

    override fun write(pcm: ByteBuffer) {
        val size = pcm.remaining()
        if (dataBytes + size > MAX_DATA_BYTES) throw MediaStudioException.io("the WAV output would exceed 4 GiB; use flac or a lower sample rate")
        while (pcm.hasRemaining()) channel.write(pcm)
        dataBytes += size
    }

    override fun finish() {
        val blockAlign = channels * 2
        val header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((HEADER_BYTES - 8 + dataBytes).toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(sampleRate * blockAlign)
            putShort(blockAlign.toShort())
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataBytes.toInt())
            flip()
        }
        var position = 0L
        while (header.hasRemaining()) position += channel.write(header, position)
        file.close()
    }

    override fun close() {
        runCatching { file.close() }
    }

    private companion object {
        const val HEADER_BYTES = 44
        const val MAX_DATA_BYTES = 0xFFFF_FFFFL - (HEADER_BYTES - 8)
    }

}

/** Feeds 16-bit PCM to a platform encoder and hands out its output. zh-CN: 把 16 位 PCM 送入平台编码器并转交其输出. */
internal abstract class EncoderSink(mime: String, format: MediaFormat, private val sampleRate: Int, channels: Int) : PcmSink {

    protected val codec: MediaCodec = Codecs.encoder(mime, format)

    private val frameBytes = channels * 2

    private val info = MediaCodec.BufferInfo()

    private var queuedFrames = 0L

    /** Frames per channel queued so far. zh-CN: 已送入的每声道帧数. */
    protected val frames: Long get() = queuedFrames

    private var released = false

    protected abstract fun onFormat(format: MediaFormat)

    protected abstract fun onConfig(buffer: ByteBuffer)

    protected abstract fun onSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo)

    /** After the end of stream left the encoder. zh-CN: 流结束标记离开编码器之后. */
    protected abstract fun complete()

    protected abstract fun abort()

    override fun write(pcm: ByteBuffer) {
        while (pcm.hasRemaining()) {
            val index = codec.dequeueInputBuffer(Codecs.TIMEOUT_US)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index) ?: throw Codecs.error("the encoder gave no input buffer")
                buffer.clear()
                val size = minOf(buffer.remaining(), pcm.remaining()) / frameBytes * frameBytes
                if (size <= 0) throw Codecs.error("the encoder input buffer holds less than one frame")
                val slice = pcm.duplicate()
                slice.limit(slice.position() + size)
                buffer.put(slice)
                pcm.position(pcm.position() + size)
                codec.queueInputBuffer(index, 0, size, timeUs(queuedFrames), 0)
                queuedFrames += size / frameBytes
            }
            drain(untilEnd = false)
        }
    }

    override fun finish() {
        while (true) {
            val index = codec.dequeueInputBuffer(Codecs.TIMEOUT_US)
            if (index >= 0) {
                codec.queueInputBuffer(index, 0, 0, timeUs(queuedFrames), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                break
            }
            drain(untilEnd = false)
        }
        drain(untilEnd = true)
        complete()
        release()
    }

    override fun close() {
        runCatching { abort() }
        release()
    }

    private fun release() {
        if (released) return
        released = true
        runCatching { codec.stop() }
        runCatching { codec.release() }
    }

    private fun timeUs(frames: Long): Long = frames * 1_000_000L / sampleRate

    private fun drain(untilEnd: Boolean) {
        var idle = 0
        while (true) {
            val index = codec.dequeueOutputBuffer(info, if (untilEnd) Codecs.TIMEOUT_US else 0L)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!untilEnd) return
                    if (++idle > MAX_IDLE_POLLS) throw Codecs.error("the encoder did not finish within ${MAX_IDLE_POLLS * Codecs.TIMEOUT_US / 1_000_000L} s")
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> onFormat(codec.outputFormat)
                index >= 0 -> {
                    idle = 0
                    val buffer = codec.getOutputBuffer(index) ?: throw Codecs.error("the encoder gave no output buffer")
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        onConfig(buffer)
                    } else if (info.size > 0) {
                        onSample(buffer, info)
                    }
                    val ended = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(index, false)
                    if (ended) return
                }
            }
        }
    }

    private companion object {
        const val MAX_IDLE_POLLS = 1_000
    }

}

/**
 * Native FLAC: the encoder's `fLaC` + STREAMINFO header, then its frames; the total sample count is patched in at the
 * end (the platform encoder cannot seek back).
 * zh-CN: 原生 FLAC: 编码器给出的 `fLaC` + STREAMINFO 头, 然后是各帧; 结束时补写总样本数 (平台编码器无法回写).
 */
internal class FlacSink(path: String, sampleRate: Int, channels: Int) :
    EncoderSink(MediaFormat.MIMETYPE_AUDIO_FLAC, format(sampleRate, channels), sampleRate, channels) {

    private val file = RandomAccessFile(path, "rw").apply { setLength(0) }

    private val channel = file.channel

    private var header: ByteArray? = null

    private var headerWritten = false

    override fun onFormat(format: MediaFormat) {
        if (header == null) format.getByteBuffer("csd-0")?.let { header = streamHeader(it.duplicate()) }
    }

    override fun onConfig(buffer: ByteBuffer) {
        if (header == null) header = streamHeader(buffer)
    }

    override fun onSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        writeHeader()
        while (buffer.hasRemaining()) channel.write(buffer)
    }

    override fun complete() {
        writeHeader()
        val field = ByteBuffer.allocate(8)
        channel.read(field, TOTAL_SAMPLES_OFFSET)
        field.flip()
        val mask = (1L shl 36) - 1
        val value = (field.getLong() and mask.inv()) or (frames and mask)
        field.clear()
        field.putLong(value).flip()
        channel.write(field, TOTAL_SAMPLES_OFFSET)
        file.close()
    }

    override fun abort() {
        file.close()
    }

    private fun writeHeader() {
        if (headerWritten) return
        val bytes = header ?: throw Codecs.error("the FLAC encoder gave no stream header")
        channel.write(ByteBuffer.wrap(bytes), 0L)
        channel.position(bytes.size.toLong())
        headerWritten = true
    }

    private companion object {

        const val COMPRESSION_LEVEL = 5

        const val STREAMINFO_BYTES = 34

        // "fLaC" (4) + block header (4) + 10 bytes into STREAMINFO. zh-CN: "fLaC" (4) + 块头 (4) + STREAMINFO 内 10 字节.
        const val TOTAL_SAMPLES_OFFSET = 18L

        val MAGIC = "fLaC".toByteArray(Charsets.US_ASCII)

        fun format(sampleRate: Int, channels: Int): MediaFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_FLAC, sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, COMPRESSION_LEVEL)
            }

        /**
         * `fLaC` and the STREAMINFO block flagged as the last metadata block, from codec data with or without the magic.
         * zh-CN: 从带或不带魔数的编解码数据中取出 `fLaC` 与 STREAMINFO 块, 并把该块标为最后一个元数据块.
         */
        fun streamHeader(data: ByteBuffer): ByteArray {
            val bytes = ByteArray(data.remaining()).also { data.get(it) }
            val start = if (bytes.size >= 4 && bytes.copyOfRange(0, 4).contentEquals(MAGIC)) 4 else 0
            if (bytes.size < start + 4 + STREAMINFO_BYTES) throw Codecs.error("the FLAC stream header is ${bytes.size} bytes")
            val type = bytes[start].toInt() and 0x7F
            val length = ((bytes[start + 1].toInt() and 0xFF) shl 16) or ((bytes[start + 2].toInt() and 0xFF) shl 8) or (bytes[start + 3].toInt() and 0xFF)
            if (type != 0 || length != STREAMINFO_BYTES) throw Codecs.error("the FLAC stream header starts with block type $type of $length bytes")
            return MAGIC + byteArrayOf(0x80.toByte(), 0, 0, STREAMINFO_BYTES.toByte()) + bytes.copyOfRange(start + 4, start + 4 + STREAMINFO_BYTES)
        }

    }

}

/**
 * An encoded track in a MediaMuxer container: M4A (AAC LC in MPEG-4) or OGG Opus (Android 10+). The codec data travels
 * in the output format.
 * zh-CN: MediaMuxer 容器中的一条编码轨: M4A (MPEG-4 中的 AAC LC) 或 OGG Opus (Android 10+). 编解码数据随输出格式传递.
 */
internal class MuxerSink(path: String, muxerFormat: Int, format: MediaFormat, sampleRate: Int, channels: Int) :
    EncoderSink(format.getString(MediaFormat.KEY_MIME)!!, format, sampleRate, channels) {

    private val muxer = MediaMuxer(path, muxerFormat)

    private var track = -1

    private val sample = MediaCodec.BufferInfo()

    override fun onFormat(format: MediaFormat) {
        if (track >= 0) return
        track = muxer.addTrack(format)
        muxer.start()
    }

    override fun onConfig(buffer: ByteBuffer) {
        // The codec data travels in the output format. zh-CN: 编解码数据随输出格式传递.
    }

    override fun onSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (track < 0) throw Codecs.error("the encoder gave samples before its format")
        sample.set(info.offset, info.size, info.presentationTimeUs, info.flags and (MediaCodec.BUFFER_FLAG_END_OF_STREAM or MediaCodec.BUFFER_FLAG_CODEC_CONFIG).inv())
        muxer.writeSampleData(track, buffer, sample)
    }

    override fun complete() {
        if (track < 0) throw Codecs.error("the encoder gave no output")
        muxer.stop()
        muxer.release()
    }

    override fun abort() {
        if (track >= 0) runCatching { muxer.stop() }
        muxer.release()
    }

}

/** Opens the sink of an [AudioContainer]. zh-CN: 打开某种 [AudioContainer] 的输出端. */
internal object PcmSinks {

    /** [bitRate] applies to M4A and OGG. zh-CN: [bitRate] 用于 M4A 与 OGG. */
    fun open(container: AudioContainer, path: String, sampleRate: Int, channels: Int, bitRate: Int?): PcmSink = when (container) {
        AudioContainer.WAV -> WavSink(path, sampleRate, channels)
        AudioContainer.FLAC -> FlacSink(path, sampleRate, channels)
        AudioContainer.M4A -> MuxerSink(
            path,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, requireNotNull(bitRate) { "M4A needs a bit rate" })
            },
            sampleRate,
            channels,
        )
        AudioContainer.OGG -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ogg(path, sampleRate, channels, requireNotNull(bitRate) { "OGG needs a bit rate" })
        } else {
            throw MediaStudioException.unsupported("ogg output needs Android 10 (API 29) or later")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ogg(path: String, sampleRate: Int, channels: Int, bitRate: Int) = MuxerSink(
        path,
        MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG,
        MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        },
        sampleRate,
        channels,
    )

}

/** Creates and configures platform codecs with contract errors. zh-CN: 创建并配置平台编解码器, 失败时给出契约错误. */
internal object Codecs {

    const val TIMEOUT_US = 10_000L

    fun error(detail: String, cause: Throwable? = null) = MediaStudioException(MediaStudioErrorCodes.CODEC_ERROR, detail, cause)

    fun decoder(mime: String, format: MediaFormat): MediaCodec = create(mime, format, encoder = false)

    fun encoder(mime: String, format: MediaFormat): MediaCodec = create(mime, format, encoder = true)

    private fun create(mime: String, format: MediaFormat, encoder: Boolean): MediaCodec {
        val kind = if (encoder) "encoder" else "decoder"
        val codec = try {
            if (encoder) MediaCodec.createEncoderByType(mime) else MediaCodec.createDecoderByType(mime)
        } catch (e: Exception) {
            throw MediaStudioException.unsupported("no $mime $kind on this device (${e.message ?: e.javaClass.simpleName})")
        }
        try {
            codec.configure(format, null, null, if (encoder) MediaCodec.CONFIGURE_FLAG_ENCODE else 0)
            codec.start()
        } catch (e: RuntimeException) {
            runCatching { codec.release() }
            throw error("the $mime $kind rejected $format (${e.message ?: e.javaClass.simpleName})", e)
        }
        return codec
    }

}

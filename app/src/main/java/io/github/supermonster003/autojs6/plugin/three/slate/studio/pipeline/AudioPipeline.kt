@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.media.MediaCodec
import android.media.MediaExtractor
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
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.BitRates
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * `extractAudio` to WAV, FLAC and OGG (roadmap P2.2, D15): decodes the first audio track to 16-bit PCM, mixes the
 * channels and resamples with media3 audio processors, then writes WAV itself, FLAC through the platform encoder and
 * OGG Opus through the platform encoder and MediaMuxer (Android 10+). Defaults: WAV keeps the channels, FLAC and OGG
 * keep at most 2; OGG runs at 48 kHz, the others keep the source rate.
 * zh-CN: `extractAudio` 输出 WAV, FLAC 与 OGG (路线图 P2.2, D15): 把第一条音轨解码为 16 位 PCM, 用 media3 音频处理器混合声道并
 * 重采样, 然后由插件自己写 WAV, 用平台编码器写 FLAC, 用平台编码器与 MediaMuxer 写 OGG Opus (Android 10+). 默认: WAV 保留声道数,
 * FLAC 与 OGG 最多保留 2 个声道; OGG 为 48 kHz, 其他保留源采样率.
 */
internal class AudioExport(private val task: TaskRunner.JobContext, private val notes: MutableList<String>) {

    fun run(input: MediaInfo, output: String, spec: OperationSpec.ExtractAudio, from: Double, to: Double) {
        require(spec.container != AudioContainer.M4A) { "M4A runs through media3 Transformer" }
        val stage = if (spec.container == AudioContainer.WAV) MediaStudioContract.STAGE_DECODING else MediaStudioContract.STAGE_ENCODING
        val durationUs = (input.durationMillis * 1_000L).coerceAtLeast(1L)
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var chain: PcmChain? = null
        var completed = false
        try {
            try {
                extractor.setDataSource(input.path)
            } catch (e: Exception) {
                throw MediaStudioException.unsupported("cannot read media from ${input.path} (${e.message ?: e.javaClass.simpleName})")
            }
            val track = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw MediaStudioException.unsupported("no audio track in ${input.path}")
            extractor.selectTrack(track)
            val trackFormat = extractor.getTrackFormat(track)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
            val codec = Codecs.decoder(mime, trackFormat).also { decoder = it }
            val info = MediaCodec.BufferInfo()
            var pcmFormat: PcmFormat? = null
            var scratch = ByteBuffer.allocateDirect(INITIAL_SCRATCH_BYTES).order(ByteOrder.nativeOrder())
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                task.throwIfCancelled()
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index) ?: throw codecError("the decoder gave no input buffer")
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime.coerceAtLeast(0L), 0)
                            extractor.advance()
                        }
                    }
                }
                val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val changed = PcmFormat.of(codec.outputFormat)
                        if (chain != null && changed != pcmFormat) {
                            throw MediaStudioException.unsupported("the audio format of ${input.path} changed while decoding ($pcmFormat -> $changed)")
                        }
                        pcmFormat = changed
                    }
                    index >= 0 -> {
                        if (info.size > 0) {
                            val format = pcmFormat ?: PcmFormat.of(codec.outputFormat).also { pcmFormat = it }
                            val buffer = codec.getOutputBuffer(index) ?: throw codecError("the decoder gave no output buffer")
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val needed = Pcm16.bytesFor(info.size, format.encoding)
                            if (scratch.capacity() < needed) scratch = ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder())
                            scratch.clear()
                            Pcm16.convert(buffer, format.encoding, scratch)
                            scratch.flip()
                            val active = chain ?: PcmChain(format, targetRate(spec, format), targetChannels(spec, format), sink(spec, output, format)).also {
                                chain = it
                                if (format.channels > 2 && targetChannels(spec, format) == 2) {
                                    notes += "the ${format.channels} channel input was mixed down to 2 channels"
                                }
                            }
                            active.write(scratch)
                            if (info.presentationTimeUs > 0) task.progress(from + (to - from) * (info.presentationTimeUs.toDouble() / durationUs).coerceIn(0.0, 1.0), stage)
                        }
                        codec.releaseOutputBuffer(index, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
            val active = chain ?: throw codecError("no audio samples were decoded from ${input.path}")
            task.throwIfCancelled()
            active.finish()
            completed = true
        } finally {
            decoder?.let { codec -> runCatching { codec.stop() }; runCatching { codec.release() } }
            runCatching { extractor.release() }
            if (!completed) chain?.close()
        }
    }

    private fun targetChannels(spec: OperationSpec.ExtractAudio, source: PcmFormat): Int =
        spec.audio.channels ?: if (spec.container == AudioContainer.WAV) source.channels else minOf(source.channels, 2)

    private fun targetRate(spec: OperationSpec.ExtractAudio, source: PcmFormat): Int =
        spec.audio.sampleRate ?: if (spec.container == AudioContainer.OGG) OPUS_DEFAULT_SAMPLE_RATE else source.sampleRate

    private fun sink(spec: OperationSpec.ExtractAudio, output: String, source: PcmFormat): (Int, Int) -> PcmSink = { rate, channels ->
        when (spec.container) {
            AudioContainer.WAV -> WavSink(output, rate, channels)
            AudioContainer.FLAC -> FlacSink(output, rate, channels)
            AudioContainer.OGG -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OggSink(output, rate, channels, spec.audio.bitRate ?: BitRates.opus(channels))
            } else {
                throw MediaStudioException.unsupported("ogg output needs Android 10 or later")
            }
            AudioContainer.M4A -> error("M4A runs through media3 Transformer; source $source")
        }
    }

    companion object {

        const val TIMEOUT_US = 10_000L

        const val OPUS_DEFAULT_SAMPLE_RATE = 48_000

        private const val INITIAL_SCRATCH_BYTES = 64 * 1024

        fun codecError(detail: String, cause: Throwable? = null) = MediaStudioException(MediaStudioErrorCodes.CODEC_ERROR, detail, cause)

    }

}

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
 * 16-bit PCM through the channel mixer and the Sonic resampler where the target differs from the source.
 * zh-CN: 目标与源不同时, 让 16 位 PCM 经过声道混合器与 Sonic 重采样器.
 */
private class PcmChain(source: PcmFormat, targetRate: Int, targetChannels: Int, sinkFactory: (Int, Int) -> PcmSink) : Closeable {

    private val pipeline: AudioProcessingPipeline?

    private val sink: PcmSink

    init {
        val processors = buildList<AudioProcessor> {
            if (targetChannels != source.channels) add(AudioProcessors.channelMixer(targetChannels))
            if (targetRate != source.sampleRate) add(SonicAudioProcessor().apply { setOutputSampleRateHz(targetRate) })
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
        sink = sinkFactory(targetRate, targetChannels)
    }

    fun write(pcm: ByteBuffer) {
        val chain = pipeline ?: return sink.write(pcm)
        var stalls = 0
        while (pcm.hasRemaining()) {
            val before = pcm.remaining()
            chain.queueInput(pcm)
            val drained = drain(chain)
            stalls = if (pcm.remaining() == before && !drained) stalls + 1 else 0
            if (stalls > MAX_STALLS) throw MediaStudioException(MediaStudioErrorCodes.INTERNAL_ERROR, "the audio processors stopped taking input")
        }
    }

    fun finish() {
        pipeline?.let { chain ->
            chain.queueEndOfStream()
            var stalls = 0
            while (!chain.isEnded) {
                stalls = if (drain(chain)) 0 else stalls + 1
                if (stalls > MAX_STALLS) throw MediaStudioException(MediaStudioErrorCodes.INTERNAL_ERROR, "the audio processors did not end")
            }
            drain(chain)
        }
        sink.finish()
    }

    override fun close() {
        sink.close()
    }

    private fun drain(chain: AudioProcessingPipeline): Boolean {
        var any = false
        while (true) {
            val output = chain.getOutput()
            if (!output.hasRemaining()) return any
            sink.write(output)
            any = true
        }
    }

    private companion object {
        const val MAX_STALLS = 1_000
    }

}

/** Canonical 44-byte header WAV, header written on [finish]. zh-CN: 44 字节标准头的 WAV, 头部在 [finish] 时写入. */
private class WavSink(path: String, private val sampleRate: Int, private val channels: Int) : PcmSink {

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
private abstract class EncoderSink(mime: String, format: MediaFormat, private val sampleRate: Int, channels: Int) : PcmSink {

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
            val index = codec.dequeueInputBuffer(AudioExport.TIMEOUT_US)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index) ?: throw AudioExport.codecError("the encoder gave no input buffer")
                buffer.clear()
                val size = minOf(buffer.remaining(), pcm.remaining()) / frameBytes * frameBytes
                if (size <= 0) throw AudioExport.codecError("the encoder input buffer holds less than one frame")
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
            val index = codec.dequeueInputBuffer(AudioExport.TIMEOUT_US)
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
            val index = codec.dequeueOutputBuffer(info, if (untilEnd) AudioExport.TIMEOUT_US else 0L)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!untilEnd) return
                    if (++idle > MAX_IDLE_POLLS) throw AudioExport.codecError("the encoder did not finish within ${MAX_IDLE_POLLS * AudioExport.TIMEOUT_US / 1_000_000L} s")
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> onFormat(codec.outputFormat)
                index >= 0 -> {
                    idle = 0
                    val buffer = codec.getOutputBuffer(index) ?: throw AudioExport.codecError("the encoder gave no output buffer")
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
private class FlacSink(path: String, sampleRate: Int, channels: Int) :
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
        val bytes = header ?: throw AudioExport.codecError("the FLAC encoder gave no stream header")
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
            if (bytes.size < start + 4 + STREAMINFO_BYTES) throw AudioExport.codecError("the FLAC stream header is ${bytes.size} bytes")
            val type = bytes[start].toInt() and 0x7F
            val length = ((bytes[start + 1].toInt() and 0xFF) shl 16) or ((bytes[start + 2].toInt() and 0xFF) shl 8) or (bytes[start + 3].toInt() and 0xFF)
            if (type != 0 || length != STREAMINFO_BYTES) throw AudioExport.codecError("the FLAC stream header starts with block type $type of $length bytes")
            return MAGIC + byteArrayOf(0x80.toByte(), 0, 0, STREAMINFO_BYTES.toByte()) + bytes.copyOfRange(start + 4, start + 4 + STREAMINFO_BYTES)
        }

    }

}

/** Opus in OGG through MediaMuxer (Android 10+). zh-CN: 经 MediaMuxer 写入 OGG 的 Opus (Android 10+). */
@RequiresApi(Build.VERSION_CODES.Q)
private class OggSink(path: String, sampleRate: Int, channels: Int, bitRate: Int) :
    EncoderSink(MediaFormat.MIMETYPE_AUDIO_OPUS, format(sampleRate, channels, bitRate), sampleRate, channels) {

    private val muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)

    private var track = -1

    private val sample = MediaCodec.BufferInfo()

    override fun onFormat(format: MediaFormat) {
        if (track >= 0) return
        track = muxer.addTrack(format)
        muxer.start()
    }

    override fun onConfig(buffer: ByteBuffer) {
        // The Opus headers travel in the output format. zh-CN: Opus 头随输出格式传递.
    }

    override fun onSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (track < 0) throw AudioExport.codecError("the Opus encoder gave samples before its format")
        sample.set(info.offset, info.size, info.presentationTimeUs, info.flags and (MediaCodec.BUFFER_FLAG_END_OF_STREAM or MediaCodec.BUFFER_FLAG_CODEC_CONFIG).inv())
        muxer.writeSampleData(track, buffer, sample)
    }

    override fun complete() {
        if (track < 0) throw AudioExport.codecError("the Opus encoder gave no output")
        muxer.stop()
        muxer.release()
    }

    override fun abort() {
        if (track >= 0) runCatching { muxer.stop() }
        muxer.release()
    }

    private companion object {
        fun format(sampleRate: Int, channels: Int, bitRate: Int): MediaFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            }
    }

}

/** Creates and configures platform codecs with contract errors. zh-CN: 创建并配置平台编解码器, 失败时给出契约错误. */
private object Codecs {

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
            throw AudioExport.codecError("the $mime $kind rejected $format (${e.message ?: e.javaClass.simpleName})", e)
        }
        return codec
    }

}

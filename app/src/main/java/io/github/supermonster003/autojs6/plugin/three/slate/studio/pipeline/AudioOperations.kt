package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioTargets
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.PcmMath
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * The audio operations of roadmap P2.5 (D15) and `extractAudio` to WAV / FLAC / OGG: every input is decoded to 16-bit
 * PCM in one target format ([AudioTargets], [DecodedAudio]), processed and encoded through [PcmSinks].
 * `audioTrim` cuts in source time, `audioConcat` appends, `audioMix` sums the inputs with their volume and start offset
 * (a plain sum, clipping is reported in `warnings`, appendix D Q17 (a)), `audioVolume` applies a linear gain or peak
 * normalization and linear fades (normalization and fade out read the input twice), `audioSpeed` runs Sonic (pitch kept
 * or shifted with the speed), `audioConvert` re-encodes.
 * zh-CN: 路线图 P2.5 (D15) 的音频操作以及 `extractAudio` 输出 WAV / FLAC / OGG: 每个输入都解码为同一目标格式的 16 位 PCM
 * ([AudioTargets], [DecodedAudio]), 加工后经 [PcmSinks] 编码. `audioTrim` 按源时间裁剪, `audioConcat` 依次拼接, `audioMix`
 * 按各输入的音量与起点相加 (直接相加, 削波在 `warnings` 中报告, 附录 D Q17 (a)), `audioVolume` 施加线性增益或峰值归一化以及
 * 线性淡入淡出 (归一化与淡出会把输入读两遍), `audioSpeed` 运行 Sonic (保持音调或随速度变调), `audioConvert` 重新编码.
 */
internal class AudioOperations(private val task: TaskRunner.JobContext, private val notes: MutableList<String>) {

    fun run(spec: OperationSpec.AudioOutput, inputs: List<MediaInfo>, output: String, from: Double, to: Double) {
        val stage = if (spec.container == AudioContainer.WAV) MediaStudioContract.STAGE_DECODING else MediaStudioContract.STAGE_ENCODING
        val progress = Progress(from, to, stage)
        when (spec) {
            is OperationSpec.ExtractAudio -> {
                require(spec.container != AudioContainer.M4A) { "extractAudio to M4A runs through media3 Transformer" }
                copy(spec, inputs.single(), output, progress)
            }
            is OperationSpec.AudioConvert -> copy(spec, inputs.single(), output, progress)
            is OperationSpec.AudioTrim -> {
                val input = inputs.single()
                if (spec.startMillis >= input.durationMillis) {
                    throw MediaStudioException.invalid("options.start (${spec.startMillis} ms) is not before the end of the input (${input.durationMillis} ms)")
                }
                val endUs = spec.endMillis?.takeIf { it < input.durationMillis }?.let { it * 1_000L }
                copy(spec, input, output, progress, startUs = spec.startMillis * 1_000L, endUs = endUs)
            }
            is OperationSpec.AudioSpeed -> {
                val factor = spec.factor.toFloat()
                copy(spec, inputs.single(), output, progress, speed = factor, pitch = if (spec.keepPitch) 1f else factor)
            }
            is OperationSpec.AudioVolume -> volume(spec, inputs.single(), output, progress)
            is OperationSpec.AudioConcat -> concat(spec, inputs, output, progress)
            is OperationSpec.AudioMix -> mix(spec, inputs, output, progress)
        }
    }

    /** One input to the output, cut to source time [startUs, endUs). zh-CN: 单个输入写到输出, 按源时间裁到 [startUs, endUs). */
    private fun copy(
        spec: OperationSpec.AudioOutput,
        input: MediaInfo,
        output: String,
        progress: Progress,
        startUs: Long = 0L,
        endUs: Long? = null,
        speed: Float = 1f,
        pitch: Float = 1f,
    ) {
        val spanUs = ((endUs ?: (input.durationMillis * 1_000L)) - startUs).coerceAtLeast(1L)
        DecodedAudio(task, input.path, startUs, endUs, speed, pitch) { source -> targetOf(spec, source) }.use { decoded ->
            Output(spec, output).use { out ->
                while (true) {
                    val samples = decoded.read() ?: break
                    out.open(decoded.target!!, decoded.source!!.channels)
                    out.write(samples)
                    progress.report((decoded.positionUs - startUs).toDouble() / spanUs)
                }
                out.finish("no audio samples were decoded from ${input.path}")
            }
        }
    }

    /**
     * Gain or normalization with fades. Normalization needs the peak and the fade out the frame count, so those read the
     * input once before writing ([ANALYSIS_SHARE] of the progress).
     * zh-CN: 带淡入淡出的增益或归一化. 归一化需要峰值, 淡出需要总帧数, 因此写出前先把输入读一遍 (占进度的 [ANALYSIS_SHARE]).
     */
    private fun volume(spec: OperationSpec.AudioVolume, input: MediaInfo, output: String, progress: Progress) {
        val durationUs = (input.durationMillis * 1_000L).coerceAtLeast(1L)
        val analysis = spec.normalizeDbfs != null || spec.fadeOutMillis != null
        var factor = spec.gain ?: 1.0
        var totalFrames: Long? = null
        if (analysis) {
            val scan = progress.part(0.0, ANALYSIS_SHARE)
            var peak = 0
            var frames = 0L
            DecodedAudio(task, input.path) { source -> targetOf(spec, source) }.use { decoded ->
                while (true) {
                    val samples = decoded.read() ?: break
                    peak = maxOf(peak, PcmMath.peak(samples))
                    frames += samples.size / decoded.target!!.channels
                    scan.report(decoded.positionUs.toDouble() / durationUs)
                }
            }
            if (frames == 0L) throw Codecs.error("no audio samples were decoded from ${input.path}")
            totalFrames = frames
            spec.normalizeDbfs?.let { dbfs ->
                factor = PcmMath.normalizeFactor(peak, dbfs) ?: 1.0.also { notes += "the input is silent; options.normalize left it unchanged" }
            }
        }
        val write = if (analysis) progress.part(ANALYSIS_SHARE, 1.0) else progress
        var clipped = 0L
        var frame = 0L
        var channels = 1
        DecodedAudio(task, input.path) { source -> targetOf(spec, source) }.use { decoded ->
            Output(spec, output).use { out ->
                var fades: PcmMath.Fades? = null
                while (true) {
                    val samples = decoded.read() ?: break
                    val target = decoded.target!!
                    channels = target.channels
                    out.open(target, decoded.source!!.channels)
                    val active = fades ?: PcmMath.Fades(
                        inFrames = spec.fadeInMillis?.let { AudioTargets.frames(it, target.sampleRate) } ?: 0L,
                        outFrames = spec.fadeOutMillis?.let { AudioTargets.frames(it, target.sampleRate) } ?: 0L,
                        totalFrames = totalFrames,
                    ).also { fades = it }
                    clipped += PcmMath.applyGain(samples, channels, frame, factor, active.takeUnless { it.isEmpty })
                    frame += samples.size / channels
                    out.write(samples)
                    write.report(decoded.positionUs.toDouble() / durationUs)
                }
                out.finish("no audio samples were decoded from ${input.path}")
            }
        }
        if (clipped > 0) notes += clippedNote(clipped, frame * channels, "lower options.gain")
    }

    private fun concat(spec: OperationSpec.AudioConcat, inputs: List<MediaInfo>, output: String, progress: Progress) {
        val (rate, channels) = AudioTargets.common(inputs.map { it.sampleRate }, inputs.map { it.channels })
        val target = AudioTargets.of(spec.container, spec.audio, rate, channels)
        val totalUs = inputs.sumOf { it.durationMillis * 1_000L }.coerceAtLeast(1L)
        var doneUs = 0L
        var written = 0L
        Output(spec, output).use { out ->
            out.open(target, channels)
            inputs.forEach { input ->
                DecodedAudio(task, input.path) { target }.use { decoded ->
                    while (true) {
                        val samples = decoded.read() ?: break
                        written += samples.size
                        out.write(samples)
                        progress.report((doneUs + decoded.positionUs).toDouble() / totalUs)
                    }
                }
                doneUs += input.durationMillis * 1_000L
            }
            if (written == 0L) throw Codecs.error("no audio samples were decoded from the inputs")
            out.finish("no audio samples were decoded from the inputs")
        }
    }

    /**
     * Sums the inputs in blocks of [MIX_FRAMES] frames; each input starts at its offset, the output lasts until the last
     * input ends (appendix D Q17 (a), Q18 (a)).
     * zh-CN: 以 [MIX_FRAMES] 帧为一块把各输入相加; 每个输入从自己的起点开始, 输出持续到最后一个输入结束 (附录 D Q17 (a), Q18 (a)).
     */
    private fun mix(spec: OperationSpec.AudioMix, inputs: List<MediaInfo>, output: String, progress: Progress) {
        val (rate, sourceChannels) = AudioTargets.common(inputs.map { it.sampleRate }, inputs.map { it.channels })
        val target = AudioTargets.of(spec.container, spec.audio, rate, sourceChannels)
        val channels = target.channels
        val tracks = ArrayList<MixInput>(inputs.size)
        try {
            inputs.forEachIndexed { index, input ->
                val track = spec.tracks[index]
                tracks += MixInput(DecodedAudio(task, input.path) { target }, track.volume.toFloat(), AudioTargets.frames(track.atMillis, target.sampleRate), channels)
            }
            val expected = inputs.indices.maxOf { tracks[it].offsetFrames + AudioTargets.frames(inputs[it].durationMillis, target.sampleRate) }.coerceAtLeast(1L)
            val sum = FloatArray(MIX_FRAMES * channels)
            val block = ShortArray(MIX_FRAMES * channels)
            var position = 0L
            var clipped = 0L
            Output(spec, output).use { out ->
                out.open(target, sourceChannels)
                while (true) {
                    task.throwIfCancelled()
                    sum.fill(0f)
                    var length = 0
                    for (track in tracks) {
                        val start = track.offsetFrames - position
                        if (start >= MIX_FRAMES) {
                            // Still to come: the block is silence for it. zh-CN: 尚未开始: 本块对它是静音.
                            length = MIX_FRAMES
                            continue
                        }
                        val first = maxOf(start, 0L).toInt()
                        var frame = first
                        while (frame < MIX_FRAMES) {
                            val at = frame * channels
                            val taken = track.take(MIX_FRAMES - frame) { samples, from, frames ->
                                PcmMath.accumulate(sum, at, samples, from, frames * channels, track.volume)
                            }
                            if (taken == 0) break
                            frame += taken
                        }
                        if (frame > first || !track.exhausted) length = maxOf(length, frame)
                    }
                    if (length == 0) break
                    clipped += PcmMath.clip(sum, length * channels, block)
                    out.write(block, length * channels)
                    position += length
                    progress.report(position.toDouble() / expected)
                }
                if (position == 0L) throw Codecs.error("no audio samples were decoded from the inputs")
                if (clipped > 0) notes += clippedNote(clipped, position * channels, "lower the track volumes")
                out.finish("no audio samples were decoded from the inputs")
            }
        } finally {
            tracks.forEach { it.source.close() }
        }
    }

    private fun targetOf(spec: OperationSpec.AudioOutput, source: PcmFormat): AudioTargets.Target =
        AudioTargets.of(spec.container, spec.audio, source.sampleRate, source.channels)

    private fun clippedNote(clipped: Long, total: Long, advice: String): String =
        String.format(Locale.ROOT, "%d of %d samples (%.2f %%) were clipped; %s", clipped, total, 100.0 * clipped / total.coerceAtLeast(1L), advice)

    private inner class Progress(private val from: Double, private val to: Double, private val stage: String) {

        fun report(fraction: Double) = task.progress(from + (to - from) * fraction.coerceIn(0.0, 1.0), stage)

        fun part(start: Double, end: Double) = Progress(from + (to - from) * start, from + (to - from) * end, stage)

    }

    /**
     * The sink of one operation, opened with the first target format; [finish] completes it, [close] alone discards it.
     * zh-CN: 一个操作的输出端, 在得到目标格式时打开; [finish] 完成输出, 只调用 [close] 则丢弃.
     */
    private inner class Output(private val spec: OperationSpec.AudioOutput, private val path: String) : Closeable {

        private var sink: PcmSink? = null

        private var finished = false

        private var buffer: ByteBuffer = ByteBuffer.allocateDirect(INITIAL_BUFFER_BYTES).order(ByteOrder.LITTLE_ENDIAN)

        fun open(target: AudioTargets.Target, sourceChannels: Int) {
            if (sink != null) return
            if (spec.audio.channels == null && sourceChannels > 2 && target.channels == 2) {
                notes += "the $sourceChannels channel input was mixed down to 2 channels"
            }
            sink = PcmSinks.open(spec.container, path, target.sampleRate, target.channels, AudioTargets.bitRate(spec.container, spec.audio, target))
        }

        fun write(samples: ShortArray, count: Int = samples.size) {
            val active = checkNotNull(sink) { "the output is not open" }
            val bytes = count * 2
            if (buffer.capacity() < bytes) buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.clear()
            buffer.asShortBuffer().put(samples, 0, count)
            buffer.limit(bytes)
            active.write(buffer)
        }

        fun finish(empty: String) {
            val active = sink ?: throw Codecs.error(empty)
            task.throwIfCancelled()
            active.finish()
            finished = true
        }

        override fun close() {
            if (!finished) sink?.close()
        }

    }

    /** One input of [mix] and its unread samples. zh-CN: [mix] 的一个输入及其未读样本. */
    private class MixInput(val source: DecodedAudio, val volume: Float, val offsetFrames: Long, private val channels: Int) {

        private var chunk: ShortArray? = null

        private var index = 0

        var exhausted = false
            private set

        /**
         * Up to [frames] frames, handed to [use] as (samples, first sample, frame count); 0 at the end of the input.
         * zh-CN: 至多 [frames] 帧, 以 (样本, 首样本下标, 帧数) 交给 [use]; 输入结束时返回 0.
         */
        fun take(frames: Int, use: (ShortArray, Int, Int) -> Unit): Int {
            var current = chunk
            while (current == null || current.size - index < channels) {
                if (exhausted) return 0
                current = source.read()
                if (current == null) {
                    exhausted = true
                    chunk = null
                    return 0
                }
                chunk = current
                index = 0
            }
            val count = minOf(frames, (current.size - index) / channels)
            use(current, index, count)
            index += count * channels
            return count
        }

    }

    companion object {

        /** Share of the progress for the first pass of `audioVolume`. zh-CN: `audioVolume` 第一遍读取所占的进度份额. */
        const val ANALYSIS_SHARE = 0.4

        /** Frames per mix block. zh-CN: 每个混音块的帧数. */
        const val MIX_FRAMES = 4_096

        private const val INITIAL_BUFFER_BYTES = 64 * 1024

    }

}

/**
 * Pull decoder of the first audio track of [path]: [read] returns interleaved 16-bit PCM in the [target] format (chosen
 * by [targetOf] once the decoder reports its output), cut to source time [startUs, endUs) by counting frames from the
 * first decoded timestamp, run through Sonic at [speed] / [pitch]; null at the end.
 * zh-CN: [path] 第一条音轨的拉取式解码器: [read] 返回 [target] 格式的交错 16 位 PCM (解码器报告输出格式后由 [targetOf] 选定),
 * 从第一个解码时间戳起按帧计数裁到源时间 [startUs, endUs), 再按 [speed] / [pitch] 经过 Sonic; 结束时返回 null.
 */
internal class DecodedAudio(
    private val task: TaskRunner.JobContext,
    private val path: String,
    private val startUs: Long = 0L,
    private val endUs: Long? = null,
    private val speed: Float = 1f,
    private val pitch: Float = 1f,
    private val targetOf: (PcmFormat) -> AudioTargets.Target,
) : Closeable {

    private val extractor = MediaExtractor()

    private val decoder: MediaCodec = try {
        openDecoder()
    } catch (e: Throwable) {
        runCatching { extractor.release() }
        throw e
    }

    private val info = MediaCodec.BufferInfo()

    private val queue = ArrayDeque<ShortArray>()

    private val collect: (ByteBuffer) -> Unit = { buffer -> queue.addLast(buffer.toShorts()) }

    private var scratch: ByteBuffer = ByteBuffer.allocateDirect(INITIAL_SCRATCH_BYTES).order(ByteOrder.nativeOrder())

    private var converter: PcmConverter? = null

    private var inputDone = false

    private var windowDone = false

    private var ended = false

    private var released = false

    private var nextFrame: Long? = null

    private var startFrame = 0L

    private var endFrame = Long.MAX_VALUE

    /** The decoder output format. zh-CN: 解码器输出格式. */
    var source: PcmFormat? = null
        private set

    /** The format of [read], known after the first samples. zh-CN: [read] 的格式, 读到第一批样本后可知. */
    var target: AudioTargets.Target? = null
        private set

    /** Source time of the last decoded buffer. zh-CN: 最后一个解码缓冲区的源时间. */
    var positionUs: Long = startUs
        private set

    fun read(): ShortArray? {
        while (true) {
            queue.removeFirstOrNull()?.let { return it }
            if (ended) return null
            step()
        }
    }

    override fun close() {
        if (released) return
        released = true
        runCatching { decoder.stop() }
        runCatching { decoder.release() }
        runCatching { extractor.release() }
    }

    private fun openDecoder(): MediaCodec {
        try {
            extractor.setDataSource(path)
        } catch (e: Exception) {
            throw MediaStudioException.unsupported("cannot read media from $path (${e.message ?: e.javaClass.simpleName})")
        }
        val track = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: throw MediaStudioException.unsupported("no audio track in $path")
        extractor.selectTrack(track)
        // A little earlier, so the decoder has settled at the cut. zh-CN: 略早一些, 让解码器在切点前稳定下来.
        if (startUs > 0) extractor.seekTo((startUs - PREROLL_US).coerceAtLeast(0L), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val format = extractor.getTrackFormat(track)
        return Codecs.decoder(format.getString(MediaFormat.KEY_MIME)!!, format)
    }

    private fun step() {
        task.throwIfCancelled()
        if (!inputDone) {
            val index = decoder.dequeueInputBuffer(Codecs.TIMEOUT_US)
            if (index >= 0) {
                val buffer = decoder.getInputBuffer(index) ?: throw Codecs.error("the decoder gave no input buffer")
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) {
                    decoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    inputDone = true
                } else {
                    decoder.queueInputBuffer(index, 0, size, extractor.sampleTime.coerceAtLeast(0L), 0)
                    extractor.advance()
                }
            }
        }
        val index = decoder.dequeueOutputBuffer(info, Codecs.TIMEOUT_US)
        when {
            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                val changed = PcmFormat.of(decoder.outputFormat)
                if (converter != null && changed != source) {
                    throw MediaStudioException.unsupported("the audio format of $path changed while decoding ($source -> $changed)")
                }
                source = changed
            }
            index >= 0 -> {
                val last = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                if (info.size > 0 && !windowDone) consume(index)
                decoder.releaseOutputBuffer(index, false)
                if (last || windowDone) end()
            }
        }
    }

    private fun consume(index: Int) {
        val format = source ?: PcmFormat.of(decoder.outputFormat).also { source = it }
        val active = converter ?: open(format)
        val buffer = decoder.getOutputBuffer(index) ?: throw Codecs.error("the decoder gave no output buffer")
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        val needed = Pcm16.bytesFor(info.size, format.encoding)
        if (scratch.capacity() < needed) scratch = ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder())
        scratch.clear()
        Pcm16.convert(buffer, format.encoding, scratch)
        scratch.flip()
        val frameBytes = format.channels * 2
        val frames = scratch.remaining() / frameBytes
        val first = nextFrame ?: Math.round(info.presentationTimeUs * format.sampleRate / 1_000_000.0)
        nextFrame = first + frames
        positionUs = info.presentationTimeUs
        val from = maxOf(first, startFrame)
        val until = minOf(first + frames, endFrame)
        if (until > from) {
            scratch.position(((from - first) * frameBytes).toInt())
            scratch.limit(((until - first) * frameBytes).toInt())
            active.write(scratch, collect)
        }
        if (first + frames >= endFrame) windowDone = true
    }

    private fun open(format: PcmFormat): PcmConverter {
        val chosen = targetOf(format)
        target = chosen
        startFrame = Math.round(startUs * format.sampleRate / 1_000_000.0)
        endFrame = endUs?.let { Math.round(it * format.sampleRate / 1_000_000.0) } ?: Long.MAX_VALUE
        return PcmConverter(format, chosen.sampleRate, chosen.channels, speed, pitch).also { converter = it }
    }

    private fun end() {
        converter?.finish(collect)
        ended = true
    }

    private fun ByteBuffer.toShorts(): ShortArray {
        val shorts = ShortArray(remaining() / 2)
        order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        position(limit())
        return shorts
    }

    private companion object {

        const val PREROLL_US = 100_000L

        const val INITIAL_SCRATCH_BYTES = 64 * 1024

    }

}

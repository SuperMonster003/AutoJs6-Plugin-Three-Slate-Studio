// Copy of the AutoJs6 host kernel file app/src/main/java/org/autojs/autojs/core/record/AudioRecorder.kt at host commit
// 3ef5cec633 (roadmap P1.2; unchanged up to host 063b9f0b8a), kept by 3-Slate Studio roadmap D41 / P4.5; see README.md.
// Plugin changes, each marked "Plugin:" below: the plugin's own [MediaProjection] replaces the host ScreenCapturer;
// pause / resume (the capture keeps reading and drops the blocks while paused, so the recorded frames, the AAC
// presentation times and maxDuration skip the pause); the level of the last block for the level meter; the
// permission message names 3-Slate Studio; the saved file is moved with File.renameTo below API 26 (the plugin's
// minimum is API 24 without library desugaring).
package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import java.io.File
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Snapshot of an [AudioRecorder]. [recordedMillis] counts captured frames, [elapsedMillis] wall time since start.
 *
 * zh-CN: [AudioRecorder] 的状态快照. [recordedMillis] 按已采集帧数计, [elapsedMillis] 为开始以来的挂钟时间.
 */
data class AudioRecordingStatus(
    val id: String,
    val state: String,
    val path: String,
    val format: AudioOutputFormat,
    val track: AudioTrackOptions,
    val elapsedMillis: Long,
    val recordedMillis: Long,
    val maxDurationMillis: Long,
    val error: String?,
)

/**
 * Saved audio recording. [durationMillis] counts recorded frames (exact for WAV, the AAC priming is not included),
 * [elapsedMillis] is the wall time from start to stop.
 *
 * zh-CN: 已保存的录音. [durationMillis] 按已录帧数计 (WAV 精确, 不含 AAC 预读), [elapsedMillis] 为开始到停止的挂钟时间.
 */
data class AudioRecordingResult(
    val id: String,
    val path: String,
    val reason: String,
    val format: AudioOutputFormat,
    val track: AudioTrackOptions,
    val sizeBytes: Long,
    val durationMillis: Long,
    val elapsedMillis: Long,
) {
    val mimeType: String get() = format.mimeType
}

/**
 * Audio-only recording of the microphone, of other apps' playback (Android 10+, through [projection]) or
 * of both mixed, saved as M4A (AAC) or WAV (PCM16). The file is written next to [output] under a temporary name and moved
 * into place when the recording is saved; a cancelled or failed recording leaves no file. Every method is thread-safe.
 *
 * - [start] opens the sources and returns once audio flows; microphone sources hold [lease] while capturing.
 * - [stop] saves the file; after an automatic stop ([AudioRecordingOptions.maxDurationMillis], the WAV size limit, the
 *   lease's stop action) it returns the stored result. [cancel] discards the file.
 * - [onStop] is called once, whichever way the recording ends.
 *
 * Both modes need RECORD_AUDIO; playback capture obeys each app's capture policy (silence when an app forbids it). The
 * 3-Slate Studio plugin keeps a copy of this kernel (3-Slate Studio roadmap D41): keep the copy in sync when this changes.
 *
 * zh-CN: 纯录音: 麦克风, 其他应用的播放声音 (内录, Android 10+, 经 [projection]) 或两者混合, 保存为 M4A (AAC)
 * 或 WAV (PCM16). 文件先以临时名写在 [output] 旁边, 保存时再移到目标位置; 取消或失败的录音不留文件. 所有方法线程安全.
 *
 * - [start] 打开音源, 音频开始流动后返回; 麦克风来源在采集期间持有 [lease].
 * - [stop] 保存文件; 自动停止 ([AudioRecordingOptions.maxDurationMillis], WAV 大小上限, 租约的停止按钮) 之后返回已保存的结果.
 *   [cancel] 丢弃文件.
 * - 无论以哪种方式结束, [onStop] 都只调用一次.
 *
 * 两种方式都需要 RECORD_AUDIO; 内录遵守各应用的捕获策略 (应用禁止时为静音). 3-Slate Studio 插件保留这份内核的副本
 * (3-Slate Studio 路线图 D41): 修改时同步副本.
 */
class AudioRecorder(
    private val context: Context,
    private val output: File,
    val options: AudioRecordingOptions,
    // Plugin: the plugin's own projection (roadmap D41) instead of the host's screen capture session.
    private val projection: MediaProjection? = null,
    private val lease: RecordingForegroundLease? = null,
    private val onStop: RecordingStopListener<AudioRecordingResult>? = null,
) : AutoCloseable {

    val id = "recording-${UUID.randomUUID()}"

    private val lock = Any()
    private val autoStopping = AtomicBoolean(false)
    private var state = RecordingStates.IDLE
    private var capture: AudioCapture? = null
    private var muxer: RecordingMuxer? = null
    private var temporary: File? = null
    private var startNs = 0L
    private var endNs = 0L
    private var recordedFrames = 0L
    private var result: AudioRecordingResult? = null
    private var failure: Throwable? = null

    init {
        RecordingOptions.checkAudioOutput(output, options.overwrite)
    }

    fun start(): AudioRecordingStatus = synchronized(lock) {
        check(state == RecordingStates.IDLE) { "This audio recording was already started." }
        val parent = requireNotNull(output.absoluteFile.parentFile) { "Recording output requires a parent directory." }
        require(parent.isDirectory || parent.mkdirs()) { "Cannot create recording output directory." }
        try {
            val temp = File.createTempFile(".autojs6-recording-", ".partial", parent).also { temporary = it }
            val sink = when (options.format) {
                AudioOutputFormat.M4A -> AacSink(options.track, RecordingMuxer(temp, hasVideo = false, hasAudio = true).also { muxer = it })
                AudioOutputFormat.WAV -> WavSink(temp, options.track)
            }
            startNs = System.nanoTime()
            AudioCapture(
                context = context,
                projection = projection,
                track = options.track,
                id = id,
                sink = sink,
                originNs = null,
                maxFrames = RecordingOptions.maxFrames(options.maxDurationMillis, options.track.sampleRate),
                lease = lease,
                threadName = "AudioRecording-${options.format.key.uppercase()}",
                onLeaseStop = { reason -> endLater(reason, null) },
                onEnd = { reason, error ->
                    when {
                        error != null -> endLater(RecordingStopReasons.FAILED, error)
                        reason != null -> endLater(reason, null)
                    }
                },
            ).also { capture = it }.start()
            state = RecordingStates.RECORDING
            status()
        } catch (error: Throwable) {
            state = RecordingStates.FAILED
            failure = error
            endNs = System.nanoTime()
            cleanup()
            throw error
        }
    }

    fun status(): AudioRecordingStatus = synchronized(lock) {
        val now = if (state == RecordingStates.RECORDING || state == RecordingStates.PAUSED) System.nanoTime() else endNs
        val active = capture
        AudioRecordingStatus(
            id = id,
            state = state,
            path = output.absolutePath,
            format = options.format,
            track = options.track,
            elapsedMillis = if (startNs == 0L) 0 else ((now - startNs) / 1_000_000).coerceAtLeast(0),
            recordedMillis = RecordingOptions.framesToMillis(active?.frames ?: recordedFrames, options.track.sampleRate),
            maxDurationMillis = options.maxDurationMillis,
            error = (active?.failure ?: failure)?.let { it.message ?: it.javaClass.name },
        )
    }

    /** Saves the recording, or returns the stored result of an earlier stop. zh-CN: 保存录音, 或返回之前停止时保存的结果. */
    fun stop(): AudioRecordingResult {
        end(RecordingStopReasons.STOPPED, null, save = true)
        synchronized(lock) {
            result?.let { return it }
            failure?.let { throw it }
            throw IllegalStateException(if (state == RecordingStates.IDLE) "Audio recording has not started." else "Audio recording was cancelled.")
        }
    }

    /** Discards the recording; false when it had already ended or never started. zh-CN: 丢弃录音; 已结束或未开始时返回 false. */
    fun cancel(): Boolean = end(RecordingStopReasons.CANCELLED, null, save = false)

    /**
     * Plugin: pauses the recording; false when it is not recording. The sources stay open and the blocks read meanwhile
     * are dropped, so the saved file and the recorded duration skip the pause.
     * zh-CN: 插件新增: 暂停录音; 不在录音时返回 false. 音源保持打开, 期间读到的数据块被丢弃, 保存的文件与已录时长都跳过暂停.
     */
    fun pause(): Boolean = synchronized(lock) {
        if (state != RecordingStates.RECORDING) return false
        checkNotNull(capture).paused = true
        state = RecordingStates.PAUSED
        true
    }

    /** Plugin: resumes a paused recording; false when it is not paused. zh-CN: 插件新增: 恢复暂停的录音; 未暂停时返回 false. */
    fun resume(): Boolean = synchronized(lock) {
        if (state != RecordingStates.PAUSED) return false
        checkNotNull(capture).paused = false
        state = RecordingStates.RECORDING
        true
    }

    /** Plugin: peak of the last block read (0 - 1), also while paused. zh-CN: 插件新增: 最近读到的数据块的峰值 (0 - 1), 暂停时也更新. */
    fun level(): Float = synchronized(lock) { capture?.level ?: 0f }

    override fun close() {
        cancel()
    }

    private fun end(reason: String, cause: Throwable?, save: Boolean): Boolean {
        val saved: AudioRecordingResult?
        val error: Throwable?
        synchronized(lock) {
            if (state != RecordingStates.RECORDING && state != RecordingStates.PAUSED) return false
            val active = checkNotNull(capture)
            var file: AudioRecordingResult? = null
            var problem = cause
            try {
                if (save && problem == null) {
                    active.finish()
                    recordedFrames = active.frames
                    endNs = System.nanoTime()
                    file = saveFile(reason)
                } else {
                    runCatching { active.close() }
                    recordedFrames = active.frames
                }
            } catch (e: Throwable) {
                problem = e
            } finally {
                if (endNs == 0L || file == null) endNs = System.nanoTime()
                cleanup()
            }
            state = when {
                file != null -> RecordingStates.STOPPED
                problem != null -> RecordingStates.FAILED
                else -> RecordingStates.CANCELLED
            }
            result = file
            failure = problem
            saved = file
            error = problem
        }
        try {
            onStop?.onStop(reason, saved, error)
        } catch (e: Throwable) {
            Log.w(TAG, "Audio recording stop listener failed.", e)
        }
        return true
    }

    /**
     * Plugin: [Files.move] from API 26, [File.renameTo] before (both on the same volume as the temporary file).
     * zh-CN: 插件新增: API 26 起用 [Files.move], 之前用 [File.renameTo] (临时文件与输出在同一卷).
     */
    private fun moveRecording(temp: File, target: File, overwrite: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (overwrite) Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            // No REPLACE_EXISTING: preserve files created by another caller while recording.
            else Files.move(temp.toPath(), target.toPath())
            return
        }
        if (target.exists()) {
            if (!overwrite) throw java.io.IOException("${target.path} was created while recording.")
            if (!target.delete()) throw java.io.IOException("Cannot replace ${target.path}.")
        }
        if (!temp.renameTo(target)) throw java.io.IOException("Cannot move the recording to ${target.path}.")
    }

    private fun saveFile(reason: String): AudioRecordingResult {
        muxer?.let { it.finish(); muxer = null }
        val temp = checkNotNull(temporary)
        check(temp.length() > 0) { "Audio recording produced no data." }
        moveRecording(temp, output, options.overwrite)
        temporary = null
        return AudioRecordingResult(
            id = id,
            path = output.absolutePath,
            reason = reason,
            format = options.format,
            track = options.track,
            sizeBytes = output.length(),
            durationMillis = RecordingOptions.framesToMillis(recordedFrames, options.track.sampleRate),
            elapsedMillis = ((endNs - startNs) / 1_000_000).coerceAtLeast(0),
        )
    }

    private fun cleanup() {
        runCatching { capture?.close() }
        capture = null
        runCatching { muxer?.close() }
        muxer = null
        temporary?.delete()
        temporary = null
    }

    private fun endLater(reason: String, error: Throwable?) {
        if (!autoStopping.compareAndSet(false, true)) return
        Thread({ end(reason, error, save = error == null) }, "AudioRecording-stop").start()
    }

    private companion object {
        const val TAG = "AudioRecorder"
    }
}

/**
 * Destination of captured PCM16: AAC into a [RecordingMuxer] track ([AacSink]) or a WAV file ([WavSink]).
 *
 * zh-CN: 采集到的 PCM16 的去向: 写入 [RecordingMuxer] 音轨的 AAC ([AacSink]) 或 WAV 文件 ([WavSink]).
 */
internal interface PcmSink : AutoCloseable {

    /** Completes once the sink accepts audio (AAC: the encoder published its format). zh-CN: 可以接收音频时完成 (AAC: 编码器给出格式后). */
    val ready: CompletableFuture<Unit>

    fun start()

    fun hasRoomFor(sampleCount: Int): Boolean = true

    /**
     * Queues [count] interleaved samples presented at [ptsUs]; false when [active] turned false while waiting for the
     * encoder.
     *
     * zh-CN: 写入 [count] 个交错采样, 呈现时间 [ptsUs]; 等待编码器期间 [active] 变为 false 时返回 false.
     */
    fun write(samples: ShortArray, count: Int, ptsUs: Long, active: () -> Boolean): Boolean

    /** Ends the stream (AAC: end of stream and the last drain; WAV: final header). zh-CN: 结束流 (AAC: 流结束标记与最后一次取出; WAV: 最终文件头). */
    fun finish()

    fun fail(error: Throwable) {
        ready.completeExceptionally(error)
    }
}

/** AAC-LC encoding into one muxer track. zh-CN: AAC-LC 编码, 写入复用器的一条音轨. */
internal class AacSink(private val track: AudioTrackOptions, private val muxer: RecordingMuxer) : PcmSink {
    override val ready = CompletableFuture<Unit>()
    private var codec: MediaCodec? = null
    private val info = MediaCodec.BufferInfo()
    private var trackIndex = -1
    private var lastPts = -1L

    override fun start() {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, track.sampleRate, track.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, track.bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxOf(8192, AudioCapture.FRAMES_PER_READ * track.channels * 2))
        }
        val name = MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format)
        val encoder = if (name != null) MediaCodec.createByCodecName(name) else MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec = encoder
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (error: RuntimeException) {
            throw IllegalArgumentException("No AAC encoder supports ${track.sampleRate} Hz with ${track.channels} channel(s) at ${track.bitRate} bit/s.", error)
        }
        encoder.start()
    }

    override fun write(samples: ShortArray, count: Int, ptsUs: Long, active: () -> Boolean): Boolean {
        val encoder = checkNotNull(codec)
        var offset = 0
        var pts = ptsUs
        while (offset < count) {
            var input = encoder.dequeueInputBuffer(10_000)
            while (input < 0 && active()) {
                drain(false)
                input = encoder.dequeueInputBuffer(10_000)
            }
            if (input < 0) return false
            val buffer = checkNotNull(encoder.getInputBuffer(input))
            buffer.clear()
            val size = minOf(count - offset, buffer.capacity() / 2 / track.channels * track.channels)
            check(size > 0) { "AAC encoder input buffer is too small." }
            buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples, offset, size)
            val at = maxOf(0L, pts, lastPts + 1)
            encoder.queueInputBuffer(input, 0, size * 2, at, 0)
            lastPts = at
            offset += size
            pts = at + Pcm16.framesToMicros((size / track.channels).toLong(), track.sampleRate)
            drain(false)
        }
        return true
    }

    override fun finish() {
        val encoder = checkNotNull(codec)
        val input = encoder.dequeueInputBuffer(1_000_000)
        check(input >= 0) { "AAC encoder cannot accept end of stream." }
        val endPts = maxOf(0L, lastPts + Pcm16.framesToMicros(AudioCapture.FRAMES_PER_READ.toLong(), track.sampleRate))
        encoder.queueInputBuffer(input, 0, 0, endPts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        drain(true)
    }

    private fun drain(waitForEos: Boolean): Boolean {
        val encoder = checkNotNull(codec)
        val deadline = System.nanoTime() + 2_000_000_000L
        while (true) {
            val index = encoder.dequeueOutputBuffer(info, if (waitForEos) 10_000 else 0)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    ready.complete(Unit)
                }
                index >= 0 -> {
                    try {
                        if (info.size > 0) {
                            check(trackIndex >= 0)
                            muxer.write(trackIndex, checkNotNull(encoder.getOutputBuffer(index)), info, false)
                        }
                    } finally {
                        encoder.releaseOutputBuffer(index, false)
                    }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return true
                }
                !waitForEos -> return false
            }
            if (waitForEos) check(System.nanoTime() < deadline) { "AAC encoder did not finish." }
        }
    }

    override fun close() {
        val encoder = codec ?: return
        codec = null
        runCatching { encoder.stop() }
        runCatching { encoder.release() }
    }
}

/** PCM16 into a WAV file; full at [WavFileWriter.MAX_DATA_BYTES]. zh-CN: PCM16 写入 WAV 文件; 到 [WavFileWriter.MAX_DATA_BYTES] 为满. */
internal class WavSink(private val file: File, private val track: AudioTrackOptions) : PcmSink {
    override val ready = CompletableFuture<Unit>()
    private var writer: WavFileWriter? = null

    override fun start() {
        writer = WavFileWriter(file, track.sampleRate, track.channels)
        ready.complete(Unit)
    }

    override fun hasRoomFor(sampleCount: Int): Boolean = checkNotNull(writer).hasRoomFor(sampleCount)

    override fun write(samples: ShortArray, count: Int, ptsUs: Long, active: () -> Boolean): Boolean {
        checkNotNull(writer).write(samples, count)
        return true
    }

    override fun finish() {
        checkNotNull(writer).finish()
    }

    override fun close() {
        writer?.close()
        writer = null
    }
}

/**
 * PCM capture of one [track] into a [sink] on its own thread. Playback uses [projection]; a two-source (mixed)
 * track averages both. With [originNs] the presentation times follow the AudioRecord monotonic timestamps relative to
 * that origin (shared with the video of a screen recording), otherwise the captured frame count. Capture stops after
 * [maxFrames] frames or when the sink is full and then reports [RecordingStopReasons.MAX_DURATION] /
 * [RecordingStopReasons.MAX_SIZE] through [onEnd]; a failure is reported there too.
 *
 * zh-CN: 在独立线程上把一条音轨 [track] 采集到 [sink]. 内录使用 [projection]; 两路来源 (混合) 取平均. 给出 [originNs] 时,
 * 呈现时间按 AudioRecord 的单调时间戳相对该原点计算 (与录屏视频共用), 否则按已采集帧数计算. 采集满 [maxFrames] 帧或 sink
 * 已满时停止, 并经 [onEnd] 报告 [RecordingStopReasons.MAX_DURATION] / [RecordingStopReasons.MAX_SIZE]; 失败也经它报告.
 */
internal class AudioCapture(
    private val context: Context,
    private val projection: MediaProjection?,
    private val track: AudioTrackOptions,
    private val id: String,
    private val sink: PcmSink,
    private val originNs: Long?,
    private val maxFrames: Long,
    private val lease: RecordingForegroundLease?,
    private val threadName: String,
    private val onLeaseStop: (String) -> Unit,
    private val onEnd: (reason: String?, error: Throwable?) -> Unit,
) : AutoCloseable {

    private class Input(val record: AudioRecord, val channels: Int)

    private val inputs = ArrayList<Input>()
    private var thread: Thread? = null
    private var leased = false
    @Volatile private var running = false

    @Volatile var failure: Throwable? = null
        private set

    /** Plugin: true drops the blocks read (pause). zh-CN: 插件新增: 为 true 时丢弃读到的数据块 (暂停). */
    @Volatile var paused = false

    /** Plugin: peak of the last block (0 - 1). zh-CN: 插件新增: 最近一个数据块的峰值 (0 - 1). */
    @Volatile var level = 0f
        private set

    /** Frames captured so far. zh-CN: 已采集的帧数. */
    @Volatile var frames = 0L
        private set

    fun start() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Grant 3-Slate Studio microphone (RECORD_AUDIO) permission before recording audio, including playback capture.")
        }
        if (track.source.usesPlayback && Build.VERSION.SDK_INT < 29) throw IllegalArgumentException(PLAYBACK_API_MESSAGE)
        if (track.source.usesPlayback) requireNotNull(projection) { "Playback audio capture requires a media projection." }
        try {
            if (track.source.usesMicrophone && lease != null) {
                val ready = CompletableFuture<Unit>()
                leased = true
                lease.acquire(context, id,
                    onReady = { it.fold({ ready.complete(Unit) }, { error -> ready.completeExceptionally(error) }) },
                    onStop = { reason -> onLeaseStop(reason) })
                await(ready)
            }
            if (track.source.usesPlayback) inputs.add(open(playback = true))
            if (track.source.usesMicrophone) inputs.add(open(playback = false))
            sink.start()
            inputs.forEach {
                it.record.startRecording()
                check(it.record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Android did not start audio capture." }
            }
            running = true
            thread = Thread(::run, threadName).also { it.start() }
            await(sink.ready)
        } catch (error: Throwable) {
            close()
            throw error
        }
    }

    /** Opens a source with the track's channel count, falling back to mono (duplicated on output). zh-CN: 按音轨声道数打开音源, 不支持时退回单声道 (输出时复制). */
    private fun open(playback: Boolean): Input {
        if (track.channels == 2) {
            val stereo = runCatching { build(playback, AudioFormat.CHANNEL_IN_STEREO) }.getOrNull()
            if (stereo != null && stereo.state == AudioRecord.STATE_INITIALIZED) return Input(stereo, 2)
            stereo?.release()
        }
        val mono = build(playback, AudioFormat.CHANNEL_IN_MONO)
        if (mono.state != AudioRecord.STATE_INITIALIZED) {
            mono.release()
            throw IllegalStateException("Android could not initialize the requested audio source.")
        }
        return Input(mono, 1)
    }

    @SuppressLint("MissingPermission") // start() checks RECORD_AUDIO before opening any source.
    private fun build(playback: Boolean, channelMask: Int): AudioRecord {
        val channels = if (channelMask == AudioFormat.CHANNEL_IN_STEREO) 2 else 1
        val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(track.sampleRate).setChannelMask(channelMask).build()
        val bufferBytes = maxOf(AudioRecord.getMinBufferSize(track.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT), 8192 * channels)
        val builder = AudioRecord.Builder().setAudioFormat(format).setBufferSizeInBytes(bufferBytes)
        if (!playback) return builder.setAudioSource(MediaRecorder.AudioSource.MIC).build()
        if (Build.VERSION.SDK_INT < 29) throw IllegalArgumentException(PLAYBACK_API_MESSAGE)
        val capture = AudioPlaybackCaptureConfiguration.Builder(checkNotNull(projection))
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA).addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build()
        return builder.setAudioPlaybackCaptureConfig(capture).build()
    }

    private fun run() {
        val block = FRAMES_PER_READ * track.channels
        val primary = ShortArray(block)
        val secondary = ShortArray(block)
        val mono = ShortArray(FRAMES_PER_READ)
        val timestamp = AudioTimestamp()
        var reason: String? = null
        var error: Throwable? = null
        try {
            while (running) {
                val first = inputs[0]
                if (!readFrames(first, primary, mono)) break
                val clockNs = if (originNs != null) clock(first.record, timestamp) else 0L
                if (inputs.size == 2) {
                    if (!readFrames(inputs[1], secondary, mono)) break
                    Pcm16.mixInto(primary, secondary, block)
                }
                // Plugin: the level meter and the pause.
                level = Pcm16.peak(primary, block)
                if (paused) continue
                val count = minOf(FRAMES_PER_READ.toLong(), maxFrames - frames).toInt()
                if (!sink.hasRoomFor(count * track.channels)) {
                    reason = RecordingStopReasons.MAX_SIZE
                    break
                }
                val pts = if (originNs != null) maxOf(0L, (clockNs - originNs) / 1000) else Pcm16.framesToMicros(frames, track.sampleRate)
                if (!sink.write(primary, count * track.channels, pts) { running }) break
                frames += count
                if (frames >= maxFrames) {
                    reason = RecordingStopReasons.MAX_DURATION
                    break
                }
            }
            sink.finish()
        } catch (e: Throwable) {
            error = e
            failure = e
            sink.fail(e)
        } finally {
            running = false
            inputs.forEach { runCatching { it.record.stop() }; runCatching { it.record.release() } }
            runCatching { sink.close() }
            releaseLease()
        }
        onEnd(reason, error)
    }

    /** Capture time of the first frame of the block just read. zh-CN: 刚读出的这一块首帧的采集时间. */
    private fun clock(record: AudioRecord, timestamp: AudioTimestamp): Long {
        val now = System.nanoTime()
        return if (record.getTimestamp(timestamp, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS && timestamp.nanoTime > 0)
            timestamp.nanoTime - (timestamp.framePosition - frames) * 1_000_000_000L / track.sampleRate
        else now - FRAMES_PER_READ * 1_000_000_000L / track.sampleRate
    }

    private fun readFrames(input: Input, out: ShortArray, mono: ShortArray): Boolean {
        if (input.channels == track.channels) return read(input.record, out, out.size)
        if (!read(input.record, mono, FRAMES_PER_READ)) return false
        Pcm16.monoToStereo(mono, out, FRAMES_PER_READ)
        return true
    }

    private fun read(record: AudioRecord, buffer: ShortArray, size: Int): Boolean {
        var offset = 0
        while (offset < size && running) {
            val count = record.read(buffer, offset, size - offset, AudioRecord.READ_BLOCKING)
            if (!running) return false
            check(count > 0) { "Android audio capture read failed: $count" }
            offset += count
        }
        return running
    }

    /** Stops the capture and rethrows its failure. zh-CN: 停止采集并重新抛出采集失败. */
    fun finish() {
        close()
        failure?.let { throw it }
    }

    override fun close() {
        running = false
        inputs.forEach { runCatching { it.record.stop() } }
        val worker = thread
        if (worker != null) {
            if (worker !== Thread.currentThread()) {
                worker.join(3000)
                check(!worker.isAlive) { "Audio recording cleanup timed out." }
            }
        } else {
            inputs.forEach { runCatching { it.record.release() } }
            runCatching { sink.close() }
        }
        releaseLease()
    }

    @Synchronized private fun releaseLease() {
        if (!leased) return
        leased = false
        lease?.release(id)
    }

    private fun await(future: CompletableFuture<Unit>) {
        try {
            future.get(3, TimeUnit.SECONDS)
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        }
    }

    companion object {
        const val FRAMES_PER_READ = 1024
        const val PLAYBACK_API_MESSAGE = "Playback audio capture requires Android 10 or later."
    }
}

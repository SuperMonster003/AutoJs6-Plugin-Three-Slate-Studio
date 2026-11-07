// Copy of the AutoJs6 host kernel file app/src/main/java/org/autojs/autojs/core/record/RecordingOptions.kt at host commit
// 3ef5cec633 (roadmap P1.2; unchanged up to host 063b9f0b8a), kept by 3-Slate Studio roadmap D41 / P4.5; see README.md.
// Plugin changes: only the audio parts (ScreenRecordingOptions and the video ranges come with roadmap P7, from the same
// file); [RecordingStates.PAUSED] for the pause of the Studio recorder.
package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import java.io.File

/**
 * Audio source of a recording. [key] is the option value of the script APIs (Node `audio`; the Rhino module maps its own
 * `source` values onto these).
 *
 * zh-CN: 录制的音频来源. [key] 是脚本 API 的选项值 (Node 的 `audio`; Rhino 模块把自己的 `source` 取值映射到这里).
 */
enum class RecordingAudioSource(val key: String) {
    NONE("none"),
    MICROPHONE("microphone"),
    PLAYBACK("playback"),
    MIXED("mixed");

    val usesMicrophone: Boolean get() = this == MICROPHONE || this == MIXED

    /** Playback capture needs a MediaProjection and Android 10 or later. zh-CN: 内录需要 MediaProjection 与 Android 10 及以上. */
    val usesPlayback: Boolean get() = this == PLAYBACK || this == MIXED

    companion object {
        fun of(key: String?): RecordingAudioSource? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Container of an audio-only recording: [M4A] is AAC in MPEG-4, [WAV] is PCM16 little-endian (RIFF, at most 4 GiB of data).
 *
 * zh-CN: 纯录音的容器: [M4A] 为 MPEG-4 封装的 AAC, [WAV] 为 PCM16 小端 (RIFF, 数据最多 4 GiB).
 */
enum class AudioOutputFormat(val key: String, val mimeType: String) {
    M4A("m4a", "audio/mp4"),
    WAV("wav", "audio/wav");

    companion object {
        fun of(key: String?): AudioOutputFormat? = entries.firstOrNull { it.key == key }
    }
}

/**
 * One audio track: source, sample rate (Hz), channel count (1 or 2) and AAC bit rate (bit/s; ignored by WAV output).
 * A stereo microphone track falls back to duplicated mono when the device has no stereo microphone input.
 *
 * zh-CN: 一条音轨: 来源, 采样率 (Hz), 声道数 (1 或 2) 与 AAC 码率 (bit/s; WAV 输出忽略). 设备没有立体声麦克风输入时,
 * 立体声麦克风音轨退回单声道并复制到两个声道.
 */
data class AudioTrackOptions(
    val source: RecordingAudioSource,
    val sampleRate: Int,
    val channels: Int,
    val bitRate: Int,
) {
    init {
        RecordingOptions.validate(this)
    }
}

/**
 * Options of [AudioRecorder]: the [track], the output [format], [maxDurationMillis] (0 = no limit, counted in recorded
 * frames) and whether an existing output file is replaced ([overwrite]).
 *
 * zh-CN: [AudioRecorder] 的选项: 音轨 [track], 输出格式 [format], [maxDurationMillis] (0 为不限, 按已录帧数计),
 * 以及是否替换已存在的输出文件 ([overwrite]).
 */
data class AudioRecordingOptions(
    val track: AudioTrackOptions,
    val format: AudioOutputFormat = AudioOutputFormat.M4A,
    val maxDurationMillis: Long = 0,
    val overwrite: Boolean = false,
) {
    init {
        RecordingOptions.validateMaxDuration(maxDurationMillis)
    }
}

/**
 * Ranges, defaults and checks shared by the recording kernel and its script adapters. The messages name the option keys
 * of the script APIs.
 *
 * zh-CN: 录制内核及其脚本适配层共用的范围, 默认值与校验. 消息里使用脚本 API 的选项名.
 */
object RecordingOptions {

    val SAMPLE_RATES: IntRange = 8000..192_000
    val CHANNELS: IntRange = 1..2
    val AUDIO_BIT_RATES: IntRange = 16_000..320_000

    /** Upper bound of a max duration: 24 hours. zh-CN: 最长时长的上限: 24 小时. */
    const val MAX_DURATION_LIMIT_MILLIS = 24L * 60 * 60 * 1000

    const val DEFAULT_SAMPLE_RATE = 48_000

    /** Playback and mixed tracks default to stereo AAC at 192 kbit/s (3-Slate Studio Q1 (a)). zh-CN: 内录与混合默认立体声 AAC 192 kbit/s (3-Slate Studio Q1 (a)). */
    const val DEFAULT_PLAYBACK_CHANNELS = 2
    const val DEFAULT_PLAYBACK_BIT_RATE = 192_000

    /** Microphone tracks default to mono AAC at 128 kbit/s. zh-CN: 麦克风默认单声道 AAC 128 kbit/s. */
    const val DEFAULT_MICROPHONE_CHANNELS = 1
    const val DEFAULT_MICROPHONE_BIT_RATE = 128_000

    const val CHANNELS_MESSAGE = "channels must be 1 or 2."

    /**
     * Default track of [source] for the Rhino module and the plugin: 48 kHz; playback and mixed stereo at 192 kbit/s,
     * microphone mono at 128 kbit/s. The Node bridge keeps its own mono defaults.
     *
     * zh-CN: Rhino 模块与插件使用的 [source] 默认音轨: 48 kHz; 内录与混合为立体声 192 kbit/s, 麦克风为单声道 128 kbit/s.
     * Node 桥接保留自己的单声道默认值.
     */
    fun defaultTrack(source: RecordingAudioSource): AudioTrackOptions = when {
        source.usesPlayback -> AudioTrackOptions(source, DEFAULT_SAMPLE_RATE, DEFAULT_PLAYBACK_CHANNELS, DEFAULT_PLAYBACK_BIT_RATE)
        else -> AudioTrackOptions(source, DEFAULT_SAMPLE_RATE, DEFAULT_MICROPHONE_CHANNELS, DEFAULT_MICROPHONE_BIT_RATE)
    }

    fun integerMessage(name: String, range: IntRange): String = "$name must be an integer in $range."

    fun validate(track: AudioTrackOptions) {
        require(track.source != RecordingAudioSource.NONE) { "An audio track needs a microphone, playback or mixed source." }
        require(track.sampleRate in SAMPLE_RATES) { integerMessage("sampleRate", SAMPLE_RATES) }
        require(track.channels in CHANNELS) { CHANNELS_MESSAGE }
        require(track.bitRate in AUDIO_BIT_RATES) { integerMessage("bitRate", AUDIO_BIT_RATES) }
    }

    fun validateMaxDuration(millis: Long) {
        require(millis in 0..MAX_DURATION_LIMIT_MILLIS) { "maxDuration must be between 0 (no limit) and $MAX_DURATION_LIMIT_MILLIS milliseconds." }
    }

    /** Checks before an audio recording starts; with [overwrite] an existing file is replaced only when the recording is saved. */
    fun checkAudioOutput(output: File, overwrite: Boolean) {
        require(!output.isDirectory) { "Audio recording output is a directory: ${output.path}" }
        require(overwrite || !output.exists()) { "Audio recording output already exists: ${output.path}" }
    }

    /** Frames of [maxDurationMillis] at [sampleRate]; [Long.MAX_VALUE] for no limit. zh-CN: [maxDurationMillis] 对应的帧数; 不限时为 [Long.MAX_VALUE]. */
    fun maxFrames(maxDurationMillis: Long, sampleRate: Int): Long =
        if (maxDurationMillis <= 0) Long.MAX_VALUE else maxDurationMillis * sampleRate / 1000

    /** Milliseconds of [frames] at [sampleRate]. zh-CN: [frames] 帧对应的毫秒数. */
    fun framesToMillis(frames: Long, sampleRate: Int): Long = frames * 1000 / sampleRate
}

/**
 * Reasons passed to [RecordingStopListener] and kept in the results. A foreground lease may pass its own reason (for
 * example the Node service's "service-destroyed").
 *
 * zh-CN: 传给 [RecordingStopListener] 并记入结果的停止原因. 前台租约可以传入自己的原因 (例如 Node 服务的 "service-destroyed").
 */
object RecordingStopReasons {
    const val STOPPED = "stopped"
    const val MAX_DURATION = "max-duration"
    const val MAX_SIZE = "max-size"
    const val CANCELLED = "cancelled"
    const val FAILED = "failed"
    const val NOTIFICATION_STOP = "notification-stop"
}

/**
 * States of [AudioRecordingStatus.state]. [PAUSED] is a plugin addition (the Studio recorder's pause).
 *
 * zh-CN: [AudioRecordingStatus.state] 的取值. [PAUSED] 为插件新增 (Studio 录音的暂停).
 */
object RecordingStates {
    const val IDLE = "idle"
    const val RECORDING = "recording"
    const val PAUSED = "paused"
    const val STOPPED = "stopped"
    const val CANCELLED = "cancelled"
    const val FAILED = "failed"
}

/**
 * Called once when a recording ends, after its file is saved or discarded, on the thread that ended it (the caller of
 * stop / cancel, or a kernel thread for automatic stops). [result] is set when the file was saved, [error] when the
 * recording failed; both are null when it was cancelled.
 *
 * zh-CN: 录制结束时调用一次, 在文件保存或丢弃之后, 运行在结束它的线程上 (stop / cancel 的调用者, 自动停止时为内核线程).
 * 保存了文件时 [result] 非空, 失败时 [error] 非空, 取消时两者都为 null.
 */
fun interface RecordingStopListener<T : Any> {
    fun onStop(reason: String, result: T?, error: Throwable?)
}

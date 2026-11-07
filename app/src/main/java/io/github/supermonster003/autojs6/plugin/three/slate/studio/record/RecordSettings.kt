package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputPaths
import java.util.TimeZone

/**
 * The options of the Studio recording page (roadmap P4.5, D41). They are the options of the script `audio.record`
 * (roadmap appendix B.2): [sampleRate], [channels] and [bitRate] are null for the default of the [source] (appendix D Q1:
 * microphone mono 48 kHz at 128 kbit/s, playback and mixed stereo 48 kHz at 192 kbit/s), [maxDurationMillis] 0 for no
 * limit. [track] and [options] validate with the kernel's own checks, so the page cannot start what `audio.record` would
 * reject.
 * zh-CN: Studio 录音页的选项 (路线图 P4.5, D41), 即脚本 `audio.record` 的选项 (路线图附录 B.2): [sampleRate], [channels] 与
 * [bitRate] 为 null 时取 [source] 的默认值 (附录 D Q1: 麦克风单声道 48 kHz 128 kbit/s, 内录与混合立体声 48 kHz 192 kbit/s),
 * [maxDurationMillis] 为 0 表示不限. [track] 与 [options] 用内核自己的校验, 页面不会开始 `audio.record` 会拒绝的录音.
 */
internal data class RecordSettings(
    val source: RecordingAudioSource = RecordingAudioSource.MICROPHONE,
    val format: AudioOutputFormat = AudioOutputFormat.M4A,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val bitRate: Int? = null,
    val maxDurationMillis: Long = 0,
) {

    /** The default track of [source]. zh-CN: [source] 的默认音轨. */
    val defaults: AudioTrackOptions get() = RecordingOptions.defaultTrack(source)

    /** The kernel track; IllegalArgumentException for values `audio.record` rejects. zh-CN: 内核音轨; `audio.record` 会拒绝的值抛 IllegalArgumentException. */
    fun track(): AudioTrackOptions =
        AudioTrackOptions(source, sampleRate ?: defaults.sampleRate, channels ?: defaults.channels, bitRate ?: defaults.bitRate)

    fun options(): AudioRecordingOptions = AudioRecordingOptions(track(), format, maxDurationMillis)

    /** The settings a device of [sdk] can record: playback and mixed need API 29. zh-CN: [sdk] 设备能录的设置: 内录与混合需要 API 29. */
    fun supportedOn(sdk: Int): RecordSettings = if (source in sources(sdk)) this else copy(source = RecordingAudioSource.MICROPHONE)

    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_SOURCE, source.key)
        putString(KEY_FORMAT, format.key)
        sampleRate?.let { putInt(KEY_SAMPLE_RATE, it) }
        channels?.let { putInt(KEY_CHANNELS, it) }
        bitRate?.let { putInt(KEY_BIT_RATE, it) }
        putLong(KEY_MAX_DURATION, maxDurationMillis)
    }

    companion object {

        /** API level of AudioPlaybackCaptureConfiguration. zh-CN: AudioPlaybackCaptureConfiguration 的 API 级别. */
        const val PLAYBACK_MIN_SDK = 29

        /** The operation name in default output names (D14). zh-CN: 默认输出名称中的操作名 (D14). */
        const val OP = "record"

        val SAMPLE_RATE_CHOICES: List<Int> = listOf(8000, 16_000, 22_050, 32_000, 44_100, 48_000)
        val CHANNEL_CHOICES: List<Int> = listOf(1, 2)
        val BIT_RATE_CHOICES: List<Int> = listOf(64_000, 96_000, 128_000, 160_000, 192_000, 256_000, 320_000)
        val MAX_DURATION_CHOICES: List<Long> = listOf(0L, 60_000L, 5 * 60_000L, 10 * 60_000L, 30 * 60_000L, 60 * 60_000L)

        private const val KEY_SOURCE = "source"
        private const val KEY_FORMAT = "format"
        private const val KEY_SAMPLE_RATE = "sampleRate"
        private const val KEY_CHANNELS = "channels"
        private const val KEY_BIT_RATE = "bitRate"
        private const val KEY_MAX_DURATION = "maxDuration"

        /** The sources the page offers on [sdk]; below API 29 only the microphone. zh-CN: [sdk] 上页面提供的音源; API 29 以下只有麦克风. */
        fun sources(sdk: Int): List<RecordingAudioSource> = if (sdk >= PLAYBACK_MIN_SDK) {
            listOf(RecordingAudioSource.MICROPHONE, RecordingAudioSource.PLAYBACK, RecordingAudioSource.MIXED)
        } else {
            listOf(RecordingAudioSource.MICROPHONE)
        }

        fun fromBundle(bundle: Bundle?): RecordSettings {
            if (bundle == null) return RecordSettings()
            fun int(key: String): Int? = if (bundle.containsKey(key)) bundle.getInt(key) else null
            return RecordSettings(
                source = RecordingAudioSource.of(bundle.getString(KEY_SOURCE))?.takeIf { it != RecordingAudioSource.NONE } ?: RecordingAudioSource.MICROPHONE,
                format = AudioOutputFormat.of(bundle.getString(KEY_FORMAT)) ?: AudioOutputFormat.M4A,
                sampleRate = int(KEY_SAMPLE_RATE),
                channels = int(KEY_CHANNELS),
                bitRate = int(KEY_BIT_RATE),
                maxDurationMillis = bundle.getLong(KEY_MAX_DURATION, 0L),
            )
        }

        /**
         * The output of a recording started at [timestampMillis] (roadmap D14): `slate-record-<yyyyMMdd-HHmmss>.<ext>` (no
         * single source, like compose and audioMix) in [customFolder] (the Studio output folder of the settings, D37) or
         * in `Music/3-Slate Studio/` under [storageRoot]; a taken name gets the suffix `-<n>`.
         * zh-CN: 在 [timestampMillis] 开始的录音的输出 (路线图 D14): `slate-record-<yyyyMMdd-HHmmss>.<ext>` (没有单一来源, 与
         * compose 和 audioMix 相同), 放在 [customFolder] (设置中的 Studio 输出目录, D37) 或 [storageRoot] 下的
         * `Music/3-Slate Studio/`; 重名时加后缀 `-<n>`.
         */
        fun output(
            storageRoot: String,
            customFolder: String?,
            format: AudioOutputFormat,
            timestampMillis: Long,
            zone: TimeZone,
            taken: (String) -> Boolean,
        ): String {
            val folder = customFolder?.trimEnd('/')?.takeIf { it.startsWith("/") } ?: OutputPaths.defaultDirectory(storageRoot, OutputKind.AUDIO)
            val name = OutputPaths.defaultFileName(null, OP, format.key, timestampMillis, zone)
            return OutputPaths.uniquePath("$folder/$name", folder = false, taken)
        }
    }
}

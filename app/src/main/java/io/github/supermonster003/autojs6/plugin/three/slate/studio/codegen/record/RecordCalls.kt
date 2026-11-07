package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.record

import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.CodeGenerator
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.AudioOutputFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordSettings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordingAudioSource

/**
 * The script equivalent to a Studio recording (roadmap P4.5, D11): `audio.record` with the page's options, a wait as
 * long as the recording and `stop()`. Only `source` and the options that differ from the defaults of that source
 * (roadmap appendix B.2, appendix D Q1) are written; `source` uses the short key `mic` of the script API. The path is
 * written only for a custom output folder (D37): the script's default output is `Music/AutoJs6/` (D14), like an absent
 * wizard output means the script default.
 * zh-CN: 与 Studio 录音等价的脚本 (路线图 P4.5, D11): 带页面选项的 `audio.record`, 与录音等长的等待, 以及 `stop()`. 只写出
 * `source` 与不同于该音源默认值的选项 (路线图附录 B.2, 附录 D Q1); `source` 使用脚本 API 的简写 `mic`. 只有自定义输出目录
 * (D37) 时写出路径: 脚本的默认输出为 `Music/AutoJs6/` (D14), 与向导输出留空表示脚本默认值相同.
 */
internal object RecordCalls {

    /** The options object of `audio.record`, in the order of the script documentation. zh-CN: `audio.record` 的选项对象, 顺序同脚本文档. */
    fun options(settings: RecordSettings): LinkedHashMap<String, Any?> {
        val track = settings.track()
        val defaults = settings.defaults
        return linkedMapOf<String, Any?>().apply {
            put("source", sourceKey(settings.source))
            if (settings.format != AudioOutputFormat.M4A) put("format", settings.format.key)
            if (track.sampleRate != defaults.sampleRate) put("sampleRate", track.sampleRate)
            if (track.channels != defaults.channels) put("channels", track.channels)
            if (settings.format == AudioOutputFormat.M4A && track.bitRate != defaults.bitRate) put("bitRate", track.bitRate)
            if (settings.maxDurationMillis > 0) put("maxDuration", settings.maxDurationMillis)
        }
    }

    /**
     * The script: [output] is the explicit path (null for the default output), [durationMillis] the length to wait (the
     * recorded duration, rounded up to 100 ms, at least 100 ms).
     * zh-CN: 脚本: [output] 为显式路径 (默认输出为 null), [durationMillis] 为等待时长 (已录时长, 向上取整到 100 ms, 至少 100 ms).
     */
    fun code(settings: RecordSettings, output: String?, durationMillis: Long): String {
        val arguments = listOfNotNull(output, options(settings)).toTypedArray()
        val wait = ((durationMillis.coerceAtLeast(1) + 99) / 100) * 100
        return listOf(
            "let recording = " + CodeGenerator.call("audio", "record", *arguments),
            "sleep($wait);",
            "let result = recording.stop();",
            "console.log(result.path);",
        ).joinToString("\n")
    }

    /** The script key of [source]; `mic` is the documented short form. zh-CN: [source] 的脚本键; `mic` 是文档中的简写. */
    fun sourceKey(source: RecordingAudioSource): String = when (source) {
        RecordingAudioSource.MICROPHONE -> "mic"
        else -> source.key
    }

}

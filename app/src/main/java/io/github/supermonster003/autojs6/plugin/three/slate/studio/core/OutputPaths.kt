package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Default output paths of roadmap D14: videos go to `Movies/3-Slate Studio/`, audio to `Music/3-Slate Studio/`, named
 * `<source>-<op>-<timestamp>.<ext>`. Roadmap P2.1 adds the collision suffix and the MediaStore registration.
 * zh-CN: 路线图 D14 的默认输出路径: 视频放 `Movies/3-Slate Studio/`, 音频放 `Music/3-Slate Studio/`, 文件名为
 * `<源名>-<操作>-<时间戳>.<ext>`. 路线图 P2.1 再加重名后缀与 MediaStore 登记.
 */
internal object OutputPaths {

    const val FOLDER_NAME = "3-Slate Studio"
    const val VIDEO_PARENT = "Movies"
    const val AUDIO_PARENT = "Music"

    /** Base name when the operation has no single source (compose, audioMix). zh-CN: 操作没有单一来源时的基础名. */
    const val FALLBACK_SOURCE_NAME = "slate"

    /** java.time needs API 26 (min SDK is 24), so the formatter is SimpleDateFormat. zh-CN: java.time 需 API 26 (最低 24), 故用 SimpleDateFormat. */
    private const val TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss"

    /** Characters FAT and exFAT volumes reject, plus control characters. zh-CN: FAT 与 exFAT 卷拒绝的字符及控制字符. */
    private val UNSAFE = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun defaultDirectory(storageRoot: String, audio: Boolean): String =
        storageRoot.trimEnd('/') + "/" + (if (audio) AUDIO_PARENT else VIDEO_PARENT) + "/" + FOLDER_NAME

    fun defaultFileName(sourcePath: String?, op: String, extension: String, timestampMillis: Long, zone: TimeZone): String {
        val source = sourcePath
            ?.substringAfterLast('/')
            ?.let { name -> if (name.lastIndexOf('.') > 0) name.substringBeforeLast('.') else name }
            ?.let { sanitize(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: FALLBACK_SOURCE_NAME
        val ext = extension.trimStart('.').lowercase()
        require(ext.isNotEmpty() && sanitize(ext) == ext) { "invalid extension: $extension" }
        val stamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.ROOT).apply { timeZone = zone }.format(Date(timestampMillis))
        return "$source-$op-$stamp.$ext"
    }

    fun defaultOutput(
        storageRoot: String,
        sourcePath: String?,
        op: String,
        extension: String,
        audio: Boolean,
        timestampMillis: Long,
        zone: TimeZone,
    ): String = defaultDirectory(storageRoot, audio) + "/" + defaultFileName(sourcePath, op, extension, timestampMillis, zone)

    internal fun sanitize(name: String): String = UNSAFE.replace(name, "_").trim().trimEnd('.')

}

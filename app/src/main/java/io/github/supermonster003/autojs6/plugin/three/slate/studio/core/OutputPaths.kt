package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Default output paths of roadmap D14: videos go to `Movies/3-Slate Studio/`, audio to `Music/3-Slate Studio/`, named
 * `<source>-<op>-<timestamp>.<ext>`; a taken name gets the suffix `-<n>` (roadmap P2.1, [uniquePath]). MediaStoreRegistrar
 * registers the finished file.
 * zh-CN: 路线图 D14 的默认输出路径: 视频放 `Movies/3-Slate Studio/`, 音频放 `Music/3-Slate Studio/`, 文件名为
 * `<源名>-<操作>-<时间戳>.<ext>`; 重名时加后缀 `-<n>` (路线图 P2.1, [uniquePath]). 完成的文件由 MediaStoreRegistrar 登记.
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

    /** Highest collision suffix tried before giving up. zh-CN: 放弃前尝试的最大重名序号. */
    const val MAX_COLLISION_SUFFIX = 9999

    /**
     * [path] when [taken] says it is free, otherwise the first free `<base>-<n>.<ext>` with n from 1. Only default
     * outputs get a suffix; an explicit output that exists is IO_ERROR (host contract).
     * zh-CN: [taken] 表明 [path] 空闲时返回它, 否则返回第一个空闲的 `<base>-<n>.<ext>` (n 从 1 起). 只有默认输出会加后缀;
     * 显式给出且已存在的输出为 IO_ERROR (宿主契约).
     */
    fun uniquePath(path: String, taken: (String) -> Boolean): String {
        if (!taken(path)) return path
        val directory = path.substringBeforeLast('/', "")
        val name = path.substringAfterLast('/')
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        for (n in 1..MAX_COLLISION_SUFFIX) {
            val candidate = "$directory/$base-$n$extension"
            if (!taken(candidate)) return candidate
        }
        throw MediaStudioException.io("no free output name for $path")
    }

    internal fun sanitize(name: String): String = UNSAFE.replace(name, "_").trim().trimEnd('.')

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioOps
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What an operation writes: it decides the default folder ([parent]) and the accepted output paths. Images, GIF and
 * frame folders go to `Pictures` (appendix D Q14 (a)).
 * zh-CN: 操作写出的内容: 决定默认文件夹 ([parent]) 与可接受的输出路径. 图片, GIF 与帧文件夹放 `Pictures` (附录 D Q14 (a)).
 */
internal enum class OutputKind(val parent: String) {
    VIDEO(OutputPaths.VIDEO_PARENT),
    AUDIO(OutputPaths.AUDIO_PARENT),
    IMAGE(OutputPaths.PICTURES_PARENT),
    GIF(OutputPaths.PICTURES_PARENT),

    /** `frames` writes a folder of images. zh-CN: `frames` 写出一个图片文件夹. */
    DIRECTORY(OutputPaths.PICTURES_PARENT),
    ;

    companion object {
        fun of(op: String): OutputKind = when (op) {
            MediaStudioOps.EXTRACT_AUDIO, MediaStudioOps.AUDIO_TRIM, MediaStudioOps.AUDIO_CONCAT, MediaStudioOps.AUDIO_MIX,
            MediaStudioOps.AUDIO_VOLUME, MediaStudioOps.AUDIO_CONVERT, MediaStudioOps.AUDIO_SPEED -> AUDIO
            MediaStudioOps.FRAMES -> DIRECTORY
            MediaStudioOps.FRAME, MediaStudioOps.THUMBNAIL -> IMAGE
            MediaStudioOps.TO_GIF -> GIF
            else -> VIDEO
        }
    }
}

/**
 * Default output paths of roadmap D14: videos go to `Movies/3-Slate Studio/`, audio to `Music/3-Slate Studio/`, images,
 * GIF and frame folders to `Pictures/3-Slate Studio/` (appendix D Q14), named `<source>-<op>-<timestamp>.<ext>` (folders
 * without the extension); a taken name gets the suffix `-<n>` (roadmap P2.1, [uniquePath]). MediaStoreRegistrar
 * registers the finished files.
 * zh-CN: 路线图 D14 的默认输出路径: 视频放 `Movies/3-Slate Studio/`, 音频放 `Music/3-Slate Studio/`, 图片, GIF 与帧文件夹放
 * `Pictures/3-Slate Studio/` (附录 D Q14), 名称为 `<源名>-<操作>-<时间戳>.<ext>` (文件夹不带扩展名); 重名时加后缀 `-<n>`
 * (路线图 P2.1, [uniquePath]). 完成的文件由 MediaStoreRegistrar 登记.
 */
internal object OutputPaths {

    const val FOLDER_NAME = "3-Slate Studio"
    const val VIDEO_PARENT = "Movies"
    const val AUDIO_PARENT = "Music"
    const val PICTURES_PARENT = "Pictures"

    /** Base name when the operation has no single source (compose, audioMix). zh-CN: 操作没有单一来源时的基础名. */
    const val FALLBACK_SOURCE_NAME = "slate"

    /** java.time needs API 26 (min SDK is 24), so the formatter is SimpleDateFormat. zh-CN: java.time 需 API 26 (最低 24), 故用 SimpleDateFormat. */
    private const val TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss"

    /** Characters FAT and exFAT volumes reject, plus control characters. zh-CN: FAT 与 exFAT 卷拒绝的字符及控制字符. */
    private val UNSAFE = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun defaultDirectory(storageRoot: String, audio: Boolean): String =
        defaultDirectory(storageRoot, if (audio) OutputKind.AUDIO else OutputKind.VIDEO)

    fun defaultDirectory(storageRoot: String, kind: OutputKind): String = storageRoot.trimEnd('/') + "/" + kind.parent + "/" + FOLDER_NAME

    /** The source file name without its extension, made safe, or [FALLBACK_SOURCE_NAME]. zh-CN: 去掉扩展名并处理过的源文件名, 或 [FALLBACK_SOURCE_NAME]. */
    fun sourceStem(sourcePath: String?): String = sourcePath
        ?.substringAfterLast('/')
        ?.let { name -> if (name.lastIndexOf('.') > 0) name.substringBeforeLast('.') else name }
        ?.let { sanitize(it) }
        ?.takeIf { it.isNotEmpty() }
        ?: FALLBACK_SOURCE_NAME

    /** `<source>-<op>-<timestamp>`, the name of a default frame folder. zh-CN: `<源名>-<操作>-<时间戳>`, 默认帧文件夹的名称. */
    fun defaultBaseName(sourcePath: String?, op: String, timestampMillis: Long, zone: TimeZone): String {
        val stamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.ROOT).apply { timeZone = zone }.format(Date(timestampMillis))
        return "${sourceStem(sourcePath)}-$op-$stamp"
    }

    fun defaultFileName(sourcePath: String?, op: String, extension: String, timestampMillis: Long, zone: TimeZone): String {
        val ext = extension.trimStart('.').lowercase()
        require(ext.isNotEmpty() && sanitize(ext) == ext) { "invalid extension: $extension" }
        return defaultBaseName(sourcePath, op, timestampMillis, zone) + "." + ext
    }

    fun defaultOutput(
        storageRoot: String,
        sourcePath: String?,
        op: String,
        extension: String,
        kind: OutputKind,
        timestampMillis: Long,
        zone: TimeZone,
    ): String = defaultDirectory(storageRoot, kind) + "/" + defaultFileName(sourcePath, op, extension, timestampMillis, zone)

    /**
     * `<source>-<n>.<ext>` for frame [index] (from 0) of [count]; n starts at 1 and has at least 4 digits.
     * zh-CN: 共 [count] 帧中第 [index] 帧 (从 0 起) 的文件名 `<源名>-<n>.<ext>`; n 从 1 起, 至少 4 位.
     */
    fun frameFileName(sourcePath: String?, index: Int, count: Int, extension: String): String {
        val digits = maxOf(FRAME_NUMBER_DIGITS, count.toString().length)
        return "${sourceStem(sourcePath)}-${(index + 1).toString().padStart(digits, '0')}.$extension"
    }

    private const val FRAME_NUMBER_DIGITS = 4

    /** Highest collision suffix tried before giving up. zh-CN: 放弃前尝试的最大重名序号. */
    const val MAX_COLLISION_SUFFIX = 9999

    /**
     * [path] when [taken] says it is free, otherwise the first free `<base>-<n>.<ext>` with n from 1 (`<name>-<n>` for a
     * [folder]). Only default outputs get a suffix; an explicit output that exists is IO_ERROR (host contract).
     * zh-CN: [taken] 表明 [path] 空闲时返回它, 否则返回第一个空闲的 `<base>-<n>.<ext>` (n 从 1 起; [folder] 为 `<name>-<n>`).
     * 只有默认输出会加后缀; 显式给出且已存在的输出为 IO_ERROR (宿主契约).
     */
    fun uniquePath(path: String, folder: Boolean = false, taken: (String) -> Boolean): String {
        if (!taken(path)) return path
        val directory = path.substringBeforeLast('/', "")
        val name = path.substringAfterLast('/')
        // A folder name has no extension, whatever dots the source name had. zh-CN: 文件夹名没有扩展名, 无论源名含几个点.
        val dot = if (folder) -1 else name.lastIndexOf('.')
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

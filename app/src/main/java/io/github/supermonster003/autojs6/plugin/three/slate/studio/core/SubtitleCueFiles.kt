package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.os.ParcelFileDescriptor
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer

/** One parsed cue as `subtitlesParse` answers it: milliseconds and the plain text. zh-CN: `subtitlesParse` 返回的一条 cue: 毫秒与纯文本. */
internal data class CueText(val startMillis: Long, val endMillis: Long, val text: String)

/**
 * The cues file of `subtitlesParse` (media studio contract version 2, appendix D Q36 (b)): the cues are written as a
 * UTF-8 JSON array `[{"start": ms, "end": ms, "text": "..."}]` into [DIRECTORY] of the cache folder, a read-only
 * descriptor is opened and the file is deleted at once, so nothing stays on disk whatever happens to the task; the
 * descriptor travels in the result under `cuesFile` and is closed after `onComplete` (or when the result is discarded).
 * The host reads it to its end; neither side needs a storage permission and the 256 KiB event limit does not apply.
 * zh-CN: `subtitlesParse` 的 cue 文件 (媒体工坊契约版本 2, 附录 D Q36 (b)): cue 以 UTF-8 JSON 数组 `[{"start": 毫秒, "end": 毫秒,
 * "text": "..."}]` 写入缓存目录的 [DIRECTORY], 打开只读描述符后立即删除文件, 因此无论任务结果如何磁盘上都不留文件; 描述符放在结果的
 * `cuesFile` 中, 在 `onComplete` 之后 (或结果被丢弃时) 关闭. 宿主把它读到结尾; 两侧都不需要存储权限, 也不受 256 KiB 事件上限限制.
 */
internal object SubtitleCueFiles {

    const val DIRECTORY = "subtitles-parse"

    /**
     * Writes [cues] and answers the open descriptor; the file is already deleted. More than
     * [MediaStudioLimits.MAX_CUES_FILE_BYTES] of JSON is LIMIT_EXCEEDED, a write failure IO_ERROR.
     * zh-CN: 写出 [cues] 并返回已打开的描述符; 文件已被删除. JSON 超过 [MediaStudioLimits.MAX_CUES_FILE_BYTES] 为 LIMIT_EXCEEDED, 写入失败为 IO_ERROR.
     */
    fun open(cacheDir: File, taskId: Int, source: String, cues: List<CueText>): ParcelFileDescriptor {
        val folder = File(cacheDir, DIRECTORY)
        if (!folder.isDirectory && !folder.mkdirs()) throw MediaStudioException.io("cannot create the cache folder ${folder.path}")
        val file = File(folder, "cues-$taskId-${System.nanoTime()}.json")
        try {
            try {
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8), BUFFER_CHARS).use { write(cues, it) }
            } catch (e: IOException) {
                throw MediaStudioException.io("cannot write the cues of $source (${e.message ?: e.javaClass.simpleName})")
            }
            val size = file.length()
            if (size > MediaStudioLimits.MAX_CUES_FILE_BYTES) {
                throw MediaStudioException(
                    MediaStudioErrorCodes.LIMIT_EXCEEDED,
                    "the ${cues.size} cues of $source take $size bytes of JSON, more than the ${MediaStudioLimits.MAX_CUES_FILE_BYTES} bytes the host reads",
                )
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } finally {
            // The open descriptor keeps the content readable. zh-CN: 已打开的描述符仍可读取内容.
            file.delete()
        }
    }

    /** Deletes what a process that died between writing and deleting left. zh-CN: 删除进程在写入与删除之间死亡时留下的文件. */
    fun clearStale(cacheDir: File) {
        File(cacheDir, DIRECTORY).listFiles()?.forEach { it.delete() }
    }

    /** The JSON array of [cues] in file order. zh-CN: [cues] 按文件顺序的 JSON 数组. */
    fun write(cues: List<CueText>, out: Writer) {
        out.write("[")
        cues.forEachIndexed { index, cue ->
            if (index > 0) out.write(",\n")
            out.write("{\"")
            out.write(MediaStudioResultKeys.CUE_START)
            out.write("\":")
            out.write(cue.startMillis.toString())
            out.write(",\"")
            out.write(MediaStudioResultKeys.CUE_END)
            out.write("\":")
            out.write(cue.endMillis.toString())
            out.write(",\"")
            out.write(MediaStudioResultKeys.CUE_TEXT)
            out.write("\":")
            string(cue.text, out)
            out.write("}")
        }
        out.write("]\n")
    }

    /**
     * A JSON string; control characters and unpaired surrogates (which UTF-8 cannot carry) are escaped as `\uXXXX`.
     * zh-CN: JSON 字符串; 控制字符与不成对的代理 (UTF-8 无法承载) 转义为 `\uXXXX`.
     */
    private fun string(text: String, out: Writer) {
        out.write('"'.code)
        var i = 0
        while (i < text.length) {
            val char = text[i]
            when {
                char == '"' -> out.write("\\\"")
                char == '\\' -> out.write("\\\\")
                char == '\n' -> out.write("\\n")
                char == '\r' -> out.write("\\r")
                char == '\t' -> out.write("\\t")
                char < ' ' -> out.write(unicodeEscape(char))
                Character.isHighSurrogate(char) && i + 1 < text.length && Character.isLowSurrogate(text[i + 1]) -> {
                    out.write(char.code)
                    out.write(text[i + 1].code)
                    i++
                }
                Character.isSurrogate(char) -> out.write(unicodeEscape(char))
                else -> out.write(char.code)
            }
            i++
        }
        out.write('"'.code)
    }

    private fun unicodeEscape(char: Char) = "\\u" + char.code.toString(16).padStart(4, '0')

    private const val BUFFER_CHARS = 64 * 1024
}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Locale
import kotlin.math.roundToLong

/** Subtitle file formats of roadmap D31. zh-CN: 路线图 D31 的字幕文件格式. */
internal enum class SubtitleFormat(val id: String, val mime: String) {
    SRT("srt", "application/x-subrip"),
    ASS("ass", "text/x-ssa"),
    ;

    val extension: String get() = id

    companion object {
        val IDS: List<String> = entries.map { it.id }

        fun of(id: String): SubtitleFormat = entries.first { it.id == id }

        /** The format of a file name; `.ssa` reads as ASS. zh-CN: 文件名对应的格式; `.ssa` 按 ASS 读取. */
        fun ofFileName(name: String): SubtitleFormat? = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "srt" -> SRT
            "ass", "ssa" -> ASS
            else -> null
        }
    }
}

/** How the text of a cue is marked up. zh-CN: cue 文本的标记方式. */
internal enum class SubtitleMarkup {
    /** `<b>`, `<i>`, `<u>`, `<font color>` and `{\...}` blocks (common in SRT files). zh-CN: `<b>` 等与 `{\...}` 块 (SRT 中常见). */
    SRT,

    /** `{\...}` override blocks and the escapes `\N`, `\n`, `\h`. zh-CN: `{\...}` 覆盖块与转义 `\N`, `\n`, `\h`. */
    ASS,
}

/**
 * One cue: [startMillis] inclusive, [endMillis] exclusive, [text] with its markup ([markup]; line breaks as `\n` for SRT,
 * as written for ASS). [style] / [layer] / the margins come from an ASS `Dialogue` line (margins 0: those of the style).
 * zh-CN: 一条 cue: [startMillis] 含, [endMillis] 不含, [text] 保留标记 ([markup]; SRT 的换行为 `\n`, ASS 按原样). [style] / [layer] /
 * 边距来自 ASS 的 `Dialogue` 行 (边距为 0 表示用样式的值).
 */
internal data class SubtitleCue(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
    val markup: SubtitleMarkup = SubtitleMarkup.SRT,
    val style: String? = null,
    val layer: Int = 0,
    val marginL: Int = 0,
    val marginR: Int = 0,
    val marginV: Int = 0,
) {
    fun isActive(timeMillis: Double): Boolean = timeMillis >= startMillis && timeMillis < endMillis
}

/**
 * An ASS style of the basic subset of roadmap D31: font, size, primary / outline / back color (ARGB), bold, italic,
 * border style (1 outline and shadow, 3 opaque box), outline, shadow, alignment (numpad 1 - 9), margins; sizes are
 * script pixels (`PlayResX` / `PlayResY`).
 * zh-CN: 路线图 D31 基础子集的 ASS 样式: 字体, 字号, 主色 / 描边色 / 背景色 (ARGB), 粗体, 斜体, 边框样式 (1 描边加阴影, 3 不透明底框),
 * 描边, 阴影, 对齐 (小键盘 1 - 9), 边距; 尺寸为脚本像素 (`PlayResX` / `PlayResY`).
 */
internal data class AssStyle(
    val name: String,
    val font: String = "sans-serif",
    val fontSize: Double = 20.0,
    val primaryColor: Int = Colors.WHITE,
    val outlineColor: Int = Colors.BLACK,
    val backColor: Int = Colors.BLACK,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val borderStyle: Int = 1,
    val outline: Double = 2.0,
    val shadow: Double = 2.0,
    val alignment: Int = 2,
    val marginL: Double = 10.0,
    val marginR: Double = 10.0,
    val marginV: Double = 10.0,
)

/**
 * A parsed subtitle file or cue array. [styles] and [playResX] / [playResY] are those of an ASS script (empty / null for
 * SRT and cue arrays); [wrapStyle] 2 makes `\n` a line break.
 * zh-CN: 解析后的字幕文件或 cue 数组. [styles] 与 [playResX] / [playResY] 为 ASS 脚本的值 (SRT 与 cue 数组为空 / null);
 * [wrapStyle] 为 2 时 `\n` 也换行.
 */
internal data class SubtitleTrack(
    val format: SubtitleFormat,
    val cues: List<SubtitleCue>,
    val styles: Map<String, AssStyle> = emptyMap(),
    val playResX: Int? = null,
    val playResY: Int? = null,
    val wrapStyle: Int = 0,
    val warnings: List<String> = emptyList(),
) {
    /** The style of [cue] (`*Default` and unknown names fall back to `Default`). zh-CN: [cue] 的样式 (`*Default` 与未知名称退回 `Default`). */
    fun styleOf(cue: SubtitleCue): AssStyle? {
        if (styles.isEmpty()) return null
        val name = cue.style?.trim()?.removePrefix("*")
        return name?.let { styles[it] } ?: styles[DEFAULT_STYLE] ?: styles.values.first()
    }

    companion object {
        const val DEFAULT_STYLE = "Default"
    }
}

/** A styled piece of cue text; null attributes keep the style. zh-CN: cue 文本中带样式的一段; null 属性沿用样式. */
internal data class TextRun(
    val text: String,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val color: Int? = null,
    /** Script pixels for ASS, output pixels otherwise. zh-CN: ASS 为脚本像素, 其他为输出像素. */
    val fontSize: Double? = null,
)

/**
 * The text of a cue split into [runs] (line breaks are `\n` inside them), with the cue-wide `\an` [alignment] and `\pos`
 * [position] (script pixels for ASS) when the text sets them.
 * zh-CN: 拆分为 [runs] 的 cue 文本 (换行为其中的 `\n`), 以及文本设置的整条 cue 的 `\an` 对齐 [alignment] 与 `\pos` 位置 [position]
 * (ASS 为脚本像素).
 */
internal data class RichText(val runs: List<TextRun>, val alignment: Int? = null, val position: Pair<Double, Double>? = null) {
    val plain: String get() = runs.joinToString("") { it.text }
}

/**
 * Collects the unsupported features met while parsing, so a file with thousands of karaoke tags yields one warning.
 * zh-CN: 收集解析中遇到的不支持的特性, 使含数千个卡拉 OK 标签的文件只产生一条警告.
 */
internal class SubtitleWarnings {

    private val tags = LinkedHashMap<String, Int>()

    private val notes = LinkedHashSet<String>()

    fun tag(name: String) {
        tags[name] = (tags[name] ?: 0) + 1
    }

    fun note(text: String) {
        if (notes.size < MAX_NOTES) notes += text
    }

    fun list(): List<String> = buildList {
        if (tags.isNotEmpty()) {
            add("ASS override tags that 3-Slate Studio 1.0.0 does not render were ignored: " + tags.entries.joinToString { (name, count) -> "\\$name ($count)" })
        }
        addAll(notes)
    }

    private companion object {
        const val MAX_NOTES = 32
    }
}

/**
 * Inline markup of roadmap D31: in ASS only `b`, `i`, `c` (`1c`), `fs`, `an` and `pos` are rendered, the other tags are
 * ignored and reported (drawing mode `\p` also hides its drawing commands); SRT text takes `<b>`, `<i>`, `<u>`,
 * `<font color>` and the same `{\...}` blocks. A tag without a value returns to the style.
 * zh-CN: 路线图 D31 的内联标记: ASS 只渲染 `b`, `i`, `c` (`1c`), `fs`, `an` 与 `pos`, 其他标签忽略并报告 (绘图模式 `\p` 同时隐藏
 * 绘图命令); SRT 文本支持 `<b>`, `<i>`, `<u>`, `<font color>` 与同样的 `{\...}` 块. 不带值的标签恢复样式.
 */
internal object SubtitleMarkups {

    /** Tag names, longest first so `fscx` is not read as `fs`. zh-CN: 标签名, 长的在前, 避免把 `fscx` 读成 `fs`. */
    private val TAG_NAMES = listOf(
        "fscx", "fscy", "fsp", "fade", "fad", "frx", "fry", "frz", "fax", "fay", "fs", "fn", "fe", "fr",
        "xbord", "ybord", "bord", "xshad", "yshad", "shad", "blur", "be", "alpha", "iclip", "clip", "move", "org", "pos",
        "pbo", "kf", "ko", "an", "1c", "2c", "3c", "4c", "1a", "2a", "3a", "4a", "b", "i", "u", "s", "c", "a", "k", "K", "q", "r", "p", "t",
    )

    private val HTML_TAG = Regex("""<\s*(/?)\s*(b|i|u|s|font)\b([^>]*)>""", RegexOption.IGNORE_CASE)

    private val FONT_COLOR = Regex("""color\s*=\s*["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE)

    private val FONT_OTHER = Regex("""\b(face|size)\s*=""", RegexOption.IGNORE_CASE)

    private class State {
        var bold: Boolean? = null
        var italic: Boolean? = null
        var underline: Boolean? = null
        var color: Int? = null
        var fontSize: Double? = null
        var drawing = false
        val colors = ArrayDeque<Int?>()
        var alignment: Int? = null
        var position: Pair<Double, Double>? = null
    }

    fun parse(text: String, markup: SubtitleMarkup, wrapStyle: Int = 0, warnings: SubtitleWarnings = SubtitleWarnings()): RichText {
        val state = State()
        val runs = mutableListOf<TextRun>()
        val buffer = StringBuilder()

        fun flush() {
            if (buffer.isNotEmpty()) {
                if (!state.drawing) runs += TextRun(buffer.toString(), state.bold, state.italic, state.underline, state.color, state.fontSize)
                buffer.setLength(0)
            }
        }

        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '{' && text.indexOf('}', i + 1) > i -> {
                    val end = text.indexOf('}', i + 1)
                    val block = text.substring(i + 1, end)
                    flush()
                    overrides(block, state, warnings)
                    i = end + 1
                }
                markup == SubtitleMarkup.ASS && ch == '\\' && i + 1 < text.length && text[i + 1] in "Nnh" -> {
                    buffer.append(
                        when (text[i + 1]) {
                            'N' -> '\n'
                            'n' -> if (wrapStyle == 2) '\n' else ' '
                            else -> ' '
                        },
                    )
                    i += 2
                }
                markup == SubtitleMarkup.SRT && ch == '<' -> {
                    val match = HTML_TAG.matchAt(text, i)
                    if (match == null) {
                        buffer.append(ch)
                        i++
                    } else {
                        flush()
                        html(match, state, warnings)
                        i = match.range.last + 1
                    }
                }
                else -> {
                    buffer.append(ch)
                    i++
                }
            }
        }
        flush()
        return RichText(merge(runs), state.alignment, state.position)
    }

    /** Adjacent runs with the same attributes become one. zh-CN: 属性相同的相邻片段合并为一段. */
    private fun merge(runs: List<TextRun>): List<TextRun> = buildList {
        runs.forEach { run ->
            val last = lastOrNull()
            if (last != null && last.copy(text = "") == run.copy(text = "")) set(size - 1, last.copy(text = last.text + run.text)) else add(run)
        }
    }

    private fun html(match: MatchResult, state: State, warnings: SubtitleWarnings) {
        val closing = match.groupValues[1].isNotEmpty()
        when (match.groupValues[2].lowercase(Locale.ROOT)) {
            "b" -> state.bold = if (closing) null else true
            "i" -> state.italic = if (closing) null else true
            "u" -> state.underline = if (closing) null else true
            "s" -> if (!closing) warnings.note("SRT strikeout <s> is not rendered and was ignored")
            "font" -> if (closing) {
                state.color = state.colors.removeLastOrNull()
            } else {
                val attributes = match.groupValues[3]
                state.colors.addLast(state.color)
                FONT_COLOR.find(attributes)?.groupValues?.get(1)?.let { value ->
                    val color = Colors.parse(value) ?: Colors.parse("#$value")
                    if (color == null) warnings.note("SRT font color $value is not a color and was ignored") else state.color = color
                }
                if (FONT_OTHER.containsMatchIn(attributes)) warnings.note("SRT font face and size are not rendered and were ignored")
            }
        }
    }

    /** One `{...}` block: tags start with a backslash, anything else is a comment. zh-CN: 一个 `{...}` 块: 标签以反斜杠开头, 其余为注释. */
    private fun overrides(block: String, state: State, warnings: SubtitleWarnings) {
        var i = block.indexOf('\\')
        while (i >= 0 && i < block.length) {
            val start = i + 1
            val name = TAG_NAMES.firstOrNull { block.startsWith(it, start) }
                ?: block.substring(start).takeWhile { it.isLetter() }.ifEmpty { null }
            if (name == null) {
                i = block.indexOf('\\', start)
                continue
            }
            var cursor = start + name.length
            val argument: String
            if (cursor < block.length && block[cursor] == '(') {
                var depth = 0
                var end = cursor
                while (end < block.length) {
                    if (block[end] == '(') depth++
                    if (block[end] == ')' && --depth == 0) break
                    end++
                }
                argument = block.substring(cursor + 1, end.coerceAtMost(block.length))
                cursor = (end + 1).coerceAtMost(block.length)
            } else {
                val end = block.indexOf('\\', cursor).let { if (it < 0) block.length else it }
                argument = block.substring(cursor, end).trim()
                cursor = end
            }
            tag(name, argument, state, warnings)
            i = block.indexOf('\\', cursor).takeIf { it >= cursor } ?: -1
        }
    }

    private fun tag(name: String, argument: String, state: State, warnings: SubtitleWarnings) {
        when (name) {
            // b1 / b0, or a weight: 600 and above is bold. zh-CN: b1 / b0, 或字重: 600 及以上为粗体.
            "b" -> state.bold = if (argument.isEmpty()) null else argument.toIntOrNull()?.let { it == 1 || it >= 600 } ?: state.bold
            "i" -> state.italic = if (argument.isEmpty()) null else argument.toIntOrNull()?.let { it != 0 } ?: state.italic
            "c", "1c" -> state.color = if (argument.isEmpty()) null else AssColors.parse(argument)?.let { it or (0xFF shl 24) } ?: state.color
            "fs" -> state.fontSize = if (argument.isEmpty()) null else argument.toDoubleOrNull()?.takeIf { it > 0 } ?: state.fontSize
            "an" -> argument.toIntOrNull()?.takeIf { it in 1..9 }?.let { if (state.alignment == null) state.alignment = it }
            "pos" -> {
                val values = argument.split(',').mapNotNull { it.trim().toDoubleOrNull() }
                if (values.size == 2 && state.position == null) state.position = values[0] to values[1]
            }
            "p" -> {
                state.drawing = (argument.toIntOrNull() ?: 0) > 0
                warnings.tag(name)
            }
            else -> warnings.tag(name)
        }
    }
}

/** ASS colors `&HAABBGGRR&` (alpha 00 is opaque) or decimal BGR values, as ARGB. zh-CN: ASS 颜色 `&HAABBGGRR&` (alpha 00 为不透明) 或十进制 BGR 值, 转为 ARGB. */
internal object AssColors {

    fun parse(text: String): Int? {
        val value = text.trim().trimEnd('&')
        val raw: Long = when {
            value.startsWith("&H", ignoreCase = true) -> value.substring(2).takeIf { hex -> hex.isNotEmpty() && hex.length <= 8 && hex.all { it.isHexDigit() } }?.toLong(16)
            value.startsWith("H", ignoreCase = true) -> value.substring(1).takeIf { hex -> hex.isNotEmpty() && hex.length <= 8 && hex.all { it.isHexDigit() } }?.toLong(16)
            else -> value.toLongOrNull()
        } ?: return null
        val alpha = 0xFF - ((raw shr 24) and 0xFF).toInt()
        val blue = ((raw shr 16) and 0xFF).toInt()
        val green = ((raw shr 8) and 0xFF).toInt()
        val red = (raw and 0xFF).toInt()
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    /** `&HAABBGGRR` of an ARGB color. zh-CN: ARGB 颜色的 `&HAABBGGRR` 写法. */
    fun format(argb: Int): String {
        val alpha = 0xFF - ((argb ushr 24) and 0xFF)
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF
        return "&H%02X%02X%02X%02X".format(Locale.ROOT, alpha, blue, green, red)
    }

    private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}

/**
 * Reads and writes SRT and ASS (roadmap P3.3, D31). Reading is lenient: a BOM (UTF-8 / UTF-16), CRLF or CR line ends,
 * missing or extra empty lines, missing cue numbers, `.` instead of `,` before the milliseconds and overlapping times are
 * accepted; cues that end before they start or have no text are skipped with a warning. ASS keeps the basic subset of
 * D31 and reports the rest in the warnings.
 * zh-CN: 读写 SRT 与 ASS (路线图 P3.3, D31). 读取是宽松的: 接受 BOM (UTF-8 / UTF-16), CRLF 或 CR 换行, 缺少或多余的空行, 缺少序号,
 * 毫秒前用 `.` 代替 `,`, 以及时间重叠; 结束早于开始或没有文本的 cue 跳过并给出警告. ASS 保留 D31 的基础子集, 其余写进警告.
 */
internal object SubtitleFiles {

    /** Larger subtitle files are refused (a feature film is about 100 KiB). zh-CN: 更大的字幕文件被拒绝 (一部电影约 100 KiB). */
    const val MAX_FILE_BYTES = 16L * 1024 * 1024

    /** ASS scripts without `PlayResX` / `PlayResY` use 384 x 288 (VSFilter). zh-CN: 未写 `PlayResX` / `PlayResY` 的 ASS 脚本按 384 x 288 (同 VSFilter). */
    const val DEFAULT_PLAY_RES_X = 384
    const val DEFAULT_PLAY_RES_Y = 288

    private val TIME = """(?:(\d+):)?(\d{1,2}):(\d{1,2})[,.](\d{1,3})"""

    private val SRT_TIMING = Regex("""^\s*$TIME\s*-->\s*$TIME.*$""")

    private val ASS_TIME = Regex("""^\s*(\d+):(\d{1,2}):(\d{1,2})(?:[.,](\d{1,3}))?\s*$""")

    /**
     * Reads the subtitle file [path] (at most [MAX_FILE_BYTES]); the planner already checked that it exists.
     * zh-CN: 读取字幕文件 [path] (至多 [MAX_FILE_BYTES]); 规划器已检查它存在.
     */
    fun read(path: String, warnings: SubtitleWarnings = SubtitleWarnings()): SubtitleTrack {
        val file = File(path)
        val length = file.length()
        if (length > MAX_FILE_BYTES) throw OperationOptions.limit("the subtitle file $path has $length bytes, at most $MAX_FILE_BYTES")
        val bytes = try {
            file.readBytes()
        } catch (e: IOException) {
            throw MediaStudioException.io("cannot read $path (${e.message ?: e.javaClass.simpleName})")
        }
        return parse(decode(bytes, file.name, warnings), file.name, warnings)
    }

    /** The cues of [source]; cue arrays use SRT markup. zh-CN: [source] 的 cue; cue 数组按 SRT 标记解析. */
    fun load(source: SubtitleSource, warnings: SubtitleWarnings = SubtitleWarnings()): SubtitleTrack = when (source) {
        is SubtitleSource.File -> read(source.path, warnings)
        is SubtitleSource.Cues -> SubtitleTrack(SubtitleFormat.SRT, source.cues)
    }

    /** Decodes [bytes] (BOM aware, UTF-8 by default). zh-CN: 解码 [bytes] (识别 BOM, 默认 UTF-8). */
    fun decode(bytes: ByteArray, name: String, warnings: SubtitleWarnings): String {
        fun startsWith(vararg prefix: Int) = bytes.size >= prefix.size && prefix.indices.all { (bytes[it].toInt() and 0xFF) == prefix[it] }
        val text = when {
            startsWith(0xEF, 0xBB, 0xBF) -> String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            startsWith(0xFF, 0xFE) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            startsWith(0xFE, 0xFF) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            else -> try {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString()
            } catch (e: CharacterCodingException) {
                warnings.note("$name is not valid UTF-8; the invalid bytes were replaced (save the file as UTF-8)")
                String(bytes, Charsets.UTF_8)
            }
        }
        return text.removePrefix("﻿")
    }

    /**
     * Parses [text] of the file [name]: the extension decides (`.srt`, `.ass`, `.ssa`), unless the text starts with
     * `[Script Info]`; other names are recognized by their content.
     * zh-CN: 解析文件 [name] 的 [text]: 按扩展名 (`.srt`, `.ass`, `.ssa`) 决定格式, 文本以 `[Script Info]` 开头时按 ASS; 其他文件名按内容识别.
     */
    fun parse(text: String, name: String, warnings: SubtitleWarnings = SubtitleWarnings()): SubtitleTrack {
        val looksAss = text.trimStart().startsWith("[Script Info]", ignoreCase = true)
        val format = when {
            looksAss -> SubtitleFormat.ASS
            else -> SubtitleFormat.ofFileName(name) ?: if (text.lineSequence().any { SRT_TIMING.matches(it) }) SubtitleFormat.SRT else null
        } ?: throw MediaStudioException.unsupported("cannot read $name as SRT or ASS subtitles")
        val track = if (format == SubtitleFormat.SRT) parseSrt(text, warnings) else parseAss(text, warnings)
        if (track.cues.isEmpty()) throw MediaStudioException.unsupported("$name holds no subtitle cues")
        return track
    }

    fun parseSrt(text: String, warnings: SubtitleWarnings = SubtitleWarnings()): SubtitleTrack {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val cues = mutableListOf<SubtitleCue>()
        var timing: Pair<Long, Long>? = null
        var timingLine = 0
        val body = mutableListOf<String>()

        fun finish(nextIsCue: Boolean) {
            // A number right before the next timing line is the number of that cue. zh-CN: 紧挨下一行时间轴的数字是下一条 cue 的序号.
            if (nextIsCue && body.isNotEmpty() && body.last().trim().let { it.isNotEmpty() && it.all(Char::isDigit) }) body.removeAt(body.lastIndex)
            val (start, end) = timing ?: return
            val content = body.map { it.trimEnd() }.filter { it.isNotBlank() }.joinToString("\n")
            when {
                end <= start -> warnings.note("the cue at line $timingLine ends before it starts; it was skipped")
                content.isEmpty() -> warnings.note("the cue at line $timingLine has no text; it was skipped")
                else -> cues += SubtitleCue(start, end, content, SubtitleMarkup.SRT)
            }
        }

        lines.forEachIndexed { index, line ->
            val match = SRT_TIMING.matchEntire(line)
            if (match == null) {
                if (timing != null || body.isNotEmpty()) body += line
                return@forEachIndexed
            }
            finish(nextIsCue = true)
            val g = match.groupValues
            timing = millis(g[1], g[2], g[3], g[4]) to millis(g[5], g[6], g[7], g[8])
            timingLine = index + 1
            body.clear()
        }
        finish(nextIsCue = false)
        return SubtitleTrack(SubtitleFormat.SRT, cues, warnings = warnings.list())
    }

    private fun millis(hours: String, minutes: String, seconds: String, fraction: String): Long =
        ((hours.ifEmpty { "0" }.toLong() * 60 + minutes.toLong()) * 60 + seconds.toLong()) * 1000 + fraction.padEnd(3, '0').toLong()

    /** `H:MM:SS.cc` of ASS (centiseconds; up to 3 digits accepted). zh-CN: ASS 的 `H:MM:SS.cc` (百分秒; 接受至多 3 位). */
    private fun assMillis(text: String): Long? = ASS_TIME.matchEntire(text)?.groupValues?.let { g -> millis(g[1], g[2], g[3], g[4].ifEmpty { "0" }.let { f -> if (f.length == 2) f + "0" else f }) }

    fun parseAss(text: String, warnings: SubtitleWarnings = SubtitleWarnings()): SubtitleTrack {
        var section = ""
        var playResX: Int? = null
        var playResY: Int? = null
        var wrapStyle = 0
        var styleFormat: List<String>? = null
        var eventFormat: List<String>? = null
        var legacy = false
        val styles = LinkedHashMap<String, AssStyle>()
        val cues = mutableListOf<SubtitleCue>()
        val ignoredFields = LinkedHashSet<String>()
        var effects = 0
        text.replace("\r\n", "\n").replace('\r', '\n').split('\n').forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("!:")) return@forEachIndexed
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.lowercase(Locale.ROOT)
                if (section == "[v4 styles]") legacy = true
                return@forEachIndexed
            }
            val colon = line.indexOf(':')
            if (colon < 0) return@forEachIndexed
            val key = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()
            when (section) {
                "[script info]" -> when (key.lowercase(Locale.ROOT)) {
                    "playresx" -> playResX = value.toIntOrNull()?.takeIf { it > 0 }
                    "playresy" -> playResY = value.toIntOrNull()?.takeIf { it > 0 }
                    "wrapstyle" -> wrapStyle = value.toIntOrNull() ?: 0
                }
                "[v4+ styles]", "[v4 styles]" -> when (key.lowercase(Locale.ROOT)) {
                    "format" -> styleFormat = value.split(',').map { it.trim().lowercase(Locale.ROOT) }
                    "style" -> {
                        val format = styleFormat ?: DEFAULT_STYLE_FORMAT
                        val fields = value.split(',', limit = format.size).map { it.trim() }
                        if (fields.size < format.size) {
                            warnings.note("the style at line ${index + 1} has ${fields.size} of ${format.size} fields; it was skipped")
                        } else {
                            val style = style(format.zip(fields).toMap(), legacy, ignoredFields)
                            styles[style.name] = style
                        }
                    }
                }
                "[events]" -> when (key.lowercase(Locale.ROOT)) {
                    "format" -> eventFormat = value.split(',').map { it.trim().lowercase(Locale.ROOT) }
                    "dialogue" -> {
                        val format = eventFormat ?: DEFAULT_EVENT_FORMAT
                        val fields = value.split(',', limit = format.size)
                        if (fields.size < format.size) {
                            warnings.note("the dialogue at line ${index + 1} has ${fields.size} of ${format.size} fields; it was skipped")
                            return@forEachIndexed
                        }
                        val map = format.zip(fields).toMap()
                        val start = map["start"]?.let(::assMillis)
                        val end = map["end"]?.let(::assMillis)
                        val body = map["text"].orEmpty()
                        when {
                            start == null || end == null -> warnings.note("the dialogue at line ${index + 1} has no valid times; it was skipped")
                            end <= start -> warnings.note("the dialogue at line ${index + 1} ends before it starts; it was skipped")
                            body.isBlank() -> Unit
                            else -> {
                                if (map["effect"].orEmpty().isNotBlank()) effects++
                                cues += SubtitleCue(
                                    startMillis = start,
                                    endMillis = end,
                                    text = body,
                                    markup = SubtitleMarkup.ASS,
                                    style = map["style"]?.trim(),
                                    layer = map["layer"]?.trim()?.toIntOrNull() ?: 0,
                                    marginL = map["marginl"]?.trim()?.toIntOrNull() ?: 0,
                                    marginR = map["marginr"]?.trim()?.toIntOrNull() ?: 0,
                                    marginV = map["marginv"]?.trim()?.toIntOrNull() ?: 0,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (ignoredFields.isNotEmpty()) warnings.note("ASS style fields that 3-Slate Studio 1.0.0 does not render were ignored: ${ignoredFields.joinToString()}")
        if (effects > 0) warnings.note("ASS dialogue effects (Banner, Scroll) are not rendered; $effects lines show without them")
        // libass derives a missing PlayRes from the other one (4:3). zh-CN: libass 由另一个值推出缺少的 PlayRes (4:3).
        val (x, y) = when {
            playResX == null && playResY == null -> DEFAULT_PLAY_RES_X to DEFAULT_PLAY_RES_Y
            playResX == null -> playResY!! * 4 / 3 to playResY!!
            playResY == null -> playResX!! to if (playResX == 1280) 1024 else playResX!! * 3 / 4
            else -> playResX!! to playResY!!
        }
        return SubtitleTrack(SubtitleFormat.ASS, cues, styles, x, y, wrapStyle, warnings.list())
    }

    private fun style(fields: Map<String, String>, legacy: Boolean, ignored: MutableSet<String>): AssStyle {
        fun number(key: String) = fields[key]?.toDoubleOrNull()
        fun flag(key: String) = fields[key]?.toIntOrNull()?.let { it != 0 }
        fun color(key: String) = fields[key]?.let(AssColors::parse)
        if (number("scalex")?.let { it != 100.0 } == true) ignored += "ScaleX"
        if (number("scaley")?.let { it != 100.0 } == true) ignored += "ScaleY"
        if (number("spacing")?.let { it != 0.0 } == true) ignored += "Spacing"
        if (number("angle")?.let { it != 0.0 } == true) ignored += "Angle"
        if (flag("underline") == true) ignored += "Underline"
        if (flag("strikeout") == true) ignored += "StrikeOut"
        val defaults = AssStyle(name = "")
        val alignment = fields["alignment"]?.toIntOrNull()?.let { if (legacy) legacyAlignment(it) else it }?.takeIf { it in 1..9 } ?: defaults.alignment
        return AssStyle(
            name = fields["name"]?.removePrefix("*").orEmpty().ifEmpty { SubtitleTrack.DEFAULT_STYLE },
            font = fields["fontname"]?.ifEmpty { null } ?: defaults.font,
            fontSize = number("fontsize")?.takeIf { it > 0 } ?: defaults.fontSize,
            primaryColor = color("primarycolour") ?: defaults.primaryColor,
            outlineColor = color("outlinecolour") ?: color("tertiarycolour") ?: defaults.outlineColor,
            backColor = color("backcolour") ?: defaults.backColor,
            bold = flag("bold") ?: defaults.bold,
            italic = flag("italic") ?: defaults.italic,
            borderStyle = fields["borderstyle"]?.toIntOrNull() ?: defaults.borderStyle,
            outline = number("outline")?.coerceAtLeast(0.0) ?: defaults.outline,
            shadow = number("shadow")?.coerceAtLeast(0.0) ?: defaults.shadow,
            alignment = alignment,
            marginL = number("marginl") ?: defaults.marginL,
            marginR = number("marginr") ?: defaults.marginR,
            marginV = number("marginv") ?: defaults.marginV,
        )
    }

    /** SSA v4 alignment: 1 - 3 bottom, 5 - 7 top, 9 - 11 middle. zh-CN: SSA v4 的对齐: 1 - 3 底部, 5 - 7 顶部, 9 - 11 中部. */
    private fun legacyAlignment(value: Int): Int = when (value) {
        in 1..3 -> value
        in 5..7 -> value + 2
        in 9..11 -> value - 5
        else -> 2
    }

    private val DEFAULT_STYLE_FORMAT = listOf(
        "name", "fontname", "fontsize", "primarycolour", "secondarycolour", "outlinecolour", "backcolour", "bold", "italic",
        "underline", "strikeout", "scalex", "scaley", "spacing", "angle", "borderstyle", "outline", "shadow", "alignment",
        "marginl", "marginr", "marginv", "encoding",
    )

    private val DEFAULT_EVENT_FORMAT = listOf("layer", "start", "end", "style", "name", "marginl", "marginr", "marginv", "effect", "text")

    /** The display text of [cue] without markup. zh-CN: 去掉标记后的 [cue] 显示文本. */
    fun plainText(cue: SubtitleCue, wrapStyle: Int = 0): String = SubtitleMarkups.parse(cue.text, cue.markup, wrapStyle).plain

    /**
     * SRT: numbered from 1, `HH:MM:SS,mmm`, CRLF line ends, UTF-8 without BOM; empty lines inside a text would end the
     * cue for other readers, so they are dropped.
     * zh-CN: SRT: 序号从 1 起, `HH:MM:SS,mmm`, CRLF 换行, 无 BOM 的 UTF-8; 文本中的空行会让其他读取器提前结束 cue, 因此去掉.
     */
    fun writeSrt(cues: List<SubtitleCue>): String = buildString {
        cues.forEachIndexed { index, cue ->
            append(index + 1).append(CRLF)
            append(srtTime(cue.startMillis)).append(" --> ").append(srtTime(cue.endMillis)).append(CRLF)
            cue.text.replace("\r\n", "\n").replace('\r', '\n').split('\n').filter { it.isNotBlank() }.forEach { append(it).append(CRLF) }
            append(CRLF)
        }
    }

    /**
     * ASS: a 1920 x 1080 script with one `Default` style matching the burn defaults (font size = height / 18, outline,
     * bottom center, margins 5 %); times are rounded to centiseconds, line breaks become `\N`.
     * zh-CN: ASS: 1920 x 1080 的脚本, 一个与烧录默认值一致的 `Default` 样式 (字号 = 高 / 18, 描边, 底部居中, 边距 5 %); 时间四舍五入到
     * 百分秒, 换行写成 `\N`.
     */
    fun writeAss(cues: List<SubtitleCue>): String = buildString {
        val crlf = CRLF
        append("[Script Info]").append(crlf)
        append("; Script generated by 3-Slate Studio").append(crlf)
        append("ScriptType: v4.00+").append(crlf)
        append("PlayResX: 1920").append(crlf)
        append("PlayResY: 1080").append(crlf)
        append("WrapStyle: 0").append(crlf)
        append("ScaledBorderAndShadow: yes").append(crlf).append(crlf)
        append("[V4+ Styles]").append(crlf)
        append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding").append(crlf)
        append("Style: Default,sans-serif,60,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,3.75,0,2,96,96,54,1").append(crlf).append(crlf)
        append("[Events]").append(crlf)
        append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text").append(crlf)
        cues.forEach { cue ->
            val text = cue.text.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\\N")
            append("Dialogue: ${cue.layer},${assTime(cue.startMillis)},${assTime(cue.endMillis)},${cue.style ?: SubtitleTrack.DEFAULT_STYLE},,0,0,0,,$text").append(crlf)
        }
    }

    fun srtTime(millis: Long): String {
        val h = millis / 3_600_000
        val m = millis / 60_000 % 60
        val s = millis / 1000 % 60
        return "%02d:%02d:%02d,%03d".format(Locale.ROOT, h, m, s, millis % 1000)
    }

    fun assTime(millis: Long): String {
        val cs = (millis / 10.0).roundToLong()
        return "%d:%02d:%02d.%02d".format(Locale.ROOT, cs / 360_000, cs / 6000 % 60, cs / 100 % 60, cs % 100)
    }

    private const val CRLF = "\r\n"
}

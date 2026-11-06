package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SRT and ASS reading and writing, the inline markup subset, style resolution and the subtitle options (roadmap P3.3,
 * D31).
 * zh-CN: SRT 与 ASS 的读写, 内联标记子集, 样式解析与字幕选项 (路线图 P3.3, D31).
 */
class SubtitlesTest {

    private fun srt(text: String, warnings: SubtitleWarnings = SubtitleWarnings()) = SubtitleFiles.parseSrt(text, warnings)

    private fun times(track: SubtitleTrack) = track.cues.map { it.startMillis to it.endMillis }

    @Test
    fun `SRT accepts a BOM, CRLF, CR and stray empty lines`() {
        val body = "1\r\n00:00:01,000 --> 00:00:02,500\r\nHello\r\nworld\r\n\r\n\r\n\r\n2\r\n00:00:03,000 --> 00:00:04,000\r\nSecond\r\n"
        val utf8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + body.toByteArray()
        val warnings = SubtitleWarnings()
        val track = SubtitleFiles.parse(SubtitleFiles.decode(utf8, "a.srt", warnings), "a.srt", warnings)
        assertEquals(SubtitleFormat.SRT, track.format)
        assertEquals(listOf(1000L to 2500L, 3000L to 4000L), times(track))
        assertEquals(listOf("Hello\nworld", "Second"), track.cues.map { it.text })
        assertEquals(emptyList<String>(), warnings.list())
        // UTF-16 with a BOM, CR only line ends. zh-CN: 带 BOM 的 UTF-16, 只有 CR 的换行.
        val utf16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + body.replace("\r\n", "\r").toByteArray(Charsets.UTF_16LE)
        assertEquals(track.cues, SubtitleFiles.parse(SubtitleFiles.decode(utf16, "a.srt", warnings), "a.srt").cues)
        // Leading empty lines, a missing number, a missing empty line before the next number, empty lines inside a text, dot milliseconds.
        // zh-CN: 开头的空行, 缺少序号, 下一个序号前缺少空行, 文本中的空行, 用点分隔毫秒.
        val loose = srt("\n\n00:00:01.5 --> 00:00:02,000\nA\n\nstill A\n7\n00:00:05,000 --> 0:00:06,000\nB\n\n\n")
        assertEquals(listOf(1500L to 2000L, 5000L to 6000L), times(loose))
        assertEquals(listOf("A\nstill A", "B"), loose.cues.map { it.text })
        // A cue whose text is a number keeps it. zh-CN: 文本为数字的 cue 保留该数字.
        assertEquals(listOf("42"), srt("1\n00:00:01,000 --> 00:00:02,000\n42\n\n").cues.map { it.text })
        // Hours above 99 and minutes without hours. zh-CN: 超过 99 的小时与不带小时的分钟.
        assertEquals(listOf(360_000_000L to 360_001_000L, 61_000L to 62_000L), times(srt("100:00:00,000 --> 100:00:01,000\nx\n\n01:01,000 --> 01:02,000\ny\n")))
    }

    @Test
    fun `SRT keeps overlapping cues and skips broken ones with a warning`() {
        val warnings = SubtitleWarnings()
        val track = srt(
            "1\n00:00:01,000 --> 00:00:05,000\nFirst\n\n2\n00:00:02,000 --> 00:00:03,000\nOverlap\n\n" +
                "3\n00:00:04,000 --> 00:00:04,000\nZero\n\n4\n00:00:06,000 --> 00:00:07,000\n\n5\n00:00:08,000 --> 00:00:09,000\nLast\n",
            warnings,
        )
        assertEquals(listOf(1000L to 5000L, 2000L to 3000L, 8000L to 9000L), times(track))
        assertEquals(
            listOf("the cue at line 10 ends before it starts; it was skipped", "the cue at line 14 has no text; it was skipped"),
            warnings.list(),
        )
        val invalid = SubtitleWarnings()
        SubtitleFiles.decode(byteArrayOf(0x41, 0xC3.toByte(), 0x28), "b.srt", invalid)
        assertEquals(listOf("b.srt is not valid UTF-8; the invalid bytes were replaced (save the file as UTF-8)"), invalid.list())
        val none = assertThrows(MediaStudioException::class.java) { SubtitleFiles.parse("nothing here", "c.srt") }
        assertEquals(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, none.code)
        assertEquals("c.srt holds no subtitle cues", none.detail)
        assertEquals("cannot read d.txt as SRT or ASS subtitles", assertThrows(MediaStudioException::class.java) { SubtitleFiles.parse("hello", "d.txt") }.detail)
        // Content decides for other extensions. zh-CN: 其他扩展名按内容判断.
        assertEquals(SubtitleFormat.SRT, SubtitleFiles.parse("00:00:01,000 --> 00:00:02,000\nx\n", "d.txt").format)
    }

    @Test
    fun `SRT markup becomes runs`() {
        val rich = SubtitleMarkups.parse("<b>Bold</b> and <i>it<u>al</u></i> <font color=\"#FF0000\">red <font color=blue>blue</font> red</font> < 3", SubtitleMarkup.SRT)
        assertEquals("Bold and ital red blue red < 3", rich.plain)
        assertEquals(TextRun("Bold", bold = true), rich.runs[0])
        assertEquals(TextRun("al", italic = true, underline = true), rich.runs.first { it.text == "al" })
        assertEquals(0xFFFF0000.toInt(), rich.runs.first { it.text == "red " }.color)
        assertEquals(0xFF0000FF.toInt(), rich.runs.first { it.text == "blue" }.color)
        assertEquals(0xFFFF0000.toInt(), rich.runs.first { it.text == " red" }.color)
        assertNull(rich.runs.last().color)
        // ASS blocks are common in SRT files. zh-CN: SRT 文件中常见 ASS 块.
        val top = SubtitleMarkups.parse("{\\an8}On top", SubtitleMarkup.SRT)
        assertEquals(8, top.alignment)
        assertEquals("On top", top.plain)
    }

    private val ass = """
        |[Script Info]
        |ScriptType: v4.00+
        |PlayResX: 1280
        |PlayResY: 720
        |
        |[V4+ Styles]
        |Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        |Style: Default,serif,48,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,2,1,2,20,20,30,1
        |Style: Sign,monospace,36,&H0000FFFF,&H000000FF,&H00FF0000,&H00000000,0,-1,0,0,120,100,0,0,3,4,0,8,10,10,10,1
        |
        |[Events]
        |Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        |Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,not shown
        |Dialogue: 0,0:00:01.00,0:00:03.50,Default,,0,0,0,,Hello, world\Nsecond line
        |Dialogue: 1,0:00:02.00,0:00:04.00,Sign,,0,0,50,,{\b1}Bold{\b0} {\c&H0000FF&\fs20}red{\c} {\i1}it
        |Dialogue: 0,0:00:05.00,0:00:06.00,Missing,,0,0,0,Banner;5,{\an7\pos(100,200)\an3\k20}Ka{\k30}ra{\t(0,500,\fs80)\move(1,2,3,4)}oke{\p1}m 0 0 l 1 1{\p0} end\hspace\nsoft
        |Dialogue: 0,0:00:07.00,0:00:06.00,Default,,0,0,0,,backwards
    """.trimMargin()

    @Test
    fun `ASS styles, dialogue fields and the tag subset`() {
        val warnings = SubtitleWarnings()
        val track = SubtitleFiles.parse(ass, "a.ass", warnings)
        assertEquals(SubtitleFormat.ASS, track.format)
        assertEquals(1280 to 720, track.playResX to track.playResY)
        assertEquals(listOf(1000L to 3500L, 2000L to 4000L, 5000L to 6000L), times(track))
        val default = track.styles.getValue("Default")
        assertEquals(AssStyle("Default", "serif", 48.0, Colors.WHITE, Colors.BLACK, 0x7F000000, bold = true, italic = false, borderStyle = 1, outline = 2.0, shadow = 1.0, alignment = 2, marginL = 20.0, marginR = 20.0, marginV = 30.0), default)
        val sign = track.styles.getValue("Sign")
        assertEquals(0xFFFFFF00.toInt(), sign.primaryColor)
        assertEquals(0xFF0000FF.toInt(), sign.outlineColor)
        assertEquals(3 to 8, sign.borderStyle to sign.alignment)
        // Commas stay in the text; the escapes. zh-CN: 文本中的逗号保留; 转义.
        assertEquals("Hello, world\nsecond line", SubtitleFiles.plainText(track.cues[0]))
        assertEquals(Triple("Sign", 1, 50), Triple(track.cues[1].style, track.cues[1].layer, track.cues[1].marginV))
        // Style inheritance: an unknown style falls back to Default. zh-CN: 样式继承: 未知样式退回 Default.
        assertEquals(default, track.styleOf(track.cues[2]))
        assertEquals(sign, track.styleOf(track.cues[1]))

        // Tag overrides: b, c, fs, i; a tag without value returns to the style. zh-CN: 标签覆盖: b, c, fs, i; 不带值的标签恢复样式.
        val rich = SubtitleMarkups.parse(track.cues[1].text, SubtitleMarkup.ASS, track.wrapStyle, warnings)
        assertEquals(
            listOf(
                TextRun("Bold", bold = true),
                TextRun(" ", bold = false),
                TextRun("red", bold = false, color = 0xFFFF0000.toInt(), fontSize = 20.0),
                TextRun(" ", bold = false, fontSize = 20.0),
                TextRun("it", bold = false, italic = true, fontSize = 20.0),
            ),
            rich.runs,
        )
        // The first an and pos win, the rest is ignored and reported; drawing commands are hidden.
        // zh-CN: 第一个 an 与 pos 生效, 其余忽略并报告; 绘图命令被隐藏.
        val karaoke = SubtitleMarkups.parse(track.cues[2].text, SubtitleMarkup.ASS, track.wrapStyle, warnings)
        assertEquals(7, karaoke.alignment)
        assertEquals(100.0 to 200.0, karaoke.position)
        assertEquals("Karaoke end space soft", karaoke.plain)
        assertEquals(
            listOf(
                "ASS override tags that 3-Slate Studio 1.0.0 does not render were ignored: \\k (2), \\t (1), \\move (1), \\p (2)",
                "the dialogue at line 17 ends before it starts; it was skipped",
                "ASS style fields that 3-Slate Studio 1.0.0 does not render were ignored: ScaleX",
                "ASS dialogue effects (Banner, Scroll) are not rendered; 1 lines show without them",
            ),
            warnings.list(),
        )
        // WrapStyle 2 turns \n into a line break. zh-CN: WrapStyle 2 时 \n 也换行.
        assertEquals("a\nb", SubtitleMarkups.parse("a\\nb", SubtitleMarkup.ASS, wrapStyle = 2).plain)
    }

    @Test
    fun `SSA styles and default script sizes`() {
        val ssa = """
            |[Script Info]
            |ScriptType: v4.00
            |
            |[V4 Styles]
            |Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding
            |Style: *Default,Arial,20,16777215,65535,255,0,0,0,1,2,2,6,10,10,10,0,1
            |
            |[Events]
            |Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            |Dialogue: Marked=0,0:00:01.00,0:00:02.00,*Default,,0000,0000,0000,,Top
        """.trimMargin()
        val track = SubtitleFiles.parse(ssa, "a.ssa")
        assertEquals(384 to 288, track.playResX to track.playResY)
        val style = track.styles.getValue("Default")
        assertEquals(8, style.alignment)
        assertEquals(0xFFFF0000.toInt(), style.outlineColor)
        assertEquals(style, track.styleOf(track.cues.single()))
        assertEquals(960 to 720, SubtitleFiles.parseAss("[Script Info]\nPlayResY: 720\n").let { it.playResX to it.playResY })
        assertEquals(1280 to 1024, SubtitleFiles.parseAss("[Script Info]\nPlayResX: 1280\n").let { it.playResX to it.playResY })
    }

    @Test
    fun `styles scale from the script and the script style overrides them`() {
        val track = SubtitleFiles.parse(ass, "a.ass")
        val sign = SubtitleStyles.resolve(track, track.cues[1], SubtitleStyleOptions(), 1920, 1080)
        assertEquals(36.0 * 1.5, sign.fontSize, 1e-9)
        assertEquals(0xFF0000FF.toInt(), sign.boxColor)
        assertNull("BorderStyle 3 draws a box, no outline", sign.strokeColor)
        assertEquals(10.0 * 1.5 to 50.0 * 1.5, sign.marginL to sign.marginV)
        val default = SubtitleStyles.resolve(track, track.cues[0], SubtitleStyleOptions(), 1920, 1080)
        assertEquals(Colors.BLACK, default.strokeColor)
        assertEquals(2.0 * 1.5 * 2, default.strokeWidth, 1e-9)
        assertEquals(0x7F000000, default.shadowColor)
        assertEquals(1.5 to 1.5, default.shadowDx to default.shadowDy)
        val overridden = SubtitleStyles.resolve(
            track,
            track.cues[0],
            SubtitleStyleOptions(fontSize = 30.0, color = 0xFF00FF00.toInt(), strokeWidth = 0.0, align = Anchor.TOP, marginV = Length.Pixels(12.0)),
            1920,
            1080,
        )
        assertEquals(30.0, overridden.fontSize, 1e-9)
        assertEquals(0xFF00FF00.toInt(), overridden.color)
        assertNull(overridden.strokeColor)
        assertEquals(8 to 12.0, overridden.alignment to overridden.marginV)
        assertTrue("bold of the ASS style is kept", overridden.bold)
        // SRT defaults: height / 18, outline font size / 8, bottom center, margins 5 %. zh-CN: SRT 默认值.
        val plain = SubtitleTrack(SubtitleFormat.SRT, listOf(SubtitleCue(0, 1000, "x")))
        val srt = SubtitleStyles.resolve(plain, plain.cues[0], SubtitleStyleOptions(), 1280, 720)
        assertEquals(40.0, srt.fontSize, 1e-9)
        assertEquals(5.0 to Colors.BLACK, srt.strokeWidth to srt.strokeColor)
        assertEquals(Triple(2, 64.0, 36.0), Triple(srt.alignment, srt.marginL, srt.marginV))
        assertNull(srt.shadowColor)
        val boxed = SubtitleStyles.resolve(plain, plain.cues[0], SubtitleStyleOptions(fontSize = 64.0, background = 0x80000000.toInt(), shadowDx = 3.0, marginH = Length.Percent(10.0)), 1280, 720)
        assertEquals(8.0, boxed.strokeWidth, 1e-9)
        assertEquals(0x80000000.toInt(), boxed.boxColor)
        assertEquals(Colors.BLACK to 3.0, boxed.shadowColor to boxed.shadowDx)
        assertEquals(128.0 to 128.0, boxed.marginL to boxed.marginR)
    }

    @Test
    fun `SRT and ASS writers round trip`() {
        val cues = listOf(
            SubtitleCue(0, 1_234, "One"),
            SubtitleCue(3_600_000 + 61_005, 3_600_000 + 62_999, "Two\nlines"),
            SubtitleCue(500, 900, "Gap\n\nremoved"),
        )
        val srt = SubtitleFiles.writeSrt(cues)
        assertTrue(srt.startsWith("1\r\n00:00:00,000 --> 00:00:01,234\r\nOne\r\n\r\n2\r\n01:01:01,005 --> 01:01:02,999\r\nTwo\r\nlines\r\n\r\n"))
        assertEquals(cues.map { it.copy(text = it.text.replace("\n\n", "\n")) }, SubtitleFiles.parseSrt(srt).cues)
        val ass = SubtitleFiles.writeAss(cues)
        assertTrue(ass.contains("Dialogue: 0,1:01:01.01,1:01:03.00,Default,,0,0,0,,Two\\Nlines\r\n"))
        val read = SubtitleFiles.parse(ass, "out.ass")
        assertEquals(listOf(0L to 1230L, 3_661_010L to 3_663_000L, 500L to 900L), times(read))
        assertEquals(listOf("One", "Two\nlines", "Gap\n\nremoved"), read.cues.map { SubtitleFiles.plainText(it) })
        assertEquals(1920 to 1080, read.playResX to read.playResY)
        assertEquals(60.0, read.styles.getValue("Default").fontSize, 1e-9)
        assertEquals("1:01:01.01", SubtitleFiles.assTime(3_661_005))
    }

    @Test
    fun `ASS colors`() {
        assertEquals(0xFFFF0000.toInt(), AssColors.parse("&H0000FF&"))
        assertEquals(0x80112233.toInt(), AssColors.parse("&H7F332211"))
        assertEquals(0xFF0000FF.toInt(), AssColors.parse("16711680"))
        assertNull(AssColors.parse("&Hxyz"))
        assertEquals("&H00332211", AssColors.format(0xFF112233.toInt()))
    }

    private fun invalid(op: String, options: Map<String, Any?>, inputs: Int = 1): String {
        val error = assertThrows(MediaStudioException::class.java) { OperationOptions.parse(op, inputs, options) }
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, error.code)
        return error.detail
    }

    private fun cue(start: Long = 0, end: Long = 1000, text: String = "x") = mapOf("start" to start, "end" to end, "text" to text)

    @Test
    fun `subtitle operation options`() {
        val burn = OperationOptions.parse(MediaStudioOps.BURN_SUBTITLES, 1, mapOf("subtitles" to "/a.srt", "style" to mapOf("fontSize" to 40L, "align" to "top", "codec" to "h265", "marginV" to "3%")))
            as OperationSpec.BurnSubtitles
        assertEquals(SubtitleSource.File("/a.srt"), burn.subtitles.source)
        assertEquals(VideoCodec.H265, burn.video.codec)
        assertEquals(SubtitleStyleOptions(fontSize = 40.0, align = Anchor.TOP, marginV = Length.Percent(3.0)), burn.subtitles.style)
        assertEquals(listOf("/a.srt"), OperationOptions.resources(burn))
        val cues = OperationOptions.parse(MediaStudioOps.BURN_SUBTITLES, 1, mapOf("cues" to listOf(cue(), cue(500, 1500, "<i>y</i>")))) as OperationSpec.BurnSubtitles
        assertEquals(SubtitleSource.Cues(listOf(SubtitleCue(0, 1000, "x"), SubtitleCue(500, 1500, "<i>y</i>"))), cues.subtitles.source)
        assertEquals("burnSubtitles needs options.subtitles or options.cues", invalid(MediaStudioOps.BURN_SUBTITLES, emptyMap()))
        assertEquals("options.subtitles and options.cues are exclusive", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("subtitles" to "/a.srt", "cues" to listOf(cue()))))
        assertEquals("options.subtitles must be an absolute path, got a.srt", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("subtitles" to "a.srt")))
        assertEquals("burnSubtitles takes exactly 1 input, got 2", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("subtitles" to "/a.srt"), inputs = 2))
        assertEquals("options.cues[0].end (1000) must be greater than options.cues[0].start (1000)", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("cues" to listOf(cue(1000, 1000)))))
        assertEquals("options.cues[1].text must have 1 to 1000 characters and not be blank, got 1", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("cues" to listOf(cue(), cue(text = " ")))))
        assertEquals("unknown option options.cues[0].style", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("cues" to listOf(cue() + ("style" to "x")))))
        assertEquals("options.cues must hold at least 1 cue", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("cues" to emptyList<Any>())))
        assertEquals("options.style.align must be one of center, top-left, top, top-right, left, right, bottom-left, bottom, bottom-right, got middle", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("subtitles" to "/a.srt", "style" to mapOf("align" to "middle"))))
        assertEquals("options.style.marginV must be at least 0% and at most 50%, got 60%", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("subtitles" to "/a.srt", "style" to mapOf("marginV" to "60%"))))
        assertEquals("unknown option options.style.text", invalid(MediaStudioOps.BURN_SUBTITLES, mapOf("subtitles" to "/a.srt", "style" to mapOf("text" to "x"))))
        val many = assertThrows(MediaStudioException::class.java) {
            OperationOptions.parse(MediaStudioOps.BURN_SUBTITLES, 1, mapOf("cues" to List(SubtitleOptions.MAX_CUES + 1) { cue() }))
        }
        assertEquals(MediaStudioErrorCodes.LIMIT_EXCEEDED to "options.cues holds 10001 cues, at most 10000", many.code to many.detail)

        assertEquals(OperationSpec.SubtitlesParse, OperationOptions.parse(MediaStudioOps.SUBTITLES_PARSE, 1, emptyMap()))
        assertEquals("unknown option options.format", invalid(MediaStudioOps.SUBTITLES_PARSE, mapOf("format" to "srt")))
        val write = OperationOptions.parse(MediaStudioOps.SUBTITLES_WRITE, 0, mapOf("cues" to listOf(cue()), "format" to "ass")) as OperationSpec.SubtitlesWrite
        assertEquals(SubtitleFormat.ASS, write.format)
        assertEquals("subtitlesWrite takes no inputs, got 1", invalid(MediaStudioOps.SUBTITLES_WRITE, mapOf("cues" to listOf(cue()), "format" to "srt"), inputs = 1))
        assertEquals("subtitlesWrite needs options.format or an output ending with .srt or .ass", invalid(MediaStudioOps.SUBTITLES_WRITE, mapOf("cues" to listOf(cue())), inputs = 0))
        assertEquals("options.format must be one of srt, ass, got vtt", invalid(MediaStudioOps.SUBTITLES_WRITE, mapOf("cues" to listOf(cue()), "format" to "vtt"), inputs = 0))
    }
}

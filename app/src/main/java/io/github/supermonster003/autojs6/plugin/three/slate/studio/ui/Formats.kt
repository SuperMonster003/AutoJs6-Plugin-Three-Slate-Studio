package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui

import android.content.Context
import android.text.format.Formatter
import java.util.Locale

/**
 * Number formats of the Studio screens. Times, dimensions and rates use ASCII digits and separators in every language,
 * like the script values they mirror; file sizes follow the system formatter of the language.
 * zh-CN: Studio 界面的数值格式. 时间, 尺寸与速率在所有语言中都用 ASCII 数字与分隔符, 与它们对应的脚本值一致; 文件大小沿用系统按
 * 语言的格式.
 */
internal object Formats {

    /** `m:ss.S` below an hour, `h:mm:ss` from an hour. zh-CN: 一小时以内为 `m:ss.S`, 一小时起为 `h:mm:ss`. */
    fun duration(millis: Long): String {
        val total = millis.coerceAtLeast(0)
        val hours = total / 3_600_000
        val minutes = total / 60_000 % 60
        val seconds = total / 1000 % 60
        val tenths = total / 100 % 10
        return ltr(
            if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
            else String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, tenths),
        )
    }

    fun size(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes.coerceAtLeast(0))

    fun resolution(width: Int, height: Int): String = ltr("$width x $height")

    fun bitRate(bitsPerSecond: Long): String = ltr(
        when {
            bitsPerSecond >= 1_000_000 -> String.format(Locale.ROOT, "%.1f Mbps", bitsPerSecond / 1_000_000.0)
            bitsPerSecond >= 1_000 -> String.format(Locale.ROOT, "%d kbps", bitsPerSecond / 1_000)
            else -> "$bitsPerSecond bps"
        },
    )

    fun frameRate(framesPerSecond: Double): String = ltr(String.format(Locale.ROOT, "%.2f fps", framesPerSecond))

    fun sampleRate(hertz: Int): String = ltr("$hertz Hz")

    fun degrees(value: Int): String = ltr("$value deg")

    /**
     * Isolates [text] left to right (LRI ... PDI): in an Arabic line, "640 x 360" or "5.2 Mbps" would otherwise be
     * reordered by the bidirectional algorithm (numbers next to Latin letters and spaces).
     * zh-CN: 把 [text] 隔离为从左到右 (LRI ... PDI): 否则在阿拉伯语行中 "640 x 360" 或 "5.2 Mbps" 会被双向算法重排 (数字与拉丁
     * 字母, 空格相邻).
     */
    fun ltr(text: String): String = "\u2066$text\u2069"

    /**
     * Isolates [text] in the direction of its first strong character (FSI ... PDI), for paths and names inside a line
     * of the other direction: a path keeps its leading slash in Arabic, an Arabic folder name stays right to left.
     * zh-CN: 按首个强方向字符隔离 [text] (FSI ... PDI), 用于另一方向行中的路径与名称: 路径在阿拉伯语中保留开头的斜杠, 阿拉伯语
     * 目录名仍从右到左.
     */
    fun isolate(text: String): String = "\u2068$text\u2069"

    fun number(value: Double): String = if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) value.toLong().toString() else value.toString()
}

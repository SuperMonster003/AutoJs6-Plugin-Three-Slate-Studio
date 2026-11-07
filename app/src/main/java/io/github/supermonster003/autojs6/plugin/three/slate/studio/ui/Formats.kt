package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui

import android.content.Context
import android.text.format.Formatter
import java.util.Locale

/**
 * Number formats of the Studio screens. Times and sizes use ASCII digits and separators in every language, like the
 * script values they mirror.
 * zh-CN: Studio 界面的数值格式. 时间与尺寸在所有语言中都用 ASCII 数字与分隔符, 与它们对应的脚本值一致.
 */
internal object Formats {

    /** `m:ss.S` below an hour, `h:mm:ss` from an hour. zh-CN: 一小时以内为 `m:ss.S`, 一小时起为 `h:mm:ss`. */
    fun duration(millis: Long): String {
        val total = millis.coerceAtLeast(0)
        val hours = total / 3_600_000
        val minutes = total / 60_000 % 60
        val seconds = total / 1000 % 60
        val tenths = total / 100 % 10
        return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, tenths)
    }

    fun size(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes.coerceAtLeast(0))

    fun resolution(width: Int, height: Int): String = "$width x $height"

    fun bitRate(bitsPerSecond: Long): String = when {
        bitsPerSecond >= 1_000_000 -> String.format(Locale.ROOT, "%.1f Mbps", bitsPerSecond / 1_000_000.0)
        bitsPerSecond >= 1_000 -> String.format(Locale.ROOT, "%d kbps", bitsPerSecond / 1_000)
        else -> "$bitsPerSecond bps"
    }

    fun number(value: Double): String = if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) value.toLong().toString() else value.toString()
}

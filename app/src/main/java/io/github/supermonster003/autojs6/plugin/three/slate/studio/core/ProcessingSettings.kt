package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.autojs.plugin.mediastudio.api.MediaStudioLimits

/**
 * Which video encoders an export tries first (roadmap D37, appendix D Q50 (b)): [AUTO] keeps the order of media3's
 * default selector (the platform's codec list, hardware encoders usually first), [HARDWARE] and [SOFTWARE] move the
 * encoders of that kind to the front. The others stay as fallbacks, so a preference never makes an export fail.
 * zh-CN: 导出时先尝试哪类视频编码器 (路线图 D37, 附录 D Q50 (b)): [AUTO] 保持 media3 默认选择器的顺序 (平台编解码器列表, 通常硬件
 * 编码器在前), [HARDWARE] 与 [SOFTWARE] 把该类编码器移到前面. 其余编码器仍作为回退, 偏好不会让导出失败.
 */
internal enum class EncoderPreference(val id: String) {
    AUTO("auto"),
    HARDWARE("hardware"),
    SOFTWARE("software"),
    ;

    /**
     * [encoders] reordered: a stable partition by [isHardware], the preferred kind first; [AUTO] keeps the order.
     * zh-CN: 重新排序的 [encoders]: 按 [isHardware] 稳定划分, 偏好的类型在前; [AUTO] 保持原顺序.
     */
    fun <T> order(encoders: List<T>, isHardware: (T) -> Boolean): List<T> = when (this) {
        AUTO -> encoders
        HARDWARE -> encoders.partition(isHardware).let { (hardware, software) -> hardware + software }
        SOFTWARE -> encoders.partition(isHardware).let { (hardware, software) -> software + hardware }
    }

    companion object {
        fun of(id: String?): EncoderPreference = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/**
 * The processing settings of roadmap D37 that the executor reads (appendix D Q50 (b), the maintainer's choice of
 * 2026-11-07): the hardware encoder preference and the task concurrency. Unlike the wizard defaults of the Studio
 * preferences they apply to Studio and script tasks alike, since both run in this process on the same executor. Stored
 * in the Studio preferences file; the concurrency stays within 1 - `MediaStudioLimits.MAX_CONCURRENT_ENCODES` (the
 * contract limit, unchanged).
 * zh-CN: 执行器读取的路线图 D37 加工设置 (附录 D Q50 (b), 维护者 2026-11-07 选): 硬件编码器偏好与任务并发数. 与 Studio 偏好中的
 * 向导默认值不同, 它们对 Studio 任务与脚本任务同样生效, 因为两者都在本进程的同一个执行器上运行. 存在 Studio 偏好文件中; 并发数限定在
 * 1 - `MediaStudioLimits.MAX_CONCURRENT_ENCODES` (契约上限, 不变).
 */
internal class ProcessingSettings(private val prefs: SharedPreferences) {

    var encoderPreference: EncoderPreference
        get() = EncoderPreference.of(prefs.getString(KEY_ENCODER_PREFERENCE, null))
        set(value) = prefs.edit { putString(KEY_ENCODER_PREFERENCE, value.id) }

    var concurrency: Int
        get() = prefs.getInt(KEY_CONCURRENCY, MAX_CONCURRENCY).coerceIn(MIN_CONCURRENCY, MAX_CONCURRENCY)
        set(value) = prefs.edit { putInt(KEY_CONCURRENCY, value.coerceIn(MIN_CONCURRENCY, MAX_CONCURRENCY)) }

    companion object {

        /** The Studio preferences file (shared with `ui/StudioPreferences`). zh-CN: Studio 偏好文件 (与 `ui/StudioPreferences` 共用). */
        const val FILE = "studio"
        const val KEY_ENCODER_PREFERENCE = "encoderPreference"
        const val KEY_CONCURRENCY = "taskConcurrency"

        const val MIN_CONCURRENCY = 1
        const val MAX_CONCURRENCY = MediaStudioLimits.MAX_CONCURRENT_ENCODES

        fun of(context: Context) = ProcessingSettings(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }

}

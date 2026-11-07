package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui

import android.content.Context
import android.content.SharedPreferences
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec

/**
 * The Studio settings of roadmap D37 that the screens apply: the output folder of wizard tasks (null: the default
 * folders of D14), the default video codec and bit rate policy of the wizards, and the folders the home screen lists
 * besides the fixed ones. Scripts are not affected: their defaults stay those of roadmap D14 / D29.
 * zh-CN: 路线图 D37 中由界面应用的 Studio 设置: 向导任务的输出目录 (null: D14 的默认目录), 向导的默认视频编码与码率策略, 以及
 * 首页在固定目录之外列出的目录. 不影响脚本: 脚本默认值仍按路线图 D14 / D29.
 */
internal class StudioPreferences(private val prefs: SharedPreferences) {

    /** How the wizards choose a video bit rate the form leaves empty. zh-CN: 表单留空时向导选择视频码率的方式. */
    enum class BitRatePolicy(val id: String, val factor: Double) {
        /** The plugin's estimate (roadmap D29, appendix D Q12). zh-CN: 插件的估算 (路线图 D29, 附录 D Q12). */
        AUTO("auto", 1.0),
        HIGH("high", 1.5),
        SMALL("small", 0.6),
        ;

        companion object {
            fun of(id: String?): BitRatePolicy = entries.firstOrNull { it.id == id } ?: AUTO
        }
    }

    var outputFolder: String?
        get() = prefs.getString(KEY_OUTPUT_FOLDER, null)?.takeIf { it.startsWith("/") }
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_OUTPUT_FOLDER) else putString(KEY_OUTPUT_FOLDER, value) }.apply()

    var codec: VideoCodec
        get() = VideoCodec.entries.firstOrNull { it.id == prefs.getString(KEY_CODEC, null) } ?: VideoCodec.H264
        set(value) = prefs.edit().putString(KEY_CODEC, value.id).apply()

    var bitRatePolicy: BitRatePolicy
        get() = BitRatePolicy.of(prefs.getString(KEY_BIT_RATE, null))
        set(value) = prefs.edit().putString(KEY_BIT_RATE, value.id).apply()

    /** Folders relative to the shared storage root, e.g. `DCIM/Camera`. zh-CN: 相对共享存储根的目录, 例如 `DCIM/Camera`. */
    var libraryFolders: List<String>
        get() = prefs.getString(KEY_FOLDERS, null).orEmpty().split('\n').map { it.trim('/') }.filter { it.isNotEmpty() }.distinct()
        set(value) = prefs.edit().putString(KEY_FOLDERS, value.map { it.trim('/') }.filter { it.isNotEmpty() }.distinct().joinToString("\n")).apply()

    companion object {
        private const val FILE = "studio"
        private const val KEY_OUTPUT_FOLDER = "outputFolder"
        private const val KEY_CODEC = "codec"
        private const val KEY_BIT_RATE = "bitRatePolicy"
        private const val KEY_FOLDERS = "libraryFolders"

        fun of(context: Context) = StudioPreferences(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }

}

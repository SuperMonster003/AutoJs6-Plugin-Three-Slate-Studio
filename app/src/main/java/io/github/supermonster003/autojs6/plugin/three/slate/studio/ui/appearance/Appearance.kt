package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.os.SystemClock
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as C
import java.util.Locale
import java.util.concurrent.Executors

/**
 * A read-only host snapshot or the resolved appearance of the plugin's screens (roadmap P3.2, standalone settings
 * specification section 7). Colors keep their seed; the palette derives the control colors from them.
 * zh-CN: 宿主的只读快照, 或插件界面解析后的外观 (路线图 P3.2, 独立设置规范第 7 节). 颜色保留种子色, 控件色由调色板派生.
 */
internal data class HostAppearance(val language: String, val dark: Boolean, val primary: Int, val accent: Int)

internal fun HostAppearance.configure(configuration: Configuration): Configuration = Configuration(configuration).apply {
    val locale = regionalLocale(language, List(configuration.locales.size()) { configuration.locales[it] })
    setLocales(LocaleList(locale))
    setLayoutDirection(locale)
    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
}

internal fun HostAppearance.wrap(context: Context): Context = context.createConfigurationContext(configure(context.resources.configuration))

/**
 * The locale of a language tag with a region (3-Macaw Speech roadmap D160): under a bare `en` some system builds (Sony XQ-AT72, Android 12)
 * format a file size as "309", about a hundred direction marks and "kB", which the screens draw as a wide gap. A tag without
 * a region takes the region of the first system locale of the same language and script, otherwise its likely region.
 * zh-CN: 带地区的语言标签区域设置 (3-Macaw Speech 路线图 D160): 只有 `en` 时, 部分系统 (Sony XQ-AT72, Android 12) 把文件大小格式化为 "309", 约一百个方向标记与 "kB",
 * 界面上显示为很宽的空白. 没有地区的标签取语言与文字相同的第一个系统区域设置的地区, 否则取其常见地区.
 */
internal fun regionalLocale(language: String, system: List<Locale>): Locale {
    val locale = Locale.forLanguageTag(language)
    if (locale.country.isNotEmpty() || locale.language.isEmpty()) return locale
    val region = system.firstOrNull { it.country.isNotEmpty() && it.language == locale.language && script(it) == script(locale) }?.country
        ?: LIKELY_REGIONS[locale.language + script(locale)]
        ?: return locale
    return Locale.Builder().setLocale(locale).setRegion(region).build()
}

/** The script of a Chinese locale, also when only its region tells it. zh-CN: 中文区域设置的文字, 只有地区时由地区推断. */
private fun script(locale: Locale): String = when {
    locale.script.isNotEmpty() || locale.language != "zh" -> locale.script
    locale.country in setOf("TW", "HK", "MO") -> "Hant"
    else -> "Hans"
}

/** Likely regions of the screen languages (CLDR likely subtags). zh-CN: 界面语言的常见地区 (CLDR likely subtags). */
private val LIKELY_REGIONS = mapOf(
    "zhHans" to "CN", "zhHant" to "TW", "en" to "US", "fr" to "FR", "es" to "ES", "ja" to "JP", "ko" to "KR", "ru" to "RU", "ar" to "EG",
)

/**
 * The plugin's own appearance preferences (standalone settings specification section 7): language, night mode and theme
 * color follow AutoJs6 by default; a choice made here is never written to the host. Adapted from 3-Block Compose.
 * zh-CN: 插件自己的外观偏好 (独立设置规范第 7 节): 语言, 夜间模式与主题色默认跟随 AutoJs6; 此处的选择从不写回宿主. 改编自 3-Block Compose.
 */
internal data class AppearancePreferences(
    val language: String = FOLLOW_HOST,
    val darkMode: String = FOLLOW_HOST,
    val color: Int? = null,
) {
    fun resolve(host: HostAppearance?, systemLanguage: String, systemDark: Boolean): HostAppearance {
        val resolvedLanguage = when (language) {
            FOLLOW_HOST -> host?.language ?: systemLanguage
            FOLLOW_SYSTEM -> systemLanguage
            in LANGUAGES -> language
            else -> host?.language ?: systemLanguage
        }
        val dark = when (darkMode) {
            LIGHT -> false
            DARK -> true
            FOLLOW_SYSTEM -> systemDark
            else -> host?.dark ?: systemDark
        }
        return HostAppearance(
            resolvedLanguage,
            dark,
            (color ?: host?.primary ?: DEFAULT_COLOR) or OPAQUE,
            (color ?: host?.accent ?: DEFAULT_COLOR) or OPAQUE,
        )
    }

    /** Returns the actual persistence result; nothing is written to the host. zh-CN: 返回真实的保存结果; 不写入宿主. */
    fun save(context: Context): Boolean {
        require(language in LANGUAGES && darkMode in DARK_MODES)
        val editor = file(context).edit().putString(KEY_LANGUAGE, language).putString(KEY_DARK_MODE, darkMode)
        if (color == null) editor.remove(KEY_COLOR) else editor.putInt(KEY_COLOR, color or OPAQUE)
        return editor.commit()
    }

    companion object {
        /** #FFDEAD, the host's default seed (specification section 6). zh-CN: 宿主默认种子色 #FFDEAD. */
        const val DEFAULT_COLOR: Int = -0x2153
        const val FOLLOW_HOST = "host"
        const val FOLLOW_SYSTEM = "system"
        const val LIGHT = "light"
        const val DARK = "dark"
        private const val OPAQUE = -0x1000000
        /** The preference file and keys. zh-CN: 偏好文件与键名. */
        internal const val FILE = "app-appearance"
        internal const val KEY_LANGUAGE = "language"
        internal const val KEY_DARK_MODE = "darkMode"
        internal const val KEY_COLOR = "color"
        val LANGUAGES = listOf(FOLLOW_HOST, FOLLOW_SYSTEM, "zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "en", "fr", "es", "ja", "ko", "ru", "ar")
        val DARK_MODES = listOf(FOLLOW_HOST, FOLLOW_SYSTEM, LIGHT, DARK)

        fun read(context: Context): AppearancePreferences = runCatching {
            val prefs = file(context).all
            AppearancePreferences(
                (prefs[KEY_LANGUAGE] as? String)?.takeIf { it in LANGUAGES } ?: FOLLOW_HOST,
                (prefs[KEY_DARK_MODE] as? String)?.takeIf { it in DARK_MODES } ?: FOLLOW_HOST,
                (prefs[KEY_COLOR] as? Int)?.or(OPAQUE),
            )
        }.getOrDefault(AppearancePreferences())

        fun resolve(context: Context, host: HostAppearance?): HostAppearance {
            // The system configuration is independent of any locale or night override of a plugin Activity.
            val system = Resources.getSystem().configuration
            val language = if (system.locales.isEmpty) Locale.getDefault().toLanguageTag() else system.locales[0].toLanguageTag()
            return read(context).resolve(host, language, system.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        }

        private fun file(context: Context) = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }
}

/**
 * The official host settings Provider is the only source of the host appearance; missing access (no AutoJs6, a debug
 * signature, an older host) is a normal fallback. Adapted from 3-Block Compose.
 * zh-CN: 官方宿主设置 Provider 是宿主外观的唯一来源; 无法访问 (未装 AutoJs6, 调试签名, 旧宿主) 属于正常回退. 改编自 3-Block Compose.
 */
internal object HostAppearanceReader {
    private data class Cached(val value: HostAppearance, val readAt: Long)
    private const val CACHE_TTL_MILLIS = 30_000L
    @Volatile private var cache: Cached? = null
    val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "SlateHostAppearance").apply { isDaemon = true } }

    fun cached(): HostAppearance? = cache?.takeIf { SystemClock.elapsedRealtime() - it.readAt < CACHE_TTL_MILLIS }?.value

    /** Called only after a still-resumed Activity accepts the refresh, including a missing host. zh-CN: 仅在仍处于前台的 Activity 接受刷新后调用. */
    fun accept(value: HostAppearance?) {
        cache = value?.let { Cached(it, SystemClock.elapsedRealtime()) }
    }

    /** Must run on [worker]; keeps no Activity reference and closes the unstable client. zh-CN: 必须在 [worker] 上运行. */
    fun read(context: Context): HostAppearance? = runCatching {
        context.applicationContext.contentResolver.acquireUnstableContentProviderClient(Uri.parse(C.CONTENT_URI))?.use { provider ->
            provider.call(C.METHOD_GET_SETTINGS, null, null)?.let(::decode)
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    fun decode(value: Bundle): HostAppearance? = runCatching {
        require(!value.hasFileDescriptors())
        require(value.get(C.KEY_PROTOCOL_VERSION) is Int && value.getInt(C.KEY_PROTOCOL_VERSION) == C.PROTOCOL_VERSION)
        require(value.getString(C.KEY_HOST_PACKAGE_NAME) == C.HOST_PACKAGE_NAME)
        require(value.get(C.KEY_DARK_MODE_ACTIVE) is Boolean)
        require(value.get(C.KEY_THEME_COLOR_PRIMARY) is Int && value.get(C.KEY_THEME_COLOR_ACCENT) is Int)
        val language = requireNotNull(value.getString(C.KEY_RESOLVED_LANGUAGE_TAG))
        require(language.length in 2..80 && language.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")))
        // forLanguageTag alone silently truncates malformed subtags; Builder validates the whole tag.
        val locale = Locale.Builder().setLanguageTag(language).build()
        require(locale.language.isNotEmpty())
        HostAppearance(
            language, value.getBoolean(C.KEY_DARK_MODE_ACTIVE),
            value.getInt(C.KEY_THEME_COLOR_PRIMARY) or -0x1000000, value.getInt(C.KEY_THEME_COLOR_ACCENT) or -0x1000000,
        )
    }.getOrNull()
}

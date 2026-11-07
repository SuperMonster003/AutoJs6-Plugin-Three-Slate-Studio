package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The appearance rules of the standalone settings specification (sections 6, 7, 9, 10): source priority and fallbacks,
 * the HEX / RGB input, the HCT accent rule, readable accents on every neutral surface and the launcher alias states.
 * zh-CN: 独立设置规范 (第 6, 7, 9, 10 节) 的外观规则: 来源优先级与回退, HEX / RGB 输入, HCT 强调色规则, 各中性表面上可读的强调色以及启动器 alias 状态.
 */
class AppearanceTest {

    private val host = HostAppearance(language = "ja", dark = true, primary = 0xff3f51b5.toInt(), accent = 0xffe91e63.toInt())

    @Test
    fun `the defaults follow AutoJs6 and fall back to the system and NavajoWhite`() {
        val defaults = AppearancePreferences()
        assertEquals(host, defaults.resolve(host, systemLanguage = "en-US", systemDark = false))
        val fallback = defaults.resolve(null, systemLanguage = "en-US", systemDark = false)
        assertEquals("en-US", fallback.language)
        assertEquals(false, fallback.dark)
        assertEquals(AppearancePreferences.DEFAULT_COLOR or -0x1000000, fallback.primary)
        assertEquals(0xffffdead.toInt(), fallback.accent)
    }

    @Test
    fun `local choices win over the host and the host is never needed for them`() {
        val own = AppearancePreferences(language = "ar", darkMode = AppearancePreferences.LIGHT, color = 0x00ff9800)
        val resolved = own.resolve(host, systemLanguage = "en", systemDark = true)
        assertEquals("ar", resolved.language)
        assertEquals(false, resolved.dark)
        assertEquals("a stored color is made opaque", 0xffff9800.toInt(), resolved.primary)
        assertEquals("the own color is the accent seed as well", 0xffff9800.toInt(), resolved.accent)
        val system = AppearancePreferences(language = AppearancePreferences.FOLLOW_SYSTEM, darkMode = AppearancePreferences.FOLLOW_SYSTEM)
            .resolve(host, systemLanguage = "fr-FR", systemDark = true)
        assertEquals("fr-FR", system.language)
        assertEquals(true, system.dark)
        assertEquals(AppearancePreferences.DARK_MODES.first(), AppearancePreferences.FOLLOW_HOST)
        assertEquals(12, AppearancePreferences.LANGUAGES.size)
    }

    @Test
    fun `a language without a region takes the system region or its likely one`() {
        // Roadmap D160: a bare `en` let a system build format "309 kB" with about a hundred direction marks.
        // zh-CN: 路线图 D160: 只有 `en` 时, 有系统把 "309 kB" 格式化为夹着约一百个方向标记的文本.
        fun tag(language: String, vararg system: String) = regionalLocale(language, system.map(Locale::forLanguageTag)).toLanguageTag()
        assertEquals("en-US", tag("en", "zh-CN"))
        assertEquals("en-GB", tag("en", "zh-CN", "en-GB"))
        assertEquals("zh-Hans-CN", tag("zh-Hans", "zh-TW"))
        assertEquals("zh-Hans-SG", tag("zh-Hans", "zh-SG"))
        assertEquals("zh-Hant-HK", tag("zh-Hant-HK", "zh-TW"))
        assertEquals("ar-EG", tag("ar"))
        assertEquals("fr-FR", tag("fr-FR", "fr-CA"))
        assertEquals("no likely region for other languages", "de", tag("de"))
        AppearancePreferences.LANGUAGES.drop(2).forEach { language ->
            assertTrue("$language gets a region", regionalLocale(language, emptyList()).country.isNotEmpty())
        }
    }

    @Test
    fun `the color input accepts HEX and RGB and rejects everything else`() {
        assertEquals(0xffffdead.toInt(), ThemeColorValue.parse("#FFDEAD"))
        assertEquals(0xffffdead.toInt(), ThemeColorValue.parse("  ffdead "))
        assertEquals(0xff0080ff.toInt(), ThemeColorValue.parse("RGB( 0, 128 ,255 )"))
        listOf("", "#FFF", "#FFDEADAA", "FFDEAD1", "rgb(256, 0, 0)", "rgb(-1, 0, 0)", "rgb(1, 2)", "rgb(1, 2, 3) x", "#GGGGGG", "rgba(1, 2, 3, 4)")
            .forEach { assertNull(it, ThemeColorValue.parse(it)) }
        assertEquals("#FFDEAD", ThemeColorValue.hex(0xffffdead.toInt()))
        assertEquals("rgb(255, 222, 173)", ThemeColorValue.rgb(0xffffdead.toInt()))
        assertEquals(16, ThemeColorValue.presets.size)
        assertEquals(0xffffdead.toInt(), ThemeColorValue.presets.first())
    }

    @Test
    fun `the HCT rule keeps gray seeds gray and gives readable on-primary roles`() {
        val gray = ThemeAccentRoles.fromSeed(0xff808080.toInt(), dark = false)
        assertTrue("chroma below 4 stays neutral", isGray(gray.primary) && isGray(gray.onPrimary))
        ThemeColorValue.presets.forEach { seed ->
            listOf(false, true).forEach { dark ->
                val roles = ThemeAccentRoles.fromSeed(seed, dark)
                assertTrue("${ThemeColorValue.hex(seed)} dark=$dark", ColorPolicy.contrastRatio(roles.primary, roles.onPrimary) >= 4.5)
            }
        }
        val light = ThemeAccentRoles.fromSeed(0xffffdead.toInt(), dark = false)
        assertTrue("tone 40 is darker than the pale seed", ColorPolicy.luminance(light.primary) < ColorPolicy.luminance(0xffffdead.toInt()))
    }

    @Test
    fun `the accent reads on every neutral surface for extreme seeds`() {
        listOf(0xffffffff.toInt(), 0xff000000.toInt(), 0xffffff00.toInt(), 0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt(), 0xffffdead.toInt())
            .forEach { seed ->
                listOf(false, true).forEach { dark ->
                    val palette = SlatePalette.resolve(HostAppearance("en", dark, seed, seed))
                    listOf(palette.background, palette.surface, palette.surfaceVariant).forEach { surface ->
                        assertTrue("${ThemeColorValue.hex(seed)} dark=$dark", ColorPolicy.contrastRatio(palette.accent, surface) >= 4.5)
                    }
                    assertTrue(ColorPolicy.contrastRatio(palette.text, palette.background) >= 4.5)
                    assertTrue(ColorPolicy.contrastRatio(palette.muted, palette.surface) >= 4.5)
                    assertEquals(if (dark) 0xff121212.toInt() else 0xfff3f4f5.toInt(), palette.background)
                }
            }
    }

    @Test
    fun `the launcher choice is the one explicit alias and Auto otherwise`() {
        val enabled = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val disabled = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val default = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        fun states(vararg values: Int) = LauncherIconMode.entries.zip(values.toList()).toMap()
        assertEquals(LauncherIconMode.AUTO, LauncherIconStatePolicy.resolve(states(default, default, default, default)))
        assertEquals(LauncherIconMode.DARK, LauncherIconStatePolicy.resolve(states(disabled, enabled, disabled, disabled)))
        assertEquals("a mixed state keeps the explicit Auto", LauncherIconMode.AUTO, LauncherIconStatePolicy.resolve(states(enabled, disabled, enabled, disabled)))
        assertEquals(LauncherIconMode.TRANSPARENT, LauncherIconStatePolicy.resolve(states(disabled, disabled, default, enabled)))
        assertTrue(LauncherIconStatePolicy.enabled(LauncherIconMode.AUTO, default))
        assertTrue(!LauncherIconStatePolicy.enabled(LauncherIconMode.LIGHT, default))
        assertEquals(listOf("AdaptiveLightIconAlias", "AdaptiveDarkIconAlias", "AdaptiveAutoIconAlias", "TransparentIconAlias"), LauncherIconMode.entries.map { it.alias })
    }

    private fun isGray(color: Int): Boolean {
        val r = color shr 16 and 0xff
        val g = color shr 8 and 0xff
        val b = color and 0xff
        return kotlin.math.abs(r - g) <= 1 && kotlin.math.abs(g - b) <= 1
    }
}

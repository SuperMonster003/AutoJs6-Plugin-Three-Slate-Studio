package io.github.supermonster003.autojs6.plugin.three.slate.studio

import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.children
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * Every locale carries the same sorted string keys, the default and explicit English resources are identical, the
 * descriptions follow the description rules, and every locale ships the generated Plugin Center instruction.
 * zh-CN: 每个语言的字符串键相同且按名称排序, 默认与显式英语资源一致, 描述符合描述规则, 每个语言都附带生成的插件中心说明.
 */
class StringResourceParityTest {

    @Test
    fun `every locale defines the same strings in sorted order`() {
        val reference = RepositoryFiles.strings("values")
        assertTrue("plugin_description must exist", "plugin_description" in reference.keys)
        (LOCALE_DIRECTORIES + "values").forEach { directory ->
            val localized = RepositoryFiles.strings(directory)
            assertEquals("$directory must define the same keys as values", reference.keys, localized.keys)
            assertEquals("$directory strings must be sorted by name", localized.keys.sorted(), localized.keys.toList())
            localized.forEach { (name, value) -> assertTrue("$directory/$name is blank", value.isNotBlank()) }
        }
        assertEquals("values and values-en must be identical", reference, RepositoryFiles.strings("values-en"))
    }

    @Test
    fun `identity strings come from the build and are never translated`() {
        (LOCALE_DIRECTORIES + "values").forEach { directory ->
            val keys = RepositoryFiles.strings(directory).keys
            IDENTITY_STRINGS.forEach { name -> assertFalse("$directory must not define $name (it is a resValue)", name in keys) }
            val document = RepositoryFiles.xml("app/src/main/res/$directory/strings.xml")
            assertTrue("$directory/strings.xml holds strings only", document.children("plurals").isEmpty() && document.children("string-array").isEmpty())
            document.children("string").forEach { assertFalse("$directory: translatable=false belongs to the build", it.getAttribute("translatable") == "false") }
        }
    }

    @Test
    fun `plugin descriptions follow the description rules`() {
        (LOCALE_DIRECTORIES + "values").forEach { directory ->
            val description = RepositoryFiles.strings(directory).getValue("plugin_description")
            assertFalse("$directory plugin_description must not end with punctuation", description.last() in ".!?;:,")
            assertFalse("$directory plugin_description must not mention AutoJs6", description.contains("AutoJs6"))
            assertTrue("$directory plugin_description has ASCII punctuation only", RepositoryFiles.nonAsciiPunctuation(description).isEmpty())
        }
    }

    @Test
    fun `plugin descriptions equal the README synopsis of the same language`() {
        README_LANGUAGES.forEach { (directory, language) ->
            val synopsis = Regex("\"text_plugin_synopsis\": \"([^\"]*)\"")
                .find(RepositoryFiles.text(".readme/lang_$language.json"))?.groupValues?.get(1)
            assertEquals("$directory vs .readme/lang_$language.json", synopsis, RepositoryFiles.strings(directory).getValue("plugin_description"))
        }
    }

    @Test
    fun `every locale ships the generated plugin instruction and release shrinking keeps it`() {
        (LOCALE_DIRECTORIES.map { it.replace("values", "raw") } + "raw").forEach { directory ->
            val instruction = RepositoryFiles.path("app/src/main/res/$directory/plugin_instruction.md")
            assertTrue("missing $directory/plugin_instruction.md (run .python/generate_markdown.py)", Files.isRegularFile(instruction))
            assertTrue("$directory/plugin_instruction.md is empty", Files.size(instruction) > 0)
        }
        assertEquals(
            "raw and raw-en must be identical",
            RepositoryFiles.text("app/src/main/res/raw/plugin_instruction.md"),
            RepositoryFiles.text("app/src/main/res/raw-en/plugin_instruction.md"),
        )
        // The host reads the instruction by name across packages; without tools:keep, shrinkResources removes it.
        val keep = RepositoryFiles.xml("app/src/main/res/raw/keep.xml")
        assertEquals("@raw/plugin_instruction", keep.getAttributeNS("http://schemas.android.com/tools", "keep"))
    }

    @Test
    fun `the launcher icon sources and the Icon Studio outputs exist`() {
        listOf(
            "mipmap/ic_launcher.png", "mipmap-night/ic_launcher.png",
            "mipmap/ic_plugin_center.png", "mipmap-night/ic_plugin_center.png",
            "mipmap/ic_icon_studio_application.png", "mipmap-anydpi-v26/ic_icon_studio_application.xml",
            "mipmap-night-anydpi-v26/ic_icon_studio_application.xml", "raw/keep_plugin_center_icon.xml",
            "mipmap/ic_launcher_system.png", "mipmap/ic_launcher_system_light.png", "mipmap/ic_launcher_monochrome.png",
            "mipmap/ic_launcher_system_auto.xml", "mipmap-notnight/ic_launcher_system_auto.xml",
            "mipmap-anydpi-v26/ic_launcher_system.xml", "mipmap-anydpi-v26/ic_launcher_system_light.xml",
            "mipmap-anydpi-v26/ic_launcher_system_auto.xml", "mipmap-notnight-anydpi-v26/ic_launcher_system_auto.xml",
        ).forEach {
            assertTrue("missing $it (run .python/generate_icon_studio.py)", Files.isRegularFile(RepositoryFiles.path("app/src/main/res/$it")))
        }
    }

    private companion object {
        val LOCALE_DIRECTORIES = listOf(
            "values-ar", "values-en", "values-es", "values-fr", "values-ja", "values-ko", "values-ru",
            "values-zh", "values-zh-rHK", "values-zh-rTW",
        )
        val README_LANGUAGES = mapOf(
            "values-ar" to "ar", "values-en" to "en", "values-es" to "es", "values-fr" to "fr", "values-ja" to "ja",
            "values-ko" to "ko", "values-ru" to "ru", "values-zh" to "zh-Hans", "values-zh-rHK" to "zh-Hant-HK",
            "values-zh-rTW" to "zh-Hant-TW", "values" to "en",
        )
        val IDENTITY_STRINGS = listOf(
            "app_name", "plugin_author", "plugin_engine", "plugin_id", "plugin_name", "plugin_requires_host_version",
            "plugin_variant", "plugin_version_date",
        )
    }

}

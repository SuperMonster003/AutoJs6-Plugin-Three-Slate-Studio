package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release

import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.util.Locale

/**
 * The release history page (plugin-center P21.1, release history specification section 10): the changelog of a locale,
 * the fallback to English, the parser and the grouping by category.
 * zh-CN: 发行历史页 (plugin-center P21.1, 发行历史规范第 10 节): 区域对应的 changelog, 回退到英文, 解析与按类别分组.
 */
class ReleaseHistoryTest {

    private fun first(tag: String) = ReleaseHistoryAssets.candidatesFor(Locale.forLanguageTag(tag)).first()

    @Test
    fun mapsChineseLocalesAndFallsBackToEnglish() {
        assertEquals("doc/CHANGELOG-zh-Hant-HK.md", first("zh-HK"))
        assertEquals("doc/CHANGELOG-zh-Hant-HK.md", first("zh-Hant-MO"))
        assertEquals("doc/CHANGELOG-zh-Hant-TW.md", first("zh-TW"))
        assertEquals("doc/CHANGELOG-zh-Hant-TW.md", first("zh-Hant"))
        assertEquals("doc/CHANGELOG-zh-Hans.md", first("zh-Hans-CN"))
        assertEquals("doc/CHANGELOG-zh-Hans.md", first("zh-Hans-HK"))
        assertEquals(listOf("doc/CHANGELOG-ar.md", "doc/CHANGELOG-en.md"), ReleaseHistoryAssets.candidatesFor(Locale.forLanguageTag("ar-EG")))
        assertEquals(listOf("doc/CHANGELOG-en.md"), ReleaseHistoryAssets.candidatesFor(Locale.forLanguageTag("de-DE")))
        assertEquals(listOf("doc/CHANGELOG-en.md"), ReleaseHistoryAssets.candidatesFor(Locale.ENGLISH))
    }

    /**
     * A missing, blank or unparsable localized changelog falls back to English; nothing usable gives the empty state.
     * zh-CN: 缺失, 空白或解析不出版本的本地化 changelog 回退到英文; 都不可用时为空状态.
     */
    @Test
    fun skipsMissingBlankOrUnparsableLocalizedAssetsAndReportsCompleteFailure() {
        val candidates = ReleaseHistoryAssets.candidatesFor(Locale.FRANCE)
        val english = "# v1.0.0\n\n###### 2026/11/07\n\n* `Feature` English note\n"
        fun load(french: String?) = loadReleaseHistory(candidates) { path ->
            when {
                path.endsWith("-en.md") -> english
                french == null -> throw FileNotFoundException(path)
                else -> french
            }
        }
        assertEquals("English note", load(null).single().items.single().text)
        assertEquals("English note", load(" ").single().items.single().text)
        assertEquals("English note", load("### Historique\n").single().items.single().text)
        assertEquals("Note", load("# v1.0.0\n\n* `Nouveauté` Note\n").single().items.single().text)
        assertTrue(loadReleaseHistory(candidates) { " " }.isEmpty())
        assertTrue(loadReleaseHistory(candidates) { throw FileNotFoundException(it) }.isEmpty())
    }

    @Test
    fun parsesTheGeneratedChangelogDialect() {
        val markdown = """
            ******

            ### Release History

            ******

            # v1.1.0

            ###### 2026/11/10

            * `Hint` First note
            * `Improvement` Second note

            ## v1.0.0

            ###### 2026/11/07

            * `Feature` Third note
            * Untagged note
        """.trimIndent()
        val entries = ReleaseHistoryParser.parse(markdown)
        assertEquals(listOf("v1.1.0", "v1.0.0"), entries.map { it.version })
        assertEquals("2026/11/10", entries[0].date)
        assertEquals(listOf("Hint" to "First note", "Improvement" to "Second note"), entries[0].items.map { it.tag to it.text })
        assertEquals(listOf("Feature" to "Third note", null to "Untagged note"), entries[1].items.map { it.tag to it.text })
        assertTrue(ReleaseHistoryParser.parse("### Release History\n\n* `Feature` Orphan\n").isEmpty())
    }

    /**
     * The page names each category once and lists its items beneath; the collapsed summary counts them.
     * zh-CN: 页面每个类别只标注一次, 其下列出该类各条; 收起时的汇总给出各类条目数.
     */
    @Test
    fun groupsItemsByCategoryInTheOrderTheyFirstAppear() {
        val entry = ReleaseHistoryParser.parse("# v1.2.0\n\n* `Hint` A\n* `Added` B\n* `Added` C\n* `Fixed` D\n* `Added` E\n* Untagged F\n").single()
        assertEquals(
            listOf("Hint" to listOf("A"), "Added" to listOf("B", "C", "E"), "Fixed" to listOf("D"), null to listOf("Untagged F")),
            entry.groups.map { it.tag to it.items },
        )
        assertEquals(listOf("Hint" to 1, "Added" to 3, "Fixed" to 1), entry.categoryCounts)
    }

    /**
     * Every version heading of the bundled Simplified Chinese and English changelogs becomes one release with items.
     * zh-CN: 内置简体中文与英文 changelog 的每个版本标题都成为一个带条目的版本.
     */
    @Test
    fun parsesEveryReleaseOfTheBundledChangelogs() {
        for (name in listOf("CHANGELOG-zh-Hans.md", "CHANGELOG-en.md")) {
            val markdown = RepositoryFiles.text("app/src/main/assets/doc/$name")
            val headings = markdown.lineSequence().count { Regex("^#{1,2}\\s+v\\S+\\s*$").matches(it.trim()) }
            val entries = ReleaseHistoryParser.parse(markdown)
            assertTrue("$name: $headings", headings >= 2)
            assertEquals(name, headings, entries.size)
            assertTrue(name, entries.all { it.date != null && it.items.isNotEmpty() && it.categoryCounts.isNotEmpty() })
        }
    }
}

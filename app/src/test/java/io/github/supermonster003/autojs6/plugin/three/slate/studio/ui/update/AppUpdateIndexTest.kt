package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The official plugin index as the update source (roadmap appendix D Q45 = (b)): parsing of `plugins.official.generated.json`
 * and the versionCode comparison.
 * zh-CN: 以官方插件索引为更新数据源 (路线图附录 D Q45 = (b)): `plugins.official.generated.json` 的解析与 versionCode 比较.
 */
class AppUpdateIndexTest {

    private val pkg = AppUpdateChecker.PACKAGE_NAME
    private val page = AppUpdateChecker.RELEASES_PAGE

    private fun release(code: Long, name: String, prerelease: Boolean = false, url: String = "$page/tag/v$name", notes: String = "notes $name") =
        """{"versionName":"$name","versionCode":$code,"versionDate":"2026-11-06","changelogUrl":"$url","changelogText":"$notes","assets":[],"prerelease":$prerelease}"""

    private fun index(vararg items: String) = """{"schemaVersion":2,"source":"OFFICIAL","items":[${items.joinToString(",")}]}"""

    private fun item(packageName: String, vararg releases: String) = """{"packageName":"$packageName","releases":[${releases.joinToString(",")}]}"""

    private fun found(text: String) = (AppUpdateChecker.decode(text) as UpdateResult.Release).release

    @Test
    fun picksHighestPublishedVersionCodeOfThePlugin() {
        val text = index(
            item("io.github.supermonster003.autojs6.plugin.three.macaw.speech", release(900, "9.0.0")),
            item(pkg, release(20, "1.0.0"), release(31, "1.0.1"), release(25, "1.0.0-hotfix")),
        )
        val latest = found(text)
        assertEquals(31L, latest.versionCode)
        assertEquals("1.0.1", latest.versionName)
        assertEquals("v1.0.1", latest.tag)
        assertEquals("notes 1.0.1", latest.notes)
        assertEquals("$page/tag/v1.0.1", latest.pageUrl)
    }

    @Test
    fun skipsPrereleasesAndInvalidEntries() {
        val text = index(item(pkg, release(20, "1.0.0"), release(40, "1.1.0-rc1", prerelease = true), release(0, "0.0.0"), release(50, "bad name")))
        assertEquals(20L, found(text).versionCode)
    }

    @Test
    fun missingPluginOrNoReleaseIsNoRelease() {
        assertEquals(UpdateResult.NoRelease, AppUpdateChecker.decode(index(item("other.package", release(5, "1.0.0")))))
        assertEquals(UpdateResult.NoRelease, AppUpdateChecker.decode(index(item(pkg))))
        assertEquals(UpdateResult.NoRelease, AppUpdateChecker.decode(index(item(pkg, release(9, "1.0.0", prerelease = true)))))
        assertEquals(UpdateResult.NoRelease, AppUpdateChecker.decode(index()))
    }

    @Test
    fun foreignPageFallsBackToReleasesList() {
        val text = index(item(pkg, release(20, "1.0.0", url = "https://example.com/phish")))
        assertEquals(page, found(text).pageUrl)
    }

    @Test
    fun notesAreCapped() {
        val text = index(item(pkg, release(20, "1.0.0", notes = "x".repeat(10_000))))
        assertEquals(AppUpdateChecker.MAXIMUM_NOTES_CHARACTERS, found(text).notes.length)
    }

    @Test(expected = org.json.JSONException::class)
    fun malformedIndexFails() {
        AppUpdateChecker.decode("""{"schemaVersion":2}""")
    }

    @Test
    fun comparesVersionCodeOnly() {
        val candidate = AppRelease(versionCode = 31, versionName = "1.0.1", notes = "", pageUrl = page)
        assertTrue(UpdateVersionPolicy.isNewer(candidate, 30))
        assertFalse(UpdateVersionPolicy.isNewer(candidate, 31))
        assertFalse(UpdateVersionPolicy.isNewer(candidate, 32))
    }

    @Test
    fun indexHostsFollowTheHostOrder() {
        val urls = AppUpdateChecker.INDEX_URLS
        assertEquals(3, urls.size)
        assertTrue(urls[0].startsWith("https://raw.githubusercontent.com/SuperMonster003/AutoJs6-Official-Plugins-Index/"))
        assertTrue(urls[1].startsWith("https://cdn.jsdelivr.net/gh/SuperMonster003/AutoJs6-Official-Plugins-Index@main/"))
        assertTrue(urls[2].startsWith("https://fastly.jsdelivr.net/gh/SuperMonster003/AutoJs6-Official-Plugins-Index@main/"))
        assertTrue(urls.all { it.endsWith("/plugins.official.generated.json") })
    }
}

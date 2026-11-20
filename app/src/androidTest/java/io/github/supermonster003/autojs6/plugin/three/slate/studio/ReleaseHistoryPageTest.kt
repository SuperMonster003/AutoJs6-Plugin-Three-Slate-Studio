package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.app.Activity
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release.ReleaseHistoryAssets
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release.loadReleaseHistory
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The release history page (plugin-center P21.1, release history specification section 10): the title "Release
 * history" with the plugin name as the subtitle, the newest release expanded and the others collapsed with their
 * category counts, a header toggles its release, the top bar expands or collapses all, the choice survives recreation,
 * the settings row opens the page, and Tab walks every release down to the oldest one.
 * zh-CN: 发行历史页 (plugin-center P21.1, 发行历史规范第 10 节): 标题 "发行历史", 子标题为插件名称; 最新版本展开, 其余收起并显示各类别条目数; 点版本头
 * 切换该版本, 顶栏按钮展开或收起全部, 重建后保留; 设置页那一行打开本页; Tab 依次走过各版本直到最早的版本.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseHistoryPageTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @After
    fun finishAll() {
        instrumentation.setInTouchMode(true)
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
    }

    /** Unmerged: the count chips sit inside the clickable header, which merges its children. zh-CN: 未合并树: 计数胶囊在合并子节点的可点按版本头里. */
    private fun exists(tag: String): Boolean = compose.shows(hasTestTag(tag), useUnmergedTree = true)

    private fun versions(): List<String> {
        val locale = context.resources.configuration.locales[0]
        return loadReleaseHistory(ReleaseHistoryAssets.candidatesFor(locale)) { path ->
            context.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
        }.map { it.version }
    }

    private fun assertTitleAndSubtitle() {
        compose.onNode(hasText(context.getString(R.string.release_history))).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.app_name))).assertIsDisplayed()
    }

    @Test
    fun theTitleIsReleaseHistoryAndTheSubtitleThePluginName() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("slate.releaseHistory.list") }
            assertTitleAndSubtitle()
            assertFalse(exists("slate.emptyState"))
        }
    }

    /**
     * The newest release starts expanded, the others show their category counts; a header expands or collapses its
     * release, the top bar expands or collapses all of them, and the choice survives recreation.
     * zh-CN: 最新版本默认展开, 其余显示各类别计数; 点版本头展开或收起该版本, 顶栏按钮展开或收起全部, 选择在重建后保留.
     */
    @Test
    fun releasesExpandAndCollapse() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("slate.releaseHistory.list") }
            val versions = versions()
            assertTrue("releases: ${versions.size}", versions.size >= 2)
            val (newest, second) = versions
            assertTrue(exists("slate.releaseHistory.body.$newest"))
            assertFalse(exists("slate.releaseHistory.counts.$newest"))
            assertFalse(exists("slate.releaseHistory.body.$second"))
            assertTrue(exists("slate.releaseHistory.counts.$second"))

            compose.onNodeWithTag("slate.releaseHistory.entry.$second").performScrollTo().performClick()
            compose.waitUntil(TIMEOUT) { exists("slate.releaseHistory.body.$second") }
            compose.onNodeWithTag("slate.releaseHistory.entry.$newest").performScrollTo().performClick()
            compose.waitUntil(TIMEOUT) { !exists("slate.releaseHistory.body.$newest") && exists("slate.releaseHistory.counts.$newest") }

            scenario.recreate()
            compose.waitUntil(TIMEOUT) { exists("slate.releaseHistory.list") }
            assertTrue(exists("slate.releaseHistory.body.$second"))
            assertFalse(exists("slate.releaseHistory.body.$newest"))

            compose.onNodeWithTag("slate.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_expand_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { exists("slate.releaseHistory.body.$newest") }
            compose.onNodeWithTag("slate.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_collapse_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { !exists("slate.releaseHistory.body.$newest") && !exists("slate.releaseHistory.body.$second") }
        }
    }

    /** The "Release history" row of the settings page opens the page. zh-CN: 设置页的 "发行历史" 一行打开本页. */
    @Test
    fun theSettingsRowOpensThePage() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("slate.settings.releaseHistory") }
            compose.onNodeWithTag("slate.settings.releaseHistory").performScrollTo()
                .assert(hasText(context.getString(R.string.release_history)))
                .performClick()
            compose.waitUntil(TIMEOUT) { resumed() is ReleaseHistoryActivity && exists("slate.releaseHistory.list") }
            assertTitleAndSubtitle()
        }
    }

    /**
     * Every release takes focus in turn, scrolled into view, down to the oldest one; the back button and the top bar
     * button are reachable too.
     * zh-CN: 各版本依次获得焦点并滚入视野, 直到最早的版本; 返回按钮与顶栏按钮同样可达.
     */
    @Test
    fun theReleaseHistoryCanBeReadToTheEndFromTheKeyboard() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("slate.releaseHistory.list") }
            awaitWindowFocus(scenario)
            // Newer versions take setInTouchMode at once; API 24 learns it only from a key. zh-CN: 新版本立即生效; API 24 要靠按键得知.
            instrumentation.setInTouchMode(false)
            key(KeyEvent.KEYCODE_DPAD_UP)
            val versions = versions()
            val visited = tabCycle(scenario)
            fun expect(ok: Boolean, what: String) { if (!ok) fail("$what; visited: $visited; ${focusedWindow()}") }
            expect("slate.topBar.back" in visited, "Tab never reached the back button")
            expect("slate.releaseHistory.toggleAll" in visited, "Tab never reached the expand / collapse all button")
            expect(UNTAGGED !in visited, "something focusable has no name")
            val expected = versions.map { "slate.releaseHistory.entry.$it" }
            val entries = visited.filter { it.startsWith("slate.releaseHistory.entry.") }
            expect(entries == expected, "the releases in Tab order: expected $expected, got $entries")
        }
    }

    private fun resumed(): Activity? {
        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    /** The test tag of the focused control or of its nearest tagged ancestor. zh-CN: 获得焦点的控件或其最近的带标签祖先的测试标签. */
    private fun focusedTag(): String? {
        val node = compose.onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull() ?: return null
        return generateSequence(node) { it.parent }.firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.TestTag) } ?: UNTAGGED
    }

    /**
     * The window with input focus as `dumpsys window` reports it; for failure messages only (it connects UiAutomation).
     * zh-CN: `dumpsys window` 报告的输入焦点窗口; 只用于失败消息 (会连接 UiAutomation).
     */
    private fun focusedWindow(): String = runCatching {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys window windows"))
            .bufferedReader().useLines { lines -> lines.map(String::trim).filter { it.startsWith("mCurrentFocus=") }.distinct().joinToString() }
            .ifEmpty { "mCurrentFocus not in dumpsys" }
    }.getOrElse { "dumpsys failed: $it" }

    /**
     * Keys go to the focused window: while a system dialog (an ANR, say) has focus, every Tab would be lost (plugin-center Q137).
     * zh-CN: 按键送往有焦点的窗口: 系统对话框 (例如 ANR) 占着焦点时 Tab 都会丢失 (plugin-center Q137).
     */
    private fun awaitWindowFocus(scenario: ActivityScenario<*>, visited: List<String> = emptyList()) {
        var focused = false
        try {
            compose.waitUntil(TIMEOUT) { scenario.onActivity { focused = it.hasWindowFocus() }; focused }
        } catch (timeout: ComposeTimeoutException) {
            fail("the page has no window focus after $TIMEOUT ms; visited: $visited; ${focusedWindow()}")
        }
    }

    /** Presses Tab until focus returns to a control seen before; every focused control must be on screen. zh-CN: 按 Tab 直到焦点回到见过的控件. */
    private fun tabCycle(scenario: ActivityScenario<*>): List<String> {
        val visited = mutableListOf<String>()
        repeat(MAX_TABS) {
            awaitWindowFocus(scenario, visited)
            key(KeyEvent.KEYCODE_TAB)
            val tag = focusedTag() ?: return@repeat
            if (tag in visited) return visited
            compose.onNode(isFocused(), useUnmergedTree = true).assertIsDisplayed()
            visited += tag
        }
        return visited
    }

    private companion object {
        const val TIMEOUT = 15_000L
        const val MAX_TABS = 60
        const val UNTAGGED = "(untagged)"
    }
}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.supermonster003.autojs6.plugin.three.slate.studio.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.DetailScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.LibraryScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioTasks
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.TasksScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.ResultScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.ToolboxScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.WizardScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update.UpdateDialogs

/**
 * The Studio home (roadmap P4.1, D37, D35): three tabs (library, toolbox, tasks) with a bottom bar below 600 dp and a
 * navigation rail from 600 dp, and the pages they open (media detail, wizard, result). The automatic update check runs
 * when the home opens (not on a recreation); the settings open from the top bar.
 * zh-CN: Studio 首页 (路线图 P4.1, D37, D35): 三个标签 (媒体库, 工具箱, 任务), 600 dp 以下用底部导航栏, 600 dp 起用侧边导航栏, 以及
 * 由它们打开的页面 (媒体详情, 向导, 结果). 打开首页时运行自动更新检查 (重建时不运行); 设置从顶栏打开.
 */
class HomeActivity : StandaloneActivity() {

    internal enum class Tab(val route: String, @param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
        LIBRARY("library", R.string.home_tab_library, R.drawable.ic_movie_24),
        TOOLBOX("toolbox", R.string.home_tab_toolbox, R.drawable.ic_build_24),
        TASKS("tasks", R.string.home_tab_tasks, R.drawable.ic_list_24),
    }

    private lateinit var updates: AppUpdateController
    private var storageGranted by mutableStateOf(false)

    /** Bumped on resume so the library lists again (files may have changed meanwhile). zh-CN: 恢复时递增, 媒体库重新列出. */
    private var resumes by mutableIntStateOf(0)
    private var currentRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updates = AppUpdateController(AppUpdateStore.of(this), BuildConfig.VERSION_CODE.toLong())
        if (savedInstanceState == null) updates.checkAutomaticallyIfDue()
    }

    override fun onResume() {
        super.onResume()
        storageGranted = StorageAccess.isGranted(this)
        StudioTasks.refresh(this)
        resumes++
    }

    override fun onDestroy() {
        if (isFinishing) updates.cancel()
        super.onDestroy()
    }

    /** A wizard draft is not interrupted by an appearance change. zh-CN: 外观变化不打断向导草稿. */
    override fun holdsAppearance(): Boolean = updates.state != null || currentRoute?.startsWith(ROUTE_WIZARD) == true

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE) storageGranted = StorageAccess.isGranted(this)
    }

    internal fun requestStorage() = StorageAccess.request(this, REQUEST_STORAGE)

    internal fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    @Composable
    override fun Content() {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        currentRoute = entry?.destination?.route
        NavHost(navController = nav, startDestination = ROUTE_MAIN) {
            composable(ROUTE_MAIN) { MainScreen(nav) }
            composable(
                "$ROUTE_DETAIL/{path}",
                arguments = listOf(navArgument("path") { type = NavType.StringType }),
            ) { back ->
                DetailScreen(
                    path = Uri.decode(back.arguments?.getString("path").orEmpty()),
                    onBack = { nav.popBackStack() },
                    onProcess = { op, path -> nav.navigate(wizardRoute(op, path)) },
                )
            }
            composable(
                "$ROUTE_WIZARD/{op}?input={input}",
                arguments = listOf(
                    navArgument("op") { type = NavType.StringType },
                    navArgument("input") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { back ->
                WizardScreen(
                    op = back.arguments?.getString("op").orEmpty(),
                    initialInput = back.arguments?.getString("input")?.let(Uri::decode),
                    onBack = { nav.popBackStack() },
                    onSubmitted = { key -> nav.navigate("$ROUTE_RESULT/$key") { popUpTo(ROUTE_MAIN) } },
                )
            }
            composable("$ROUTE_RESULT/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { back ->
                ResultScreen(
                    key = back.arguments?.getString("key").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenDetail = { path -> nav.navigate(detailRoute(path)) },
                    onRunAgain = { op, input -> nav.navigate(wizardRoute(op, input)) },
                )
            }
        }
        UpdateDialogs(updates, onOpenRelease = { url -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
            onReleaseHistory = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) })
    }

    @Composable
    private fun MainScreen(nav: NavHostController) {
        var tab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
        val snackbar = androidx.compose.runtime.remember { SnackbarHostState() }
        val wide = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } >= WIDE_DP.dp
        val tabContent: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                when (tab) {
                    Tab.LIBRARY -> LibraryScreen(
                        storageGranted = storageGranted,
                        refreshKey = resumes,
                        onGrantStorage = ::requestStorage,
                        onOpen = { path -> nav.navigate(detailRoute(path)) },
                        onOpenSettings = ::openSettings,
                    )
                    Tab.TOOLBOX -> ToolboxScreen(onOpen = { op -> nav.navigate(wizardRoute(op, null)) })
                    Tab.TASKS -> TasksScreen(
                        onOpenResult = { key -> nav.navigate("$ROUTE_RESULT/$key") },
                        onOpenDetail = { path -> nav.navigate(detailRoute(path)) },
                    )
                }
            }
        }
        LaunchedEffect(tab) { if (tab == Tab.TASKS) StudioTasks.refresh(this@HomeActivity) }
        SlateScreen(
            title = stringResource(R.string.app_name),
            snackbarHostState = snackbar,
            actions = {
                if (tab == Tab.TASKS && StudioTasks.rows.any { it.isFinal && it.request != null }) {
                    TopBarAction(R.drawable.ic_delete_24, stringResource(R.string.tasks_clear_finished), "slate.tasks.clear") { StudioTasks.clearFinished() }
                }
                TopBarAction(R.drawable.ic_settings_24, stringResource(R.string.settings_title), "slate.home.settings", ::openSettings)
            },
            bottomBar = {
                if (!wide) {
                    NavigationBar(modifier = Modifier.testTag("slate.home.navigation")) {
                        Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                                label = { Text(stringResource(item.label)) },
                                modifier = Modifier.testTag("slate.home.tab.${item.route}"),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            if (wide) {
                Row(Modifier.fillMaxSize().padding(padding)) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.background, modifier = Modifier.testTag("slate.home.navigation")) {
                        Tab.entries.forEach { item ->
                            NavigationRailItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                                label = { Text(stringResource(item.label)) },
                                modifier = Modifier.testTag("slate.home.tab.${item.route}"),
                            )
                        }
                    }
                    tabContent(Modifier.weight(1f).fillMaxSize())
                }
            } else {
                tabContent(Modifier.fillMaxSize().padding(padding))
            }
        }
    }

    internal companion object {
        const val REQUEST_STORAGE = 1
        const val WIDE_DP = 600
        const val ROUTE_MAIN = "main"
        const val ROUTE_DETAIL = "detail"
        const val ROUTE_WIZARD = "wizard"
        const val ROUTE_RESULT = "result"

        fun detailRoute(path: String) = "$ROUTE_DETAIL/${Uri.encode(path)}"

        fun wizardRoute(op: String, input: String?) = if (input == null) "$ROUTE_WIZARD/$op" else "$ROUTE_WIZARD/$op?input=${Uri.encode(input)}"
    }

}

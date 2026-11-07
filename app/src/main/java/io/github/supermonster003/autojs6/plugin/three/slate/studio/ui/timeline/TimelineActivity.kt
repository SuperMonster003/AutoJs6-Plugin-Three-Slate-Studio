package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.DetailScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.ResultScreen
import java.io.File

/**
 * The timeline editor screen (roadmap P4.3): the editor, then the result page of its export (the same page as the
 * toolbox wizards, with the equivalent `video.compose` script) and the media detail of the output. [EXTRA_PROJECT]
 * opens a `.slate.json` file, [EXTRA_OPEN_PROJECTS] starts with the project list.
 * zh-CN: 时间线剪辑器界面 (路线图 P4.3): 剪辑器, 其导出的结果页 (与工具箱向导相同的页面, 带等价的 `video.compose` 脚本) 与输出的媒体详情.
 * [EXTRA_PROJECT] 打开一个 `.slate.json` 文件, [EXTRA_OPEN_PROJECTS] 以工程列表开始.
 */
class TimelineActivity : StandaloneActivity() {

    private val model: EditorModel by viewModels()

    private var openProjects = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            intent?.getStringExtra(EXTRA_PROJECT)?.let { model.open(File(it)) }
            openProjects = intent?.getBooleanExtra(EXTRA_OPEN_PROJECTS, false) == true
        }
    }

    override fun onPause() {
        model.preview.pause()
        super.onPause()
    }

    // Recreating for an appearance change would only cost the playback position; keep the screen while editing.
    // zh-CN: 外观变化时重建只会丢失播放位置; 编辑中保持界面.
    override fun holdsAppearance(): Boolean = model.dirty || model.preview.playing

    @Composable
    override fun Content() {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = ROUTE_EDITOR) {
            composable(ROUTE_EDITOR) {
                EditorScreen(
                    model = model,
                    openProjects = openProjects,
                    onFinish = ::finish,
                    onExported = { key -> nav.navigate("$ROUTE_RESULT/$key") },
                )
            }
            composable("$ROUTE_RESULT/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { back ->
                ResultScreen(
                    key = back.arguments?.getString("key").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenDetail = { path -> nav.navigate("$ROUTE_DETAIL/${Uri.encode(path)}") },
                    onRunAgain = { _, _ -> nav.popBackStack(ROUTE_EDITOR, inclusive = false) },
                )
            }
            composable("$ROUTE_DETAIL/{path}", arguments = listOf(navArgument("path") { type = NavType.StringType })) { back ->
                DetailScreen(
                    path = Uri.decode(back.arguments?.getString("path").orEmpty()),
                    onBack = { nav.popBackStack() },
                    onProcess = { op, path ->
                        startActivity(Intent(this@TimelineActivity, HomeActivity::class.java).putExtra(HomeActivity.EXTRA_ROUTE, HomeActivity.wizardRoute(op, path)))
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT = "project"
        const val EXTRA_OPEN_PROJECTS = "openProjects"
        private const val ROUTE_EDITOR = "editor"
        private const val ROUTE_RESULT = "result"
        private const val ROUTE_DETAIL = "detail"

        fun newProject(context: Context): Intent = Intent(context, TimelineActivity::class.java)

        fun openProjects(context: Context): Intent = Intent(context, TimelineActivity::class.java).putExtra(EXTRA_OPEN_PROJECTS, true)
    }

}

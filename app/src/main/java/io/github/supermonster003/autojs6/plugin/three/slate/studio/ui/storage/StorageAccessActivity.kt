package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.storage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyAction
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen

/**
 * The storage guide of roadmap P2.1: opened when a script request needs all files access the plugin does not hold
 * (STORAGE_PERMISSION_DENIED, at most once a minute). It explains why and opens the system page; it closes itself once
 * access is granted. The request itself is not retried: the script runs again. Since P4.1 it uses the Studio theme.
 * zh-CN: 路线图 P2.1 的存储引导: 脚本请求需要插件尚未获得的所有文件访问权限时打开 (STORAGE_PERMISSION_DENIED, 每分钟最多一次).
 * 它说明原因并打开系统页面, 授权后自行关闭. 请求本身不会重试, 由脚本重新运行. P4.1 起使用 Studio 主题.
 */
class StorageAccessActivity : StandaloneActivity() {

    override fun onResume() {
        super.onResume()
        if (StorageAccess.isGranted(this)) finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE && StorageAccess.isGranted(this)) finish()
    }

    @Composable
    override fun Content() {
        SlateScreen(title = stringResource(R.string.app_name), onBack = ::finish) { padding ->
            EmptyState(
                title = stringResource(R.string.home_storage_title),
                body = stringResource(R.string.storage_guide_note) + "\n\n" + stringResource(R.string.home_storage_denied),
                icon = R.drawable.ic_storage_24,
                primary = EmptyAction(stringResource(R.string.home_storage_grant), { StorageAccess.request(this, REQUEST_STORAGE) }, "slate.storage.grant"),
                secondary = listOf(EmptyAction(stringResource(R.string.storage_guide_close), ::finish, "slate.storage.close")),
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }

    private companion object {
        const val REQUEST_STORAGE = 1
    }

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.storage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.SlateTheme

/**
 * The storage guide of roadmap P2.1: opened when a script request needs all files access the plugin does not hold
 * (STORAGE_PERMISSION_DENIED, at most once a minute). It explains why and opens the system page; it closes itself once
 * access is granted. The request itself is not retried: the script runs again.
 * zh-CN: 路线图 P2.1 的存储引导: 脚本请求需要插件尚未获得的所有文件访问权限时打开 (STORAGE_PERMISSION_DENIED, 每分钟最多一次).
 * 它说明原因并打开系统页面, 授权后自行关闭. 请求本身不会重试, 由脚本重新运行.
 */
class StorageAccessActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SlateTheme {
                StorageAccessScreen(onGrant = { StorageAccess.request(this, REQUEST_STORAGE) }, onClose = ::finish)
            }
        }
    }

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

    private companion object {

        const val REQUEST_STORAGE = 1

    }

}

@Composable
private fun StorageAccessScreen(onGrant: () -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The brand name starts with a digit: lay it out by its own direction (Three bidi rule).
            // zh-CN: 品牌名以数字开头, 按其自身方向排版 (Three 系列双向规则).
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(textDirection = TextDirection.Content),
            )
            Text(text = stringResource(R.string.home_storage_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.storage_guide_note), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(R.string.home_storage_denied), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGrant) { Text(text = stringResource(R.string.home_storage_grant)) }
                TextButton(onClick = onClose) { Text(text = stringResource(R.string.storage_guide_close)) }
            }
        }
    }
}

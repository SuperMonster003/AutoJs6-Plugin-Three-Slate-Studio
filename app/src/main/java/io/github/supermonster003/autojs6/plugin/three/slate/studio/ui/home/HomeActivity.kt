package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.SlateTheme

/**
 * Skeleton home screen (roadmap P0.1): the plugin description and the all files access grant of roadmap D9. Roadmap
 * P4.1 replaces it with the Studio home, task queue, settings and release history.
 * zh-CN: 骨架首页 (路线图 P0.1): 插件描述与路线图 D9 的所有文件访问授权. 路线图 P4.1 换成 Studio 首页, 任务队列, 设置与
 * 发行历史.
 */
class HomeActivity : ComponentActivity() {

    private var storageGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SlateTheme {
                HomeScreen(storageGranted = storageGranted, onGrantStorage = ::requestStorageAccess)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        storageGranted = StorageAccess.isGranted(this)
    }

    private fun requestStorageAccess() = StorageAccess.request(this, REQUEST_STORAGE)

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE) storageGranted = StorageAccess.isGranted(this)
    }

    private companion object {

        const val REQUEST_STORAGE = 1

    }

}

@Composable
private fun HomeScreen(storageGranted: Boolean, onGrantStorage: () -> Unit) {
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
            Text(text = stringResource(R.string.plugin_description), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(R.string.home_storage_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(if (storageGranted) R.string.home_storage_granted else R.string.home_storage_denied),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!storageGranted) {
                Button(onClick = onGrantStorage) { Text(text = stringResource(R.string.home_storage_grant)) }
            }
            Text(
                text = stringResource(R.string.home_skeleton_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

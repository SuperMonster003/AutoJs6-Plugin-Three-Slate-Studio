package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R

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
        storageGranted = hasStorageAccess(this)
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appPage = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:$packageName".toUri())
            try {
                startActivity(appPage)
            } catch (_: ActivityNotFoundException) {
                // Some ROMs only offer the list page. zh-CN: 部分 ROM 只提供列表页.
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            @Suppress("DEPRECATION")
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE) storageGranted = hasStorageAccess(this)
    }

    internal companion object {

        private const val REQUEST_STORAGE = 1

        /**
         * All files access on Android 11+, the legacy storage permissions below (roadmap D9).
         * zh-CN: Android 11 及以上为所有文件访问, 以下为旧版存储权限 (路线图 D9).
         */
        fun hasStorageAccess(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return Environment.isExternalStorageManager()
            return listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
        }

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

@Composable
private fun SlateTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

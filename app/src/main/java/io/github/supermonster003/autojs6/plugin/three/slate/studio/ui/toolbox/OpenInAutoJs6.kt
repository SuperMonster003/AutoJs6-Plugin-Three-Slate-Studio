package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.AutoJs6Scripts
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.GeneratedScript
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The "Open in AutoJs6" button of a generated script (roadmap D36, P4.4): shown only when the host is installed and new
 * enough ([AutoJs6Scripts.isAvailable]); large code goes through the cache and a content URI, which is deleted when the
 * host answers. The host shows its own message when it refuses a script; [snackbar] reports what fails before that. The
 * timeline editor (P4.3) and the recorder (P4.5) use the same button.
 * zh-CN: 生成脚本的 "在 AutoJs6 中打开" 按钮 (路线图 D36, P4.4): 只在宿主已安装且版本足够时显示 ([AutoJs6Scripts.isAvailable]);
 * 大代码经缓存与 content URI 发送, 宿主应答后删除. 宿主拒绝脚本时会自己提示; [snackbar] 报告此前的失败. 时间线剪辑器 (P4.3) 与录音页
 * (P4.5) 使用同一按钮.
 */
@Composable
internal fun OpenInAutoJs6Button(script: GeneratedScript, snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val available = remember { AutoJs6Scripts.isAvailable(context) }
    if (!available) return
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // The host answers once the editor has the code (or with the reason it refused). zh-CN: 编辑器拿到代码后宿主应答 (或给出拒绝原因).
        val error = result.data?.getStringExtra(AutoJs6Scripts.EXTRA_ERROR)
        Log.i(TAG, "CREATE_SCRIPT answered: " + (error ?: if (result.resultCode == Activity.RESULT_OK) "accepted" else "cancelled"))
        AutoJs6Scripts.cleanUp(context, pending)
        pending = null
    }
    val failed = stringResource(R.string.open_in_autojs6_failed)
    val tooLarge = stringResource(R.string.open_in_autojs6_too_large)
    val source = stringResource(R.string.app_name)
    OutlinedButton(
        onClick = {
            // Prepared on an IO thread, launched on the main thread. zh-CN: 在 IO 线程准备, 在主线程启动.
            scope.launch(Dispatchers.Main.immediate) {
                val request = withContext(Dispatchers.IO) { runCatching { AutoJs6Scripts.intentOf(context, script, source) } }
                    .getOrElse {
                        Log.w(TAG, "cannot prepare the script", it)
                        snackbar.showSnackbar(failed)
                        return@launch
                    }
                when (request) {
                    is AutoJs6Scripts.Request.TooLarge -> {
                        Log.w(TAG, "CREATE_SCRIPT not sent: ${request.bytes} bytes")
                        snackbar.showSnackbar(tooLarge)
                    }
                    is AutoJs6Scripts.Request.Ready -> try {
                        pending = request.file
                        launcher.launch(request.intent)
                        Log.i(TAG, "CREATE_SCRIPT sent: " + if (request.file == null) "inline" else "content URI")
                    } catch (e: RuntimeException) {
                        // ActivityNotFoundException or SecurityException (no PLUGIN permission) above all.
                        // zh-CN: 主要是找不到入口 (ActivityNotFoundException) 或缺少 PLUGIN 权限 (SecurityException).
                        Log.w(TAG, "CREATE_SCRIPT not sent", e)
                        AutoJs6Scripts.cleanUp(context, request.file)
                        pending = null
                        snackbar.showSnackbar(failed)
                    }
                }
            }
        },
        modifier = modifier.testTag("slate.code.openInAutoJs6"),
    ) {
        Icon(painterResource(R.drawable.ic_open_in_new_24), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_open_in_autojs6))
    }
}

private const val TAG = "SlateOpenInAutoJs6"

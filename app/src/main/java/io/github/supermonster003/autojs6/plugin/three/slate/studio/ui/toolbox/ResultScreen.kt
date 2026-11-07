package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.MediaActions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioTasks
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.TaskRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.stageLabel
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.stateLabel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys

/**
 * The result page of a wizard task (roadmap P4.2): state and progress until it ends, then the output path(s), the media
 * info of the result, open / share, the failure code and detail, and the equivalent script call (copyable). The full
 * code generator with the host entry is roadmap P4.4.
 * zh-CN: 向导任务的结果页 (路线图 P4.2): 结束前显示状态与进度, 之后显示输出路径, 结果的媒体信息, 打开 / 分享, 失败码与详情, 以及等价
 * 的脚本调用 (可复制). 完整的代码生成器与宿主入口是路线图 P4.4.
 */
@Composable
internal fun ResultScreen(key: String, onBack: () -> Unit, onOpenDetail: (String) -> Unit, onRunAgain: (op: String, input: String?) -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val row = StudioTasks.rows.firstOrNull { it.key == key } ?: StudioTasks.row(key)
    val labels = row?.let { Labels.operations[it.request?.schema?.op ?: it.op] }
    SlateScreen(
        title = labels?.let { stringResource(it.title) } ?: stringResource(R.string.result_title),
        onBack = onBack,
        snackbarHostState = snackbar,
    ) { padding ->
        if (row == null) {
            EmptyState(title = stringResource(R.string.result_gone), icon = R.drawable.ic_history_24, modifier = Modifier.padding(padding))
            return@SlateScreen
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("slate.result")) {
            SectionHeader(stringResource(R.string.result_state))
            Text(
                stringResource(stateLabel(row.state)),
                style = MaterialTheme.typography.titleMedium,
                color = if (row.state == MediaStudioContract.TASK_FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp).testTag("slate.result.state"),
            )
            if (!row.isFinal) {
                Spacer(Modifier.height(8.dp))
                if (row.state == MediaStudioContract.TASK_QUEUED) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                else LinearProgressIndicator(progress = { (row.percent / 100.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                Text(
                    stringResource(R.string.tasks_progress, String.format(Locale.ROOT, "%.0f%%", row.percent), stringResource(stageLabel(row.stage))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                if (row.taskId != null) {
                    OutlinedButton(onClick = { StudioTasks.cancel(context, row) }, modifier = Modifier.padding(horizontal = 24.dp).testTag("slate.result.cancel")) {
                        Text(stringResource(R.string.ui_dialog_cancel))
                    }
                }
            }
            if (row.state == MediaStudioContract.TASK_FAILED || row.state == MediaStudioContract.TASK_CANCELLED) {
                row.errorCode?.let { code ->
                    Text(
                        listOfNotNull(code, row.errorMessage?.takeIf { it.isNotBlank() }).joinToString(": "),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("slate.result.error"),
                    )
                }
            }
            val outputs = outputsOf(row)
            if (row.state == MediaStudioContract.TASK_COMPLETED && outputs.isNotEmpty()) {
                SectionHeader(stringResource(R.string.result_output))
                outputs.take(MAX_LISTED).forEachIndexed { index, path ->
                    if (index > 0) Hairline(insetStart = 24.dp)
                    val isFile = File(path).isFile
                    SettingsRow(File(path).name, summary = path, showChevron = isFile, onClick = if (isFile) { { onOpenDetail(path) } } else null,
                        modifier = Modifier.testTag("slate.result.output.$index"))
                }
                if (outputs.size > MAX_LISTED) {
                    Text(stringResource(R.string.result_more, outputs.size - MAX_LISTED), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                }
                val failed = stringResource(R.string.action_failed)
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val files = outputs.filter { File(it).isFile }
                    if (files.size == 1) {
                        OutlinedButton(onClick = { if (!MediaActions.open(context, files[0])) scope.launch { snackbar.showSnackbar(failed) } },
                            modifier = Modifier.testTag("slate.result.open")) {
                            Icon(painterResource(R.drawable.ic_open_in_new_24), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_open_with))
                        }
                    }
                    if (files.isNotEmpty()) {
                        OutlinedButton(onClick = { if (!MediaActions.share(context, files)) scope.launch { snackbar.showSnackbar(failed) } },
                            modifier = Modifier.testTag("slate.result.share")) {
                            Icon(painterResource(R.drawable.ic_share_24), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_share))
                        }
                    }
                }
                val info = infoRows(context, row.result)
                if (info.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.result_info))
                    info.forEachIndexed { index, (label, value) ->
                        if (index > 0) Hairline(insetStart = 24.dp)
                        SettingsRow(stringResource(label), summary = value, showChevron = false, modifier = Modifier.testTag("slate.result.info.$index"))
                    }
                }
                val warnings = row.result?.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty()
                if (warnings.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.result_warnings))
                    warnings.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp))
                    }
                }
            }
            val request = row.request
            if (request != null) {
                SectionHeader(stringResource(R.string.wizard_code))
                CodeCard(request.code, copyable = true, tag = "slate.result.code")
                OutlinedButton(
                    onClick = { onRunAgain(request.schema.op, request.inputs.singleOrNull()) },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("slate.result.again"),
                ) { Text(stringResource(R.string.result_run_again)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The output paths: `paths` (frames), else `path`, else the requested output. zh-CN: 输出路径. */
private fun outputsOf(row: TaskRow): List<String> {
    val result = row.result
    result?.getStringArray(MediaStudioResultKeys.PATHS)?.takeIf { it.isNotEmpty() }?.let { return it.toList() }
    result?.getString(MediaStudioResultKeys.PATH)?.let { return listOf(it) }
    return listOfNotNull(row.output)
}

private fun infoRows(context: Context, result: Bundle?): List<Pair<Int, String>> {
    if (result == null) return emptyList()
    return buildList {
        result.getLong(MediaStudioResultKeys.DURATION_MILLIS, -1).takeIf { it > 0 }?.let { add(R.string.option_duration to Formats.duration(it)) }
        val width = result.getInt(MediaStudioResultKeys.WIDTH, 0)
        val height = result.getInt(MediaStudioResultKeys.HEIGHT, 0)
        if (width > 0 && height > 0) add(R.string.detail_resolution to Formats.resolution(width, height))
        result.getString(MediaStudioResultKeys.VIDEO_CODEC)?.let { add(R.string.detail_video_codec to it) }
        result.getString(MediaStudioResultKeys.AUDIO_CODEC)?.let { add(R.string.detail_audio_codec to it) }
        if (!result.containsKey(MediaStudioResultKeys.VIDEO_CODEC) && !result.containsKey(MediaStudioResultKeys.AUDIO_CODEC)) {
            result.getString(MediaStudioResultKeys.CODEC)?.let { add(R.string.option_codec to it) }
        }
        result.getDouble(MediaStudioResultKeys.FRAME_RATE, 0.0).takeIf { it > 0 }?.let { add(R.string.option_frame_rate to Formats.frameRate(it)) }
        result.getLong(MediaStudioResultKeys.BIT_RATE, 0).takeIf { it > 0 }?.let { add(R.string.option_bit_rate to Formats.bitRate(it)) }
        result.getInt(MediaStudioResultKeys.SAMPLE_RATE, 0).takeIf { it > 0 }?.let { add(R.string.option_sample_rate to Formats.sampleRate(it)) }
        result.getInt(MediaStudioResultKeys.CHANNELS, 0).takeIf { it > 0 }?.let { add(R.string.option_channels to it.toString()) }
        result.getString(MediaStudioResultKeys.MIME)?.let { add(R.string.detail_format to it) }
        result.getLong(MediaStudioResultKeys.SIZE_BYTES, -1).takeIf { it >= 0 }?.let { add(R.string.detail_size to Formats.size(context, it)) }
    }
}

private const val MAX_LISTED = 20

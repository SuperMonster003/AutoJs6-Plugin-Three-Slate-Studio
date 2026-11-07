package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.Labels
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import org.autojs.plugin.mediastudio.api.MediaStudioContract

/**
 * The task queue (roadmap P4.1, D35): Studio and script tasks of the shared executor, newest first, with progress, stage,
 * cancel and the failure reason (for script tasks too, kept by the runner: appendix D Q51 (b)); finished Studio tasks open
 * their result page, finished script tasks their output.
 * zh-CN: 任务队列 (路线图 P4.1, D35): 共用执行器中的 Studio 与脚本任务, 最新在前, 带进度, 阶段, 取消与失败原因 (脚本任务的失败原因由
 * 执行器保存: 附录 D Q51 (b)); 已结束的 Studio 任务打开结果页, 已结束的脚本任务打开输出.
 */
@Composable
internal fun TasksScreen(onOpenResult: (String) -> Unit, onOpenDetail: (String) -> Unit) {
    val context = LocalContext.current
    // The runner notifies state changes; progress of script tasks is polled while the page shows.
    // zh-CN: 执行器通知状态变化; 页面显示期间轮询脚本任务的进度.
    LaunchedEffect(Unit) {
        while (true) {
            StudioTasks.refresh(context)
            delay(POLL_MILLIS)
        }
    }
    val rows = StudioTasks.rows
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize().testTag("slate.tasks.empty")) {
            EmptyState(title = stringResource(R.string.tasks_empty_title), body = stringResource(R.string.tasks_empty_body), icon = R.drawable.ic_list_24)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().testTag("slate.tasks")) {
        items(rows, key = { it.key }) { row ->
            TaskItem(
                row = row,
                onClick = when {
                    row.request != null -> { { onOpenResult(row.key) } }
                    row.state == MediaStudioContract.TASK_COMPLETED && row.output != null && File(row.output).isFile -> { { onOpenDetail(row.output) } }
                    else -> null
                },
                onCancel = { StudioTasks.cancel(context, row) },
            )
            Hairline(insetStart = 24.dp)
        }
    }
}

@Composable
private fun TaskItem(row: TaskRow, onClick: (() -> Unit)?, onCancel: () -> Unit) {
    val labels = Labels.operations[row.op]
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 12.dp).testTag("slate.tasks.item"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(labels?.icon ?: R.drawable.ic_schedule_24), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (labels != null) stringResource(labels.title) else row.op,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.tasks_origin_state, stringResource(originLabel(row.origin)), stringResource(stateLabel(row.state))),
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.state == MediaStudioContract.TASK_FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!row.isFinal) {
                Spacer(Modifier.height(6.dp))
                val fraction = (row.percent / 100.0).toFloat().coerceIn(0f, 1f)
                if (row.state == MediaStudioContract.TASK_QUEUED) LinearProgressIndicator(Modifier.fillMaxWidth())
                else LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.tasks_progress, String.format(Locale.ROOT, "%.0f%%", row.percent), stringResource(stageLabel(row.stage))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (row.state == MediaStudioContract.TASK_FAILED && row.errorCode != null) {
                Text(
                    listOfNotNull(row.errorCode, row.errorMessage?.takeIf { it.isNotBlank() }).joinToString(": "),
                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("slate.tasks.error"),
                )
            }
            row.output?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
            }
        }
        if (!row.isFinal && row.taskId != null) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onCancel, modifier = Modifier.testTag("slate.tasks.cancel")) { Text(stringResource(R.string.ui_dialog_cancel)) }
        } else if (row.isFinal) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painterResource(if (row.state == MediaStudioContract.TASK_COMPLETED) R.drawable.ic_check_circle_24 else R.drawable.ic_error_24),
                contentDescription = null,
                tint = if (row.state == MediaStudioContract.TASK_COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@StringRes
internal fun stateLabel(state: String): Int = when (state) {
    MediaStudioContract.TASK_QUEUED -> R.string.tasks_state_queued
    MediaStudioContract.TASK_RUNNING -> R.string.tasks_state_running
    MediaStudioContract.TASK_COMPLETED -> R.string.tasks_state_completed
    MediaStudioContract.TASK_FAILED -> R.string.tasks_state_failed
    else -> R.string.tasks_state_cancelled
}

@StringRes
internal fun stageLabel(stage: String): Int = when (stage) {
    MediaStudioContract.STAGE_QUEUED -> R.string.tasks_stage_queued
    MediaStudioContract.STAGE_PROBING -> R.string.tasks_stage_probing
    MediaStudioContract.STAGE_DECODING -> R.string.tasks_stage_decoding
    MediaStudioContract.STAGE_ENCODING -> R.string.tasks_stage_encoding
    MediaStudioContract.STAGE_MUXING -> R.string.tasks_stage_muxing
    else -> R.string.tasks_stage_writing
}

@StringRes
private fun originLabel(origin: String): Int = if (origin == MediaStudioContract.ORIGIN_STUDIO) R.string.tasks_origin_studio else R.string.tasks_origin_script

private const val POLL_MILLIS = 500L

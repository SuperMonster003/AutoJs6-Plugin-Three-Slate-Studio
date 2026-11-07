package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.record

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.InputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionSchemas
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.Labels
import java.io.File

/**
 * "Send to toolbox" (roadmap P4.5): the wizards that take a media file of one kind as their input, for the library rows
 * and the saved recording; the chosen wizard opens with the file as its input.
 * zh-CN: "送进工具箱" (路线图 P4.5): 以某类媒体文件为输入的向导, 供媒体库各行与保存的录音使用; 选中的向导以该文件为输入打开.
 */
internal object ToolboxTargets {

    fun operations(kind: MediaKind): List<OperationSchema> = OptionSchemas.ALL.filter { schema ->
        when (schema.inputs.kind) {
            InputKind.VIDEO -> kind == MediaKind.VIDEO
            InputKind.AUDIO -> kind == MediaKind.AUDIO
            InputKind.MEDIA -> kind == MediaKind.VIDEO || kind == MediaKind.AUDIO
            InputKind.SUBTITLE -> false
        }
    }
}

/** The wizards for [path] of [kind]. zh-CN: 适用于 [kind] 类型文件 [path] 的向导. */
@Composable
internal fun SendToToolboxDialog(path: String, kind: MediaKind, onDismiss: () -> Unit, onChoose: (op: String) -> Unit) {
    val operations = ToolboxTargets.operations(kind)
    SlateDialog(
        title = stringResource(R.string.record_send_to_toolbox),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_cancel),
        onConfirm = onDismiss,
        dismissText = null,
        tag = "slate.dialog.sendToToolbox",
    ) {
        Text(
            stringResource(R.string.record_send_to_toolbox_message, Formats.isolate(File(path).name)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding).padding(bottom = 8.dp),
        )
        operations.forEachIndexed { index, schema ->
            if (index > 0) Hairline()
            val labels = Labels.operations.getValue(schema.op)
            SettingsRow(
                stringResource(labels.title),
                summary = stringResource(labels.summary),
                icon = labels.icon,
                showChevron = false,
                onClick = { onChoose(schema.op) },
                modifier = Modifier.testTag("slate.sendToToolbox.${schema.op}"),
            )
        }
    }
}

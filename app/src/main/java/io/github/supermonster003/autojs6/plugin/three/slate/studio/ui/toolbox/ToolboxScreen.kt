package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationCategory
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionSchemas
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SettingsRow

/**
 * The toolbox tab (roadmap D35, P4.2): one wizard per processing API, grouped as video, image, audio and subtitles,
 * after the Studio recorder (roadmap P4.5) when [onRecord] is given.
 * zh-CN: 工具箱标签 (路线图 D35, P4.2): 每个加工 API 一个向导, 按视频, 图片, 音频与字幕分组; 给出 [onRecord] 时前面是 Studio 录音
 * (路线图 P4.5).
 */
@Composable
internal fun ToolboxScreen(onOpen: (String) -> Unit, onRecord: (() -> Unit)? = null) {
    LazyColumn(Modifier.fillMaxSize().testTag("slate.toolbox")) {
        if (onRecord != null) {
            item(key = "header.record") { SectionHeader(stringResource(R.string.record_section)) }
            item(key = "record") {
                SettingsRow(
                    stringResource(R.string.record_title),
                    summary = stringResource(R.string.record_toolbox_summary),
                    icon = R.drawable.ic_mic_24,
                    onClick = onRecord,
                    modifier = Modifier.testTag("slate.toolbox.record"),
                )
            }
        }
        OperationCategory.entries.forEach { category ->
            val schemas = OptionSchemas.inCategory(category)
            if (schemas.isEmpty()) return@forEach
            item(key = "header.$category") { SectionHeader(stringResource(Labels.category(category))) }
            schemas.forEachIndexed { index, schema ->
                item(key = schema.op) {
                    if (index > 0) Hairline()
                    val labels = Labels.operations.getValue(schema.op)
                    SettingsRow(
                        stringResource(labels.title),
                        summary = stringResource(labels.summary),
                        icon = labels.icon,
                        onClick = { onOpen(schema.op) },
                        modifier = Modifier.testTag("slate.toolbox.${schema.op}"),
                    )
                }
            }
        }
        item(key = "bottom") { Spacer(Modifier.height(24.dp)) }
    }
}

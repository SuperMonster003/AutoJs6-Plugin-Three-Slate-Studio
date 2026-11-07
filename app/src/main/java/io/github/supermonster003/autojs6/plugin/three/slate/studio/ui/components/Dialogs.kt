package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.ColorPolicy
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.ThemeAccentRoles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.ThemeColorValue

/** Geometry of the centered dialogs (specification section 8). zh-CN: 居中对话框的几何尺寸 (规范第 8 节). */
internal object DialogDefaults {
    val cornerRadius = 24.dp
    val maxWidth = 560.dp
    val horizontalMargin = 24.dp
    val contentPadding = 24.dp
    val buttonMinHeight = 48.dp
    const val MAX_HEIGHT_FRACTION = 0.85f
}

/**
 * Centered dialog: 24 dp corners, at most 560 dp wide and 85% of the window high; the title and the buttons stay fixed
 * while [content] scrolls. Dismissing (back, an outside tap, the dismiss button) never confirms. [dismissText] null
 * leaves only the confirm button (information dialogs).
 * zh-CN: 居中对话框: 24 dp 圆角, 宽至多 560 dp, 高至多窗口的 85%; 标题与按钮固定, 只有 [content] 滚动. 关闭 (返回, 点击外部, 取消按钮)
 * 从不确认. [dismissText] 为 null 时只有确定按钮 (信息对话框).
 */
@Composable
internal fun SlateDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String?,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    scrollable: Boolean = true,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    tag: String = "slate.dialog",
    content: @Composable ColumnScope.() -> Unit,
) {
    // The dialog window covers the screen and its content keeps clear of the system bars and the keyboard, so the
    // title and the buttons stay visible while only the content scrolls (specification section 8). A tap beside the
    // surface dismisses, as the platform dialog does. The dialog is a window of its own, so its test tags are exposed as
    // resource ids here too (the device scripts find controls by them, uiautomator).
    // zh-CN: 对话框是独立窗口, 所以这里也把测试标签作为资源 id 暴露 (设备脚本按它查找控件, uiautomator).
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            modifier = modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .pointerInput(onDismissRequest) { detectTapGestures(onTap = { onDismissRequest() }) }
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            val width = (maxWidth - DialogDefaults.horizontalMargin * 2).coerceAtMost(DialogDefaults.maxWidth)
            val height = maxHeight * DialogDefaults.MAX_HEIGHT_FRACTION
            Surface(
                modifier = Modifier
                    .widthIn(max = width)
                    .width(width)
                    .heightIn(max = height)
                    // Taps on the surface itself never reach the dismissing background.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag(tag),
                shape = RoundedCornerShape(DialogDefaults.cornerRadius),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = DialogDefaults.contentPadding, end = DialogDefaults.contentPadding, top = DialogDefaults.contentPadding, bottom = 16.dp),
                    )
                    val scroll = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
                    Column(modifier = Modifier.weight(1f, fill = false).then(scroll)) { content() }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (neutralText != null && onNeutral != null) {
                            TextButton(onClick = onNeutral, modifier = Modifier.defaultMinSize(minHeight = DialogDefaults.buttonMinHeight).testTag("$tag.neutral")) {
                                Text(neutralText)
                            }
                            Spacer(Modifier.weight(1f))
                        }
                        if (dismissText != null) {
                            TextButton(onClick = onDismissRequest, modifier = Modifier.defaultMinSize(minHeight = DialogDefaults.buttonMinHeight).testTag("$tag.cancel")) {
                                Text(dismissText)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        TextButton(
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            modifier = Modifier.defaultMinSize(minHeight = DialogDefaults.buttonMinHeight).testTag("$tag.confirm"),
                        ) { Text(confirmText) }
                    }
                }
            }
        }
    }
}

/** A dialog with a message, a confirm and an optional dismiss button. zh-CN: 带消息, 确定按钮与可选取消按钮的对话框. */
@Composable
internal fun MessageDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.ui_dialog_confirm),
    dismissText: String? = stringResource(R.string.ui_dialog_cancel),
    tag: String = "slate.dialog",
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    SlateDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText,
        neutralText = neutralText,
        onNeutral = onNeutral,
        tag = tag,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding).testTag("$tag.message"),
        )
        extra()
        Spacer(Modifier.height(8.dp))
    }
}

/** One option of a choice dialog: 16 sp label, optional 14 sp note. zh-CN: 选择对话框的一个选项: 16 sp 文字, 可选 14 sp 说明. */
internal data class Choice(val label: String, val note: String? = null, val enabled: Boolean = true)

/**
 * Single choice with "select, then confirm" (specification section 8): a row only changes the pending index, [onConfirm]
 * fires once on confirm, and cancel, back or an outside tap save nothing. The pending index survives recreation.
 * zh-CN: "先选后确定" 的单选 (规范第 8 节): 点击行只改变待定索引, 确定时触发一次 [onConfirm], 取消, 返回与点击外部不保存. 待定索引在重建后保留.
 */
@Composable
internal fun ChoiceDialog(
    title: String,
    options: List<Choice>,
    selectedIndex: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    tag: String = "slate.choice",
) {
    var pending by rememberSaveable(selectedIndex) { mutableIntStateOf(selectedIndex) }
    val valid = pending in options.indices && options[pending].enabled
    SlateDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { if (valid) onConfirm(pending) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = valid,
        modifier = modifier,
        tag = tag,
    ) {
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding).padding(bottom = 8.dp),
            )
        }
        options.forEachIndexed { index, option ->
            ChoiceRow(option, selected = index == pending, onSelect = { pending = index }, modifier = Modifier.testTag("$tag.$index"))
        }
    }
}

/**
 * Multiple choice with the semantics of [ChoiceDialog]; [requireOne] keeps confirm disabled until one option is ticked.
 * zh-CN: 语义同 [ChoiceDialog] 的多选; [requireOne] 时至少勾选一项才能确定.
 */
@Composable
internal fun MultiChoiceDialog(
    title: String,
    options: List<Choice>,
    selected: Set<Int>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<Int>) -> Unit,
    confirmText: String = stringResource(R.string.ui_dialog_confirm),
    requireOne: Boolean = false,
    message: String? = null,
    tag: String = "slate.multiChoice",
) {
    var pending by rememberSaveable(selected) { mutableStateOf(selected.sorted().toIntArray()) }
    val chosen = pending.filter { it in options.indices && options[it].enabled }.toSet()
    val valid = !requireOne || chosen.isNotEmpty()
    SlateDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        confirmText = confirmText,
        onConfirm = { if (valid) onConfirm(chosen) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = valid,
        tag = tag,
    ) {
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding).padding(bottom = 8.dp),
            )
        }
        options.forEachIndexed { index, option ->
            CheckRow(
                option = option,
                checked = index in chosen,
                onCheckedChange = { checked -> pending = (if (checked) pending.toSet() + index else pending.toSet() - index).sorted().toIntArray() },
                modifier = Modifier.testTag("$tag.$index"),
            )
        }
    }
}

/** One radio row of a single-choice list. zh-CN: 单选列表的一行. */
@Composable
internal fun ChoiceRow(option: Choice, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val minHeight = if (option.note.isNullOrEmpty()) 56.dp else 72.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = option.enabled, role = Role.RadioButton, onClick = onSelect)
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = DialogDefaults.contentPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = option.enabled)
        Spacer(Modifier.width(16.dp))
        OptionTexts(option)
    }
}

/** One checkbox row of a multiple-choice list. zh-CN: 多选列表的一行. */
@Composable
internal fun CheckRow(option: Choice, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val minHeight = if (option.note.isNullOrEmpty()) 56.dp else 72.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = option.enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = DialogDefaults.contentPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = option.enabled)
        Spacer(Modifier.width(16.dp))
        OptionTexts(option)
    }
}

@Composable
private fun OptionTexts(option: Choice) {
    Column {
        Text(option.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.disabledIf(!option.enabled))
        if (!option.note.isNullOrEmpty()) {
            Text(
                option.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.disabledIf(!option.enabled),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Theme color picker (specification section 9): follow AutoJs6, the shared 16 presets, a HEX / RGB input and a local
 * preview. Nothing outside the dialog changes until confirm; `null` in [onConfirm] means "follow AutoJs6".
 * zh-CN: 主题色选择器 (规范第 9 节): 跟随 AutoJs6, 统一的 16 个预设, HEX / RGB 输入与局部预览. 确定之前对话框外的一切不变; [onConfirm] 收到
 * `null` 表示跟随 AutoJs6.
 */
@Composable
internal fun ThemeColorDialog(
    currentColor: Int?,
    hostColor: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    var followsHost by rememberSaveable { mutableStateOf(currentColor == null) }
    var fromPreset by rememberSaveable { mutableStateOf(currentColor != null && currentColor in ThemeColorValue.presets) }
    var selected by rememberSaveable { mutableIntStateOf(currentColor ?: hostColor) }
    var text by rememberSaveable { mutableStateOf(ThemeColorValue.hex(currentColor ?: hostColor)) }
    val valid = ThemeColorValue.parse(text) != null
    val effective = if (followsHost) hostColor else selected
    val dark = ColorPolicy.luminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.2
    val roles = ThemeAccentRoles.fromSeed(effective, dark)
    val presetsLabel = stringResource(R.string.ui_theme_presets)
    val previewLabel = stringResource(R.string.ui_theme_preview)
    SlateDialog(
        title = stringResource(R.string.settings_theme_color),
        onDismissRequest = onDismissRequest,
        confirmText = stringResource(R.string.ui_dialog_confirm),
        onConfirm = { if (followsHost || valid) onConfirm(if (followsHost) null else selected) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = followsHost || valid,
        tag = "slate.theme",
    ) {
        Column(modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding)) {
            ThemeRadioRow(followsHost, stringResource(R.string.settings_follow_autojs6), ThemeColorValue.hex(hostColor), { followsHost = true }, Modifier.testTag("slate.theme.follow"))
            Text(presetsLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            PresetGrid(
                selectedSeed = if (!followsHost && fromPreset && valid) selected else null,
                label = presetsLabel,
                onSelect = { seed ->
                    followsHost = false
                    fromPreset = true
                    selected = seed
                    text = ThemeColorValue.hex(seed)
                },
            )
            ThemeRadioRow(!followsHost && !fromPreset, stringResource(R.string.ui_theme_custom), null, { followsHost = false; fromPreset = false }, Modifier.testTag("slate.theme.custom"))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.take(64)
                    followsHost = false
                    fromPreset = false
                    ThemeColorValue.parse(text)?.let { selected = it }
                },
                label = { Text(stringResource(R.string.ui_theme_input)) },
                placeholder = { Text("#RRGGBB / rgb(r, g, b)") },
                isError = !followsHost && !valid,
                supportingText = if (!followsHost && !valid) ({ Text(stringResource(R.string.ui_theme_invalid)) }) else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("slate.theme.input"),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .background(Color(roles.primary), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "$previewLabel ${ThemeColorValue.hex(effective)}" }
                    .testTag("slate.theme.preview"),
                contentAlignment = Alignment.Center,
            ) {
                Text(previewLabel, color = Color(roles.onPrimary), style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = "${ThemeColorValue.hex(effective)}  ${ThemeColorValue.rgb(effective)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("slate.theme.value"),
            )
        }
    }
}

@Composable
private fun ThemeRadioRow(selected: Boolean, label: String, description: String?, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PresetGrid(selectedSeed: Int?, label: String, onSelect: (Int) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 56.dp).toInt().coerceIn(1, 8)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ThemeColorValue.presets.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { seed ->
                        Box(modifier = Modifier.weight(1f).height(52.dp), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .selectable(selected = seed == selectedSeed, role = Role.RadioButton) { onSelect(seed) }
                                    .background(Color(seed), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                                    .semantics { contentDescription = "$label ${ThemeColorValue.hex(seed)}" }
                                    .testTag("slate.theme.preset.${ThemeColorValue.hex(seed)}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (seed == selectedSeed) {
                                    Icon(painterResource(R.drawable.ic_check_24), contentDescription = null, tint = Color(ColorPolicy.onFilledColor(seed)), modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

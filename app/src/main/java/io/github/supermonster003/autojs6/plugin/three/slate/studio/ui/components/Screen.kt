package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R

/**
 * Geometry of the settings list (standalone settings specification section 4). zh-CN: 设置列表的几何尺寸 (独立设置规范第 4 节).
 */
internal object RowDefaults {
    val horizontalPadding = 24.dp
    val verticalPadding = 12.dp
    val rowMinHeight = 72.dp
    val singleLineRowMinHeight = 56.dp
    val iconSize = 24.dp
    val iconSlot = 40.dp
    val textStart = 64.dp
    val headerTopPadding = 24.dp
    val headerBottomPadding = 8.dp
    const val CHEVRON_ALPHA = 0.6f
}

/** 38% alpha for disabled text and icons. zh-CN: 禁用的文字与图标使用 38% 不透明度. */
internal fun Color.disabledIf(disabled: Boolean): Color = if (disabled) copy(alpha = alpha * 0.38f) else this

/**
 * Standard page: neutral background, a top bar with the back arrow and the system bar insets handled by [Scaffold].
 * zh-CN: 标准页面: 中性背景, 带返回箭头的顶栏, 系统栏 insets 由 [Scaffold] 处理.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlateScreen(
    title: String,
    modifier: Modifier = Modifier,
    singleLineTitle: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                // The direction follows the text, so the Latin title "3-Slate Studio" keeps its order in RTL layouts.
                title = { Text(title, style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content), maxLines = if (singleLineTitle) 1 else 2,
                    overflow = if (singleLineTitle) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("slate.topBar.back")) {
                            Icon(painterResource(R.drawable.ic_arrow_back_24), contentDescription = stringResource(R.string.ui_navigation_back))
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState, modifier = Modifier.testTag("slate.snackbar")) },
        content = content,
    )
}

/** A top bar action: a 48 dp icon button with its description. zh-CN: 顶栏动作: 带描述的 48 dp 图标按钮. */
@Composable
internal fun TopBarAction(@DrawableRes icon: Int, description: String, tag: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
        Icon(painterResource(icon), contentDescription = description)
    }
}

/** Group title: 14 sp medium, secondary color, 24 / 8 dp; a heading for screen readers. zh-CN: 分组标题; 对读屏器为标题. */
@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .semantics { heading() }
            .fillMaxWidth()
            .padding(
                start = RowDefaults.horizontalPadding,
                end = RowDefaults.horizontalPadding,
                top = RowDefaults.headerTopPadding,
                bottom = RowDefaults.headerBottomPadding,
            ),
    )
}

/** In-group hairline from the text start (64 dp with icons). zh-CN: 组内淡分隔线, 从文字起点 (有图标时 64 dp) 开始. */
@Composable
internal fun Hairline(modifier: Modifier = Modifier, insetStart: Dp = RowDefaults.textStart) {
    HorizontalDivider(
        modifier = modifier.padding(start = insetStart, end = RowDefaults.horizontalPadding),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Setting row: optional leading icon, title and summary, optional chevron. The whole row is the click target; the
 * decorative icons carry no accessibility focus.
 * zh-CN: 设置行: 可选的前置图标, 标题与摘要, 可选的箭头. 整行是点击目标; 装饰图标不获得无障碍焦点.
 */
@Composable
internal fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    summaryColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    val minHeight = if (summary.isNullOrEmpty()) RowDefaults.singleLineRowMinHeight else RowDefaults.rowMinHeight
    val click = if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(click)
            .semantics(mergeDescendants = true) {}
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = RowDefaults.horizontalPadding, vertical = RowDefaults.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LeadingIcon(icon, enabled)
        RowTexts(title, summary, enabled, summaryColor, Modifier.weight(1f))
        if (showChevron && onClick != null) {
            Spacer(Modifier.width(16.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = RowDefaults.CHEVRON_ALPHA).disabledIf(!enabled),
                modifier = Modifier.size(RowDefaults.iconSize),
            )
        }
    }
}

/**
 * Setting row with a trailing switch: the row owns the toggle and fires [onToggle] once per tap; the switch only shows
 * the state (specification section 4).
 * zh-CN: 带开关的设置行: 由整行负责切换, 每次点击只触发一次 [onToggle]; 开关只显示状态 (规范第 4 节).
 */
@Composable
internal fun SwitchRow(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val minHeight = if (summary.isNullOrEmpty()) RowDefaults.singleLineRowMinHeight else RowDefaults.rowMinHeight
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onToggle)
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = RowDefaults.horizontalPadding, vertical = RowDefaults.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LeadingIcon(icon, enabled)
        RowTexts(title, summary, enabled, Color.Unspecified, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun LeadingIcon(@DrawableRes icon: Int, enabled: Boolean) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.disabledIf(!enabled),
        modifier = Modifier.size(RowDefaults.iconSize),
    )
    Spacer(Modifier.width(RowDefaults.iconSlot - RowDefaults.iconSize))
}

@Composable
private fun RowTexts(title: String, summary: String?, enabled: Boolean, summaryColor: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.disabledIf(!enabled))
        if (!summary.isNullOrEmpty()) {
            val color = if (summaryColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else summaryColor
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = color.disabledIf(!enabled),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Button of [EmptyState]. zh-CN: [EmptyState] 的按钮. */
internal data class EmptyAction(val label: String, val onClick: () -> Unit, val tag: String? = null)

/**
 * Empty, error and missing-permission state: 48 dp icon, 16 sp title, 14 sp body, one filled and up to two text actions.
 * zh-CN: 空状态, 错误与缺少权限状态: 48 dp 图标, 16 sp 标题, 14 sp 正文, 一个填充按钮与至多两个文字按钮.
 */
@Composable
internal fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    @DrawableRes icon: Int? = null,
    primary: EmptyAction? = null,
    secondary: List<EmptyAction> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp).testTag("slate.emptyState"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
        }
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        if (!body.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (primary != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = primary.onClick,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).then(primary.tag?.let { Modifier.testTag(it) } ?: Modifier),
            ) { Text(primary.label) }
        }
        if (secondary.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row {
                secondary.forEachIndexed { index, action ->
                    if (index > 0) Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = action.onClick,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).then(action.tag?.let { Modifier.testTag(it) } ?: Modifier),
                    ) { Text(action.label) }
                }
            }
        }
    }
}

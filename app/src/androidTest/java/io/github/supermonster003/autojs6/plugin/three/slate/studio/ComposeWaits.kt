package io.github.supermonster003.autojs6.plugin.three.slate.studio

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Whether a node matching [matcher] is shown, for the conditions of `waitUntil` right after a screen opens or closes
 * (plugin-center P21.2): between two screens (the settings page paused, the release history not resumed yet) the app may have no
 * Compose hierarchy at all, and the default query then throws "No compose hierarchies found" at once instead of letting
 * the wait go on. Here no hierarchy counts as "not shown yet".
 * zh-CN: 是否显示了与 [matcher] 匹配的节点, 用于界面刚打开或关闭时 `waitUntil` 的条件 (plugin-center P21.2): 两个界面之间 (设置页已暂停, 发行历史页尚未恢复) 应用可能
 * 完全没有 Compose 层级, 默认查询这时会立即抛出 "No compose hierarchies found", 而不是继续等待. 这里把没有层级视为 "尚未显示".
 * [useUnmergedTree] finds the nodes merged into a clickable parent, such as the lines of a clickable card.
 * zh-CN: [useUnmergedTree] 可找到合并进可点按父节点的节点, 例如可点按卡片中的各行.
 */
internal fun ComposeTestRule.shows(matcher: SemanticsMatcher, useUnmergedTree: Boolean = false): Boolean =
    onAllNodes(matcher, useUnmergedTree).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

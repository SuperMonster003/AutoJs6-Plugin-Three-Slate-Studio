package io.github.supermonster003.autojs6.plugin.three.slate.studio

import androidx.activity.ComponentActivity

/**
 * The empty host of the Compose tests that set their own content
 * (`ActivityScenario.launch(ComposeTestActivity::class.java)`, then `setContent`). It stands in for the activity of
 * `androidx.compose.ui:ui-test-manifest`, which is exported and would add an exported
 * `androidx.activity.ComponentActivity` to the debug APK; this one is not exported and keeps the same theme. Debug
 * builds only.
 *
 * zh-CN: 自行设置内容的 Compose 测试 (`ActivityScenario.launch(ComposeTestActivity::class.java)` 后 `setContent`)
 * 所用的空宿主. 它代替 `androidx.compose.ui:ui-test-manifest` 中的 Activity: 那个 Activity 是导出的, 会往 debug APK
 * 加入一个导出的 `androidx.activity.ComponentActivity`; 这个不导出, 主题相同. 仅 debug 构建.
 */
class ComposeTestActivity : ComponentActivity()

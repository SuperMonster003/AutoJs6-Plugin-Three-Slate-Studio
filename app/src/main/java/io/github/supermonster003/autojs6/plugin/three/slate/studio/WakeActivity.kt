package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.app.Activity
import android.os.Bundle

/**
 * Activation entry of the plugin protocol (new repository specification 6): the host starts it to bring the process up,
 * and it finishes at once without any user interface.
 * zh-CN: 插件协议的激活入口 (新仓库规范第 6 节): 宿主启动它以拉起进程, 它立即结束, 不显示任何界面.
 */
class WakeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }

}

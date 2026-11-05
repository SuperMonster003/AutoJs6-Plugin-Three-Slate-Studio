package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginInfo

/**
 * Answers `org.autojs.plugin.INFO` (category `three-slate-studio`) so the Plugin Center lists the identity of this
 * package before and independently of the capability service.
 * zh-CN: 响应 `org.autojs.plugin.INFO` (category `three-slate-studio`), 使插件中心独立于能力服务显示本包的身份.
 */
class ThreeSlateStudioInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo(): PluginInfo = pluginRuntimeFields().toPluginInfo()
    }

    override fun onBind(intent: Intent?): IBinder = binder

}

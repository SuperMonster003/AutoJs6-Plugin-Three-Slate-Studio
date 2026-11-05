package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.MediaStudioEngine.Companion.withContractVersion
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mediastudio.api.IMediaStudioCallback
import org.autojs.plugin.mediastudio.api.IMediaStudioPlugin
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes

/**
 * Answers `org.autojs.plugin.MEDIA_STUDIO` (category `media-studio`), guarded by `org.autojs.permission.PLUGIN`, and
 * delegates to the process-wide [MediaStudioEngine] (roadmap P2.1): submit / cancel / probe / listTasks with the live
 * capability bundle.
 * zh-CN: 响应 `org.autojs.plugin.MEDIA_STUDIO` (category `media-studio`), 受 `org.autojs.permission.PLUGIN` 保护, 委托给进程级
 * 的 [MediaStudioEngine] (路线图 P2.1): submit / cancel / probe / listTasks 与实时能力 Bundle.
 */
class ThreeSlateStudioService : Service() {

    private val engine by lazy { MediaStudioEngine.get(this) }

    private val binder = object : IMediaStudioPlugin.Stub() {

        override fun getInfo(): PluginInfo = pluginRuntimeFields().toPluginInfo()

        override fun getCapabilities(): Bundle = pluginRuntimeFields().liveCapabilities(this@ThreeSlateStudioService).withContractVersion()

        override fun submit(request: Bundle?, callback: IMediaStudioCallback?): Int {
            requireNotNull(request) { MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "request is required") }
            requireNotNull(callback) { MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "callback is required") }
            return engine.submit(request, callback, Binder.getCallingUid())
        }

        override fun cancel(taskId: Int): Boolean = engine.cancel(taskId)

        override fun probe(request: Bundle?): Bundle = engine.probe(request)

        override fun listTasks(query: Bundle?): Bundle = engine.listTasks(query, Binder.getCallingUid())

    }

    override fun onBind(intent: Intent?): IBinder = binder

}

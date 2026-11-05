package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.RequestValidation
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mediastudio.api.IMediaStudioCallback
import org.autojs.plugin.mediastudio.api.IMediaStudioPlugin
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys

/**
 * Answers `org.autojs.plugin.MEDIA_STUDIO` (category `media-studio`), guarded by `org.autojs.permission.PLUGIN`. The
 * skeleton reports its identity and capabilities and rejects every operation with `INVALID_OPTIONS: NOT_IMPLEMENTED`
 * until roadmap P2.1 lands the task runner.
 * zh-CN: 响应 `org.autojs.plugin.MEDIA_STUDIO` (category `media-studio`), 受 `org.autojs.permission.PLUGIN` 保护. 骨架只返回
 * 身份与能力, 在路线图 P2.1 落地任务执行器之前, 所有操作都以 `INVALID_OPTIONS: NOT_IMPLEMENTED` 拒绝.
 */
class ThreeSlateStudioService : Service() {

    private val binder = object : IMediaStudioPlugin.Stub() {

        override fun getInfo(): PluginInfo = pluginRuntimeFields().toPluginInfo()

        override fun getCapabilities(): Bundle = pluginRuntimeFields().capabilities().withContractVersion()

        override fun submit(request: Bundle?, callback: IMediaStudioCallback?): Int {
            requireNotNull(request) { MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "request is required") }
            requireNotNull(callback) { MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "callback is required") }
            val rejection = RequestValidation.rejection(
                op = request.getString(MediaStudioRequestKeys.OP),
                inputs = request.getStringArray(MediaStudioRequestKeys.INPUTS)?.toList().orEmpty(),
                hostToken = request.getString(MediaStudioRequestKeys.HOST_TOKEN),
            ) ?: RequestValidation.notImplemented(request.getString(MediaStudioRequestKeys.OP).orEmpty())
            // Known but missing operations are a state of this build, malformed envelopes an argument error.
            // zh-CN: 已知但未实现的操作属于本构建的状态问题, 外层字段错误属于参数错误.
            if (rejection.contains(ThreeSlateStudioContract.NOT_IMPLEMENTED)) throw IllegalStateException(rejection)
            throw IllegalArgumentException(rejection)
        }

        override fun cancel(taskId: Int): Boolean = false

        override fun probe(request: Bundle?): Bundle =
            errorBundle(MediaStudioErrorCodes.INVALID_OPTIONS, "${ThreeSlateStudioContract.NOT_IMPLEMENTED}: ${MediaStudioOps.PROBE}")

        override fun listTasks(query: Bundle?): Bundle = Bundle().apply {
            putParcelableArray(MediaStudioResultKeys.TASKS, emptyArray<Bundle>())
        }.withContractVersion()

    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun errorBundle(code: String, message: String): Bundle = Bundle().apply {
        putString(MediaStudioContract.KEY_ERROR_CODE, code)
        putString(MediaStudioContract.KEY_ERROR_MESSAGE, message)
    }.withContractVersion()

    private fun Bundle.withContractVersion(): Bundle = apply {
        putInt(MediaStudioContract.KEY_CONTRACT_VERSION, ThreeSlateStudioContract.CONTRACT_VERSION)
    }

}

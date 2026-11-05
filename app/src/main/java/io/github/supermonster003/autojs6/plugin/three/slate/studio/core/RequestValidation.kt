package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits
import org.autojs.plugin.mediastudio.api.MediaStudioOps

/**
 * First checks of a submit request, shared by the Binder service and the Studio screens. Roadmap P2.1 extends it with
 * the per operation option schemas; the skeleton only checks the envelope and then reports `NOT_IMPLEMENTED`.
 * zh-CN: submit 请求的首轮检查, 由 Binder 服务与 Studio 界面共用. 路线图 P2.1 再按操作扩展选项模式; 骨架只检查外层字段,
 * 然后报告 `NOT_IMPLEMENTED`.
 */
internal object RequestValidation {

    /**
     * Returns null when the request may run, otherwise the rejection message encoded with
     * [MediaStudioErrorCodes.encode] ("CODE: detail").
     * zh-CN: 请求可以执行时返回 null, 否则返回经 [MediaStudioErrorCodes.encode] 编码的拒绝消息 ("CODE: detail").
     */
    fun rejection(op: String?, inputs: List<String?>, hostToken: String?): String? {
        if (op.isNullOrBlank()) {
            return MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "op is required")
        }
        if (!MediaStudioOps.isKnown(op)) {
            return MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "unknown op $op")
        }
        if (inputs.size > MediaStudioLimits.MAX_INPUTS) {
            return MediaStudioErrorCodes.encode(
                MediaStudioErrorCodes.LIMIT_EXCEEDED,
                "${inputs.size} inputs, at most ${MediaStudioLimits.MAX_INPUTS}",
            )
        }
        if (inputs.any { it.isNullOrBlank() }) {
            return MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "inputs must be non-empty paths")
        }
        if (hostToken != null && hostToken.length > MediaStudioLimits.MAX_HOST_TOKEN_CHARS) {
            return MediaStudioErrorCodes.encode(
                MediaStudioErrorCodes.INVALID_OPTIONS,
                "hostToken longer than ${MediaStudioLimits.MAX_HOST_TOKEN_CHARS} characters",
            )
        }
        if (op !in ThreeSlateStudioContract.IMPLEMENTED_OPS) {
            return notImplemented(op)
        }
        return null
    }

    /**
     * The contract has no code of its own for missing operations, so they are `INVALID_OPTIONS` with a
     * `NOT_IMPLEMENTED` detail (host contract P1.1).
     * zh-CN: 契约没有专用于未实现操作的错误码, 因此归为 `INVALID_OPTIONS`, 详情以 `NOT_IMPLEMENTED` 开头 (宿主契约 P1.1).
     */
    fun notImplemented(op: String): String = MediaStudioErrorCodes.encode(
        MediaStudioErrorCodes.INVALID_OPTIONS,
        "${ThreeSlateStudioContract.NOT_IMPLEMENTED}: $op",
    )

}

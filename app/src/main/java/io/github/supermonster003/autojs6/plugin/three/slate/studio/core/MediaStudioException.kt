package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioLimits

/**
 * A failure with a contract error code (roadmap appendix C.3). The message is `CODE: detail`
 * ([MediaStudioErrorCodes.encode]), capped at [MediaStudioLimits.MAX_ERROR_MESSAGE_CHARS] characters.
 * zh-CN: 带契约错误码的失败 (路线图附录 C.3). 消息为 `CODE: detail` ([MediaStudioErrorCodes.encode]), 最长
 * [MediaStudioLimits.MAX_ERROR_MESSAGE_CHARS] 个字符.
 */
internal class MediaStudioException(
    val code: String,
    val detail: String,
    cause: Throwable? = null,
) : Exception(encodeMessage(code, detail), cause) {

    init {
        require(code in MediaStudioErrorCodes.ALL) { "unknown error code $code" }
    }

    companion object {

        fun encodeMessage(code: String, detail: String): String =
            MediaStudioErrorCodes.encode(code, detail).take(MediaStudioLimits.MAX_ERROR_MESSAGE_CHARS)

        fun invalid(detail: String) = MediaStudioException(MediaStudioErrorCodes.INVALID_OPTIONS, detail)

        fun unsupported(detail: String) = MediaStudioException(MediaStudioErrorCodes.UNSUPPORTED_FORMAT, detail)

        fun io(detail: String, cause: Throwable? = null) = MediaStudioException(MediaStudioErrorCodes.IO_ERROR, detail, cause)

    }

}

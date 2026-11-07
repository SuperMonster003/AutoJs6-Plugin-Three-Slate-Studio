package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

/**
 * The codec budget of `compose` (appendix D Q37 (a), the maintainer's choice of 2026-11-07): the 8 / 16 track limits of
 * Q34 (c) stay, and an export that fails because the device cannot start another encoder or decoder answers an
 * actionable message instead of the raw media3 codec dump. On three Sony devices (API 28 / 31 / 33, appendix F) 6 video
 * tracks with overlapping transitions plus 16 audio tracks passed and 8 + 16 failed with
 * `ERROR_CODE_ENCODER_INIT_FAILED` / `ERROR_CODE_DECODER_INIT_FAILED` (`InsufficientResources`).
 * zh-CN: `compose` 的编解码器预算 (附录 D Q37 (a), 维护者 2026-11-07 选): 保持 Q34 (c) 的 8 / 16 轨上限, 导出因设备无法再启动编码器或
 * 解码器而失败时, 返回可操作的提示而非 media3 的原始编解码器信息. 三台 Sony (API 28 / 31 / 33, 附录 F) 上 6 条带重叠转场的视频轨加
 * 16 条音频轨通过, 8 + 16 以 `ERROR_CODE_ENCODER_INIT_FAILED` / `ERROR_CODE_DECODER_INIT_FAILED` (`InsufficientResources`) 失败.
 */
internal object CodecResources {

    const val ENCODER_INIT_FAILED = "ERROR_CODE_ENCODER_INIT_FAILED"
    const val DECODER_INIT_FAILED = "ERROR_CODE_DECODER_INIT_FAILED"

    /** `MediaCodec.CodecException.ERROR_INSUFFICIENT_RESOURCE` / `ERROR_RECLAIMED`. zh-CN: 资源不足 / 被系统回收. */
    val RESOURCE_CODEC_ERRORS = setOf(1100, 1101)

    /** Words of a codec failure message that point at exhausted resources. zh-CN: 指向资源耗尽的编解码器失败消息用词. */
    private val RESOURCE_WORDS = listOf("insufficientresource", "insufficient resource", "no_memory", "no memory", "resource")

    /**
     * The largest count measured to pass (6 video tracks with overlapping transitions x 2 + 16 audio tracks); an init
     * failure above it is taken as exhausted resources even when the codec says nothing more.
     * zh-CN: 实测通过的最大路数 (6 条带重叠转场的视频轨 x 2 + 16 条音频轨); 超过它的初始化失败即使编解码器没有更多说明也按资源耗尽处理.
     */
    const val MEASURED_PASSING_SOURCES = 28

    /** The tracks behind [count]. zh-CN: [count] 的轨道构成. */
    data class Budget(val overlappingVideoTracks: Int, val otherVideoTracks: Int, val audioTracks: Int) {
        /** 2 per video track with an overlapping transition, 1 per other video track, 1 per audio track. zh-CN: 带重叠转场的视频轨计 2, 其他视频轨计 1, 音频轨各计 1. */
        val count: Int get() = overlappingVideoTracks * 2 + otherVideoTracks + audioTracks
    }

    /** The budget of [timeline]. zh-CN: [timeline] 的预算. */
    fun budgetOf(timeline: TimelineSpec): Budget {
        val overlapping = timeline.videoTracks.count { clips -> clips.any { it.transition?.type?.overlaps == true } }
        return Budget(overlapping, timeline.videoTracks.size - overlapping, timeline.audioTracks.size)
    }

    /**
     * Whether a failed export of [errorCodeName] ran out of codec resources: an encoder or decoder could not start and
     * the cause says so ([codecErrorCodes], [causeTexts]) or [budget] is above [MEASURED_PASSING_SOURCES].
     * zh-CN: [errorCodeName] 的导出失败是否因编解码器资源耗尽: 编码器或解码器无法启动, 且原因说明了这一点 ([codecErrorCodes], [causeTexts])
     * 或 [budget] 超过 [MEASURED_PASSING_SOURCES].
     */
    fun isExhausted(errorCodeName: String, codecErrorCodes: List<Int>, causeTexts: List<String>, budget: Budget): Boolean {
        if (errorCodeName != ENCODER_INIT_FAILED && errorCodeName != DECODER_INIT_FAILED) return false
        return resourceSign(codecErrorCodes, causeTexts) || budget.count > MEASURED_PASSING_SOURCES
    }

    fun resourceSign(codecErrorCodes: List<Int>, causeTexts: List<String>): Boolean =
        codecErrorCodes.any { it in RESOURCE_CODEC_ERRORS } || causeTexts.any { text -> RESOURCE_WORDS.any { text.contains(it, ignoreCase = true) } }

    /**
     * The CODEC_ERROR detail of an exhausted export, for example `the device could not start another video encoder
     * (ERROR_CODE_ENCODER_INIT_FAILED, InsufficientResources): this timeline decodes about 32 sources at once (8 video
     * tracks with overlapping transitions x 2 + 0 other video tracks + 16 audio tracks); use fewer video tracks or fewer
     * overlapping transitions`.
     * zh-CN: 资源耗尽的导出的 CODEC_ERROR 详情, 示例见上.
     */
    fun message(errorCodeName: String, insufficient: Boolean, budget: Budget): String {
        val side = if (errorCodeName == DECODER_INIT_FAILED) "decoder" else "encoder"
        val sign = if (insufficient) ", InsufficientResources" else ""
        return "the device could not start another video $side ($errorCodeName$sign): this timeline decodes about ${budget.count} sources at once " +
            "(${budget.overlappingVideoTracks} video tracks with overlapping transitions x 2 + ${budget.otherVideoTracks} other video tracks + " +
            "${budget.audioTracks} audio tracks); use fewer video tracks or fewer overlapping transitions"
    }
}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.FieldType
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionField
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.ScriptCalls
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.BitRates
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StudioPreferences.BitRatePolicy
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioRequest
import kotlin.math.roundToLong

/**
 * The bit rate policy of the Studio settings (D37) for wizard requests that leave the video bit rate empty: the plugin's
 * estimate of roadmap D29 (appendix D Q12) for the output size, times the policy factor, written into the request so the
 * equivalent script call shows the value that ran. The automatic policy leaves the request as it is.
 * zh-CN: Studio 设置 (D37) 的码率策略, 用于视频码率留空的向导请求: 按输出尺寸取路线图 D29 (附录 D Q12) 的插件估算值, 乘以策略系数,
 * 写进请求, 使等价脚本调用显示实际运行的值. 自动策略不改动请求.
 */
internal object BitRatePlanner {

    fun apply(request: StudioRequest, policy: BitRatePolicy): StudioRequest {
        if (policy == BitRatePolicy.AUTO) return request
        val schema = request.schema
        val inStyle = (schema.field("style")?.type as? FieldType.Group)?.fields.orEmpty().any(::isVideoBitRate)
        val topLevel = schema.field("bitRate")?.let(::isVideoBitRate) == true
        if (!inStyle && !topLevel) return request
        val scope = if (inStyle) (request.options["style"] as? Map<*, *>).orEmpty() else request.options
        if (scope["bitRate"] != null) return request
        val info = request.inputs.firstOrNull()?.let { runCatching { MediaProbe.probe(it) }.getOrNull() } ?: return request
        val sourceWidth = info.width ?: return request
        val sourceHeight = info.height ?: return request
        val (width, height) = targetSize(request.options, sourceWidth, sourceHeight)
        val codec = VideoCodec.entries.firstOrNull { it.id == scope["codec"] } ?: VideoCodec.H264
        val frameRate = (request.options["frameRate"] as? Number)?.toDouble() ?: info.frameRate
        val estimate = BitRates.estimateVideo(width, height, frameRate, codec)
        val bitRate = (estimate * policy.factor).roundToLong()
            .coerceIn(OperationOptions.MIN_VIDEO_BIT_RATE.toLong(), OperationOptions.MAX_VIDEO_BIT_RATE.toLong())
        val options = if (inStyle) request.options + ("style" to (scope.entries.associate { (k, v) -> k.toString() to v } + ("bitRate" to bitRate)))
        else request.options + ("bitRate" to bitRate)
        return request.copy(options = options, code = ScriptCalls.code(schema, request.inputs, options, request.output))
    }

    /** A video bit rate field (audio bit rates have other bounds). zh-CN: 视频码率字段 (音频码率的范围不同). */
    private fun isVideoBitRate(field: OptionField): Boolean =
        field.key == "bitRate" && (field.type as? FieldType.Integer)?.min == OperationOptions.MIN_VIDEO_BIT_RATE.toLong()

    /** The output size the options ask for, else the source size. zh-CN: 选项要求的输出尺寸, 否则为源尺寸. */
    private fun targetSize(options: Map<String, Any?>, sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        val crop = options["crop"] as? Map<*, *>
        var width = (crop?.get("width") as? Number)?.toInt() ?: sourceWidth
        var height = (crop?.get("height") as? Number)?.toInt() ?: sourceHeight
        val requestedWidth = (options["width"] as? Number)?.toInt()
        val requestedHeight = (options["height"] as? Number)?.toInt()
        val scale = (options["scale"] as? Number)?.toDouble()
        when {
            requestedWidth != null && requestedHeight != null -> { width = requestedWidth; height = requestedHeight }
            requestedWidth != null -> { height = (height.toDouble() * requestedWidth / width).toInt(); width = requestedWidth }
            requestedHeight != null -> { width = (width.toDouble() * requestedHeight / height).toInt(); height = requestedHeight }
            scale != null -> { width = (width * scale).toInt(); height = (height * scale).toInt() }
        }
        return width.coerceAtLeast(1) to height.coerceAtLeast(1)
    }
}

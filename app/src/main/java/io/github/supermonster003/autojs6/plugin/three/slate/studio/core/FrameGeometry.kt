package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import kotlin.math.roundToLong

/** A frame size in pixels. zh-CN: 以像素表示的帧尺寸. */
internal data class FrameSize(val width: Int, val height: Int) {
    override fun toString() = "${width}x$height"
}

/**
 * The frame sizes of the video operations (roadmap P2.2): encoders need even sizes, one given side keeps the aspect
 * ratio of the source, and the effects of `transform` run crop, rotate, flip, then resize. Crops are converted to the
 * normalized device coordinates media3 expects. Pure, so the JVM tests freeze it.
 * zh-CN: 视频操作的帧尺寸 (路线图 P2.2): 编码器需要偶数尺寸, 只给一边时按源宽高比计算另一边, `transform` 的效果顺序为裁切, 旋转,
 * 翻转, 缩放. 裁切换算为 media3 使用的归一化设备坐标. 纯逻辑, 由 JVM 测试冻结.
 */
internal object FrameGeometry {

    /** The largest even value not above [value], at least 2. zh-CN: 不大于 [value] 的最大偶数, 至少为 2. */
    fun even(value: Int): Int = (value and 1.inv()).coerceAtLeast(2)

    fun even(size: FrameSize): FrameSize = FrameSize(even(size.width), even(size.height))

    /**
     * The target size of [width] / [height] options against the displayed [source] size: both sides as given, one side
     * with the other from the aspect ratio, neither the source; always even.
     * zh-CN: [width] / [height] 选项相对显示尺寸 [source] 的目标尺寸: 两边都给则照用, 只给一边则按宽高比算另一边, 都不给则用源尺寸;
     * 结果总是偶数.
     */
    fun target(width: Int?, height: Int?, source: FrameSize): FrameSize = when {
        width != null && height != null -> FrameSize(even(width), even(height))
        width != null -> FrameSize(even(width), even(scaled(source.height, width.toDouble() / source.width)))
        height != null -> FrameSize(even(scaled(source.width, height.toDouble() / source.height)), even(height))
        else -> even(source)
    }

    /** [size] times [scale], even. zh-CN: [size] 乘以 [scale], 取偶数. */
    fun scale(size: FrameSize, scale: Double): FrameSize =
        FrameSize(even(scaled(size.width, scale)), even(scaled(size.height, scale)))

    /** The size after a clockwise rotation of 0 / 90 / 180 / 270 degrees. zh-CN: 顺时针旋转 0 / 90 / 180 / 270 度后的尺寸. */
    fun rotate(size: FrameSize, degrees: Int): FrameSize =
        if (degrees == 90 || degrees == 270) FrameSize(size.height, size.width) else size

    /**
     * Null when [crop] lies inside a frame of [frame], otherwise the INVALID_OPTIONS detail.
     * zh-CN: [crop] 位于 [frame] 帧内时返回 null, 否则返回 INVALID_OPTIONS 的详情.
     */
    fun cropProblem(crop: CropRect, frame: FrameSize): String? {
        val fits = crop.x >= 0 && crop.y >= 0 &&
            crop.x.toLong() + crop.width <= frame.width && crop.y.toLong() + crop.height <= frame.height
        if (fits) return null
        return "options.crop (x ${crop.x}, y ${crop.y}, ${crop.width}x${crop.height}) lies outside the $frame frame"
    }

    /**
     * [crop] (pixels, origin top left) as the (left, right, bottom, top) normalized device coordinates of media3 `Crop`,
     * where x and y run from -1 to 1 and y points up.
     * zh-CN: 把 [crop] (像素, 原点在左上) 换算为 media3 `Crop` 的归一化设备坐标 (left, right, bottom, top), x 与 y 取 -1 到 1,
     * y 轴向上.
     */
    fun cropNdc(crop: CropRect, frame: FrameSize): FloatArray {
        val left = 2.0 * crop.x / frame.width - 1.0
        val right = 2.0 * (crop.x + crop.width) / frame.width - 1.0
        val top = 1.0 - 2.0 * crop.y / frame.height
        val bottom = 1.0 - 2.0 * (crop.y + crop.height) / frame.height
        return floatArrayOf(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat())
    }

    /**
     * Null when [size] is within the option range of roadmap P2.2 (16 - 8192 per side), otherwise the INVALID_OPTIONS
     * detail.
     * zh-CN: [size] 在路线图 P2.2 的选项范围内 (每边 16 - 8192) 时返回 null, 否则返回 INVALID_OPTIONS 的详情.
     */
    fun sizeProblem(size: FrameSize): String? {
        val range = OperationOptions.MIN_DIMENSION..OperationOptions.MAX_DIMENSION
        if (size.width in range && size.height in range) return null
        return "the output size $size is outside ${range.first} - ${range.last} pixels per side"
    }

    private fun scaled(value: Int, factor: Double): Int = (value * factor).roundToLong().coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

}

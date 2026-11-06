package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import java.io.OutputStream

/**
 * The GIF89a writer of `toGif` (roadmap D32, appendix D Q2 (a)), pure Kotlin so JVM tests cover it. Every frame gets its
 * own local color table of at most 256 colors ([GifPalette]): the exact colors when the frame has no more, otherwise a
 * median cut of its 15-bit color histogram, optionally with Floyd-Steinberg dithering. Frames are opaque, full size and
 * never disposed; the image data is LZW coded ([GifLzw]). [loop] is the NETSCAPE2.0 repeat count (0 plays forever) or
 * null for no loop extension, which plays once.
 * zh-CN: `toGif` 的 GIF89a 写入器 (路线图 D32, 附录 D Q2 (a)), 纯 Kotlin, 便于 JVM 测试覆盖. 每帧有自己的局部颜色表,
 * 最多 256 色 ([GifPalette]): 帧内颜色不超过 256 种时取原色, 否则对 15 位颜色直方图做中位切分, 可选 Floyd-Steinberg 抖动.
 * 帧不透明, 铺满画面且不清除; 图像数据用 LZW 编码 ([GifLzw]). [loop] 为 NETSCAPE2.0 重复次数 (0 为无限循环), null 时不写
 * 循环扩展, 只播放一次.
 */
internal class GifEncoder(
    private val out: OutputStream,
    val width: Int,
    val height: Int,
    loop: Int?,
    private val dither: Boolean,
) {

    var frameCount = 0
        private set

    /** Sum of the frame delays in centiseconds. zh-CN: 各帧延时之和, 单位为百分之一秒. */
    var durationCentis = 0L
        private set

    init {
        require(width in 1..MAX_SIZE && height in 1..MAX_SIZE) { "GIF size out of range: ${width}x$height" }
        require(loop == null || loop in 0..MAX_LOOP) { "loop out of range: $loop" }
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(width)
        writeShort(height)
        // No global color table; color resolution 8 bits. zh-CN: 无全局颜色表; 颜色分辨率 8 位.
        out.write(0x70)
        out.write(0)
        out.write(0)
        if (loop != null) {
            out.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
            out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
            out.write(byteArrayOf(0x03, 0x01))
            writeShort(loop)
            out.write(0)
        }
    }

    /**
     * Appends one frame of [width] x [height] pixels (0xAARRGGBB, alpha ignored) shown for [delayCentis].
     * zh-CN: 追加一帧 [width] x [height] 像素 (0xAARRGGBB, 忽略 alpha), 显示 [delayCentis].
     */
    fun addFrame(pixels: IntArray, delayCentis: Int) {
        require(pixels.size == width * height) { "frame has ${pixels.size} pixels, expected ${width * height}" }
        require(delayCentis in 0..MAX_DELAY) { "delay out of range: $delayCentis" }
        val quantized = GifPalette.quantize(pixels, width, dither)
        val bits = GifPalette.bitsFor(quantized.palette.size)
        // Graphic control extension: disposal "do not dispose", no transparency.
        // zh-CN: 图形控制扩展: 处置方式 "不清除", 无透明色.
        out.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, 0x04))
        writeShort(delayCentis)
        out.write(0)
        out.write(0)
        // Image descriptor with a local color table of 2^bits entries. zh-CN: 图像描述符, 带 2^bits 项的局部颜色表.
        out.write(0x2C)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        out.write(0x80 or (bits - 1))
        val table = ByteArray(3 shl bits)
        quantized.palette.forEachIndexed { index, rgb ->
            table[index * 3] = (rgb shr 16).toByte()
            table[index * 3 + 1] = (rgb shr 8).toByte()
            table[index * 3 + 2] = rgb.toByte()
        }
        out.write(table)
        GifLzw.encode(quantized.indices, maxOf(2, bits), out)
        frameCount++
        durationCentis += delayCentis
    }

    /** Writes the trailer; the stream stays open. zh-CN: 写入结尾标记; 不关闭流. */
    fun finish() {
        out.write(0x3B)
        out.flush()
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    companion object {
        const val MAX_SIZE = 65_535
        const val MAX_LOOP = 65_535
        const val MAX_DELAY = 65_535

        /**
         * Delays in centiseconds for [frames] frames at [fps], rounded on the running total so their sum stays at
         * `frames * 100 / fps` (30 fps gives 3, 4, 3, ...).
         * zh-CN: [fps] 下 [frames] 帧的延时 (百分之一秒), 按累计值取整, 使总和保持 `frames * 100 / fps` (30 fps 为 3, 4, 3, ...).
         */
        fun delays(frames: Int, fps: Int): IntArray = IntArray(frames) { index ->
            (Math.round((index + 1) * 100.0 / fps) - Math.round(index * 100.0 / fps)).toInt()
        }
    }

}

/** A frame reduced to palette indices. zh-CN: 归约为调色板索引的一帧. */
internal class QuantizedFrame(val palette: IntArray, val indices: ByteArray)

/**
 * Palette selection for [GifEncoder]: exact colors when a frame has at most 256, otherwise median cut over the 15-bit
 * (5 bits per channel) histogram, each box averaged from the real colors it holds. Pixels map to the nearest entry by
 * squared RGB distance through a per-bucket cache; with dithering the quantization error spreads Floyd-Steinberg style.
 * zh-CN: [GifEncoder] 的调色板选择: 帧内颜色不超过 256 种时用原色, 否则对 15 位 (每通道 5 位) 直方图做中位切分, 每个盒子取其中
 * 真实颜色的平均值. 像素经按桶缓存的查找映射到 RGB 平方距离最近的项; 开启抖动时量化误差按 Floyd-Steinberg 方式扩散.
 */
internal object GifPalette {

    const val MAX_COLORS = 256
    private const val BUCKETS = 1 shl 15

    /** Color table bits (1..8) for [colors] entries. zh-CN: [colors] 项所需的颜色表位数 (1..8). */
    fun bitsFor(colors: Int): Int {
        var bits = 1
        while ((1 shl bits) < colors) bits++
        return bits
    }

    fun quantize(pixels: IntArray, width: Int, dither: Boolean): QuantizedFrame {
        exact(pixels)?.let { return it }
        val palette = medianCut(pixels, MAX_COLORS)
        val lookup = NearestColor(palette)
        val indices = ByteArray(pixels.size)
        if (!dither) {
            for (i in pixels.indices) indices[i] = lookup.index(pixels[i]).toByte()
            return QuantizedFrame(palette, indices)
        }
        val height = pixels.size / width
        // Errors of the current and the next row, three channels, in 1/16 units. zh-CN: 当前行与下一行三个通道的误差, 单位 1/16.
        var current = IntArray((width + 2) * 3)
        var next = IntArray((width + 2) * 3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val c = pixels[i]
                val e = (x + 1) * 3
                val r = clamp(((c shr 16) and 0xFF) + current[e] / 16)
                val g = clamp(((c shr 8) and 0xFF) + current[e + 1] / 16)
                val b = clamp((c and 0xFF) + current[e + 2] / 16)
                val index = lookup.index((r shl 16) or (g shl 8) or b)
                indices[i] = index.toByte()
                val p = palette[index]
                val dr = r - ((p shr 16) and 0xFF)
                val dg = g - ((p shr 8) and 0xFF)
                val db = b - (p and 0xFF)
                spread(current, e + 3, dr, dg, db, 7)
                spread(next, e - 3, dr, dg, db, 3)
                spread(next, e, dr, dg, db, 5)
                spread(next, e + 3, dr, dg, db, 1)
            }
            val done = current
            current = next
            next = done.also { it.fill(0) }
        }
        return QuantizedFrame(palette, indices)
    }

    private fun spread(row: IntArray, at: Int, dr: Int, dg: Int, db: Int, weight: Int) {
        row[at] += dr * weight
        row[at + 1] += dg * weight
        row[at + 2] += db * weight
    }

    private fun clamp(value: Int) = if (value < 0) 0 else if (value > 255) 255 else value

    /** The frame with its own colors, or null when it has more than 256. zh-CN: 用帧自身颜色的结果, 超过 256 种时为 null. */
    private fun exact(pixels: IntArray): QuantizedFrame? {
        val table = IntIntTable(1024)
        val palette = IntArray(MAX_COLORS)
        var count = 0
        val indices = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val rgb = pixels[i] and 0xFFFFFF
            var index = table.get(rgb)
            if (index < 0) {
                if (count == MAX_COLORS) return null
                index = count++
                palette[index] = rgb
                table.put(rgb, index)
            }
            indices[i] = index.toByte()
        }
        return QuantizedFrame(palette.copyOf(count), indices)
    }

    /** At most [maxColors] entries (0xRRGGBB) for [pixels] by median cut. zh-CN: 中位切分得到 [pixels] 的至多 [maxColors] 项 (0xRRGGBB). */
    fun medianCut(pixels: IntArray, maxColors: Int): IntArray {
        val counts = LongArray(BUCKETS)
        val sums = LongArray(BUCKETS * 3)
        for (c in pixels) {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val bucket = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
            counts[bucket]++
            sums[bucket * 3] += r.toLong()
            sums[bucket * 3 + 1] += g.toLong()
            sums[bucket * 3 + 2] += b.toLong()
        }
        val used = (0 until BUCKETS).filter { counts[it] > 0 }.toIntArray()
        val boxes = mutableListOf(Box(used, 0, used.size, counts))
        while (boxes.size < maxColors) {
            val box = boxes.filter { it.splittable }.maxByOrNull { it.score } ?: break
            boxes.remove(box)
            boxes += box.split(counts)
        }
        return IntArray(boxes.size) { index -> boxes[index].average(counts, sums) }
    }

    /** Buckets `used[start, end)`. zh-CN: 桶 `used[start, end)`. */
    private class Box(val used: IntArray, val start: Int, val end: Int, counts: LongArray) {

        private val min = IntArray(3) { Int.MAX_VALUE }
        private val max = IntArray(3) { Int.MIN_VALUE }
        val population: Long
        val axis: Int
        val range: Int

        init {
            var total = 0L
            for (i in start until end) {
                val bucket = used[i]
                for (channel in 0..2) {
                    val v = channelOf(bucket, channel)
                    if (v < min[channel]) min[channel] = v
                    if (v > max[channel]) max[channel] = v
                }
                total += counts[bucket]
            }
            population = total
            val ranges = IntArray(3) { max[it] - min[it] }
            axis = (0..2).maxBy { ranges[it] }
            range = ranges[axis]
        }

        val splittable get() = end - start > 1
        val score get() = population * (range + 1)

        fun split(counts: LongArray): List<Box> {
            val sorted = (start until end).map { used[it] }.sortedBy { channelOf(it, axis) }
            for (i in sorted.indices) used[start + i] = sorted[i]
            val half = population / 2
            var running = 0L
            var cut = start + 1
            for (i in start until end - 1) {
                running += counts[used[i]]
                cut = i + 1
                if (running >= half) break
            }
            return listOf(Box(used, start, cut, counts), Box(used, cut, end, counts))
        }

        fun average(counts: LongArray, sums: LongArray): Int {
            var total = 0L
            var r = 0L
            var g = 0L
            var b = 0L
            for (i in start until end) {
                val bucket = used[i]
                total += counts[bucket]
                r += sums[bucket * 3]
                g += sums[bucket * 3 + 1]
                b += sums[bucket * 3 + 2]
            }
            return (((r + total / 2) / total).toInt() shl 16) or (((g + total / 2) / total).toInt() shl 8) or ((b + total / 2) / total).toInt()
        }

        private fun channelOf(bucket: Int, channel: Int) = (bucket shr (10 - channel * 5)) and 0x1F
    }

    /** Nearest palette entry per 15-bit bucket, computed on first use. zh-CN: 每个 15 位桶的最近调色板项, 首次使用时计算. */
    private class NearestColor(private val palette: IntArray) {
        private val cache = IntArray(BUCKETS) { -1 }

        fun index(rgb: Int): Int {
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            val bucket = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
            val cached = cache[bucket]
            if (cached >= 0) return cached
            // The bucket center stands for every color in it. zh-CN: 以桶中心代表桶内全部颜色.
            val cr = ((r shr 3) shl 3) or 4
            val cg = ((g shr 3) shl 3) or 4
            val cb = ((b shr 3) shl 3) or 4
            var best = 0
            var bestDistance = Int.MAX_VALUE
            for (i in palette.indices) {
                val p = palette[i]
                val dr = cr - ((p shr 16) and 0xFF)
                val dg = cg - ((p shr 8) and 0xFF)
                val db = cb - (p and 0xFF)
                val distance = dr * dr + dg * dg + db * db
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = i
                }
            }
            cache[bucket] = best
            return best
        }
    }

}

/**
 * GIF LZW coding (variable code width up to 12 bits, a clear code when the table is full, 255-byte sub-blocks).
 * zh-CN: GIF 的 LZW 编码 (变长码宽至多 12 位, 码表满时写清除码, 255 字节子块).
 */
internal object GifLzw {

    private const val MAX_CODES = 4096

    /** Writes the minimum code size byte, the sub-blocks and the block terminator. zh-CN: 写入最小码宽字节, 子块与块结束符. */
    fun encode(indices: ByteArray, minCodeSize: Int, out: OutputStream) {
        require(minCodeSize in 2..8) { "minimum code size out of range: $minCodeSize" }
        out.write(minCodeSize)
        val blocks = SubBlockWriter(out)
        val clear = 1 shl minCodeSize
        val end = clear + 1
        var next = end + 1
        var codeSize = minCodeSize + 1
        val table = IntIntTable(MAX_CODES * 2)
        var bits = 0L
        var bitCount = 0

        fun emit(code: Int) {
            bits = bits or (code.toLong() shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                blocks.write((bits and 0xFF).toInt())
                bits = bits ushr 8
                bitCount -= 8
            }
        }

        emit(clear)
        if (indices.isNotEmpty()) {
            val mask = clear - 1
            var prefix = indices[0].toInt() and mask
            for (i in 1 until indices.size) {
                val k = indices[i].toInt() and mask
                val key = (prefix shl 8) or k
                val code = table.get(key)
                if (code >= 0) {
                    prefix = code
                    continue
                }
                emit(prefix)
                if (next == MAX_CODES) {
                    emit(clear)
                    next = end + 1
                    codeSize = minCodeSize + 1
                    table.clear()
                } else {
                    if (next >= (1 shl codeSize)) codeSize++
                    table.put(key, next++)
                }
                prefix = k
            }
            emit(prefix)
        }
        emit(end)
        if (bitCount > 0) blocks.write((bits and 0xFF).toInt())
        blocks.finish()
    }

    private class SubBlockWriter(private val out: OutputStream) {
        private val buffer = ByteArray(255)
        private var size = 0

        fun write(byte: Int) {
            buffer[size++] = byte.toByte()
            if (size == buffer.size) flush()
        }

        private fun flush() {
            if (size == 0) return
            out.write(size)
            out.write(buffer, 0, size)
            size = 0
        }

        fun finish() {
            flush()
            out.write(0)
        }
    }

}

/** Open addressing map of non-negative int keys to non-negative values (-1 = absent). zh-CN: 非负整数键到非负值的开放寻址表 (-1 表示不存在). */
internal class IntIntTable(capacity: Int) {

    private val size = Integer.highestOneBit(maxOf(16, capacity - 1)) shl 1
    private val mask = size - 1
    private val keys = IntArray(size) { -1 }
    private val values = IntArray(size)

    fun get(key: Int): Int {
        var slot = hash(key)
        while (true) {
            val k = keys[slot]
            if (k == key) return values[slot]
            if (k < 0) return -1
            slot = (slot + 1) and mask
        }
    }

    /** The caller keeps the load below one half. zh-CN: 调用方保证装载率低于一半. */
    fun put(key: Int, value: Int) {
        var slot = hash(key)
        while (keys[slot] >= 0 && keys[slot] != key) slot = (slot + 1) and mask
        keys[slot] = key
        values[slot] = value
    }

    fun clear() = keys.fill(-1)

    private fun hash(key: Int) = (key * -0x61c88647) ushr 16 and mask

}

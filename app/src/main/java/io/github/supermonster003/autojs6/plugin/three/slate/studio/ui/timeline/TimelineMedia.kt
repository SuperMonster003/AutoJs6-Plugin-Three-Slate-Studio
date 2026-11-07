package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Thumbnails of video clips and waveforms of audio, made off the main thread for the timeline tracks (roadmap P4.3).
 * Thumbnails are frames at whole steps of [THUMB_STEP_MS] (so zooming reuses them); waveforms are peak levels per
 * [PEAK_MS] of the first [MAX_WAVEFORM_MS] of a file. [version] changes when something new is ready, which redraws the
 * tracks.
 * zh-CN: 视频片段的缩略图与音频的波形, 在后台线程生成, 供时间线轨道使用 (路线图 P4.3). 缩略图取 [THUMB_STEP_MS] 整数倍处的帧 (缩放时可复用);
 * 波形为文件前 [MAX_WAVEFORM_MS] 内每 [PEAK_MS] 的峰值. 有新结果时 [version] 变化, 轨道随之重绘.
 */
internal class TimelineMedia {

    val version = mutableIntStateOf(0)

    private val main = Handler(Looper.getMainLooper())
    private val thumbs = LruCache<String, ImageBitmap>(MAX_THUMBS)
    private val pendingThumbs = HashSet<String>()
    private val failedThumbs = HashSet<String>()
    private val thumbWorker = Executors.newSingleThreadExecutor()
    private val waveWorker = Executors.newSingleThreadExecutor()
    private val waveforms = HashMap<String, FloatArray>()
    private val pendingWaves = HashSet<String>()
    private var closed = false

    /** The thumbnail of [path] at [timeMs] rounded to the step, or null while it is made. zh-CN: 缩略图, 生成中为 null. */
    fun thumbnail(path: String, timeMs: Long, heightPx: Int): ImageBitmap? {
        val step = (timeMs / THUMB_STEP_MS) * THUMB_STEP_MS
        val key = "$path@$step"
        thumbs.get(key)?.let { return it }
        if (key in pendingThumbs || key in failedThumbs || closed) return null
        pendingThumbs += key
        thumbWorker.execute {
            val bitmap = frameOf(path, step, heightPx)
            main.post {
                pendingThumbs -= key
                if (bitmap == null) failedThumbs += key else thumbs.put(key, bitmap)
                version.intValue++
            }
        }
        return null
    }

    /** Peaks (0 - 1) per [PEAK_MS] of [path], or null while they are made. zh-CN: 峰值, 生成中为 null. */
    fun waveform(path: String): FloatArray? {
        waveforms[path]?.let { return it }
        if (path in pendingWaves || closed) return null
        pendingWaves += path
        waveWorker.execute {
            val peaks = runCatching { peaksOf(path) }.getOrNull() ?: FloatArray(0)
            main.post {
                pendingWaves -= path
                waveforms[path] = peaks
                version.intValue++
            }
        }
        return null
    }

    fun close() {
        closed = true
        thumbWorker.shutdownNow()
        waveWorker.shutdownNow()
    }

    private fun frameOf(path: String, timeMs: Long, heightPx: Int): ImageBitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val us = timeMs * 1_000L
            val frame: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val (dw, dh) = if (rotation % 180 != 0) h to w else w to h
                if (dw > 0 && dh > 0) {
                    val targetH = max(1, heightPx)
                    val targetW = max(1, (dw.toLong() * targetH / dh).toInt())
                    retriever.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetW, targetH)
                } else retriever.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                retriever.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { full ->
                    val targetH = max(1, heightPx)
                    val targetW = max(1, full.width * targetH / max(1, full.height))
                    full.scale(targetW, targetH).also { if (it !== full) full.recycle() }
                }
            }
            frame?.asImageBitmap()
        } finally {
            retriever.release()
        }
    }.getOrNull()

    /** Decodes the first audio track to 16 bit PCM and keeps the peak of each [PEAK_MS]. zh-CN: 解码首条音轨并保存每 [PEAK_MS] 的峰值. */
    private fun peaksOf(path: String): FloatArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(path)
            val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: return FloatArray(0)
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val durationMs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) / 1_000L else MAX_WAVEFORM_MS
            val buckets = ((min(durationMs, MAX_WAVEFORM_MS) + PEAK_MS - 1) / PEAK_MS).toInt().coerceAtLeast(1)
            val peaks = FloatArray(buckets)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var frames = 0L
            while (!closed) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0 || extractor.sampleTime / 1_000L > MAX_WAVEFORM_MS) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val out = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    codec.outputFormat.let {
                        sampleRate = it.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = it.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                } else if (out >= 0) {
                    val buffer = codec.getOutputBuffer(out)
                    if (buffer != null && info.size > 0) {
                        val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                        shorts.position(info.offset / 2)
                        val count = info.size / 2
                        for (i in 0 until count) {
                            val frame = frames + i / channels
                            val bucket = (frame * 1_000L / sampleRate / PEAK_MS).toInt()
                            if (bucket >= buckets) break
                            val level = abs(shorts.get().toInt()) / 32768f
                            if (level > peaks[bucket]) peaks[bucket] = level
                        }
                        frames += count / channels
                    }
                    codec.releaseOutputBuffer(out, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            return peaks
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    companion object {
        const val THUMB_STEP_MS = 1_000L
        const val PEAK_MS = 20L
        const val MAX_WAVEFORM_MS = 30L * 60_000L
        const val MAX_THUMBS = 240
        private const val TIMEOUT_US = 10_000L
    }

}

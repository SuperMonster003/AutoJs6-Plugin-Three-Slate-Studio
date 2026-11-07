// Copy of the AutoJs6 host kernel file app/src/main/java/org/autojs/autojs/core/record/RecordingMuxer.kt at host commit
// 3ef5cec633 (roadmap P1.2; unchanged up to host 063b9f0b8a), kept by 3-Slate Studio roadmap D41 / P4.5; see README.md.
// Plugin change: none besides the package.
package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Serializes the video / audio codec drains into one MPEG-4 file (MP4 or M4A) without a final file-sized remux. The
 * muxer starts once every expected track has published its format; samples drained before that are buffered (2 MiB).
 *
 * zh-CN: 把视频 / 音频编码器的输出串行写入同一个 MPEG-4 文件 (MP4 或 M4A), 不做整文件重封装. 所有预期的音视频轨都给出格式后
 * 才启动复用器, 之前取出的采样先缓存 (2 MiB).
 */
internal class RecordingMuxer(file: File, private val hasVideo: Boolean, private val hasAudio: Boolean) : AutoCloseable {
    private val trackCount = (if (hasVideo) 1 else 0) + (if (hasAudio) 1 else 0)
    private val muxer: MediaMuxer
    private var tracks = 0
    private var started = false
    private var closed = false
    private data class Sample(val track: Int, val bytes: ByteArray, val timeUs: Long, val flags: Int)
    private val pending = ArrayList<Sample>()
    private var pendingBytes = 0
    var videoSamples = 0
        private set
    var audioSamples = 0
        private set

    init {
        require(trackCount > 0) { "A recording needs a video or an audio track." }
        muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    @Synchronized fun addTrack(format: MediaFormat): Int {
        check(!closed && !started) { "Recording track changed after muxer startup." }
        val track = muxer.addTrack(format)
        if (++tracks == trackCount) {
            muxer.start()
            started = true
            pending.forEach { sample ->
                muxer.writeSampleData(sample.track, ByteBuffer.wrap(sample.bytes), MediaCodec.BufferInfo().apply {
                    set(0, sample.bytes.size, sample.timeUs, sample.flags)
                })
            }
            pending.clear()
            pendingBytes = 0
        }
        return track
    }

    @Synchronized fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo, video: Boolean) {
        check(!closed)
        if (info.size == 0 || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
        if (video) videoSamples++ else audioSamples++
        if (started) muxer.writeSampleData(track, data, info)
        else {
            // Only startup samples wait for the other encoder's format. Startup itself is bounded.
            check(pendingBytes + info.size <= 2 * 1024 * 1024) { "Recording encoder did not publish its track format." }
            val bytes = ByteArray(info.size)
            data.duplicate().apply { position(info.offset); limit(info.offset + info.size) }.get(bytes)
            pending.add(Sample(track, bytes, info.presentationTimeUs, info.flags))
            pendingBytes += bytes.size
        }
    }

    @Synchronized fun finish() {
        check(started && (!hasVideo || videoSamples > 0) && (!hasAudio || audioSamples > 0)) { "Recording has no complete audio/video tracks." }
        try { muxer.stop() } finally { closed = true; muxer.release(); pending.clear() }
    }

    @Synchronized override fun close() {
        if (closed) return
        closed = true
        if (started) runCatching { muxer.stop() }
        runCatching { muxer.release() }
        pending.clear()
    }
}

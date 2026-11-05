package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

/**
 * The encoders of this device as [EncoderIds] (roadmap D29, Q7), scanned once per process from [MediaCodecList]. The
 * capability bundle lists them so the host can check `codec` before it submits.
 * zh-CN: 本机的编码器, 以 [EncoderIds] 表示 (路线图 D29, Q7), 每个进程从 [MediaCodecList] 扫描一次. 能力 Bundle 列出它们, 宿主
 * 提交前可据此检查 `codec`.
 */
internal object CodecSupport {

    @Volatile
    private var cached: Set<String>? = null

    /** MediaMuxer writes OGG from Android 10 (API 29). zh-CN: MediaMuxer 从 Android 10 (API 29) 起可写 OGG. */
    val oggMuxer: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun encoders(): Set<String> = cached ?: scan().also { cached = it }

    fun videoEncoders(): List<String> = listOf(EncoderIds.H264, EncoderIds.H265).filter { it in encoders() }

    fun audioEncoders(): List<String> = listOf(EncoderIds.AAC, EncoderIds.FLAC, EncoderIds.OPUS, EncoderIds.PCM).filter { it in encoders() }

    /** `mp4` when an H.264 or H.265 encoder exists. zh-CN: 存在 H.264 或 H.265 编码器时为 `mp4`. */
    fun videoOutputFormats(): List<String> = if (videoEncoders().isEmpty()) emptyList() else listOf(RequestPlanner.VIDEO_EXTENSION)

    /** The `extractAudio` containers this device can write. zh-CN: 本机可写的 `extractAudio` 容器. */
    fun audioOutputFormats(): List<String> = AudioContainer.entries.filter { container ->
        when (container) {
            AudioContainer.M4A -> EncoderIds.AAC in encoders()
            AudioContainer.WAV -> true
            AudioContainer.FLAC -> EncoderIds.FLAC in encoders()
            AudioContainer.OGG -> oggMuxer && EncoderIds.OPUS in encoders()
        }
    }.map { it.id }

    private fun scan(): Set<String> {
        val types = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .filter { it.isEncoder }
            .flatMap { info -> info.supportedTypes.map { it.lowercase() } }
            .toSet()
        return buildSet {
            if (MediaFormat.MIMETYPE_VIDEO_AVC in types) add(EncoderIds.H264)
            if (MediaFormat.MIMETYPE_VIDEO_HEVC in types) add(EncoderIds.H265)
            if (MediaFormat.MIMETYPE_AUDIO_AAC in types) add(EncoderIds.AAC)
            if (MediaFormat.MIMETYPE_AUDIO_FLAC in types) add(EncoderIds.FLAC)
            if (MediaFormat.MIMETYPE_AUDIO_OPUS in types) add(EncoderIds.OPUS)
            // WAV is written by the plugin itself. zh-CN: WAV 由插件自己写出.
            add(EncoderIds.PCM)
        }
    }

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Bundle
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import java.io.File
import kotlin.math.roundToLong

/** One track of a probed file. zh-CN: 探测文件中的一条轨道. */
internal data class MediaTrack(
    val mime: String,
    val codec: String,
    val width: Int?,
    val height: Int?,
    val sampleRate: Int?,
    val channels: Int?,
    val bitRate: Int?,
    val language: String?,
    val frameRate: Double?,
    val durationMillis: Long?,
) {
    val isVideo: Boolean get() = mime.startsWith("video/")
    val isAudio: Boolean get() = mime.startsWith("audio/")
}

/**
 * What `probe` answers (roadmap P2.1): [width] and [height] are the displayed size (rotation applied), the raw coded size
 * stays in [tracks].
 * zh-CN: `probe` 的结果 (路线图 P2.1): [width] 与 [height] 为显示尺寸 (已应用旋转), 编码尺寸保留在 [tracks] 中.
 */
internal data class MediaInfo(
    val path: String,
    val sizeBytes: Long,
    val durationMillis: Long,
    val mime: String?,
    val width: Int?,
    val height: Int?,
    val rotation: Int,
    val frameRate: Double?,
    val bitRate: Long?,
    val videoCodec: String?,
    val audioCodec: String?,
    val sampleRate: Int?,
    val channels: Int?,
    val language: String?,
    val tracks: List<MediaTrack>,
) {
    val hasVideo: Boolean get() = videoCodec != null
    val hasAudio: Boolean get() = audioCodec != null
    val media: MediaWarnings.Media get() = MediaWarnings.Media(sizeBytes, durationMillis)
}

/**
 * Probes a file with [MediaExtractor] (tracks) and [MediaMetadataRetriever] (container mime, bit rate, rotation). The
 * frame rate comes from the track format, otherwise from the timestamps of the first [FRAME_RATE_SAMPLES] samples.
 * zh-CN: 用 [MediaExtractor] (轨道) 与 [MediaMetadataRetriever] (容器 mime, 码率, 旋转) 探测文件. 帧率取自轨道格式, 否则按前
 * [FRAME_RATE_SAMPLES] 个样本的时间戳估算.
 */
internal object MediaProbe {

    private const val FRAME_RATE_SAMPLES = 120

    fun probe(path: String): MediaInfo {
        val file = File(path)
        if (!file.isFile) throw MediaStudioException.io("input not found: $path")
        if (!file.canRead()) throw MediaStudioException.io("input is not readable: $path")
        val extractor = MediaExtractor()
        try {
            try {
                extractor.setDataSource(path)
            } catch (e: Exception) {
                throw MediaStudioException.unsupported("cannot read media from $path (${e.message ?: e.javaClass.simpleName})")
            }
            val extracted = (0 until extractor.trackCount).mapNotNull { index -> track(extractor, index) }
            if (extracted.none { it.isVideo || it.isAudio }) throw MediaStudioException.unsupported("no audio or video track in $path")
            val metadata = Metadata.read(path)
            // The FLAC extractor of the platform decodes the stream itself and reports audio/raw.
            // zh-CN: 平台的 FLAC 提取器自行解码数据流, 报告的是 audio/raw.
            val flac = metadata.mime == MediaFormat.MIMETYPE_AUDIO_FLAC || hasFlacMarker(file)
            val tracks = if (!flac) extracted else extracted.map { track ->
                if (track.mime == MediaFormat.MIMETYPE_AUDIO_RAW) track.copy(mime = MediaFormat.MIMETYPE_AUDIO_FLAC, codec = EncoderIds.FLAC) else track
            }
            val video = tracks.firstOrNull { it.isVideo }
            val audio = tracks.firstOrNull { it.isAudio }
            val rotation = normalizeRotation(metadata.rotation ?: 0)
            val swap = rotation == 90 || rotation == 270
            val durationMillis = tracks.mapNotNull { it.durationMillis }.maxOrNull() ?: metadata.durationMillis ?: 0L
            val sizeBytes = file.length()
            val bitRate = metadata.bitRate ?: if (durationMillis > 0) (sizeBytes * 8_000.0 / durationMillis).roundToLong() else null
            return MediaInfo(
                path = path,
                sizeBytes = sizeBytes,
                durationMillis = durationMillis,
                mime = normalizeMime(metadata.mime, hasVideo = video != null) ?: mimeOfExtension(file.extension, video != null),
                width = if (swap) video?.height else video?.width,
                height = if (swap) video?.width else video?.height,
                rotation = rotation,
                frameRate = video?.frameRate,
                bitRate = bitRate,
                videoCodec = video?.codec,
                audioCodec = audio?.codec,
                sampleRate = audio?.sampleRate,
                channels = audio?.channels,
                language = audio?.language ?: video?.language,
                tracks = tracks,
            )
        } finally {
            extractor.release()
        }
    }

    /** `audioProbe`: the file must carry an audio track. zh-CN: `audioProbe`: 文件必须含音轨. */
    fun probeAudio(path: String): MediaInfo = probe(path).also { info ->
        if (!info.hasAudio) throw MediaStudioException.unsupported("no audio track in $path")
    }

    private fun track(extractor: MediaExtractor, index: Int): MediaTrack? {
        val format = extractor.getTrackFormat(index)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        val video = mime.startsWith("video/")
        val frameRate = if (video) format.number(MediaFormat.KEY_FRAME_RATE)?.takeIf { it > 0 } ?: estimateFrameRate(extractor, index) else null
        return MediaTrack(
            mime = mime,
            codec = codecId(mime),
            width = format.int(MediaFormat.KEY_WIDTH),
            height = format.int(MediaFormat.KEY_HEIGHT),
            sampleRate = format.int(MediaFormat.KEY_SAMPLE_RATE),
            channels = format.int(MediaFormat.KEY_CHANNEL_COUNT),
            bitRate = format.int(MediaFormat.KEY_BIT_RATE),
            language = format.string(MediaFormat.KEY_LANGUAGE)?.takeUnless { it.isBlank() || it == "und" },
            frameRate = frameRate?.let { Math.round(it * 1000.0) / 1000.0 },
            durationMillis = format.long(MediaFormat.KEY_DURATION)?.takeIf { it > 0 }?.let { it / 1000L },
        )
    }

    private fun estimateFrameRate(extractor: MediaExtractor, index: Int): Double? {
        extractor.selectTrack(index)
        try {
            val times = ArrayList<Long>(FRAME_RATE_SAMPLES)
            while (times.size < FRAME_RATE_SAMPLES) {
                val time = extractor.sampleTime
                if (time < 0) break
                times += time
                if (!extractor.advance()) break
            }
            if (times.size < 2) return null
            times.sort()
            val span = times.last() - times.first()
            if (span <= 0) return null
            return (times.size - 1) * 1_000_000.0 / span
        } finally {
            extractor.unselectTrack(index)
        }
    }

    /** Codec ids of the results: the encoder ids of [EncoderIds] where they exist. zh-CN: 结果中的编码 id: 有 [EncoderIds] 的用编码器 id. */
    fun codecId(mime: String): String = when (mime.lowercase()) {
        MediaFormat.MIMETYPE_VIDEO_AVC -> EncoderIds.H264
        MediaFormat.MIMETYPE_VIDEO_HEVC -> EncoderIds.H265
        MediaFormat.MIMETYPE_VIDEO_VP8 -> "vp8"
        MediaFormat.MIMETYPE_VIDEO_VP9 -> "vp9"
        "video/av01" -> "av1"
        MediaFormat.MIMETYPE_VIDEO_MPEG4 -> "mpeg4"
        MediaFormat.MIMETYPE_VIDEO_H263 -> "h263"
        MediaFormat.MIMETYPE_AUDIO_AAC -> EncoderIds.AAC
        MediaFormat.MIMETYPE_AUDIO_FLAC -> EncoderIds.FLAC
        MediaFormat.MIMETYPE_AUDIO_OPUS -> EncoderIds.OPUS
        MediaFormat.MIMETYPE_AUDIO_RAW -> EncoderIds.PCM
        MediaFormat.MIMETYPE_AUDIO_VORBIS -> "vorbis"
        MediaFormat.MIMETYPE_AUDIO_MPEG -> "mp3"
        MediaFormat.MIMETYPE_AUDIO_AMR_NB -> "amr-nb"
        MediaFormat.MIMETYPE_AUDIO_AMR_WB -> "amr-wb"
        MediaFormat.MIMETYPE_AUDIO_AC3 -> "ac3"
        MediaFormat.MIMETYPE_AUDIO_EAC3 -> "eac3"
        else -> mime.substringAfter('/')
    }

    /** A native FLAC stream starts with the marker `fLaC`. zh-CN: 原生 FLAC 流以标记 `fLaC` 开头. */
    private fun hasFlacMarker(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val marker = ByteArray(4)
            input.read(marker) == marker.size && marker.contentEquals(FLAC_MARKER)
        }
    }.getOrDefault(false)

    private val FLAC_MARKER = "fLaC".toByteArray(Charsets.US_ASCII)

    private fun normalizeRotation(degrees: Int): Int = ((degrees % 360) + 360) % 360

    private fun normalizeMime(mime: String?, hasVideo: Boolean): String? = when (mime) {
        null, "" -> null
        "audio/x-wav", "audio/wave" -> "audio/wav"
        "application/ogg" -> if (hasVideo) mime else "audio/ogg"
        else -> mime
    }

    private fun mimeOfExtension(extension: String, hasVideo: Boolean): String? = when (extension.lowercase()) {
        "mp4" -> "video/mp4"
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "ogg", "opus" -> if (hasVideo) "video/ogg" else "audio/ogg"
        "webm" -> if (hasVideo) "video/webm" else "audio/webm"
        "mkv" -> "video/x-matroska"
        "3gp" -> "video/3gpp"
        else -> null
    }

    private class Metadata(val mime: String?, val durationMillis: Long?, val bitRate: Long?, val rotation: Int?) {
        companion object {
            fun read(path: String): Metadata {
                val retriever = MediaMetadataRetriever()
                return try {
                    retriever.setDataSource(path)
                    Metadata(
                        mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                        durationMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                        bitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.takeIf { it > 0 },
                        rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull(),
                    )
                } catch (_: RuntimeException) {
                    Metadata(null, null, null, null)
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }
    }

    private fun MediaFormat.int(key: String): Int? {
        if (!containsKey(key)) return null
        return runCatching { getInteger(key) }.getOrNull() ?: runCatching { getFloat(key).toInt() }.getOrNull()
    }

    private fun MediaFormat.number(key: String): Double? {
        if (!containsKey(key)) return null
        return runCatching { getInteger(key).toDouble() }.getOrNull() ?: runCatching { getFloat(key).toDouble() }.getOrNull()
    }

    private fun MediaFormat.long(key: String): Long? {
        if (!containsKey(key)) return null
        return runCatching { getLong(key) }.getOrNull() ?: runCatching { getInteger(key).toLong() }.getOrNull()
    }

    private fun MediaFormat.string(key: String): String? = if (containsKey(key)) runCatching { getString(key) }.getOrNull() else null

}

/** The probe result Bundle (roadmap appendix C, `MediaStudioResultKeys`). zh-CN: 探测结果 Bundle (路线图附录 C). */
internal fun MediaInfo.toBundle(): Bundle = Bundle().apply {
    putString(MediaStudioResultKeys.PATH, path)
    putLong(MediaStudioResultKeys.DURATION_MILLIS, durationMillis)
    putLong(MediaStudioResultKeys.SIZE_BYTES, sizeBytes)
    mime?.let { putString(MediaStudioResultKeys.MIME, it) }
    putBoolean(MediaStudioResultKeys.HAS_VIDEO, hasVideo)
    putBoolean(MediaStudioResultKeys.HAS_AUDIO, hasAudio)
    (videoCodec ?: audioCodec)?.let { putString(MediaStudioResultKeys.CODEC, it) }
    bitRate?.let { putLong(MediaStudioResultKeys.BIT_RATE, it) }
    if (hasVideo) {
        width?.let { putInt(MediaStudioResultKeys.WIDTH, it) }
        height?.let { putInt(MediaStudioResultKeys.HEIGHT, it) }
        putInt(MediaStudioResultKeys.ROTATION, rotation)
        frameRate?.let { putDouble(MediaStudioResultKeys.FRAME_RATE, it) }
        putString(MediaStudioResultKeys.VIDEO_CODEC, videoCodec)
    }
    if (hasAudio) {
        putString(MediaStudioResultKeys.AUDIO_CODEC, audioCodec)
        sampleRate?.let { putInt(MediaStudioResultKeys.SAMPLE_RATE, it) }
        channels?.let { putInt(MediaStudioResultKeys.CHANNELS, it) }
    }
    language?.let { putString(MediaStudioResultKeys.LANGUAGE, it) }
    putParcelableArray(MediaStudioResultKeys.TRACKS, tracks.map { it.toBundle() }.toTypedArray())
}

private fun MediaTrack.toBundle(): Bundle = Bundle().apply {
    putString(MediaStudioResultKeys.MIME, mime)
    putString(MediaStudioResultKeys.CODEC, codec)
    width?.let { putInt(MediaStudioResultKeys.WIDTH, it) }
    height?.let { putInt(MediaStudioResultKeys.HEIGHT, it) }
    sampleRate?.let { putInt(MediaStudioResultKeys.SAMPLE_RATE, it) }
    channels?.let { putInt(MediaStudioResultKeys.CHANNELS, it) }
    bitRate?.let { putLong(MediaStudioResultKeys.BIT_RATE, it.toLong()) }
    language?.let { putString(MediaStudioResultKeys.LANGUAGE, it) }
    frameRate?.let { putDouble(MediaStudioResultKeys.FRAME_RATE, it) }
    durationMillis?.let { putLong(MediaStudioResultKeys.DURATION_MILLIS, it) }
}

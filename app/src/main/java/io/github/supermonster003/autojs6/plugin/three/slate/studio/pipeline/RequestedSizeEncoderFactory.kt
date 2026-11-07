@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.content.Context
import android.media.metrics.LogSessionId
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Codec
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EncoderSelector
import androidx.media3.transformer.EncoderUtil
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.EncoderPreference

/**
 * The encoder factory of every export (appendix D Q38 (a), the maintainer's choice of 2026-11-07): media3's
 * [DefaultEncoderFactory] with fallback chooses the encoder, the MIME type and the bit rate as before, but when it also
 * changed the frame size (an encoder whose capability table is stricter than the encoder itself, such as the MTK and
 * Google encoders of the Redmi 22120RN86C that answer `isSizeSupported` false for 1920x1080 and 640x360 although they
 * run them, which made media3 encode 1920x1088 / 640x368 with black bars) the same encoder is configured and started at
 * the requested size first. Only when that fails does the media3 fallback size stay, and [onNote] reports the requested
 * and the encoded size (also when only the width changed, which media3's own fallback report leaves out). The video
 * encoder candidates follow the [preference] of the settings (roadmap D37, appendix D Q50 (b)): hardware or software
 * encoders first, the others as fallbacks.
 * zh-CN: 每次导出使用的编码器工厂 (附录 D Q38 (a), 维护者 2026-11-07 选): 由带回退的 media3 [DefaultEncoderFactory] 照旧选择编码器,
 * MIME 类型与码率; 但它同时改变了画面尺寸时 (编码器能力表比编码器本身更严, 例如 Redmi 22120RN86C 的 MTK 与谷歌编码器对 1920x1080 与
 * 640x360 的 `isSizeSupported` 回答 false, 实际却能运行, 导致 media3 编出带黑边的 1920x1088 / 640x368), 先以请求尺寸配置并启动同一个
 * 编码器. 只有失败时才保留 media3 的回退尺寸, 并由 [onNote] 报告请求与实际的尺寸 (只改宽度时也报告, media3 自己的回退报告不含宽度).
 * 视频编码器的候选顺序按设置中的 [preference] (路线图 D37, 附录 D Q50 (b)): 硬件或软件编码器在前, 其余作为回退.
 */
internal class RequestedSizeEncoderFactory(
    private val context: Context,
    private val videoSettings: VideoEncoderSettings,
    audioSettings: AudioEncoderSettings,
    private val preference: EncoderPreference = EncoderPreference.AUTO,
    private val onNote: (String) -> Unit,
) : Codec.EncoderFactory {

    private val fallback: DefaultEncoderFactory = DefaultEncoderFactory.Builder(context)
        .setEnableFallback(true)
        .setVideoEncoderSelector(selector(preference))
        .setRequestedVideoEncoderSettings(videoSettings)
        .setRequestedAudioEncoderSettings(audioSettings)
        .build()

    override fun createForAudioEncoding(format: Format, logSessionId: LogSessionId?): Codec =
        fallback.createForAudioEncoding(format, logSessionId)

    override fun audioNeedsEncoding(): Boolean = fallback.audioNeedsEncoding()

    override fun videoNeedsEncoding(): Boolean = fallback.videoNeedsEncoding()

    override fun createForVideoEncoding(format: Format, logSessionId: LogSessionId?): Codec {
        val chosen = fallback.createForVideoEncoding(format, logSessionId)
        val configured = chosen.configurationFormat
        if (format.width <= 0 || format.height <= 0 || (configured.width == format.width && configured.height == format.height)) return chosen
        val name = chosen.name
        val mime = configured.sampleMimeType ?: format.sampleMimeType
        val bitrate = configured.averageBitrate
        // One encoder at a time: devices short of codec instances fail the second one (appendix D Q37).
        // zh-CN: 同一时间只保留一个编码器: 编解码器实例紧张的设备会让第二个失败 (附录 D Q37).
        chosen.release()
        val exact = try {
            exactFactory(name, bitrate).createForVideoEncoding(format.buildUpon().setSampleMimeType(mime).build(), logSessionId)
        } catch (e: ExportException) {
            Log.i(TAG, "$name refused ${format.width}x${format.height}: ${e.errorCodeName} ${e.message}")
            null
        } catch (e: RuntimeException) {
            Log.i(TAG, "$name refused ${format.width}x${format.height}", e)
            null
        }
        if (exact != null) {
            val size = exact.configurationFormat
            if (size.width == format.width && size.height == format.height) return exact
            exact.release()
        }
        val again = fallback.createForVideoEncoding(format, logSessionId)
        val encoded = again.configurationFormat
        onNote(sizeNote(again.name, format.width, format.height, encoded.width, encoded.height))
        return again
    }

    /**
     * A factory without fallback that only offers the encoder [name] with [bitrate] (the choice of the fallback factory),
     * so the requested size is the only difference.
     * zh-CN: 不带回退的工厂, 只提供编码器 [name] 并使用码率 [bitrate] (带回退工厂的选择), 因此唯一的差别是请求尺寸.
     */
    private fun exactFactory(name: String, bitrate: Int): DefaultEncoderFactory {
        val settings = if (bitrate > 0) videoSettings.buildUpon().setBitrate(bitrate).build() else videoSettings
        return DefaultEncoderFactory.Builder(context)
            .setEnableFallback(false)
            .setVideoEncoderSelector { mime -> ImmutableList.copyOf(EncoderUtil.getSupportedEncoders(mime).filter { it.name == name }) }
            .setRequestedVideoEncoderSettings(settings)
            .build()
    }

    companion object {

        private const val TAG = "SlateStudioEncoders"

        /**
         * media3's default candidates in the order of [preference]; hardware acceleration as media3 reports it.
         * zh-CN: 按 [preference] 排序的 media3 默认候选; 是否硬件加速取 media3 的判断.
         */
        fun selector(preference: EncoderPreference): EncoderSelector = if (preference == EncoderPreference.AUTO) {
            EncoderSelector.DEFAULT
        } else {
            EncoderSelector { mime ->
                ImmutableList.copyOf(preference.order(EncoderSelector.DEFAULT.selectEncoderInfos(mime)) { EncoderUtil.isHardwareAccelerated(it, mime) })
            }
        }

        /**
         * The warning when the media3 fallback size stays, for example `the encoder c2.mtk.avc.encoder did not run
         * 1920x1080; the video was encoded at 1920x1088, the frame fitted inside`.
         * zh-CN: 保留 media3 回退尺寸时的警告, 示例见上.
         */
        fun sizeNote(encoder: String, requestedWidth: Int, requestedHeight: Int, encodedWidth: Int, encodedHeight: Int): String =
            "the encoder $encoder did not run ${requestedWidth}x$requestedHeight; the video was encoded at ${encodedWidth}x$encodedHeight, the frame fitted inside"
    }
}

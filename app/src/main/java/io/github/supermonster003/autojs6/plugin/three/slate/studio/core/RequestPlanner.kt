package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import java.io.File
import java.util.TimeZone

/** A submit request as plain values (the service converts the Bundle). zh-CN: 以纯值表示的 submit 请求 (由服务转换 Bundle). */
internal data class SubmitEnvelope(
    val op: String?,
    val inputs: List<String?>,
    val output: String?,
    val options: Map<String, Any?>,
    val hostToken: String?,
    val origin: String?,
    val registerMedia: Boolean,
)

/** A request that passed every submit check. zh-CN: 通过全部 submit 检查的请求. */
internal data class PlannedTask(
    val op: String,
    val inputs: List<String>,
    val output: String,
    val spec: OperationSpec,
    val origin: String,
    val registerMedia: Boolean,
) {
    val audioOutput: Boolean get() = spec is OperationSpec.ExtractAudio
}

/** The file checks of [RequestPlanner], replaceable in JVM tests. zh-CN: [RequestPlanner] 的文件检查, JVM 测试中可替换. */
internal interface PlannerFiles {
    fun exists(path: String): Boolean
    fun isReadableFile(path: String): Boolean
    fun isFile(path: String): Boolean

    /** True when [path] is a directory afterwards. zh-CN: 之后 [path] 是目录时为 true. */
    fun ensureDirectory(path: String): Boolean

    object Local : PlannerFiles {
        override fun exists(path: String) = File(path).exists()
        override fun isReadableFile(path: String) = File(path).let { it.isFile && it.canRead() }
        override fun isFile(path: String) = File(path).isFile
        override fun ensureDirectory(path: String) = File(path).let { it.isDirectory || it.mkdirs() || it.isDirectory }
    }
}

/** Encoder ids of the capability bundle (`mediaStudioVideoEncoders` / `AudioEncoders`). zh-CN: 能力 Bundle 中的编码器 id. */
internal object EncoderIds {
    const val H264 = "h264"
    const val H265 = "h265"
    const val AAC = "aac"
    const val FLAC = "flac"
    const val OPUS = "opus"
    const val PCM = "pcm"
}

/**
 * The submit checks after the envelope, in contract order: output path and extension, options ([OperationOptions]),
 * storage permission, inputs (exist and readable), output (free and its folder creatable), encoders. Every failure is a
 * [MediaStudioException]; checks that need the media content (trim start beyond the duration, a missing video or audio
 * track) run in the task and end in onError.
 * zh-CN: 外层字段之后的 submit 检查, 按契约顺序: 输出路径与扩展名, 选项 ([OperationOptions]), 存储权限, 输入 (存在且可读),
 * 输出 (未被占用且目录可建), 编码器. 每项失败都是 [MediaStudioException]; 需要媒体内容的检查 (裁剪起点超过时长, 缺视频轨或
 * 音频轨) 在任务中进行, 以 onError 结束.
 */
internal class RequestPlanner(
    private val storageRoot: String,
    /** Encoder ids present on the device ([EncoderIds]). zh-CN: 设备上可用的编码器 id ([EncoderIds]). */
    private val encoders: Set<String>,
    /** MediaMuxer writes OGG (API 29+). zh-CN: MediaMuxer 可写 OGG (API 29+). */
    private val oggMuxer: Boolean,
    /** True when [path] needs the all files access the plugin does not hold. zh-CN: [path] 需要插件尚未获得的全部文件访问权限时为 true. */
    private val lacksStorageAccess: (String) -> Boolean,
    private val isClaimed: (String) -> Boolean,
    private val files: PlannerFiles = PlannerFiles.Local,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> TimeZone = TimeZone::getDefault,
) {

    fun plan(envelope: SubmitEnvelope): PlannedTask {
        RequestValidation.rejection(envelope.op, envelope.inputs, envelope.hostToken)?.let { rejection ->
            val (code, detail) = MediaStudioErrorCodes.decode(rejection) ?: (MediaStudioErrorCodes.INVALID_OPTIONS to rejection)
            throw MediaStudioException(code, detail)
        }
        val op = envelope.op!!
        if (op !in OperationOptions.SUBMIT_OPS) {
            throw MediaStudioException.invalid("$op is answered by probe(), not submit")
        }
        val origin = envelope.origin ?: MediaStudioContract.ORIGIN_SCRIPT
        if (origin !in MediaStudioContract.ORIGINS) {
            throw MediaStudioException.invalid("origin must be one of ${MediaStudioContract.ORIGINS.joinToString()}, got $origin")
        }
        val inputs = envelope.inputs.map { it!! }
        inputs.firstOrNull { !it.startsWith("/") }?.let { throw MediaStudioException.invalid("inputs must be absolute paths, got $it") }

        var options = envelope.options
        val explicitOutput = envelope.output?.takeIf { it.isNotBlank() }
        if (explicitOutput != null) {
            if (!explicitOutput.startsWith("/") || explicitOutput.endsWith("/")) {
                throw MediaStudioException.invalid("output must be an absolute file path, got $explicitOutput")
            }
            val name = explicitOutput.substringAfterLast('/')
            val extension = if (name.lastIndexOf('.') > 0) name.substringAfterLast('.').lowercase() else ""
            if (op == MediaStudioOps.EXTRACT_AUDIO) {
                val container = AudioContainer.ofExtension(extension) ?: throw MediaStudioException.unsupported(
                    "audio output must end with ${AudioContainer.entries.joinToString { "." + it.extension }}, got $name",
                )
                val format = options["format"]
                if (format == null) {
                    options = options + ("format" to container.id)
                } else if (format is String && format != container.id && format in AudioContainer.entries.map { it.id }) {
                    throw MediaStudioException.invalid("options.format $format does not match the output extension .$extension")
                }
            } else if (extension != VIDEO_EXTENSION) {
                throw MediaStudioException.unsupported("video output must end with .$VIDEO_EXTENSION, got $name")
            }
        }
        val spec = OperationOptions.parse(op, inputs.size, options)

        val output = explicitOutput ?: defaultOutput(op, inputs, spec)
        (inputs + output).firstOrNull(lacksStorageAccess)?.let { path ->
            throw MediaStudioException(
                MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED,
                "the plugin needs all files access for $path; grant it in the 3-Slate Studio storage screen",
            )
        }
        inputs.forEach { input ->
            if (!files.isFile(input)) throw MediaStudioException.io("input not found: $input")
            if (!files.isReadableFile(input)) throw MediaStudioException.io("input is not readable: $input")
        }
        if (files.exists(output)) throw MediaStudioException.io("output already exists: $output")
        if (isClaimed(output)) throw MediaStudioException.io("output $output is in use by another task")
        val parent = output.substringBeforeLast('/').ifEmpty { "/" }
        if (!files.ensureDirectory(parent)) throw MediaStudioException.io("cannot create the output folder $parent")
        checkEncoders(spec)
        return PlannedTask(op, inputs, output, spec, origin, envelope.registerMedia)
    }

    private fun defaultOutput(op: String, inputs: List<String>, spec: OperationSpec): String {
        val audio = spec is OperationSpec.ExtractAudio
        val extension = (spec as? OperationSpec.ExtractAudio)?.container?.extension ?: VIDEO_EXTENSION
        // Concat has no single source, so its default name uses the fallback (roadmap D14).
        // zh-CN: 拼接没有单一来源, 默认文件名用后备名 (路线图 D14).
        val source = inputs.singleOrNull()
        val candidate = OutputPaths.defaultOutput(storageRoot, source, op, extension, audio, clock(), zone())
        return OutputPaths.uniquePath(candidate) { files.exists(it) || isClaimed(it) }
    }

    private fun checkEncoders(spec: OperationSpec) {
        val video = when (spec) {
            is OperationSpec.Concat -> spec.video
            is OperationSpec.Speed -> spec.video
            is OperationSpec.Transform -> spec.video
            is OperationSpec.Transcode -> spec.video
            else -> null
        }
        if (video?.codec == VideoCodec.H265 && EncoderIds.H265 !in encoders) {
            throw MediaStudioException.unsupported("this device has no H.265 encoder; use codec h264")
        }
        if (video != null && video.codec == VideoCodec.H264 && EncoderIds.H264 !in encoders) {
            throw MediaStudioException.unsupported("this device has no H.264 encoder")
        }
        if (spec is OperationSpec.ExtractAudio) {
            when (spec.container) {
                AudioContainer.M4A -> if (EncoderIds.AAC !in encoders) throw MediaStudioException.unsupported("this device has no AAC encoder")
                AudioContainer.WAV -> Unit
                AudioContainer.FLAC -> if (EncoderIds.FLAC !in encoders) throw MediaStudioException.unsupported("this device has no FLAC encoder")
                AudioContainer.OGG -> {
                    if (!oggMuxer) throw MediaStudioException.unsupported("ogg output needs Android 10 (API 29) or later")
                    if (EncoderIds.OPUS !in encoders) throw MediaStudioException.unsupported("this device has no Opus encoder")
                }
            }
        }
    }

    companion object {
        const val VIDEO_EXTENSION = "mp4"
    }

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
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
    /** The `compose` timeline (contract key `timeline`). zh-CN: `compose` 的时间线 (契约键 `timeline`). */
    val timeline: Map<String, Any?>? = null,
)

/**
 * A request that passed every submit check; [output] is a folder for [OutputKind.DIRECTORY].
 * zh-CN: 通过全部 submit 检查的请求; [OutputKind.DIRECTORY] 时 [output] 为文件夹.
 */
internal data class PlannedTask(
    val op: String,
    val inputs: List<String>,
    val output: String,
    val spec: OperationSpec,
    val origin: String,
    val registerMedia: Boolean,
) {
    val kind: OutputKind get() = OutputKind.of(op)

    val audioOutput: Boolean get() = kind == OutputKind.AUDIO
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
 * track) run in the task and end in onError. The output extension follows the [OutputKind]: `.mp4` for video, an
 * [AudioContainer] for audio, `.png` / `.jpg` / `.jpeg` for images, `.gif`; `frames` takes a folder (a trailing `/` is
 * allowed, an existing folder is used as it is). An image or audio extension fills a missing `format` option and must
 * match a given one.
 * zh-CN: 外层字段之后的 submit 检查, 按契约顺序: 输出路径与扩展名, 选项 ([OperationOptions]), 存储权限, 输入 (存在且可读),
 * 输出 (未被占用且目录可建), 编码器. 每项失败都是 [MediaStudioException]; 需要媒体内容的检查 (裁剪起点超过时长, 缺视频轨或
 * 音频轨) 在任务中进行, 以 onError 结束. 输出扩展名按 [OutputKind]: 视频 `.mp4`, 音频为某种 [AudioContainer], 图片 `.png` /
 * `.jpg` / `.jpeg`, `.gif`; `frames` 接受文件夹 (可以 `/` 结尾, 已有文件夹按原样使用). 图片或音频扩展名在缺少 `format` 选项时
 * 补上它, 给出时必须一致.
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
        var inputs = envelope.inputs.map { it!! }
        inputs.firstOrNull { !it.startsWith("/") }?.let { throw MediaStudioException.invalid("inputs must be absolute paths, got $it") }

        val kind = OutputKind.of(op)
        var options = envelope.options
        var explicitOutput = envelope.output?.takeIf { it.isNotBlank() }
        if (kind == OutputKind.NONE) return planReadOnly(op, inputs, explicitOutput, options, origin)
        if (explicitOutput == null && kind == OutputKind.SUBTITLE) throw MediaStudioException.invalid("$op needs an output path")
        if (explicitOutput != null && kind == OutputKind.DIRECTORY) {
            if (!explicitOutput.startsWith("/")) throw MediaStudioException.invalid("output must be an absolute folder path, got $explicitOutput")
            explicitOutput = explicitOutput.trimEnd('/').ifEmpty { "/" }
        } else if (explicitOutput != null) {
            if (!explicitOutput.startsWith("/") || explicitOutput.endsWith("/")) {
                throw MediaStudioException.invalid("output must be an absolute file path, got $explicitOutput")
            }
            options = checkExtension(kind, explicitOutput.substringAfterLast('/'), options)
        }
        val spec = OperationOptions.parse(op, inputs.size, options, envelope.timeline)
        // compose reads the clips of its timeline; the inputs the host lists are the same files. zh-CN: compose 读取时间线中的片段; 宿主列出的输入是同样的文件.
        if (spec is OperationSpec.Compose) inputs = spec.timeline.sources

        val output = explicitOutput ?: defaultOutput(op, inputs, spec, kind)
        val resources = OperationOptions.resources(spec)
        (inputs + resources + output).firstOrNull(lacksStorageAccess)?.let { path ->
            throw MediaStudioException(
                MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED,
                "the plugin needs all files access for $path; grant it in the 3-Slate Studio storage screen",
            )
        }
        inputs.forEach { input ->
            if (!files.isFile(input)) throw MediaStudioException.io("input not found: $input")
            if (!files.isReadableFile(input)) throw MediaStudioException.io("input is not readable: $input")
        }
        resources.forEach { resource ->
            if (!files.isFile(resource)) throw MediaStudioException.io("file not found: $resource")
            if (!files.isReadableFile(resource)) throw MediaStudioException.io("file is not readable: $resource")
        }
        if (kind == OutputKind.DIRECTORY) {
            if (files.isFile(output)) throw MediaStudioException.io("output is a file, not a folder: $output")
        } else if (files.exists(output)) {
            throw MediaStudioException.io("output already exists: $output")
        }
        if (isClaimed(output)) throw MediaStudioException.io("output $output is in use by another task")
        val parent = output.substringBeforeLast('/').ifEmpty { "/" }
        if (!files.ensureDirectory(parent)) throw MediaStudioException.io("cannot create the output folder $parent")
        checkEncoders(spec)
        return PlannedTask(op, inputs, output, spec, origin, envelope.registerMedia)
    }

    /**
     * `subtitlesParse`: reads its input and writes nothing, so there is no output to check or claim (the task output is
     * empty).
     * zh-CN: `subtitlesParse`: 读取输入, 不写文件, 因此没有要检查或占用的输出 (任务输出为空).
     */
    private fun planReadOnly(op: String, inputs: List<String>, output: String?, options: Map<String, Any?>, origin: String): PlannedTask {
        if (output != null) throw MediaStudioException.invalid("$op writes no file; leave out the output, got $output")
        val spec = OperationOptions.parse(op, inputs.size, options)
        inputs.firstOrNull(lacksStorageAccess)?.let { path ->
            throw MediaStudioException(
                MediaStudioErrorCodes.STORAGE_PERMISSION_DENIED,
                "the plugin needs all files access for $path; grant it in the 3-Slate Studio storage screen",
            )
        }
        inputs.forEach { input ->
            if (!files.isFile(input)) throw MediaStudioException.io("input not found: $input")
            if (!files.isReadableFile(input)) throw MediaStudioException.io("input is not readable: $input")
        }
        return PlannedTask(op, inputs, "", spec, origin, registerMedia = false)
    }

    /**
     * The options with `format` filled from the output [name], after checking its extension against [kind].
     * zh-CN: 按 [kind] 检查输出 [name] 的扩展名后, 返回由扩展名补上 `format` 的选项.
     */
    private fun checkExtension(kind: OutputKind, name: String, options: Map<String, Any?>): Map<String, Any?> {
        val extension = if (name.lastIndexOf('.') > 0) name.substringAfterLast('.').lowercase() else ""
        val format = options["format"]
        fun filled(id: String, known: Collection<String>, same: (String) -> Boolean): Map<String, Any?> = when {
            format == null -> options + ("format" to id)
            format is String && format in known && !same(format) ->
                throw MediaStudioException.invalid("options.format $format does not match the output extension .$extension")
            else -> options
        }
        return when (kind) {
            OutputKind.AUDIO -> {
                val container = AudioContainer.ofExtension(extension) ?: throw MediaStudioException.unsupported(
                    "audio output must end with ${AudioContainer.entries.joinToString { "." + it.extension }}, got $name",
                )
                filled(container.id, AudioContainer.entries.map { it.id }) { it == container.id }
            }
            OutputKind.IMAGE -> {
                val image = ImageFormat.of(extension)
                    ?: throw MediaStudioException.unsupported("image output must end with .png, .jpg or .jpeg, got $name")
                filled(extension, ImageFormat.NAMES) { ImageFormat.of(it) == image }
            }
            OutputKind.GIF -> options.also {
                if (extension != GIF_EXTENSION) throw MediaStudioException.unsupported("GIF output must end with .$GIF_EXTENSION, got $name")
            }
            OutputKind.VIDEO -> options.also {
                if (extension != VIDEO_EXTENSION) throw MediaStudioException.unsupported("video output must end with .$VIDEO_EXTENSION, got $name")
            }
            OutputKind.SUBTITLE -> {
                val format = SubtitleFormat.entries.firstOrNull { it.extension == extension }
                    ?: throw MediaStudioException.unsupported("subtitle output must end with .srt or .ass, got $name")
                filled(format.id, SubtitleFormat.IDS) { it == format.id }
            }
            OutputKind.DIRECTORY, OutputKind.NONE -> options
        }
    }

    private fun defaultOutput(op: String, inputs: List<String>, spec: OperationSpec, kind: OutputKind): String {
        // Concat and audioMix have no single source, so their default name uses the fallback (roadmap D14).
        // zh-CN: 拼接与混音没有单一来源, 默认文件名用后备名 (路线图 D14).
        val source = inputs.singleOrNull()
        val candidate = when (kind) {
            OutputKind.DIRECTORY -> OutputPaths.defaultDirectory(storageRoot, kind) + "/" + OutputPaths.defaultBaseName(source, op, clock(), zone())
            else -> OutputPaths.defaultOutput(storageRoot, source, op, extensionOf(spec, kind), kind, clock(), zone())
        }
        return OutputPaths.uniquePath(candidate, folder = kind == OutputKind.DIRECTORY) { files.exists(it) || isClaimed(it) }
    }

    private fun extensionOf(spec: OperationSpec, kind: OutputKind): String = when (spec) {
        is OperationSpec.AudioOutput -> spec.container.extension
        is OperationSpec.Frame -> spec.image.format.extension
        is OperationSpec.Thumbnail -> spec.image.format.extension
        else -> if (kind == OutputKind.GIF) GIF_EXTENSION else VIDEO_EXTENSION
    }

    private fun checkEncoders(spec: OperationSpec) {
        val video = when (spec) {
            is OperationSpec.Concat -> spec.video
            is OperationSpec.Speed -> spec.video
            is OperationSpec.Transform -> spec.video
            is OperationSpec.Transcode -> spec.video
            is OperationSpec.Overlay -> spec.video
            is OperationSpec.Compose -> spec.video
            is OperationSpec.BurnSubtitles -> spec.video
            else -> null
        }
        if (video?.codec == VideoCodec.H265 && EncoderIds.H265 !in encoders) {
            throw MediaStudioException.unsupported("this device has no H.265 encoder; use codec h264")
        }
        if (video != null && video.codec == VideoCodec.H264 && EncoderIds.H264 !in encoders) {
            throw MediaStudioException.unsupported("this device has no H.264 encoder")
        }
        if (spec is OperationSpec.AudioOutput) {
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
        const val GIF_EXTENSION = "gif"
    }

}

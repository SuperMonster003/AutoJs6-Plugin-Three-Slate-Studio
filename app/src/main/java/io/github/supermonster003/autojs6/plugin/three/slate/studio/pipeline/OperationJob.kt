package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.content.Context
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStoreRegistrar
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaWarnings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.PlannedTask
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * One submitted operation (roadmap P2.1 / P2.2): probes the inputs, writes to a hidden part file next to the output,
 * renames it to the output when the export succeeds, probes and registers the result. The result Bundle carries
 * `path`, `durationMillis`, `sizeBytes`, `mime`, `codec`, `warnings` and `width` / `height` (video) or `sampleRate` /
 * `channels` (audio). `warnings` holds the OUTPUT_LARGE / DURATION_LONG codes (Q6 (a)) followed by plain notes such as
 * encoder fallbacks. Images, GIF and frame folders (P2.4) have their own results, see [stillResult] and [frames].
 * zh-CN: 一个已提交的操作 (路线图 P2.1 / P2.2): 探测输入, 写入输出旁的隐藏临时文件, 导出成功后改名为输出, 再探测并登记结果.
 * 结果 Bundle 含 `path`, `durationMillis`, `sizeBytes`, `mime`, `codec`, `warnings`, 以及 `width` / `height` (视频) 或
 * `sampleRate` / `channels` (音频). `warnings` 先列 OUTPUT_LARGE / DURATION_LONG 代码 (Q6 (a)), 再列编码器回退等文字说明.
 * 图片, GIF 与帧文件夹 (P2.4) 的结果另见 [stillResult] 与 [frames].
 */
internal class OperationJob(private val appContext: Context, private val planned: PlannedTask) : TaskRunner.Job<Bundle> {

    private val outputFile = File(planned.output)

    @Volatile
    private var part: File? = null

    @Volatile
    private var created = false

    @Volatile
    private var createdDirectory = false

    private val writtenFrames = CopyOnWriteArrayList<File>()

    override fun run(context: TaskRunner.JobContext): Bundle {
        context.progress(0.0, MediaStudioContract.STAGE_PROBING)
        val inputs = planned.inputs.mapIndexed { index, path ->
            context.throwIfCancelled()
            val info = if (planned.audioOutput) MediaProbe.probeAudio(path) else MediaProbe.probe(path)
            context.progress(PROBE_END * (index + 1) / planned.inputs.size, MediaStudioContract.STAGE_PROBING)
            info
        }
        if (planned.kind == OutputKind.DIRECTORY) return frames(context, inputs.single())
        val partFile = partFile(context.taskId).also { part = it }
        if (partFile.exists() && !partFile.delete()) throw MediaStudioException.io("cannot replace the temporary file ${partFile.path}")
        val notes = mutableListOf<String>()
        val spec = planned.spec
        val still = when {
            spec is OperationSpec.Frame || spec is OperationSpec.Thumbnail ->
                StillOperations(context).image(spec, inputs.single(), partFile.path, PROBE_END, EXPORT_END)
            spec is OperationSpec.ToGif -> StillOperations(context).gif(spec, inputs.single(), partFile.path, PROBE_END, EXPORT_END)
            // extractAudio to M4A remuxes or re-encodes through Transformer. zh-CN: extractAudio 到 M4A 经 Transformer 重封装或重编码.
            spec is OperationSpec.AudioOutput && !(spec is OperationSpec.ExtractAudio && spec.container == AudioContainer.M4A) -> {
                AudioOperations(context, notes).run(spec, inputs, partFile.path, PROBE_END, EXPORT_END)
                null
            }
            else -> {
                VideoOperations(appContext, context, notes).run(spec, inputs, partFile.path, PROBE_END, EXPORT_END)
                null
            }
        }
        context.throwIfCancelled()
        context.progress(EXPORT_END, MediaStudioContract.STAGE_WRITING)
        if (!partFile.isFile || partFile.length() == 0L) {
            throw MediaStudioException(MediaStudioErrorCodes.CODEC_ERROR, "the export wrote no data to ${partFile.path}")
        }
        if (outputFile.exists()) {
            throw MediaStudioException.io("${outputFile.path} appeared while the task ran; the result was not saved")
        }
        if (!partFile.renameTo(outputFile)) throw MediaStudioException.io("cannot move the result to ${outputFile.path}")
        created = true
        part = null
        if (still != null) return stillResult(context, still, inputs, notes)
        val result = try {
            MediaProbe.probe(outputFile.path)
        } catch (e: MediaStudioException) {
            throw MediaStudioException(MediaStudioErrorCodes.CODEC_ERROR, "the output cannot be read back (${e.detail})", e)
        }
        context.progress(REGISTER_START, MediaStudioContract.STAGE_WRITING)
        // The scanner never indexes the private folders of the plugin. zh-CN: 媒体扫描从不索引插件的私有目录.
        val register = planned.registerMedia && !StorageAccess.isPrivate(appContext, result.path)
        if (register && !MediaStoreRegistrar.register(appContext, result.path, result.mime)) {
            notes += "the media scanner did not finish within 3 s; the file may show up in galleries later"
        }
        val warnings = (MediaWarnings.of(result.media, inputs.map { it.media }) + notes).distinct()
        return resultBundle(result, warnings)
    }

    override fun discard() {
        part?.let { if (it.exists()) it.delete() }
        if (planned.kind == OutputKind.DIRECTORY) {
            // Only the frames of this task; a folder that already held files keeps them. zh-CN: 只删本任务的帧; 原有文件保留.
            writtenFrames.forEach { it.delete() }
            if (createdDirectory) outputFile.delete()
        } else if (created && outputFile.exists()) {
            outputFile.delete()
        }
    }

    /**
     * `frames` (roadmap P2.4): the images go straight into the output folder, created when missing. The result carries
     * `path` (the folder), `paths` (the images in frame order), `width`, `height`, `mime`, `sizeBytes` (all images) and
     * `warnings`; the images are registered in one scan.
     * zh-CN: `frames` (路线图 P2.4): 图片直接写进输出文件夹, 文件夹不存在时创建. 结果含 `path` (文件夹), `paths` (按帧顺序的图片),
     * `width`, `height`, `mime`, `sizeBytes` (全部图片) 与 `warnings`; 图片在一次扫描中登记.
     */
    private fun frames(context: TaskRunner.JobContext, input: MediaInfo): Bundle {
        if (!outputFile.isDirectory) {
            if (outputFile.exists()) throw MediaStudioException.io("output is a file, not a folder: ${outputFile.path}")
            if (!outputFile.mkdirs()) throw MediaStudioException.io("cannot create the output folder ${outputFile.path}")
            createdDirectory = true
        }
        val spec = planned.spec as OperationSpec.Frames
        val frames = StillOperations(context).frames(spec, input, outputFile, { writtenFrames += it }, PROBE_END, EXPORT_END)
        context.throwIfCancelled()
        context.progress(REGISTER_START, MediaStudioContract.STAGE_WRITING)
        val notes = mutableListOf<String>()
        val paths = frames.files.map { it.path }
        if (planned.registerMedia && !StorageAccess.isPrivate(appContext, outputFile.path) &&
            !MediaStoreRegistrar.registerAll(appContext, paths, frames.mime)
        ) {
            notes += "the media scanner did not finish within 3 s; the files may show up in galleries later"
        }
        val size = frames.files.sumOf { it.length() }
        val warnings = (MediaWarnings.of(MediaWarnings.Media(size, 0), listOf(input.media)) + notes).distinct()
        return Bundle().apply {
            putString(MediaStudioResultKeys.PATH, outputFile.path)
            putStringArray(MediaStudioResultKeys.PATHS, paths.toTypedArray())
            putInt(MediaStudioResultKeys.WIDTH, frames.width)
            putInt(MediaStudioResultKeys.HEIGHT, frames.height)
            putString(MediaStudioResultKeys.MIME, frames.mime)
            putLong(MediaStudioResultKeys.SIZE_BYTES, size)
            putStringArray(MediaStudioResultKeys.WARNINGS, warnings.toTypedArray())
        }
    }

    /**
     * `frame`, `thumbnail` and `toGif`: `path`, `width`, `height`, `mime`, `sizeBytes`, `warnings`, and `durationMillis`
     * for a GIF. No codec, the images are not probed again.
     * zh-CN: `frame`, `thumbnail` 与 `toGif`: `path`, `width`, `height`, `mime`, `sizeBytes`, `warnings`, GIF 另有
     * `durationMillis`. 不含编解码器, 也不再探测图片.
     */
    private fun stillResult(context: TaskRunner.JobContext, still: StillOperations.Still, inputs: List<MediaInfo>, notes: MutableList<String>): Bundle {
        context.progress(REGISTER_START, MediaStudioContract.STAGE_WRITING)
        if (planned.registerMedia && !StorageAccess.isPrivate(appContext, outputFile.path) &&
            !MediaStoreRegistrar.register(appContext, outputFile.path, still.mime)
        ) {
            notes += "the media scanner did not finish within 3 s; the file may show up in galleries later"
        }
        val size = outputFile.length()
        val warnings = (MediaWarnings.of(MediaWarnings.Media(size, still.durationMillis ?: 0), inputs.map { it.media }) + notes).distinct()
        return Bundle().apply {
            putString(MediaStudioResultKeys.PATH, outputFile.path)
            putInt(MediaStudioResultKeys.WIDTH, still.width)
            putInt(MediaStudioResultKeys.HEIGHT, still.height)
            putString(MediaStudioResultKeys.MIME, still.mime)
            putLong(MediaStudioResultKeys.SIZE_BYTES, size)
            still.durationMillis?.let { putLong(MediaStudioResultKeys.DURATION_MILLIS, it) }
            putStringArray(MediaStudioResultKeys.WARNINGS, warnings.toTypedArray())
        }
    }

    private fun partFile(taskId: Int): File {
        val parent = outputFile.absoluteFile.parentFile ?: throw MediaStudioException.io("${outputFile.path} has no parent folder")
        val extension = outputFile.extension.takeIf { it.isNotEmpty() }?.let { ".$it" } ?: ""
        return File(parent, ".${outputFile.nameWithoutExtension}.part-$taskId$extension")
    }

    private fun resultBundle(info: MediaInfo, warnings: List<String>): Bundle = Bundle().apply {
        putString(MediaStudioResultKeys.PATH, info.path)
        putLong(MediaStudioResultKeys.DURATION_MILLIS, info.durationMillis)
        putLong(MediaStudioResultKeys.SIZE_BYTES, info.sizeBytes)
        info.mime?.let { putString(MediaStudioResultKeys.MIME, it) }
        if (planned.audioOutput) {
            info.audioCodec?.let { putString(MediaStudioResultKeys.CODEC, it) }
            info.sampleRate?.let { putInt(MediaStudioResultKeys.SAMPLE_RATE, it) }
            info.channels?.let { putInt(MediaStudioResultKeys.CHANNELS, it) }
        } else {
            info.width?.let { putInt(MediaStudioResultKeys.WIDTH, it) }
            info.height?.let { putInt(MediaStudioResultKeys.HEIGHT, it) }
            info.videoCodec?.let { putString(MediaStudioResultKeys.CODEC, it) }
        }
        putStringArray(MediaStudioResultKeys.WARNINGS, warnings.toTypedArray())
    }

    private companion object {
        const val PROBE_END = 5.0
        const val EXPORT_END = 95.0
        const val REGISTER_START = 97.0
    }

}

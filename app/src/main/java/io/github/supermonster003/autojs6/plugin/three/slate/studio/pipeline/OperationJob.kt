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
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.PlannedTask
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TaskRunner
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import java.io.File

/**
 * One submitted operation (roadmap P2.1 / P2.2): probes the inputs, writes to a hidden part file next to the output,
 * renames it to the output when the export succeeds, probes and registers the result. The result Bundle carries
 * `path`, `durationMillis`, `sizeBytes`, `mime`, `codec`, `warnings` and `width` / `height` (video) or `sampleRate` /
 * `channels` (audio). `warnings` holds the OUTPUT_LARGE / DURATION_LONG codes (Q6 (a)) followed by plain notes such as
 * encoder fallbacks.
 * zh-CN: 一个已提交的操作 (路线图 P2.1 / P2.2): 探测输入, 写入输出旁的隐藏临时文件, 导出成功后改名为输出, 再探测并登记结果.
 * 结果 Bundle 含 `path`, `durationMillis`, `sizeBytes`, `mime`, `codec`, `warnings`, 以及 `width` / `height` (视频) 或
 * `sampleRate` / `channels` (音频). `warnings` 先列 OUTPUT_LARGE / DURATION_LONG 代码 (Q6 (a)), 再列编码器回退等文字说明.
 */
internal class OperationJob(private val appContext: Context, private val planned: PlannedTask) : TaskRunner.Job<Bundle> {

    private val outputFile = File(planned.output)

    @Volatile
    private var part: File? = null

    @Volatile
    private var created = false

    override fun run(context: TaskRunner.JobContext): Bundle {
        context.progress(0.0, MediaStudioContract.STAGE_PROBING)
        val inputs = planned.inputs.mapIndexed { index, path ->
            context.throwIfCancelled()
            val info = if (planned.audioOutput) MediaProbe.probeAudio(path) else MediaProbe.probe(path)
            context.progress(PROBE_END * (index + 1) / planned.inputs.size, MediaStudioContract.STAGE_PROBING)
            info
        }
        val partFile = partFile(context.taskId).also { part = it }
        if (partFile.exists() && !partFile.delete()) throw MediaStudioException.io("cannot replace the temporary file ${partFile.path}")
        val notes = mutableListOf<String>()
        val spec = planned.spec
        if (spec is OperationSpec.ExtractAudio && spec.container != AudioContainer.M4A) {
            AudioExport(context, notes).run(inputs.single(), partFile.path, spec, PROBE_END, EXPORT_END)
        } else {
            VideoOperations(appContext, context, notes).run(spec, inputs, partFile.path, PROBE_END, EXPORT_END)
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
        if (created && outputFile.exists()) outputFile.delete()
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

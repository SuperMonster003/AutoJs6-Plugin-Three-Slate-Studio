package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.timeline

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.timeline.TimelineCalls
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.BitRates
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.CodecResources
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaInfo
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputPaths
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleCue
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.preview.TimelinePreview
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditHistory
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditOps
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.EditorProject
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.ExportSettings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.ProjectFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.slate.studio.project.TimelineJson
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StudioPreferences
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioTasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TimeZone
import kotlin.math.roundToLong

/**
 * The state of the timeline editor screen (roadmap P4.3): the project with its undo history ([EditHistory], at least
 * [UNDO_STEPS] steps), the selection, the zoom, the project file and the preview. Edits go through [edit] (one step) or
 * [beginGesture] / [gesture] / [endGesture] (a drag is one step); media is probed off the main thread and cached for
 * saving and for the export check.
 * zh-CN: 时间线剪辑器界面的状态 (路线图 P4.3): 工程及其撤销历史 ([EditHistory], 至少 [UNDO_STEPS] 步), 选择, 缩放, 工程文件与预览.
 * 编辑经 [edit] (一步) 或 [beginGesture] / [gesture] / [endGesture] (一次拖动为一步); 媒体在后台线程探测并缓存, 供保存与导出检查使用.
 */
internal class EditorModel(application: Application) : AndroidViewModel(application) {

    private val history = EditHistory(blank(application), UNDO_STEPS)

    var project by mutableStateOf(history.current)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    var selected by mutableStateOf<Long?>(null)

    /** Track pixels per second of timeline, in dp. zh-CN: 每秒时间线对应的轨道宽度 (dp). */
    var zoom by mutableDoubleStateOf(ProjectFiles.EditorState.DEFAULT_ZOOM)
    var file by mutableStateOf<File?>(null)
        private set
    var saved by mutableStateOf(history.current)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<Message?>(null)

    val preview = TimelinePreview(application)
    val media = TimelineMedia()
    private val sources = HashMap<String, MediaInfo>()
    private var gestureBase: EditorProject? = null

    val dirty: Boolean get() = project != saved

    val name: String? get() = file?.let(ProjectStore::nameOf)

    /** A message for the snackbar; [detail] is shown as it is (plugin error details stay English). zh-CN: 提示消息. */
    data class Message(@param:StringRes val text: Int, val detail: String? = null)

    init {
        preview.project = project
    }

    override fun onCleared() {
        preview.release()
        media.close()
    }

    // Edits. zh-CN: 编辑.

    fun edit(change: (EditorProject) -> EditorProject) {
        history.apply(change(history.current))
        publish()
    }

    fun beginGesture() {
        history.begin()
        gestureBase = history.current
    }

    /** Applies [change] to the state at the start of the gesture. zh-CN: 把 [change] 作用于手势开始时的状态. */
    fun gesture(change: (EditorProject) -> EditorProject) {
        val base = gestureBase ?: return
        history.update(change(base))
        publish()
    }

    fun endGesture() {
        gestureBase = null
        history.commit()
        publish()
    }

    fun undo() {
        if (history.undo()) publish()
    }

    fun redo() {
        if (history.redo()) publish()
    }

    private fun publish() {
        project = history.current
        canUndo = history.canUndo
        canRedo = history.canRedo
        preview.project = project
        selected?.let { id -> if (!contains(project, id)) selected = null }
        val end = project.durationUs / 1_000.0
        if (preview.positionMs > end) preview.seek(end) else if (!preview.playing) preview.sync()
    }

    val playheadMs: Long get() = preview.positionMs.roundToLong()

    // Media. zh-CN: 媒体.

    enum class AddKind { VIDEO, AUDIO, IMAGE }

    fun add(paths: List<String>, kind: AddKind) {
        if (paths.isEmpty()) return
        viewModelScope.launch {
            busy = true
            val probed = withContext(Dispatchers.IO) { paths.map { path -> path to runCatching { if (kind == AddKind.IMAGE) null else probe(path) } } }
            busy = false
            var current = history.current
            var last: Long? = null
            for ((path, result) in probed) {
                val info = result.getOrElse { error ->
                    message = Message(R.string.timeline_message_unreadable, (error as? MediaStudioException)?.detail ?: error.message)
                    continue
                }
                val (next, id) = when (kind) {
                    AddKind.VIDEO -> EditOps.addVideo(current, videoTrackOfSelection(current), sourceOf(info!!))
                    AddKind.AUDIO -> EditOps.addAudio(current, audioTrackOfSelection(current), sourceOf(info!!))
                    AddKind.IMAGE -> EditOps.addImage(current, playheadMs, path)
                }
                if (id < 0) {
                    message = Message(if (kind == AddKind.AUDIO) R.string.timeline_message_no_audio else R.string.timeline_message_no_video)
                    continue
                }
                current = next
                last = id
            }
            if (current != history.current) {
                history.apply(current)
                selected = last
                publish()
            }
        }
    }

    private fun videoTrackOfSelection(project: EditorProject): Int = selected?.let { project.videoClip(it)?.first } ?: 0

    private fun audioTrackOfSelection(project: EditorProject): Int = selected?.let { project.audioClip(it)?.first } ?: 0

    private fun sourceOf(info: MediaInfo) = EditOps.SourceInfo(info.path, info.durationMillis, info.width ?: 0, info.height ?: 0, info.hasVideo, info.hasAudio)

    private fun probe(path: String): MediaInfo = synchronized(sources) { sources[path] } ?: MediaProbe.probe(path).also { synchronized(sources) { sources[path] = it } }

    // Subtitles. zh-CN: 字幕.

    /** Reads an SRT or ASS file into cues. zh-CN: 把 SRT 或 ASS 文件读成 cue. */
    fun importSubtitles(path: String, append: Boolean) {
        viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { runCatching { cuesOf(path) } }
            busy = false
            result.onSuccess { cues ->
                if (cues.isEmpty()) message = Message(R.string.timeline_message_no_cues)
                else edit { EditOps.importCues(it, cues, append) }
            }.onFailure { error ->
                message = Message(R.string.timeline_message_unreadable, (error as? MediaStudioException)?.detail ?: error.message)
            }
        }
    }

    /** The cues as an SRT file. zh-CN: cue 的 SRT 文本. */
    fun srt(): String = SubtitleFiles.writeSrt(project.subtitles.cues.map { SubtitleCue(it.startMs, it.endMs, it.text) })

    fun writeSrt(uri: Uri) {
        val text = srt()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) } }.isSuccess
            }
            message = Message(if (ok) R.string.timeline_message_srt_saved else R.string.timeline_message_write_failed)
        }
    }

    // Projects. zh-CN: 工程.

    fun newProject() {
        preview.pause()
        history.reset(blank(getApplication()))
        file = null
        saved = history.current
        selected = null
        zoom = ProjectFiles.EditorState.DEFAULT_ZOOM
        publish()
        preview.seek(0.0)
    }

    /** Saves to the current file or, the first time, to a new one in [ProjectStore]. zh-CN: 保存到当前文件, 首次保存到新文件. */
    fun save(onSaved: (File) -> Unit = {}) {
        val snapshot = project
        if (snapshot.isEmpty) {
            message = Message(R.string.timeline_message_empty)
            return
        }
        val state = ProjectFiles.EditorState(zoom, playheadMs, selected)
        val target = file ?: ProjectStore.newFile(getApplication())
        viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val infos = snapshot.sources.associateWith { probe(it) }
                    ProjectFiles.save(target, snapshot, state, infos)
                }
            }
            busy = false
            result.onSuccess {
                file = target
                saved = snapshot
                message = Message(R.string.timeline_message_saved)
                onSaved(target)
            }.onFailure { error ->
                message = Message(R.string.timeline_message_write_failed, (error as? MediaStudioException)?.detail ?: error.message)
            }
        }
    }

    /** Opens [target]; [text] is given for a project read from a document (it is then saved as a new file). zh-CN: 打开工程. */
    fun open(target: File?, text: String? = null) {
        preview.pause()
        viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                runCatching { ProjectFiles.open(text ?: target!!.readText(Charsets.UTF_8), ::probe, ::cuesOf) }
            }
            busy = false
            result.onSuccess { (opened, state, infos) ->
                synchronized(sources) { sources.putAll(infos) }
                history.reset(opened)
                file = if (text == null) target else null
                saved = if (text == null) opened else EditorProject()
                zoom = state.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
                selected = state.selected
                publish()
                preview.seek(state.playheadMs.toDouble())
            }.onFailure { error ->
                message = Message(R.string.timeline_message_open_failed, (error as? MediaStudioException)?.detail ?: error.message)
            }
        }
    }

    // Export. zh-CN: 导出.

    /** The plugin's own validation of the timeline, or null when it passes. zh-CN: 插件自身对时间线的校验, 通过时为 null. */
    suspend fun check(): String? = withContext(Dispatchers.IO) { TimelineJson.check(project, ::probe) }

    /**
     * The decoder budget of the timeline (6 tracks with overlapping transitions + 16 audio tracks pass, appendix D Q37),
     * or null when it is within the measured count.
     * zh-CN: 时间线的解码器预算 (6 条带重叠转场的视频轨 + 16 条音频轨可通过, 附录 D Q37), 不超过实测值时为 null.
     */
    fun budget(): CodecResources.Budget? = runCatching { CodecResources.budgetOf(TimelineJson.spec(project)) }.getOrNull()
        ?.takeIf { it.count > CodecResources.MEASURED_PASSING_SOURCES }

    fun setExport(settings: ExportSettings) = edit { it.copy(export = settings) }

    /**
     * Submits the export to the task queue and answers the row key: the output goes to the Studio output folder (D37)
     * when one is set, and the bit rate policy of the settings fills an empty bit rate like in the wizards.
     * zh-CN: 把导出提交到任务队列并返回行键: 设置了 Studio 输出目录 (D37) 时输出到该目录, 码率留空时按设置的码率策略填写, 与向导相同.
     */
    fun export(): String {
        preview.pause()
        val context = getApplication<Application>()
        val prefs = StudioPreferences.of(context)
        var target = project
        if (target.export.bitRate == null && prefs.bitRatePolicy != StudioPreferences.BitRatePolicy.AUTO) {
            val (w, h) = TimelinePreview.frameSize(target)
            val codec = VideoCodec.entries.firstOrNull { it.id == target.export.codec } ?: VideoCodec.H264
            val estimate = (BitRates.estimateVideo(w, h, target.frameRate.toDouble(), codec) * prefs.bitRatePolicy.factor).roundToLong()
                .coerceIn(OperationOptions.MIN_VIDEO_BIT_RATE.toLong(), OperationOptions.MAX_VIDEO_BIT_RATE.toLong())
            target = target.copy(export = target.export.copy(bitRate = estimate))
        }
        if (target.export.output.isNullOrBlank()) {
            prefs.outputFolder?.let { folder ->
                val base = OutputPaths.defaultBaseName(null, "compose", System.currentTimeMillis(), TimeZone.getDefault())
                val path = OutputPaths.uniquePath(folder.trimEnd('/') + "/" + base + ".mp4") { File(it).exists() }
                target = target.copy(export = target.export.copy(output = path))
            }
        }
        return StudioTasks.submit(context, TimelineCalls.request(target))
    }

    internal companion object {
        const val UNDO_STEPS = 50
        const val MIN_ZOOM = 4.0
        const val MAX_ZOOM = 480.0

        /** A new project with the codec of the settings. zh-CN: 使用设置中编码格式的新工程. */
        fun blank(application: Application? = null): EditorProject {
            val codec = application?.let { StudioPreferences.of(it).codec.id } ?: VideoCodec.H264.id
            return EditorProject(export = ExportSettings(codec = codec))
        }

        fun contains(project: EditorProject, id: Long): Boolean = project.videoClip(id) != null || project.audioClip(id) != null ||
            project.overlays.any { it.id == id } || project.subtitles.cues.any { it.id == id } ||
            project.videoTracks.any { it.id == id } || project.audioTracks.any { it.id == id }

        fun cuesOf(path: String): List<Triple<Long, Long, String>> = SubtitleFiles.read(path).cues.map { cue ->
            Triple(cue.startMillis, cue.endMillis, SubtitleFiles.plainText(cue))
        }
    }

}

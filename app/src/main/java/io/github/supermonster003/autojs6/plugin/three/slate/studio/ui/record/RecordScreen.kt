package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.record

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.record.RecordCalls
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputPaths
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.AudioOutputFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.AudioRecordingStatus
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordSettings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordingAudioSource
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordingService
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordingStates
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordingStopReasons
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.StudioRecorder
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StudioPreferences
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.ChoiceRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox.CodeCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.TimeZone

/** Lets the permission callbacks continue the start flow defined after them. zh-CN: 让权限回调继续在其后定义的启动流程. */
private class StartFlow {
    var proceed: () -> Unit = {}
}

/**
 * The Studio recording page (roadmap P4.5, D41): source (microphone; playback and mixed on API 29+), format, sample
 * rate, channels, bit rate and max duration with the defaults of `audio.record`, a level meter and the recorded time,
 * pause / resume / stop. Starting asks in turn for the microphone, the notifications (once, API 33+), all files access
 * when the output needs it and the media projection for playback; a background restriction (appendix D Q13) shows the
 * battery guidance of the settings. The saved file opens in the library detail, goes to a toolbox wizard, and comes with
 * its equivalent `audio.record` script.
 * zh-CN: Studio 录音页 (路线图 P4.5, D41): 音源 (麦克风; API 29+ 另有内录与混合), 格式, 采样率, 声道, 码率与最长时长, 默认值同
 * `audio.record`; 电平表与已录时长, 暂停 / 继续 / 停止. 开始时依次申请麦克风, 通知 (API 33+, 只问一次), 输出需要时的所有文件访问,
 * 以及内录的媒体投影; 后台受限时 (附录 D Q13) 显示设置页的电池引导. 保存的文件可在媒体库详情中打开, 送进工具箱向导, 并附带等价的
 * `audio.record` 脚本.
 */
@Composable
internal fun RecordScreen(onBack: () -> Unit, onOpenDetail: (String) -> Unit, onProcess: (op: String, path: String) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val prefs = remember { RecordPreferences.of(context) }
    var settings by remember { mutableStateOf(prefs.settings.supportedOn(Build.VERSION.SDK_INT)) }
    val phase by StudioRecorder.phase.collectAsState()
    var status by remember { mutableStateOf<AudioRecordingStatus?>(null) }
    var level by remember { mutableFloatStateOf(0f) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val restricted = remember { isBackgroundRestricted(context) }
    val busy = phase is StudioRecorder.Phase.Starting || phase is StudioRecorder.Phase.Active || phase is StudioRecorder.Phase.Saving
    val projectionCancelled = stringResource(R.string.record_projection_cancelled)

    LaunchedEffect(phase) {
        while (phase is StudioRecorder.Phase.Active || phase is StudioRecorder.Phase.Saving) {
            status = StudioRecorder.status() ?: status
            level = StudioRecorder.level()
            delay(METER_INTERVAL_MILLIS)
        }
        level = 0f
    }

    fun update(next: RecordSettings) {
        settings = next
        prefs.settings = next
    }

    fun begin(grant: StudioRecorder.ProjectionGrant?) {
        val output = RecordSettings.output(
            storageRoot = sharedStorageRoot(),
            customFolder = StudioPreferences.of(context).outputFolder,
            format = settings.format,
            timestampMillis = System.currentTimeMillis(),
            zone = TimeZone.getDefault(),
        ) { File(it).exists() }
        status = null
        StudioRecorder.start(context, settings, output, grant)
    }

    val flow = remember { StartFlow() }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) flow.proceed() else dialog = DIALOG_MICROPHONE
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { flow.proceed() }
    val projection = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            begin(StudioRecorder.ProjectionGrant(result.resultCode, data))
        } else {
            scope.launch { snackbar.showSnackbar(projectionCancelled) }
        }
    }
    flow.proceed = proceed@{
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            microphone.launch(Manifest.permission.RECORD_AUDIO)
            return@proceed
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !prefs.notificationsAsked &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            prefs.notificationsAsked = true
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return@proceed
        }
        val folder = StudioPreferences.of(context).outputFolder ?: OutputPaths.defaultDirectory(sharedStorageRoot(), OutputKind.AUDIO)
        if (StorageAccess.lacksAccess(context, "$folder/probe.${settings.format.key}")) {
            dialog = DIALOG_STORAGE
            return@proceed
        }
        if (settings.source.usesPlayback) {
            val manager = context.getSystemService(MediaProjectionManager::class.java)
            if (manager == null) {
                scope.launch { snackbar.showSnackbar(projectionCancelled) }
            } else {
                runCatching { projection.launch(manager.createScreenCaptureIntent()) }
                    .onFailure { scope.launch { snackbar.showSnackbar(projectionCancelled) } }
            }
            return@proceed
        }
        begin(null)
    }

    SlateScreen(title = stringResource(R.string.record_title), onBack = onBack, snackbarHostState = snackbar) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("slate.record")) {
            Meter(phase, status, level, settings)
            Controls(
                phase = phase,
                paused = status?.state == RecordingStates.PAUSED,
                onStart = { flow.proceed() },
                onPause = { if (StudioRecorder.pause()) status = StudioRecorder.status() },
                onResume = { if (StudioRecorder.resume()) status = StudioRecorder.status() },
                onStop = { StudioRecorder.stop() },
            )
            if (restricted) {
                SettingsRow(
                    stringResource(R.string.record_background_title),
                    summary = stringResource(R.string.record_background_restricted),
                    summaryColor = MaterialTheme.colorScheme.error,
                    icon = R.drawable.ic_warning_24,
                    onClick = { dialog = DIALOG_BATTERY },
                    modifier = Modifier.testTag("slate.record.battery"),
                )
            }
            when (val current = phase) {
                is StudioRecorder.Phase.Finished -> SavedRecording(current, onOpenDetail = onOpenDetail, onSend = { dialog = DIALOG_SEND })
                is StudioRecorder.Phase.Failed -> Text(
                    stringResource(R.string.record_failed, current.message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).testTag("slate.record.error"),
                )
                else -> Unit
            }
            Options(settings, enabled = !busy, onChange = ::update, onOpen = { dialog = it })
            Spacer(Modifier.height(24.dp))
        }
    }

    val close = { dialog = null }
    when (dialog) {
        DIALOG_SAMPLE_RATE -> ChoiceDialog(
            title = stringResource(R.string.record_sample_rate),
            options = RecordSettings.SAMPLE_RATE_CHOICES.map { Choice(defaultMarked(Formats.sampleRate(it), it == settings.defaults.sampleRate)) },
            selectedIndex = RecordSettings.SAMPLE_RATE_CHOICES.indexOf(settings.track().sampleRate),
            onDismissRequest = close,
            onConfirm = { index -> close(); update(settings.copy(sampleRate = RecordSettings.SAMPLE_RATE_CHOICES[index])) },
            tag = "slate.dialog.sampleRate",
        )
        DIALOG_CHANNELS -> ChoiceDialog(
            title = stringResource(R.string.record_channels),
            options = RecordSettings.CHANNEL_CHOICES.map { Choice(defaultMarked(channelsLabel(it), it == settings.defaults.channels)) },
            selectedIndex = RecordSettings.CHANNEL_CHOICES.indexOf(settings.track().channels),
            onDismissRequest = close,
            onConfirm = { index -> close(); update(settings.copy(channels = RecordSettings.CHANNEL_CHOICES[index])) },
            tag = "slate.dialog.channels",
        )
        DIALOG_BIT_RATE -> ChoiceDialog(
            title = stringResource(R.string.record_bit_rate),
            options = RecordSettings.BIT_RATE_CHOICES.map { Choice(defaultMarked(Formats.bitRate(it.toLong()), it == settings.defaults.bitRate)) },
            selectedIndex = RecordSettings.BIT_RATE_CHOICES.indexOf(settings.track().bitRate),
            onDismissRequest = close,
            onConfirm = { index -> close(); update(settings.copy(bitRate = RecordSettings.BIT_RATE_CHOICES[index])) },
            tag = "slate.dialog.bitRate",
        )
        DIALOG_MAX_DURATION -> ChoiceDialog(
            title = stringResource(R.string.record_max_duration),
            options = RecordSettings.MAX_DURATION_CHOICES.map { Choice(durationLabel(it)) },
            selectedIndex = RecordSettings.MAX_DURATION_CHOICES.indexOf(settings.maxDurationMillis),
            onDismissRequest = close,
            onConfirm = { index -> close(); update(settings.copy(maxDurationMillis = RecordSettings.MAX_DURATION_CHOICES[index])) },
            tag = "slate.dialog.maxDuration",
        )
        DIALOG_MICROPHONE -> MessageDialog(
            title = stringResource(R.string.record_microphone_title),
            message = stringResource(R.string.record_microphone_message),
            onDismissRequest = close,
            onConfirm = { close(); openAppDetails(context) },
            confirmText = stringResource(R.string.settings_battery_open),
            tag = "slate.dialog.microphone",
        )
        DIALOG_STORAGE -> MessageDialog(
            title = stringResource(R.string.home_storage_title),
            message = stringResource(R.string.record_storage_message),
            onDismissRequest = close,
            onConfirm = { close(); activity?.let { StorageAccess.request(it, REQUEST_STORAGE) } },
            confirmText = stringResource(R.string.home_storage_grant),
            tag = "slate.dialog.storage",
        )
        DIALOG_BATTERY -> MessageDialog(
            title = stringResource(R.string.settings_battery),
            message = stringResource(R.string.settings_battery_message),
            onDismissRequest = close,
            onConfirm = { close(); openAppDetails(context) },
            confirmText = stringResource(R.string.settings_battery_open),
            tag = "slate.dialog.battery",
        )
        DIALOG_SEND -> {
            val finished = phase as? StudioRecorder.Phase.Finished
            if (finished != null) {
                SendToToolboxDialog(finished.result.path, MediaKind.AUDIO, onDismiss = close, onChoose = { op -> close(); onProcess(op, finished.result.path) })
            }
        }
    }
}

/** The recorded time, the state and the level meter. zh-CN: 已录时长, 状态与电平表. */
@Composable
private fun Meter(phase: StudioRecorder.Phase, status: AudioRecordingStatus?, level: Float, settings: RecordSettings) {
    val recorded = when (phase) {
        is StudioRecorder.Phase.Finished -> phase.result.durationMillis
        is StudioRecorder.Phase.Active, is StudioRecorder.Phase.Saving -> status?.recordedMillis ?: 0L
        else -> 0L
    }
    val stateLabel = when (phase) {
        is StudioRecorder.Phase.Starting -> stringResource(R.string.record_state_starting)
        is StudioRecorder.Phase.Active -> stringResource(if (status?.state == RecordingStates.PAUSED) R.string.record_state_paused else R.string.record_state_recording)
        is StudioRecorder.Phase.Saving -> stringResource(R.string.record_state_saving)
        is StudioRecorder.Phase.Finished -> stringResource(R.string.record_saved)
        else -> stringResource(R.string.record_state_idle)
    }
    val limit = (phase as? StudioRecorder.Phase.Active)?.settings?.maxDurationMillis ?: settings.maxDurationMillis
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            Formats.duration(recorded),
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
            modifier = Modifier.testTag("slate.record.time"),
        )
        val limitText = if (limit > 0) stringResource(R.string.record_limit, Formats.duration(limit)) else null
        Text(
            listOfNotNull(stateLabel, limitText).joinToString("  "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("slate.record.state"),
        )
        Spacer(Modifier.height(16.dp))
        val meterDescription = stringResource(R.string.record_level)
        LinearProgressIndicator(
            progress = { level.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = meterDescription }.testTag("slate.record.level"),
        )
    }
}

@Composable
private fun Controls(
    phase: StudioRecorder.Phase,
    paused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (phase) {
            is StudioRecorder.Phase.Starting, is StudioRecorder.Phase.Saving -> CircularProgressIndicator(Modifier.size(32.dp))
            is StudioRecorder.Phase.Active -> {
                OutlinedButton(onClick = if (paused) onResume else onPause, modifier = Modifier.testTag(if (paused) "slate.record.resume" else "slate.record.pause")) {
                    Icon(painterResource(if (paused) R.drawable.ic_mic_24 else R.drawable.ic_pause_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (paused) R.string.record_resume else R.string.record_pause))
                }
                Button(onClick = onStop, modifier = Modifier.testTag("slate.record.stop")) {
                    Icon(painterResource(R.drawable.ic_stop_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.record_stop))
                }
            }
            else -> Button(onClick = onStart, modifier = Modifier.testTag("slate.record.start")) {
                Icon(painterResource(R.drawable.ic_mic_24), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.record_start))
            }
        }
    }
}

/** The saved file, its actions and the equivalent script. zh-CN: 保存的文件, 其操作与等价脚本. */
@Composable
private fun SavedRecording(finished: StudioRecorder.Phase.Finished, onOpenDetail: (String) -> Unit, onSend: () -> Unit) {
    val context = LocalContext.current
    val result = finished.result
    SectionHeader(stringResource(R.string.record_saved))
    val summary = listOf(
        Formats.duration(result.durationMillis),
        Formats.size(context, result.sizeBytes),
        Formats.sampleRate(result.track.sampleRate),
        channelsLabel(result.track.channels),
    ).joinToString(", ")
    SettingsRow(
        Formats.isolate(File(result.path).name),
        summary = summary,
        icon = R.drawable.ic_music_note_24,
        onClick = { onOpenDetail(result.path) },
        modifier = Modifier.testTag("slate.record.result"),
    )
    reasonNote(result.reason)?.let { note ->
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).testTag("slate.record.reason"))
    }
    Hairline()
    SettingsRow(
        stringResource(R.string.record_send_to_toolbox),
        summary = stringResource(R.string.record_send_to_toolbox_summary),
        icon = R.drawable.ic_build_24,
        onClick = onSend,
        modifier = Modifier.testTag("slate.record.send"),
    )
    SectionHeader(stringResource(R.string.wizard_code))
    val explicit = remember(result.path) {
        result.path.takeUnless { File(it).parent == OutputPaths.defaultDirectory(sharedStorageRoot(), OutputKind.AUDIO) }
    }
    CodeCard(RecordCalls.code(finished.settings, explicit, result.durationMillis), copyable = true, tag = "slate.record.code")
}

/** Source, format and quality options. zh-CN: 音源, 格式与音质选项. */
@Composable
private fun Options(settings: RecordSettings, enabled: Boolean, onChange: (RecordSettings) -> Unit, onOpen: (String) -> Unit) {
    SectionHeader(stringResource(R.string.record_source))
    RecordSettings.sources(Build.VERSION.SDK_INT).forEach { source ->
        ChoiceRow(
            Choice(stringResource(RecordingService.sourceLabel(source)), note = sourceNote(source), enabled = enabled),
            selected = settings.source == source,
            onSelect = { onChange(settings.copy(source = source)) },
            modifier = Modifier.testTag("slate.record.source.${source.key}"),
        )
    }
    if (Build.VERSION.SDK_INT < RecordSettings.PLAYBACK_MIN_SDK) {
        Text(stringResource(R.string.record_playback_unavailable), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).testTag("slate.record.playbackUnavailable"))
    }
    SectionHeader(stringResource(R.string.record_format))
    AudioOutputFormat.entries.forEach { format ->
        ChoiceRow(
            Choice(formatLabel(format), enabled = enabled),
            selected = settings.format == format,
            onSelect = { onChange(settings.copy(format = format)) },
            modifier = Modifier.testTag("slate.record.format.${format.key}"),
        )
    }
    SectionHeader(stringResource(R.string.record_quality))
    val track = runCatching { settings.track() }.getOrNull() ?: settings.defaults
    SettingsRow(stringResource(R.string.record_sample_rate), summary = defaultMarked(Formats.sampleRate(track.sampleRate), track.sampleRate == settings.defaults.sampleRate),
        icon = R.drawable.ic_tune_24, enabled = enabled, onClick = { onOpen(DIALOG_SAMPLE_RATE) }, modifier = Modifier.testTag("slate.record.sampleRate"))
    Hairline()
    SettingsRow(stringResource(R.string.record_channels), summary = defaultMarked(channelsLabel(track.channels), track.channels == settings.defaults.channels),
        icon = R.drawable.ic_tune_24, enabled = enabled, onClick = { onOpen(DIALOG_CHANNELS) }, modifier = Modifier.testTag("slate.record.channels"))
    Hairline()
    if (settings.format == AudioOutputFormat.M4A) {
        SettingsRow(stringResource(R.string.record_bit_rate), summary = defaultMarked(Formats.bitRate(track.bitRate.toLong()), track.bitRate == settings.defaults.bitRate),
            icon = R.drawable.ic_tune_24, enabled = enabled, onClick = { onOpen(DIALOG_BIT_RATE) }, modifier = Modifier.testTag("slate.record.bitRate"))
    } else {
        SettingsRow(stringResource(R.string.record_bit_rate), summary = stringResource(R.string.record_bit_rate_wav), icon = R.drawable.ic_tune_24, enabled = false,
            modifier = Modifier.testTag("slate.record.bitRate"))
    }
    Hairline()
    SettingsRow(stringResource(R.string.record_max_duration), summary = durationLabel(settings.maxDurationMillis), icon = R.drawable.ic_schedule_24,
        enabled = enabled, onClick = { onOpen(DIALOG_MAX_DURATION) }, modifier = Modifier.testTag("slate.record.maxDuration"))
}

@Composable
private fun sourceNote(source: RecordingAudioSource): String? = when (source) {
    RecordingAudioSource.PLAYBACK -> stringResource(R.string.record_source_playback_note)
    RecordingAudioSource.MIXED -> stringResource(R.string.record_source_mixed_note)
    else -> null
}

@Composable
private fun formatLabel(format: AudioOutputFormat): String = when (format) {
    AudioOutputFormat.M4A -> stringResource(R.string.record_format_m4a)
    AudioOutputFormat.WAV -> stringResource(R.string.record_format_wav)
}

@Composable
private fun channelsLabel(channels: Int): String =
    stringResource(if (channels == 1) R.string.record_channels_mono else R.string.record_channels_stereo)

@Composable
private fun defaultMarked(label: String, isDefault: Boolean): String = if (isDefault) stringResource(R.string.record_default_value, label) else label

@Composable
private fun durationLabel(millis: Long): String =
    if (millis <= 0) stringResource(R.string.record_max_duration_none) else (millis / 60_000).toInt().let { pluralStringResource(R.plurals.record_minutes, it, it) }

@Composable
private fun reasonNote(reason: String): String? = when (reason) {
    RecordingStopReasons.MAX_DURATION -> stringResource(R.string.record_reason_max_duration)
    RecordingStopReasons.MAX_SIZE -> stringResource(R.string.record_reason_max_size)
    else -> null
}

/** `/sdcard` as the media folders use it. zh-CN: 媒体目录所用的 `/sdcard`. */
@Suppress("DEPRECATION")
private fun sharedStorageRoot(): String = Environment.getExternalStorageDirectory().path

private fun isBackgroundRestricted(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true

private fun openAppDetails(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }
}

private const val METER_INTERVAL_MILLIS = 100L
private const val REQUEST_STORAGE = 7

private const val DIALOG_SAMPLE_RATE = "sampleRate"
private const val DIALOG_CHANNELS = "channels"
private const val DIALOG_BIT_RATE = "bitRate"
private const val DIALOG_MAX_DURATION = "maxDuration"
private const val DIALOG_MICROPHONE = "microphone"
private const val DIALOG_STORAGE = "storage"
private const val DIALOG_BATTERY = "battery"
private const val DIALOG_SEND = "send"

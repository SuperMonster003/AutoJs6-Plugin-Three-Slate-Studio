package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.settings

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioApplication
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.CodecSupport
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.EncoderPreference
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.ProcessingSettings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.MediaStudioEngine
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StudioPreferences
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StudioPreferences.BitRatePolicy
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.LauncherIconMode
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance.ThemeColorValue
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.CheckRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.MultiChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SwitchRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.ThemeColorDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaLibrary
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update.UpdateDialogs
import java.util.Locale

/**
 * The settings screen (roadmap P4.1, D37, standalone settings specification, precedent 3-Prism Camera): appearance
 * (language, night mode, theme color, launcher icon; the first three follow AutoJs6 by default), media and output (the
 * output folder, default video codec and bit rate policy of the toolbox wizards, the extra folders of the home screen;
 * scripts keep the defaults of roadmap D14 / D29), permissions and background (all files access of D9, notifications,
 * unrestricted battery use of appendix D Q13, the task notification channel) and about (version, release history, the
 * update check with its automatic check and ignored releases). Every choice dialog edits a draft and saves on confirm.
 * The hardware encoder preference and the task concurrency of D37 (appendix D Q50 (b), `core/ProcessingSettings`) apply to
 * Studio and script tasks alike, since both run on the executor of this process.
 * zh-CN: 设置页 (路线图 P4.1, D37, 独立设置规范, 先例 3-Prism Camera): 外观 (语言, 夜间模式, 主题色, 启动器图标; 前三项默认跟随 AutoJs6),
 * 媒体与输出 (工具箱向导的输出目录, 默认视频编码与码率策略, 首页的附加目录; 脚本仍用路线图 D14 / D29 的默认值), 权限与后台 (D9 的所有文件
 * 访问, 通知, 附录 D Q13 的电池不受限制, 任务通知渠道) 与关于 (版本, 发行历史, 更新检查及其自动检查与已忽略版本). 每个选择对话框编辑草稿,
 * 确定时保存. D37 的硬件编码器偏好与任务并发数 (附录 D Q50 (b), `core/ProcessingSettings`) 对 Studio 任务与脚本任务同样生效, 因为两者都在本进程的
 * 执行器上运行.
 */
class SettingsActivity : StandaloneActivity() {

    private enum class Dialog { LANGUAGE, DARK_MODE, THEME_COLOR, LAUNCHER_ICON, OUTPUT_FOLDER, CODEC, BIT_RATE, ENCODER, CONCURRENCY, FOLDERS, BATTERY, IGNORED }

    private val snackbar = SnackbarHostState()
    private var revision by mutableIntStateOf(0)
    private var pendingMessage by mutableStateOf<String?>(null)
    private var dialog by mutableStateOf<Dialog?>(null)
    private var storageGranted by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(true)
    private var battery by mutableStateOf(Battery.OPTIMIZED)
    private lateinit var prefs: StudioPreferences
    private lateinit var processing: ProcessingSettings
    private lateinit var updates: AppUpdateController

    /** A denial without rationale means "don't ask again": the next tap opens the app settings. zh-CN: 无理由的拒绝即 "不再询问": 下次点按打开应用设置. */
    private var notificationsBlocked = false

    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsGranted = granted
        notificationsBlocked = !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** The output folder picked with the system picker. zh-CN: 用系统选择器选择的输出目录. */
    private val outputFolderRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val path = uri?.let(MediaLibrary::treePathOf) ?: return@registerForActivityResult run { if (uri != null) pendingMessage = getString(R.string.settings_folder_unsupported) }
        prefs.outputFolder = path
        revision++
    }

    /** A folder added to the home screen: it must lie in the shared storage. zh-CN: 添加到首页的目录: 必须位于共享存储中. */
    private val libraryFolderRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        val root = MediaLibrary.storageRoot.trimEnd('/')
        val path = MediaLibrary.treePathOf(uri)
        val relative = path?.takeIf { it.startsWith("$root/") }?.removePrefix("$root/")
        if (relative.isNullOrEmpty()) {
            pendingMessage = getString(R.string.settings_folder_unsupported)
        } else {
            prefs.libraryFolders = prefs.libraryFolders + relative
            revision++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = StudioPreferences.of(this)
        processing = ProcessingSettings.of(this)
        updates = AppUpdateController(AppUpdateStore.of(this), BuildConfig.VERSION_CODE.toLong())
    }

    override fun onResume() {
        super.onResume()
        // Back from the system pages: read the permissions and the battery state again.
        // zh-CN: 从系统页面返回: 重新读取权限与电池状态.
        storageGranted = StorageAccess.isGranted(this)
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        battery = Battery.read(this)
    }

    override fun onDestroy() {
        if (isFinishing) updates.cancel()
        super.onDestroy()
    }

    override fun holdsAppearance(): Boolean = dialog != null || updates.state != null

    @Composable
    override fun Content() {
        val message = pendingMessage
        LaunchedEffect(message) {
            if (message != null) {
                snackbar.showSnackbar(message)
                pendingMessage = null
            }
        }
        val appearancePreferences = remember(revision) { AppearancePreferences.read(this) }
        SlateScreen(title = stringResource(R.string.settings_title), onBack = ::finish, snackbarHostState = snackbar) { padding ->
            // A saved choice bumps the revision and rebuilds the rows from the stored values. zh-CN: 保存后修订号增加, 各行按存储值重建.
            key(revision) {
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("slate.settings")) {
                    SectionHeader(stringResource(R.string.settings_appearance))
                    SettingsRow(stringResource(R.string.settings_language), summary = languageSummary(appearancePreferences.language), icon = R.drawable.ic_language_24,
                        onClick = { dialog = Dialog.LANGUAGE }, modifier = Modifier.testTag("slate.settings.language"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_night_mode), summary = darkModeSummary(appearancePreferences.darkMode), icon = R.drawable.ic_dark_mode_24,
                        onClick = { dialog = Dialog.DARK_MODE }, modifier = Modifier.testTag("slate.settings.night"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_theme_color), summary = colorSummary(appearancePreferences.color), icon = R.drawable.ic_palette_24,
                        onClick = { dialog = Dialog.THEME_COLOR }, modifier = Modifier.testTag("slate.settings.color"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_launcher_icon), summary = stringResource(iconLabel(LauncherIcons.current(this@SettingsActivity))), icon = R.drawable.ic_apps_24,
                        onClick = { dialog = Dialog.LAUNCHER_ICON }, modifier = Modifier.testTag("slate.settings.icon"))

                    SectionHeader(stringResource(R.string.settings_media))
                    SettingsRow(stringResource(R.string.settings_output_folder), summary = prefs.outputFolder ?: stringResource(R.string.settings_output_folder_default),
                        icon = R.drawable.ic_folder_24, onClick = { dialog = Dialog.OUTPUT_FOLDER }, modifier = Modifier.testTag("slate.settings.outputFolder"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_codec), summary = codecLabel(prefs.codec), icon = R.drawable.ic_movie_24,
                        onClick = { dialog = Dialog.CODEC }, modifier = Modifier.testTag("slate.settings.codec"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_bit_rate), summary = stringResource(bitRateLabel(prefs.bitRatePolicy)), icon = R.drawable.ic_tune_24,
                        onClick = { dialog = Dialog.BIT_RATE }, modifier = Modifier.testTag("slate.settings.bitRate"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_encoder), summary = stringResource(encoderLabel(processing.encoderPreference)), icon = R.drawable.ic_build_24,
                        onClick = { dialog = Dialog.ENCODER }, modifier = Modifier.testTag("slate.settings.encoder"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_concurrency), summary = stringResource(concurrencyLabel(processing.concurrency)),
                        icon = R.drawable.ic_list_24, onClick = { dialog = Dialog.CONCURRENCY }, modifier = Modifier.testTag("slate.settings.concurrency"))
                    Hairline()
                    val folders = prefs.libraryFolders
                    SettingsRow(stringResource(R.string.settings_library_folders),
                        summary = if (folders.isEmpty()) stringResource(R.string.settings_library_fixed_only) else folders.joinToString(", "),
                        icon = R.drawable.ic_storage_24, onClick = { dialog = Dialog.FOLDERS }, modifier = Modifier.testTag("slate.settings.folders"))

                    SectionHeader(stringResource(R.string.settings_permissions))
                    SettingsRow(stringResource(R.string.settings_storage_permission),
                        summary = stringResource(if (storageGranted) R.string.settings_permission_granted else R.string.settings_storage_missing),
                        summaryColor = if (storageGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        icon = R.drawable.ic_storage_24, showChevron = !storageGranted,
                        onClick = if (storageGranted) null else { { StorageAccess.request(this@SettingsActivity, REQUEST_STORAGE) } },
                        modifier = Modifier.testTag("slate.settings.storagePermission"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Hairline()
                        SettingsRow(stringResource(R.string.settings_notification_permission),
                            summary = stringResource(if (notificationsGranted) R.string.settings_permission_granted else R.string.settings_notification_missing),
                            summaryColor = if (notificationsGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            icon = R.drawable.ic_notifications_24, showChevron = !notificationsGranted,
                            onClick = if (notificationsGranted) null else ::askNotifications, modifier = Modifier.testTag("slate.settings.notificationPermission"))
                    }
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_battery), summary = stringResource(battery.label),
                        summaryColor = if (battery == Battery.RESTRICTED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = R.drawable.ic_battery_24, onClick = { dialog = Dialog.BATTERY }, modifier = Modifier.testTag("slate.settings.battery"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_task_notification), summary = stringResource(R.string.settings_task_notification_summary),
                        icon = R.drawable.ic_notifications_24, onClick = ::openNotificationSettings, modifier = Modifier.testTag("slate.settings.taskNotification"))

                    SectionHeader(stringResource(R.string.settings_about))
                    SettingsRow(stringResource(R.string.settings_version),
                        summary = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, stringResource(R.string.plugin_version_date)),
                        icon = R.drawable.ic_info_24, showChevron = false, modifier = Modifier.testTag("slate.settings.version"))
                    Hairline()
                    SettingsRow(stringResource(R.string.release_history_title), summary = stringResource(R.string.settings_release_history_summary), icon = R.drawable.ic_history_24,
                        onClick = { startActivity(Intent(this@SettingsActivity, ReleaseHistoryActivity::class.java)) }, modifier = Modifier.testTag("slate.settings.releaseHistory"))
                    Hairline()
                    SettingsRow(stringResource(R.string.update_check), summary = stringResource(R.string.settings_update_summary), icon = R.drawable.ic_update_24,
                        onClick = updates::checkManually, modifier = Modifier.testTag("slate.settings.update"))
                    Hairline()
                    val store = remember { AppUpdateStore.of(this@SettingsActivity) }
                    var automatic by remember { mutableStateOf(store.automaticChecks) }
                    SwitchRow(stringResource(R.string.update_automatic), automatic, { automatic = it; store.automaticChecks = it },
                        summary = stringResource(R.string.update_automatic_summary), icon = R.drawable.ic_notifications_24, modifier = Modifier.testTag("slate.settings.autoUpdate"))
                    Hairline()
                    val ignored = store.ignoredTags.size
                    SettingsRow(stringResource(R.string.update_ignored), summary = ignoredSummary(ignored), icon = R.drawable.ic_close_24,
                        enabled = ignored > 0, onClick = { dialog = Dialog.IGNORED }, modifier = Modifier.testTag("slate.settings.ignored"))
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        Dialogs()
        UpdateDialogs(updates, onOpenRelease = { url -> open(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            onReleaseHistory = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) })
    }

    @Composable
    private fun ignoredSummary(count: Int): String =
        if (count == 0) stringResource(R.string.update_ignored_none) else pluralStringResource(R.plurals.update_ignored_count, count, count)

    @Composable
    private fun Dialogs() {
        val close = { dialog = null }
        when (dialog) {
            null -> Unit
            Dialog.LANGUAGE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_language),
                    options = AppearancePreferences.LANGUAGES.map { Choice(languageLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.LANGUAGES.indexOf(current.language).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(language = AppearancePreferences.LANGUAGES[index])) },
                    tag = "slate.dialog.language",
                )
            }
            Dialog.DARK_MODE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_night_mode),
                    options = AppearancePreferences.DARK_MODES.map { Choice(darkModeLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.DARK_MODES.indexOf(current.darkMode).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(darkMode = AppearancePreferences.DARK_MODES[index])) },
                    tag = "slate.dialog.night",
                )
            }
            Dialog.THEME_COLOR -> {
                val current = AppearancePreferences.read(this)
                ThemeColorDialog(
                    currentColor = current.color,
                    hostColor = HostAppearanceReader.cached()?.primary ?: AppearancePreferences.DEFAULT_COLOR,
                    onDismissRequest = close,
                    onConfirm = { color -> close(); saveAppearance(current.copy(color = color)) },
                )
            }
            Dialog.LAUNCHER_ICON -> ChoiceDialog(
                title = stringResource(R.string.settings_launcher_icon),
                options = LauncherIconMode.entries.map { mode ->
                    Choice(stringResource(iconLabel(mode)), when (mode) {
                        LauncherIconMode.AUTO -> stringResource(R.string.launcher_icon_auto_note)
                        LauncherIconMode.TRANSPARENT -> stringResource(R.string.launcher_icon_transparent_note)
                        else -> null
                    })
                },
                selectedIndex = LauncherIcons.current(this).ordinal,
                onDismissRequest = close,
                onConfirm = { index ->
                    close()
                    val result = runCatching { LauncherIcons.select(this, LauncherIconMode.entries[index]) }
                    revision++
                    pendingMessage = getString(if (result.isSuccess) R.string.launcher_icon_applied_note else R.string.launcher_icon_failed)
                },
                tag = "slate.dialog.icon",
            )
            Dialog.OUTPUT_FOLDER -> ChoiceDialog(
                title = stringResource(R.string.settings_output_folder),
                message = stringResource(R.string.settings_output_folder_message),
                options = listOf(
                    Choice(stringResource(R.string.settings_output_folder_reset), stringResource(R.string.settings_output_folder_default)),
                    Choice(stringResource(R.string.settings_output_folder_choose), prefs.outputFolder),
                ),
                selectedIndex = if (prefs.outputFolder == null) 0 else 1,
                onDismissRequest = close,
                onConfirm = { index ->
                    close()
                    if (index == 0) {
                        prefs.outputFolder = null
                        revision++
                    } else {
                        launchTree(outputFolderRequest)
                    }
                },
                tag = "slate.dialog.outputFolder",
            )
            Dialog.CODEC -> {
                val available = remember { CodecSupport.videoEncoders() }
                ChoiceDialog(
                    title = stringResource(R.string.settings_codec),
                    message = stringResource(R.string.settings_wizard_only_message),
                    options = VideoCodec.entries.map { codec ->
                        val supported = codec.id in available
                        Choice(codecLabel(codec), if (supported) null else stringResource(R.string.settings_codec_unavailable), enabled = supported)
                    },
                    selectedIndex = prefs.codec.ordinal,
                    onDismissRequest = close,
                    onConfirm = { index -> close(); prefs.codec = VideoCodec.entries[index]; revision++ },
                    tag = "slate.dialog.codec",
                )
            }
            Dialog.BIT_RATE -> ChoiceDialog(
                title = stringResource(R.string.settings_bit_rate),
                message = stringResource(R.string.settings_bit_rate_message),
                options = BitRatePolicy.entries.map { Choice(stringResource(bitRateLabel(it)), stringResource(bitRateNote(it))) },
                selectedIndex = prefs.bitRatePolicy.ordinal,
                onDismissRequest = close,
                onConfirm = { index -> close(); prefs.bitRatePolicy = BitRatePolicy.entries[index]; revision++ },
                tag = "slate.dialog.bitRate",
            )
            Dialog.ENCODER -> ChoiceDialog(
                title = stringResource(R.string.settings_encoder),
                message = stringResource(R.string.settings_encoder_message),
                options = EncoderPreference.entries.map { Choice(stringResource(encoderLabel(it)), stringResource(encoderNote(it))) },
                selectedIndex = processing.encoderPreference.ordinal,
                onDismissRequest = close,
                onConfirm = { index -> close(); processing.encoderPreference = EncoderPreference.entries[index]; revision++ },
                tag = "slate.dialog.encoder",
            )
            Dialog.CONCURRENCY -> {
                val counts = (ProcessingSettings.MIN_CONCURRENCY..ProcessingSettings.MAX_CONCURRENCY).toList()
                ChoiceDialog(
                    title = stringResource(R.string.settings_concurrency),
                    message = stringResource(R.string.settings_concurrency_message),
                    options = counts.map { count -> Choice(stringResource(concurrencyLabel(count)), stringResource(concurrencyNote(count))) },
                    selectedIndex = counts.indexOf(processing.concurrency),
                    onDismissRequest = close,
                    onConfirm = { index ->
                        close()
                        processing.concurrency = counts[index]
                        // A raised limit starts queued tasks now. zh-CN: 调高上限后立即启动排队中的任务.
                        MediaStudioEngine.peek()?.runner?.limitsChanged()
                        revision++
                    },
                    tag = "slate.dialog.concurrency",
                )
            }
            Dialog.FOLDERS -> FoldersDialog(close)
            Dialog.BATTERY -> MessageDialog(
                title = stringResource(R.string.settings_battery),
                message = stringResource(R.string.settings_battery_message),
                onDismissRequest = close,
                onConfirm = { close(); openAppSettings() },
                confirmText = stringResource(R.string.settings_battery_open),
                tag = "slate.dialog.battery",
            )
            Dialog.IGNORED -> {
                val store = AppUpdateStore.of(this)
                val tags = store.ignoredTags.sorted()
                MultiChoiceDialog(
                    title = stringResource(R.string.update_ignored),
                    message = stringResource(R.string.update_ignored_message),
                    options = tags.map { Choice(it) },
                    selected = emptySet(),
                    onDismissRequest = close,
                    onConfirm = { chosen -> close(); store.stopIgnoring(chosen.map(tags::get)); revision++ },
                    confirmText = stringResource(R.string.update_stop_ignoring),
                    requireOne = true,
                    tag = "slate.dialog.ignored",
                )
            }
        }
    }

    /**
     * The folders of the home screen: the fixed ones (read only) and the added ones, which can be ticked and removed;
     * the neutral button adds one with the system picker.
     * zh-CN: 首页的目录: 固定目录 (只读) 与添加的目录 (可勾选移除); 中性按钮用系统选择器添加一个.
     */
    @Composable
    private fun FoldersDialog(close: () -> Unit) {
        val added = prefs.libraryFolders
        var ticked by rememberSaveable { mutableStateOf(emptyList<String>()) }
        SlateDialog(
            title = stringResource(R.string.settings_library_folders),
            onDismissRequest = close,
            confirmText = stringResource(R.string.settings_library_remove),
            onConfirm = {
                close()
                prefs.libraryFolders = added - ticked.toSet()
                revision++
            },
            confirmEnabled = ticked.isNotEmpty(),
            dismissText = stringResource(R.string.ui_dialog_close),
            neutralText = stringResource(R.string.settings_library_add),
            onNeutral = { close(); launchTree(libraryFolderRequest) },
            tag = "slate.dialog.folders",
        ) {
            val textModifier = Modifier.padding(horizontal = DialogDefaults.contentPadding, vertical = 4.dp)
            Text(stringResource(R.string.settings_library_message), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = textModifier)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.settings_library_fixed), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = textModifier)
            MediaLibrary.FIXED_FOLDERS.forEach { folder ->
                Text(folder, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), modifier = textModifier)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.settings_library_added), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = textModifier)
            if (added.isEmpty()) {
                Text(stringResource(R.string.settings_library_none), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = textModifier)
            }
            added.forEach { folder ->
                CheckRow(Choice(folder), folder in ticked, { checked -> ticked = if (checked) ticked + folder else ticked - folder },
                    Modifier.testTag("slate.dialog.folders.$folder"))
            }
        }
    }

    // Actions -------------------------------------------------------------------------------------------------------

    private fun saveAppearance(next: AppearancePreferences) {
        if (next == AppearancePreferences.read(this)) return
        if (next.save(this)) {
            revision++
            applyAppearanceChange()
        } else {
            pendingMessage = getString(R.string.settings_save_failed)
        }
    }

    private fun launchTree(launcher: androidx.activity.result.ActivityResultLauncher<Uri?>) {
        runCatching { launcher.launch(null) }.onFailure {
            Log.w(TAG, "No folder picker", it)
            pendingMessage = getString(R.string.settings_open_failed)
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (notificationsBlocked) openAppSettings() else notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openAppSettings() {
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    /** The settings of the task progress channel (API 26+), else the app details. zh-CN: 任务进度通知渠道的设置 (API 26+), 否则为应用详情. */
    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, ThreeSlateStudioApplication.CHANNEL_TASKS))
        } else {
            openAppSettings()
        }
    }

    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure {
            Log.w(TAG, "Cannot open ${intent.action}", it)
            pendingMessage = getString(R.string.settings_open_failed)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE) storageGranted = StorageAccess.isGranted(this)
    }

    // Summaries and labels --------------------------------------------------------------------------------------

    @Composable
    private fun followNote(isFollow: Boolean): String? =
        if (isFollow && HostAppearanceReader.cached() == null) stringResource(R.string.settings_follow_autojs6_unavailable) else null

    @Composable
    private fun languageSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6_summary, languageLabel(appearance.language))
        else -> languageLabel(value)
    }

    @Composable
    private fun darkModeSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(
            R.string.settings_follow_autojs6_summary,
            stringResource(if (appearance.dark) R.string.settings_dark_mode_dark else R.string.settings_dark_mode_light),
        )
        else -> darkModeLabel(value)
    }

    @Composable
    private fun colorSummary(color: Int?): String =
        if (color == null) stringResource(R.string.settings_follow_autojs6_summary, ThemeColorValue.hex(appearance.primary)) else ThemeColorValue.hex(color)

    @Composable
    private fun languageLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6)
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        "zh-Hans" -> stringResource(R.string.settings_language_zh_hans)
        "zh-Hant-HK" -> stringResource(R.string.settings_language_zh_hant_hk)
        "zh-Hant-TW" -> stringResource(R.string.settings_language_zh_hant_tw)
        "en" -> stringResource(R.string.settings_language_en)
        "fr" -> stringResource(R.string.settings_language_fr)
        "es" -> stringResource(R.string.settings_language_es)
        "ja" -> stringResource(R.string.settings_language_ja)
        "ko" -> stringResource(R.string.settings_language_ko)
        "ru" -> stringResource(R.string.settings_language_ru)
        "ar" -> stringResource(R.string.settings_language_ar)
        else -> Locale.forLanguageTag(value).getDisplayName(Locale.forLanguageTag(value)).ifBlank { value }
    }

    @Composable
    private fun darkModeLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        AppearancePreferences.LIGHT -> stringResource(R.string.settings_dark_mode_light)
        AppearancePreferences.DARK -> stringResource(R.string.settings_dark_mode_dark)
        else -> stringResource(R.string.settings_follow_autojs6)
    }

    private fun codecLabel(codec: VideoCodec): String = when (codec) {
        VideoCodec.H264 -> "H.264"
        VideoCodec.H265 -> "H.265"
    }

    @StringRes
    private fun bitRateLabel(policy: BitRatePolicy): Int = when (policy) {
        BitRatePolicy.AUTO -> R.string.settings_bit_rate_auto
        BitRatePolicy.HIGH -> R.string.settings_bit_rate_high
        BitRatePolicy.SMALL -> R.string.settings_bit_rate_small
    }

    @StringRes
    private fun concurrencyLabel(count: Int): Int = if (count <= 1) R.string.settings_concurrency_one else R.string.settings_concurrency_two

    @StringRes
    private fun concurrencyNote(count: Int): Int = if (count <= 1) R.string.settings_concurrency_one_note else R.string.settings_concurrency_two_note

    @StringRes
    private fun encoderLabel(preference: EncoderPreference): Int = when (preference) {
        EncoderPreference.AUTO -> R.string.settings_encoder_auto
        EncoderPreference.HARDWARE -> R.string.settings_encoder_hardware
        EncoderPreference.SOFTWARE -> R.string.settings_encoder_software
    }

    @StringRes
    private fun encoderNote(preference: EncoderPreference): Int = when (preference) {
        EncoderPreference.AUTO -> R.string.settings_encoder_auto_note
        EncoderPreference.HARDWARE -> R.string.settings_encoder_hardware_note
        EncoderPreference.SOFTWARE -> R.string.settings_encoder_software_note
    }

    @StringRes
    private fun bitRateNote(policy: BitRatePolicy): Int = when (policy) {
        BitRatePolicy.AUTO -> R.string.settings_bit_rate_auto_note
        BitRatePolicy.HIGH -> R.string.settings_bit_rate_high_note
        BitRatePolicy.SMALL -> R.string.settings_bit_rate_small_note
    }

    @StringRes
    private fun iconLabel(mode: LauncherIconMode): Int = when (mode) {
        LauncherIconMode.LIGHT -> R.string.launcher_icon_light
        LauncherIconMode.DARK -> R.string.launcher_icon_dark
        LauncherIconMode.AUTO -> R.string.launcher_icon_auto
        LauncherIconMode.TRANSPARENT -> R.string.launcher_icon_transparent
    }

    /**
     * How the system treats the plugin in the background (appendix D Q13): restricted (API 28+ background restriction,
     * HyperOS default), optimized (Doze may defer it) or unrestricted.
     * zh-CN: 系统在后台如何对待插件 (附录 D Q13): 受限 (API 28+ 后台限制, HyperOS 默认), 已优化 (Doze 可能推迟) 或不受限制.
     */
    private enum class Battery(@param:StringRes val label: Int) {
        UNRESTRICTED(R.string.settings_battery_unrestricted),
        OPTIMIZED(R.string.settings_battery_optimized),
        RESTRICTED(R.string.settings_battery_restricted),
        ;

        companion object {
            fun read(activity: SettingsActivity): Battery {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    activity.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
                ) return RESTRICTED
                val power = activity.getSystemService(PowerManager::class.java)
                return if (power?.isIgnoringBatteryOptimizations(activity.packageName) == true) UNRESTRICTED else OPTIMIZED
            }
        }
    }

    private companion object {
        const val TAG = "SlateSettings"
        const val REQUEST_STORAGE = 1
    }

}

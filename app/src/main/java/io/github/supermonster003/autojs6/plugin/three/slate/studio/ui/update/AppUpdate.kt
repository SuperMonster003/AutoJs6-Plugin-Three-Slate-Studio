package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.supermonster003.autojs6.plugin.three.slate.studio.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.CheckRow
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.MessageDialog
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The newest release of the plugin in the official plugin index (appendix D Q45 = (b)). [tag] is "v" + [versionName], the
 * key of "ignore this version".
 * zh-CN: 官方插件索引中插件的最新发行版 (附录 D Q45 = (b)). [tag] 为 "v" + [versionName], 用作 "忽略此版本" 的键.
 */
internal data class AppRelease(val versionCode: Long, val versionName: String, val notes: String, val pageUrl: String) {
    val tag: String get() = "v$versionName"
    val name: String get() = "3-Slate Studio $versionName"
}

/** Compares version codes, the order the host and the official index use. Pure Kotlin. zh-CN: 按 versionCode 比较, 与宿主及官方索引一致. 纯 Kotlin. */
internal object UpdateVersionPolicy {
    fun isNewer(candidate: AppRelease, installedVersionCode: Long): Boolean = candidate.versionCode > installedVersionCode
}

/**
 * Update preferences (3-Stone AI rules): automatic checks on by default and at most once every 12 hours, ignored tags.
 * [clock] is injectable for the JVM tests.
 * zh-CN: 更新偏好 (沿用 3-Stone AI): 自动检查默认开启且每 12 小时至多一次, 已忽略的版本. [clock] 可注入, 供 JVM 测试使用.
 */
internal class AppUpdateStore(private val storage: Storage, private val clock: () -> Long = System::currentTimeMillis) {

    /** The persistence behind the store (SharedPreferences in the app). zh-CN: 存储后端 (应用中为 SharedPreferences). */
    interface Storage {
        fun getBoolean(key: String, default: Boolean): Boolean
        fun putBoolean(key: String, value: Boolean)
        fun getLong(key: String, default: Long): Long
        fun putLong(key: String, value: Long)
        fun getStrings(key: String): Set<String>
        fun putStrings(key: String, value: Set<String>)
    }

    var automaticChecks: Boolean
        get() = storage.getBoolean(KEY_AUTOMATIC, true)
        set(value) = storage.putBoolean(KEY_AUTOMATIC, value)

    val ignoredTags: Set<String> get() = storage.getStrings(KEY_IGNORED)

    fun ignore(tag: String) = storage.putStrings(KEY_IGNORED, ignoredTags + tag)

    fun stopIgnoring(tags: Collection<String>) = storage.putStrings(KEY_IGNORED, ignoredTags - tags.toSet())

    fun shouldCheckAutomatically(): Boolean {
        if (!automaticChecks) return false
        val last = storage.getLong(KEY_LAST_ATTEMPT, 0L)
        val now = clock()
        // A clock set back counts as due as well. zh-CN: 时钟被调回时同样视为到期.
        return last <= 0L || now < last || now - last >= INTERVAL_MILLIS
    }

    fun recordAutomaticAttempt() = storage.putLong(KEY_LAST_ATTEMPT, clock())

    companion object {
        val INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(12)
        /** The preferences file of the update check. zh-CN: 更新检查的偏好文件. */
        internal const val FILE = "app-update-settings"
        internal const val KEY_AUTOMATIC = "automatic-checks"
        private const val KEY_IGNORED = "ignored-tags"
        private const val KEY_LAST_ATTEMPT = "last-automatic-attempt"

        fun of(context: Context): AppUpdateStore {
            val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            return AppUpdateStore(object : Storage {
                override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
                override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
                override fun getLong(key: String, default: Long) = preferences.getLong(key, default)
                override fun putLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()
                override fun getStrings(key: String): Set<String> = preferences.getStringSet(key, emptySet()).orEmpty().toSet()
                override fun putStrings(key: String, value: Set<String>) = preferences.edit().putStringSet(key, value).apply()
            })
        }
    }
}

/** What a check found. zh-CN: 检查结果. */
internal sealed interface UpdateResult {
    data class Release(val release: AppRelease) : UpdateResult

    /** The official index lists no published release of the plugin yet. zh-CN: 官方索引尚未登记插件的已发布版本. */
    data object NoRelease : UpdateResult
}

/**
 * Reads the official plugin index (appendix D Q45 = (b)): `plugins.official.generated.json` of
 * SuperMonster003/AutoJs6-Official-Plugins-Index, the file the host's plugin center reads, from raw.githubusercontent.com
 * and then the two jsDelivr mirrors the host falls back to. Limits of 3-Stone AI: 15 s connect, 20 s read, no redirects,
 * at most 6000 characters of notes; 45 s in total across the hosts and at most 8 MiB per response (the index is about
 * 0.9 MiB with every official plugin). [cancel] aborts the connection.
 * zh-CN: 读取官方插件索引 (附录 D Q45 = (b)): SuperMonster003/AutoJs6-Official-Plugins-Index 的 `plugins.official.generated.json`, 即宿主插件中心
 * 读取的文件, 先访问 raw.githubusercontent.com, 失败时依次回退到宿主同样使用的两个 jsDelivr 镜像. 沿用 3-Stone AI 的限制: 连接 15 秒, 读取 20 秒,
 * 不跟随重定向, 说明至多 6000 字符; 全部主机合计 45 秒, 每个响应至多 8 MiB (含全部官方插件的索引约 0.9 MiB). [cancel] 中止连接.
 */
internal class AppUpdateChecker(private val indexUrls: List<String> = INDEX_URLS) {

    inner class Call internal constructor() {
        private val cancelled = AtomicBoolean(false)
        @Volatile var connection: HttpURLConnection? = null

        val isCancelled: Boolean get() = cancelled.get()

        /** The total time limit closed the connection (a failure, not a user cancel). zh-CN: 总时限关闭了连接 (属于失败, 不是用户取消). */
        @Volatile var timedOut = false

        fun cancel() {
            cancelled.set(true)
            runCatching { connection?.disconnect() }
        }
    }

    fun check(callback: (Call, Result<UpdateResult>) -> Unit): Call {
        val call = Call()
        worker.execute {
            // The total limit: the connection is closed when the check takes longer. zh-CN: 总时限: 超时即关闭连接.
            val timeout = Runnable {
                call.timedOut = true
                call.cancel()
            }
            main.postDelayed(timeout, TOTAL_TIMEOUT_MILLIS)
            val result = runCatching { decode(fetchFirst(call), PACKAGE_NAME) }
            main.removeCallbacks(timeout)
            callback(call, result)
        }
        return call
    }

    /** Tries the hosts in order; the first complete response wins. zh-CN: 依次尝试各主机, 取第一个完整响应. */
    private fun fetchFirst(call: Call): String {
        var failure: IOException? = null
        for (url in indexUrls) {
            if (call.isCancelled) throw IOException("cancelled")
            try {
                return fetch(call, url)
            } catch (e: IOException) {
                failure = e
            }
        }
        throw failure ?: IOException("No index host")
    }

    private fun fetch(call: Call, url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Three-Slate-Studio-update-checker/${BuildConfig.VERSION_NAME}")
        }
        call.connection = connection
        try {
            if (call.isCancelled) throw IOException("cancelled")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Index host returned HTTP $code")
            val declared = connection.contentLengthLong
            if (declared > MAXIMUM_RESPONSE_BYTES) throw IOException("Index response is too large")
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    if (output.size() > MAXIMUM_RESPONSE_BYTES) throw IOException("Index response is too large")
                }
            }
            return output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** The release package name; debug builds share it. zh-CN: 发行包名, debug 构建相同. */
        const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
        private const val INDEX_REPOSITORY = "SuperMonster003/AutoJs6-Official-Plugins-Index"
        private const val INDEX_FILE = "plugins.official.generated.json"
        /** The hosts of the host's `PluginIndexMirrorSession`, in its order. zh-CN: 与宿主 `PluginIndexMirrorSession` 相同的主机与顺序. */
        val INDEX_URLS = listOf(
            "https://raw.githubusercontent.com/$INDEX_REPOSITORY/refs/heads/main/$INDEX_FILE",
            "https://cdn.jsdelivr.net/gh/$INDEX_REPOSITORY@main/$INDEX_FILE",
            "https://fastly.jsdelivr.net/gh/$INDEX_REPOSITORY@main/$INDEX_FILE",
        )
        const val RELEASES_PAGE = "https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 20_000
        const val TOTAL_TIMEOUT_MILLIS = 45_000L
        const val MAXIMUM_RESPONSE_BYTES = 8 * 1024 * 1024
        const val MAXIMUM_NOTES_CHARACTERS = 6_000
        private val VERSION_NAME_PATTERN = Regex("^[0-9A-Za-z._+-]{1,64}$")
        private val worker = Executors.newSingleThreadExecutor { Thread(it, "SlateUpdateCheck").apply { isDaemon = true } }
        private val main by lazy { Handler(Looper.getMainLooper()) }

        /**
         * Finds the newest published release of [packageName] in the index JSON: the item with that `packageName`, its
         * `releases` that are not prereleases, the highest `versionCode`. The notes are the release's `changelogText`; the
         * page is its `changelogUrl` when that is a release page of the plugin's repository, else the releases page.
         * Pure apart from `org.json`.
         * zh-CN: 在索引 JSON 中找出 [packageName] 最新的已发布版本: 该 `packageName` 的条目, 其 `releases` 中非预发布的项, versionCode 最大者. 说明取
         * `changelogText`; 页面取 `changelogUrl` (须为插件仓库的发行页, 否则用发行列表页). 除 `org.json` 外无依赖.
         */
        fun decode(text: String, packageName: String = PACKAGE_NAME): UpdateResult {
            val items = JSONObject(text).getJSONArray("items")
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                if (item.optString("packageName") != packageName) continue
                val releases = item.optJSONArray("releases") ?: return UpdateResult.NoRelease
                var best: AppRelease? = null
                for (j in 0 until releases.length()) {
                    val release = releases.optJSONObject(j) ?: continue
                    if (release.optBoolean("prerelease", false)) continue
                    val code = release.optLong("versionCode", -1L)
                    val name = release.optString("versionName").trim()
                    if (code <= 0L || !VERSION_NAME_PATTERN.matches(name)) continue
                    if (best != null && best.versionCode >= code) continue
                    val page = release.optString("changelogUrl").trim()
                    best = AppRelease(
                        versionCode = code,
                        versionName = name,
                        notes = release.optString("changelogText").trim().take(MAXIMUM_NOTES_CHARACTERS),
                        pageUrl = if (page.startsWith("$RELEASES_PAGE/tag/")) page else RELEASES_PAGE,
                    )
                }
                return best?.let(UpdateResult::Release) ?: UpdateResult.NoRelease
            }
            return UpdateResult.NoRelease
        }
    }
}

/**
 * The update check of the settings page (appendix D Q42 = (b), ported from 3-Macaw Speech P5.4, 3-Stone AI rules; the
 * official plugin index as the source, appendix D Q45 = (b)): a
 * manual check shows a cancellable progress dialog and every result; an automatic check (when the settings page opens,
 * appendix D Q46, at most every 12 hours) stays silent unless a newer release that is not ignored exists. Compose state,
 * main thread only.
 * zh-CN: 设置页的更新检查 (附录 D Q42 = (b), 移植自 3-Macaw Speech P5.4, 沿用 3-Stone AI 规则; 数据源为官方插件索引, 附录 D Q45 = (b)): 手动检查显示可取消的进度对话框与每种结果; 自动检查
 * (打开设置页时, 附录 D Q46, 每 12 小时至多一次) 只在存在未被忽略的新版本时提示. Compose 状态, 仅在主线程使用.
 */
internal class AppUpdateController(
    private val store: AppUpdateStore,
    private val installedVersionCode: Long,
    private val checker: AppUpdateChecker = AppUpdateChecker(),
) {
    sealed interface State {
        data object Checking : State
        data class UpToDate(val noRelease: Boolean) : State
        data class Available(val release: AppRelease) : State
        data object Failed : State
    }

    var state by mutableStateOf<State?>(null)
        private set

    private var active: AppUpdateChecker.Call? = null

    fun checkManually() = check(manual = true)

    fun checkAutomaticallyIfDue() {
        if (active != null || !store.shouldCheckAutomatically()) return
        store.recordAutomaticAttempt()
        check(manual = false)
    }

    fun dismiss() {
        if (state == State.Checking) active?.cancel()
        state = null
    }

    fun ignore(tag: String) = store.ignore(tag)

    fun cancel() {
        active?.cancel()
        active = null
        if (state == State.Checking) state = null
    }

    private fun check(manual: Boolean) {
        if (active != null) return
        if (manual) state = State.Checking
        active = checker.check { call, result ->
            mainHandler.post {
                if (active === call) active = null
                if (call.isCancelled && !call.timedOut) {
                    if (state == State.Checking) state = null
                    return@post
                }
                result.fold(
                    onSuccess = { found ->
                        when (found) {
                            UpdateResult.NoRelease -> if (manual) state = State.UpToDate(noRelease = true)
                            is UpdateResult.Release -> {
                                val release = found.release
                                when {
                                    !UpdateVersionPolicy.isNewer(release, installedVersionCode) -> if (manual) state = State.UpToDate(noRelease = false)
                                    !manual && release.tag in store.ignoredTags -> Unit
                                    else -> state = State.Available(release)
                                }
                            }
                        }
                    },
                    onFailure = { if (manual) state = State.Failed },
                )
            }
        }
    }

    private companion object {
        val mainHandler = Handler(Looper.getMainLooper())
    }
}

/**
 * The dialogs of [AppUpdateController]: progress (cancel), up to date, failure, and the release dialog with "view
 * release", "later", the neutral "release history" button and the "ignore this version" check box.
 * zh-CN: [AppUpdateController] 的对话框: 进度 (可取消), 已是最新, 失败, 以及带 "查看发行版", "稍后", Neutral "发行历史" 按钮与 "忽略此版本" 复选框的发行版对话框.
 */
@Composable
internal fun UpdateDialogs(controller: AppUpdateController, onOpenRelease: (String) -> Unit, onReleaseHistory: () -> Unit) {
    when (val state = controller.state) {
        null -> Unit
        AppUpdateController.State.Checking -> MessageDialog(
            title = stringResource(R.string.update_checking),
            message = stringResource(R.string.update_checking_summary),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_cancel),
            dismissText = null,
            tag = "slate.update.checking",
        )
        is AppUpdateController.State.UpToDate -> MessageDialog(
            title = stringResource(R.string.update_up_to_date),
            message = if (state.noRelease) stringResource(R.string.update_no_release, BuildConfig.VERSION_NAME) else stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_confirm),
            dismissText = null,
            tag = "slate.update.current",
        )
        AppUpdateController.State.Failed -> MessageDialog(
            title = stringResource(R.string.update_check),
            message = stringResource(R.string.update_check_failed),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_confirm),
            dismissText = null,
            tag = "slate.update.failed",
        )
        is AppUpdateController.State.Available -> {
            val release = state.release
            var ignore by rememberSaveable(release.tag) { mutableStateOf(false) }
            fun close(open: Boolean) {
                if (ignore) controller.ignore(release.tag)
                controller.dismiss()
                if (open) onOpenRelease(release.pageUrl)
            }
            SlateDialog(
                title = release.name,
                onDismissRequest = { close(open = false) },
                confirmText = stringResource(R.string.update_view_release),
                onConfirm = { close(open = true) },
                dismissText = stringResource(R.string.update_later),
                neutralText = stringResource(R.string.release_history_title),
                onNeutral = {
                    if (ignore) controller.ignore(release.tag)
                    controller.dismiss()
                    onReleaseHistory()
                },
                tag = "slate.update.available",
            ) {
                Text(
                    stringResource(R.string.update_available_summary, release.tag) + if (release.notes.isNotBlank()) "\n\n" + release.notes else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding),
                )
                CheckRow(Choice(stringResource(R.string.update_ignore_version)), ignore, { ignore = it }, Modifier.testTag("slate.update.ignore"))
            }
        }
    }
}

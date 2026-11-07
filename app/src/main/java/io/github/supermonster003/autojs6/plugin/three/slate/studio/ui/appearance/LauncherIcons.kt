package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.appearance

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import java.util.concurrent.Executors

/**
 * The four stable launcher aliases are the only source of the persisted launcher choice (icon specification section 4,
 * roadmap P3.2); the automatic one is enabled by default.
 * zh-CN: 四个稳定的启动器 alias 是启动器选择的唯一持久来源 (图标规范第 4 节, 路线图 P3.2); 默认启用自动图标.
 */
internal enum class LauncherIconMode(val alias: String) {
    LIGHT("AdaptiveLightIconAlias"), DARK("AdaptiveDarkIconAlias"),
    AUTO("AdaptiveAutoIconAlias"), TRANSPARENT("TransparentIconAlias");

    fun component(context: Context) = ComponentName(context.packageName, "${context.packageName}.launcher.$alias")
}

internal object LauncherIconStatePolicy {
    fun enabled(mode: LauncherIconMode, state: Int) = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
        (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && mode == LauncherIconMode.AUTO)

    /** An explicit choice wins; several explicit entries or none normalize to the automatic default. zh-CN: 显式选择优先. */
    fun resolve(states: Map<LauncherIconMode, Int>): LauncherIconMode {
        val explicit = LauncherIconMode.entries.filter { states.getValue(it) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
        return explicit.firstOrNull { it == LauncherIconMode.AUTO } ?: explicit.firstOrNull() ?: LauncherIconMode.AUTO
    }
}

/**
 * Switches the launcher alias without killing the app, keeping the real activity and shortcut targets stable; adapted
 * from 3-Block Compose with exact shortcut ownership rollback.
 * zh-CN: 不杀进程地切换启动器 alias, 真实 Activity 与快捷方式目标保持不变; 改编自 3-Block Compose, 含快捷方式归属的精确回滚.
 */
internal object LauncherIcons {
    private val worker by lazy { Executors.newSingleThreadExecutor { Thread(it, "SlateLauncherIcon") } }

    const val REAL_ACTIVITY = "ui.home.HomeActivity"

    private fun realActivity(context: Context) = ComponentName(context.packageName, "${context.packageName}.$REAL_ACTIVITY")

    private fun snapshot(context: Context) = LauncherIconMode.entries.associateWith {
        context.packageManager.getComponentEnabledSetting(it.component(context))
    }

    fun current(context: Context) = LauncherIconStatePolicy.resolve(snapshot(context))

    /** Repairs a mixed or empty alias state; a consistent state is left untouched. zh-CN: 修复混合或空的 alias 状态; 一致的状态不动. */
    @Synchronized fun normalize(context: Context) {
        val before = snapshot(context)
        val mode = LauncherIconStatePolicy.resolve(before)
        val consistent = LauncherIconMode.entries.all { LauncherIconStatePolicy.enabled(it, before.getValue(it)) == (it == mode) }
        if (!consistent) select(context, mode)
    }

    fun normalizeAsync(context: Context, onComplete: (() -> Unit)? = null) {
        val app = context.applicationContext
        worker.execute {
            try {
                normalize(app)
            } catch (_: Exception) {
                // Retried on the next foreground launch. zh-CN: 下次前台启动时重试.
            } finally {
                onComplete?.invoke()
            }
        }
    }

    @Synchronized fun select(context: Context, mode: LauncherIconMode) {
        val pm = context.packageManager
        val before = snapshot(context)
        val shortcutBackup = mutableShortcuts(context)
        try {
            pm.setComponentEnabledSetting(mode.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            updateOwnership(context, shortcutBackup, mode.component(context))
            apply(context, LauncherIconMode.entries.associateWith {
                if (it == mode) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            })
        } catch (failure: Exception) {
            runCatching { apply(context, before) }
            if (Build.VERSION.SDK_INT >= 25 && shortcutBackup.isNotEmpty()) runCatching {
                check(context.getSystemService(ShortcutManager::class.java)?.updateShortcuts(shortcutBackup) != false)
            }
            throw failure
        }
        // Recovery is separate from ownership: never re-enable shortcuts the app disabled itself.
        if (Build.VERSION.SDK_INT >= 28) {
            val recovered = shortcutBackup.filter { !it.isEnabled && it.disabledReason == ShortcutInfo.DISABLED_REASON_APP_CHANGED }.map { it.id }
            if (recovered.isNotEmpty()) runCatching { context.getSystemService(ShortcutManager::class.java)?.enableShortcuts(recovered) }
        }
    }

    private fun apply(context: Context, states: Map<LauncherIconMode, Int>) {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= 33) {
            pm.setComponentEnabledSettings(states.map { (mode, state) ->
                PackageManager.ComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            })
        } else {
            states.entries.sortedBy { if (LauncherIconStatePolicy.enabled(it.key, it.value)) 0 else 1 }.forEach { (mode, state) ->
                pm.setComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    private fun mutableShortcuts(context: Context): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < 25) return emptyList()
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return emptyList()
        val ours = LauncherIconMode.entries.map { it.component(context) }.toSet() + realActivity(context)
        return (manager.dynamicShortcuts + manager.pinnedShortcuts).distinctBy { it.id }.filter {
            it.activity in ours && !it.isDeclaredInManifest && !(Build.VERSION.SDK_INT >= 30 && it.isImmutable)
        }
    }

    private fun updateOwnership(context: Context, originals: List<ShortcutInfo>, target: ComponentName) {
        if (Build.VERSION.SDK_INT < 25) return
        val aliases = LauncherIconMode.entries.map { it.component(context) }.toSet()
        val real = realActivity(context)
        val updates = originals.filter { it.activity != target || it.intents?.any { intent -> intent.component in aliases } == true }.map { shortcut ->
            ShortcutInfo.Builder(context, shortcut.id).setActivity(target).apply {
                shortcut.shortLabel?.let(::setShortLabel)
                shortcut.longLabel?.let(::setLongLabel)
                // Preserve stable real-activity targets and all source Intent fields.
                shortcut.intents?.map { original -> Intent(original).apply { if (component in aliases) component = real } }?.toTypedArray()?.let(::setIntents)
            }.build()
        }
        if (updates.isNotEmpty()) check(context.getSystemService(ShortcutManager::class.java)?.updateShortcuts(updates) != false) {
            "The launcher could not update shortcut ownership"
        }
    }
}

/**
 * Package replacement only repairs local components and recoverable shortcuts: no screen, network or model loading.
 * zh-CN: 包替换时只修复本地组件与可恢复的快捷方式: 不打开界面, 不联网, 不加载模型.
 */
class LauncherIconUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        LauncherIcons.normalizeAsync(context) { pending.finish() }
    }
}

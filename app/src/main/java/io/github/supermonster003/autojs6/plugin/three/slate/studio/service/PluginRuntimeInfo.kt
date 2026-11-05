package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mediastudio.api.MediaStudioCapabilityKeys

/**
 * The PluginInfo fields as plain values, so JVM tests can freeze the mapping without Android.
 * zh-CN: 以纯值表示的 PluginInfo 字段, 使 JVM 测试无需 Android 即可冻结映射.
 */
internal data class PluginRuntimeFields(
    val name: String,
    val description: String,
    val instruction: String,
    val author: String,
    val id: String,
    val engine: String,
    val variant: String,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
    /**
     * Always empty: the only native library is the ABI split `libandroidx.graphics.path.so` of Compose, present for all
     * four ABIs, so the universal APK runs on every ABI (roadmap D25, appendix D Q9).
     * zh-CN: 恒为空: 唯一的原生库是 Compose 按 ABI 拆分的 `libandroidx.graphics.path.so`, 四个 ABI 齐全, 因此 universal APK
     * 可在所有 ABI 上运行 (路线图 D25, 附录 D Q9).
     */
    val supportedAbis: List<String>,
    val requiredHostVersion: Long,
    /** Media studio contract version and implemented operations (roadmap D28). zh-CN: 媒体工坊契约版本与已实现的操作 (路线图 D28). */
    val contractVersion: Int,
    val ops: List<String>,
)

/**
 * Assembles the fields; [id], [engine] and [variant] come from the build resources, which the JVM tests keep equal to
 * [ThreeSlateStudioContract] and the README sources.
 * zh-CN: 组装字段; [id], [engine] 与 [variant] 来自构建资源, JVM 测试保证其与 [ThreeSlateStudioContract] 及 README 源一致.
 */
internal fun pluginRuntimeFields(
    name: String,
    description: String,
    author: String,
    id: String,
    engine: String,
    variant: String,
    versionName: String,
    versionCode: Long,
    versionDate: String,
): PluginRuntimeFields = PluginRuntimeFields(
    name = name,
    description = description,
    instruction = ThreeSlateStudioContract.INSTRUCTION_REFERENCE,
    author = author,
    id = id,
    engine = engine,
    variant = variant,
    versionName = versionName,
    versionCode = versionCode,
    versionDate = versionDate,
    supportedAbis = emptyList(),
    requiredHostVersion = ThreeSlateStudioContract.REQUIRED_HOST_VERSION,
    contractVersion = ThreeSlateStudioContract.CONTRACT_VERSION,
    ops = ThreeSlateStudioContract.IMPLEMENTED_OPS,
)

/**
 * The capability bundle of PluginInfo and of `IMediaStudioPlugin.getCapabilities` (roadmap P2.1 adds encoders, formats
 * and the storage permission state).
 * zh-CN: PluginInfo 与 `IMediaStudioPlugin.getCapabilities` 共用的能力 Bundle (路线图 P2.1 再加编码器, 格式与存储权限状态).
 */
internal fun PluginRuntimeFields.capabilities(): Bundle = Bundle().apply {
    putLong(MediaStudioCapabilityKeys.REQUIRES_HOST_VERSION, requiredHostVersion)
    putInt(MediaStudioCapabilityKeys.CONTRACT_VERSION, contractVersion)
    putStringArray(MediaStudioCapabilityKeys.OPS, ops.toTypedArray())
}

/**
 * The identity answered by the INFO service and by `IMediaStudioPlugin.getInfo`.
 * zh-CN: INFO 服务与 `IMediaStudioPlugin.getInfo` 返回的身份.
 */
internal fun PluginRuntimeFields.toPluginInfo(): PluginInfo = PluginInfo().also { info ->
    info.name = name
    info.description = description
    info.instruction = instruction
    info.author = author
    info.id = id
    info.engine = engine
    info.variant = variant
    info.versionName = versionName
    info.versionCode = versionCode
    info.versionDate = versionDate
    // ABI independent: an explicit empty array, never null (new repository specification 5.5, roadmap D25).
    // zh-CN: 与 ABI 无关: 显式空数组, 不为 null (新仓库规范 5.5, 路线图 D25).
    info.supportedAbis = supportedAbis.toTypedArray()
    info.capabilities = capabilities()
}

/**
 * Reads the localized description, build resources and the installed package version.
 * zh-CN: 读取本地化描述, 构建资源与已安装包的版本.
 */
internal fun Context.pluginRuntimeFields(): PluginRuntimeFields {
    val appContext = applicationContext
    val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    return pluginRuntimeFields(
        name = appContext.getString(R.string.plugin_name),
        description = appContext.getString(R.string.plugin_description),
        author = appContext.getString(R.string.plugin_author),
        id = appContext.getString(R.string.plugin_id),
        engine = appContext.getString(R.string.plugin_engine),
        variant = appContext.getString(R.string.plugin_variant),
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = packageInfo.versionCodeCompat(),
        versionDate = appContext.getString(R.string.plugin_version_date),
    )
}

private fun PackageInfo.versionCodeCompat(): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return longVersionCode
    @Suppress("DEPRECATION")
    return versionCode.toLong()
}

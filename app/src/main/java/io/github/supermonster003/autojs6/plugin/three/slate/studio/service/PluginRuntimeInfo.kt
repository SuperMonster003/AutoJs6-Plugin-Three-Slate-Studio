package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.CodecSupport
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.EncoderIds
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.SubtitleFormat
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mediastudio.api.MediaStudioCapabilityKeys
import org.autojs.plugin.mediastudio.api.MediaStudioLimits

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
    /**
     * Encoders and output formats of this device (roadmap D29, Q7), scanned by [CodecSupport]; empty in JVM tests.
     * zh-CN: 本机的编码器与输出格式 (路线图 D29, Q7), 由 [CodecSupport] 扫描; JVM 测试中为空.
     */
    val videoEncoders: List<String> = emptyList(),
    val audioEncoders: List<String> = emptyList(),
    val videoOutputFormats: List<String> = emptyList(),
    val audioOutputFormats: List<String> = emptyList(),
    /** Ceilings of roadmap D28. zh-CN: 路线图 D28 的上限. */
    val maxConcurrentEncodes: Int = MediaStudioLimits.MAX_CONCURRENT_ENCODES,
    val maxTasksPerToken: Int = MediaStudioLimits.MAX_TASKS_PER_TOKEN,
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
    videoEncoders: List<String> = emptyList(),
    audioEncoders: List<String> = emptyList(),
    videoOutputFormats: List<String> = emptyList(),
    audioOutputFormats: List<String> = emptyList(),
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
    videoEncoders = videoEncoders,
    audioEncoders = audioEncoders,
    videoOutputFormats = videoOutputFormats,
    audioOutputFormats = audioOutputFormats,
)

/**
 * The static capability bundle of PluginInfo: contract version, operations, encoders, output formats and ceilings.
 * zh-CN: PluginInfo 的静态能力 Bundle: 契约版本, 操作, 编码器, 输出格式与上限.
 */
internal fun PluginRuntimeFields.capabilities(): Bundle = Bundle().apply {
    putLong(MediaStudioCapabilityKeys.REQUIRES_HOST_VERSION, requiredHostVersion)
    putInt(MediaStudioCapabilityKeys.CONTRACT_VERSION, contractVersion)
    putStringArray(MediaStudioCapabilityKeys.OPS, ops.toTypedArray())
    putStringArray(MediaStudioCapabilityKeys.VIDEO_ENCODERS, videoEncoders.toTypedArray())
    putStringArray(MediaStudioCapabilityKeys.AUDIO_ENCODERS, audioEncoders.toTypedArray())
    putStringArray(MediaStudioCapabilityKeys.VIDEO_OUTPUT_FORMATS, videoOutputFormats.toTypedArray())
    putStringArray(MediaStudioCapabilityKeys.AUDIO_OUTPUT_FORMATS, audioOutputFormats.toTypedArray())
    putStringArray(MediaStudioCapabilityKeys.SUBTITLE_FORMATS, SubtitleFormat.IDS.toTypedArray())
    putInt(MediaStudioCapabilityKeys.MAX_CONCURRENT_ENCODES, maxConcurrentEncodes)
    putInt(MediaStudioCapabilityKeys.MAX_TASKS_PER_TOKEN, maxTasksPerToken)
}

/**
 * The live bundle of `IMediaStudioPlugin.getCapabilities`: the static keys plus the storage permission state and
 * free-text warnings (no H.265 encoder, low shared storage, background restriction).
 * zh-CN: `IMediaStudioPlugin.getCapabilities` 的实时 Bundle: 静态键加上存储权限状态与自由文本警告 (没有 H.265 编码器, 共享存储
 * 空间不足, 后台受限).
 */
internal fun PluginRuntimeFields.liveCapabilities(context: Context): Bundle = capabilities().apply {
    putBoolean(MediaStudioCapabilityKeys.STORAGE_PERMISSION, StorageAccess.isGranted(context))
    putStringArray(MediaStudioCapabilityKeys.WARNINGS, runtimeWarnings(context).toTypedArray())
}

private fun PluginRuntimeFields.runtimeWarnings(context: Context): List<String> = buildList {
    // HyperOS restricts newly installed apps in the background (RUN_ANY_IN_BACKGROUND ignore), and the system then
    // ignores startForeground() without an exception (seen on the Xiaomi Pad, API 35, 2026-11-06).
    // zh-CN: HyperOS 默认限制新装应用的后台 (RUN_ANY_IN_BACKGROUND ignore), 此时系统忽略 startForeground() 且不抛异常
    // (2026-11-06 在小米平板 API 35 上观察到).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true) {
        add("the system restricts 3-Slate Studio in the background; tasks longer than 10 s run without the foreground service and its progress notification; allow unrestricted battery use for the plugin")
    }
    if (videoEncoders.isEmpty()) {
        add("this device has no H.264 or H.265 encoder; video operations answer UNSUPPORTED_FORMAT")
    } else if (EncoderIds.H265 !in videoEncoders) {
        add("this device has no H.265 encoder; codec h265 answers UNSUPPORTED_FORMAT")
    }
    @Suppress("DEPRECATION")
    val free = runCatching { StatFs(Environment.getExternalStorageDirectory().path).availableBytes }.getOrNull()
    if (free != null && free < LOW_STORAGE_BYTES) {
        add("shared storage has ${free / (1024 * 1024)} MiB free, below ${LOW_STORAGE_BYTES / (1024 * 1024)} MiB")
    }
}

/** Free shared storage below this adds a warning. zh-CN: 共享存储可用空间低于此值时加入警告. */
private const val LOW_STORAGE_BYTES = 1024L * 1024 * 1024

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
        videoEncoders = CodecSupport.videoEncoders(),
        audioEncoders = CodecSupport.audioEncoders(),
        videoOutputFormats = CodecSupport.videoOutputFormats(),
        audioOutputFormats = CodecSupport.audioOutputFormats(),
    )
}

private fun PackageInfo.versionCodeCompat(): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return longVersionCode
    @Suppress("DEPRECATION")
    return versionCode.toLong()
}

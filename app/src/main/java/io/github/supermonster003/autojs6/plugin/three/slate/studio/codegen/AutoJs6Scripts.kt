package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioContract
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.MediaActions
import java.io.File
import java.util.UUID

/**
 * "Open in AutoJs6" (roadmap D19, D36, P4.4): hands a [GeneratedScript] to the host's `CREATE_SCRIPT` entry (roadmap
 * P1.4), which opens it in the editor as a new, unsaved file. Code within the inline limits travels in the Intent; larger
 * code (up to [MAX_CODE_URI_BYTES]) is written to `cache/create-script/` and sent as a content URI of the plugin's
 * FileProvider with a read grant (roadmap P1.7, appendix D Q28 (c) / Q31 (a)). The entry needs the host build of
 * [ThreeSlateStudioContract.REQUIRED_HOST_VERSION] or later; older or missing hosts get no button, only the copy button
 * (roadmap D36).
 *
 * The literals are those of the host's `AutoJs6CreateScriptContract` (common-plugin-api, host commits `5fc200c3ab` and
 * `271949817a`), frozen by the host; the staged common-plugin-api 1.1.1 predates the contract, so they are repeated here.
 * zh-CN: "在 AutoJs6 中打开" (路线图 D19, D36, P4.4): 把 [GeneratedScript] 交给宿主的 `CREATE_SCRIPT` 入口 (路线图 P1.4), 宿主在编辑器中以
 * 未保存的新文件打开. 不超过内联上限的代码随 Intent 发送; 更大的代码 (至多 [MAX_CODE_URI_BYTES]) 写入 `cache/create-script/`, 以插件
 * FileProvider 的 content URI 加读取授权发送 (路线图 P1.7, 附录 D Q28 (c) / Q31 (a)). 入口需要 [ThreeSlateStudioContract.REQUIRED_HOST_VERSION]
 * 或更新的宿主构建; 宿主过旧或未安装时不显示按钮, 只提供复制 (路线图 D36).
 *
 * 字面值取自宿主的 `AutoJs6CreateScriptContract` (common-plugin-api, 宿主提交 `5fc200c3ab` 与 `271949817a`), 已由宿主冻结; 暂存的
 * common-plugin-api 1.1.1 早于该契约, 所以在此重复.
 */
internal object AutoJs6Scripts {

    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"
    const val ACTION_CREATE_SCRIPT = "org.autojs.autojs.action.CREATE_SCRIPT"
    const val EXTRA_CODE = "code"
    const val EXTRA_CODE_URI = "codeUri"
    const val EXTRA_SUGGESTED_NAME = "suggestedName"
    const val EXTRA_SOURCE = "source"
    const val EXTRA_ERROR = "error"

    /** Inline limits: UTF-8 bytes and UTF-16 chars. zh-CN: 内联上限: UTF-8 字节数与 UTF-16 字符数. */
    const val MAX_CODE_BYTES = 512 * 1024
    const val MAX_CODE_CHARS = 200_000

    /** The host reads at most this many bytes of a URI (appendix D Q31 (a)). zh-CN: 宿主经 URI 至多读取的字节数 (附录 D Q31 (a)). */
    const val MAX_CODE_URI_BYTES = 4 * 1024 * 1024

    /** The cache folder exposed by `res/xml/shared_files.xml`. zh-CN: `res/xml/shared_files.xml` 暴露的缓存目录. */
    const val CACHE_FOLDER = "create-script"

    /** Files older than this are left from earlier sends and deleted. zh-CN: 早于此时长的文件是此前发送遗留的, 予以删除. */
    const val STALE_MILLIS = 60 * 60 * 1000L

    private const val TAG = "SlateAutoJs6Scripts"

    /** How a script travels to the host. zh-CN: 脚本发送给宿主的方式. */
    sealed interface Transport {
        data object Inline : Transport
        data object ContentUri : Transport
        data class TooLarge(val bytes: Int) : Transport
    }

    fun transportOf(code: String): Transport {
        val bytes = code.toByteArray(Charsets.UTF_8).size
        return when {
            bytes <= MAX_CODE_BYTES && code.length <= MAX_CODE_CHARS -> Transport.Inline
            bytes <= MAX_CODE_URI_BYTES -> Transport.ContentUri
            else -> Transport.TooLarge(bytes)
        }
    }

    fun isCompatible(hostVersionCode: Long?): Boolean = hostVersionCode != null && hostVersionCode >= ThreeSlateStudioContract.REQUIRED_HOST_VERSION

    @Suppress("DEPRECATION")
    fun hostVersionCode(context: Context): Long? = try {
        val info = context.packageManager.getPackageInfo(HOST_PACKAGE_NAME, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /**
     * True when the host is installed, new enough and its entry resolves for this plugin.
     * zh-CN: 宿主已安装, 版本足够且入口对本插件可解析时为 true.
     */
    @Suppress("DEPRECATION")
    fun isAvailable(context: Context): Boolean =
        isCompatible(hostVersionCode(context)) && context.packageManager.resolveActivity(baseIntent(), 0) != null

    /** The result of [intentOf]. zh-CN: [intentOf] 的结果. */
    sealed interface Request {
        /** Start [intent] for a result, then call [cleanUp] with [file]. zh-CN: 以等待结果的方式启动 [intent], 之后用 [file] 调用 [cleanUp]. */
        data class Ready(val intent: Intent, val file: File?) : Request
        data class TooLarge(val bytes: Int) : Request
    }

    /**
     * The `CREATE_SCRIPT` Intent of [script]: the code inline, or a content URI with a read grant (written to the cache
     * on the calling thread). [source] is the plugin name the host shows.
     * zh-CN: [script] 的 `CREATE_SCRIPT` Intent: 内联代码, 或带读取授权的 content URI (在调用线程写入缓存). [source] 为宿主显示的插件名.
     */
    fun intentOf(context: Context, script: GeneratedScript, source: String): Request {
        val intent = baseIntent()
            .putExtra(EXTRA_SUGGESTED_NAME, script.suggestedName)
            .putExtra(EXTRA_SOURCE, source)
        return when (val transport = transportOf(script.code)) {
            Transport.Inline -> Request.Ready(intent.putExtra(EXTRA_CODE, script.code), null)
            Transport.ContentUri -> {
                cleanUp(context, null)
                val folder = File(context.cacheDir, "$CACHE_FOLDER/${UUID.randomUUID()}").apply { mkdirs() }
                val file = File(folder, script.suggestedName.substringAfterLast('/').ifEmpty { "script.js" })
                file.writeText(script.code, Charsets.UTF_8)
                val uri: Uri = FileProvider.getUriForFile(context, MediaActions.authority(context), file)
                // The grant covers the data and ClipData URIs, not extras (host protocol "URI mode").
                // zh-CN: 授权只覆盖 data 与 ClipData 中的 URI, 不覆盖 extra (宿主协议 "URI mode").
                intent.putExtra(EXTRA_CODE_URI, uri)
                intent.clipData = ClipData.newRawUri(null, uri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Request.Ready(intent, file)
            }
            is Transport.TooLarge -> Request.TooLarge(transport.bytes)
        }
    }

    /**
     * Deletes [file] (the host keeps its own copy once it answered) and files of earlier sends older than
     * [STALE_MILLIS].
     * zh-CN: 删除 [file] (宿主应答后已保留自己的副本) 以及此前发送遗留的, 早于 [STALE_MILLIS] 的文件.
     */
    fun cleanUp(context: Context, file: File?) {
        runCatching {
            file?.parentFile?.deleteRecursively()
            val now = System.currentTimeMillis()
            File(context.cacheDir, CACHE_FOLDER).listFiles()?.filter { now - it.lastModified() > STALE_MILLIS }?.forEach { it.deleteRecursively() }
        }.onFailure { Log.w(TAG, "cannot clean the script cache", it) }
    }

    private fun baseIntent(): Intent = Intent(ACTION_CREATE_SCRIPT).setPackage(HOST_PACKAGE_NAME)

}

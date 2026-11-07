package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaLibrary
import java.io.File
import java.util.Locale

/**
 * Opening and sharing files from the Studio screens (roadmap P4.1, P4.2): a [FileProvider] URI under the shared storage
 * (`res/xml/shared_files.xml`), else the MediaStore URI; the receiver gets a temporary read grant only.
 * zh-CN: 从 Studio 界面打开与分享文件 (路线图 P4.1, P4.2): 共享存储下用 [FileProvider] URI (`res/xml/shared_files.xml`), 否则用
 * MediaStore URI; 接收方只获得临时读取授权.
 */
internal object MediaActions {

    private const val TAG = "SlateMediaActions"

    fun authority(context: Context) = "${context.packageName}.files"

    fun uriOf(context: Context, path: String): Uri? = runCatching { FileProvider.getUriForFile(context, authority(context), File(path)) }
        .getOrElse { MediaLibrary.contentUri(context, path) }

    fun mimeOf(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (extension) {
            "srt" -> "application/x-subrip"
            "ass", "ssa" -> "text/x-ssa"
            "vtt" -> "text/vtt"
            "opus" -> "audio/ogg"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }

    /** Opens [path] in another app; false when nothing can show it. zh-CN: 用其他应用打开 [path]; 无应用可打开时为 false. */
    fun open(context: Context, path: String): Boolean {
        val uri = uriOf(context, path) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeOf(path)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return start(context, Intent.createChooser(intent, context.getString(R.string.action_open_with)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Shares [paths] (one or many). zh-CN: 分享 [paths] (一个或多个). */
    fun share(context: Context, paths: List<String>): Boolean {
        val uris = paths.mapNotNull { uriOf(context, it) }
        if (uris.isEmpty()) return false
        val mime = paths.map(::mimeOf).distinct().singleOrNull() ?: (paths.map { mimeOf(it).substringBefore('/') }.distinct().singleOrNull()?.let { "$it/*" } ?: "*/*")
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).setType(mime).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        intent.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return start(context, Intent.createChooser(intent, context.getString(R.string.action_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun start(context: Context, intent: Intent): Boolean = runCatching { context.startActivity(intent) }
        .onFailure { Log.w(TAG, "cannot start ${intent.action}", it) }.isSuccess
}

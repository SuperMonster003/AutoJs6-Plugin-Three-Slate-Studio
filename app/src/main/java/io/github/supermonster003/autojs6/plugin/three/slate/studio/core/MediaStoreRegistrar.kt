package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Registers a finished output in MediaStore (`registerMedia`, default true) through the media scanner, so galleries and
 * music players list it. Waits at most [TIMEOUT_MILLIS]; a late scan still completes in the background.
 * zh-CN: 经媒体扫描把完成的输出登记到 MediaStore (`registerMedia`, 默认 true), 使图库与音乐播放器能列出它. 最多等待
 * [TIMEOUT_MILLIS]; 超时的扫描仍在后台完成.
 */
internal object MediaStoreRegistrar {

    const val TIMEOUT_MILLIS = 3_000L

    /** True when the scanner answered with a content URI in time. zh-CN: 扫描器及时返回内容 URI 时为 true. */
    fun register(context: Context, path: String, mime: String?): Boolean {
        val latch = CountDownLatch(1)
        var uri: Uri? = null
        MediaScannerConnection.scanFile(context.applicationContext, arrayOf(path), mime?.let { arrayOf(it) }) { _, scanned ->
            uri = scanned
            latch.countDown()
        }
        latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        return uri != null
    }

}

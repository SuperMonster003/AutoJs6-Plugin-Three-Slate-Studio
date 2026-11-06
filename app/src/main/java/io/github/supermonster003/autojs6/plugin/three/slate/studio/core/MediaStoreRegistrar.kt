package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    /**
     * One scan for many files of one [mime] (the images of `frames`); true when every file got a URI within
     * [TIMEOUT_MILLIS] in total.
     * zh-CN: 一次扫描登记同一 [mime] 的多个文件 (`frames` 的图片); 全部文件在合计 [TIMEOUT_MILLIS] 内取得 URI 时为 true.
     */
    fun registerAll(context: Context, paths: List<String>, mime: String?): Boolean {
        if (paths.isEmpty()) return true
        val latch = CountDownLatch(paths.size)
        val registered = AtomicInteger()
        MediaScannerConnection.scanFile(context.applicationContext, paths.toTypedArray(), mime?.let { type -> Array(paths.size) { type } }) { _, scanned ->
            if (scanned != null) registered.incrementAndGet()
            latch.countDown()
        }
        latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        return registered.get() == paths.size
    }

}

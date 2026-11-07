package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StudioPreferences
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** The kind of a library item. zh-CN: 媒体库条目的类型. */
internal enum class MediaKind { VIDEO, AUDIO, IMAGE, OTHER }

/** One media file of the library folders. zh-CN: 媒体库目录中的一个媒体文件. */
internal data class MediaEntry(
    val id: Long,
    val uri: Uri,
    val path: String,
    val name: String,
    val kind: MediaKind,
    val sizeBytes: Long,
    val durationMillis: Long?,
    val width: Int?,
    val height: Int?,
    val modifiedSeconds: Long,
    val folder: String,
    val mime: String?,
)

/**
 * The home screen's media (roadmap D37): MediaStore rows under the Studio folders of D14 (`Movies` / `Music` /
 * `Pictures` `3-Slate Studio`, appendix D Q14 added `Pictures`), the host's recording folders (`Movies/AutoJs6`,
 * `Music/AutoJs6`) and the folders the user added; no full scan. File actions are allowed under these folders only.
 * zh-CN: 首页的媒体 (路线图 D37): D14 的 Studio 目录 (`Movies` / `Music` / `Pictures` 下的 `3-Slate Studio`, 附录 D Q14 增加了
 * `Pictures`), 宿主的录制目录 (`Movies/AutoJs6`, `Music/AutoJs6`) 与用户添加的目录下的 MediaStore 记录; 不扫描全盘. 文件操作只
 * 限于这些目录.
 */
internal object MediaLibrary {

    /** Folders relative to the shared storage root. zh-CN: 相对共享存储根的目录. */
    val FIXED_FOLDERS = listOf(
        "Movies/3-Slate Studio",
        "Movies/AutoJs6",
        "Music/3-Slate Studio",
        "Music/AutoJs6",
        "Pictures/3-Slate Studio",
    )

    @Suppress("DEPRECATION")
    val storageRoot: String get() = Environment.getExternalStorageDirectory().path

    fun folders(context: Context): List<String> = (FIXED_FOLDERS + StudioPreferences.of(context).libraryFolders).distinct()

    fun absolute(relative: String): String = storageRoot.trimEnd('/') + "/" + relative.trim('/')

    /** The library folder that holds [path], or null outside the library. zh-CN: 包含 [path] 的媒体库目录, 不在库中时为 null. */
    fun folderOf(context: Context, path: String): String? = folders(context).firstOrNull { folder ->
        val root = absolute(folder)
        path.startsWith("$root/")
    }

    /** Lists the media of [folders], newest first; must run off the main thread. zh-CN: 列出 [folders] 中的媒体, 最新在前; 须在后台线程运行. */
    fun query(context: Context, folders: List<String>): List<MediaEntry> {
        val resolver = context.contentResolver
        val uri = MediaStore.Files.getContentUri("external")
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DATA)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.DATE_MODIFIED)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.DURATION)
        }.toTypedArray()
        val roots = folders.map(::absolute)
        val data = MediaStore.Files.FileColumns.DATA
        // Hidden files (the `.part` and `.frame-` temporaries of the executor) are left out. zh-CN: 不列隐藏文件 (执行器的临时文件).
        val selection = roots.joinToString(" OR ", prefix = "(", postfix = ")") { "$data LIKE ? ESCAPE '\\'" } +
            " AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}, " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE})" +
            " AND $data NOT LIKE ?"
        val arguments = roots.map { escapeLike(it) + "/%" } + "%/.%"
        val entries = ArrayList<MediaEntry>()
        val cursor = runCatching {
            resolver.query(uri, projection, selection, arguments.toTypedArray(), "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
        }.getOrNull() ?: return entries
        cursor.use { c -> while (c.moveToNext()) entry(c, roots)?.let(entries::add) }
        return entries
    }

    private fun entry(c: Cursor, roots: List<String>): MediaEntry? {
        val path = c.string(MediaStore.Files.FileColumns.DATA) ?: return null
        val id = c.long(MediaStore.Files.FileColumns._ID) ?: return null
        val kind = when (c.int(MediaStore.Files.FileColumns.MEDIA_TYPE)) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaKind.VIDEO
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaKind.AUDIO
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaKind.IMAGE
            else -> MediaKind.OTHER
        }
        val content = when (kind) {
            MediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaKind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MediaKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaKind.OTHER -> MediaStore.Files.getContentUri("external")
        }
        val root = roots.firstOrNull { path.startsWith("$it/") } ?: return null
        return MediaEntry(
            id = id,
            uri = ContentUris.withAppendedId(content, id),
            path = path,
            name = c.string(MediaStore.Files.FileColumns.DISPLAY_NAME) ?: File(path).name,
            kind = kind,
            sizeBytes = c.long(MediaStore.Files.FileColumns.SIZE) ?: File(path).length(),
            durationMillis = c.long(MediaStore.MediaColumns.DURATION)?.takeIf { it > 0 },
            width = c.int(MediaStore.MediaColumns.WIDTH)?.takeIf { it > 0 },
            height = c.int(MediaStore.MediaColumns.HEIGHT)?.takeIf { it > 0 },
            modifiedSeconds = c.long(MediaStore.Files.FileColumns.DATE_MODIFIED) ?: 0L,
            folder = root.removePrefix(storageRoot.trimEnd('/') + "/"),
            mime = c.string(MediaStore.Files.FileColumns.MIME_TYPE),
        )
    }

    private fun escapeLike(text: String) = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun Cursor.index(column: String) = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }
    private fun Cursor.string(column: String) = index(column)?.let(::getString)
    private fun Cursor.long(column: String) = index(column)?.let(::getLong)
    private fun Cursor.int(column: String) = index(column)?.let(::getInt)

    /** A content URI for [path] (MediaStore row, scanned when missing); null when the system cannot give one. zh-CN: [path] 的内容 URI. */
    fun contentUri(context: Context, path: String): Uri? {
        val resolver = context.contentResolver
        val uri = MediaStore.Files.getContentUri("external")
        runCatching {
            resolver.query(uri, arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE), "${MediaStore.Files.FileColumns.DATA} = ?", arrayOf(path), null)
        }.getOrNull()?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(0)
                val base = when (c.getInt(1)) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    else -> uri
                }
                return ContentUris.withAppendedId(base, id)
            }
        }
        return scan(context, path)
    }

    /** Scans [path] into MediaStore and waits up to 3 s for its URI. zh-CN: 把 [path] 扫描进 MediaStore, 最多等 3 秒取得 URI. */
    fun scan(context: Context, path: String): Uri? {
        val latch = CountDownLatch(1)
        var result: Uri? = null
        MediaScannerConnection.scanFile(context.applicationContext, arrayOf(path), null) { _, uri -> result = uri; latch.countDown() }
        latch.await(3, TimeUnit.SECONDS)
        return result
    }

    /**
     * Renames a library file and answers its new path; null outside the library, for an invalid name or when the name is
     * taken. zh-CN: 重命名媒体库文件并返回新路径; 不在库中, 名称无效或已被占用时为 null.
     */
    fun rename(context: Context, path: String, newName: String): String? {
        if (folderOf(context, path) == null) return null
        val name = newName.trim()
        if (name.isEmpty() || name.contains('/') || name == "." || name == "..") return null
        val source = File(path)
        val target = File(source.parentFile, name)
        if (target.exists() || !source.renameTo(target)) return null
        deleteRow(context.contentResolver, path)
        scan(context, target.path)
        return target.path
    }

    /** Deletes a library file and its MediaStore row. zh-CN: 删除媒体库文件及其 MediaStore 记录. */
    fun delete(context: Context, path: String): Boolean {
        if (folderOf(context, path) == null) return false
        val file = File(path)
        val removed = !file.exists() || file.delete()
        deleteRow(context.contentResolver, path)
        return removed
    }

    private fun deleteRow(resolver: ContentResolver, path: String) {
        runCatching { resolver.delete(MediaStore.Files.getContentUri("external"), "${MediaStore.Files.FileColumns.DATA} = ?", arrayOf(path)) }
    }

    /**
     * The file path behind a document picked with the system picker, when the plugin can read it directly (roadmap D9:
     * all files access): external storage documents, MediaStore documents and file URIs; null for other providers.
     * zh-CN: 系统选择器所选文档对应的文件路径 (路线图 D9: 所有文件访问): 外部存储文档, MediaStore 文档与 file URI; 其他提供方为 null.
     */
    fun pathOf(context: Context, uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.path
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        val authority = uri.authority ?: return null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            when (authority) {
                "com.android.externalstorage.documents" -> {
                    val (volume, relative) = id.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    val root = if (volume.equals("primary", ignoreCase = true)) storageRoot else "/storage/$volume"
                    return root.trimEnd('/') + "/" + relative
                }
                "com.android.providers.downloads.documents" -> if (id.startsWith("raw:")) return id.removePrefix("raw:")
                "com.android.providers.media.documents" -> {
                    val (type, number) = id.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    val base = when (type) {
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Files.getContentUri("external")
                    }
                    number.toLongOrNull()?.let { return dataOf(context, ContentUris.withAppendedId(base, it)) }
                }
            }
            return null
        }
        if (authority == MediaStore.AUTHORITY) return dataOf(context, uri)
        return null
    }

    /** The folder path of a tree picked with the system picker. zh-CN: 系统选择器所选目录树的路径. */
    fun treePathOf(uri: Uri): String? = runCatching {
        if (uri.authority != "com.android.externalstorage.documents") return null
        val id = DocumentsContract.getTreeDocumentId(uri)
        val (volume, relative) = id.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val root = if (volume.equals("primary", ignoreCase = true)) storageRoot else "/storage/$volume"
        (root.trimEnd('/') + "/" + relative).trimEnd('/')
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun dataOf(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

}

/**
 * Thumbnails of library items, decoded on a worker and kept in a small memory cache.
 * zh-CN: 媒体库条目的缩略图, 在后台线程解码并放入小型内存缓存.
 */
internal object Thumbnails {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val worker = Executors.newFixedThreadPool(2) { Thread(it, "SlateThumbnails").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private const val SIZE = 256

    fun cached(entry: MediaEntry): Bitmap? = cache.get(key(entry))

    fun load(context: Context, entry: MediaEntry, onLoaded: (Bitmap?) -> Unit) {
        val key = key(entry)
        cache.get(key)?.let { onLoaded(it); return }
        val application = context.applicationContext
        worker.execute {
            val bitmap = runCatching { decode(application, entry) }.getOrNull()
            if (bitmap != null) cache.put(key, bitmap)
            main.post { onLoaded(bitmap) }
        }
    }

    private fun key(entry: MediaEntry) = "${entry.id}:${entry.modifiedSeconds}"

    @Suppress("DEPRECATION")
    private fun decode(context: Context, entry: MediaEntry): Bitmap? {
        if (entry.kind == MediaKind.AUDIO || entry.kind == MediaKind.OTHER) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.contentResolver.loadThumbnail(entry.uri, Size(SIZE, SIZE), null)
        }
        return when (entry.kind) {
            MediaKind.VIDEO -> MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, entry.id, MediaStore.Video.Thumbnails.MINI_KIND, null)
            else -> {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(entry.path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= SIZE && bounds.outHeight / (sample * 2) >= SIZE) sample *= 2
                BitmapFactory.decodeFile(entry.path, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    }
}

package io.github.supermonster003.autojs6.plugin.three.slate.studio

import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Shared file access for the JVM guard tests, which read the repository sources directly.
 * zh-CN: JVM 守卫测试共用的文件访问工具, 直接读取仓库源文件.
 */
internal object RepositoryFiles {

    const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

    /** The repository root: the closest ancestor of the working directory that holds `app/src/main`. */
    val root: Path by lazy {
        generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main")) }
    }

    fun path(relative: String): Path = root.resolve(relative)

    fun text(relative: String): String = Files.readString(path(relative))

    fun xml(relative: String): Element {
        val file = path(relative)
        check(Files.isRegularFile(file)) { "Missing $relative" }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(file.toFile()).documentElement
    }

    fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == tag }
    }

    fun Element.child(tag: String): Element = children(tag).single()

    fun Element.android(name: String): String = androidOrNull(name) ?: error("Missing android:$name on <$tagName>")

    fun Element.androidOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NAMESPACE, name)) getAttributeNS(ANDROID_NAMESPACE, name) else null

    /** `<string>` resources of one values directory in file order, with Android escapes resolved. */
    fun strings(directory: String): LinkedHashMap<String, String> = LinkedHashMap<String, String>().also { map ->
        xml("app/src/main/res/$directory/strings.xml").children("string").forEach { element ->
            map[element.getAttribute("name")] = element.textContent.replace("\\'", "'").replace("\\\"", "\"")
        }
    }

    /** Non-ASCII punctuation, arrows, the ideographic space and full-width forms; U+00B7 stays allowed. */
    fun nonAsciiPunctuation(text: String): List<Int> = buildList {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (codePoint > 0x7F && codePoint != 0xB7 && (isPunctuation(codePoint) || codePoint in 0x2190..0x21FF ||
                    codePoint == 0x3000 || codePoint in 0xFF01..0xFF65)
            ) {
                add(codePoint)
            }
            index += Character.charCount(codePoint)
        }
    }

    private fun isPunctuation(codePoint: Int): Boolean = when (Character.getType(codePoint).toByte()) {
        Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
        Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
        Character.OTHER_PUNCTUATION,
        -> true
        else -> false
    }

}

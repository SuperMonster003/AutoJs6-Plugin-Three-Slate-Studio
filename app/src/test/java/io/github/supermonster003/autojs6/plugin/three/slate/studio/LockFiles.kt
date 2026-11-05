package io.github.supermonster003.autojs6.plugin.three.slate.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Reads the `locks/` files with the same rules as `app/build.gradle.kts`: comments and blank lines are skipped, keys are
 * unique, `format=1` comes first and every digest is lowercase hex.
 * zh-CN: 以与 `app/build.gradle.kts` 相同的规则读取 `locks/` 文件: 跳过注释与空行, 键唯一, 首行为 `format=1`, 摘要均为小写十六进制.
 */
internal object LockFiles {

    fun read(relative: String): Map<String, String> {
        val lines = Files.readAllLines(RepositoryFiles.path(relative)).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        assertEquals("$relative must start with format=1", "format=1", lines.first())
        val entries = LinkedHashMap<String, String>()
        lines.forEach { line ->
            assertTrue("$relative: malformed line $line", line.contains('='))
            assertTrue("$relative: duplicate key in $line", entries.put(line.substringBefore('='), line.substringAfter('=')) == null)
        }
        entries.filterKeys { it.endsWith(".sha256") }.forEach { (key, value) ->
            assertTrue("$relative: $key must be lowercase hex", Regex("[0-9a-f]{64}").matches(value))
        }
        return entries
    }

    fun sha256(relative: String): String =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(RepositoryFiles.path(relative))).joinToString("") { "%02x".format(it) }

}

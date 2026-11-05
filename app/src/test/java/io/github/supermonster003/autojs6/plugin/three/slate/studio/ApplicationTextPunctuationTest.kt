package io.github.supermonster003.autojs6.plugin.three.slate.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Every language uses ASCII punctuation (D:/idea-projects/COMMON_AGENTS.md): resources, generated documents, JSON
 * sources, code comments and repository documents. Upstream files kept byte for byte (the Gradle wrapper script and the
 * shared Icon Studio kernel) are the documented exceptions and are not scanned.
 * zh-CN: 所有语言只用 ASCII 标点 (COMMON_AGENTS.md): 资源, 生成文档, JSON 源, 代码注释与仓库文档. 原样保存的上游文件
 * (Gradle Wrapper 脚本与共享的 Icon Studio 内核) 属于规范例外, 不扫描.
 */
class ApplicationTextPunctuationTest {

    @Test
    fun `repository text uses ASCII punctuation in every language`() {
        val violations = mutableListOf<String>()
        SCANNED_ROOTS.map { RepositoryFiles.path(it) }.filter { Files.exists(it) }.forEach { scanned ->
            Files.walk(scanned).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.isScannedText() && !it.isExcluded() }.forEach { path ->
                    RepositoryFiles.nonAsciiPunctuation(Files.readString(path)).distinct().forEach { codePoint ->
                        violations += "${RepositoryFiles.root.relativize(path)}: U+%04X (%s)".format(codePoint, String(Character.toChars(codePoint)))
                    }
                }
            }
        }
        assertTrue("Text contains non-ASCII punctuation:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun `the punctuation rule covers every language and keeps the value separator`() {
        val samples = listOf(0xFF0C, 0x3002, 0x3001, 0x201C, 0x2019, 0x2026, 0x2014, 0x00AB, 0x060C, 0x061F, 0x00BF, 0x2192, 0x3000, 0xFF08)
        samples.forEach { codePoint ->
            assertEquals("U+%04X must be reported".format(codePoint), listOf(codePoint), RepositoryFiles.nonAsciiPunctuation(String(Character.toChars(codePoint))))
        }
        assertTrue(RepositoryFiles.nonAsciiPunctuation("1.0 \u00b7 2.0, r\u00e9gulier, \u6587\u4ef6, \u0645\u0644\u0641").isEmpty())
    }

    private fun Path.isScannedText(): Boolean = fileName.toString().substringAfterLast('.', "") in EXTENSIONS ||
        fileName.toString() in NAMES

    private fun Path.isExcluded(): Boolean {
        val relative = RepositoryFiles.root.relativize(this).toString().replace('\\', '/')
        return relative in EXCLUDED_FILES || relative.split('/').any { it == "build" || it == ".gradle" || it == ".kotlin" }
    }

    private companion object {
        val SCANNED_ROOTS = listOf(
            "app/src", "build-logic/convention/src", ".changelog", ".readme", ".python", ".icons/recipe.json", ".github", "docs",
            "README.md", "ROADMAP.md", "AGENTS.md", "THIRD_PARTY_NOTICES.md", "libs/README.md", "locks", "gradle/libs.versions.toml",
            "settings.gradle.kts", "build.gradle.kts", "app/build.gradle.kts", "app/proguard-rules.pro", "version.properties",
            ".gitignore", ".gitattributes",
        )
        val EXTENSIONS = setOf("xml", "md", "json", "kt", "kts", "java", "py", "bat", "pro", "toml", "properties", "lock", "yml", "aidl")
        val NAMES = setOf(".gitignore", ".gitattributes")
        // Upstream files kept byte for byte (COMMON_AGENTS.md exception): the shared Icon Studio kernel.
        val EXCLUDED_FILES = setOf(".python/icon_studio_runtime.py")
    }

}

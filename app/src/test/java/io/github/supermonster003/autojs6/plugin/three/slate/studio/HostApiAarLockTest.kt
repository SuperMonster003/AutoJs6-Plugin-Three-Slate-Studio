package io.github.supermonster003.autojs6.plugin.three.slate.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.zip.ZipFile

/**
 * The host API lock, the staged AARs and the human-readable records describe the same bytes (roadmap 4.5, P0.1).
 * Gradle enforces the lock during configuration; this test keeps `libs/README.md` and `THIRD_PARTY_NOTICES.md` honest
 * and pins the artifact set (`common-plugin-api` and `media-studio-api` of roadmap P1.1).
 * zh-CN: 宿主 API 锁文件, 暂存的 AAR 与人读记录描述同一份字节 (路线图 4.5, P0.1). Gradle 在配置阶段强制锁文件; 本测试
 * 保证 `libs/README.md` 与 `THIRD_PARTY_NOTICES.md` 如实记录, 并固定产物集合 (路线图 P1.1 的 `common-plugin-api` 与 `media-studio-api`).
 */
class HostApiAarLockTest {

    private val lock: Map<String, String> by lazy { LockFiles.read("locks/host-api-aars.lock") }

    private val ids: Set<String> get() = lock.keys.filter { it.endsWith(".file") }.map { it.removeSuffix(".file") }.toSet()

    @Test
    fun `the lock lists the staged host AARs with matching digests`() {
        assertEquals(setOf("common-plugin-api", "media-studio-api"), ids)
        assertEquals(setOf("format") + ids.flatMap { listOf("$it.file", "$it.sha256") }, lock.keys)
        ids.forEach { id ->
            val file = lock.getValue("$id.file")
            assertEquals("$id is staged under its module name", "$id.aar", file)
            assertEquals("$id: digest drifted from the lock", lock.getValue("$id.sha256"), LockFiles.sha256("libs/$file"))
        }
    }

    @Test
    fun `libs holds exactly the locked host AARs`() {
        Files.list(RepositoryFiles.path("libs")).use { files ->
            val staged = files.map { it.fileName.toString() }.filter { it.endsWith(".aar") || it.endsWith(".jar") }.sorted().toList()
            assertEquals(ids.map { lock.getValue("$it.file") }.sorted(), staged)
        }
    }

    @Test
    fun `the staged host AARs are release bytecode without native code`() {
        ids.forEach { id ->
            val file = lock.getValue("$id.file")
            assertFalse("$id: debug AARs are not accepted", file.contains("debug"))
            val names = ZipFile(RepositoryFiles.path("libs/$file").toFile()).use { zip -> zip.entries().asSequence().map { it.name }.toList() }
            assertTrue("$id must contain classes.jar", "classes.jar" in names)
            assertTrue("$id must not ship native libraries (roadmap D25)", names.none { it.startsWith("jni/") })
        }
    }

    @Test
    fun `the human-readable records repeat every locked host AAR`() {
        val readme = RepositoryFiles.text("libs/README.md")
        val notices = RepositoryFiles.text("THIRD_PARTY_NOTICES.md")
        ids.forEach { id ->
            val file = lock.getValue("$id.file")
            assertTrue("libs/README.md must describe $file", readme.contains("`$file`"))
            assertTrue("libs/README.md must repeat the digest of $id", readme.contains(lock.getValue("$id.sha256")))
            assertTrue("THIRD_PARTY_NOTICES.md must list $file", notices.contains("`libs/$file`"))
        }
    }

    @Test
    fun `the build script reads the lock instead of listing files`() {
        val build = RepositoryFiles.text("app/build.gradle.kts")
        assertTrue(build.contains("readLock(\"locks/host-api-aars.lock\")"))
        assertTrue(build.contains("implementation(files(hostApiAars))"))
        assertFalse("no sibling repository paths", build.contains("../AutoJs6") || build.contains("flatDir"))
        assertFalse("no fileTree dependencies", build.contains("fileTree("))
        assertFalse("no mavenLocal()", RepositoryFiles.text("settings.gradle.kts").contains("mavenLocal(") || build.contains("mavenLocal("))
    }

}

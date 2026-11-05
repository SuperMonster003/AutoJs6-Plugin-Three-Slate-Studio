package io.github.supermonster003.autojs6.plugin.three.slate.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps `THIRD_PARTY_NOTICES.md` in step with `locks/runtime-dependencies.lock` (roadmap D39, P0.1). Gradle's
 * verifyRuntimeDependencyLock ties the lock to the resolved release classpath; this test ties the generated notices to
 * the lock, so `test` fails when the generator was not rerun.
 * zh-CN: 使 `THIRD_PARTY_NOTICES.md` 与 `locks/runtime-dependencies.lock` 同步 (路线图 D39, P0.1). Gradle 的
 * verifyRuntimeDependencyLock 把锁与解析出的 release 类路径绑定; 本测试把生成的声明与锁绑定, 未重跑生成器时 `test` 失败.
 */
class ThirdPartyNoticesTest {

    private val lock: Map<String, String> by lazy { LockFiles.read(RUNTIME_LOCK) - "format" }

    private val notices: String by lazy { RepositoryFiles.text("THIRD_PARTY_NOTICES.md") }

    @Test
    fun `every locked runtime artifact is listed once with its version`() {
        lock.forEach { (coordinate, version) ->
            assertTrue("THIRD_PARTY_NOTICES.md must list $coordinate $version", notices.contains("| `$coordinate` | $version |"))
        }
        val listed = Regex("""^\| `([^`/]+:[^`]+)` \| """, RegexOption.MULTILINE).findAll(notices).map { it.groupValues[1] }.toList()
        assertEquals(lock.keys.sorted(), listed.sorted())
        assertTrue(notices.contains("packages ${lock.size} Maven artifacts"))
    }

    @Test
    fun `the lock is sorted and holds release artifacts only`() {
        assertEquals(lock.keys.sorted(), lock.keys.toList())
        assertTrue("the Compose native library dependency (roadmap D25, Q9)", lock.containsKey("androidx.graphics:graphics-path"))
        listOf("androidx.media3:media3-transformer", "androidx.media3:media3-effect", "androidx.media3:media3-muxer").forEach {
            assertTrue("$it is a processing dependency (roadmap D29)", lock.containsKey(it))
        }
        lock.keys.forEach { coordinate ->
            assertFalse("$coordinate is a test or debug artifact", TEST_OR_DEBUG.any { coordinate.startsWith(it) })
            assertFalse("$coordinate: BOMs ship nothing", coordinate.endsWith("-bom"))
        }
    }

    @Test
    fun `the notices carry the MIT text of the Checker Framework qualifiers and the FFmpeg note`() {
        assertEquals("3.43.0", lock["org.checkerframework:checker-qual"])
        assertTrue(notices.contains("Copyright 2004-present by the Checker Framework developers"))
        assertTrue(notices.contains("The FFmpeg library of version 1.1.0 is not part of this APK"))
        assertTrue(notices.contains("LGPL-2.1-or-later"))
    }

    private companion object {
        const val RUNTIME_LOCK = "locks/runtime-dependencies.lock"
        val TEST_OR_DEBUG = listOf(
            "junit:", "org.hamcrest:", "androidx.test", "androidx.compose.ui:ui-test", "androidx.compose.ui:ui-tooling-android",
            "androidx.compose.ui:ui-tooling-data", "androidx.compose.ui:ui-tooling=",
        )
    }

}

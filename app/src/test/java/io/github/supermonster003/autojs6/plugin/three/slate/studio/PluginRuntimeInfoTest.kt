package io.github.supermonster003.autojs6.plugin.three.slate.studio

import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.pluginRuntimeFields
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioPluginActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure PluginInfo field mapping and the identity published by the build, the Manifest and the README sources
 * (roadmap 5.5, D25, D28). The Android-backed `toPluginInfo()` / `capabilities()` mapping is exercised by the device
 * contract test.
 * zh-CN: 纯数据的 PluginInfo 字段映射, 以及构建, Manifest 与 README 源发布的身份 (路线图 5.5, D25, D28). 依赖 Android 的
 * `toPluginInfo()` / `capabilities()` 映射由设备契约测试覆盖.
 */
class PluginRuntimeInfoTest {

    @Test
    fun `runtime fields keep the identity, the package version, no ABI and the implemented operations`() {
        val fields = pluginRuntimeFields(
            name = "3-Slate Studio",
            description = "Media",
            author = "SuperMonster003",
            id = "three-slate-studio",
            engine = "media-studio",
            variant = "default",
            versionName = "1.0.0",
            versionCode = 2L,
            versionDate = "Nov 5, 2026",
        )
        assertEquals("3-Slate Studio", fields.name)
        assertEquals("Media", fields.description)
        assertEquals("@raw/plugin_instruction", fields.instruction)
        assertEquals(ThreeSlateStudioContract.INSTRUCTION_REFERENCE, fields.instruction)
        assertEquals("SuperMonster003", fields.author)
        assertEquals("three-slate-studio", fields.id)
        assertEquals("media-studio", fields.engine)
        assertEquals("default", fields.variant)
        assertEquals("1.0.0", fields.versionName)
        assertEquals(2L, fields.versionCode)
        assertEquals("Nov 5, 2026", fields.versionDate)
        // Roadmap D25 / Q9: the Compose library exists for all four ABIs, so the universal APK reports an empty list.
        assertEquals(emptyList<String>(), fields.supportedAbis)
        // Roadmap D23: P1.5 confirmed 6641 as the local build with the contract; P5.4 writes the final one.
        assertEquals(6641L, fields.requiredHostVersion)
        assertEquals(ThreeSlateStudioContract.REQUIRED_HOST_VERSION, fields.requiredHostVersion)
        assertEquals(1, fields.contractVersion)
        assertEquals("P2.1 / P2.2 implement probe and the basic operations", ThreeSlateStudioContract.IMPLEMENTED_OPS, fields.ops)
        assertTrue(fields.ops.containsAll(listOf(MediaStudioOps.PROBE, MediaStudioOps.TRIM, MediaStudioOps.TRANSCODE, MediaStudioOps.AUDIO_PROBE)))
        assertTrue("compose comes with P3.1", MediaStudioOps.COMPOSE in fields.ops)
        assertFalse("burnSubtitles comes with P3.3", MediaStudioOps.BURN_SUBTITLES in fields.ops)
    }

    @Test
    fun `identity constants follow roadmap 5_5 and the host contract`() {
        assertEquals("three-slate-studio", ThreeSlateStudioContract.PLUGIN_ID)
        assertEquals("media-studio", ThreeSlateStudioContract.ENGINE)
        assertEquals("default", ThreeSlateStudioContract.VARIANT)
        assertEquals("org.autojs.plugin.INFO", ThreeSlateStudioContract.INFO_ACTION)
        assertEquals(ThreeSlateStudioContract.PLUGIN_ID, ThreeSlateStudioContract.INFO_CATEGORY)
        assertEquals("org.autojs.plugin.MEDIA_STUDIO", ThreeSlateStudioContract.SERVICE_ACTION)
        assertEquals(MediaStudioPluginActions.MEDIA_STUDIO, ThreeSlateStudioContract.SERVICE_ACTION)
        assertEquals("media-studio", ThreeSlateStudioContract.SERVICE_CATEGORY)
        assertEquals(1, ThreeSlateStudioContract.CONTRACT_VERSION)
        assertEquals(MediaStudioContract.CONTRACT_VERSION, ThreeSlateStudioContract.CONTRACT_VERSION)
        assertTrue(MediaStudioOps.ALL.containsAll(ThreeSlateStudioContract.IMPLEMENTED_OPS))
        assertEquals("NOT_IMPLEMENTED", ThreeSlateStudioContract.NOT_IMPLEMENTED)
    }

    @Test
    fun `the build publishes the same identity as the constants and builds one universal APK`() {
        val build = RepositoryFiles.text("app/build.gradle.kts")
        assertTrue(build.contains("val globalApplicationId = \"$PACKAGE_NAME\""))
        assertTrue(build.contains("namespace = globalApplicationId"))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("resValue(\"string\", \"app_name\", \"3-Slate Studio\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_name\", \"3-Slate Studio\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_author\", \"SuperMonster003\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_id\", \"${ThreeSlateStudioContract.PLUGIN_ID}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_engine\", \"${ThreeSlateStudioContract.ENGINE}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_variant\", \"${ThreeSlateStudioContract.VARIANT}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_requires_host_version\", \"\$requiredHostVersion\")"))
        // The minimum host build is changed in the build script, the constant and the README sources together.
        assertTrue(build.contains("val requiredHostVersion = ${ThreeSlateStudioContract.REQUIRED_HOST_VERSION}\n"))
        // Roadmap D25: one universal APK; the release task allows exactly the Compose library, stored uncompressed.
        assertFalse("no ABI splits (roadmap D25)", build.contains("splits {"))
        assertTrue(build.contains("abi.enableSplit = false"))
        assertTrue(build.contains("register(\"appendDigestToReleasedFiles\")"))
        assertTrue(
            build.contains(
                "val allowedNativeLibraries = listOf(\"arm64-v8a\", \"armeabi-v7a\", \"x86_64\", \"x86\").map { \"lib/\$it/libandroidx.graphics.path.so\" }",
            ),
        )
        assertTrue(build.contains("nativeAlignment { expectNoNativeLibraries.set(false) }"))
        assertTrue(build.contains("check(natives.all { it.method == ZipEntry.STORED })"))
        assertTrue(build.contains("ApkVerifier.Builder(apk).build().verify()"))
        // Roadmap D39: the runtime dependency lock gates `check` and release builds.
        assertTrue(build.contains("named(\"check\") { dependsOn(\"verifyRuntimeDependencyLock\") }"))
        assertTrue(build.contains("matching { it.name == \"preReleaseBuild\" }.configureEach { dependsOn(\"verifyRuntimeDependencyLock\") }"))

        val settings = RepositoryFiles.text("settings.gradle.kts")
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-three-slate-studio\""))
    }

    @Test
    fun `the README sources publish the same identity as the constants`() {
        val common = RepositoryFiles.text(".readme/common.json")
        mapOf(
            "plugin_application_id" to PACKAGE_NAME,
            "plugin_id" to ThreeSlateStudioContract.PLUGIN_ID,
            "plugin_engine" to ThreeSlateStudioContract.ENGINE,
            "plugin_variant" to ThreeSlateStudioContract.VARIANT,
            "info_action" to ThreeSlateStudioContract.INFO_ACTION,
            "info_category" to ThreeSlateStudioContract.INFO_CATEGORY,
            "discovery_action" to ThreeSlateStudioContract.SERVICE_ACTION,
            "discovery_category" to ThreeSlateStudioContract.SERVICE_CATEGORY,
            "required_host_version_code" to ThreeSlateStudioContract.REQUIRED_HOST_VERSION.toString(),
            "app_name" to "3-Slate Studio",
        ).forEach { (key, value) ->
            assertTrue(".readme/common.json must publish $key = $value", common.contains("\"$key\": \"$value\""))
        }
    }

    @Test
    fun `the package of the Kotlin sources is the application id`() {
        assertEquals(PACKAGE_NAME, ThreeSlateStudioContract::class.java.packageName)
    }

    private companion object {
        const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
    }

}

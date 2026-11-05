package io.github.supermonster003.autojs6.plugin.three.slate.studio

import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.android
import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.androidOrNull
import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.child
import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.children
import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Keeps `AndroidManifest.xml` and [ThreeSlateStudioContract] from drifting apart: the host discovers the plugin through
 * the Manifest (Wake activity, INFO and MEDIA_STUDIO services), while the services and the tests use the Kotlin
 * constants (roadmap 5.5, D9, D16, D25, D41).
 * zh-CN: 防止 `AndroidManifest.xml` 与 [ThreeSlateStudioContract] 漂移: 宿主经 Manifest 发现插件 (Wake Activity, INFO 与
 * MEDIA_STUDIO 服务), 服务与测试则使用 Kotlin 常量 (路线图 5.5, D9, D16, D25, D41).
 */
class ManifestContractTest {

    private val manifest: Element by lazy { RepositoryFiles.xml("app/src/main/AndroidManifest.xml") }

    private val application: Element get() = manifest.child("application")

    @Test
    fun `the permissions are the plugin permission, storage, the reserved recorder set and the network`() {
        assertEquals(
            listOf(
                PLUGIN_PERMISSION,
                "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                "android.permission.RECORD_AUDIO",
                "android.permission.FOREGROUND_SERVICE_MICROPHONE",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION",
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
            ),
            manifest.children("uses-permission").map { it.android("name") },
        )
        val permissions = manifest.children("uses-permission").associateBy { it.android("name") }
        assertEquals("32", permissions.getValue("android.permission.READ_EXTERNAL_STORAGE").android("maxSdkVersion"))
        assertEquals("29", permissions.getValue("android.permission.WRITE_EXTERNAL_STORAGE").android("maxSdkVersion"))
        assertTrue("no uses-sdk override: SDK levels come from version.properties", manifest.children("uses-sdk").isEmpty())
        assertTrue("no feature requirements", manifest.children("uses-feature").isEmpty())
    }

    @Test
    fun `package queries name the host only`() {
        val queries = manifest.child("queries")
        assertEquals(listOf("org.autojs.autojs6"), queries.children("package").map { it.android("name") })
        assertTrue(queries.children("intent").isEmpty())
    }

    @Test
    fun `the application disables backups and uses the generated title and system icon`() {
        assertEquals("false", application.android("allowBackup"))
        assertEquals("false", application.android("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.android("dataExtractionRules"))
        assertEquals("@string/app_name", application.android("label"))
        assertEquals("true", application.android("supportsRtl"))
        assertEquals("true", application.android("requestLegacyExternalStorage"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("icon"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("roundIcon"))
        assertEquals(".ThreeSlateStudioApplication", application.android("name"))
        assertNull("16 KB aligned libraries stay uncompressed in the APK (roadmap Q9)", application.androidOrNull("extractNativeLibs"))
    }

    @Test
    fun `application meta-data names the wake activity, the author and the 16 KB page alignment`() {
        val metaData = application.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(
            mapOf(
                PluginCapabilityKeys.NATIVE_PAGE_ALIGNMENT to "16384",
                "org.autojs.plugin.WAKE_ACTIVITY" to ".WakeActivity",
                "org.autojs.plugin.info.AUTHOR" to "@string/plugin_author",
            ),
            metaData,
        )
    }

    @Test
    fun `the wake activity follows the activation contract`() {
        val wake = activity(".WakeActivity")
        assertEquals("true", wake.android("exported"))
        assertEquals("true", wake.android("excludeFromRecents"))
        assertEquals("true", wake.android("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.android("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.android("theme"))
        assertFilter(wake, action = "org.autojs.plugin.action.WAKE", category = DEFAULT_CATEGORY)
    }

    @Test
    fun `the activities are the wake and home screens`() {
        assertEquals(listOf(".WakeActivity", ".ui.home.HomeActivity"), application.children("activity").map { it.android("name") })
        val home = activity(".ui.home.HomeActivity")
        assertEquals("false", home.android("exported"))
        assertTrue("the launcher reaches the home screen through the aliases only", home.children("intent-filter").isEmpty())
    }

    @Test
    fun `four launcher aliases point at the home screen and only the automatic one is enabled`() {
        val aliases = application.children("activity-alias")
        assertEquals(
            listOf(
                ".launcher.AdaptiveLightIconAlias" to "@mipmap/ic_launcher_system_light",
                ".launcher.AdaptiveDarkIconAlias" to "@mipmap/ic_launcher_system",
                ".launcher.AdaptiveAutoIconAlias" to "@mipmap/ic_launcher_system_auto",
                ".launcher.TransparentIconAlias" to "@mipmap/ic_launcher",
            ),
            aliases.map { it.android("name") to it.android("icon") },
        )
        aliases.forEach { alias ->
            assertEquals(".ui.home.HomeActivity", alias.android("targetActivity"))
            assertEquals(alias.android("icon"), alias.android("roundIcon"))
            assertEquals("@string/app_name", alias.android("label"))
            assertEquals("true", alias.android("exported"))
            assertEquals(listOf("android.intent.action.MAIN"), alias.child("intent-filter").children("action").map { it.android("name") })
            assertEquals(listOf(LAUNCHER_CATEGORY), alias.child("intent-filter").children("category").map { it.android("name") })
        }
        assertEquals(listOf(".launcher.AdaptiveAutoIconAlias"), aliases.filter { it.android("enabled") == "true" }.map { it.android("name") })
    }

    @Test
    fun `the INFO and MEDIA_STUDIO services match the discovery constants`() {
        assertEquals(
            listOf(".service.ThreeSlateStudioInfoService", ".service.ThreeSlateStudioService"),
            application.children("service").map { it.android("name") },
        )
        val info = service(".service.ThreeSlateStudioInfoService")
        assertFilter(info, action = PluginActions.INFO, category = ThreeSlateStudioContract.INFO_CATEGORY)
        assertEquals(ThreeSlateStudioContract.INFO_ACTION, PluginActions.INFO)
        assertRequiresHostVersion(info)

        val capability = service(".service.ThreeSlateStudioService")
        assertFilter(capability, action = ThreeSlateStudioContract.SERVICE_ACTION, category = ThreeSlateStudioContract.SERVICE_CATEGORY)
        assertRequiresHostVersion(capability)

        listOf(info, capability).forEach { service ->
            assertEquals("true", service.android("exported"))
            assertEquals(PLUGIN_PERMISSION, service.android("permission"))
            assertNull("services run in the plugin's default process", service.androidOrNull("process"))
            // The task and recorder foreground services arrive with roadmap P2.1 / P4.5.
            assertNull("discovery services are not foreground services", service.androidOrNull("foregroundServiceType"))
        }
    }

    @Test
    fun `every exported component requires the plugin permission`() {
        val components = listOf("activity", "service", "receiver", "provider").flatMap { application.children(it) }
        val exported = components.filter { it.androidOrNull("exported") == "true" }
        assertEquals(emptyList<String>(), exported.filter { it.androidOrNull("permission") != PLUGIN_PERMISSION }.map { it.android("name") })
        assertTrue("no receivers in the skeleton", application.children("receiver").isEmpty())
        assertTrue("no providers in the skeleton", application.children("provider").isEmpty())
        // Launcher aliases are the only components exported without a permission: the launcher must start them.
        application.children("activity-alias").forEach { assertNull(it.androidOrNull("permission")) }
    }

    private fun activity(name: String): Element = application.children("activity").single { it.android("name") == name }

    private fun service(name: String): Element = application.children("service").single { it.android("name") == name }

    private fun assertFilter(component: Element, action: String, category: String) {
        val filter = component.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.android("name") })
        assertEquals(listOf(category), filter.children("category").map { it.android("name") })
        assertTrue(filter.children("data").isEmpty())
    }

    private fun assertRequiresHostVersion(service: Element) {
        val metaData = service.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(mapOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION to "@string/plugin_requires_host_version"), metaData)
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val DEFAULT_CATEGORY = "android.intent.category.DEFAULT"
        const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"
    }

}

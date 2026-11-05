package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.ThreeSlateStudioInfoService
import io.github.supermonster003.autojs6.plugin.three.slate.studio.service.ThreeSlateStudioService
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mediastudio.api.IMediaStudioCallback
import org.autojs.plugin.mediastudio.api.IMediaStudioPlugin
import org.autojs.plugin.mediastudio.api.MediaStudioCapabilityKeys
import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioRequestKeys
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The host-facing activation and discovery contract against the installed APK (roadmap P0.3): the Wake activity,
 * discovery of exactly one service per formal action and category, explicit binding with the expected Binder descriptors,
 * `getInfo()` marshalled through a real Parcel with the transaction code the host uses, and the `IMediaStudioPlugin`
 * skeleton (capabilities, `NOT_IMPLEMENTED` rejections, empty task list).
 *
 * The services run in the plugin's own process, so the calls below use `transact` with explicit Parcels or a
 * [MarshallingBinder] instead of plain `asInterface`, which would return the local object and skip marshalling.
 *
 * zh-CN: 针对已安装 APK 验证面向宿主的激活与发现契约 (路线图 P0.3): Wake Activity, 每组正式 action 与 category 恰好发现一个服务,
 * 显式绑定与 Binder 描述符, 以宿主使用的事务号经真实 Parcel 编组的 `getInfo()`, 以及 `IMediaStudioPlugin` 骨架 (能力,
 * `NOT_IMPLEMENTED` 拒绝, 空任务列表). 服务与测试同进程, 因此下文用显式 Parcel 调用 `transact` 或经 [MarshallingBinder],
 * 不直接用会返回本地对象而跳过编组的 `asInterface`.
 */
@RunWith(AndroidJUnit4::class)
class ThreeSlateStudioContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val metaData = requireNotNull(context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData)
        assertEquals(".WakeActivity", metaData.getString("org.autojs.plugin.WAKE_ACTIVITY"))
        assertEquals(context.getString(R.string.plugin_author), metaData.getString("org.autojs.plugin.info.AUTHOR"))
        assertEquals(16384, metaData.getInt(PluginCapabilityKeys.NATIVE_PAGE_ALIGNMENT))

        val component = ComponentName(packageName, "$packageName.WakeActivity")
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue(activityInfo.exported)
        assertTrue(activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val matches = queryActivities(Intent("org.autojs.plugin.action.WAKE").addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName))
        assertEquals("the WAKE action resolves exactly one activity", listOf(component.className), matches)
    }

    @Test
    fun launcherResolvesTheAutomaticIconAliasOnly() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        assertEquals(listOf("$packageName.launcher.AdaptiveAutoIconAlias"), queryActivities(launcher))
    }

    @Test
    fun infoServiceAnswersTheMediaStudioIdentity() {
        val serviceInfo = discoverSingleService(
            ThreeSlateStudioContract.INFO_ACTION, ThreeSlateStudioContract.INFO_CATEGORY, ThreeSlateStudioInfoService::class.java.name,
        )
        assertRequiresHostVersion(serviceInfo)
        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = transactPluginInfo(binder)
            assertIdentity(info)
            val capabilities = requireNotNull(info.capabilities)
            assertEquals(
                setOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION, MediaStudioCapabilityKeys.CONTRACT_VERSION, MediaStudioCapabilityKeys.OPS),
                capabilities.keySet(),
            )
            assertSkeletonCapabilities(capabilities)
        }
    }

    @Test
    fun capabilityServiceAnswersTheMediaStudioContract() {
        val serviceInfo = discoverSingleService(
            ThreeSlateStudioContract.SERVICE_ACTION, ThreeSlateStudioContract.SERVICE_CATEGORY, ThreeSlateStudioService::class.java.name,
        )
        assertRequiresHostVersion(serviceInfo)
        withBoundService(serviceInfo) { binder ->
            assertEquals(IMediaStudioPlugin.DESCRIPTOR, binder.interfaceDescriptor)
            val plugin = IMediaStudioPlugin.Stub.asInterface(MarshallingBinder(binder))
            assertIdentity(plugin.info)
            val capabilities = plugin.capabilities
            assertEquals(ThreeSlateStudioContract.CONTRACT_VERSION, capabilities.getInt(MediaStudioContract.KEY_CONTRACT_VERSION))
            assertSkeletonCapabilities(capabilities)
        }
    }

    @Test
    fun submitRejectsEveryOperationUntilTheProcessingCoreLands() {
        withMediaStudio { plugin ->
            val callback = object : IMediaStudioCallback.Stub() {
                override fun onProgress(taskId: Int, progress: Bundle?) = Unit
                override fun onComplete(taskId: Int, result: Bundle?) = Unit
                override fun onError(taskId: Int, error: Bundle?) = Unit
            }
            MediaStudioOps.ALL.forEach { op ->
                val error = assertThrows(IllegalStateException::class.java) { plugin.submit(request(op), callback) }
                assertEquals(MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "NOT_IMPLEMENTED: $op"), error.message)
            }
            val unknown = assertThrows(IllegalArgumentException::class.java) { plugin.submit(request("explode"), callback) }
            assertEquals(MediaStudioErrorCodes.encode(MediaStudioErrorCodes.INVALID_OPTIONS, "unknown op explode"), unknown.message)
            assertThrows(IllegalArgumentException::class.java) { plugin.submit(request(MediaStudioOps.TRIM), null) }
        }
    }

    @Test
    fun probeCancelAndListTasksAnswerTheSkeletonState() {
        withMediaStudio { plugin ->
            val probe = plugin.probe(Bundle().apply { putString(MediaStudioRequestKeys.INPUTS, "/sdcard/a.mp4") })
            assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, probe.getString(MediaStudioContract.KEY_ERROR_CODE))
            assertEquals("NOT_IMPLEMENTED: ${MediaStudioOps.PROBE}", probe.getString(MediaStudioContract.KEY_ERROR_MESSAGE))
            assertEquals(ThreeSlateStudioContract.CONTRACT_VERSION, probe.getInt(MediaStudioContract.KEY_CONTRACT_VERSION))
            assertFalse(plugin.cancel(1))
            val tasks = plugin.listTasks(Bundle())
            assertEquals(ThreeSlateStudioContract.CONTRACT_VERSION, tasks.getInt(MediaStudioContract.KEY_CONTRACT_VERSION))
            tasks.classLoader = Bundle::class.java.classLoader
            @Suppress("DEPRECATION")
            assertEquals(0, requireNotNull(tasks.getParcelableArray(MediaStudioResultKeys.TASKS)).size)
        }
    }

    @Test
    fun repeatedBindingsAnswerTheSameIdentity() {
        val serviceInfo = discoverSingleService(
            ThreeSlateStudioContract.INFO_ACTION, ThreeSlateStudioContract.INFO_CATEGORY, ThreeSlateStudioInfoService::class.java.name,
        )
        val ids = (1..3).map {
            var id: String? = null
            withBoundService(serviceInfo) { binder -> id = transactPluginInfo(binder).id }
            id
        }
        assertEquals(List(3) { ThreeSlateStudioContract.PLUGIN_ID }, ids)
    }

    private fun request(op: String): Bundle = Bundle().apply {
        putString(MediaStudioRequestKeys.OP, op)
        putStringArray(MediaStudioRequestKeys.INPUTS, arrayOf("/storage/emulated/0/Movies/clip.mp4"))
        putString(MediaStudioRequestKeys.HOST_TOKEN, "contract-test")
    }

    private fun withMediaStudio(block: (IMediaStudioPlugin) -> Unit) {
        val serviceInfo = discoverSingleService(
            ThreeSlateStudioContract.SERVICE_ACTION, ThreeSlateStudioContract.SERVICE_CATEGORY, ThreeSlateStudioService::class.java.name,
        )
        withBoundService(serviceInfo) { binder -> block(IMediaStudioPlugin.Stub.asInterface(MarshallingBinder(binder))) }
    }

    private fun assertSkeletonCapabilities(capabilities: Bundle) {
        assertEquals(ThreeSlateStudioContract.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
        assertEquals(ThreeSlateStudioContract.CONTRACT_VERSION, capabilities.getInt(MediaStudioCapabilityKeys.CONTRACT_VERSION))
        // No operation before roadmap P2: an explicit empty array. zh-CN: 路线图 P2 之前没有操作: 显式空数组.
        assertArrayEquals(emptyArray<String>(), requireNotNull(capabilities.getStringArray(MediaStudioCapabilityKeys.OPS)))
    }

    private fun assertRequiresHostVersion(serviceInfo: ServiceInfo) {
        assertEquals(
            ThreeSlateStudioContract.REQUIRED_HOST_VERSION.toString(),
            serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString(),
        )
    }

    private fun assertIdentity(info: PluginInfo) {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        assertEquals("3-Slate Studio", info.name)
        assertEquals(context.getString(R.string.plugin_name), info.name)
        assertEquals(context.getString(R.string.plugin_description), info.description)
        assertEquals(ThreeSlateStudioContract.INSTRUCTION_REFERENCE, info.instruction)
        assertTrue("the instruction resource exists", context.resources.getIdentifier("plugin_instruction", "raw", packageName) != 0)
        assertEquals("SuperMonster003", info.author)
        assertEquals(ThreeSlateStudioContract.PLUGIN_ID, info.id)
        assertEquals(context.getString(R.string.plugin_id), info.id)
        assertEquals(ThreeSlateStudioContract.ENGINE, info.engine)
        assertEquals(ThreeSlateStudioContract.VARIANT, info.variant)
        assertEquals(packageInfo.versionName, info.versionName)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        assertEquals(versionCode, info.versionCode)
        assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
        // Roadmap D25 / Q9: one universal APK, so an explicit empty array (never null).
        assertArrayEquals(emptyArray<String>(), requireNotNull(info.supportedAbis))
    }

    /** `getInfo()` is the first transaction of IPluginInfoProvider. zh-CN: `getInfo()` 是 IPluginInfoProvider 的第一个事务. */
    private fun transactPluginInfo(binder: IBinder): PluginInfo {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(IPluginInfoProvider.DESCRIPTOR)
            assertTrue("getInfo() transaction must be handled", binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0))
            reply.readException()
            assertTrue("getInfo() must not return null", reply.readInt() != 0)
            return pluginInfoCreator.createFromParcel(reply)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun queryActivities(intent: Intent): List<String> {
        @Suppress("DEPRECATION")
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
    }

    private fun discoverSingleService(action: String, category: String, expectedClassName: String): ServiceInfo {
        val intent = Intent(action).addCategory(category).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        assertEquals("$action / $category must resolve exactly one service: ${matches.map { it.serviceInfo.name }}", 1, matches.size)
        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        assertEquals("services run in the plugin's default process", packageName, serviceInfo.processName)
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }
        val explicit = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicit, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out waiting for ${serviceInfo.name}", connected.await(10, TimeUnit.SECONDS))
            block(requireNotNull(binderReference.get()) { "${serviceInfo.name} returned a null binding" })
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"

        @Suppress("UNCHECKED_CAST")
        val pluginInfoCreator: Parcelable.Creator<PluginInfo> =
            PluginInfo::class.java.getField("CREATOR").get(null) as Parcelable.Creator<PluginInfo>
    }

}

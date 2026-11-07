package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * The application: creates the progress notification channel of the processing tasks (roadmap P2.1) and the channel of
 * the Studio recorder (roadmap P4.5). Creating a channel again only refreshes its localized name.
 * zh-CN: 应用类: 创建加工任务的进度通知渠道 (路线图 P2.1) 与 Studio 录音的渠道 (路线图 P4.5). 重复创建渠道只会刷新其本地化名称.
 */
class ThreeSlateStudioApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_TASKS, getString(R.string.notification_channel_tasks), NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        }
        val recording = NotificationChannel(CHANNEL_RECORDING, getString(R.string.notification_channel_recording), NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannels(listOf(channel, recording))
    }

    companion object {

        /** Progress of the processing tasks. zh-CN: 加工任务的进度. */
        const val CHANNEL_TASKS = "processing_tasks"

        /** The running Studio recording (roadmap P4.5). zh-CN: 进行中的 Studio 录音 (路线图 P4.5). */
        const val CHANNEL_RECORDING = "recording"

    }

}

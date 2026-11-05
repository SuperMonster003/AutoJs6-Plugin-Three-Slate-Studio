package io.github.supermonster003.autojs6.plugin.three.slate.studio.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioApplication
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home.HomeActivity
import kotlin.math.roundToInt

/**
 * The data sync foreground service of roadmap P2.1: [MediaStudioEngine] starts it once a task has run for 10 s, it shows
 * the running and queued counts (with the percent of a single running task) every second and stops itself when the
 * queue is empty. When Android refuses the start from the background the tasks keep running in the bound service.
 * zh-CN: 路线图 P2.1 的 dataSync 前台服务: 有任务运行满 10 s 时由 [MediaStudioEngine] 启动, 每秒显示运行中与排队中的个数
 * (只有一个运行中任务时附带其百分比), 队列为空时自行停止. Android 拒绝后台启动时, 任务仍在绑定的服务中继续运行.
 */
class ProcessingForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val tick = Runnable { update() }

    private var foreground = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    // ServiceCompat drops the service type below API 29, where FOREGROUND_SERVICE_TYPE_DATA_SYNC does not exist.
    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = MediaStudioEngine.get(this).queueState()
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(state), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            foreground = true
        } catch (e: RuntimeException) {
            Log.w(TAG, "cannot enter the foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (state.isIdle) {
            finish()
        } else {
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, UPDATE_INTERVAL_MILLIS)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Android 15 limits data sync services to 6 hours a day; the tasks continue in the bound service.
     * zh-CN: Android 15 限制 dataSync 服务每天 6 小时; 任务在绑定的服务中继续.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        isRunning = false
        super.onDestroy()
    }

    private fun update() {
        val state = MediaStudioEngine.peek()?.queueState()
        if (state == null || state.isIdle) {
            finish()
            return
        }
        if (canNotify()) getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification(state))
        handler.postDelayed(tick, UPDATE_INTERVAL_MILLIS)
    }

    private fun finish() {
        handler.removeCallbacks(tick)
        if (foreground) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foreground = false
        stopSelf()
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notification(state: MediaStudioEngine.QueueState): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, ThreeSlateStudioApplication.CHANNEL_TASKS)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.notification_tasks_title))
            .setContentText(getString(R.string.notification_tasks_text, state.running, state.queued))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        val percent = state.percent
        if (percent != null) builder.setProgress(100, percent.roundToInt().coerceIn(0, 100), false) else builder.setProgress(0, 0, state.running > 0)
        return builder.build()
    }

    companion object {

        private const val TAG = "SlateStudioForeground"

        const val NOTIFICATION_ID = 3_100

        const val UPDATE_INTERVAL_MILLIS = 1_000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, ProcessingForegroundService::class.java))
            } catch (e: RuntimeException) {
                // ForegroundServiceStartNotAllowedException on Android 12+ when the plugin is in the background.
                // zh-CN: 插件处于后台时, Android 12 及以上会抛 ForegroundServiceStartNotAllowedException.
                Log.w(TAG, "cannot start the foreground service", e)
            }
        }

    }

}

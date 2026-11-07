package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

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
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ThreeSlateStudioApplication
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.Formats
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.home.HomeActivity

/**
 * The foreground service of the Studio recorder (roadmap P4.5, D41): `microphone`, plus `mediaProjection` for playback
 * and mixed sources (API 29+). It enters the foreground before the projection is created (required on API 34+), holds a
 * partial wake lock so the recording continues with the screen off, and shows the elapsed time with pause / resume and
 * stop actions. The recording itself lives in [StudioRecorder]; roadmap P7.1 reuses this consent and service for screen
 * recording.
 * zh-CN: Studio 录音的前台服务 (路线图 P4.5, D41): `microphone`, 内录与混合 (API 29+) 另加 `mediaProjection`. 先进入前台再创建
 * 投影 (API 34+ 的要求), 持有部分唤醒锁使熄屏后录音继续, 通知显示已录时长并带暂停 / 继续与停止按钮. 录音本身在
 * [StudioRecorder] 中; 路线图 P7.1 的录屏复用这套授权与服务.
 */
class RecordingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { update() }
    private var projection: MediaProjection? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var foreground = false

    private val projectionCallback = object : MediaProjection.Callback() {
        // The user ended the projection (status bar chip on API 34+) or another app took it: save what was recorded.
        // zh-CN: 用户结束了投影 (API 34+ 的状态栏标识) 或被其他应用接管: 保存已录制的内容.
        override fun onStop() {
            StudioRecorder.stopFromSystem()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin(intent.getBooleanExtra(EXTRA_PLAYBACK, false))
            ACTION_STOP -> StudioRecorder.stop()
            ACTION_PAUSE -> { StudioRecorder.pause(); update() }
            ACTION_RESUME -> { StudioRecorder.resume(); update() }
            else -> if (!foreground) stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    private fun begin(playback: Boolean) {
        val settings = (StudioRecorder.phase.value as? StudioRecorder.Phase.Starting)?.settings
        try {
            // Always enter the foreground first: a service started with startForegroundService must do so.
            // zh-CN: 总是先进入前台: 以 startForegroundService 启动的服务必须如此.
            enterForeground(playback)
        } catch (error: RuntimeException) {
            // ForegroundServiceStartNotAllowedException / SecurityException: the page reports it.
            // zh-CN: ForegroundServiceStartNotAllowedException / SecurityException: 由页面报告.
            StudioRecorder.takeGrant()
            StudioRecorder.fail(settings, error)
            stopSelf()
            return
        }
        if (settings == null) {
            finish()
            return
        }
        acquireWakeLock()
        val grant = StudioRecorder.takeGrant()
        if (playback) {
            val created = try {
                val manager = getSystemService(MediaProjectionManager::class.java)
                grant?.let { manager?.getMediaProjection(it.resultCode, it.data) }
            } catch (error: RuntimeException) {
                StudioRecorder.fail(settings, error)
                finish()
                return
            }
            if (created == null) {
                StudioRecorder.fail(settings, IllegalStateException("Playback audio capture requires a media projection."))
                finish()
                return
            }
            created.registerCallback(projectionCallback, handler)
            projection = created
        }
        StudioRecorder.begin(this, projection)
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, UPDATE_INTERVAL_MILLIS)
    }

    // ServiceCompat drops the types below API 29, where they do not exist. zh-CN: API 29 以下 ServiceCompat 忽略类型.
    @SuppressLint("InlinedApi")
    private fun enterForeground(playback: Boolean) {
        val types = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && playback ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && playback -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            else -> 0
        }
        val notification = notification()
        if (types != 0) ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, types) else startForeground(NOTIFICATION_ID, notification)
        foreground = true
    }

    @SuppressLint("WakelockTimeout") // Released when the recording ends; bounded by the 24 h kernel limit plus a minute.
    private fun acquireWakeLock() {
        if (wakeLock != null) return
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "3-Slate Studio:recording").apply {
            setReferenceCounted(false)
            acquire(RecordingOptions.MAX_DURATION_LIMIT_MILLIS + 60_000L)
        }
    }

    private fun update() {
        handler.removeCallbacks(tick)
        if (!foreground) return
        if (canNotify()) runCatching { getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification()) }
        handler.postDelayed(tick, UPDATE_INTERVAL_MILLIS)
    }

    private fun finish() {
        handler.removeCallbacks(tick)
        projection?.let { active ->
            runCatching { active.unregisterCallback(projectionCallback) }
            runCatching { active.stop() }
        }
        projection = null
        wakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        wakeLock = null
        if (foreground) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foreground = false
        stopSelf()
    }

    override fun onDestroy() {
        // The system ends the service: keep what was recorded. zh-CN: 系统结束服务: 保留已录制的内容.
        if (StudioRecorder.phase.value is StudioRecorder.Phase.Active) StudioRecorder.stopFromSystem()
        finish()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notification(): Notification {
        val status = StudioRecorder.status()
        val paused = status?.state == RecordingStates.PAUSED
        val source = (StudioRecorder.phase.value.let { phase ->
            when (phase) {
                is StudioRecorder.Phase.Starting -> phase.settings
                is StudioRecorder.Phase.Active -> phase.settings
                is StudioRecorder.Phase.Saving -> phase.settings
                else -> null
            }
        })?.source ?: RecordingAudioSource.MICROPHONE
        val open = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, HomeActivity::class.java).putExtra(HomeActivity.EXTRA_ROUTE, HomeActivity.ROUTE_RECORD).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = getString(R.string.notification_recording_text, Formats.duration(status?.recordedMillis ?: 0), getString(sourceLabel(source)))
        val builder = NotificationCompat.Builder(this, ThreeSlateStudioApplication.CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_mic_24)
            .setContentTitle(getString(if (paused) R.string.record_state_paused else R.string.record_state_recording))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (paused) {
            builder.addAction(R.drawable.ic_mic_24, getString(R.string.record_resume), action(ACTION_RESUME, REQUEST_RESUME))
        } else {
            builder.addAction(R.drawable.ic_pause_24, getString(R.string.record_pause), action(ACTION_PAUSE, REQUEST_PAUSE))
        }
        builder.addAction(R.drawable.ic_stop_24, getString(R.string.record_stop), action(ACTION_STOP, REQUEST_STOP))
        return builder.build()
    }

    private fun action(name: String, request: Int): PendingIntent = PendingIntent.getService(
        this,
        request,
        Intent(this, RecordingService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {

        private const val TAG = "SlateStudioRecording"

        const val NOTIFICATION_ID = 3_200
        const val UPDATE_INTERVAL_MILLIS = 1_000L

        const val ACTION_START = "io.github.supermonster003.autojs6.plugin.three.slate.studio.record.START"
        const val ACTION_STOP = "io.github.supermonster003.autojs6.plugin.three.slate.studio.record.STOP"
        const val ACTION_PAUSE = "io.github.supermonster003.autojs6.plugin.three.slate.studio.record.PAUSE"
        const val ACTION_RESUME = "io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RESUME"
        private const val EXTRA_PLAYBACK = "playback"

        private const val REQUEST_OPEN = 1
        private const val REQUEST_PAUSE = 2
        private const val REQUEST_RESUME = 3
        private const val REQUEST_STOP = 4

        fun sourceLabel(source: RecordingAudioSource): Int = when (source) {
            RecordingAudioSource.PLAYBACK -> R.string.record_source_playback
            RecordingAudioSource.MIXED -> R.string.record_source_mixed
            else -> R.string.record_source_mic
        }

        /** Starts the service from the visible page; false when Android refuses. zh-CN: 从可见页面启动服务; Android 拒绝时为 false. */
        internal fun start(context: Context, playback: Boolean): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java).setAction(ACTION_START).putExtra(EXTRA_PLAYBACK, playback))
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "cannot start the recording service", e)
            false
        }

        private val main = Handler(Looper.getMainLooper())

        @Volatile
        private var instance: RecordingService? = null

        /** Leaves the foreground and stops the service, on the main thread. zh-CN: 在主线程上退出前台并停止服务. */
        internal fun finishRunning() {
            main.post { instance?.finish() }
        }
    }

}

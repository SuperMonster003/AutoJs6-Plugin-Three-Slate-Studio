// Copy of the AutoJs6 host kernel file app/src/main/java/org/autojs/autojs/core/record/RecordingForegroundLease.kt at host commit
// 3ef5cec633 (roadmap P1.2; unchanged up to host 063b9f0b8a), kept by 3-Slate Studio roadmap D41 / P4.5; see README.md.
// Plugin change: none besides the package (the Studio recorder passes no lease: its RecordingService is in the foreground before the kernel starts).
package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import android.content.Context

/**
 * Foreground disclosure held while a recording captures the microphone (Android requires a microphone foreground service
 * for capture outside the visible activity). The kernel acquires it before opening the microphone and releases it when
 * the capture ends; the host's implementation is the Node audio recording service.
 *
 * zh-CN: 录制采集麦克风期间持有的前台提示 (在可见界面之外采集时, Android 要求麦克风类型的前台服务). 内核在打开麦克风之前获取,
 * 采集结束时释放; 宿主的实现是 Node 录音服务.
 */
interface RecordingForegroundLease {

    /**
     * Acquires the lease for [id]. [onReady] is called once with the outcome; [onStop] is called with a reason when the
     * user or the system ends the lease (for example the notification's stop action).
     *
     * zh-CN: 为 [id] 获取租约. [onReady] 以结果调用一次; 用户或系统结束租约时 (例如通知的停止按钮) 以原因调用 [onStop].
     */
    fun acquire(context: Context, id: String, onReady: (Result<Unit>) -> Unit, onStop: (String) -> Unit)

    fun release(id: String)
}

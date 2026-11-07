@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.preview

import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.transformer.CompositionPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaProbe
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineLayout
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TimelineOptions
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P4.3 spike (appendix D Q3, D34): plays timelines with media3 `CompositionPlayer` (multiple input video graph)
 * through [PreviewComposition] into an [ImageReader] and measures what a user would see: output frame rate, frame
 * gaps (p50 / p95 / max, gaps over 100 ms), frames dropped by the player, the lateness of each frame against its
 * release time (the audio clock), the time to the first frame, extra wall time against the timeline, and seek latency.
 * Scenarios: `main` (30 s, 1080p, 8 clips with the 7 transitions of D30, one clip at 1.5x, text and image overlay, cue
 * subtitles, clip audio + music), `pip` (the same plus a second video track scaled to 40 %). Runs only with the
 * instrumentation argument `previewSpike=true` (`.python/preview_spike.py`); each scenario reports one status line
 * with the key `spike`.
 * zh-CN: 路线图 P4.3 spike (附录 D Q3, D34): 经 [PreviewComposition] 用 media3 `CompositionPlayer` (多输入视频图) 把时间线播放到
 * [ImageReader], 测用户实际看到的: 输出帧率, 帧间隔 (p50 / p95 / 最大, 超过 100 ms 的间隔), 播放器丢的帧, 每帧相对释放时刻 (音频
 * 时钟) 的迟到, 首帧耗时, 比时间线多用的墙钟时间, 跳转延迟. 场景: `main` (30 秒, 1080p, 8 段用上 D30 的 7 种转场, 一段 1.5 倍速, 文字
 * 与图片叠加, cue 字幕, 片段声音 + 音乐), `pip` (同上再加一条缩到 40 % 的视频轨). 仅在仪器参数 `previewSpike=true` 时运行
 * (`.python/preview_spike.py`); 每个场景以键 `spike` 报告一行状态.
 */
@RunWith(AndroidJUnit4::class)
class PreviewSpikeTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val directory = File(context.cacheDir, "preview-spike")

    @Test
    fun playTimelines() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("set the instrumentation argument previewSpike=true", arguments.getString("previewSpike") == "true")
        val scenarios = arguments.getString("scenarios")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: listOf("main", "pip")
        val started = SystemClock.elapsedRealtime()
        val clips = (0 until 3).map { PreviewFixtures.clip(directory, it, CLIP_US) }
        val music = PreviewFixtures.music(directory, 30_000_000L)
        val logo = PreviewFixtures.logo(directory)
        status("name=fixtures;millis=${SystemClock.elapsedRealtime() - started};sdk=${Build.VERSION.SDK_INT};model=${Build.MODEL}")
        scenarios.forEach { name ->
            val timeline = timeline(name, clips, music, logo)
            val line = runCatching { play(name, timeline) }.getOrElse { e ->
                "name=$name;error=${(e.javaClass.simpleName + ": " + e.message).clean()}"
            }
            status(line)
        }
    }

    private fun play(name: String, timeline: Map<String, Any?>): String {
        val spec = TimelineOptions.parse(timeline)
        val layout = TimelineLayout.resolve(spec, MediaProbe::probe)
        val mixStarted = SystemClock.elapsedRealtime()
        val audio = if ("audio" in flags(name)) PreviewAudio().mix(layout, File(directory, "preview-${name.hashCode()}.wav")) else null
        val mixMillis = SystemClock.elapsedRealtime() - mixStarted
        val built = PreviewComposition.build(layout, audio, directory, clock = "noclock" !in flags(name), bottomLaneFirst = "swap" in flags(name))
        val thread = HandlerThread("spike-player").apply { start() }
        val readerThread = HandlerThread("spike-reader").apply { start() }
        val handler = Handler(thread.looper)
        val outWidth = OUTPUT_WIDTH
        val outHeight = (OUTPUT_WIDTH.toLong() * layout.height / layout.width).toInt() and 1.inv()
        val reader = if (Build.VERSION.SDK_INT >= 29) {
            ImageReader.newInstance(outWidth, outHeight, PixelFormat.RGBA_8888, 4, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
        } else {
            ImageReader.newInstance(outWidth, outHeight, PixelFormat.RGBA_8888, 4)
        }
        val arrivals = CopyOnWriteArrayList<LongArray>()
        reader.setOnImageAvailableListener({ r ->
            val image = runCatching { r.acquireNextImage() }.getOrNull() ?: return@setOnImageAvailableListener
            arrivals += longArrayOf(System.nanoTime(), image.timestamp)
            image.close()
        }, Handler(readerThread.looper))
        val released = ConcurrentHashMap<Long, Long>()
        val dropped = AtomicInteger()
        val buffering = AtomicInteger()
        val error = AtomicReference<String?>()
        val ready = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val positions = CopyOnWriteArrayList<LongArray>()
        val player = AtomicReference<CompositionPlayer>()
        var playStartNs = 0L
        var preparedMillis = 0L
        val created = SystemClock.elapsedRealtime()
        handler.post {
            val p = CompositionPlayer.Builder(context).setLooper(thread.looper)
                .setVideoGraphFactory(MultipleInputVideoGraph.Factory())
                .build()
            player.set(p)
            p.setVideoFrameMetadataListener { presentationTimeUs, releaseTimeNs, _, _ -> released[releaseTimeNs] = presentationTimeUs }
            p.addAnalyticsListener(object : AnalyticsListener {
                override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
                    dropped.addAndGet(droppedFrames)
                }
            })
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> ready.countDown()
                        Player.STATE_BUFFERING -> if (playStartNs != 0L) buffering.incrementAndGet()
                        Player.STATE_ENDED -> ended.countDown()
                    }
                }

                override fun onPlayerError(e: PlaybackException) {
                    error.set("${e.errorCodeName}: ${e.message} / ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}")
                    ready.countDown()
                    ended.countDown()
                }
            })
            try {
                p.setVideoSurface(reader.surface, Size(outWidth, outHeight))
                p.setComposition(built.composition)
                p.prepare()
            } catch (e: RuntimeException) {
                error.set("${e.javaClass.simpleName}: ${e.message} at ${e.stackTrace.take(3).joinToString(" < ")}")
                ready.countDown()
                ended.countDown()
            }
        }
        try {
            check(ready.await(PREPARE_TIMEOUT_S, TimeUnit.SECONDS)) { "not ready after $PREPARE_TIMEOUT_S s" }
            error.get()?.let { throw IllegalStateException(it) }
            preparedMillis = SystemClock.elapsedRealtime() - created
            val beforePlay = arrivals.size
            handler.post {
                playStartNs = System.nanoTime()
                player.get().play()
                handler.post(object : Runnable {
                    override fun run() {
                        val p = player.get() ?: return
                        positions += longArrayOf(System.nanoTime(), p.currentPosition, if (p.isPlaying) 1 else 0)
                        if (p.playbackState != Player.STATE_ENDED) handler.postDelayed(this, SAMPLE_MS)
                    }
                })
            }
            val limit = layout.durationMillis / 1000 + PLAY_SLACK_S
            val finished = ended.await(limit, TimeUnit.SECONDS)
            val endNs = System.nanoTime()
            error.get()?.let { throw IllegalStateException(it) }
            val played = arrivals.drop(beforePlay).filter { it[0] in playStartNs..endNs }
            val seek = if (finished) seekLatencies(handler, player, arrivals) else emptyList()
            val last = positions.lastOrNull()
            val state = "ended=$finished;lastPositionMs=${last?.get(1) ?: -1};playing=${last?.get(2) ?: -1}"
            return report(name, layout, played, released, positions, playStartNs, endNs, dropped.get(), buffering.get(), preparedMillis, mixMillis, seek, built.warnings) +
                ";" + state
        } finally {
            val done = CountDownLatch(1)
            handler.post {
                runCatching { player.get()?.release() }
                done.countDown()
            }
            done.await(10, TimeUnit.SECONDS)
            built.close()
            reader.close()
            thread.quitSafely()
            readerThread.quitSafely()
            audio?.delete()
        }
    }

    /** Pauses, seeks to three places and waits for the next frame. zh-CN: 暂停, 跳转到三处并等待下一帧. */
    private fun seekLatencies(handler: Handler, player: AtomicReference<CompositionPlayer>, arrivals: List<LongArray>): List<Long> =
        listOf(5_000L, 15_000L, 25_000L).map { position ->
            val before = arrivals.size
            val started = System.nanoTime()
            handler.post {
                player.get().pause()
                player.get().seekTo(position)
            }
            val deadline = SystemClock.elapsedRealtime() + SEEK_TIMEOUT_MS
            while (arrivals.size == before && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(5)
            if (arrivals.size == before) -1L else (arrivals[before][0] - started) / 1_000_000L
        }

    private fun report(
        name: String,
        layout: TimelineLayout,
        played: List<LongArray>,
        released: Map<Long, Long>,
        positions: List<LongArray>,
        playStartNs: Long,
        endNs: Long,
        dropped: Int,
        buffering: Int,
        preparedMillis: Long,
        mixMillis: Long,
        seek: List<Long>,
        warnings: List<String>,
    ): String {
        val gaps = played.zipWithNext { a, b -> (b[0] - a[0]) / 1_000_000.0 }.sorted()
        val spanSeconds = if (played.size > 1) (played.last()[0] - played.first()[0]) / 1e9 else 0.0
        val fps = if (spanSeconds > 0) (played.size - 1) / spanSeconds else 0.0
        val expected = layout.durationMillis * layout.frameRate / 1000
        // Lateness: arrival against the release time the player planned (image timestamp = release time).
        // zh-CN: 迟到: 到达时刻对比播放器计划的释放时刻 (图像时间戳 = 释放时刻).
        val matched = played.mapNotNull { (arrival, stamp) -> released[stamp]?.let { Triple(arrival, stamp, it) } }
        val lateness = matched.map { (arrival, stamp, _) -> (arrival - stamp) / 1_000_000.0 }.sorted()
        // A/V offset: the presentation time of each arriving frame against the player position at that moment.
        // zh-CN: 音画偏差: 每个到达帧的呈现时间对比该时刻的播放位置.
        val offsets = matched.mapNotNull { (arrival, _, pts) -> positionAt(positions, arrival)?.let { pts / 1000.0 - it } }.sorted()
        val wallMillis = (endNs - playStartNs) / 1_000_000L
        val values = linkedMapOf<String, Any>(
            "name" to name,
            "size" to "${layout.width}x${layout.height}",
            "frameRate" to layout.frameRate,
            "durationMs" to layout.durationMillis,
            "frames" to played.size,
            "expected" to expected,
            "fps" to fmt(fps),
            "gapP50" to fmt(gaps.percentile(0.5)),
            "gapP95" to fmt(gaps.percentile(0.95)),
            "gapMax" to fmt(gaps.lastOrNull() ?: 0.0),
            "gapsOver50" to gaps.count { it > 50.0 },
            "gapsOver100" to gaps.count { it > 100.0 },
            "dropped" to dropped,
            "buffering" to buffering,
            "matched" to matched.size,
            "lateP50" to fmt(lateness.percentile(0.5)),
            "lateP95" to fmt(lateness.percentile(0.95)),
            "avP50" to fmt(offsets.percentile(0.5)),
            "avP95abs" to fmt(offsets.map { kotlin.math.abs(it) }.sorted().percentile(0.95)),
            "wallMs" to wallMillis,
            "extraMs" to (wallMillis - layout.durationMillis),
            "prepareMs" to preparedMillis,
            "firstFrameMs" to (played.firstOrNull()?.let { (it[0] - playStartNs) / 1_000_000L } ?: -1L),
            "mixMs" to mixMillis,
            "seekMs" to seek.joinToString("/"),
            "warnings" to warnings.size,
        )
        return values.entries.joinToString(";") { (key, value) -> "$key=${value.toString().clean()}" }
    }

    /** The player position (ms) at [nanos], interpolated between samples while playing. zh-CN: [nanos] 时的播放位置 (毫秒). */
    private fun positionAt(samples: List<LongArray>, nanos: Long): Double? {
        val index = samples.indexOfFirst { it[0] > nanos }
        if (index <= 0) return null
        val before = samples[index - 1]
        val after = samples[index]
        if (before[2] == 0L || after[2] == 0L) return null
        val fraction = (nanos - before[0]).toDouble() / (after[0] - before[0])
        return before[1] + (after[1] - before[1]) * fraction
    }

    /** `main` = trans+ovl+audio, `pip` = main+pip; otherwise the tokens joined by `+`. zh-CN: 场景名中的特性标记. */
    private fun flags(name: String): Set<String> = when (name) {
        "main" -> setOf("trans", "ovl", "audio")
        "pip" -> setOf("trans", "ovl", "audio", "pip")
        else -> name.split('+').toSet()
    }

    private fun timeline(name: String, clips: List<File>, music: File, logo: File): Map<String, Any?> {
        val flags = flags(name)
        val types = if ("trans" in flags) listOf("cut", "fade", "dissolve", "slide", "wipe", "zoom", "blur")
        else if ("dissolve" in flags) List(7) { "dissolve" } else List(7) { "cut" }
        // 8 clips of 4 s from the three 10 s sources; the transitions take 600 ms; clip 3 plays at 1.5x.
        // zh-CN: 从三段 10 秒素材各取 4 秒, 共 8 段; 转场 600 ms; 第 3 段 1.5 倍速.
        val main = (0 until 8).map { index ->
            val clip = linkedMapOf<String, Any?>(
                "src" to clips[index % clips.size].path,
                "start" to (index % 3) * 2_000L,
                "end" to (index % 3) * 2_000L + if (index == 3) 6_000L else 4_000L,
            )
            if (index == 3) clip["speed"] = 1.5
            if ("audio" !in flags) clip["volume"] = 0.0
            if (index > 0) {
                val type = types[index - 1]
                clip["transition"] = linkedMapOf<String, Any?>("type" to type, "duration" to 600L).apply {
                    if (type == "slide") put("direction", "left")
                    if (type == "wipe") put("direction", "up")
                }
            }
            clip
        }
        val video = mutableListOf<Map<String, Any?>>(mapOf("clips" to main))
        if ("pip" in flags) {
            video += mapOf(
                "clips" to listOf(
                    linkedMapOf<String, Any?>("src" to clips[1].path, "start" to 0L, "end" to 9_000L, "at" to 2_000L, "volume" to 0.0, "transform" to mapOf("scale" to 0.4)),
                    linkedMapOf<String, Any?>("src" to clips[2].path, "start" to 0L, "end" to 9_000L, "volume" to 0.0, "transform" to mapOf("scale" to 0.4),
                        "transition" to mapOf("type" to "dissolve", "duration" to 800L)),
                ),
            )
        }
        val cues = (0 until 14).map { index ->
            mapOf("start" to index * 2_000L + 200L, "end" to index * 2_000L + 1_800L, "text" to "Subtitle line ${index + 1}")
        }
        val timeline = linkedMapOf<String, Any?>("frameRate" to 30L, "video" to video)
        if ("audio" in flags) {
            timeline["audio"] = listOf(mapOf("clips" to listOf(mapOf("src" to music.path, "volume" to 0.4, "fadeIn" to 1_000L, "fadeOut" to 2_000L))))
        }
        if ("ovl" !in flags) return timeline
        return timeline.apply {
            put("overlays", listOf(
                mapOf("type" to "text", "text" to "3-Slate preview", "from" to 0L, "to" to 30_000L, "x" to "50%", "y" to "8%", "fontSize" to 56L, "color" to "#FFFFFF", "stroke" to "#000000"),
                mapOf("type" to "image", "src" to logo.path, "from" to 1_000L, "to" to 29_000L, "x" to "92%", "y" to "88%", "scale" to 0.5, "alpha" to 0.8),
            ))
            put("subtitles", mapOf("cues" to cues))
        }
    }

    private fun status(line: String) = instrumentation.sendStatus(STATUS_CODE, Bundle().apply { putString("spike", line) })

    private fun List<Double>.percentile(p: Double): Double = if (isEmpty()) 0.0 else this[((size - 1) * p).toInt()]

    private fun fmt(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    private fun String.clean(): String = replace(';', ',').replace('\n', ' ').replace('=', ':').take(400)

    private companion object {
        const val STATUS_CODE = 2
        const val CLIP_US = 10_000_000L
        const val OUTPUT_WIDTH = 1280
        const val SAMPLE_MS = 50L
        const val PREPARE_TIMEOUT_S = 60L
        const val PLAY_SLACK_S = 30L
        const val SEEK_TIMEOUT_MS = 5_000L
    }

}

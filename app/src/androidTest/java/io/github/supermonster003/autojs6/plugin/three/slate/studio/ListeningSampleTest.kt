package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.os.Bundle
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioResultKeys
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Roadmap P2.5: a mix to listen to on a device. Synthesizes three parts of a 16 second loop in C major at 120 bpm, each
 * in another format so the mix resamples and mixes channels: an arpeggio (44.1 kHz mono WAV), a bass line entering at
 * 4 s (22.05 kHz mono, converted to FLAC by the plugin) and drums (48 kHz stereo, converted to M4A by the plugin). It
 * mixes them with [MediaStudioOps.AUDIO_MIX] to `3-slate-mix.m4a`, then applies [MediaStudioOps.AUDIO_VOLUME] with
 * normalize and a 3 s fade out to `3-slate-mix-faded.m4a`, and copies both to the app specific Music folder on the
 * external storage. Runs only with the instrumentation argument `listeningSample=true` (`.python/listening_sample.py`);
 * `connectedDebugAndroidTest` skips it. Each result is sent as an instrumentation status with the key `listening`.
 * zh-CN: 路线图 P2.5: 供真机试听的混音. 合成 C 大调 120 bpm 16 秒循环的三个声部, 各用不同格式, 让混音经过重采样与声道混合:
 * 琶音 (44.1 kHz 单声道 WAV), 4 秒进入的低音 (22.05 kHz 单声道, 由插件转为 FLAC), 鼓 (48 kHz 立体声, 由插件转为 M4A).
 * 用 [MediaStudioOps.AUDIO_MIX] 混成 `3-slate-mix.m4a`, 再以 [MediaStudioOps.AUDIO_VOLUME] 归一化并淡出 3 秒得到
 * `3-slate-mix-faded.m4a`, 两者复制到外部存储的应用专属 Music 目录. 仅在仪器参数 `listeningSample=true` 时运行
 * (`.python/listening_sample.py`), `connectedDebugAndroidTest` 会跳过. 每项结果以键 `listening` 的仪器状态发送.
 */
@RunWith(AndroidJUnit4::class)
class ListeningSampleTest : MediaStudioTestBase() {

    @Test
    fun mixALoop() {
        assumeTrue("set the instrumentation argument listeningSample=true", InstrumentationRegistry.getArguments().getString("listeningSample") == "true")
        val arpeggio = writeWav(output("arpeggio.wav"), MELODY_RATE, 1, arpeggio(MELODY_RATE))
        val bassWav = writeWav(output("bass.wav"), BASS_RATE, 1, bass(BASS_RATE))
        val drumsWav = writeWav(output("drums.wav"), DRUM_RATE, 2, drums(DRUM_RATE))
        val target = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.apply { mkdirs() }
        withMediaStudio { plugin ->
            run(plugin, request(MediaStudioOps.AUDIO_CONVERT, listOf(bassWav), output("bass.flac")))
            run(plugin, request(MediaStudioOps.AUDIO_CONVERT, listOf(drumsWav), output("drums.m4a")))
            val tracks = listOf(options("volume" to 0.6), options("volume" to 0.7, "at" to 4_000), options("volume" to 0.5))
            val mix = run(plugin, request(MediaStudioOps.AUDIO_MIX, listOf(arpeggio, output("bass.flac"), output("drums.m4a")), output("3-slate-mix.m4a"), options("tracks" to tracks)))
            report("mix", mix)
            val faded = run(plugin, request(MediaStudioOps.AUDIO_VOLUME, listOf(output("3-slate-mix.m4a")), output("3-slate-mix-faded.m4a"), options("normalize" to -1.0, "fadeOut" to 3_000)))
            report("faded", faded)
        }
        listOf("3-slate-mix.m4a", "3-slate-mix-faded.m4a").forEach { name -> output(name).copyTo(File(target, name), overwrite = true) }
        InstrumentationRegistry.getInstrumentation().sendStatus(STATUS_CODE, Bundle().apply { putString("listening", "name=folder;path=${target.path}") })
    }

    private fun report(name: String, result: Bundle) {
        val values = listOf(
            "name" to name,
            "durationMillis" to result.getLong(MediaStudioResultKeys.DURATION_MILLIS),
            "codec" to result.getString(MediaStudioResultKeys.CODEC),
            "sampleRate" to result.getInt(MediaStudioResultKeys.SAMPLE_RATE),
            "channels" to result.getInt(MediaStudioResultKeys.CHANNELS),
            "warnings" to result.getStringArray(MediaStudioResultKeys.WARNINGS).orEmpty().joinToString(" | "),
        )
        // `;` separates the fields, so values carry `,` instead. zh-CN: `;` 分隔字段, 因此值中的 `;` 换成 `,`.
        val line = values.joinToString(";") { (key, value) -> "$key=${value.toString().replace(';', ',').replace('\n', ' ')}" }
        InstrumentationRegistry.getInstrumentation().sendStatus(STATUS_CODE, Bundle().apply { putString("listening", line) })
    }

    // region Synthesis

    /** Eighth notes over C, G, Am, F, twice. zh-CN: C, G, Am, F 上的八分音符, 两遍. */
    private fun arpeggio(rate: Int): FloatArray {
        val out = FloatArray(rate * LOOP_SECONDS)
        val chords = listOf(
            listOf(C4, E4, G4, C5, G4, E4, C4, E4),
            listOf(B3, D4, G4, B4, G4, D4, B3, D4),
            listOf(A3, C4, E4, A4, E4, C4, A3, C4),
            listOf(F3, A3, C4, F4, C4, A3, F3, A3),
        )
        (0 until 2).forEach { pass ->
            chords.forEachIndexed { chord, notes ->
                notes.forEachIndexed { step, frequency ->
                    val start = pass * 8.0 + chord * 2.0 + step * EIGHTH
                    tone(out, rate, start, frequency, length = 0.45, amplitude = 0.28, harmonics = 4, decay = 4.0)
                }
            }
        }
        return out
    }

    /** Quarter note roots from Am, the chord at 4 s. zh-CN: 从 4 秒处的 Am 开始的四分音符根音. */
    private fun bass(rate: Int): FloatArray {
        val roots = listOf(A2, F2, C2, G2, A2, F2)
        val out = FloatArray(rate * (LOOP_SECONDS - 4))
        roots.forEachIndexed { chord, root ->
            (0 until 4).forEach { beat -> tone(out, rate, chord * 2.0 + beat * BEAT, root, length = 0.45, amplitude = 0.45, harmonics = 6, decay = 2.5) }
        }
        return out
    }

    /** Kick on 1, 3 and the and of 3, snare on 2 and 4, eighth note hats on the right. zh-CN: 底鼓在 1, 3 拍与 3 拍后半, 军鼓在 2, 4 拍, 八分音符踩镲偏右. */
    private fun drums(rate: Int): FloatArray {
        val frames = rate * LOOP_SECONDS
        val left = FloatArray(frames)
        val right = FloatArray(frames)
        val random = Random(SEED)
        (0 until LOOP_SECONDS * 2).forEach { beat ->
            val time = beat * BEAT
            when (beat % 4) {
                0, 2 -> kick(left, right, rate, time)
                else -> snare(left, right, rate, time, random)
            }
            if (beat % 4 == 2) kick(left, right, rate, time + EIGHTH)
            hat(left, right, rate, time + EIGHTH, random)
            hat(left, right, rate, time, random, amplitude = 0.06f)
        }
        return FloatArray(frames * 2) { if (it % 2 == 0) left[it / 2] else right[it / 2] }
    }

    private fun tone(out: FloatArray, rate: Int, startSeconds: Double, frequency: Double, length: Double, amplitude: Double, harmonics: Int, decay: Double) {
        val first = (startSeconds * rate).roundToInt()
        val count = min((length * rate).roundToInt(), out.size - first)
        val norm = (1..harmonics).sumOf { 1.0 / it }
        for (i in 0 until count) {
            val t = i.toDouble() / rate
            val envelope = min(1.0, t / ATTACK) * exp(-decay * t) * min(1.0, (count - i).toDouble() / (RELEASE * rate))
            val value = (1..harmonics).sumOf { k -> sin(2 * PI * frequency * k * t) / k } / norm
            out[first + i] += (amplitude * envelope * value).toFloat()
        }
    }

    private fun kick(left: FloatArray, right: FloatArray, rate: Int, time: Double) {
        val first = (time * rate).roundToInt()
        var phase = 0.0
        for (i in 0 until min((0.3 * rate).roundToInt(), left.size - first)) {
            val t = i.toDouble() / rate
            phase += 2 * PI * (50 + 90 * exp(-30 * t)) / rate
            val value = (0.45 * min(1.0, t / ATTACK) * exp(-12 * t) * sin(phase)).toFloat()
            left[first + i] += value
            right[first + i] += value
        }
    }

    private fun snare(left: FloatArray, right: FloatArray, rate: Int, time: Double, random: Random) {
        val first = (time * rate).roundToInt()
        for (i in 0 until min((0.2 * rate).roundToInt(), left.size - first)) {
            val t = i.toDouble() / rate
            val value = (min(1.0, t / ATTACK) * (0.18 * exp(-25 * t) * (random.nextDouble() * 2 - 1) + 0.12 * exp(-30 * t) * sin(2 * PI * 185 * t))).toFloat()
            left[first + i] += value
            right[first + i] += value
        }
    }

    private fun hat(left: FloatArray, right: FloatArray, rate: Int, time: Double, random: Random, amplitude: Float = 0.1f) {
        val first = (time * rate).roundToInt()
        var previous = 0.0
        for (i in 0 until min((0.05 * rate).roundToInt(), left.size - first)) {
            val noise = random.nextDouble() * 2 - 1
            val value = (amplitude * exp(-70.0 * i / rate) * (noise - previous) / 2).toFloat()
            previous = noise
            left[first + i] += value * 0.5f
            right[first + i] += value
        }
    }

    private fun writeWav(file: File, rate: Int, channels: Int, samples: FloatArray): File {
        val data = samples.size * 2
        val buffer = ByteBuffer.allocate(MediaStudioTestBase.WAV_HEADER + data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray()).put("fmt ".toByteArray()).putInt(16)
        buffer.putShort(1).putShort(channels.toShort()).putInt(rate).putInt(rate * channels * 2).putShort((channels * 2).toShort()).putShort(16)
        buffer.put("data".toByteArray()).putInt(data)
        samples.forEach { buffer.putShort((it.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()) }
        file.writeBytes(buffer.array())
        return file
    }

    // endregion

    private companion object {
        const val STATUS_CODE = 2
        const val LOOP_SECONDS = 16
        const val MELODY_RATE = 44_100
        const val BASS_RATE = 22_050
        const val DRUM_RATE = 48_000
        const val BEAT = 0.5
        const val EIGHTH = 0.25
        const val ATTACK = 0.005
        const val RELEASE = 0.01
        const val SEED = 3

        const val C2 = 65.41
        const val F2 = 87.31
        const val G2 = 98.0
        const val A2 = 110.0
        const val F3 = 174.61
        const val A3 = 220.0
        const val B3 = 246.94
        const val C4 = 261.63
        const val D4 = 293.66
        const val E4 = 329.63
        const val F4 = 349.23
        const val G4 = 392.0
        const val A4 = 440.0
        const val B4 = 493.88
        const val C5 = 523.25
    }

}

package io.github.supermonster003.autojs6.plugin.three.slate.studio.record

import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.CodeGenerator
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.record.RecordCalls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.TimeZone

/**
 * Roadmap P4.5: the options of the Studio recording page against the checks and defaults of `audio.record` (appendix
 * B.2, appendix D Q1), the sources per API level, the default output names of D14, the equivalent script and the WAV
 * writer of the kernel copy.
 * zh-CN: 路线图 P4.5: Studio 录音页的选项对照 `audio.record` 的校验与默认值 (附录 B.2, 附录 D Q1), 各 API 级别的音源, D14 的默认输出
 * 名称, 等价脚本, 以及内核副本的 WAV 写入.
 */
class RecordSettingsTest {

    private val instant: Long = Instant.parse("2026-11-07T10:20:30Z").toEpochMilli()
    private val shanghai: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

    @Test
    fun `defaults follow audio record and Q1`() {
        val mic = RecordSettings().track()
        assertEquals(RecordingAudioSource.MICROPHONE, mic.source)
        assertEquals(48_000, mic.sampleRate)
        assertEquals(1, mic.channels)
        assertEquals(128_000, mic.bitRate)
        listOf(RecordingAudioSource.PLAYBACK, RecordingAudioSource.MIXED).forEach { source ->
            val track = RecordSettings(source = source).track()
            assertEquals(48_000, track.sampleRate)
            assertEquals(2, track.channels)
            assertEquals(192_000, track.bitRate)
        }
        val options = RecordSettings().options()
        assertEquals(AudioOutputFormat.M4A, options.format)
        assertEquals(0L, options.maxDurationMillis)
        assertFalse(options.overwrite)
    }

    @Test
    fun `overrides replace the defaults of the source`() {
        val track = RecordSettings(source = RecordingAudioSource.MIXED, sampleRate = 44_100, channels = 1, bitRate = 96_000).track()
        assertEquals(44_100, track.sampleRate)
        assertEquals(1, track.channels)
        assertEquals(96_000, track.bitRate)
    }

    @Test
    fun `values audio record rejects are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(sampleRate = 7_999).track() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(sampleRate = 192_001).track() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(channels = 0).track() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(channels = 3).track() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(bitRate = 15_999).track() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(bitRate = 320_001).track() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(source = RecordingAudioSource.NONE).track() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(maxDurationMillis = -1).options() }
        assertThrows(IllegalArgumentException::class.java) { RecordSettings(maxDurationMillis = RecordingOptions.MAX_DURATION_LIMIT_MILLIS + 1).options() }
        RecordSettings(maxDurationMillis = RecordingOptions.MAX_DURATION_LIMIT_MILLIS).options()
    }

    @Test
    fun `every offered choice passes the kernel checks`() {
        RecordSettings.sources(35).forEach { source ->
            AudioOutputFormat.entries.forEach { format ->
                RecordSettings.SAMPLE_RATE_CHOICES.forEach { RecordSettings(source, format, sampleRate = it).options() }
                RecordSettings.CHANNEL_CHOICES.forEach { RecordSettings(source, format, channels = it).options() }
                RecordSettings.BIT_RATE_CHOICES.forEach { RecordSettings(source, format, bitRate = it).options() }
                RecordSettings.MAX_DURATION_CHOICES.forEach { RecordSettings(source, format, maxDurationMillis = it).options() }
            }
        }
        assertTrue(RecordSettings().track().sampleRate in RecordSettings.SAMPLE_RATE_CHOICES)
        assertTrue(RecordingOptions.DEFAULT_PLAYBACK_BIT_RATE in RecordSettings.BIT_RATE_CHOICES)
        assertTrue(RecordingOptions.DEFAULT_MICROPHONE_BIT_RATE in RecordSettings.BIT_RATE_CHOICES)
    }

    @Test
    fun `playback and mixed need API 29`() {
        assertEquals(listOf(RecordingAudioSource.MICROPHONE), RecordSettings.sources(28))
        assertEquals(listOf(RecordingAudioSource.MICROPHONE, RecordingAudioSource.PLAYBACK, RecordingAudioSource.MIXED), RecordSettings.sources(29))
        assertEquals(RecordingAudioSource.MICROPHONE, RecordSettings(source = RecordingAudioSource.PLAYBACK).supportedOn(28).source)
        assertEquals(RecordingAudioSource.MIXED, RecordSettings(source = RecordingAudioSource.MIXED).supportedOn(31).source)
    }

    @Test
    fun `default output follows D14 in Music`() {
        val m4a = RecordSettings.output("/storage/emulated/0", null, AudioOutputFormat.M4A, instant, shanghai) { false }
        assertEquals("/storage/emulated/0/Music/3-Slate Studio/slate-record-20261107-182030.m4a", m4a)
        val wav = RecordSettings.output("/storage/emulated/0/", null, AudioOutputFormat.WAV, instant, TimeZone.getTimeZone("UTC")) { false }
        assertEquals("/storage/emulated/0/Music/3-Slate Studio/slate-record-20261107-102030.wav", wav)
    }

    @Test
    fun `custom folder and taken names`() {
        val custom = RecordSettings.output("/storage/emulated/0", "/storage/emulated/0/Podcasts/", AudioOutputFormat.M4A, instant, shanghai) { false }
        assertEquals("/storage/emulated/0/Podcasts/slate-record-20261107-182030.m4a", custom)
        val relative = RecordSettings.output("/sdcard", "Podcasts", AudioOutputFormat.M4A, instant, shanghai) { false }
        assertEquals("/sdcard/Music/3-Slate Studio/slate-record-20261107-182030.m4a", relative)
        val taken = setOf(
            "/sdcard/Music/3-Slate Studio/slate-record-20261107-182030.m4a",
            "/sdcard/Music/3-Slate Studio/slate-record-20261107-182030-1.m4a",
        )
        assertEquals(
            "/sdcard/Music/3-Slate Studio/slate-record-20261107-182030-2.m4a",
            RecordSettings.output("/sdcard", null, AudioOutputFormat.M4A, instant, shanghai) { it in taken },
        )
    }

    @Test
    fun `equivalent script writes the source and the options that differ from its defaults`() {
        assertEquals(linkedMapOf<String, Any?>("source" to "mic"), RecordCalls.options(RecordSettings()))
        assertEquals(
            linkedMapOf<String, Any?>("source" to "playback", "format" to "wav", "channels" to 1, "maxDuration" to 60_000L),
            RecordCalls.options(RecordSettings(RecordingAudioSource.PLAYBACK, AudioOutputFormat.WAV, channels = 1, bitRate = 64_000, maxDurationMillis = 60_000)),
        )
        assertEquals(
            linkedMapOf<String, Any?>("source" to "mixed", "sampleRate" to 44_100, "bitRate" to 128_000),
            RecordCalls.options(RecordSettings(RecordingAudioSource.MIXED, sampleRate = 44_100, channels = 2, bitRate = 128_000)),
        )
        // The script comes from CodeGenerator.recordScript (P4.4), with the wait rounded up to 100 ms.
        val script = RecordCalls.script(RecordSettings(), null, 3_021)
        assertEquals(CodeGenerator.recordScript(null, linkedMapOf("source" to "mic"), 3_100), script)
        assertEquals("audio.record", script.api)
        assertTrue(script.code, script.code.contains("audio.record(null, { source: \"mic\" });\nsleep(3100);\nlet result = recording.stop();\n"))
        val explicit = RecordCalls.script(RecordSettings(RecordingAudioSource.MIXED), "/sdcard/Podcasts/a.m4a", 0).code
        assertTrue(explicit, explicit.contains("let recording = audio.record(\"/sdcard/Podcasts/a.m4a\", { source: \"mixed\" });\nsleep(100);"))
    }

    @Test
    fun `wav writer of the kernel copy writes a canonical header`() {
        val file = Files.createTempFile("slate-record", ".wav").toFile()
        try {
            WavFileWriter(file, 48_000, 2).apply {
                write(ShortArray(960) { (it % 7).toShort() }, 960)
                finish()
            }
            val bytes = file.readBytes()
            assertEquals(WavFileWriter.HEADER_BYTES + 960 * 2, bytes.size)
            assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
            assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
            assertEquals(1920, int32(bytes, 40))
            assertEquals(36 + 1920, int32(bytes, 4))
            assertEquals(2, bytes[22].toInt())
            assertEquals(48_000, int32(bytes, 24))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `pcm helpers mix, widen mono and measure the peak`() {
        val target = shortArrayOf(1000, -1000, 32767)
        Pcm16.mixInto(target, shortArrayOf(3000, -3000, 32767), 3)
        assertEquals(listOf<Short>(2000, -2000, 32767), target.toList())
        val stereo = ShortArray(4)
        Pcm16.monoToStereo(shortArrayOf(5, -6), stereo, 2)
        assertEquals(listOf<Short>(5, 5, -6, -6), stereo.toList())
        assertEquals(0f, Pcm16.peak(ShortArray(8), 8))
        assertEquals(0.5f, Pcm16.peak(shortArrayOf(100, -16384, 200), 3), 1e-6f)
        assertEquals(1f, Pcm16.peak(shortArrayOf(Short.MIN_VALUE), 1))
        assertEquals(1000L, RecordingOptions.framesToMillis(48_000, 48_000))
        assertEquals(96_000L, RecordingOptions.maxFrames(2_000, 48_000))
        assertEquals(Long.MAX_VALUE, RecordingOptions.maxFrames(0, 48_000))
    }

    @Test
    fun `existing outputs are refused before recording`() {
        val file = Files.createTempFile("slate-record", ".m4a").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) { RecordingOptions.checkAudioOutput(file, overwrite = false) }
            RecordingOptions.checkAudioOutput(file, overwrite = true)
            assertThrows(IllegalArgumentException::class.java) { RecordingOptions.checkAudioOutput(File(file.parentFile, "."), overwrite = true) }
        } finally {
            file.delete()
        }
    }

    private fun int32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}

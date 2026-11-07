package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The processing settings of roadmap D37 / appendix D Q50 (b): the encoder order of each preference and the stored
 * values with their defaults and bounds.
 * zh-CN: 路线图 D37 / 附录 D Q50 (b) 的加工设置: 每种偏好的编码器顺序, 以及存储的值, 默认值与范围.
 */
class ProcessingSettingsTest {

    /** A map-backed SharedPreferences for the JVM. zh-CN: JVM 上以 Map 实现的 SharedPreferences. */
    private class MemoryPreferences : SharedPreferences {
        val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = key in values
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?) = apply { values[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = apply { this@MemoryPreferences.values[key] = value }
            override fun putLong(key: String, value: Long) = apply { this@MemoryPreferences.values[key] = value }
            override fun putFloat(key: String, value: Float) = apply { this@MemoryPreferences.values[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { this@MemoryPreferences.values[key] = value }
            override fun remove(key: String) = apply { values.remove(key) }
            override fun clear() = apply { values.clear() }
            override fun commit(): Boolean = true
            override fun apply() = Unit
        }
    }

    @Test
    fun preferencesMoveTheirKindToTheFrontAndKeepTheRest() {
        val encoders = listOf("c2.qti.avc.encoder", "c2.android.avc.encoder", "OMX.qcom.video.encoder.avc", "OMX.google.h264.encoder")
        val hardware: (String) -> Boolean = { !it.contains("android") && !it.contains("google") }
        assertEquals(encoders, EncoderPreference.AUTO.order(encoders, hardware))
        assertEquals(
            listOf("c2.qti.avc.encoder", "OMX.qcom.video.encoder.avc", "c2.android.avc.encoder", "OMX.google.h264.encoder"),
            EncoderPreference.HARDWARE.order(encoders, hardware),
        )
        assertEquals(
            listOf("c2.android.avc.encoder", "OMX.google.h264.encoder", "c2.qti.avc.encoder", "OMX.qcom.video.encoder.avc"),
            EncoderPreference.SOFTWARE.order(encoders, hardware),
        )
        assertEquals("no encoder of the preferred kind keeps the others", listOf("a", "b"), EncoderPreference.SOFTWARE.order(listOf("a", "b")) { true })
        assertEquals(listOf("auto", "hardware", "software"), EncoderPreference.entries.map { it.id })
        assertEquals(EncoderPreference.AUTO, EncoderPreference.of("gpu"))
        assertEquals(EncoderPreference.SOFTWARE, EncoderPreference.of("software"))
    }

    @Test
    fun storedValuesDefaultsAndBounds() {
        val prefs = MemoryPreferences()
        val settings = ProcessingSettings(prefs)
        assertEquals(EncoderPreference.AUTO, settings.encoderPreference)
        assertEquals("the contract limit by default", 2, settings.concurrency)
        settings.encoderPreference = EncoderPreference.HARDWARE
        settings.concurrency = 1
        assertEquals(EncoderPreference.HARDWARE, ProcessingSettings(prefs).encoderPreference)
        assertEquals(1, ProcessingSettings(prefs).concurrency)
        assertEquals(mapOf("encoderPreference" to "hardware", "taskConcurrency" to 1), prefs.values)
        settings.concurrency = 5
        assertEquals(2, settings.concurrency)
        prefs.values["taskConcurrency"] = 0
        assertEquals(1, settings.concurrency)
        assertEquals("studio", ProcessingSettings.FILE)
    }

}

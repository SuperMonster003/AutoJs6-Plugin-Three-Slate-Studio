package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.record

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.AudioOutputFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordSettings
import io.github.supermonster003.autojs6.plugin.three.slate.studio.record.RecordingAudioSource

/**
 * The last options of the recording page and whether the notification permission was asked once (API 33+).
 * zh-CN: 录音页上次使用的选项, 以及是否已申请过一次通知权限 (API 33+).
 */
internal class RecordPreferences(private val prefs: SharedPreferences) {

    var settings: RecordSettings
        get() = RecordSettings(
            source = RecordingAudioSource.of(prefs.getString(KEY_SOURCE, null))?.takeIf { it != RecordingAudioSource.NONE } ?: RecordingAudioSource.MICROPHONE,
            format = AudioOutputFormat.of(prefs.getString(KEY_FORMAT, null)) ?: AudioOutputFormat.M4A,
            sampleRate = prefs.getInt(KEY_SAMPLE_RATE, 0).takeIf { it > 0 },
            channels = prefs.getInt(KEY_CHANNELS, 0).takeIf { it > 0 },
            bitRate = prefs.getInt(KEY_BIT_RATE, 0).takeIf { it > 0 },
            maxDurationMillis = prefs.getLong(KEY_MAX_DURATION, 0L).coerceAtLeast(0L),
        )
        set(value) = prefs.edit {
            putString(KEY_SOURCE, value.source.key)
            putString(KEY_FORMAT, value.format.key)
            putInt(KEY_SAMPLE_RATE, value.sampleRate ?: 0)
            putInt(KEY_CHANNELS, value.channels ?: 0)
            putInt(KEY_BIT_RATE, value.bitRate ?: 0)
            putLong(KEY_MAX_DURATION, value.maxDurationMillis)
        }

    var notificationsAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS_ASKED, value) }

    companion object {
        private const val FILE = "record"
        private const val KEY_SOURCE = "source"
        private const val KEY_FORMAT = "format"
        private const val KEY_SAMPLE_RATE = "sampleRate"
        private const val KEY_CHANNELS = "channels"
        private const val KEY_BIT_RATE = "bitRate"
        private const val KEY_MAX_DURATION = "maxDuration"
        private const val KEY_NOTIFICATIONS_ASKED = "notificationsAsked"

        fun of(context: Context) = RecordPreferences(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }

}

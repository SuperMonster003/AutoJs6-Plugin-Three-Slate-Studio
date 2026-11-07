# record: Studio recording kernel (roadmap D41, P4.5)

The Studio recorder keeps its own copy of the AutoJs6 host recording kernel instead of calling the host (roadmap D41): the microphone through `AudioRecord`, the playback capture and the mixed source (Android 10+) through `AudioPlaybackCaptureConfiguration` with the plugin's own MediaProjection consent, inside the `microphone` / `mediaProjection` foreground service `RecordingService`. Roadmap 1.2.0 P7.1 reuses the same consent and service for screen recording.

## Copied files

Copied from the host repository `D:/idea-projects/AutoJs6`, directory `app/src/main/java/org/autojs/autojs/core/record/`, at host commit `3ef5cec633` (roadmap P1.2, 2026-11-05; the files were unchanged up to host `063b9f0b8a`, 2026-11-07). Each file names its source and its plugin changes in its header; plugin changes inside the code are marked `Plugin:`.

| Plugin file | Host file | Plugin changes |
| --- | --- | --- |
| `AudioRecorder.kt` | `AudioRecorder.kt` | `MediaProjection?` instead of the host `ScreenCapturer?`; pause / resume (blocks read while paused are dropped, the recorded frames, AAC presentation times and `maxDuration` skip the pause); level of the last block; the permission message names 3-Slate Studio |
| `RecordingOptions.kt` | `RecordingOptions.kt` | audio parts only (P7 adds `ScreenRecordingOptions` and the video ranges from the same file); `RecordingStates.PAUSED` |
| `Pcm16.kt` | `Pcm16.kt` | `peak` for the level meter |
| `RecordingMuxer.kt` | `RecordingMuxer.kt` | none |
| `WavFileWriter.kt` | `WavFileWriter.kt` | none |
| `RecordingForegroundLease.kt` | `RecordingForegroundLease.kt` | none (the Studio recorder passes no lease: `RecordingService` is in the foreground before the kernel starts) |

When the host kernel changes (`git -C D:/idea-projects/AutoJs6 log -- app/src/main/java/org/autojs/autojs/core/record`), port the change, update the commit in the headers and in this table, and run `RecordSettingsTest` and the device test `RecordingDeviceTest`.

## Plugin files

- `RecordSettings.kt`: the options of the Studio recording page, with the defaults of the script `audio.record` (roadmap appendix B.2, appendix D Q1: microphone mono 48 kHz AAC 128 kbit/s, playback and mixed stereo 48 kHz AAC 192 kbit/s), and the default output (roadmap D14: `Music/3-Slate Studio/slate-record-<yyyyMMdd-HHmmss>.<ext>`, or the Studio output folder of the settings).
- `StudioRecorder.kt`: the one recording of the plugin process (state for the page and the notification, MediaStore registration of the saved file).
- `RecordingService.kt`: the foreground service (notification with pause / resume and stop, partial wake lock so the recording continues with the screen off).

## Screen recording copy (roadmap P7)

The screen recording kernel copy belongs to roadmap 1.2.0 P7 and is not here yet. When P7 copies `ScreenRecorder.kt`, `RecordingFrames.kt` and the screen parts of `RecordingOptions.kt`, the default size without `width` / `height` follows appendix D Q33 (a): copy the host `core/record/RecordingVideoSizes.kt` from host commits `95b3d8ab42` and `0d5e37d97d` (the aligned full size first, then scaled down by the limits of both directions and narrowed step by step, always checked with `areSizeAndRateSupported`, the largest area over all encoders), and name that source in the file header like the files above.

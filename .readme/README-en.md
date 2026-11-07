<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Video and audio processing, multi-track timeline editing, transitions and subtitles, with script code generated from the app screens</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The README.md file is currently available in the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Slate Studio is the video and audio plugin of AutoJs6 and also an editing app that works without AutoJs6. It trims, joins, speeds up, scales, rotates, crops and watermarks videos, extracts frames, makes GIFs and transcodes; it trims, joins, mixes, fades and converts audio; and its multi-track timeline editor combines clips with transitions, overlays and subtitles.

Processing runs in the plugin with Jetpack Media3 Transformer and the hardware codecs of the device, so the AutoJs6 host stays small. AutoJs6 scripts process media through the global modules `video` and `audio`; screen recording (`video.record`) and audio recording (`audio.record`) are built into AutoJs6 and work without the plugin. Every tool screen of the plugin shows the equivalent script code, which can be copied or opened as a new script in AutoJs6.

> Version 1.0.0 is the first release. Processing media in scripts needs AutoJs6 6.8.1 (internal build 6641) or later; the plugin screens, the timeline editor and the Studio recorder do not depend on AutoJs6.

******

### Features

******

- Video editing: trim by time, join, change the speed (0.25x to 8x), scale, rotate, crop and flip, add text or image watermarks, extract or remove the audio track, and transcode (H.264 by default, H.265 when the device has an H.265 encoder).
- Frames and GIF: single frames, frame sequences and thumbnails as PNG or JPG files or as `Image` objects, and GIF animations from the built-in encoder.
- Audio: trim, join, mix several inputs (scaled down automatically when the sum would clip), adjust the volume with fade in and fade out, change the speed, and convert to M4A, WAV, FLAC or OGG (OGG needs Android 10 or later).
- Multi-track timeline editor: video, audio, overlay and subtitle tracks, 7 transitions (cut, fade, dissolve, slide, wipe, zoom, blur), live preview, undo and redo, and projects saved as `.slate.json`; subtitles support SRT import and export and a basic subset of ASS, burned into the picture on export.
- Toolbox: a wizard for every processing feature, with forms that show defaults and ranges; tasks run in a queue that shows the progress and the reason of failures, and long tasks keep a foreground notification.
- Code generation: every wizard, the timeline export and the recorder produce the equivalent `video.*` / `audio.*` script, ready to copy or to open as a new script in AutoJs6.
- Studio recorder: record the microphone, the device audio (Android 10 and later) or both from the plugin screens as M4A or WAV, with pause, and keep recording with the screen off.
- FFmpeg library (planned for 1.1.0): an optional download that adds formats such as MP3, WebM and MKV and complete ASS subtitle rendering.

******

### Screenshots

******

These screenshots come from Android 12 and 13 phones in the light theme with the Simplified Chinese interface.

| Media library | Toolbox | Timeline editor | Studio recorder |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/library.png?raw=true" alt="library" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/toolbox.png?raw=true" alt="toolbox" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/timeline.png?raw=true" alt="timeline" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/recorder.png?raw=true" alt="recorder" width="200" /> |

******

### Usage

******

1. Install 3-Slate Studio from the Plugin Center of AutoJs6, or download the APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) and install it. There is a single package for every ABI.
2. Open the plugin and grant all files access when asked, so that it can read and write the paths you choose.
3. To use the processing methods of `video` and `audio` in scripts, use AutoJs6 6.8.1 (internal build 6641) or later and enable the plugin in the Plugin Center.

> The plugin supports devices running Android 7.0 (API 24) or later. Its only native library comes from Jetpack Compose and is aligned to 16 KB memory pages, so a single package serves every ABI.

******

### Script API

******

Scripts use these features through the global modules `video` and `audio` (also available as `$video` and `$audio`) (the AutoJs6 documentation of [video](https://docs.autojs6.com/#/video) and [audio](https://docs.autojs6.com/#/audio) describes every method and option):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }]);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });
toastLog(video.toGif(joined.path, { fps: 10, width: 480 }).path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }]);

images.requestScreenCapture();
let recording = video.record({ audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.compose({
    video: [{ clips: [
        { src: "/sdcard/Movies/a.mp4", end: 8000 },
        { src: "/sdcard/Movies/b.mp4", start: 2000, end: 10000, transition: { type: "dissolve", duration: 500 } },
    ] }],
    audio: [{ clips: [{ src: "/sdcard/Music/bgm.m4a", end: 15500, volume: 0.4, fadeIn: 1000, fadeOut: 2000 }] }],
    overlays: [{ type: "text", text: "AutoJs6", from: 1000, to: 4000, x: "50%", y: "10%", fontSize: 48 }],
});

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- Every processing method has a blocking form and an `xxxAsync` form that returns a Promise; `options.onProgress(progress)` reports `percent`, `stage` and `etaMillis`, and stopping the script cancels its tasks.
- When `output` is omitted, videos go to `Movies/3-Slate Studio/`, audio to `Music/3-Slate Studio/`, and frames, thumbnails and GIFs to `Pictures/3-Slate Studio/`, named after the source, the operation and the time; results carry the actual path and media information and are registered in the media library.
- `video.compose(timeline)` renders a multi-track timeline described as an object (up to 8 video tracks and 16 audio tracks, clips, transitions, overlays, subtitles), the same format the timeline editor exports.
- `video.record` and `audio.record` are recorded by AutoJs6 and save to `Movies/AutoJs6/` and `Music/AutoJs6/` when the path is omitted; screen recording needs `images.requestScreenCapture` first.
- `video.tasks()` / `audio.tasks()` list and cancel the running tasks of the script; failures throw a `MediaStudioError` with a stable `code` such as `INVALID_OPTIONS` or `STORAGE_PERMISSION_DENIED`.

******

### Sample scripts

******

The [samples](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/tree/master/samples) folder of this repository has 4 scripts that run as they are, and the samples bundled with AutoJs6 include the same scripts (folder 3-Slate Studio):

- Record the screen for 10 seconds and make a GIF: `video.record` records the screen without sound, then `video.toGif` turns it into a GIF of 10 frames per second, 480 pixels wide (the first run asks you to confirm the screen capture).
- Record audio and convert it to FLAC: `audio.record` records the microphone, then `audio.convert` turns it into lossless FLAC (the first run asks for the recording permission).
- Trim, watermark and join: `video.probe` reads the source, `video.trim` keeps the first 5 seconds, `video.overlay` adds a text watermark, and `video.concat` joins both parts and reports the progress.
- Timeline composition: `video.compose` combines two videos (with a dissolve), background music (with fades) and a text overlay into one portrait video.

The last two samples read `/sdcard/Movies/a.mp4`, `/sdcard/Movies/b.mp4` and `/sdcard/Music/bgm.m4a` by default; put your media there or change the paths.

******

### Compatibility

******

The plugin supports Android 7.0 (API 24) and later and works without AutoJs6. Enabling it in the Plugin Center and using the processing methods of `video` and `audio` in scripts needs AutoJs6 6.8.1 (internal build 6641) or later (F10); both modules work in the Rhino, Node.js and Bun engines.

The numbers in parentheses point to the evidence entries in [appendix F of the roadmap](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md). "Not tested" means that the version or format was not checked before the 1.0.0 release, not that it does not work.

**By Android version**

| Android | Devices | Processing | Plugin screens | Studio recorder | Sample scripts |
| --- | --- | --- | --- | --- | --- |
| 7.0 (API 24) | Android emulator | Passed (F5) | Passed (F4, F6) | Not tested | Not tested |
| 7.1 - 8.1 (API 25 - 27) | - | Not tested | Not tested | Not tested | Not tested |
| 9 (API 28) | Sony G8441 | Passed (F3) | Recorder screen passed (F8) | Passed, microphone only (F8) | Not tested |
| 10 - 11 (API 29 - 30) | - | Not tested | Not tested | Not tested | Not tested |
| 12 (API 31) | Sony XQ-AT72 | Passed (F3, F5) | Passed (F4, F5, F7) | Passed, with device audio (F8) | Not tested |
| 12L (API 32) | - | Not tested | Not tested | Not tested | Not tested |
| 13 (API 33) | Sony XQ-DQ72, Redmi 22120RN86C | Passed (F1, F3) | Passed (F6, F8) | Passed, with device audio (F8) | Passed (F9) |
| 14 (API 34) | - | Not tested | Not tested | Not tested | Not tested |
| 15 (API 35) | Xiaomi Pad 23046RP50C, OnePlus OPD2413, Android emulator | Passed (F1, F2, F3, F5) | Passed (F4, F5, F6) | Passed, with device audio (F8) | Not tested |

Processing: the 25 script operations or the 20 toolbox wizards; plugin screens: media library, toolbox, tasks, settings and the timeline editor; sample scripts: the 4 samples above.

**By format**

| Format | As input | As output | Notes |
| --- | --- | --- | --- |
| MP4: H.264 video + AAC audio | Passed (F2, F3) | Passed (F2, F3) | Default video output |
| MP4: H.265 / HEVC video | Not tested | Passed (F1, F2, F3) | Only when the device has an H.265 encoder; all 6 phones and tablets tested have one |
| M4A (AAC) | Passed (F2, F3) | Passed (F2, F3, F8) | Default audio output and default format of the Studio recorder |
| WAV (16-bit PCM) | Passed (F2) | Passed (F2, F3) | Lossless, large files |
| FLAC | Not tested | Passed (F2, F3) | Lossless compression |
| OGG (Opus) | Not tested | Passed (F2, F3) | Needs Android 10 or later (the system writes OGG from Android 10); tested on Android 12 - 15 |
| GIF | Not tested | Passed (F2, F3) | Built-in encoder, 1 - 50 frames per second |
| PNG, JPG images | Passed (F2, F3) | Passed (F2, F3) | Input: overlay images; output: frames and thumbnails |
| SRT subtitles | Passed (F2, F3, F6) | Passed (F2) | Imported into the timeline editor or burned into the picture |
| ASS subtitles (basic subset) | Passed (F2) | Passed (F2, F3) | Unsupported tags are ignored with a warning; complete ASS rendering is planned for 1.1.0 |
| MP3, AMR and other audio | Not tested | Not in 1.0.0 | Input depends on the system decoders of the device; MP3 output is planned with the FFmpeg library of 1.1.0 |
| WebM, MKV | Not tested | Not in 1.0.0 | Planned with the FFmpeg library of 1.1.0 |

******

### FAQ

******

#### Can I use it without AutoJs6?

Yes. The media library, the toolbox, the timeline editor and the Studio recorder work on their own. AutoJs6 is only needed when scripts process media.

#### Why does the plugin ask for all files access?

Scripts pass ordinary file paths such as `/sdcard/DCIM/...`, and the plugin reads and writes them directly, like 3-Maple Image. Without the permission, processing reports `STORAGE_PERMISSION_DENIED` and the plugin screen explains how to grant it. `video.frame` returns an `Image` object, but it still hands the picture to AutoJs6 through a temporary image in `Movies/3-Slate Studio/`, so it needs the same permission.

#### Does screen recording need the plugin?

No. `video.record` and `audio.record` are built into AutoJs6 (6.8.1 or later); the plugin adds the processing methods and its own Studio recorder.

#### Can I use it with an older AutoJs6?

Scripts need AutoJs6 6.8.1 (internal build 6641) or later to use the plugin; older versions do not have the `video` and `audio` modules yet. The plugin itself still works as a standalone editing app.

#### The timeline export reports `CODEC_ERROR` and asks for fewer tracks. What can I do?

During an overlapping transition (dissolve, slide, wipe, zoom, blur) a video track decodes two clips at once, and a device has a limited number of codecs. The three Sony phones tested export 6 video tracks with transitions plus 16 audio tracks, while 8 + 16 ends with `CODEC_ERROR`; use fewer video tracks or fewer overlapping transitions.

******

### Permissions and security

******

Permissions requested by the plugin and what they are for:

- All files access (`MANAGE_EXTERNAL_STORAGE`; the storage permissions on Android 10 and earlier): reads and writes the file paths that scripts and screens choose, and writes outputs without a path to the `3-Slate Studio` folders under `Movies`, `Music` and `Pictures`.
- Media access (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, Android 13 and later): lists the videos and audio of the device in the media library of the home screen.
- Microphone (`RECORD_AUDIO`): used only by the Studio recorder; `audio.record` and `video.record` in scripts are recorded by AutoJs6 with its own recording permission.
- Screen capture (the screen sharing consent of the system, not a manifest permission): needed when the Studio recorder captures the device audio (device audio or mixed); the system asks when the recording starts.
- Notifications (`POST_NOTIFICATIONS`, Android 13 and later): show the processing progress and the recording notification with pause and stop buttons; without it these notifications are not shown.
- Foreground services and wake lock (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `WAKE_LOCK`): keep long processing running in the background and the recording going with the screen off.
- Network (`INTERNET`, `ACCESS_NETWORK_STATE`): in 1.0.0 only for the update check, which reads the official plugin index at most once every 12 hours when the home screen opens and can be turned off in the settings.
- AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`): communicates with AutoJs6.

The plugin keeps its permission surface as small as possible:

- Protected entry points: the capability service and the activation entry are protected by the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly.
- Storage: all files access only serves the paths that scripts and the Studio screens choose; outputs without a path go only to the `3-Slate Studio` folders under `Movies`, `Music` and `Pictures`.
- Recording: the microphone and the device audio are captured only while the Studio recorder runs, with a foreground notification; capturing the device audio also needs the screen capture consent of the system.
- Network: version 1.0.0 only checks for updates; the planned FFmpeg library (1.1.0) will be downloaded only when you ask for it and checked against its SHA-256 before it is loaded.

******

### FFmpeg library

******

The FFmpeg library is planned for version 1.1.0 and will be built under LGPL-2.1-or-later. It will not be part of the APK: it will be downloaded for your ABI only when you ask for it, loaded dynamically, and come with its own license notices and the location of the matching source code. Without it, every 1.0.0 feature keeps working. The formats and the script interface of the library will be described when 1.1.0 is released.

> Some codecs that FFmpeg supports, such as H.265 / HEVC and AAC, may be covered by patents in some countries or regions. The library is offered for convenience only and comes with no patent license; check the rules that apply to you before you use it.

******

### Plugin interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate capabilities:

```text
application id: io.github.supermonster003.autojs6.plugin.three.slate.studio
plugin id: three-slate-studio
engine: media-studio
variant: default
info service: org.autojs.plugin.INFO (category three-slate-studio)
capability service: org.autojs.plugin.MEDIA_STUDIO (category media-studio)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libandroidx.graphics.path.so of Jetpack Compose only (arm64-v8a, armeabi-v7a, x86_64, x86; 16 KB aligned); a single APK for every ABI
```

******

### Roadmap

******

Plans, design decisions and progress are tracked as a checklist in ROADMAP.md. Unchecked items are plans, not features of the current version.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### Release history

******

#### v1.0.0

_2026/11/08_

- `Hint` Processing media with video / audio in scripts needs AutoJs6 6.8.1 (internal build 6641) or later; the plugin screens, the timeline editor and the Studio recorder do not depend on AutoJs6
- `Hint` See the [video](https://docs.autojs6.com/#/video) and [audio](https://docs.autojs6.com/#/audio) chapters of the AutoJs6 documentation for the script API
- `Feature` Video processing: trim, join, speed (0.25x - 8x), scale, rotate, crop and flip, text or image watermarks, audio track extraction or removal, and transcoding (H.264, or H.265 where the device supports it)
- `Feature` Frames, thumbnails and GIF: PNG / JPG files or Image objects, and a built-in GIF encoder (palette, dithering, looping)
- `Feature` Audio processing: trim, join, mix (scaled down automatically when it would clip), volume and fades, speed, and conversion to M4A / WAV / FLAC / OGG (OGG needs Android 10+)
- `Feature` Multi-track composition with video.compose: up to 8 video tracks and 16 audio tracks, 7 transitions, text / image overlays and subtitles
- `Feature` Subtitles: SRT reading and writing and a basic subset of ASS, burned into the picture with video.burnSubtitles, and video.subtitles.parse / write for subtitle files
- `Feature` AutoJs6 script modules video / audio (AutoJs6 6.8.1, Rhino, Node.js and Bun): every processing method has an xxxAsync form and a progress callback, and stopping the script cancels its tasks
- `Feature` Studio screens: media library, toolbox, task queue and settings; the appearance follows AutoJs6 or uses its own language, night mode and theme color, and the settings guide you to all files access and unrestricted battery use
- `Feature` Toolbox wizards: one wizard per processing feature, with forms that show defaults and ranges, and a result page with the output, the media information and the equivalent script
- `Feature` Timeline editor: multi-track editing, transitions, overlay and subtitle editing, live preview, undo / redo, projects saved as .slate.json, and export into the task queue
- `Feature` Code generation: the wizards, the timeline and the recorder show the equivalent complete script, ready to copy or to open in AutoJs6 as a new script
- `Feature` Studio recorder: microphone, device audio (Android 10+) or both, M4A / WAV, pause / resume, recording with the screen off and controls in the notification
- `Feature` Long tasks run in a foreground service with a progress notification; the settings choose the hardware encoder preference and how many tasks run at once (1 - 2)
- `Feature` Outputs without a path go to the 3-Slate Studio folders under Movies, Music and Pictures and are registered in the media library
- `Feature` Update check: reads the official plugin index at most once every 12 hours when the home screen opens, and a release can be ignored
- `Feature` A single APK for every ABI: the only native library comes from Jetpack Compose and is aligned to 16 KB memory pages
- `Feature` Screens, README, Plugin Center instructions and changelog in 10 languages
- `Dependency` Attach Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer and UI, for processing and previews)
- `Dependency` Attach Jetpack Compose BOM 2026.09.00 (the plugin screens)
- `Dependency` Attach AndroidX Activity Compose 1.13.0
- `Dependency` Attach Navigation Compose 2.10.2
- `Dependency` Attach AndroidX Core KTX 1.18.0
- `Dependency` Attach Material Color Utilities (MaterialKolor) 4.1.1 (theme color of the plugin screens)

##### For more release history, refer to

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build

******

This section targets developers who want to build the plugin from source.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE). Third-party components and data are listed in the third-party notices.

- [Third-party notices (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### Related links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6

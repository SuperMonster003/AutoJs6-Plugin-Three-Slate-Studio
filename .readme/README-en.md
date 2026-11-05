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

> This is a development preview: the AutoJs6 Plugin Center can discover the plugin, but the processing features, the Studio screens and the processing methods of the `video` and `audio` modules are not delivered yet. The text below describes the design goals of each version; see the roadmap for progress.

******

### Features

******

- Video editing: trim by time, join, change the speed (0.25x to 8x), scale, rotate, crop and flip, add text or image watermarks, extract or remove the audio track, and transcode (H.264 by default, H.265 where the device supports it).
- Frames and GIF: single frames, frame sequences and thumbnails as image files or `Image` objects, and GIF animations from the built-in encoder.
- Audio: trim, join, mix several inputs, adjust the volume with fade in and fade out, change the speed, and convert to M4A, WAV, FLAC or OGG.
- Multi-track timeline: video, audio and overlay tracks, 7 transitions (cut, fade, dissolve, slide, wipe, zoom, blur) and subtitles with SRT import and export and a basic subset of ASS, burned into the picture on export.
- Code generation: every tool screen and the timeline export produce the equivalent `video.*` / `audio.*` code, ready to copy or to open as a new script in AutoJs6.
- Studio recorder: record the microphone, the device audio (Android 10 and later) or both from the plugin screens.
- FFmpeg library (1.1.0): an optional download for MP3, WebM and MKV output, command passthrough with `video.ffmpeg` and complete ASS rendering.

******

### Usage

******

1. Download the APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) and install it. There is a single package for every ABI.
2. Open the plugin and grant all files access when asked, so that it can read and write the paths you choose.
3. To use the processing methods of `video` and `audio` in scripts, upgrade AutoJs6 to internal build 6641 (6.8.1) or later and enable the plugin in the Plugin Center.

> The plugin supports devices running Android 7.0 (API 24) or later. Its only native library comes from Jetpack Compose and is aligned to 16 KB memory pages, so a single package serves every ABI.

******

### Script API

******

Scripts use these features through the global modules `video` and `audio` (also available as `$video` and `$audio`) (1.0.0 design draft; the documentation of the released version is authoritative):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }], null);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });

let gif = video.toGif(joined.path, { fps: 10, width: 480 });
toastLog(gif.path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }], null);

let recording = video.record(null, { audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- Every processing method has a blocking form and an `xxxAsync` form that returns a Promise; `options.onProgress(progress)` reports `percent`, `stage` and `etaMillis`, and stopping the script cancels its tasks.
- When `output` is omitted, videos go to `Movies/3-Slate Studio/` and audio to `Music/3-Slate Studio/`, named after the source, the operation and the time; results carry the actual path and media information and are registered in the media library.
- `video.compose(timeline)` renders a multi-track timeline described as an object (tracks, clips, transitions, overlays, subtitles), the same format the timeline editor exports.
- `video.tasks()` / `audio.tasks()` list and cancel the running tasks of the script; failures throw a `MediaStudioError` with a stable `code` such as `INVALID_OPTIONS` or `STORAGE_PERMISSION_DENIED`.

******

### FAQ

******

#### Can I use it without AutoJs6?

Yes. The tool screens, the timeline editor and the Studio recorder work on their own. AutoJs6 is only needed when scripts process media.

#### Why does the plugin ask for all files access?

Scripts pass ordinary file paths such as `/sdcard/DCIM/...`, and the plugin reads and writes them directly, like 3-Maple Image. Without the permission, processing reports `STORAGE_PERMISSION_DENIED` and the plugin screen explains how to grant it.

#### Does screen recording need the plugin?

No. `video.record` and `audio.record` are built into AutoJs6 (internal build 6641 or later); the plugin adds the processing methods and its own Studio recorder.

******

### Permissions and security

******

The plugin keeps its permission surface as small as possible:

- Protected entry points: the capability service and the activation entry are protected by the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly.
- Storage: all files access only serves the paths that scripts and the Studio screens choose; outputs without a path go to the plugin folders under `Movies` and `Music`.
- Recording: the microphone and the device audio are captured only while the Studio recorder runs, with a foreground notification; capturing the device audio also needs the screen capture consent of the system.
- Network: version 1.0.0 only checks for updates; from 1.1.0 the FFmpeg library is downloaded when you ask for it and checked against its SHA-256 before it is loaded.

******

### FFmpeg library

******

From version 1.1.0 the plugin can download an optional FFmpeg library built under LGPL-2.1-or-later. The library is not part of the APK: it is downloaded for your ABI only when you ask for it, loaded dynamically, and comes with its own license notices and the location of the matching source code. Without it, every 1.0.0 feature keeps working.

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

_2026/11/05_

- `Hint` Development preview: the AutoJs6 Plugin Center can discover the plugin; the processing features, the Studio screens and the processing methods of the video and audio script modules are not delivered yet
- `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
- `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and media studio capability service (contract version 1)
- `Feature` Home screen with the all files access grant
- `Feature` A single APK for every ABI: the only native library comes from Jetpack Compose and is aligned to 16 KB memory pages
- `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
- `Dependency` Attach Jetpack Compose BOM 2026.09.00 (the plugin screens)
- `Dependency` Attach Navigation Compose 2.10.2
- `Dependency` Attach Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer and UI for processing and previews)
- `Dependency` Attach AndroidX Core KTX 1.18.0

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

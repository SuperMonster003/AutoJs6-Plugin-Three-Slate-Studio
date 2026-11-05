3-Slate Studio is the video and audio plugin of AutoJs6 and also an editing app that works without AutoJs6. It trims, joins, speeds up, scales, rotates, crops and watermarks videos, extracts frames, makes GIFs and transcodes; it trims, joins, mixes, fades and converts audio; and its multi-track timeline editor combines clips with transitions, overlays and subtitles.

> This is a development preview: the AutoJs6 Plugin Center can discover the plugin, but the processing features, the Studio screens and the processing methods of the `video` and `audio` modules are not delivered yet. The text below describes the design goals of each version; see the roadmap for progress.

### Features

- Video editing: trim by time, join, change the speed (0.25x to 8x), scale, rotate, crop and flip, add text or image watermarks, extract or remove the audio track, and transcode (H.264 by default, H.265 where the device supports it).
- Frames and GIF: single frames, frame sequences and thumbnails as image files or `Image` objects, and GIF animations from the built-in encoder.
- Audio: trim, join, mix several inputs, adjust the volume with fade in and fade out, change the speed, and convert to M4A, WAV, FLAC or OGG.
- Multi-track timeline: video, audio and overlay tracks, 7 transitions (cut, fade, dissolve, slide, wipe, zoom, blur) and subtitles with SRT import and export and a basic subset of ASS, burned into the picture on export.
- Code generation: every tool screen and the timeline export produce the equivalent `video.*` / `audio.*` code, ready to copy or to open as a new script in AutoJs6.
- Studio recorder: record the microphone, the device audio (Android 10 and later) or both from the plugin screens.
- FFmpeg library (1.1.0): an optional download for MP3, WebM and MKV output, command passthrough with `video.ffmpeg` and complete ASS rendering.

Progress and design are available on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio).

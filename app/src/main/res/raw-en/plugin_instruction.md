3-Slate Studio is the video and audio plugin of AutoJs6 and also an editing app that works without AutoJs6. It trims, joins, speeds up, scales, rotates, crops and watermarks videos, extracts frames, makes GIFs and transcodes; it trims, joins, mixes, fades and converts audio; and its multi-track timeline editor combines clips with transitions, overlays and subtitles.

> Version 1.0.0 is the first release. Processing media in scripts needs AutoJs6 6.8.1 (internal build 6641) or later; the plugin screens, the timeline editor and the Studio recorder do not depend on AutoJs6.

### Features

- Video editing: trim by time, join, change the speed (0.25x to 8x), scale, rotate, crop and flip, add text or image watermarks, extract or remove the audio track, and transcode (H.264 by default, H.265 when the device has an H.265 encoder).
- Frames and GIF: single frames, frame sequences and thumbnails as PNG or JPG files or as `Image` objects, and GIF animations from the built-in encoder.
- Audio: trim, join, mix several inputs (scaled down automatically when the sum would clip), adjust the volume with fade in and fade out, change the speed, and convert to M4A, WAV, FLAC or OGG (OGG needs Android 10 or later).
- Multi-track timeline editor: video, audio, overlay and subtitle tracks, 7 transitions (cut, fade, dissolve, slide, wipe, zoom, blur), live preview, undo and redo, and projects saved as `.slate.json`; subtitles support SRT import and export and a basic subset of ASS, burned into the picture on export.
- Toolbox: a wizard for every processing feature, with forms that show defaults and ranges; tasks run in a queue that shows the progress and the reason of failures, and long tasks keep a foreground notification.
- Code generation: every wizard, the timeline export and the recorder produce the equivalent `video.*` / `audio.*` script, ready to copy or to open as a new script in AutoJs6.
- Studio recorder: record the microphone, the device audio (Android 10 and later) or both from the plugin screens as M4A or WAV, with pause, and keep recording with the screen off.
- FFmpeg library (planned for 1.1.0): an optional download that adds formats such as MP3, WebM and MKV and complete ASS subtitle rendering.

Progress and design are available on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio).

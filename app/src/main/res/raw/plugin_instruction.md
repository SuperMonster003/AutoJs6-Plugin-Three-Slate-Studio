3-Slate Studio is a video and audio editing app that also works on its own without AutoJs6. It trims, joins, speeds up and transcodes videos, adds watermarks or makes GIFs; it also trims, mixes and converts audio, and edits several clips into one video on a timeline with transitions and subtitles. With AutoJs6, scripts can do the same processing through the `video` and `audio` modules.

### Getting started

1. Open 3-Slate Studio. If access has not been granted yet, "Library" shows "Grant access"; tap it and allow access to all files on the system page that opens.
2. In "Toolbox", choose an operation such as "Trim video", pick the media with "Browse files" or "From the library", adjust the options and tap "Start".
3. Progress is shown while it runs; when it finishes, the output files, media info and "Equivalent script" appear. All tasks are also listed in "Tasks".
4. Before using it in scripts, make sure this plugin is enabled in "Plugin center" in AutoJs6.

### Main features

- **Toolbox**: Each operation has its own wizard, such as "Trim video", "Add text and images", "Convert to GIF" and "Mix audio"; the forms show default values and allowed ranges.
- **Timeline editor**: Tap "New timeline project" in "Toolbox" to arrange clips on video, audio, overlay and subtitle tracks, add transitions, preview in real time, and undo or redo; "Export" adds the result to the task queue.
- **Subtitles**: In the timeline, "Import subtitles" reads SRT or ASS files, which are drawn into the picture on export; "Toolbox" also has "Burn subtitles" and "Convert subtitles".
- **Audio recording**: Tap the "Record audio" button in the top bar of the home screen or "Record audio" in "Toolbox" to record M4A or WAV from "Microphone", "Playback capture" or "Mixed"; recording can be paused and continues with the screen off.
- **Equivalent script**: Results of the wizards, timeline exports and recordings come with an "Equivalent script" that you can "Copy" or "Open in AutoJs6" as a new unsaved script.
- **Library and tasks**: "Library" lists media in the output folders and the AutoJs6 recording folder, where you can preview, share, rename or "Process with the toolbox"; "Tasks" shows the progress and failure reason of each task and lets you cancel it.
- **Settings**: "Settings" in the top bar adjusts the appearance, "Output folder", "Default video codec", "Concurrent tasks" and more; "Output folder" only affects the toolbox wizards, and scripts keep their own default outputs.

### Using it in scripts

In AutoJs6 scripts, the global `video` module processes videos. The example below takes 5 seconds of `./video.mp4` starting at 2 seconds, prints the progress and converts it to a GIF:

```js
let clip = video.trim("./video.mp4", {
    start: 2000,
    duration: 5000,
    onProgress: (p) => console.log(p.stage, p.percent),
});
let gif = video.toGif(clip.path, { fps: 10, width: 480 });
console.log(gif.path, gif.sizeBytes);
```

The `audio` module processes sound. The example below mixes a voice with quieter background music and then converts the result to FLAC:

```js
let mixed = audio.mix([
    { src: "./voice.m4a" },
    { src: "./music.m4a", volume: 0.3 },
]);
let flac = audio.convert(mixed.path, { format: "flac" });
console.log(flac.path, flac.durationMillis);
```

- Every processing method also has an `Async` form that returns a Promise, such as `video.trimAsync`; the synchronous forms cannot be called on the UI thread. Stopping a script cancels its unfinished tasks.
- Relative paths are resolved against the script's working directory. When the output path is omitted, results are saved by type to the 3-Slate Studio folder in `Movies`, `Music` or `Pictures`.
- Screen recording with `video.record` and audio recording with `audio.record` are done by AutoJs6 and do not need this plugin; multi-track compositions use `video.compose`, which shares the project format of the timeline editor.
- See the [video](https://docs.autojs6.com/#/video) and [audio](https://docs.autojs6.com/#/audio) sections of the AutoJs6 docs for all methods and options.

### Requirements

- Android 7.0 or later. The plugin screens, the timeline editor and audio recording do not need AutoJs6.
- Processing in scripts requires AutoJs6 6.8.1 or later, with this plugin enabled in "Plugin center".
- All files access is used to read and write the files given by scripts and the screens, as well as the default output folders.
- Microphone permission is used for recording in the plugin screens (playback capture needs it too); "Playback capture" and "Mixed" require Android 10 or later, and the system asks you to confirm screen casting first.
- Notification permission is used to show the progress of longer tasks and the recording controls in the notification shade.

### FAQ

**Why all files access is required**

The plugin reads and writes files directly at the paths given by scripts or the screens, and the default output folders are in shared storage. Without access, processing reports `STORAGE_PERMISSION_DENIED` and opens the permission page.

**When a timeline export reports `CODEC_ERROR`**

Devices can only use a limited number of codecs at once, and overlapping transitions decode two videos at the same time. Use fewer video tracks or overlapping transitions, or set "Concurrent tasks" in "Settings" to 1.

**When long tasks are delayed or recording stops with the screen off**

Battery optimization or background restrictions may delay long tasks or stop recording when the screen is off. Tap "Battery use" in "Settings" and set battery use to Unrestricted (or Allow background activity) on the app info page that opens.

**Why some apps are silent in playback capture**

Apps can forbid other apps from capturing the sound they play, and such apps stay silent in playback capture. Use "Microphone" to record those sounds.

Details on the screens, permissions and equivalent scripts are in the [3-Slate Studio](https://docs.autojs6.com/#/threeSlateStudio) section of the AutoJs6 docs.

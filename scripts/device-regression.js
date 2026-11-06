/**
 * 3-Slate Studio device regression (roadmap P2.6): runs every P2 / P3 operation of the `video` / `audio` modules once
 * through the AutoJs6 host (Rhino) and writes one line per operation: PASS / FAIL, the time in milliseconds and a short
 * value (sizes, durations, error code). Also lists the video encoders of the device and measures the 1080p transcode
 * speed (H.264 and, when present, H.265).
 *
 * Configuration: `device-regression.json` next to this script (written by `.python/device_regression.py`):
 * `{"work": "<folder for outputs>", "video": "<1080p clip>", "video2": "<second clip>", "audio": "<m4a>",
 *   "logo": "<png>", "srt": "<srt>", "out": "<result json>", "extreme": false}`. The input media should be inside the
 * plugin's own external files folder. The checks use the returned values. Outputs and the temporary image of
 * `video.frame` are in public folders, so the plugin needs all files access, which the driver grants through appops for
 * the run. `extreme: true` also runs a timeline of 8 video tracks (with dissolves) and 16 audio tracks (appendix D Q34 (c));
 * when it fails, fewer tracks are tried (6, 4, 2 video tracks with 16 audio tracks, then 2 + 8) and the largest passing
 * timeline is reported as `extremeMax`.
 *
 * zh-CN: 3-Slate Studio 真机回归 (路线图 P2.6): 经 AutoJs6 宿主 (Rhino) 把 `video` / `audio` 模块的 P2 / P3 操作各跑一遍, 每个操作
 * 输出一行: PASS / FAIL, 毫秒耗时与简短数值 (尺寸, 时长, 错误码). 另列出设备的视频编码器, 并测量 1080p 转码倍速 (H.264, 有 H.265
 * 时也测). 配置见上 (由 `.python/device_regression.py` 写入); 输入媒体位于插件自己的外部文件目录. 检查都用返回值. 输出与 `video.frame`
 * 的临时图片在公共目录, 插件需要全部文件访问权限, 由驱动脚本在运行期间经 appops 授予.
 * `extreme: true` 时另跑 8 条视频轨 (含溶解转场) + 16 条音频轨的时间线 (附录 D Q34 (c)); 失败时依次减少轨道 (6, 4, 2 条视频轨配 16 条
 * 音频轨, 再 2 + 8), 通过的最大时间线记为 `extremeMax`.
 */

var config = JSON.parse(files.read(files.join(files.cwd(), 'device-regression.json')));
var work = config.work;
var results = [];

function out(name) {
    return work + '/' + name;
}

function brief(value) {
    if (value === null || value === undefined) return '';
    if (typeof value !== 'object') return String(value);
    if (value instanceof Array || value.length !== undefined && typeof value.length === 'number' && !value.width) return value.length + ' items';
    var parts = [];
    if (value.width) parts.push(value.width + 'x' + value.height);
    if (value.durationMillis !== undefined) parts.push(value.durationMillis + ' ms');
    if (value.codec) parts.push(value.codec);
    if (value.sizeBytes !== undefined) parts.push(value.sizeBytes + ' B');
    if (value.warnings && value.warnings.length) parts.push(value.warnings.length + ' warnings');
    return parts.join(', ');
}

function step(name, body, check) {
    var started = Date.now();
    var entry = {name: name};
    try {
        var value = body();
        entry.ms = Date.now() - started;
        entry.returned = true;
        var problem = check ? check(value) : null;
        entry.ok = !problem;
        entry.value = brief(value);
        if (problem) entry.error = problem;
        entry.raw = value && typeof value === 'object' && !(value instanceof Array) && !value.getWidth ? value : undefined;
    } catch (e) {
        entry.ms = Date.now() - started;
        entry.ok = false;
        entry.error = (e && e.code ? e.code + ': ' : '') + String(e && e.message || e).slice(0, 300);
    }
    results.push(entry);
    console.log('[slate-regression] ' + (entry.ok ? 'PASS' : 'FAIL') + ' ' + name + ' ' + entry.ms + ' ms ' + (entry.ok ? entry.value : entry.error));
    return entry;
}

function encoders() {
    var list = new android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).getCodecInfos();
    var found = [];
    for (var i = 0; i < list.length; i++) {
        var info = list[i];
        if (!info.isEncoder()) continue;
        var types = info.getSupportedTypes();
        for (var j = 0; j < types.length; j++) {
            var type = String(types[j]);
            if (type !== 'video/avc' && type !== 'video/hevc') continue;
            var name = String(info.getName());
            var hardware = android.os.Build.VERSION.SDK_INT >= 29
                ? info.isHardwareAccelerated()
                : !/^(OMX\.google\.|c2\.android\.)/.test(name);
            found.push({type: type, name: name, hardware: !!hardware});
        }
    }
    return found;
}

function sameSize(value, width, height) {
    return value.width === width && value.height === height ? null : 'size ' + value.width + 'x' + value.height;
}

var device = {
    model: String(android.os.Build.MODEL),
    sdk: android.os.Build.VERSION.SDK_INT,
    host: app.versionCode,
};
var plugin = {};
try {
    plugin = {available: video.isAvailable(), version: video.version()};
} catch (e) {
    plugin = {available: false, error: String(e.message || e)};
}

if (!plugin.available) {
    // Disabled in Plugin Center or not installed: every step would fail the same way.
    // zh-CN: 在插件中心被关闭或未安装: 每一步都会以同样原因失败.
    files.write(config.out, JSON.stringify({device: device, plugin: plugin, encoders: [], results: []}, null, 2));
    console.log('[slate-regression] plugin unavailable: ' + (plugin.error || ''));
    exit();
}

files.ensureDir(work + '/');
var codecs = encoders();
var hasHevc = codecs.some(function (c) { return c.type === 'video/hevc' && c.hardware; });

var source = step('video.probe', function () { return video.probe(config.video); }, function (v) { return v.width ? null : 'no video'; }).raw || {};
step('audio.probe', function () { return audio.probe(config.audio); }, function (v) { return v.durationMillis > 0 ? null : 'no duration'; });

var bench = step('video.transcode.h264.1080p', function () { return video.transcode(config.video, {codec: 'h264'}, out('bench-h264.mp4')); },
    function (v) { return sameSize(v, source.width, source.height); });
var benchHevc = hasHevc
    ? step('video.transcode.h265.1080p', function () { return video.transcode(config.video, {codec: 'h265'}, out('bench-h265.mp4')); },
        function (v) { return v.codec === 'h265' || v.codec === 'hevc' ? sameSize(v, source.width, source.height) : 'codec ' + v.codec; })
    : null;

step('video.trim', function () { return video.trim(config.video, {start: 1000, end: 4000}, out('trim.mp4')); },
    function (v) { return Math.abs(v.durationMillis - 3000) <= 400 ? null : 'duration ' + v.durationMillis; });
step('video.concat', function () { return video.concat([config.video2, config.video2], out('concat.mp4')); },
    function (v) { return v.durationMillis > 0 ? null : 'empty'; });
step('video.speed', function () { return video.speed(config.video2, {factor: 2}, out('speed.mp4')); }, null);
step('video.transform', function () { return video.transform(config.video2, {rotate: 90, width: 360}, out('transform.mp4')); }, null);
step('video.removeAudio', function () { return video.removeAudio(config.video2, out('remove-audio.mp4')); }, null);
step('video.extractAudio', function () { return video.extractAudio(config.video2, out('extract.m4a')); }, null);
step('video.overlay', function () {
    return video.overlay(config.video2, [
        {type: 'text', text: '3-Slate', fontSize: 48, stroke: 'black', x: '50%', y: '20%'},
        {type: 'image', src: config.logo, x: '90%', y: '90%', scale: 0.5, alpha: 0.8},
    ], out('overlay.mp4'));
}, null);
step('video.frames', function () { return video.frames(config.video2, {count: 5, format: 'jpg'}, out('frames')); },
    function (v) { return v && v.length === 5 ? null : 'frames ' + (v && v.length); });
step('video.frame', function () {
    var image = video.frame(config.video2, 1000);
    var size = {width: image.getWidth(), height: image.getHeight()};
    image.recycle();
    return size;
}, null);
step('video.thumbnail', function () { return video.thumbnail(config.video2, out('thumb.jpg')); }, null);
step('video.toGif', function () { return video.toGif(config.video2, {start: 0, end: 2000, fps: 10, width: 240}, out('clip.gif')); }, null);

step('audio.trim', function () { return audio.trim(config.audio, {start: 500, end: 2500}, out('a-trim.m4a')); }, null);
step('audio.concat', function () { return audio.concat([config.audio, config.audio], out('a-concat.wav')); }, null);
step('audio.mix', function () { return audio.mix([config.audio, {src: config.audio, volume: 0.5, at: 1000}], out('a-mix.m4a')); }, null);
step('audio.volume', function () { return audio.volume(config.audio, {normalize: true, fadeOut: 1000}, out('a-volume.flac')); }, null);
step('audio.convert', function () { return audio.convert(config.audio, out(android.os.Build.VERSION.SDK_INT >= 29 ? 'a-convert.ogg' : 'a-convert.flac')); }, null);
step('audio.speed', function () { return audio.speed(config.audio, {factor: 1.5}, out('a-speed.m4a')); }, null);

step('video.compose', function () {
    return video.compose({
        width: 640, height: 360,
        video: [{clips: [{src: config.video2}, {src: config.video2, transition: {type: 'dissolve', duration: 500}}, {src: config.video2, transition: {type: 'wipe', duration: 500}}]}],
        audio: [{clips: [{src: config.audio, volume: 0.5, fadeIn: 500}]}],
        overlays: [{type: 'text', text: 'compose', from: 0, to: 2000}],
        subtitles: {src: config.srt},
    }, out('compose.mp4'));
}, function (v) { return v.width === 640 ? null : 'size ' + v.width; });
step('video.burnSubtitles', function () { return video.burnSubtitles(config.video2, config.srt, {fontSize: 40}, out('burn.mp4')); }, null);
step('video.subtitles.parse', function () { return video.subtitles.parse(config.srt); },
    function (v) { return v.length > 0 ? null : 'no cues'; });
step('video.subtitles.write', function () {
    return video.subtitles.write(out('written.ass'), [{start: 0, end: 1000, text: 'one'}, {start: 1000, end: 2000, text: 'two'}]);
}, function (v) { return v.sizeBytes > 0 ? null : 'no file'; });

var extremeMax;
if (config.extreme) {
    // Appendix D Q34 (c): 8 video tracks with dissolves (two decoders each) and 16 audio tracks, 32 sources at once.
    // On failure, fewer tracks show what the device can take.
    // zh-CN: 附录 D Q34 (c): 8 条带溶解转场的视频轨 (各两个解码器) 与 16 条音频轨, 同时 32 个来源. 失败时减少轨道, 看设备能承受多少.
    var extreme = function (videoCount, audioCount) {
        var videoTracks = [];
        for (var t = 0; t < videoCount; t++) {
            videoTracks.push({clips: [
                {src: config.video2, transform: {scale: 1 - t * 0.1}},
                {src: config.video2, transition: {type: 'dissolve', duration: 1000}, transform: {scale: 1 - t * 0.1}},
            ]});
        }
        var audioTracks = [];
        for (var a = 0; a < audioCount; a++) audioTracks.push({clips: [{src: config.audio, volume: 0.1}]});
        var name = videoCount + '+' + audioCount;
        var entry = step('video.compose.extreme.' + name, function () {
            return video.compose({width: 640, height: 360, video: videoTracks, audio: audioTracks}, out('compose-extreme-' + name + '.mp4'));
        }, null);
        return entry.ok ? name : null;
    };
    extremeMax = extreme(8, 16);
    var fewer = [[6, 16], [4, 16], [2, 16], [2, 8]];
    for (var k = 0; !extremeMax && k < fewer.length; k++) extremeMax = extreme(fewer[k][0], fewer[k][1]);
    extremeMax = extremeMax || '';
}

var duration = source.durationMillis || 0;
var summary = {
    device: device,
    plugin: plugin,
    encoders: codecs,
    sourceDurationMillis: duration,
    extremeMax: extremeMax,
    // The speed counts every finished transcode, also one whose size check failed (an encoder that aligns the height).
    // zh-CN: 倍速计入所有完成的转码, 包括尺寸检查未通过的 (编码器对齐了高度).
    speedH264: bench.returned && duration ? duration / bench.ms : null,
    speedH265: benchHevc && benchHevc.returned && duration ? duration / benchHevc.ms : null,
    results: results.map(function (r) { return {name: r.name, ok: r.ok, ms: r.ms, value: r.value, error: r.error}; }),
};
files.write(config.out, JSON.stringify(summary, null, 2));
console.log('[slate-regression] DONE ' + results.filter(function (r) { return r.ok; }).length + ' / ' + results.length);

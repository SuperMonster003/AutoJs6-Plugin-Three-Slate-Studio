/**
 * 3-Slate Studio timeline comparison (roadmap P4.3): runs the equivalent `video.compose` script of a timeline editor
 * export through the AutoJs6 host (Rhino) and measures both outputs with the same probe, so
 * `.python/device_timeline_check.py` can compare size, duration, codecs and tracks of the editor export with those of
 * the script.
 *
 * Configuration: `device-timeline-compare.json` next to this script (written by the driver):
 * `{"out": "<result json>", "code": "<script>", "editor": "<editor output>", "script": "<script output>"}`; `code` is
 * the script the result page showed, rewritten to write `script`. The result is `{device, plugin, ok, ms, error,
 * editor: <measure>, script: <measure>}`; a measure holds the probe fields plus the track count and types.
 *
 * zh-CN: 3-Slate Studio 时间线比对 (路线图 P4.3): 经 AutoJs6 宿主 (Rhino) 运行时间线剪辑器导出的等价 `video.compose` 脚本, 并用同一
 * 探测测量两份输出, 供 `.python/device_timeline_check.py` 比对剪辑器导出与脚本产物的尺寸, 时长, 编码与轨道. 配置由驱动写入 (见上);
 * `code` 是结果页显示的脚本, 改写为输出到 `script`. 结果为 `{device, plugin, ok, ms, error, editor, script}`; 测量值为探测字段
 * 以及轨道数与轨道类型.
 */

var config = JSON.parse(files.read(files.join(files.cwd(), 'device-timeline-compare.json')));

var PROBE_FIELDS = ['durationMillis', 'width', 'height', 'frameRate', 'videoCodec', 'audioCodec', 'sampleRate', 'channels', 'mime', 'sizeBytes'];

function measure(path) {
    if (!new java.io.File(path).exists()) return {missing: true};
    var probe = video.probe(path);
    var picked = {};
    PROBE_FIELDS.forEach(function (key) {
        var value = probe[key];
        if (value !== undefined && value !== null) picked[key] = typeof value === 'number' ? value : String(value);
    });
    var tracks = probe.tracks || [];
    picked.trackCount = tracks.length;
    picked.trackTypes = tracks.map(function (track) {
        return String(track.type || track.mime || '?').split('/')[0];
    }).join(',');
    return picked;
}

function attempt(body) {
    try {
        return body();
    } catch (e) {
        return {error: String(e.message || e)};
    }
}

// The script declares its own `result`, so it runs in a function scope of its own. zh-CN: 脚本声明了自己的 `result`, 因此在单独的函数作用域中运行.
function runScript(code) {
    eval(code);
}

var plugin = attempt(function () { return {available: video.isAvailable(), version: video.version()}; });
var summary = {
    device: {model: String(android.os.Build.MODEL), sdk: android.os.Build.VERSION.SDK_INT, host: app.versionCode},
    plugin: plugin,
};
if (plugin.available) {
    var started = Date.now();
    try {
        runScript(config.code);
        summary.ok = true;
    } catch (e) {
        summary.ok = false;
        summary.error = String(e.message || e);
    }
    summary.ms = Date.now() - started;
    summary.editor = attempt(function () { return measure(config.editor); });
    summary.script = attempt(function () { return measure(config.script); });
}
files.write(config.out, JSON.stringify(summary, null, 2));
console.log('[slate-timeline-compare] DONE ' + (summary.ok ? 'ok' : 'failed'));

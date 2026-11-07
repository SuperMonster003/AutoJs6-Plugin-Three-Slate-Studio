/**
 * 3-Slate Studio wizard comparison (roadmap P4.2): runs the equivalent script call of every Studio wizard through the
 * AutoJs6 host (Rhino) and measures both outputs with the same probes, so `.python/device_wizard_check.py` can compare
 * what the wizard produced with what the script produced.
 *
 * Configuration: `device-wizard-compare.json` next to this script (written by the driver):
 * `{"out": "<result json>", "items": [{"op", "kind", "code", "wizard", "script"}]}`; `code` is the call the wizard showed,
 * rewritten to write `script` instead of `wizard`. `kind` is video, audio, image, gif, folder or subtitle. Each item
 * answers `{op, ok, ms, error, wizard: <measure>, script: <measure>}`; a measure holds the probe fields of a media file,
 * the size of an image, the count and first size of a folder, or the text of a subtitle file.
 *
 * zh-CN: 3-Slate Studio 向导比对 (路线图 P4.2): 经 AutoJs6 宿主 (Rhino) 运行每个 Studio 向导的等价脚本调用, 并用同一组探测
 * 测量两份输出, 供 `.python/device_wizard_check.py` 比对向导与脚本的产物. 配置由驱动写入 (见上); `code` 是向导显示的调用,
 * 改写为输出到 `script`. `kind` 为 video, audio, image, gif, folder 或 subtitle. 每项结果为 `{op, ok, ms, error, wizard, script}`;
 * 测量值为媒体文件的探测字段, 图片尺寸, 目录的文件数与首个尺寸, 或字幕文件的文本.
 */

var config = JSON.parse(files.read(files.join(files.cwd(), 'device-wizard-compare.json')));

var PROBE_FIELDS = ['durationMillis', 'width', 'height', 'codec', 'videoCodec', 'audioCodec', 'sampleRate', 'channels', 'mime', 'sizeBytes'];

function imageSize(path) {
    var image = images.read(path);
    if (!image) return {missing: true};
    try {
        return {width: image.getWidth(), height: image.getHeight(), sizeBytes: new java.io.File(path).length()};
    } finally {
        image.recycle();
    }
}

function measure(kind, path) {
    var file = new java.io.File(path);
    if (!file.exists()) return {missing: true};
    if (kind === 'video' || kind === 'audio') {
        var probe = kind === 'video' ? video.probe(path) : audio.probe(path);
        var picked = {};
        PROBE_FIELDS.forEach(function (key) {
            var value = probe[key];
            if (value !== undefined && value !== null) picked[key] = typeof value === 'number' ? value : String(value);
        });
        return picked;
    }
    if (kind === 'image' || kind === 'gif') return imageSize(path);
    if (kind === 'folder') {
        var names = files.listDir(path).slice().sort();
        var first = names.length ? imageSize(files.join(path, names[0])) : {};
        return {count: names.length, width: first.width, height: first.height};
    }
    return {text: String(files.read(path))};
}

function attempt(body) {
    try {
        return body();
    } catch (e) {
        return {error: String(e.message || e)};
    }
}

var plugin = attempt(function () { return {available: video.isAvailable(), version: video.version()}; });
var results = [];
if (plugin.available) {
    config.items.forEach(function (item) {
        var entry = {op: item.op};
        var started = Date.now();
        try {
            // The code is a complete statement list, as the wizard shows it.
            // zh-CN: 代码是完整的语句, 与向导显示的一致.
            eval(item.code);
            entry.ok = true;
        } catch (e) {
            entry.ok = false;
            entry.error = String(e.message || e);
        }
        entry.ms = Date.now() - started;
        entry.wizard = attempt(function () { return measure(item.kind, item.wizard); });
        entry.script = attempt(function () { return measure(item.kind, item.script); });
        results.push(entry);
    });
}
files.write(config.out, JSON.stringify({
    device: {model: String(android.os.Build.MODEL), sdk: android.os.Build.VERSION.SDK_INT, host: app.versionCode},
    plugin: plugin,
    results: results,
}, null, 2));
console.log('[slate-wizard-compare] DONE ' + results.length);

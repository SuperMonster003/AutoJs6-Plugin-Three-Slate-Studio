/**
 * 录音转 FLAC [v6.8.1+]
 *
 * audio.record 由 AutoJs6 自己录制, 不经插件; audio.convert 由 3-Slate Studio 插件加工: 先在插件中心安装并启用.
 * 演示:
 * 1. audio.record 用麦克风录音 (默认单声道 48 kHz, 128 kbps 的 M4A; 首次运行时按提示授予录音权限),
 *    maxDuration 为保险上限, 到时自动停止; onStop 在录音结束时运行一次 (失败时 reason 为 'recorder-error', error 为原因);
 * 2. 几秒后 stop() 停止并返回录音结果, 再用 audio.convert 转为无损 FLAC.
 * 录音文件先用临时名写在目标旁边, 停止时才移到目标位置, 取消 (cancel()) 或失败不会留下半截文件.
 * 内录 (source: 'playback' 或 'mixed') 需要 Android 10+, 并且要先调用 images.requestScreenCapture.
 * 可选参数 (engines.execScriptFile 的 arguments): seconds 为录音秒数, output 为写入结果摘要 (JSON) 的文件.
 */

let args = engines.myEngine().execArgv;
let seconds = args.seconds ? Number(args.seconds) : 5;
let summary = { ok: false };

try {
    /* 省略路径时保存到 Music/AutoJs6/audio-<日期>-<时间>.m4a; 显式路径以 .m4a 或 .wav 结尾, format 随扩展名. */
    let recording = audio.record({
        source: 'mic',
        maxDuration: 60 * 1000,
        onStop: (info) => console.log('录音结束:', info.reason, info.error || ''),
    });
    toastLog('开始录音 ' + seconds + ' 秒');
    sleep(seconds * 1000);
    let result = recording.stop();
    summary.recording = result.path;
    console.log('录音:', result.path, '| 时长:', result.durationMillis, 'ms', '| 大小:', result.sizeBytes, '字节');

    let flac = audio.convert(result.path, { format: 'flac' });
    summary.ok = true;
    summary.flac = flac.path;
    toastLog('FLAC 已保存到 ' + flac.path);
} catch (e) {
    /* MediaStudioError 带有 code (例如 PERMISSION_DENIED 表示缺少录音权限, PLUGIN_DISABLED 表示插件已在插件中心关闭). */
    summary.code = e.code;
    toastLog('失败 [' + e.code + ']: ' + e.message);
}

if (args.output) {
    files.write(String(args.output), JSON.stringify(summary));
}

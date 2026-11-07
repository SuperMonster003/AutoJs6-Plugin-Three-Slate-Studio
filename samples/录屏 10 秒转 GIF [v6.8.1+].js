/**
 * 录屏 10 秒转 GIF [v6.8.1+]
 *
 * video.record 由 AutoJs6 自己录制, 不经插件; video.toGif 由 3-Slate Studio 插件加工: 先在插件中心安装并启用.
 * 演示:
 * 1. images.requestScreenCapture 申请截图授权: 录屏使用同一个截图会话, 首次运行时在弹出的对话框中确认;
 * 2. video.record 开始录制无声屏幕, 10 秒后 stop() 停止并返回录制结果
 *    (省略路径时保存到 Movies/AutoJs6/screen-<日期>-<时间>.mp4; 录制中的文件先用临时名, 停止时才移到目标位置);
 * 3. video.toGif 把录像转为每秒 10 帧, 宽 480 像素的 GIF, onProgress 报告进度 {percent, stage, etaMillis, taskId}.
 * video.record 与同步加工方法都不能在 UI 线程调用; 界面脚本里的加工改用 xxxAsync 形态 (例如 video.toGifAsync 返回 Promise).
 * 可选参数 (engines.execScriptFile 的 arguments): seconds 为录制秒数, output 为写入结果摘要 (JSON) 的文件.
 */

let args = engines.myEngine().execArgv;
let seconds = args.seconds ? Number(args.seconds) : 10;
let summary = { ok: false };

if (!video.isAvailable()) {
    toastLog('3-Slate Studio 插件不可用, 请在插件中心安装或启用后重试');
    exit();
}
if (!images.requestScreenCapture()) {
    toastLog('未获得截图授权, 无法录屏');
    exit();
}

try {
    /* audio: 'none' 为无声 (默认), 也可以是 'mic' (麦克风), 'playback' (内录, Android 10+) 或 'mixed' (两者混合). */
    let recording = video.record({ audio: 'none', frameRate: 30 });
    toastLog('开始录屏 ' + seconds + ' 秒');
    sleep(seconds * 1000);
    let clip = recording.stop();
    summary.clip = clip.path;
    console.log('录像:', clip.path, '| 时长:', clip.durationMillis, 'ms', '| 尺寸:', clip.width + 'x' + clip.height);

    let gif = video.toGif(clip.path, {
        fps: 10,
        width: 480,
        onProgress: (p) => console.log('GIF', p.stage, Math.round(p.percent) + '%'),
    });
    summary.ok = true;
    summary.gif = gif.path;
    /* warnings 为插件给出的提示 (例如帧数超过上限时已自动降帧), 原样转交. */
    gif.warnings.forEach((w) => console.warn(w));
    toastLog('GIF 已保存到 ' + gif.path);
} catch (e) {
    /* e 是 MediaStudioError: code 为稳定的错误码 (例如 PLUGIN_UNAVAILABLE 表示未安装插件, INVALID_OPTIONS 表示参数有误), taskId 为所属任务. */
    summary.code = e.code;
    toastLog('失败 [' + e.code + ']: ' + e.message);
}

if (args.output) {
    files.write(String(args.output), JSON.stringify(summary));
}

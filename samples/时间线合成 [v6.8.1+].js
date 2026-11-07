/**
 * 时间线合成 [v6.8.1+]
 *
 * video.compose 由 3-Slate Studio 插件按时间线 JSON 多轨合成: 先在插件中心安装并启用, 再按插件首页的提示授予 "所有文件访问权限".
 * 演示:
 * 1. 时间线写明画面尺寸与帧率, video 为视频轨道数组, audio 为音频轨道数组, overlays 为文字 / 图片叠加层, 轨道顺序即上下层次;
 * 2. 每段 clip 的 start / end 是在素材里截取的区间, at 是放到时间线上的位置, 时间单位都是毫秒;
 * 3. transition 写在后一段上, 表示与前一段重叠部分的转场, type 可取 cut, fade, dissolve, slide, wipe, zoom, blur;
 * 4. compose 的输出路径与选项都可省略, 省略路径时保存到 Movies/3-Slate Studio/slate-compose-<日期>-<时间>.mp4.
 * 合成耗时较长, 界面脚本里改用 video.composeAsync (返回 Promise), 脚本停止时插件中的任务一并取消.
 * 可选参数 (engines.execScriptFile 的 arguments): a / b 为两段素材 (mp4), bgm 为背景音乐 (m4a), output 为写入结果摘要 (JSON) 的文件.
 */

let args = engines.myEngine().execArgv;
let sdcard = files.getSdcardPath();
let a = args.a ? String(args.a) : files.join(sdcard, 'Movies', 'a.mp4');
let b = args.b ? String(args.b) : files.join(sdcard, 'Movies', 'b.mp4');
let bgm = args.bgm ? String(args.bgm) : files.join(sdcard, 'Music', 'bgm.m4a');
let summary = { ok: false };

if (!video.isAvailable()) {
    toastLog('3-Slate Studio 插件不可用, 请在插件中心安装或启用后重试');
    exit();
}

let timeline = {
    width: 1080,
    height: 1920,
    frameRate: 30,
    background: '#000000',
    video: [
        {
            clips: [
                /* 素材 a 的 0 - 8 秒放在时间线 0 秒处. */
                { src: a, start: 0, end: 8000, at: 0 },
                /* 素材 b 的 2 - 10 秒从 7.5 秒开始, 与前一段重叠 0.5 秒, 重叠部分叠化过渡. */
                { src: b, start: 2000, end: 10000, at: 7500, transition: { type: 'dissolve', duration: 500 } },
            ],
        },
    ],
    audio: [
        /* 背景音乐音量 40%, 开头 1 秒淡入, 结尾 2 秒淡出. */
        { clips: [{ src: bgm, at: 0, end: 15500, volume: 0.4, fadeIn: 1000, fadeOut: 2000 }] },
    ],
    overlays: [
        /* from / to 是叠加层在时间线上的显示区间. */
        { type: 'text', text: 'AutoJs6', from: 1000, to: 4000, x: '50%', y: '10%', fontSize: 48, color: '#FFFFFF', stroke: '#000000' },
    ],
};

try {
    let result = video.compose(timeline, {
        onProgress: (p) => console.log('合成', p.stage, Math.round(p.percent) + '%', p.etaMillis >= 0 ? '剩余约 ' + Math.ceil(p.etaMillis / 1000) + ' 秒' : ''),
    });
    summary.ok = true;
    summary.path = result.path;
    summary.durationMillis = result.durationMillis;
    result.warnings.forEach((w) => console.warn(w));
    toastLog('已保存到 ' + result.path);
} catch (e) {
    /* 时间线有误 (例如素材不存在, 重叠时长与转场时长不符) 时 code 为 INVALID_OPTIONS 或 IO_ERROR, message 说明原因. */
    summary.code = e.code;
    toastLog('失败 [' + e.code + ']: ' + e.message);
}

if (args.output) {
    files.write(String(args.output), JSON.stringify(summary));
}

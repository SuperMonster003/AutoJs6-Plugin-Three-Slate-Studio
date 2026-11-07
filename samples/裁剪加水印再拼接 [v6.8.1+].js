/**
 * 裁剪加水印再拼接 [v6.8.1+]
 *
 * video 的加工方法由 3-Slate Studio 插件提供: 先在插件中心安装并启用, 再按插件首页的提示授予 "所有文件访问权限".
 * 演示:
 * 1. video.probe 读取素材的时长与尺寸;
 * 2. video.trim 截取前 5 秒, video.overlay 在第二段上叠加文字水印;
 * 3. video.concat 把两段拼成一个视频, onProgress 报告进度; 输出路径省略时保存到 Movies/3-Slate Studio/<源名>-<操作>-<日期>-<时间>.mp4.
 * 参数顺序: 第一个参数为素材, 其后的字符串为输出路径, 对象为选项, 两者都可省略且先后不限.
 * 可选参数 (engines.execScriptFile 的 arguments): a / b 为两段素材 (mp4), output 为写入结果摘要 (JSON) 的文件.
 */

let args = engines.myEngine().execArgv;
let a = args.a ? String(args.a) : files.join(files.getSdcardPath(), 'Movies', 'a.mp4');
let b = args.b ? String(args.b) : files.join(files.getSdcardPath(), 'Movies', 'b.mp4');
let summary = { ok: false };

if (!video.isAvailable()) {
    toastLog('3-Slate Studio 插件不可用, 请在插件中心安装或启用后重试');
    exit();
}

try {
    let info = video.probe(a);
    console.log('素材 a:', info.durationMillis, 'ms', info.width + 'x' + info.height);

    /* start / end / duration 以毫秒计; end 与 duration 二选一. */
    let head = video.trim(a, { start: 0, end: Math.min(5000, info.durationMillis) });
    console.log('裁剪完成:', head.path);

    /* overlays 数组的每一项是一层叠加: type 为 'text' 或 'image', x / y 可以写百分比. */
    let marked = video.overlay(b, [
        { type: 'text', text: 'AutoJs6', x: '50%', y: '8%', fontSize: 48, color: '#FFFFFF', stroke: '#000000' },
    ]);
    console.log('水印完成:', marked.path);

    let joined = video.concat([head.path, marked.path], {
        fit: 'contain',
        onProgress: (p) => console.log('拼接', p.stage, Math.round(p.percent) + '%'),
    });
    summary.ok = true;
    summary.path = joined.path;
    summary.durationMillis = joined.durationMillis;
    toastLog('已保存到 ' + joined.path);
} catch (e) {
    /* 常见错误码: IO_ERROR (素材不存在或无法写入), STORAGE_PERMISSION_DENIED (插件缺少所有文件访问权限), UNSUPPORTED_FORMAT, CODEC_ERROR. */
    summary.code = e.code;
    toastLog('失败 [' + e.code + ']: ' + e.message);
}

if (args.output) {
    files.write(String(args.output), JSON.stringify(summary));
}

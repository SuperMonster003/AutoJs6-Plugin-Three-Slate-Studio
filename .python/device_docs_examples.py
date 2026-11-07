"""Runs the examples of the documentation pages api/video.md and api/audio.md on a device (roadmap P5.1).

Reads every ```js block of the two pages in the sibling AutoJs6-Documentation repository, maps the example paths under
``/sdcard/Movies``, ``/sdcard/Music`` and ``/sdcard/Pictures`` to a scratch folder (``/sdcard/Download/slate-docs``),
generates the input files on the PC with ffmpeg (a.mp4, b.mp4, big.mp4, bgm.m4a, a.mp3, a.flac, ding.ogg and the others
the examples name), pushes them and one generated script that runs each block in its own function, and prints one line
per block: PASS / FAIL / SKIP, milliseconds and what the block printed. A block passes when it throws nothing, logs no
console.error and, when it calls toast (the Async example), the toast arrives. The examples that request a screen
capture show the system dialog, so they only run with ``--interactive`` (someone confirms the dialog on the phone; this
script never taps). The plugin gets all files access through appops for the run and its previous mode is restored.
Outputs the examples leave in the default folders (Movies / Music / Pictures "3-Slate Studio", Movies / Music
"AutoJs6") are found by comparing the folders before and after the run and deleted with their MediaStore rows, as are
the scratch folder and the pushed files (``--keep`` keeps them).

zh-CN: 在真机上运行文档站 api/video.md 与 api/audio.md 页的示例 (路线图 P5.1). 读取同级 AutoJs6-Documentation 仓库两页中的每个
```js 代码块, 把示例中 ``/sdcard/Movies``, ``/sdcard/Music`` 与 ``/sdcard/Pictures`` 下的路径映射到临时目录 (``/sdcard/Download/slate-docs``),
在 PC 上用 ffmpeg 生成示例用到的输入文件 (a.mp4, b.mp4, big.mp4, bgm.m4a, a.mp3, a.flac, ding.ogg 等), 推送这些文件与一个生成的脚本
(每个代码块在各自的函数中运行), 每个代码块输出一行: PASS / FAIL / SKIP, 毫秒与代码块打印的内容. 代码块不抛异常, 不调用
console.error, 且调用 toast 时 (Async 示例) toast 到达, 即为通过. 请求截图会话的示例会弹出系统对话框, 只在 ``--interactive`` 时运行
(由人在手机上确认对话框; 本脚本不点按). 运行期间经 appops 授予插件全部文件访问权限, 结束后还原原模式. 示例在默认目录
(Movies / Music / Pictures 的 "3-Slate Studio", Movies / Music 的 "AutoJs6") 留下的输出按运行前后的目录比较找出, 连同 MediaStore 记录
一并删除; 临时目录与推送的文件同样删除 (``--keep`` 保留).

Usage: py -3.12 .python/device_docs_examples.py --serial 968e9f18 [--interactive] [--docs ../AutoJs6-Documentation]
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
HOST = "org.autojs.autojs6"
RUN_INTENT = f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity"
BASE = "/sdcard/Download/slate-docs"
STAGING = "/data/local/tmp/slate-docs"
DEFAULT_FFMPEG = r"D:\Program Files\MeGUI_2418_x86\tools\ffmpeg\ffmpeg.exe"
PAGES = ("video", "audio")
DEFAULT_FOLDERS = ("/sdcard/Movies/3-Slate Studio", "/sdcard/Music/3-Slate Studio", "/sdcard/Pictures/3-Slate Studio",
                   "/sdcard/Movies/AutoJs6", "/sdcard/Music/AutoJs6")
MAPPED = re.compile(r"/sdcard/(Movies|Music|Pictures)/")

SRT = (
    "1\r\n00:00:00,500 --> 00:00:02,500\r\nfirst cue\r\n\r\n"
    "2\r\n00:00:03,000 --> 00:00:05,000\r\n<i>second</i> cue\r\n"
)
ASS = (
    "[Script Info]\r\nScriptType: v4.00+\r\nPlayResX: 1920\r\nPlayResY: 1080\r\n\r\n"
    "[V4+ Styles]\r\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, "
    "Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, "
    "MarginR, MarginV, Encoding\r\nStyle: Default,sans-serif,54,&H00FFFFFF,&H00FFFFFF,&H00000000,&H80000000,0,0,0,0,"
    "100,100,0,0,1,3,0,2,60,60,60,1\r\n\r\n"
    "[Events]\r\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\r\n"
    "Dialogue: 0,0:00:00.50,0:00:02.50,Default,,0,0,0,,first cue\r\n"
    "Dialogue: 0,0:00:03.00,0:00:05.00,Default,,0,0,0,,{\\i1}second{\\i0} cue\r\n"
)

# Input files the examples name, by mapped path (folder/name): ffmpeg arguments, or text.
# zh-CN: 示例用到的输入文件, 按映射后的路径 (目录/文件名): ffmpeg 参数或文本.
CLIP = ["-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "30", "-c:a", "aac", "-b:a", "128k", "-shortest"]
INPUTS: dict[str, list[str] | str] = {
    "Movies/a.mp4": ["-f", "lavfi", "-i", "testsrc2=size=1280x720:rate=30:duration=12", "-f", "lavfi", "-i", "sine=frequency=440:duration=12", *CLIP],
    "Movies/b.mp4": ["-f", "lavfi", "-i", "smptebars=size=1280x720:rate=30:duration=12", "-f", "lavfi", "-i", "sine=frequency=660:duration=12", *CLIP],
    "Movies/big.mp4": ["-f", "lavfi", "-i", "testsrc2=size=1920x1080:rate=30:duration=8", "-f", "lavfi", "-i", "sine=frequency=550:duration=8", *CLIP],
    "Movies/a.srt": SRT,
    "Movies/a.ass": ASS,
    "Pictures/logo.png": ["-f", "lavfi", "-i", "testsrc=size=256x256:rate=1:duration=1", "-frames:v", "1"],
    "Music/bgm.m4a": ["-f", "lavfi", "-i", "sine=frequency=330:duration=20", "-c:a", "aac", "-b:a", "128k"],
    "Music/voice.m4a": ["-f", "lavfi", "-i", "sine=frequency=880:duration=10", "-c:a", "aac", "-b:a", "128k"],
    "Music/bgm.mp3": ["-f", "lavfi", "-i", "sine=frequency=220:duration=20", "-c:a", "libmp3lame", "-b:a", "128k"],
    "Music/a.flac": ["-f", "lavfi", "-i", "sine=frequency=440:duration=5", "-c:a", "flac"],
    "Music/a.mp3": ["-f", "lavfi", "-i", "sine=frequency=500:duration=60", "-c:a", "libmp3lame", "-b:a", "128k"],
    "Music/intro.wav": ["-f", "lavfi", "-i", "sine=frequency=700:duration=3", "-c:a", "pcm_s16le"],
    "Music/body.m4a": ["-f", "lavfi", "-i", "sine=frequency=600:duration=5", "-c:a", "aac", "-b:a", "128k"],
    "Music/voice.wav": ["-f", "lavfi", "-i", "sine=frequency=900:duration=8", "-c:a", "pcm_s16le"],
    "Music/ding.ogg": ["-f", "lavfi", "-i", "sine=frequency=1200:duration=1", "-c:a", "libopus", "-b:a", "64k"],
    "Music/a.wav": ["-f", "lavfi", "-i", "sine=frequency=440:duration=5", "-ac", "2", "-c:a", "pcm_s16le"],
    "Music/lecture.m4a": ["-f", "lavfi", "-i", "sine=frequency=300:duration=10", "-c:a", "aac", "-b:a", "128k"],
}

HARNESS = r"""
var BASE = %(base)s;
var results = [];
var pendingToasts = 0;
files.write(BASE + '/started', String(Date.now()));

function text(args) {
    var parts = [];
    for (var i = 0; i < args.length; i++) {
        var value = args[i];
        parts.push(value !== null && typeof value === 'object' && !(value instanceof java.lang.Object) ? JSON.stringify(value) : String(value));
    }
    return parts.join(' ');
}

function recorder(entry) {
    function log(kind) {
        return function () {
            var line = text(arguments);
            entry.logs.push(kind + ': ' + line);
            if (kind === 'error') entry.loggedError = line;
            console[kind === 'error' ? 'error' : 'log']('[slate-docs] ' + entry.name + ' ' + line);
        };
    }
    return { log: log('log'), info: log('log'), warn: log('warn'), error: log('error') };
}

function run(name, body, before, expectToast) {
    var entry = { name: name, logs: [], toasts: [], expectToast: expectToast };
    results.push(entry);
    if (expectToast) pendingToasts++;
    var started = Date.now();
    try {
        if (before) before();
        body(recorder(entry), function (message) {
            entry.toasts.push(String(message));
            entry.toastMs = Date.now() - started;
            pendingToasts--;
        });
        entry.ms = Date.now() - started;
        entry.ok = !entry.loggedError;
        if (entry.loggedError) entry.error = 'console.error: ' + entry.loggedError;
    } catch (e) {
        entry.ms = Date.now() - started;
        entry.ok = false;
        entry.error = (e && e.code ? e.code + ': ' : '') + String(e && e.message !== undefined ? e.message : e);
        if (expectToast) pendingToasts--;
    }
}

%(blocks)s

var waited = 0;
var timer = setInterval(function () {
    waited += 1000;
    if (pendingToasts > 0 && waited < %(asyncTimeout)d) return;
    clearInterval(timer);
    results.forEach(function (entry) {
        if (entry.expectToast && entry.ok && entry.toasts.length === 0) {
            entry.ok = false;
            entry.error = 'no toast within ' + (%(asyncTimeout)d / 1000) + ' s';
        }
    });
    var tasks = [];
    try { tasks = video.tasks().map(function (t) { return t.taskId + ' ' + t.state; }); } catch (e) { tasks = ['tasks: ' + e]; }
    files.write(BASE + '/result.json', JSON.stringify({
        device: { model: String(device.model), sdk: device.sdkInt },
        results: results,
        tasksLeft: tasks,
    }));
}, 1000);
"""


def adb(serial: str, *args: str, check: bool = True, timeout: int = 300) -> str:
    env = dict(os.environ, MSYS_NO_PATHCONV="1")
    done = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout, env=env)
    if check and done.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {done.stderr.strip() or done.stdout.strip()}")
    return done.stdout


def blocks(docs: Path) -> list[dict]:
    found = []
    for page in PAGES:
        source = (docs / "api" / f"{page}.md").read_text(encoding="utf-8")
        for index, match in enumerate(re.finditer(r"```js\n(.*?)```", source, re.S)):
            code = match.group(1)
            found.append({
                "name": f"{page}#{index}",
                "code": MAPPED.sub(lambda m: f"{BASE}/{m.group(1)}/", code),
                # Requesting a screen capture shows the system dialog (zh-CN: 请求截图会话会弹出系统对话框).
                "interactive": "requestScreenCapture" in code,
                "toast": "toast(" in code,
                "line": source.count("\n", 0, match.start()) + 1,
            })
    return found


def script(found: list[dict], interactive: bool, async_timeout: int) -> str:
    parts = []
    for block in found:
        name = json.dumps(block["name"])
        if block["interactive"] and not interactive:
            parts.append(f"results.push({{ name: {name}, skipped: 'interactive (system dialog)', logs: [], toasts: [] }});")
            continue
        before = "null"
        if "subtitles.write(" in block["code"]:
            # The example writes over the parse example's input; an existing explicit output is IO_ERROR.
            # zh-CN: 该示例的输出是解析示例的输入; 显式输出已存在时为 IO_ERROR.
            targets = re.findall(r"subtitles\.write\('([^']+)'", block["code"])
            before = "function () { " + " ".join(f"files.remove({json.dumps(t)});" for t in targets) + " }"
        parts.append(f"run({name}, function (console, toast) {{\n{block['code']}}}, {before}, {json.dumps(block['toast'])});")
    return HARNESS % {"base": json.dumps(BASE), "blocks": "\n".join(parts), "asyncTimeout": async_timeout * 1000}


def make_inputs(binary: str, directory: Path) -> dict[str, Path]:
    made = {}
    for key, recipe in INPUTS.items():
        path = directory / key.replace("/", "__")
        if isinstance(recipe, str):
            path.write_bytes(recipe.encode("utf-8"))
        elif not path.exists():
            subprocess.run([binary, "-hide_banner", "-loglevel", "error", "-y", *recipe, "-f", ffmpeg_format(key), str(path)], check=True)
        made[key] = path
    return made


def ffmpeg_format(key: str) -> str:
    return {"mp4": "mp4", "m4a": "ipod", "mp3": "mp3", "flac": "flac", "wav": "wav", "ogg": "ogg", "png": "image2"}[key.rsplit(".", 1)[1]]


def listing(serial: str) -> dict[str, set[str] | None]:
    found: dict[str, set[str] | None] = {}
    for folder in DEFAULT_FOLDERS:
        listed = subprocess.run(["adb", "-s", serial, "shell", f"ls -1a '{folder}'"], capture_output=True, text=True, encoding="utf-8",
                                errors="replace", timeout=60, env=dict(os.environ, MSYS_NO_PATHCONV="1"))
        found[folder] = None if listed.returncode != 0 else {line.strip() for line in listed.stdout.splitlines() if line.strip() not in ("", ".", "..")}
    return found


def media_delete(serial: str, like: str) -> None:
    # adb joins the arguments with spaces, so the condition is quoted for the device shell.
    # zh-CN: adb 用空格拼接参数, 因此条件要为设备上的 shell 加引号.
    adb(serial, "shell", "content", "delete", "--uri", "content://media/external/file", "--where", f"\"_data LIKE '{like}'\"", check=False)


def clean(serial: str, before: dict[str, set[str] | None]) -> list[str]:
    removed = []
    after = listing(serial)
    for folder, names in after.items():
        if names is None:
            continue
        old = before.get(folder)
        for name in sorted(names - (old or set())):
            adb(serial, "shell", f"rm -rf '{folder}/{name}'", check=False)
            media_delete(serial, f"%{folder[len('/sdcard'):]}/{name}%")
            removed.append(f"{folder}/{name}")
        if old is None:
            # The run created the folder (zh-CN: 运行时新建的目录).
            adb(serial, "shell", f"rmdir '{folder}'", check=False)
            media_delete(serial, f"%{folder[len('/sdcard'):]}")
            removed.append(folder + "/")
    adb(serial, "shell", f"rm -rf '{BASE}' '{STAGING}'", check=False)
    media_delete(serial, "%/Download/slate-docs%")
    return removed


def launcher(serial: str) -> str:
    listed = adb(serial, "shell", "cmd", "package", "query-activities", "--brief", "-a", "android.intent.action.MAIN",
                 "-c", "android.intent.category.LAUNCHER", HOST)
    own = [line.strip() for line in listed.splitlines() if line.strip().startswith(HOST + "/") and "leakcanary" not in line]
    return own[0] if own else f"{HOST}/org.autojs.autojs.ui.splash.SplashActivity"


def run(serial: str, made: dict[str, Path], code: str, timeout: int) -> dict | None:
    adb(serial, "shell", f"rm -rf '{BASE}' '{STAGING}'; mkdir -p '{STAGING}' '{BASE}/Movies' '{BASE}/Music' '{BASE}/Pictures'", check=False)
    # Pushing straight into shared storage can fail when adb sets the owner (XQ-DQ72), so stage in /data/local/tmp.
    # zh-CN: 直接推进共享存储可能在 adb 设置属主时失败 (XQ-DQ72), 因此先放 /data/local/tmp.
    for key, path in made.items():
        adb(serial, "push", str(path), f"{STAGING}/{path.name}")
        adb(serial, "shell", f"cp '{STAGING}/{path.name}' '{BASE}/{key}'")
    with tempfile.TemporaryDirectory() as temporary:
        local = Path(temporary) / "docs-examples.js"
        local.write_text(code, encoding="utf-8")
        adb(serial, "push", str(local), f"{STAGING}/docs-examples.js")
        adb(serial, "shell", f"cp '{STAGING}/docs-examples.js' '{BASE}/docs-examples.js'")
    adb(serial, "shell", f"rm -rf '{STAGING}'", check=False)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "am", "start", "-n", launcher(serial), check=False)
    # A cold start goes through the splash screen to MainActivity; a run intent sent before that lands under
    # MainActivity and only runs when RunIntentActivity is resumed again (23046RP50C, right after installing the host).
    # zh-CN: 冷启动先经启动页再到 MainActivity; 在此之前发出的运行意图会被 MainActivity 盖住, 直到 RunIntentActivity 再次
    # 回到前台才运行 (23046RP50C, 刚装好宿主时).
    settle = time.time() + 30
    while time.time() < settle:
        if f"{HOST}/org.autojs.autojs.ui.main.MainActivity" in adb(serial, "shell", "dumpsys", "activity", "activities", check=False).split("topResumedActivity", 1)[-1][:300]:
            break
        time.sleep(1)
    time.sleep(2)
    deadline = time.time() + timeout
    for attempt in range(3):
        adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
            "-d", f"file://{BASE}/docs-examples.js", "-n", RUN_INTENT)
        started = time.time() + 30
        while time.time() < started:
            if adb(serial, "shell", f"ls '{BASE}/started' 2>/dev/null", check=False).strip():
                break
            time.sleep(2)
        else:
            print(f"the script did not start within 30 s, sending the run intent again ({attempt + 1})")
            continue
        break
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{BASE}/result.json", check=False)
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(3)
    return None


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--docs", type=Path, default=ROOT.parent / "AutoJs6-Documentation")
    parser.add_argument("--interactive", action="store_true", help="also run the examples that request a screen capture (confirm the dialogs on the phone)")
    parser.add_argument("--only", default="", help="comma-separated block names to run, e.g. video#3,audio#2")
    parser.add_argument("--ffmpeg", default=shutil.which("ffmpeg") or DEFAULT_FFMPEG)
    parser.add_argument("--timeout", type=int, default=900)
    parser.add_argument("--async-timeout", type=int, default=180)
    parser.add_argument("--cache", type=Path, default=Path(tempfile.gettempdir()) / "slate-docs-sources")
    parser.add_argument("--json", type=Path, help="save the result JSON here")
    parser.add_argument("--keep", action="store_true", help="keep the pushed files and outputs on the device")
    args = parser.parse_args(argv)
    sys.stdout.reconfigure(encoding="utf-8")
    found = blocks(args.docs)
    if args.only:
        wanted = {name.strip() for name in args.only.split(",") if name.strip()}
        found = [block for block in found if block["name"] in wanted]
    args.cache.mkdir(parents=True, exist_ok=True)
    made = make_inputs(args.ffmpeg, args.cache)
    code = script(found, args.interactive, args.async_timeout)
    before = listing(args.serial)
    mode = adb(args.serial, "shell", "appops", "get", PLUGIN, "MANAGE_EXTERNAL_STORAGE", check=False).partition(":")[2].split(";")[0].strip() or "default"
    removed: list[str] = []
    try:
        adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", "allow", check=False)
        result = run(args.serial, made, code, args.timeout)
    finally:
        adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", mode if mode in ("allow", "ignore", "deny", "default") else "default", check=False)
        if not args.keep:
            removed = clean(args.serial, before)
    if result is None:
        print(f"FAIL no result within {args.timeout} s (see logcat GlobalConsole for [slate-docs])")
        return 2
    if args.json:
        args.json.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    versions = {}
    for package in (HOST, PLUGIN):
        dumped = adb(args.serial, "shell", "dumpsys", "package", package, check=False)
        code_match = re.search(r"versionCode=(\d+)", dumped)
        name_match = re.search(r"versionName=(\S+)", dumped)
        versions[package] = f"{name_match.group(1) if name_match else '?'} ({code_match.group(1) if code_match else '?'})"
    print(f"device {result['device']['model']} API {result['device']['sdk']}, host {versions[HOST]}, plugin {versions[PLUGIN]}")
    lines = {block["name"]: block["line"] for block in found}
    passed = failed = skipped = 0
    for entry in result["results"]:
        where = f"{entry['name']} (line {lines.get(entry['name'], '?')})"
        if entry.get("skipped"):
            skipped += 1
            print(f"SKIP {where} {entry['skipped']}")
            continue
        printed = "; ".join(entry["logs"] + [f"toast: {t}" for t in entry["toasts"]])
        if entry["ok"]:
            passed += 1
            print(f"PASS {where} {entry['ms']} ms {printed}".rstrip())
        else:
            failed += 1
            print(f"FAIL {where} {entry['ms']} ms {entry.get('error', '')} {printed}".rstrip())
    if result.get("tasksLeft"):
        print(f"tasks left: {', '.join(result['tasksLeft'])}")
    for path in removed:
        print(f"removed {path}")
    print(f"{passed} PASS, {failed} FAIL, {skipped} SKIP")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
